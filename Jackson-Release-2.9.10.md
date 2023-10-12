Patch version of [2.9](Jackson-Release-2.9), released 2019-09-21.

It will likely remain the last full 2.9.x release.

Following fixes are included (note: this includes fixes that intermediate `2.9.9.x` micro-patches had)

### Changes, core

#### [Streaming](../../jackson-core)

* [#540](../../jackson-core/issues/540): UTF8StreamJsonParser: fix byte to int conversion for malformed escapes
* [#556](../../jackson-core/issues/556): 'IndexOutOfBoundsException' in UTF8JsonGenerator.writeString(Reader, len) when using a negative length

#### [Databind](../../jackson-databind)

* [#2331](../../jackson-databind/issues/2331): `JsonMappingException` through nested getter with generic wildcard return type
* [#2334](../../jackson-databind/issues/2334): Block one more gadget type (logback, CVE-2019-12384)
* [#2341](../../jackson-databind/issues/2341): Block one more gadget type (jdom, CVE-2019-12814)
* [#2374](../../jackson-databind/issues/2374): `ObjectMapper. getRegisteredModuleIds()` throws NPE if no modules registered
* [#2387](../../jackson-databind/issues/2387): Block one more gadget type (ehcache, CVE-2019-14379)
* [#2389](../../jackson-databind/issues/2389): Block one more gadget type (logback, CVE-2019-14439)
* [#2404](../../jackson-databind/issues/2404): FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY setting ignored when creator properties are buffered
* [#2410](../../jackson-databind/issues/2410): Block one more gadget type (HikariCP, CVE-2019-14540)
* [#2420](../../jackson-databind/issues/2420): Block one more gadget type (no CVE allocated yet)
* [#2449](../../jackson-databind/issues/2449): Block one more gadget type (HikariCP, CVE-2019-14439 / CVE-2019-16335)
* [#2460](../../jackson-databind/issues/2460): Block one mode gadget type (ehcache, CVE-2019-17267)
* [#2462](../../jackson-databind/issues/2462): Block two more gadget types (commons-configuration)
* [#2469](../../jackson-databind/issues/2469): Block one mode gadget type (xalan2)

### Changes, dataformats

#### [XML](../../jackson-dataformat-xml)

* [#336](../../jackson-dataformat-xml/issues/336): `WRITE_BIGDECIMAL_AS_PLAIN` Not Used When Writing Pretty
* [#340](../../jackson-dataformat-xml/issues/340): Incompatible woodstox-core and stax2-api dependencies (upgrade to `woodstox-core` 5.3.0)

### Changes, other

#### [Scala](../../jackson-module-scala)

The first release to support Scala 2.13. Thanks to Adriaan Moors and Seth Tisue!

* [#399](../../jackson-module-scala/issues/399): JsonScalaEnumeration annotation not picked up when using a Mixin
* [#429](../../jackson-module-scala/issues/429): Serialization behavior of case objects is different when using scala 2.13
