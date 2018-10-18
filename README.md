## Jackson Project Home @github

This is the home page of the Jackson Project, formerly known as the standard JSON library for Java
(or JVM platform in general), or, as the "best JSON parser for Java."
Or simply as "JSON for Java."
More than that, Jackson is a suite of data-processing tools for Java (and the JVM platform),
including the flagship streaming [JSON](https://en.wikipedia.org/wiki/JSON) parser / generator library,
matching data-binding library (POJOs to and from JSON)
and additional data format modules to process data encoded in
[Avro](https://github.com/FasterXML/jackson-dataformat-avro),
[BSON](https://github.com/michel-kraemer/bson4jackson),
[CBOR](https://github.com/FasterXML/jackson-dataformat-cbor),
[CSV](https://github.com/FasterXML/jackson-dataformat-csv),
[Smile](https://github.com/FasterXML/jackson-dataformat-smile),
[(Java) Properties](https://github.com/FasterXML/jackson-dataformat-properties),
[Protobuf](https://github.com/FasterXML/jackson-dataformat-protobuf),
[XML](https://github.com/FasterXML/jackson-dataformat-xml)
or [YAML](https://github.com/FasterXML/jackson-dataformat-yaml);
and even the large set of data format modules to support data types of widely used
data types such as
[Guava](../../../jackson-datatype-guava),
[Joda](../../../jackson-datatype-joda),
[PCollections](../../../jackson-datatypes-collections)
and many, many more (see below).

While the actual core components live under their own projects -- including the three core packages
([streaming](../../../jackson-core), [databind](../../../jackson-databind), [annotations](../../../jackson-annotations));
data format libraries; data type libraries; [JAX-RS provider](../../../jackson-jaxrs-providers);
and a miscellaneous set of other extension modules -- this project act as the central hub
for linking all the pieces together.

A good companion to this README is the [Jackson Project FAQ](../../wiki/FAQ).

## On reporting issues

First things first: unless you know what you are doing, *DO NOT FILE ISSUES ON THE ISSUE TRACKER OF THIS REPO*.

Instead, do one of the following:

* For Jackson usage questions (for core components or modules), please use the [Jackson-user](https://groups.google.com/forum/#!forum/jackson-user) Google group
    * or, [StackOverflow](http://stackoverflow.com), `#jackson`
* For reporting issues on Jackson implementation, report it against one of the components
    * The most common issue you will have is with [Jackson databind](../../../jackson-databind/issues)
* For suggestions and new ideas, try [Jackson Future Ideas](../../../jackson-future-ideas)

## Actively developed versions

Jackson suite has two major branches: 1.x is in maintenance mode, and only bug-fix versions are released;
2.x is the actively developed version.
These two major versions use different Java packages and Maven artifact ids, so they are not mutually compatible, but can peacefully co-exist: a project can depend on both Jackson 1.x and 2.x, without conflicts.
This is by design and was chosen as the strategy to allow smoother migration from 1.x to 2.x.

The latest stable versions from these branches are:

* [2.9.7](../../wiki/Jackson-Release-2.9), released on 19-Sep-2018.
* [1.9.13](../../wiki/JacksonRelease1.9), released 14-Jul-2013

Recommended way to use Jackson is through Maven repositories; releases are made to Central Maven Repository (CMR).
Individual projects' wiki pages sometimes also contain direct download links, pointing to CMR.

Release notes for 2.x releases are found from [Jackson Releases](../../wiki/Jackson-Releases) page.

## Active Jackson projects

Most projects listed below are lead by Jackson development team; but some by
other at-large Jackson community members.
We try to keep versioning of modules compatible to reduce confusion regarding which versions work together.

### Core modules

Core modules are the foundation on which extensions (modules) build upon.
There are 3 such modules currently (as of Jackson 2.x):

* [Streaming](../../../jackson-core) ([docs](../../../jackson-core/wiki)) ("jackson-core") defines low-level streaming API, and includes JSON-specific implementations
* [Annotations](../../../jackson-annotations) ([docs](../../../jackson-annotations/wiki)) ("jackson-annotations") contains standard Jackson annotations
* [Databind](../../../jackson-databind) ([docs](../../../jackson-databind/wiki)) ("jackson-databind") implements data-binding (and object serialization) support on `streaming` package; it depends both on `streaming` and `annotations` packages

### Third-party datatype modules

These extensions are plug-in Jackson `Module`s (registered with `ObjectMapper.registerModule()`),
and add support for datatypes of various commonly used Java libraries, by adding
serializers and deserializers so that Jackson `databind` package (`ObjectMapper` / `ObjectReader` / `ObjectWriter`) can read and write these types.

Datatype modules directly maintained by Jackson team are under the following Github repositories:

* Standard [Collections](../../../jackson-datatypes-collections) datatype modules:
    * [Guava](../../../jackson-datatypes-collections/tree/master/guava): support for many of [Guava](http://code.google.com/p/guava-libraries/) datatypes.
    * [HPPC](../../../jackson-datatypes-collections/tree/master/hppc): support for [High-Performance Primitive Containers](http://labs.carrotsearch.com/hppc.html) containers
    * [PCollections](../../../jackson-datatypes-collections/tree/master/pcollections): support for [PCollections](http://pcollections.org/) datatypes (NEW in Jackson 2.7!)
* [Hibernate](../../../jackson-datatype-hibernate): support for Hibernate features (lazy-loading, proxies)
* [Joda](../../../jackson-datatype-joda): support for types of [Joda](http://joda-time.sourceforge.net/) date/time library datatypes
* [JDK7](../../../jackson-datatype-jdk7): support for JDK 7 data types not included in previous versions
    * Deprecated in 2.7, as baseline JDK becomes 7, support to be included in `jackson-databind`
* [Java 8 Modules](../../../jackson-modules-java8): support or JDK 8 features and datatypes through 3 separate modules
    * `jackson-module-parameter-names`: Module that adds support for using a new JDK8 feature, ability to access names of constructor and method parameters, to allow omitting `@JsonProperty`.
    * `jackson-datatype-jsr310`: support for "Java 8 Dates" (ones added in JDK 8)
        * Also, for pre-Java8 users can use one of alternate pre-Java8 backports:
            * [joschi/jackson-datatype-threetenbp](https://github.com/joschi/jackson-datatype-threetenbp)
            * [lldata/jackson-datatype-threetenbp](https://github.com/lldata/jackson-datatype-threetenbp)
    * `jackson-datatype-jdk8`: support for JDK 8 data types other than date/time types, including `Optional`
* [JSR-353](../../../jackson-datatype-jsr353): support for "Java JSON API" types (specifically, its tree model objects)
* [org.json](../../../jackson-datatype-json-org): support for "org.json JSON lib" types like `JSONObject`, `JSONArray`

In addition, we are aware of additional modules that are not directly maintained by core Jackson team:

* [jackson-datatype-bolts](https://github.com/v1ctor/jackson-datatype-bolts) support for reading/writing types defined by [Yandex Bolts](https://bitbucket.org/stepancheg/bolts/wiki/Home) collection types (Functional Programming inspired immutable collections)
* [jackson-datatype-commons-lang3](https://github.com/bramp/jackson-datatype-commons-lang3) for types of [Apache Commons Lang v3](https://commons.apache.org/proper/commons-lang/)
* [jackson-datatype-money](https://github.com/zalando/jackson-datatype-money) for "Java Money", see [javax.money](http://javamoney.github.io/api.html)
* [javaslang-jackson](https://github.com/javaslang/javaslang-jackson) for [Javaslang](https://github.com/javaslang/javaslang) support (Feature-rich & self-contained functional programming in Java™ 8 and above)
* [jackson-datatype-json-lib](https://github.com/swquinn/jackson-datatype-json-lib) for supporting types defined by "net.sf.json" library (aka "json-lib")
* [jackson-datatype-jts](https://github.com/bedatadriven/jackson-datatype-jts) (JTS Geometry) for [GeoJSON](https://en.wikipedia.org/wiki/GeoJSON) support
* [jackson-lombok](https://github.com/xebia/jackson-lombok) for better support of [Lombok](http://projectlombok.org/) classes
* [jackson-datatype-mongo](https://github.com/commercehub-oss/jackson-datatype-mongo) for MongoDB types
    * NOTE: there are a few alternatives to handling MongoDB datatypes
* [jackson-module-objectify](https://github.com/tburch/jackson-module-objectify) for datatypes of [Objectify](https://github.com/objectify/objectify)
* [jackson-datatype-protobuf](https://github.com/HubSpot/jackson-datatype-protobuf) for handling datatypes defined by the standard Java protobuf library, developed by [HubSpot](http://www.hubspot.com/)
    * NOTE! This is different from `jackson-dataformat-protobuf` which adds support for encoding/decoding protobuf content but which does NOT depend on standard Java protobuf library
* [TinyTypes](https://github.com/caligin/tinytypes) includes Jackson module (group id `com.github.caligin.tinytypes`, artifact `tinytypes-jackson`)
* [jackson-datatype-vertx](https://github.com/Crunc/jackson-datatype-vertx) for reading/writing [Vert.x](http://vertx.io/) `org.vertx.java.core.json.JsonObject` objects (repackaged `org.json` node types)

### Providers for JAX-RS

[Jackson JAX-RS Providers](../../../jackson-jaxrs-providers) has handlers to add dataformat
support for JAX-RS implementations (like Jersey, RESTeasy, CXF).
Providers implement `MessageBodyReader` and `MessageBodyWriter`.
Supported formats currently include `JSON`, `Smile`, `XML`, `YAML` and `CBOR`.

### Data format modules

Data format modules offer support for data formats other than JSON.
Most of them simply implement `streaming` API abstractions, so that databinding component can be used as is; some offer (and few require) additional `databind` level functionality for handling things like schemas.

Currently following data format modules are fully usable and supported (version number in parenthesis, if included, is the
first Jackson 2.x version to include module; if missing, included from 2.0)

* [Avro](../../../jackson-dataformat-avro): supports [Avro](http://en.wikipedia.org/wiki/Apache_Avro) data format, with `streaming` implementation plus additional `databind`-level support for Avro Schemas
* [CBOR](../../../jackson-dataformat-cbor): supports [CBOR](http://tools.ietf.org/search/rfc7049) data format (a binary JSON variant).
* [CSV](../../../jackson-dataformat-csv): supports [Comma-separated values](http://en.wikipedia.org/wiki/Comma-separated_values) format -- `streaming` api, with optional convenience `databind` additions
* [Ion](../../../jackson-dataformats-binary/tree/master/ion) (NEW with Jackson 2.9!): support for [Amazon Ion](https://amznlabs.github.io/ion-docs/) binary data format (similar to CBOR, Smile, i.e. binary JSON - like)
* [(Java) Properties](../../../jackson-dataformat-properties) (2.8): creating nested structure out of implied notation (dotted by default, configurable), flattening similarly on serialization
* [Protobuf](../../../jackson-dataformat-protobuf) (2.6): supported similar to `Avro`
* [Smile](../../../jackson-dataformat-smile): supports [Smile (binary JSON)](https://github.com/FasterXML/smile-format-specification) -- 100% API/logical model compatible via `streaming` API, no changes for `databind`
* [XML](../../../jackson-dataformat-xml): supports XML; provides both `streaming` and `databind` implementations. Similar to JAXB' "code-first" mode (no support for "XML Schema first", but can use JAXB beans)
* [YAML](../../../jackson-dataformat-yaml): supports [YAML](http://en.wikipedia.org/wiki/Yaml), which being similar to JSON is fully supported with simple `streaming` implementation


There are also other data format modules, provided by developers outside Jackson core team:

* [BEncode](https://github.com/zsoltm/jackson-dataformat-bencode): support for reading/writing [BEncode](https://en.wikipedia.org/wiki/Bencode) (BitTorrent format) encoded data
* [bson4jackson](https://github.com/michel-kraemer/bson4jackson): adds support for [BSON](http://en.wikipedia.org/wiki/BSON) data format (by Mongo project).
    * Implemented as full streaming implementation, which allows full access (streaming, data-binding, tree-model)
    * Also see [MongoJack] library below; while not a dataformat module, it allows access to BSON data as well.
* [EXIficient](https://github.com/EXIficient/exificient-for-json) supports [Efficient XML Interchange](https://en.wikipedia.org/wiki/Efficient_XML_Interchange)
* [jackson-dataformat-msgpack](https://github.com/msgpack/msgpack-java/tree/develop/msgpack-jackson) adds support [MessagePack](http://en.wikipedia.org/wiki/MessagePack) (aka `MsgPack`) format
    * Implemented as full streaming implementation, which allows full access (streaming, data-binding, tree-model)
* [HOCON](https://github.com/jclawson/jackson-dataformat-hocon): experimental, partial implementation to support [HOCON](https://github.com/typesafehub/config) format -- work in progress
* [Rison](https://github.com/Hronom/jackson-dataformat-rison): Jackson backend to support [Rison](https://github.com/Nanonid/rison))

### JVM Language modules

* [Kotlin](https://github.com/FasterXML/jackson-module-kotlin) to handle native types of [Kotlin](http://kotlinlang.org/)
* [Scala](https://github.com/FasterXML/jackson-module-scala) to handle native Scala types (including but not limited to Scala collection/map types, case classes)
    * Currently (July 2017) Scala 2.10, 2.11 and 2.12 are supported (2.9 was supported up to Jackson 2.3)

### Support for Schemas

Jackson annotations define intended properties and expected handling for POJOs, and in addition to Jackson itself
using this for reading/writing JSON and other formats, it also allows generation of external schemas.
Some of this functionality is included in above-mentioned data-format extensions; but there are also
many stand-alone Schema tools, such as:

#### JSON Schema

* [Ant Task for JSON Schema Generation](https://github.com/xdarklight/jackson-jsonschema-ant-task): Generate JSON Schemas from your Java classes with Apache Ant using the Jackson library and extension modules.
* [JSON Schema generator module](../../../jackson-module-jsonSchema): programmatically generate JSON Schema, based on Jackson POJO introspection, including annotations
* [Maven plug-in](../../../jackson-schema-maven-plugin) for JSON Schema generation (based on JSON Schema module)

#### Other schema languages

* [Ember Schema Generator](../../../../marcus-nl/ember-schema-generator): Generate schemas for [Ember.js](https://github.com/emberjs/ember.js)

### Other modules, stable

Other fully usable modules by FasterXML team include:

* [Base](../../../jackson-modules-base) modules:
    * [Afterburner](../../../jackson-modules-base/tree/master/afterburner): speed up databinding by 30-40% with bytecode generation to replace use of Reflection for field access, method/constructor calls
    * [Guice](../../../jackson-modules-base/tree/master/guice): extension that allows injection values from Guice injectors (and basic Guice annotations), instead of standard `@JacksonInject` (or in addition to)
    * [JAXB Annotations](../../../jackson-modules-base/tree/master/jackson-module-jaxb-annotations): allow use of `JAXB` annotations as an alternative (in addition to or instead of) standard Jackson annotations
    * [Mr Bean](../../../jackson-modules-base/tree/master/mrbean): "type materialization" -- let Mr Bean generate implementation classes on-the-fly (NO source code generation), to avoid monkey code
    * [OSGi](../../../jackson-modules-base/tree/master/osgi): allows injection of values from OSGi registry, via standard Jackson `@JacksonInject` annotation
    * [Paranamer](../../../jackson-modules-base/tree/master/paranamer): tiny extension for automatically figuring out creator (constructor, factory method) parameter names, to avoid having to specify `@JsonProperty`.

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

* [Smile Format Discussion](https://groups.google.com/forum/#!forum/smile-format-discussion): List for discussing details of the binary JSON format called [Smile](https://en.wikipedia.org/wiki/Smile_%28data_interchange_format%29) (see [Smile Specification](https://github.com/FasterXML/smile-format-specification))
* [Jackson Users](http://jackson-users.ning.com) is a Jackson-specific discussion forum for usage questions.

### Other things related to or inspired by Jackson

* [Pyckson](https://github.com/antidot/Pyckson) is a Python library that aims for same goals as Java Jackson, such as Convention over Configuration
* [Rackson](https://github.com/griffindy/rackson) is a Ruby library that offers Jackson-like functionality on Ruby platform

## Documentation

### Web sites

* [jackson-docs](../../../jackson-docs) is our Github Jackson documentation hub
* [Jackson Wiki](https://github.com/FasterXML/jackson/wiki/) contains older documentation (some 1.x specific; but mostly relevant for both 1.x and 2.x)
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
    * [Corporate CLA](../../blob/master/contributor-agreement-corporate.txt) is used by Corporations to avoid individual employees from having to send separate CLAs; it is also favored by corporate IP lawyers.

Note that the first option is available for corporations as well, but most companies have opted to use the second option instead. Core team has no preference over which one gets used; both work; we care more about actual contributions.

### Java JSON library comparisons

Since you probably want opinions by Java developers NOT related to Jackson project, regarding which library to use,
here are links to some of existing independent comparisons:

* [Top 7 Open-Source JSON-binding providers](http://www.developer.com/lang/jscript/top-7-open-source-json-binding-providers-available-today.html) (April 2014)
* [Be a Lazy but a Productive Android Developer, Part 3: JSON Parsing Library](http://java.dzone.com/articles/be-lazy-productive-android) (April 2014)
* ["Can anyone recommend a good Java JSON library"](https://www.linkedin.com/groups/Can-anyone-recommend-good-Java-50472.S.226644043) (Linked-In group) (March 2013)
* ["Which JSON library to use on Android?"](http://thetarah.com/2012/09/21/which-json-library-should-i-use-in-my-android-and-java-projects/) (September 2012) 
