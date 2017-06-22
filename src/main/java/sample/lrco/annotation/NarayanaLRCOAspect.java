package sample.lrco.annotation;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.transaction.annotation.Transactional;
import sample.lrco.GeodeNarayanaLRCOResource;

import javax.transaction.RollbackException;
import javax.transaction.SystemException;

/**
 * Created by ctzolov on 6/21/17.
 */
@Aspect
public class NarayanaLRCOAspect implements Ordered {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private int order;

    /* (non-Javadoc) */
    @Before("@within(transactional)")
    public void doEnableNarayanaLRCO(Transactional transactional) {
        try {
            logger.info("BINGO #########################################");
            GeodeNarayanaLRCOResource.enlistGeodeAsLastCommitResource();
        } catch (SystemException e) {
            e.printStackTrace();
        } catch (RollbackException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
