Patch version of [2.15](Jackson-Release-2.15), released on May 30th, 2023.

Following fixes are included in this patch release.

### Changes, core

#### [Streaming](../../jackson-core)

* [#1019](../../jackson-core/pull/1019): Allow override of `StreamReadContraints` default with `overrideDefaultStreamReadConstraints()`
* [#1027](../../jackson-core/issues/1027): Extra module-info.class in 2.15.1
* [#1028](../../jackson-core/issues/1028): Wrong checksums in `module.json` (2.15.0, 2.15.1)
* [#1032](../../jackson-core/issues/1032): `LICENSE` misssing from 2.15.1 jar

#### [Databind](../../jackson-databind)

* [#3938](../../jackson-databind/issues/3938): Record setter not included from interface (2.15 regression)

### Changes, data formats

#### Avro

* [#379](../../jackson-dataformats-binary/issues/379): `logback-test.xml` in wrong place (avro/src/main/resources)

### Changes, JVM Languages

#### [Kotlin](../../jackson-module-kotlin)

* [#675](../../jackson-module-kotlin/issues/675): Modified to use Converter in Sequence serialization. This change allows serialization-related annotations, such as `JsonSerialize(contentUsing = ...)`, to work on `Sequence`. Also fixes [#674](../jackson-module-kotlin/issues/674).

### Changes, Other modules

#### Afterburner

* `Asm` dependency updated to 9.5 (from 9.4)

#### Mr Bean

* [#207](../../jackson-modules-base/issues/207): Mr Bean exposing `Asm` as Maven dependency despite shading
* `Asm` dependency updated to 9.5 (from 9.4)
