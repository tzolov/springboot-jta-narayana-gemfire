/*
 * Copyright 2017. the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.tzolov.geode.jta.narayana.application;

import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.CacheFactory;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.distributed.ServerLauncher;
import net.tzolov.geode.jta.narayana.jndi.SimpleNamingContextBuilder;
import net.tzolov.geode.jta.narayana.lrco.annotation.NarayanaLastResourceCommitOptimization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.annotation.PostConstruct;

import static com.gemstone.gemfire.cache.DataPolicy.PARTITION;

/**
 * @author Christian Tzolov (christian.tzolov@gmail.com)
 */
@SpringBootApplication
@NarayanaLastResourceCommitOptimization
@EnableTransactionManagement(order = 1)
public class SampleNarayanaApplication implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(SampleNarayanaApplication.class);

    // In-Memory JNDI transactionalAccountService used by Gemfire to lookup global transactions.
    // The SimpleNamingContextBuilder MUST be created before the Spring Application Context!
    private static SimpleNamingContextBuilder inMemoryJndiBuilder = SimpleNamingContextBuilder.emptyActivatedContextBuilder();

    @PostConstruct
    public void registerNarayanaUserTransaction() {
        // Gemfire uses JNDI java:/TransactionManager name to lookup the JTA transaction manager.
        inMemoryJndiBuilder.bind("java:/TransactionManager", com.arjuna.ats.jta.TransactionManager.transactionManager());
        //inMemoryJndiBuilder.bind("java:/UserTransaction", com.arjuna.ats.jta.UserTransaction.userTransaction());
    }

    @Autowired
    private AccountService transactionalAccountService;

    public static void main(String[] args) throws Exception {
        SpringApplication.run(SampleNarayanaApplication.class, args).close();
    }

    @Override
    public void run(String... strings) throws Exception {

        ServerLauncher serverLauncher = new ServerLauncher.Builder()
                .set("jmx-manager", "true")
                .set("jmx-manager-start", "true")
                .set("log-level", "debug")
                .build();

        serverLauncher.start();

        Cache cache = new CacheFactory().create();

        Region<String, Account> region = cache.<String, Account>createRegionFactory()
                .setDataPolicy(PARTITION)
                .create("testRegion");

        transactionalAccountService.createAccountAndNotify("tzolov", region);

        LOG.info("JPA entry count is " + transactionalAccountService.jpaEntryCount());

        try {
            // Using username "error" will cause transactionalAccountService to throw SampleRuntimeException
            transactionalAccountService.createAccountAndNotify("error", region);
        } catch (SampleRuntimeException ex) {
            // Log message to let test case know that exception was thrown
            LOG.error(ex.getMessage());
        }
        LOG.info("JAP entry count is still " + transactionalAccountService.jpaEntryCount());

        serverLauncher.stop();
    }
}
