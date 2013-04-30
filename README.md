## Jackson Project Home @github

This will the portal page for Jackson Project, at github.
It shall contain links to all active Jackson projects owned by Jackson project team;
as well as additional links to external resources.

## Active Jackson projects by Project team

### Core modules

Core modules are the foundation on which extensions (modules) build upon.
These are three and they are known as:

* [Streaming](../../../jackson-core) ("jackson-core") defines low-level streaming API, and includes JSON-specific implementations
* [Annotations](../../../jackson-annotations) ("jackson-annotations") contains standard Jackson annotations
* [Databind](../../../jackson-databind) ("jackson-databind") implements data-binding (and object serialization) support on `streaming` package; it depends both on `streaming` and `annotations` packages

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

### Third-party datatype modules

### Other


### Older resources

These are obsolete resources, mostly useful for historical interest:

* [Old Jackson project home](http://jackson.codehaus.org)

