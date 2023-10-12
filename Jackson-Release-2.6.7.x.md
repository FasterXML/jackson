After last full version of [2.6](Jackson-Release-2.6), [2.6.7](Jackson-Release-2.6.7), was released branch was closed.

Following micro-patches have been released since.

As of June, 2021, the branch is fully closed for micro-patches as well: 2.6.7.5 is the very last release.

## Databind, 2.6.7.1 (11-Jul-2017)

An important security fix (see `1599` below) was backported into 2.6.x branch, resulting in patch version with following fixes:

* [#1383](../../jackson-databind/issues/1383): Problem with `@JsonCreator` with 1-arg factory-method, implicit param names
* [#1599](../../jackson-databind/issues/1599): Backport the extra safety checks for polymorphic deserialization

## Databind, 2.6.7.2 (13-Nov-2018)

As per earlier cases, CVE-related backport(s):

* [#1737](../../jackson-databind/issues/1737): Block more JDK types from polymorphic deserialization

## Databind, 2.6.7.3 (16-Oct-2019)

Backported all CVE fixes up to 2.9.10

* [#1680](../../jackson-databind/issues/1680): Block more JDK gadget types (com.sun.rowset)
* [#1855](../../jackson-databind/issues/1855): Block more serialization gadgets (dbcp/tomcat, spring / CVE-2017-17485]
* [#1899](../../jackson-databind/issues/1899): Another two gadgets to exploit default typing issue in jackson-databind (CVE-2018-5968)
* [#2032](../../jackson-databind/issues/2032): Block one more gadget type (mybatis, CVE-2018-11307)
* [#2052](../../jackson-databind/issues/2052): Block one more gadget type (jodd-db, CVE-2018-12022)
* [#2058](../../jackson-databind/issues/2058): Block one more gadget type (oracle-jdbc, CVE-2018-12023)
* [#2097](../../jackson-databind/issues/2097): Block more classes from polymorphic deserialization (CVE-2018-14718 - CVE-2018-14721)
* [#2186](../../jackson-databind/issues/2186): Block more classes from polymorphic deserialization (CVE-2018-19360, CVE-2018-19361, CVE-2018-19362)
* [#2326](../../jackson-databind/issues/2326): Block one more gadget type (mysql, CVE-2019-12086)
* [#2334](../../jackson-databind/issues/2334): Block one more gadget type (logback, CVE-2019-12384)
* [#2341](../../jackson-databind/issues/2341): Block yet another gadget type (jdom, CVE-2019-12814)
* [#2387](../../jackson-databind/issues/2387): Block one more gadget type (ehcache, CVE-2019-14379)
* [#2389](../../jackson-databind/issues/2389): Block one more gadget type (logback, CVE-2019-14439)
* [#2410](../../jackson-databind/issues/2410): Block one more gadget type (HikariCP, CVE-2019-14540)
* [#2420](../../jackson-databind/issues/2420): Block one more gadget type (cxf-jax-rs, no CVE allocated yet)
* [#2449](../../jackson-databind/issues/2449): Block one more gadget type (HikariCP, CVE-2019-14439 / CVE-2019-16335)
* [#2462](../../jackson-databind/issues/2462): Block two more gadget types (commons-configuration/-2)
* [#2478](../../jackson-databind/issues/2478): Block two more gadget types (commons-dbcp, p6spy, CVE-2019-16942 / CVE-2019-16943)
* [#2498](../../jackson-databind/issues/2498): Block one more gadget type (apache-log4j-extras/1.2, CVE-2019-17531)

## Databind, 2.6.7.4 (25-Oct-2020)

Backported all CVE fixes up to 2.9.10.6

* [#1279](../../jackson-databind/issues/1279): Ensure DOM parsing defaults to not expanding external entities
* [#2469](../../jackson-databind/issues/2469): Block one more gadget type (xalan2)
* [#2526](../../jackson-databind/issues/2526): Block two more gadget types (ehcache/JNDI - CVE-2019-20330)
* [#2589](../../jackson-databind/issues/2589): `DOMDeserializer`: setExpandEntityReferences(false) may not prevent external entity expansion in all cases [CVE-2020-25649]
* [#2620](../../jackson-databind/issues/2620): Block one more gadget type (xbean-reflect/JNDI - CVE-2020-8840)
* [#2631](../../jackson-databind/issues/2631): Block one more gadget type (shaded-hikari-config, CVE-2020-9546)
* [#2634](../../jackson-databind/issues/2634): Block two more gadget types (ibatis-sqlmap, anteros-core; CVE-2020-9547 / CVE-2020-9548)
* [#2642](../../jackson-databind/issues/2642): Block one more gadget type (javax.swing, CVE-2020-10969)
* [#2648](../../jackson-databind/issues/2648): Block one more gadget type (shiro-core)
* [#2653](../../jackson-databind/issues/2653): Block one more gadget type (shiro-core, 2nd class)
* [#2658](../../jackson-databind/issues/2658): Block one more gadget type (ignite-jta, CVE-2020-10650)
* [#2659](../../jackson-databind/issues/2659): Block one more gadget type (aries.transaction.jms, CVE-2020-10672)
* [#2660](../../jackson-databind/issues/2660): Block one more gadget type (caucho-quercus, CVE-2020-10673)
* [#2662](../../jackson-databind/issues/2662): Block one more gadget type (bus-proxy, CVE-2020-10968)
* [#2664](../../jackson-databind/issues/2664): Block one more gadget type (activemq-pool[-jms], CVE-2020-11111)
* [#2666](../../jackson-databind/issues/2666): Block one more gadget type (apache/commons-proxy, CVE-2020-11112)
* [#2670](../../jackson-databind/issues/2670): Block one more gadget type (openjpa, CVE-2020-11113)
* [#2680](../../jackson-databind/issues/2680): Block one more gadget type (SSRF, spring-jpa, CVE-2020-11619)
* [#2682](../../jackson-databind/issues/2682): Block one more gadget type (commons-jelly, CVE-2020-11620)
* [#2688](../../jackson-databind/issues/2688): Block one more gadget type (apache-drill, CVE-2020-14060)
* [#2698](../../jackson-databind/issues/2698): Block one more gadget type (weblogic/oracle-aqjms, CVE-2020-14061)
* [#2704](../../jackson-databind/issues/2704): Block one more gadget type (jaxp-ri, CVE-2020-14062)
* [#2765](../../jackson-databind/issues/2765): Block one more gadget type (org.jsecurity, CVE-2020-14195)
* [#2798](../../jackson-databind/issues/2798): Block one more gadget type (com.pastdev.httpcomponents, CVE-2020-24750)
* [#2814](../../jackson-databind/issues/2814): Block one more gadget type (Anteros-DBCP, CVE-2020-24616)
* [#2826](../../jackson-databind/issues/2826): Block one more gadget type (com.nqadmin.rowset)
* [#2827](../../jackson-databind/issues/2827): Block one more gadget type (org.arrahtec:profiler-core)

## Databind, 2.6.7.5 (21-Jun-2021)

The Very Last 2.6 Micro-Patch ever.

* [#1931](../../jackson-databind/issues/1931): Block two more gadgets to exploit default typing issue (c3p0, CVE-2018-7489)
* [#2798](../../jackson-databind/issues/2798): Block one more gadget type (com.pastdev.httpcomponents, CVE-2020-24750)
* [#2854](../../jackson-databind/issues/2854): Block one more gadget type (javax.swing, CVE-2021-20190)
* [#2986](../../jackson-databind/issues/2986): Block 2 more gadget types (commons-dbcp2, CVE-2020-35490 / CVE-2020-35491)
* [#2996](../../jackson-databind/issues/2996): Block 2 more gadget types (newrelic-agent, CVE-2020-36188 / CVE-2020-36189)
* [#2997](../../jackson-databind/issues/2997): Block 2 more gadget types (tomcat/naming-factory-dbcp, CVE-2020-36186 / CVE-2020-36187)
* [#2998](../../jackson-databind/issues/2998): Block 2 more gadget types (org.apache.tomcat/tomcat-dbcp, CVE-2020-36184 / CVE-2020-36185)
* [#2999](../../jackson-databind/issues/2999): Block one more gadget type (org.glassfish.web/javax.servlet.jsp.jstl, CVE-2020-35728)
* [#3003](../../jackson-databind/issues/3003): Block one more gadget type (org.docx4j.org.apache:xalan-interpretive, CVE-2020-36183)
* [#3004](../../jackson-databind/issues/3004): Block some more DBCP-related potential gadget classes (CVE-2020-36179 / CVE-2020-36182)


