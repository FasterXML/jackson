Patch version of [2.13](Jackson-Release-2.13), being released on March 6, 2022.
(2.13.1 was released on 19-Dec-2021).

Following fixes are included in this patch release.

### Changes, core

#### [BOM/Base](../../jackson-bom)

* [#46](../../jackson-bom/issues/46): `module-info.java` is in `META-INF/versions/11` instead of `META-INF/versions/9`

#### [Streaming](../../jackson-core)

* [#732](../../jackson-core/issues/732): Update Maven wrapper
* [#739](../../jackson-core/issues/739): `JsonLocation` in 2.13 only uses identity comparison for "content reference"

#### [Databind](../../jackson-databind)

* [#3293](../../jackson-databin/issues/3293): Use Method.getParameterCount() where possible
* [#3344](../../jackson-databin/issues/3344): `Set.of()` (Java 9) cannot be deserialized with polymorphic handling
* [#3368](../../jackson-databin/issues/3368): `SnakeCaseStrategy` causes unexpected `MismatchedInputException` during deserialization
* [#3369](../../jackson-databin/issues/3369): Deserialization ignores other Object fields when Object or Array value used for enum
* [#3380](../../jackson-databin/issues/3380): `module-info.java` is in `META-INF/versions/11` instead of `META-INF/versions/9`

### Changes, [JAX-RS providers](../../jackson-jakarta-providers)

* [#161](../../jackson-jakarta-providers/issues/161): Module name in `jakarta-xmlbind/src/moditect/module-info.java` is invalid

### Changes, data formats

#### CSV

* [#303](../../jackson-dataformats-text/issues/308): `CsvMapper.typedSchemaForWithView()` with `DEFAULT_VIEW_INCLUSION`

#### YAML

* [#303](../../jackson-dataformats-text/issues/303): Update to SnakeYAML 1.30
* [#306](../../jackson-dataformats-text/issues/306): Error when generating/serializing keys with multilines and colon

### Changes, JVM Languages

#### [Scala](../../jackson-module-scala)

* [#566](../../jackson-module-scala/issues/566): when paranamer fails, fail over to using java reflection

### Changes, Other modules

#### Blackbird

* [#138](../../jackson-modules-base/issues/138): Blackbird doesn't work on Java 15+

#### Jakarta XMLBind Annotations

* [#152](../../jackson-modules-base/issues/152): jakarta.activation version conflict in 2.13

#### [JSON Schema](../../jackson-module-jsonSchema)

* [#146](../../jackson-module-jsonSchema/issues/146): `JsonSchema.equals()` implementation wrong
