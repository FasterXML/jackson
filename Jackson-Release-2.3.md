Version 2.3[.0] was released in November, 2013. It is a "minor" Jackson Releases following 2.2, meaning that it adds new functionality but be backwards compatible with earlier 2.x releases.

## Status

Branch is closed for all new releases (including micro-patches)

### Patches

Beyond initial 2.3.0 (described here), following patch releases have been made.

* [2.3.1](Jackson-Release-2.3.1) (28-Dec-2013)
* [2.3.2](Jackson-Release-2.3.2) (01-Mar-2014)
* [2.3.3](Jackson-Release-2.3.3) (10-Apr-2014)
* [2.3.4](Jackson-Release-2.3.4) (17-Jul-2014)
* [2.3.5](Jackson-Release-2.3.5) (13-Jan-2015)
* [2.3.6](Jackson-Release-2.3.6): not released as of December 2015.

Beyond latest release, following critical micro-patches have also been released

* `jackson-dataformat-smile` 2.3.5.1 (15-Jul-2015), with critical fixes:
    * [#25](https://github.com/FasterXML/jackson-dataformat-smile/pull/25): Buffer overflow when writing unescaped binary content

### Notes on maintenance

This branch is special in 2.x hierarchy in that it is planned to be long-living branch (similar to 1.9), and stay open even when 2.4 and later minor versions are released. This is because it is:

* Last version to support older Android versions (it specifically omits use of certain methods from `java.util.Arrays`)
* Last version to support Scala 2.9 (2.4 only supports 2.10 and above)

### Changes: compatibility

While initial decision had been made to require JDK 1.6 as the baseline, team decided to make an exception for version `2.3.2`, so that old Android versions could use it -- basically this means that some changes were reverted for just that version. 2.4 will fully require JDK 1.6.

### Changes: packaging

Another packaging change was to reduce amount of debug information for core (streaming, annotations, databind) packages. While this did result in significant reduction in jar sizes (20-25% smaller), it did lead to problems with developers: 2.4 will revert to full debug information for the default jars, and we will try to figure out a way to offer alternative 'minimal' packages with no debug information.

### Changes, core

#### [Core Annotations](../../jackson-annotations)

* [#13](https://github.com/FasterXML/jackson-annotations/issues/13): Add `@JsonPropertyDescription` (mostly to support descriptions in JSON Schema)

#### [Core Streaming](../../jackson-core)

* [#8](https://github.com/FasterXML/jackson-core/issues/8) Add methods in `JsonParser`/`JsonGenerator` for reading/writing "native" Object Ids (initially, to support YAML anchors, aliases)
* [#47](https://github.com/FasterXML/jackson-core/issues/47): Support YAML-style comments with `JsonParser.Feature.ALLOW_YAML_COMMENTS`
* [#60](https://github.com/FasterXML/jackson-core/issues/60): Add feature `JsonParser.Feature.STRICT_DUPLICATE_DETECTION` to allow strict verification of uniqueness of property names during streaming parsing.
* [#91](https://github.com/FasterXML/jackson-core/issues/91): Add methods in `JsonParser`/`JsonGenerator` for reading/writing "native" Type Ids (initially, to support YAML "tags") 

#### [Core Databind](../../jackson-databind)

* [#215](https://github.com/FasterXML/jackson-databind/issues/215): Allow registering `CharacterEscapes` via `ObjectWriter`
* [#227](https://github.com/FasterXML/jackson-databind/issues/227): Allow registering "general" Enum serializers, deserializers, via `SimpleModule`
* [#237](https://github.com/FasterXML/jackson-databind/issues/237): Add `DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY` to throw `JsonMappingException` on duplicate keys, tree model (`JsonNode`)
* [#238](https://github.com/FasterXML/jackson-databind/issues/238): Allow existence of overlapping getter, is-getter (choose 'regular' getter)
* [#269](https://github.com/FasterXML/jackson-databind/issues/269): Add support for new `@JsonPropertyDescription` via `AnnotationIntrospector`
* [#270](https://github.com/FasterXML/jackson-databind/issues/270): Add `SerializationFeature.USE_EQUALITY_FOR_OBJECT_ID` to allow use of equality (instead of identity) for figuring out when to use Object Id 

### Changes, Data Formats

#### [CSV](../../jackson-dataformat-csv)

* [#20](../../jackson-dataformat-csv/issues/20): Support filtering with `@JsonView`, `@JsonFilter` 

#### [XML](../../jackson-dataformat-xml)

* [#38](../../jackson-dataformat-xml/issues/38): Support root-level Collection serialization
* [#64](../../jackson-dataformat-xml/issues/64): Problems deserializing unwrapped lists, with optional (and missing) attribute.
* [#71](../../jackson-dataformat-xml/issues/71): Fix issues with `XmlMapper.convertValue()`
* Add support for `JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN`

#### [YAML](../../jackson-dataformat-yaml)

* [#17](../../jackson-dataformat-yaml/issues/17): Add support for `YAML` native type ids ("tags") 

### Changes, Data Types

#### [Guava](../../jackson-datatype-guava)

* [#29](../../jackson-datatype-guava/issues/29): Empty ImmutableMap not deserialized correctly, when type info included
* Add support for `DeserializationConfig.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY` for `ImmutableSet` and `MultiSet`

#### [Hibernate](../../jackson-datatype-hibernate)

* [#36](../../jackson-datatype-hibernate/issues/36): Support `@ElementCollection` for specifying lazy-loading
* [#44](../../jackson-datatype-hibernate/issues/44): NullPointerException when @OneToMany map is encountered
* Support handling of `JsonInclude(Include.NON_EMPTY)` for lazy-loaded Collections (partially addresses #21)

#### [Joda](../../jackson-datatype-joda)

* [#18](../../jackson-datatype-joda/issues/18): Add `JodaMapper`, sub-class of basic `ObjectMapper` that auto-registers Joda module

### Changes, JAX-RS

* [#24](../../jackson-jaxrs-providers/issues/24): Allow defining default view to use for endpoints without View annotation
* [#33](../../jackson-jaxrs-providers/issues/33): Provide a way to customize `ObjectReader` / `ObjectWriter` used by end points

### Changes, Other modules

#### [JAXB Annotations](../../jackson-module-jaxb-annotations)

* [#25](../../jackson-module-jaxb-annotations/issues/25):  `@XmlElementWrapper()` should work with USE_WRAPPER_NAME_AS_PROPERTY_NAME, use underlying "default" property name

#### [Afterburner](../../jackson-module-afterburner)

* [#33](../../jackson-module-afterburner/issues/33): `SuperSonicBeanDeserializer` not working (missing optimization)
* [#34](../../jackson-module-afterburner/34): Needs to skip private creators, can not access even from sub-class.

#### [Guice](../../jackson-module-guice)

First official release!

#### [JAXB Annotations](../../jackson-module-jaxb-annotations)

* [#25](../../jackson-module-jaxb-annotations/issues/25): @XmlElementWrapper() should work wrt USE_WRAPPER_NAME_AS_PROPERTY_NAME, use underlying "default" property name

#### [JSON Schema](../../jackson-module-jsonSchema)

* [#14](../../jackson-module-jsonSchema/issues/14): Generated schema contains multiple 'type' values
* [#18](../../jackson-module-jsonSchema/issues/18): Add mechanism to customize schema for property
* [#24](../../jackson-module-jsonSchema/issues/25): Improved Schema generation for Maps

#### [Paranamer](../../jackson-module-paranamer)

* [#3](../../jackson-module-paranamer/issues/3): Should not throw an exception if name not found

#### [Scala](../../jackson-module-scala)

* [#103](https://github.com/FasterXML/jackson-module-scala/issues/103): (Regression) Serialization of a class containing Option[JsonNode] fails
* [#103](https://github.com/FasterXML/jackson-module-scala/issues/103): (Regression) Serialization of a class containing Option[JsonNode] fails
* [#102](https://github.com/FasterXML/jackson-module-scala/issues/102): (Regression) JsonMappingException Argument of constructor has no property name annotation
* [#101](https://github.com/FasterXML/jackson-module-scala/issues/101): version 2.2.3 can't deserialize some class that version 2.2.2 can
* [#100](https://github.com/FasterXML/jackson-module-scala/issues/100): Deserializing SortedSets