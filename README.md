# JTA Narayana (e.g. JBoss JTA) with Gemfire

Use Narayana JTA provider as global transaction manager to coordinate GemFire/Geode cache transactions along with JPA/JDBC and/or JMS resources.

[Narayana](http://narayana.io//docs/project/index.html) is light-weight (e.g. out-of-container), embeddable global transaction manager. Narayana is JTA compliant and can be integrated with Gemfire/Geode to perform XA transaction across Geode, JPA/JDBC and JMS operations. 

Fruthermore Narayana support [Last Resource Commit Optiomization](http://narayana.io//docs/project/index.html#d0e1859) allowing with Gemfire/Geode transactions to be run as last resources.

## Geode/Gemfire JTA Background
Out of the box, Gemfire/Geode provides the following [JTA Global Transactions](http://geode.apache.org/docs/guide/11/developing/transactions/JTA_transactions.html) integration options:

1. Have Gemfire/Geode act as JTA transaction manager - Mind that Gemfire JTA manager is **not JTA compliant** and could cause synchronization and transaction coordination problems. In its current state you better not use it as JTA manager!
2. Coordinate with an external JTA transaction manager in a container (such as WebLogic or JBoss). Also GemFire can be set as the "last resource" while using a container. - While this approach provides a reliable JTA capabilities it requires a heavey-weight JEE container. 

The [SpringBoot Narayana](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-jta.html#boot-features-jta-narayana) 
integration extends option (2) by using Narayana as an external JTA manager without the need of running a J2EE container. 

At startup GemFire looks for a TransactionManager `javax.transaction.TransactionManager` that has been bound to its `JNDI` context. 
When GemFire finds such an external transaction manager, all GemFire region operations (such as get and put) will participate in 
global transactions hosted by this external JTA transaction manager: [Coordinates with External JTA Transactions Managers](http://geode.apache.org/docs/guide/11/developing/transactions/JTA_transactions.html#concept_cp1_zx1_wk)

Because Gemfire/Gedoe require JNDI provider to lookup the global transactions we have build a simple (in-memory) JNDI provider: `io.pivotal.poc.gemfire.gtx.jndi.SimpleNamingContextBuilder`.

Note: `SimpleNamingContextBuilder` re-uses the code from the `spring-test` project. If you know of better solution for createing in-memory JNDI providers please let me know!

#### Last Resource Commit Optimization (LRCO)
Narayana allows a single resource that is only one-phase aware (such as Geode), and does not support the prepare phase, can be enlisted with a transaction that is manipulating two-phase aware participants.
The `GeodeNarayanaLrcoResource` implements the required `com.arjuna.ats.jta.resources.LastResourceCommitOptimisation` marker interface, allowing Geode to be enlisted as Last Resource in Narayana JTA transactions.

The `NarayanaLrcoSupport.enlistGeodeAsLastCommitResource` helper allows to enlist manually Geode LRCO like this:  
```$java
 @Transactional
 public void myServiceMethod(Region region) {
    
   // Enable Geode Narayana LRCO
   NarayanaLrcoSupport.enlistGeodeAsLastCommitResource();
 
   .....
     
   region.put(KEY, VALUE);
   region.get(KEY);
   .....
 }

```
The `enlistGeodeAsLastCommitResource()` method must be called within the transactional boundaries (e.g in a transactional method) but before any Geode operation was used!
 
The `@NarayanaLastResourceCommitOptimization` annotation allows to automatically enlist Geode as Last Resource in any transaction defined by `@Tranasactional` annotation.
  
The `@NarayanaLastResourceCommitOptimization` annotation may only be used on a Spring application that is also annotated with `@EnableTransactionManagement` with an explicit `order` set to value other than `Integer#MAX_VALUE` or `Integer#MIN_VALUE`.  
 
```$java
@SpringBootApplication
@NarayanaLastResourceCommitOptimization
@EnableTransactionManagement(order = 1)
public class SampleNarayanaApplication implements CommandLineRunner { ... }

```


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
     -jar ./target/springboot-jta-narayana-gemfire-1.0.0-SNAPSHOT.jar

