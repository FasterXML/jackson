Patch version of [2.8](Jackson-Release-2.8), released 14th Oct 2016.

Following fixes are included.

### Changes, core

#### [Streaming](../../jackson-core/)

#### [Databind](../../jackson-databind/)

* [#466](../../jackson-databind/466): Jackson ignores Type information when raw return type is `BigDecimal` or `BigInteger`
* [#1001](../../jackson-databind/1001): Parameter names module gets confused with delegate creator which is a static method
* [#1324](../../jackson-databind/1324): Boolean parsing with `StdDeserializer` is too slow with huge integer value
* [#1383](../../jackson-databind/1383): Problem with `@JsonCreator` with 1-arg factory-method, implicit param names
* [#1384](../../jackson-databind/1384): `@JsonDeserialize(keyUsing = ...)` does not work correctly together with DefaultTyping.NON_FINAL
* [#1385](../../jackson-databind/1385): Polymorphic type lost when using `@JsonValue`
* [#1389](../../jackson-databind/1389): Problem with handling of multi-argument creator with Enums
* [#1392](../../jackson-databind/1392): Custom UnmodifiableSetMixin Fails in Jackson 2.7+ but works in Jackson 2.6
* [#1395](../../jackson-databind/1395): Problems deserializing primitive `long` field while using `TypeResolverBuilder`

### Changes, data formats

#### [XML](../../jackson-dataformat-xml)

* [#211](../../jackson-dataformat-xml/issues/211): Disable `SUPPORT_DTD` for `XMLInputFactory` unless explicitly overridden

#### [YAML](../../jackson-dataformat-yaml)

* [#77](../../jackson-dataformat-yaml/issues/77): Boolean-like content of string must never be unquoted

### Changes, [Jackson jr](../../jackson-jr)

* [#49](../../jackson-jr/issues/49): `ArrayIndexOutOfBoundsException` when parsing large Map

### Changes, other modules

#### [Guice](../../jackson-modules-base)

* [#22](../../jackson-modules-base/issues/22): Allow use of Guice 4.x (still only require 3.x)

#### [JAXB Annotations](../../jackson-module-jaxb-annotations)

* [#63](../../jackson-module-jaxb-annotations/issues/63): Error in type resolution of reference type (`Optional`)

#### [JSON Schema](../../jackson-module-jsonSchema)

* [#112](../../jackson-module-jsonSchema/issues/112): Update `LinkDescriptionObject` to remove redundant 'jsonSchema' property
