## Jackson Project Home @github

This will the portal page for Jackson Project, at github.
It shall contain links to all active Jackson projects owned by Jackson project team;
as well as additional links to external resources.

## Active Jackson projects

Most projects listed below are lead by Jackson development team; but some by
other at-large Jackson community members.
We try to keep versioning of modules compatible to reduce confusion regarding which versions work together.

### Core modules

Core modules are the foundation on which extensions (modules) build upon.
These are three and they are known as:

* [Streaming](../../../jackson-core) ([docs](../../../jackson-core/wiki)) ("jackson-core") defines low-level streaming API, and includes JSON-specific implementations
* [Annotations](../../../jackson-annotations) ([docs](../../../jackson-annotations/wiki)) ("jackson-annotations") contains standard Jackson annotations
* [Databind](../../../jackson-databind) ([docs](../../../jackson-databind/wiki/Documentation)) ("jackson-databind") implements data-binding (and object serialization) support on `streaming` package; it depends both on `streaming` and `annotations` packages

### Third-party datatype modules

These extensions are plug-in Jackson `Module`s (registered with `ObjectMapper.registerModule()`),
and add support for datatypes of various commonly used Java libraries, by adding
serializers and deserializers so that Jackson `databind` package (`ObjectMapper` / `ObjectReader` / `ObjectWriter`) can read and write these types.

* [Guava](../../../jackson-datatype-guava): support for many of [Guava](http://code.google.com/p/guava-libraries/) datatypes.
* [Hibernate](../../../jackson-module-hibernate): support for Hibernate features (lazy-loading, proxies)
* [HPPC](../../../jackson-datatype-hppc): support for [High-Performance Primitive Containers](http://labs.carrotsearch.com/hppc.html) containers
* [Joda](../../../jackson-datatype-joda): support for types of [Joda](http://joda-time.sourceforge.net/) date/time library datatypes
* [org.json](../../../jackson-datatype-json-org): support for "org.json JSON lib" types like `JSONObject`, `JSONArray`
* [JSR-310](../../../jackson-datatype-jsr310): support for "Java 8 Dates" -- experimental, until Java 8 is finalized
* [JSR-353](../../../jackson-datatype-jsr353): support for "Java JSON API" types (specifically, its tree model objects)

### Providers for JAX-RS

[Jackson JAX-RS Providers](../../../jackson-jaxrs-providers) has handlers to add dataformat
support for JAX-RS implementations (like Jersey, RESTeasy, CXF).
Providers implement `MessageBodyReader` and `MessageBodyWriter`.

### Data format modules

Data format modules offer support for data formats other than JSON.
Most of them simply implement `streaming` API abstractions, so that databinding component can be used as is; some offer (and few require) additional `databind` level functionality for handling things like schemas.

Currently usable data format modules by Jackson team are:

* [CSV](../../../jackson-dataformat-xml): supports [Comma-separated values](http://en.wikipedia.org/wiki/Comma-separated_values) format -- `streaming` api, with optional convenience `databind` additions
* [XML](../../../jackson-dataformat-xml): supports XML; provides both `streaming` and `databind` implementations. Similar to JAXB' "code-first" mode (no support for "XML Schema first", but can use JAXB beans)
* [YAML](../../../jackson-dataformat-xml): supports [YAML](http://en.wikipedia.org/wiki/Yaml), which being similar to JSON is fully supported with simple `streaming` implementation
* [Smile](../../../jackson-dataformat-xml): supports [Smile (binary JSON)](http://wiki.fasterxml.com/SmileFormatSpec) -- 100% API/logical model compatible via `streaming` API, no changes for `databind`
* [Avro](../../../jackson-dataformat-xml): supports [Avro](http://en.wikipedia.org/wiki/Apache_Avro) data format, with `streaming` implementation plus additional `databind`-level support for Avro Schemas

In addition, there are other experimental and external data format implementations available:

* [bson4jackson](https://github.com/michel-kraemer/bson4jackson): adds support for [BSON](http://en.wikipedia.org/wiki/BSON) data format (by Mongo project). Basic `streaming` implementation (no `databind` changes) -- external project

### JVM Language modules

* [Scala](https://github.com/FasterXML/jackson-module-scala) to handle native Scala types (including but not limited to Scala collection/map types, case classes)

### Other

* [Afterburner](https://github.com/FasterXML/jackson-module-afterburner): speed up databinding by 30-40% with bytecode generation to replace use of Reflection
* [JAXB Annotations](https://github.com/FasterXML/jackson-module-jaxb-annotations): allow use of `JAXB` annotations as an alternative (in addition to or instead of) standard Jackson annotations
* [Mr Bean](https://github.com/FasterXML/jackson-module-mrbean): "type materialization" -- let Mr Bean generate implementation classes on-the-fly (NO source code generation), to avoid monkey code
* [Paranamer](https://github.com/FasterXML/jackson-module-paranamer): tiny extension for automatically figuring out creator (constructor, factory method) parameter names, to avoid having to specify `@JsonProperty`.
* [JSON Schema generator](https://github.com/FasterXML/jackson-module-jsonSchema): Generate JSON Schema, based on Jackson POJO introspection, including annotations

## Documentation

### Web sites

* [jackson-docs](../../../jackson-docs) is our Github Jackson documentation hub
* [Jackson Wiki](wiki.fasterxml.com/JacksonHome) contains older documentation (some 1.x specific; but mostly relevant for both 1.x and 2.x)
* [CowTalk](http://cowtowncoder.com/blog/blog.html) -- Blog with lots of Jackson-specific content
* [Jackson Users](http://jackson-users.ning.com) is a Jackson-specific discussion forum

### Older resources

These are obsolete resources, mostly useful for historical interest:

* [Old Jackson project home](http://jackson.codehaus.org)

