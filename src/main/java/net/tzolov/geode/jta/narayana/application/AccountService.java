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

import org.apache.geode.cache.Region;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * @author Christian Tzolov (christian.tzolov@gmail.com)
 */
@Service
@Transactional
public class AccountService {

    private final JmsTemplate jmsTemplate;

    private final AccountRepository jpaRepository;

    @Autowired
    public AccountService(JmsTemplate jmsTemplate, AccountRepository accountRepository) {
        this.jmsTemplate = jmsTemplate;
        this.jpaRepository = accountRepository;
    }

    public void createAccountAndNotify(String username, Region<String, Account> region) {

        //
        // Enable the Geode LRCO manually (instead of global @NarayanaLastResourceCommitOptimization annotation).
        //
        //NarayanaLrcoSupport.enlistGeodeAsLastCommitResource();

        this.jmsTemplate.convertAndSend("accounts", username);

        Account account = new Account(username);

        this.jpaRepository.save(account);

        region.put(username, account);

        if ("error".equals(username)) {
            throw new SampleRuntimeException("Simulated error");
        }
    }

    public long jpaEntryCount() {
        return this.jpaRepository.count();
    }
}
