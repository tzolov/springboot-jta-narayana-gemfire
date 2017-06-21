# JTA Narayana (e.g. JBoss JTA) with Gemfire

Use Narayana JTA provider as global transaction manager to coordinate GemFire/Geode cache transactions along with JPA/JDBC and/or JMS resources.

[Narayana](http://narayana.io//docs/project/index.html) is light-weight (e.g. out-of-container), embeddable global transaction manager. Narayana is JTA compliant and can be integrated with Gemfire/Geode to perform XA transaction across Geode, JPA/JDBC and JMS operations. 

Fruthermore Narayana support [Last Resource Commit Optiomization](http://narayana.io//docs/project/index.html#d0e1859) allowing with Gemfire/Geode transactions to be run as last resources.

## Geode/Gemfire JTA Background
Out of the box, Gemfire/Geode provides the following [JTA Global Transactions](http://geode.docs.pivotal.io/docs/developing/transactions/JTA_transactions.html) integration options:

1. Have Gemfire/Geode act as JTA transaction manager - Mind that Gemfire JTA manager is **not JTA compliant** and could cause synchronization and transaction coordination problems. In its current state you better not use it as JTA manager!
2. Coordinate with an external JTA transaction manager in a container (such as WebLogic or JBoss). Also GemFire can be set as the "last resource" while using a container. - While this approach provides a reliable JTA capabilities it requires a heavey-weight JEE container. 

The [SpringBoot Narayana](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-jta.html#boot-features-jta-narayana) 
integration extends option (2) by using Narayana as an external JTA manager without the need of running a J2EE container. 

At startup GemFire looks for a TransactionManager `javax.transaction.TransactionManager` that has been bound to its `JNDI` context. 
When GemFire finds such an external transaction manager, all GemFire region operations (such as get and put) will participate in 
global transactions hosted by this external JTA transaction manager: [Coordinates with External JTA Transactions Managers](http://geode.docs.pivotal.io/docs/developing/transactions/JTA_transactions.html#concept_cp1_zx1_wk)

Because Gemfire/Gedoe require JNDI provider to lookup the global transactions we have build a simple (in-memory) JNDI provider: `io.pivotal.poc.gemfire.gtx.jndi.SimpleNamingContextBuilder`.

Note: `SimpleNamingContextBuilder` re-uses the code from the `spring-test` project. If you know of better solution for createing in-memory JNDI providers please let me know!

## Build (default with Gemfire)
``` 
mvn clean install
```

## Run
```
java -Dgemfire.name=server1 \
     -Dgemfire.server.port=40405 \
     -Dgemfire.jmx-manager-port=1199 \
     -Dgemfire.jmx-manager=true \
     -Dgemfire.jmx-manager-start=true \
     -jar ./target/springboot-jta-narayana-gemfire-1.5.4.RELEASE.jar

