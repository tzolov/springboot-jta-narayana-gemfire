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

import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import com.gemstone.gemfire.cache.Region;

@Service
@Transactional
public class AccountService {

	private final JmsTemplate jmsTemplate;

	private final AccountRepository accountRepository;

	@Autowired
	public AccountService(JmsTemplate jmsTemplate, AccountRepository accountRepository) {
		this.jmsTemplate = jmsTemplate;
		this.accountRepository = accountRepository;
	}

	public void createAccountAndNotify(String username, Region<String, Account> region) {

		this.enableLastResourceCommit();

		this.jmsTemplate.convertAndSend("accounts", username);

		Account account = new Account(username);

		this.accountRepository.save(account);

		region.put(username, account);

		if ("error".equals(username)) {
			throw new SampleRuntimeException("Simulated error");
		}
	}

	private void enableLastResourceCommit() {
		try {
			Transaction tx = com.arjuna.ats.jta.TransactionManager.transactionManager().getTransaction();
			tx.enlistResource(new GeodeLastResourceCommit());
			System.out.println("Enlist LRCO in: " + tx.getClass());
		} catch (RollbackException e) {
			e.printStackTrace();
		} catch (SystemException e) {
			e.printStackTrace();
		}
	}
}
