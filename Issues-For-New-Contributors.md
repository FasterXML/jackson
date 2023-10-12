# New Contributor Friendly issues

This is a new (created October 2019) Wiki page that tries to link to those issues across Jackson components
that are considered "New Contributor Friendly": something that may be easy to resolve (but sometimes not), but should at least be easy to tackle in some form: verify, add a unit test, investigate root cause(s), potential fix(es). And ultimately getting fixed of course.

## How does it work?

This is just a manually maintained page to actual issues: I have created/will create `good-first-issue` label on all Jackson component repos, and whenever labeling, will try to add link here. I encourage everyone else to do the same -- I intent to keep this Wiki as open as possible (either fully open, or via Team that has access, adding anyone who wants to be added)

## What if they do not look all that easy?

That is understandable -- classification is inexact science, and they may only appear less complex than truly complicated ones. Sometimes things are more difficult than they appear, as well.

But if you are unsure about the problem or possible ways to fix the problem, don't be afraid to ask.
Sometimes a small but vital piece of information from project owners can help a lot; maintainers do not always remember to add all contextual information (or assume reader is more familiar with the project).
Asking clarifying questions is encouraged, especially when including notes on parts that you (think you) understand.

## Want to be a Meta-Helper?

Besides obvious help by working on issues in some form, we could ALSO use help in maintaining this page.

One challenge with Github Wikis is that they are either fully open -- so anyone can change anything -- or must be gated by coarse Repo settings. Although ideally we would leave this Wiki wide open there are some security concerns by malicious changes here leading users to bad web sites or mis-information so Wiki here is only editable by users that belong to `Wiki` team of `FasterXML` organization.
But we trust our community so if you would like to help add/update/delete entries here, please contact us either via Issue in this repo, or by sending email note to `info` at fasterxml dot com, and we will give you access.

-----

## Jackson Core Components

### Streaming

* [#577](https://github.com/FasterXML/jackson-core/issues/577): Consider number-decoding improvements from jsoniter (esp. for `double`/`float`, `BigInteger`, `BigDecimal`)

### Databind

* [#2302](https://github.com/FasterXML/jackson-databind/issues/2302) -- Improve exception used, message, when indicating a `required` property is not set (only need to add test case first!)

## Jackson Datatype modules

### Java 8 Date/Time

* [#108](https://github.com/FasterXML/jackson-modules-java8/issues/108): Default string formats that will deserialize successfully to an Instant? (DOCUMENTATION)
* [#130](https://github.com/FasterXML/jackson-modules-java8/issues/130): Why is there no concrete `OffsetDateTimeDeserializer` class to use via annotations
* [#168](https://github.com/FasterXML/jackson-modules-java8/issues/168): InstantSerializer doesn't respect any format-related settings without replacing serializer instance

### Joda

* [#98](https://github.com/FasterXML/jackson-datatype-joda/issues/98): `JsonFormat` timezone attribute effect overwritten if pattern attribute present

### Guava

* [#2](https://github.com/FasterXML/jackson-datatypes-collections/issues/2): : Better multiset serialization / deserialization
* [#7](https://github.com/FasterXML/jackson-datatypes-collections/issues/7): Add support for `WRITE_SORTED_MAP_ENTRIES`
* [#78](https://github.com/FasterXML/jackson-datatypes-collections/issues/78): Add README for "eclipse-collections" (DOCUMENTATION)

## Jackson Dataformat Modules

### CSV

* [#198](https://github.com/FasterXML/jackson-dataformats-text/issues/198): Support writing numbers as strings for CSV

### Properties

### XML

* [#302](https://github.com/FasterXML/jackson-dataformat-xml/issues/302): Unable to serialize top-level Java8 Stream
* [#329](https://github.com/FasterXML/jackson-dataformat-xml/issues/329): Jackson ignores JacksonXmlElementWrapper on Stream

### YAML

- none currently -

## JVM Languages

### Kotlin

* [#385](https://github.com/FasterXML/jackson-module-kotlin/issues/385): Add Moditect, source module info, to allow Kotlin module usage with Java Module system

## Friends of Jackson projects

### Woodstox

* [#95](https://github.com/FasterXML/woodstox/issues/95): BaseStreamWriter.writeSpace(String) should not close open element

-----

## Recently Completed Issues

Note: these issues were solved recently by contributors outside of main development teams (new or otherwise)

### 2020

#### October / Hacktoberfest

* [#2873](https://github.com/FasterXML/jackson-databind/issues/2873) -- `MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS` should work for enum as keys -- by @ILGO0413
* [#1458](https://github.com/FasterXML/jackson-databind/issues/1458) -- `@JsonAnyGetter` should be allowed on a field -- by @dominikrebhan
* [#2291](https://github.com/FasterXML/jackson-databind/issues/2291) -- Create tutorial on how to use Builders and Jackson -- by @Hassan-Elseoudy
* [#500](https://github.com/FasterXML/jackson-core/issues/500): Allow `optional-padding` for `Base64Variant` -- PR by @pavan-kalyan 
* [#94](https://github.com/FasterXML/jackson-modules-java8/issues/94): Deserialization of timestamps with UTC timezone to LocalDateTime doesn't yield correct time -- PR by @angelyan
* [#25](https://github.com/FasterXML/jackson-datatypes-collections/issues/25): SetMultimap should be deserialized to a LinkedHashMultimap by default -- PR by @Migwel 
* [#2871](https://github.com/FasterXML/jackson-databind/issues/2871) -- Serialization of map keys does not use `@JsonValue` similar to values (no chaining?) -- maybe add `@JsonKey` annotation -- PR by @Anusien


#### July

* (databind) [#2215](https://github.com/FasterXML/jackson-databind/issues/2215): Support BigDecimal in StdValueInstantiator -- by @upsidedownsmile





