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

package sample.narayana;

import static com.gemstone.gemfire.cache.DataPolicy.PARTITION;

import javax.annotation.PostConstruct;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.CacheFactory;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.distributed.ServerLauncher;
import sample.narayana.jndi.SimpleNamingContextBuilder;
import sample.lrco.annotation.NarayanaLastResourceCommitOptimization;

@SpringBootApplication
@NarayanaLastResourceCommitOptimization
@EnableTransactionManagement(order = 1)
public class SampleNarayanaApplication implements CommandLineRunner {

	private static final Logger LOG = LoggerFactory.getLogger(SampleNarayanaApplication.class);

	// In-Memory JNDI service used by Gemfire to lookup global transactions.
	private static SimpleNamingContextBuilder inMemoryJndiBuilder ;

	// Note: the SimpleNamingContextBuilder MUST be created before the Spring Application Context!!!
	static {
		try {
			inMemoryJndiBuilder = SimpleNamingContextBuilder.emptyActivatedContextBuilder();
		}
		catch (NamingException e) {
			LOG.error("Failed to create in-memory JNDI provider", e);
		}
	}

	@PostConstruct
	public void registerNarayanaUserTransaction() {
		// Gemfire uses JNDI:java:comp/UserTransaction to lookup global transactions.
		//inMemoryJndiBuilder.bind("java:comp/UserTransaction", com.arjuna.ats.jta.UserTransaction.userTransaction());
		inMemoryJndiBuilder.bind("java:/TransactionManager", com.arjuna.ats.jta.TransactionManager.transactionManager());
	}

	@Autowired
	private AccountService service;

	@Autowired
	private AccountRepository repository;

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

		ServerLauncher.ServerState start = serverLauncher.start();

		Cache cache = new CacheFactory().create();

		Region<String, Account> region = cache.<String, Account>createRegionFactory()
				.setDataPolicy(PARTITION)
				.create("testRegion");

		service.createAccountAndNotify("josh", region);
		LOG.info("Count is " + repository.count());
		try {
			// Using username "error" will cause service to throw SampleRuntimeException
			service.createAccountAndNotify("error", region);
		}
		catch (SampleRuntimeException ex) {
			// Log message to let test case know that exception was thrown
			LOG.error(ex.getMessage());
		}
		LOG.info("Count is " + repository.count());
	}
}
