Patch version of [2.13](Jackson-Release-2.13), released on September 3, 2022.

Following fixes are included in this patch release.

### Changes, core

#### [Databind](../../jackson-databind)

* [#3275](../../jackson-databind/issues/3275): JDK 16 Illegal reflective access for `Throwable.setCause()` with `PropertyNamingStrategy.UPPER_CAMEL_CASE`
* [#3565](../../jackson-databind/issues/3565): `Arrays.asList()` value deserialization has changed from mutable to immutable in 2.13
* [#3582](../../jackson-databind/issues/3582): Add check in `BeanDeserializer._deserializeFromArray()` to prevent use of deeply nested arrays [CVE-2022-42004]

### Changes, data formats

#### XML

* [#536](../../jackson-dataformat-xml/issues/536): Upgrade Woodstox to 6.3.1 to get OSGi metadata

#### YAML

* [#329](../../jackson-dataformats-text/issues/329): Update to SnakeYAML 1.31

### Changes, datatypes

#### [Collections](../../jackson-datatypes-collections)

* [#94](../../jackson-datatypes-collection/issues/94): Eclipse Collection serialization for Pairs does not work when upgrading to EC version 11.0.0

### Changes, Other modules

#### Jakarta XmlBind

* [#175](../../jackson-modules-base/issues/175): `jackson-module-jakarta-xmlbind-annotations` should use a Jakarta namespaced Activation API

### Changes, JVM Languages

#### [Kotlin](../../jackson-module-kotlin)

* [#556](../../jackson-module-kotlin/issues/556): Broken Kotlin 1.4 support in 2.13.2

#### [Scala](../../jackson-module-scala)

* [#588](../../jackson-module-scala/issues/588): support immutable ArraySeq deserialization
* [#599](../../jackson-module-scala/issues/599): ScalaAnnotationIntrospectorModule.registerReferencedValueType didn't work properly when fields have default value
