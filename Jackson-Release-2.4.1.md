Patch released 16-Jun-2014

### Changes, core

#### [Streaming API](../../jackson-core)

* [#143](../../jackson-core/issues/143): Flaw in `BufferRecycler.allocByteBuffer(int,int)` that results in performance regression (at least for CBOR format module).

#### [Core Databind](../../jackson-databind)

* [#479](../../jackson-databind/issues/479): NPE on trying to deserialize a `String[]` from JSON Array that contains a `null` value (regression from 2.3)

### Changes, data formats

#### [Avro](../../jackson-dataformat-avro)

* [#8](../../jackson-dataformat-avro/issues/8): Error in creating Avro Schema for `java.util.Date` (and related) type.

#### [CBOR](../../jackson-dataformat-cbor)

* Change `#143` (see above) for `jackson-core` lead to sub-par performance: with fix in 2.4.1, write-performance throughput up to 40% higher for small/medium-sized documents.


#### [Smile](../../jackson-dataformat-smile)

* [#17](../../jackson-dataformat-smile/issues/17): `ArrayIndexOutOfBounds` for large data with `float` values

#### [XML](../../jackson-dataformat-xml)

* [#117](../../jackson-dataformat-xml/issues/117): `@JsonAnyGetter` + `@JsonTypeInfo` combination prevents serialization of properties as elements

### Changes, other modules

#### [JSON Schema](../../jackson-module-jsonSchema)

* [#4](../../jackson-module-jsonSchema): JSON schema generation with Jackson goes into infinite loop
