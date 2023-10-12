Patch version of [2.6](Jackson-Release-2.6), released on October 12, 2015.

### Changes, core

#### [Streaming](../../jackson-core)

* [#220](../../jackson-core/issues/220): Problem with `JsonParser.nextFieldName(SerializableString)` for byte-backed parser
* [#221](../../jackson-core/issues/221): Fixed `ArrayIndexOutOfBounds` exception for character-based `JsonGenerator`

#### [Databind](../../jackson-databind)

* [#749](../../jackson-databind/issues/749): `EnumMap` serialization ignores `SerializationFeature.WRITE_ENUMS_USING_TO_STRING`
* [#938](../../jackson-databind/issues/938): Regression: `StackOverflowError` with recursive types that contain `Map.Entry`
* [#939](../../jackson-databind/issues/939): Regression: DateConversionError in 2.6.x 
* [#940](../../jackson-databind/issues/940): Add missing `hashCode()` implementations for `JsonNode` types that did not have them
* [#941](../../jackson-databind/issues/941): Deserialization from `{}` to `ObjectNode` field causes "out of END_OBJECT token" error
* [#942](../../jackson-databind/issues/942): Handle null type id for polymorphic values that use external type id
* [#943](../../jackson-databind/issues/943): Incorrect serialization of `enum` `Map` key
* [#944](../../jackson-databind/issues/944): Failure to use custom deserializer for key deserializer
* [#949](../../jackson-databind/issues/949): Report the offending substring when number parsing fails
* [#965](../../jackson-databind/issues/965): BigDecimal values via @JsonTypeInfo/@JsonSubTypes get rounded

### Changes, data types

#### [Guava](../../jackson-datatype-guava)

* [#83](../../jackson-datatype-guava/issues/83): Generic type information for `Optional` wrapped generic type lost in visitor	
* [#84](../../jackson-datatype-guava/issues/84): Class not found exception in OSGi (`com.fasterxml.jackson.databind.ser.impl.UnwrappingBeanPropertyWriter`)
* (no separate issue): Allow use of `@JsonDeserialize(contentAs=XXX)` with `Optional`

#### [Java 8 Date/Time](../../jackson-datatype-jsr310)

* [#44](../../jackson-datatype-jsr310/issues/44): Allows users to pass a custom DateTimeFormatter for the ZonedDateTimeSerializer
* [#45](../../jackson-datatype-jsr310/issues/45): Problem Deserializing `java.time.OffsetTime` from JSON Array

#### [JDK8](../../jackson-datatype-jdk8)

* [#13](../../jackson-datatype-jdk8/issues/13): Allow use of `@JsonDeserialize(contentAs=XXX)` with `Optional`

### Changes, other modules

#### [Afterburner](../../jackson-module-afterburner)

* [#59](../../jackson-module-afterburner/issues/59): Cannot customize String deserialization behavior
* [#60](../../jackson-module-afterburner/issues/60): Cannot read some "pretty" documents 
    * NOTE: actually fixed by `jackson-core / #220` (see above)

#### [Mr Bean](../../jackson-module-mrbean)

* [#25](../../jackson-module-mrbean/issues/25): Should ignore `static` methods (setter/getter)
