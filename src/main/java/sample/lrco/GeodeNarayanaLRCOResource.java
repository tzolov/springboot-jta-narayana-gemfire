/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sample.lrco;

import com.arjuna.ats.jta.resources.LastResourceCommitOptimisation;
import com.gemstone.gemfire.LogWriter;
import com.gemstone.gemfire.cache.CacheFactory;
import com.gemstone.gemfire.cache.TransactionId;
import com.gemstone.gemfire.internal.cache.GemFireCacheImpl;
import com.gemstone.gemfire.internal.cache.TXManagerImpl;
import com.gemstone.gemfire.internal.cache.TXStateProxy;
import com.gemstone.gemfire.internal.i18n.LocalizedStrings;

import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

/**
 * @author Christian Tzolov
 */
public class GeodeNarayanaLRCOResource implements LastResourceCommitOptimisation {

    private volatile GemFireCacheImpl cache;
    private volatile TXManagerImpl gfTxMgr;
    private volatile TransactionId tid;
    private volatile boolean initDone = false;

    @Override
    public void commit(Xid xid, boolean b) throws XAException {
        LogWriter logger = this.cache.getLogger();
        if (logger.fineEnabled()) {
            logger.fine("GeodeNarayanaLRCOResource:invoked commit");
        }

        TXStateProxy tsp = this.gfTxMgr.getTXState();
        if (tsp != null && this.tid != tsp.getTransactionId()) {
            throw new IllegalStateException("Local Transaction associated with Tid = " + this.tid + " attempting to commit a different transaction");
        } else {
            try {
                this.gfTxMgr.commit();
                this.tid = null;
            } catch (Exception var4) {
                throw new XAException(var4.toString());
            }
        }
    }

    @Override
    public void end(Xid xid, int i) throws XAException {
        throw new XAException("End called on Last Resource Txt!" + xid + ", i=" + i);
    }

    @Override
    public void forget(Xid xid) throws XAException {
        throw new XAException("Forget called on Last Resource Txt!" + xid);
    }

    @Override
    public int getTransactionTimeout() throws XAException {
        return 0;
    }

    @Override
    public boolean isSameRM(XAResource xaResource) throws XAException {
        return xaResource instanceof GeodeNarayanaLRCOResource;
    }

    @Override
    public int prepare(Xid xid) throws XAException {
        throw new XAException("Prepare called on Last Resource Txt!" + xid);
    }

    @Override
    public Xid[] recover(int i) throws XAException {
        return new Xid[0];
    }

    @Override
    public void rollback(Xid xid) throws XAException {
        TXStateProxy tsp = this.gfTxMgr.getTXState();
        if (tsp != null && this.tid != tsp.getTransactionId()) {
            throw new IllegalStateException("Local Transaction associated with Tid = " + this.tid + " attempting to commit a different transaction");
        } else {
            LogWriter logger = this.cache.getLogger();
            if (logger.fineEnabled()) {
                logger.fine("GeodeNarayanaLRCOResource:invoked rollback");
            }

            try {
                this.gfTxMgr.rollback();
            } catch (IllegalStateException var8) {
                if (!var8.getMessage().equals(LocalizedStrings.TXManagerImpl_THREAD_DOES_NOT_HAVE_AN_ACTIVE_TRANSACTION.toLocalizedString())) {
                    throw new XAException(var8.toString());
                }
            } catch (Exception var9) {
                throw new XAException(var9.toString());
            } finally {
                this.tid = null;
            }
        }
    }

    @Override
    public boolean setTransactionTimeout(int i) throws XAException {
        return false;
    }

    @Override
    public void start(Xid xid, int i) throws XAException {
        try {
            if (!this.initDone || this.cache.isClosed()) {
                this.init();
            }

            LogWriter logger = this.cache.getLogger();
            if (logger.fineEnabled()) {
                logger.fine("GeodeNarayanaLRCOResource::start:" + xid + ", i=" + i);
            }

            TransactionManager tm = this.cache.getJTATransactionManager();
            System.out.println("TransactionManager: " + tm.getClass());
            if (this.tid != null) {
                throw new XAException(" A transaction is already in progress");
            } else {
                if (tm != null && tm.getTransaction() != null) {
                    if (logger.fineEnabled()) {
                        logger.fine("GeodeNarayanaLRCOResource: JTA transaction is on");
                    }

                    TXStateProxy tsp = this.gfTxMgr.getTXState();
                    if (tsp != null) {
                        throw new XAException("GemFire is already associated with a transaction");
                    }

                    this.gfTxMgr.begin();
                    tsp = this.gfTxMgr.getTXState();
                    tsp.setJCATransaction();
                    this.tid = tsp.getTransactionId();
                    if (logger.fineEnabled()) {
                        logger.fine("GeodeNarayanaLRCOResource:begun GFE transaction");
                    }
                } else if (logger.fineEnabled()) {
                    logger.fine("GeodeNarayanaLRCOResource: JTA Transaction does not exist.");
                }

            }
        } catch (SystemException var4) {
            throw new XAException(var4.getMessage());
        }
    }

    private void init() throws SystemException {
        this.cache = (GemFireCacheImpl) CacheFactory.getAnyInstance();
        LogWriter logger = this.cache.getLogger();
        if (logger.fineEnabled()) {
            logger.fine("GeodeNarayanaLRCOResource:init. Inside init");
        }

        this.gfTxMgr = this.cache.getTxManager();
        this.initDone = true;
    }

    /**
     * Use this helper method to enlist Geode as a Last Resource Commit in current transaction.
     * <p>
     * Must be called inside the transactional boundaries but before any Geode operation was used! For example:
     * <p>
     * <pre>
     *      &#64;Transactional
     *     	public void myServiceMethod(Region region) throws SystemException, RollbackException {
     *
     * 			// Enable LRCO
     * 			enlistGeodeAsLastCommitResource();
     *          ......
     *
     *          region.put(KEY, VALUE);
     *      }
     *
     * </pre>
     *
     * @throws SystemException
     * @throws RollbackException
     */
    public static void enlistGeodeAsLastCommitResource() throws SystemException, RollbackException {

        Transaction tx = com.arjuna.ats.jta.TransactionManager.transactionManager().getTransaction();
        tx.enlistResource(new GeodeNarayanaLRCOResource());

        LogWriter logger = CacheFactory.getAnyInstance().getLogger();
        if (logger.fineEnabled()) {
            logger.fine("GeodeNarayanaLRCOResource:Enlist into: " + tx.getClass());
        }
    }
}
