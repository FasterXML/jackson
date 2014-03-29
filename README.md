## Jackson Project Home @github

This will the portal page for Jackson Project.
Jackson is a suite of data-processing tools for Java (and JVM platform),
including the flagship [JSON](https://en.wikipedia.org/wiki) parsing and generation library,
as well as additional modules to process data encoded in
[Avro](https://github.com/FasterXML/jackson-dataformat-avro),
[CBOR](https://github.com/FasterXML/jackson-dataformat-cbor),
[CSV](https://github.com/FasterXML/jackson-dataformat-csv),
[Smile](https://github.com/FasterXML/jackson-dataformat-smile),
[XML](https://github.com/FasterXML/jackson-dataformat-xml)
or [YAML](https://github.com/FasterXML/jackson-dataformat-yaml)
(and list of supported format is still growing!)

Portal shall contain links to all active Jackson projects owned by Jackson project team;
as well as additional links to external resources.

## Actively developed versions

Jackson suite has two major branches: 1.x is in maintenance mode, and only bug-fix versions are released; 2.x is the actively developed version. Versions use different Java packages and Maven artifact ids, so they are not mutually compatible, but can peacefully co-exist: a project can depend on both Jackson 1.x and 2.x, without conflicts. This is by design.

The latest stable versions from these branches are:

* 2.3.2, released 01-Mar-2013
* 1.9.13, released 14-Jul-2013

Recommended way to use Jackson is via Maven; releases are made to Central Maven Repository (CMR).
Individual project pages typically contain download links, leading to CMR.

Release notes found from [Jackson Releases](../../wiki/Jackson-Releases) page.

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
* [Hibernate](../../../jackson-datatype-hibernate): support for Hibernate features (lazy-loading, proxies)
* [HPPC](../../../jackson-datatype-hppc): support for [High-Performance Primitive Containers](http://labs.carrotsearch.com/hppc.html) containers
* [Joda](../../../jackson-datatype-joda): support for types of [Joda](http://joda-time.sourceforge.net/) date/time library datatypes
* [org.json](../../../jackson-datatype-json-org): support for "org.json JSON lib" types like `JSONObject`, `JSONArray`
* [JSR-310](../../../jackson-datatype-jsr310): support for "Java 8 Dates" -- experimental, until Java 8 is finalized
* [JSR-353](../../../jackson-datatype-jsr353): support for "Java JSON API" types (specifically, its tree model objects)
* [JDK7](../../../jackson-datatype-jdk7): support for JDK 7 data types not included in previous versions
* [JDK8](../../../jackson-datatype-jdk8): support for JDK 8 data types not included in previous versions

### Providers for JAX-RS

[Jackson JAX-RS Providers](../../../jackson-jaxrs-providers) has handlers to add dataformat
support for JAX-RS implementations (like Jersey, RESTeasy, CXF).
Providers implement `MessageBodyReader` and `MessageBodyWriter`.
Supported formats currently include `JSON`, `Smile`, `XML` and `CBOR`.

### Data format modules

Data format modules offer support for data formats other than JSON.
Most of them simply implement `streaming` API abstractions, so that databinding component can be used as is; some offer (and few require) additional `databind` level functionality for handling things like schemas.

Currently following data format modules are fully usable and supported:

* [CSV](../../../jackson-dataformat-csv): supports [Comma-separated values](http://en.wikipedia.org/wiki/Comma-separated_values) format -- `streaming` api, with optional convenience `databind` additions
* [Smile](../../../jackson-dataformat-smile): supports [Smile (binary JSON)](http://wiki.fasterxml.com/SmileFormatSpec) -- 100% API/logical model compatible via `streaming` API, no changes for `databind`
* [XML](../../../jackson-dataformat-xml): supports XML; provides both `streaming` and `databind` implementations. Similar to JAXB' "code-first" mode (no support for "XML Schema first", but can use JAXB beans)
* [YAML](../../../jackson-dataformat-yaml): supports [YAML](http://en.wikipedia.org/wiki/Yaml), which being similar to JSON is fully supported with simple `streaming` implementation

In addition, following experimental data formats are available:

* [Avro](../../../jackson-dataformat-avro): supports [Avro](http://en.wikipedia.org/wiki/Apache_Avro) data format, with `streaming` implementation plus additional `databind`-level support for Avro Schemas
* [CBOR](../../../jackson-dataformat-cbor): supports [CBOR](http://tools.ietf.org/search/rfc7049) data format; complete implementation, but not yet extensively deployed.
    * note: this module is expected to mature quickly, so that version 2.4 should be production ready.

And in addition there are other data format modules, provided by developers outside Jackson core team:

* [bson4jackson](https://github.com/michel-kraemer/bson4jackson): adds support for [BSON](http://en.wikipedia.org/wiki/BSON) data format (by Mongo project). Basic `streaming` implementation (no `databind` changes) -- external project
* [HOCON](https://github.com/jclawson/jackson-dataformat-hocon): experimental, partial implementation to support [HOCON](https://github.com/typesafehub/config) format -- work in progress

### JVM Language modules

* [Scala](https://github.com/FasterXML/jackson-module-scala) to handle native Scala types (including but not limited to Scala collection/map types, case classes)

### Other, stable

Other fully usable modules by FasterXML team include:

* [Afterburner](https://github.com/FasterXML/jackson-module-afterburner): speed up databinding by 30-40% with bytecode generation to replace use of Reflection
* [JAXB Annotations](https://github.com/FasterXML/jackson-module-jaxb-annotations): allow use of `JAXB` annotations as an alternative (in addition to or instead of) standard Jackson annotations
* [JSON Schema generator](https://github.com/FasterXML/jackson-module-jsonSchema): Generate JSON Schema, based on Jackson POJO introspection, including annotations
* [Mr Bean](https://github.com/FasterXML/jackson-module-mrbean): "type materialization" -- let Mr Bean generate implementation classes on-the-fly (NO source code generation), to avoid monkey code
* [Paranamer](https://github.com/FasterXML/jackson-module-paranamer): tiny extension for automatically figuring out creator (constructor, factory method) parameter names, to avoid having to specify `@JsonProperty`.

### Other, experimental

And finally, there are other still experimental modules provided by FasterXML team

* [Guice](https://github.com/FasterXML/jackson-module-guice): extension that allows injection values from Guice injectors (and basic Guice annotations), instead of standard `@JacksonInject` (or in addition to)
    * note: as of 2.2.x, this is in proof-of-concept stage; should become stable for 2.3

## Documentation

### Web sites

* [jackson-docs](../../../jackson-docs) is our Github Jackson documentation hub
* [Jackson Wiki](http://wiki.fasterxml.com/JacksonHome) contains older documentation (some 1.x specific; but mostly relevant for both 1.x and 2.x)
* [CowTalk](http://cowtowncoder.com/blog/blog.html) -- Blog with lots of Jackson-specific content
* [Jackson Users](http://jackson-users.ning.com) is a Jackson-specific discussion forum

### Note on reporting Bugs

Jackson bugs need to be reported against component they affect: for this reason, issue tracker
is not enabled for this project.
If you are unsure which specific project issue affects, the most likely component
is `jackson-databind`, so you would use
[Jackson Databind Issue Tracker](https://github.com/FasterXML/jackson-databind/issues).

### Paperwork

* Jackson [Contributor License Agreement](../../blob/master/contributor-agreement.pdf) (CLA) is a one-page document we need from every contributor of code (we will request it for pull requests)

### Older resources

These are obsolete resources, mostly useful for historical interest:

* [Old Jackson project home](http://jackson.codehaus.org)

