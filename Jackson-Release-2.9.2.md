Patch version of [2.9](Jackson-Release-2.9), released on 14-Oct-2017.

Following fixes are included.

### Compatibility

Despite attempts to avoid regressions, there is an open issue for potential regression since 2.9.1:

* [Java 8 module / #67](https://github.com/FasterXML/jackson-modules-java8/issues/67) -- it looks like there are issues with combination of `PropertyNamingStrategy`, Creator properties, and Java 8 constructor names

### Changes, core

#### [Streaming](../../jackson-core)

#### [Databind](../../jackson-databind)

* [#1705](../../jackson-databind/issues/1705): Non-generic interface method hides type resolution info from generic base class
    * NOTE: was assumed to be fixed in `2.9.1`, but due to a mistake wasn't.
* [#1767](../../jackson-databind/issues/1767): Allow `DeserializationProblemHandler` to respond to primitive types
* [#1768](../../jackson-databind/issues/1768): Improve `TypeFactory.constructFromCanonical()` to work with `java.lang.reflect.Type.getTypeName()` format
* [#1771](../../jackson-databind/issues/1771): Pass missing argument for string formatting in `ObjectMapper`
* [#1788](../../jackson-databind/issues/1788): `StdDateFormat._parseAsISO8601()` does not parse "fractional" timezone correctly
* [#1793](../../jackson-databind/issues/1793): `java.lang.NullPointerException` in `ObjectArraySerializer.acceptJsonFormatVisitor()` for array value with `@JsonValue`

### Changes, dataformats

#### Binary formats

* [#102](../../jackson-dataformats-binary/issues/102): (avro) Incorrect deserialization of `long` with new `AvroFactory`

### Changes, other modules

#### [Afterburner](../../jackson-modules-base/tree/master/afterburner)

* [#30](../../jackson-modules-base/issues/30): (afterburner) `IncompatibleClassChangeError` deserializing interface methods with default impl
