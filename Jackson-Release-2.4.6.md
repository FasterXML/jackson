Patch version of [2.4](Jackson+Release+2.4), released on 23-Apr-2015.

This is planned to be the last release version of 2.4.x branch, since 2.5.2 was released before this release; and since 2.3 will be a long-running branch (due to compatibility reasons).

### Micro-releases

#### 2.4.5.1

Before 2.4.6 (but after 2.4.5), a critical fix needed to be released quickly (for `#735`).
As a result, version `2.4.5.1` of `jackson-databind` was released on March 26, 2015.

It contains following fixes:

* [#706](../../jackson-databind/issues/706): Add support for `@JsonUnwrapped` via JSON Schema module
* [#707](../../jackson-databind/issues/707): Error in getting string representation of an `ObjectNode` with a float number value
* [#735](../../jackson-databind/issues/735): `@JsonDeserialize` on Map with `contentUsing` custom deserializer overwrites default behavior (for other Map instances)

#### 2.4.6.1

After `2.4.6`, a few important fixes were still merged, and since there are no plans for a full release, another micro-patch was cut instead, on 09-Jun-2015. It contains following fixes:

* [#676](../../jackson-databind/issues/676): Deserialization of class with generic collection inside depends on how is was deserialized first time
* [#785](../../jackson-databind/issues/785): Add handling for classes which are available in `Thread.currentThread().getContextClassLoader()`
* [#793](../../jackson-databind/issues/793): `ObjectMapper.readTree()` does not work with defaultTyping enabled

### Changes, core

#### [Streaming](../../jackson-core)

* [#184](../../jackson-core/issues/184): `WRITE_NUMBERS_AS_STRINGS` disables `WRITE_BIGDECIMAL_AS_PLAIN`
* [#191](../../jackson-core/issues/191): Longest collision chain in symbol table now exceeds maximum

#### [Core Databind](../../jackson-databind)

* Fixes from `2.4.5.1` -- `#706`, `#707`, `#735` -- see above.
* [#744](../../jackson-databind/issues/744): Custom deserializer with parent object update failing

### Changes, data formats

#### [CBOR](../../jackson-dataformat-cbor)

* [#9](../../jackson-dataformat-cbor/issues/9): Infinite loop when trying to write binary data using CBORGenerator

#### [Smile](../../jackson-dataformat-smile)

* [#23](../../jackson-dataformat-smile/issues/23): Current location does not always updated properly

### Changes, datatypes

#### [Guava](../../jackson-datatype-guava)

* [#61](../../jackson-datatype-guava/issues/61): NPE serializing `Multimap`s with null values

#### [Hibernate](../../jackson-datatype-hibernate)

* [#65](../../jackson-datatype-hibernate/issues/65): Registered hibernateModule lead to 'NON_EMPTY' failure
