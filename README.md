# Narayana (e.g. JBoss) JTA with Geode/GemFire

Use Narayana JTA provider as global transaction manager to coordinate Geode/GemFire cache transactions along with JPA/JDBC and/or JMS resources.

[Narayana](http://narayana.io//docs/project/index.html) is light-weight (e.g. out-of-container), embeddable global transaction manager. Narayana is JTA compliant and can be integrated with Geode/GemFire to perform XA transaction across Geode, JPA/JDBC and JMS operations. 

Also Narayana supports [Last Resource Commit Optiomization](http://narayana.io//docs/project/index.html#d0e1859) allowing with Geode/GemFire transactions to be run as last resources.

## Geode/GemFire JTA Background
Out of the box, Geode/GemFire provides the following [JTA Global Transactions](http://geode.apache.org/docs/guide/11/developing/transactions/JTA_transactions.html) integration options:

1. Have Geode/GemFire act as JTA-like transaction manager _(deprecated)_ - This is **not JTA compliant** solution and could cause synchronization and transaction coordination problems. Deprecated in the latest Geode/GemFire releases and in its current state you better not use it as JTA manager!
2. Coordinate with an external JTA transaction manager in a container (such as WebLogic or JBoss). Also Geode/GemFire can be set as the "last resource" while using a container. - While this approach provides a reliable JTA capabilities it requires a heavy-weight JEE container. 

**The [SpringBoot Narayana](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-jta.html#boot-features-jta-narayana) 
integration extends option (2) by using Narayana as an external JTA manager without the need of running a J2EE container.** 

#### Geode/GemFire JTA Lookup (JNDI)
At startup Geode/GemFire looks for a TransactionManager `javax.transaction.TransactionManager` bound to well known `JNDI` context paths. 
When Geode/GemFire finds such an external transaction manager, it enlists all `Region` operations (such as `GET` and `PUT`...) to participate in 
the global transactions hosted by this external JTA transaction manager: [Coordinates with External JTA Transactions Managers](http://geode.apache.org/docs/guide/11/developing/transactions/JTA_transactions.html#concept_cp1_zx1_wk)

For the needs of Geode/GemFire JNDI lookup, we leverage the standalone JNPServer (in-memory) JNDI provider. Later requires adding the `org.jboss.naming:jnpserver:5.0.3.GA` pom dependency.
JNPServer can be used like this:
 
```java
    // Standalone JNDI server used by Gemfire to lookup global transactions.
    private static final NamingBeanImpl JndiServer = new NamingBeanImpl();

    @PostConstruct
    public void registerNarayanaUserTransaction() throws Exception {
        // Gemfire uses JNDI java:/TransactionManager name to lookup the JTA transaction manager.
        JndiServer.start();
        // Bind JTA implementation with default names
        JNDIManager.bindJTAImplementation();
    }

    ....
    JndiServer.stop();
```
Add the following `jndi.properties` file to the root classpath:
```properties
java.naming.factory.initial=org.jnp.interfaces.NamingContextFactory
java.naming.factory.url.pkgs=org.jboss.naming:org.jnp.interfaces
```

#### Last Resource Commit Optimization (LRCO)

The `Last Resource` feature in certain JTA manager implementations allow the use one `non-XAResource` (such as Geode/GemFire) in a transaction with multiple `XAResources` while ensuring consistency.

Geode/GemFire already supports `Last Resource` for some 3rd party Application Servers such as WebLogic or WebSphere. But this requires running the Geode/GemFire application inside an Application Server and installing of dedicated [Geode/GemFire JCA](http://geode.apache.org/docs/guide/11/developing/transactions/JTA_transactions.html#concept_csy_vfb_wk)

Narayana allows a single resource that is only one-phase aware and does not support the prepare phase (such as Geode), to be enlisted with a transaction that is manipulating two-phase aware participants.

The [GeodeNarayanaLrcoResource](./src/main/java/net/tzolov/geode/jta/narayana/lrco/GeodeNarayanaLrcoResource.java) implements the required `com.arjuna.ats.jta.resources.LastResourceCommitOptimisation` interface, allowing Geode/GemFire to be enlisted as Last Resource in Narayana JTA transactions.

The `NarayanaLrcoSupport.enlistGeodeAsLastCommitResource` manually enlists Geode/GemFire as Last Resource:  
```java
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
The `enlistGeodeAsLastCommitResource()` method must be called within the transactional boundaries (e.g in a transactional method) but before any Geode/GemFire operation is used!
 
The `@NarayanaLastResourceCommitOptimization` annotation allows to automatically enlist Geode/GemFire as Last Resource in any transaction defined by `@Tranasactional` annotation.
  
The `@NarayanaLastResourceCommitOptimization` annotation may only be used on a Spring application that is also annotated with `@EnableTransactionManagement` with an explicit `order` set to value other than `Integer#MAX_VALUE` or `Integer#MIN_VALUE`.  
 
```java
@SpringBootApplication
@NarayanaLastResourceCommitOptimization
@EnableTransactionManagement(order = 1)
public class SampleNarayanaApplication implements CommandLineRunner { 
  ... 
}
```

## Build
You can build for either GemFire 9.x (default) or Geode 1.x 
##### GemFire 9.x (default)
For GemFire 9.x, follow the instructions to configure the access to the [Pivotal Commercial Maven Repository](http://gemfire.docs.pivotal.io/gemfire/getting_started/installation/obtain_gemfire_maven.html)  
``` 
mvn clean install
```

##### Geode 1.x (with -Pgeode)
``` 
mvn clean install -Pgeode
```

## Run
```
java -Dgemfire.name=server1 \
     -Dgemfire.server.port=40405 \
     -Dgemfire.jmx-manager-port=1199 \
     -Dgemfire.jmx-manager=true \
     -Dgemfire.jmx-manager-start=true \
     -jar ./target/springboot-jta-narayana-gemfire-1.0.0-SNAPSHOT.jar

