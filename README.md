## Jackson Project Home @github

This is the Home Page of Jackson Project; formerly known as the standard JSON library for Java
(or JVM platform in general), or, as the "best JSON parser for Java". Or simply as "JSON for Java".

But more than that, Jackson is a suite of data-processing tools for Java (and JVM platform),
including the flagship streaming [JSON](https://en.wikipedia.org/wiki/JSON) parser / generator library,
matching data-binding library (POJOs to and from JSON)
and additional data format modules to process data encoded in
[Avro](https://github.com/FasterXML/jackson-dataformat-avro),
[CBOR](https://github.com/FasterXML/jackson-dataformat-cbor),
[CSV](https://github.com/FasterXML/jackson-dataformat-csv),
[Smile](https://github.com/FasterXML/jackson-dataformat-smile),
[XML](https://github.com/FasterXML/jackson-dataformat-xml)
or [YAML](https://github.com/FasterXML/jackson-dataformat-yaml);
and even the large set of data format modules to support data types of widely used
data types such as [Joda](../../../jackson-dataformat-joda), [Guava](../../../jackson-dataformat-guava) and
many more.

While the actual core components live under their own projects -- including the 3 core packages
([streaming](../../../jackson-core), [databind](../../../jackson-databind), [annotations](../../../jackson-annotations),
data format libraries, data type libraries, [JAX-RS provider](../../../jackson-jaxrs-providers),
and miscellaneous set of other extension modules -- this project act as the central hub
for linking all the pieces together.

## Actively developed versions

Jackson suite has two major branches: 1.x is in maintenance mode, and only bug-fix versions are released;
2.x is the actively developed version.
These two major versions use different Java packages and Maven artifact ids, so they are not mutually compatible, but can peacefully co-exist: a project can depend on both Jackson 1.x and 2.x, without conflicts.
This is by design and was chosen as the strategy to allow smoother migration from 1.x to 2.x.

The latest stable versions from these branches are:

* [2.5.3](../../wiki/Jackson-Release-2.5.3), released 24-Mar-2015
* [1.9.13](wiki.fasterxml.com/JacksonRelease19), released 14-Jul-2013

Recommended way to use Jackson is through Maven repositories; releases are made to Central Maven Repository (CMR).
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
* [Databind](../../../jackson-databind) ([docs](../../../jackson-databind/wiki)) ("jackson-databind") implements data-binding (and object serialization) support on `streaming` package; it depends both on `streaming` and `annotations` packages

### Third-party datatype modules

These extensions are plug-in Jackson `Module`s (registered with `ObjectMapper.registerModule()`),
and add support for datatypes of various commonly used Java libraries, by adding
serializers and deserializers so that Jackson `databind` package (`ObjectMapper` / `ObjectReader` / `ObjectWriter`) can read and write these types.

* [Guava](../../../jackson-datatype-guava): support for many of [Guava](http://code.google.com/p/guava-libraries/) datatypes.
* [Hibernate](../../../jackson-datatype-hibernate): support for Hibernate features (lazy-loading, proxies)
* [HPPC](../../../jackson-datatype-hppc): support for [High-Performance Primitive Containers](http://labs.carrotsearch.com/hppc.html) containers
* [Joda](../../../jackson-datatype-joda): support for types of [Joda](http://joda-time.sourceforge.net/) date/time library datatypes
* [JDK7](../../../jackson-datatype-jdk7): support for JDK 7 data types not included in previous versions
* [JDK8](../../../jackson-datatype-jdk8): support for JDK 8 data types not included in previous versions, including `Optional` (but excluding new Date types which are in JSR-310 module above)
* [JSR-310](../../../jackson-datatype-jsr310): support for "Java 8 Dates" (ones added in JDK 8)
    * Also, for pre-Java8 users can use ["ThreeTen"](https://github.com/lldata/jackson-datatype-threetenbp) module for backport on Java7
* [JSR-353](../../../jackson-datatype-jsr353): support for "Java JSON API" types (specifically, its tree model objects)
* [org.json](../../../jackson-datatype-json-org): support for "org.json JSON lib" types like `JSONObject`, `JSONArray`
* [Yandex Bolts](https://github.com/v1ctor/jackson-datatype-bolts) support for reading/writing types defined by [Yandex Bolts](https://bitbucket.org/stepancheg/bolts/wiki/Home) collection types (Functional Programming inspired immutable collections)

### Providers for JAX-RS

[Jackson JAX-RS Providers](../../../jackson-jaxrs-providers) has handlers to add dataformat
support for JAX-RS implementations (like Jersey, RESTeasy, CXF).
Providers implement `MessageBodyReader` and `MessageBodyWriter`.
Supported formats currently include `JSON`, `Smile`, `XML` and `CBOR`.

### Data format modules

Data format modules offer support for data formats other than JSON.
Most of them simply implement `streaming` API abstractions, so that databinding component can be used as is; some offer (and few require) additional `databind` level functionality for handling things like schemas.

Currently following data format modules are fully usable and supported:

* [Avro](../../../jackson-dataformat-avro): supports [Avro](http://en.wikipedia.org/wiki/Apache_Avro) data format, with `streaming` implementation plus additional `databind`-level support for Avro Schemas
* [CBOR](../../../jackson-dataformat-cbor): supports [CBOR](http://tools.ietf.org/search/rfc7049) data format (a binary JSON variant).
* [CSV](../../../jackson-dataformat-csv): supports [Comma-separated values](http://en.wikipedia.org/wiki/Comma-separated_values) format -- `streaming` api, with optional convenience `databind` additions
* [Smile](../../../jackson-dataformat-smile): supports [Smile (binary JSON)](http://wiki.fasterxml.com/SmileFormatSpec) -- 100% API/logical model compatible via `streaming` API, no changes for `databind`
* [XML](../../../jackson-dataformat-xml): supports XML; provides both `streaming` and `databind` implementations. Similar to JAXB' "code-first" mode (no support for "XML Schema first", but can use JAXB beans)
* [YAML](../../../jackson-dataformat-yaml): supports [YAML](http://en.wikipedia.org/wiki/Yaml), which being similar to JSON is fully supported with simple `streaming` implementation

In addition, following modules are being developed, but not yet complete:

* [Protobuf](../../../jackson-dataformat-protobuf): will eventually be supported similar to `Avro`. Current state as of June 2015 is:
    * `protoc` (schema) handling works, using Square's [Protoparser](https://github.com/square/protoparser) library
    * Serialization works, and is very fast (similar to the official Google Java protobuf project)
    * Deserialization also works, speed reasonable (faster than JSON, similar to Smile)

There are also other data format modules, provided by developers outside Jackson core team:

* [bson4jackson](https://github.com/michel-kraemer/bson4jackson): adds support for [BSON](http://en.wikipedia.org/wiki/BSON) data format (by Mongo project).
    * Implemented as full streaming implementation, which allows full access (streaming, data-binding, tree-model)
    * Also see [MongoJack] library below; while not a dataformat module, it allows access to BSON data as well.
* [MessagePack](https://github.com/komamitsu/jackson-dataformat-msgpack): adds [MessagePack](http://en.wikipedia.org/wiki/MessagePack) (aka `MsgPack`) support
    * Implemented as full streaming implementation, which allows full access (streaming, data-binding, tree-model)
* [HOCON](https://github.com/jclawson/jackson-dataformat-hocon): experimental, partial implementation to support [HOCON](https://github.com/typesafehub/config) format -- work in progress

### JVM Language modules

* [Kotlin](https://github.com/FasterXML/jackson-module-kotlin) to handle native types of [Kotlin](http://kotlinlang.org/) (NOTE: 2.4.3 the first official release)
* [Scala](https://github.com/FasterXML/jackson-module-scala) to handle native Scala types (including but not limited to Scala collection/map types, case classes)

### Support for Schemas

Jackson annotations define intended properties and expected handling for POJOs, and in addition to Jackson itself
using this for reading/writing JSON and other formats, it also allows generation of external schemas.
Some of this functionality is included in above-mentioned data-format extensions; but there are also
many stand-alone Schema tools, such as:

* [Ember Schema Generator](../../../../marcus-nl/ember-schema-generator): Generate schemas for [Ember.js](https://github.com/emberjs/ember.js)
* [JSON Schema generator](../../../jackson-module-jsonSchema): Generate JSON Schema, based on Jackson POJO introspection, including annotations
* [Maven plug-in](../../../jackson-schema-maven-plugin) for JSON Schema generation (based on JSON Schema module)

### Other modules, stable

Other fully usable modules by FasterXML team include:

* [Afterburner](../../../jackson-module-afterburner): speed up databinding by 30-40% with bytecode generation to replace use of Reflection
* [JAXB Annotations](../../../jackson-module-jaxb-annotations): allow use of `JAXB` annotations as an alternative (in addition to or instead of) standard Jackson annotations
* [Mr Bean](../../../jackson-module-mrbean): "type materialization" -- let Mr Bean generate implementation classes on-the-fly (NO source code generation), to avoid monkey code
* [Paranamer](../../../jackson-module-paranamer): tiny extension for automatically figuring out creator (constructor, factory method) parameter names, to avoid having to specify `@JsonProperty`.

### Other modules, experimental

And finally, there are other still experimental modules provided by FasterXML team

* [Guice](../../../jackson-module-guice): extension that allows injection values from Guice injectors (and basic Guice annotations), instead of standard `@JacksonInject` (or in addition to)
    * note: as of 2.2.x, this is in proof-of-concept stage; should become stable for 2.3
* [JDK 8 Parameter names](../../../jackson-module-parameter-names): Module that adds support for using a new JDK8 feature: ability to access names of constructor and method parameters.

### Jackson jr

While [Jackson databind](../../../jackson-databind) is a good choice for general-purpose data-binding, its
footprint and startup overhead may be problematic in some domains, such as mobile phones; and especially
for light usage (couple of reads or writes). In addition, some developers find full Jackson API overwhelming.

For all these reasons, we decided to create a much simpler, smaller library, which supports a subset of
functionality, called [Jackson jr](../../../jackson-jr).
It builds on [Jackson Streaming API](../../../jackson-core), but does not depend on databind. As a result
its size (both jar, and runtime memory usage) is considerably smaller; and its API is very compact.

### Third-party non-module libraries based on Jackson

#### Jackson helper libraries

* [Jackson Ant path filter](https://github.com/Antibrumm/jackson-antpathfilter) adds powerful filtering of properties to serialize, using Ant Path notation for hierarchic filtering

#### Support for datatypes

* [MongoJack](http://mongojack.org/) supports efficient handling of [BSON](http://en.wikipedia.org/wiki/BSON) encoded data store in [MongoDB](http://en.wikipedia.org/wiki/MongoDB).

## Participation

The easiest ways to participate beyond using Jackson is to join one of Jackson mailing lists (Jackson google groups):

* [Jackson Announce](https://groups.google.com/forum/#!forum/jackson-announce): Announcement-only list for new Jackson releases, meetups and other events related to Jackson
* [Jackson User](https://groups.google.com/forum/#!forum/jackson-user): List dedicated for discussion on Jackson usage
* [Jackson Dev](https://groups.google.com/forum/#!forum/jackson-dev): List for developers of Jackson core components and modules, discussing implementation details, API changes.

There are other related lists and forums as well:

* [Smile Format Discussion](https://groups.google.com/forum/#!forum/smile-format-discussion): List for discussing details of the binary JSON format called [Smile](https://en.wikipedia.org/wiki/Smile_%28data_interchange_format%29) (see [Smile Specification](http://wiki.fasterxml.com/SmileFormat))
* [Jackson Users](http://jackson-users.ning.com) is a Jackson-specific discussion forum for usage questions.

## Documentation

### Web sites

* [jackson-docs](../../../jackson-docs) is our Github Jackson documentation hub
* [Jackson Wiki](http://wiki.fasterxml.com/JacksonHome) contains older documentation (some 1.x specific; but mostly relevant for both 1.x and 2.x)
* [CowTalk](http://cowtowncoder.com/blog/blog.html) -- Blog with lots of Jackson-specific content

### Note on reporting Bugs

Jackson bugs need to be reported against component they affect: for this reason, issue tracker
is not enabled for this project.
If you are unsure which specific project issue affects, the most likely component
is `jackson-databind`, so you would use
[Jackson Databind Issue Tracker](https://github.com/FasterXML/jackson-databind/issues).

### Paperwork

* Contributor License Agreement, needed by core team to accept contributions. There are 2 options:
    * Standard Jackson [Contributor License Agreement](../../blob/master/contributor-agreement.pdf) (CLA) is a one-page document we need from every contributor of code (we will request it for pull requests), used mostly by individual contributors
    * [Corporate CLA](../../blob/contributor-agreement-corporate.txt) is used by Corporations to avoid individual employees from having to send separate CLAs; it is also favored by corporate IP lawyers.

Note that the first option is available for corporations as well, but most companies have opted to use the second option instead. Core team has no preference over which one gets used; both work; we care more about actual contributions.

### Java JSON library comparisons

Since you probably want opinions by Java developers NOT related to Jackson project, regarding which library to use,
here are links to some of existing independent comparisons:

* [Top 7 Open-Source JSON-binding providers](http://www.developer.com/lang/jscript/top-7-open-source-json-binding-providers-available-today.html) (April 2014)
* [Be a Lazy but a Productive Android Developer, Part 3: JSON Parsing Library](http://java.dzone.com/articles/be-lazy-productive-android) (April 2014)
* ["Can anyone recommend a good Java JSON library"](https://www.linkedin.com/groups/Can-anyone-recommend-good-Java-50472.S.226644043) (Linked-In group) (March 2013)
* ["Which JSON library to use on Android?"](http://thetarah.com/2012/09/21/which-json-library-should-i-use-in-my-android-and-java-projects/) (September 2012) 

### Older resources

These are obsolete resources, mostly useful for historical interest:

- NONE -
