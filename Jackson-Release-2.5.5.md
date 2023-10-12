Patch version of [2.5](Jackson Release 2.5), released 07-Dec-2015.
This is the last planned full patch release; it is possible that targeted micro-releases (2.5.5-1 and so on) may be made if critical problems are found.

### Changes, core

#### [Streaming](../../jackson-core)

* [#220](../../jackson-core/issues/220): Problem with `JsonParser.nextFieldName(SerializableString)` for byte-backed parser (affects Afterburner)
* [#221](../../jackson-core/issues/221): Fixed `ArrayIndexOutOfBounds` exception for character-based `JsonGenerator`

#### [Databind](../../jackson-databind)

* [#844](../../jackson-databind/issues/844): Using `@JsonCreator` still causes invalid path references in JsonMappingException
* [#852](../../jackson-databind/issues/852): Accept scientific number notation for quoted numbers too
* [#878](../../jackson-databind/issues/878): `serializeWithType` on `BeanSerializer` does not `setCurrentValue`

### Changes, other modules

#### [Mr Bean](../../jackson-module-mrbean)

* [#25](../../jackson-module-mrbean/25): Should ignore `static` methods (setter/getter)

