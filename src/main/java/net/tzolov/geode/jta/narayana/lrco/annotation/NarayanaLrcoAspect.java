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

package net.tzolov.geode.jta.narayana.lrco.annotation;

import net.tzolov.geode.jta.narayana.lrco.NarayanaLrcoSupport;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * {@link NarayanaLrcoAspect} is a Spring {@link Aspect}, the get activated when the process execution flow
 * crosses transactions boundaries.
 * <p>
 * When a Spring {@link org.springframework.stereotype.Service @Service component} class or method are annotated with a
 * {@link Transactional @Transactional annotation} then this aspect will enlist Geode as a
 * {@link com.arjuna.ats.jta.resources.LastResourceCommitOptimisation}, non-compliant XA resource withing the ongoing
 * Narayana Transaction.
 *
 * @author Christian Tzolov (christian.tzolov@gmail.com)
 */
@Aspect
public class NarayanaLrcoAspect implements Ordered {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private int order;

    @Value("${spring.jta.narayana.onePhaseCommit:false}")
    private boolean jtaNarayanaOnePhaseCommit;

    /* (non-Javadoc) */
    @Before("@within(transactional)")
    public void doEnableGeodeNarayanaLastResourceCommitOptimization(Transactional transactional) {

        Assert.isTrue(jtaNarayanaOnePhaseCommit, "The spring.jta.narayana.onePhaseCommit must be " +
                "set to true for the LastResourceCommitOptimization to work!");

        if (logger.isDebugEnabled()) {
            logger.debug("intercept a running transaction");
        }

        NarayanaLrcoSupport.enlistGeodeAsLastCommitResource();
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
