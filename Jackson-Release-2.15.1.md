Patch version of [2.15](Jackson-Release-2.15), released on May 16, 2023.

Following fixes are included in this patch release.

### Changes, core

#### [Streaming](../../jackson-core)

* [#999](../../jackson-core/issues/999): Gradle metadata for `jackson-core` `2.15.0` adds dependency on  `ch.randelshofer:fastdoubleparser`
* [#1003](../../jackson-core/pull/1003): Add FastDoubleParser section to `NOTICE`
* [#1014](../../jackson-core/pull/1014): Increase default max allowed String value length from 5 megs to 20 megs
* [#1023](../../jackson-core/pull/1023): Problem with `FilteringGeneratorDelegate` wrt `TokenFilter.Inclusion.INCLUDE_NON_NULL`

#### [Databind](../../jackson-databind)

* [#3882](../../jackson-databind/issues/3882): Error in creating nested `ArrayNode`s with `JsonNode.withArray()`
* [#3894](../../jackson-databind/issues/3894): Only avoid Records fields detection for deserialization
* [#3895](../../jackson-databind/issues/3895): 2.15.0 breaking behaviour change for records and Getter Visibility
* [#3897](../../jackson-databind/issues/3897): 2.15.0 breaks deserialization when POJO/Record only has a single field and is marked `Access.WRITE_ONLY`
* [#3913](../../jackson-databind/issues/3913): Issue with deserialization when there are unexpected properties (due to null `StreamReadConstraints`)
* [#3914](../../jackson-databind/issues/3914): Fix TypeId serialization for `JsonTypeInfo.Id.DEDUCTION`, native type ids

### Changes, data formats

#### YAML

* [#404](../../jackson-dataformats-text/issues/404): Cannot serialize YAML with Deduction-Based Polymorphism

### Changes, Other modules

#### Afterburner

* [#204](../../jackson-modules-base/issues/204): Gradle metadata for `jackson-module-afterburner` and `jackson-module-mrbean` `2.15.0` adds dependency on shaded `org.ow2.asm:asm`
