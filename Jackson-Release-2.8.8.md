Patch version of [2.8](Jackson-Release-2.8), released on April 05, 2017.

Following fixes are included.

### Changes, core

#### [Streaming](../../jackson-core)

* [#359](../../jackson-core/issues/359): `FilteringGeneratorDelegate` does not override `writeStartObject(Object forValue)`

#### [Databind](../../jackson-databind)

* [#1345](../../jackson-databind/issues/1345]): `@JsonProperty(access = READ_ONLY)` together with generated constructor (Lombok) causes exception: "Could not find creator property with name ..."
* [#1533](../../jackson-databind/issues/1533): `AsPropertyTypeDeserializer` ignores `DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT`
* [#1543](../../jackson-databind/issues/1543): JsonFormat.Shape.NUMBER_INT does not work when defined on enum type in 2.8
* [#1570](../../jackson-databind/issues/1570): `Enum` key for `Map` ignores `SerializationFeature.WRITE_ENUMS_USING_INDEX`
* [#1573](../../jackson-databind/issues/1573): Missing properties when deserializing using a builder class with a non-default constructor and a mutator annotated with `@JsonUnwrapped`
* [#1575](../../jackson-databind/issues/1575): Problem with `@JsonIgnoreProperties` on recursive property (regression in 2.8)

### Changes, dataformats

#### [Avro](../../jackson-dataformats-binary/)

* [#58](../../jackson-dataformats-binary/issues/58): Regression due to changed namespace of inner enum types

#### [CBOR](../../jackson-dataformats-binary/)

* [#62](../../jackson-dataformats-binary/issues/62): `java.lang.ArrayIndexOutOfBoundsException` at `CBORGenerator.java:548`

#### [protobuf](../../jackson-dataformats-binary/)

* [#54](../../jackson-dataformats-binary/issues/54): Some fields are left null
* [#67](../../jackson-dataformats-binary/issues/67): Serialization of multiple nesting levels has issues

#### [YAML](../../jackson-dataformat-yaml)

* [#72](../../jackson-dataformat-yaml/issues/72): Add `YAMLGenerator.Feature.LITERAL_BLOCK_STYLE` for String output

### Changes, datatypes

#### [Guava](../../jackson-datatypes-collections)

* [#12](../../jackson-datatypes-collection/issues/12): `Range` deserialization fails when `ObjectMapper` has default typing enabled

### Changes, other modules

#### [Java 8 support](../../jackson-modules-java8)

* [#13](../../jackson-modules-java8/issues/13): (datatype) `configureAbsentsAsNulls` not working for primitive optionals like `OptionalInt`
* [#15](../../jackson-modules-java8/issues/15): (datatype) Optional<Long> and OptionalLong deserialization are not consistent when deserializing from String
* [#17](../../jackson-modules-java8/issues/17): (datatype) Cached `Optional` serializer does not apply annotations for POJO properties
* [#18](../../jackson-modules-java8/issues/18): (datetime) `InstantDeserializer` is not working with offset of zero `+00:00` and `+00`
