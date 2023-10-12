Possible patch version of [2.13](Jackson-Release-2.13), released on January 23, 2023.
This will likely be the last full patch release from 2.13.x branch.

Following fixes are included in this patch release.

### Changes, core

#### [Databind](../../jackson-databind)

* [#3590](../../jackson-databind/issues/3590): Add check in primitive value deserializers to avoid deep wrapper array nesting wrt `UNWRAP_SINGLE_VALUE_ARRAYS` [CVE-2022-42003]
* [#3659](../../jackson-databind/issues/3659): Improve testing (likely via CI) to try to ensure compatibility with specific Android SDKs
* [#3661](../../jackson-databind/issues/3661): Jackson 2.13 uses Class.getTypeName() that is only available on Android SDK 26

### Changes, data formats

#### CSV

* [#343](../../jackson-dataformats-text/issues-343): Incorrect output buffer boundary check in `CsvEncoder`

#### XML

* Upgrade Woodstox to 6.4.0 for a fix to [CVE-2022-40152]

### Changes, datatypes

#### [Jakarta-JSONP / JSR-353](../../jackson-datatypes-misc)

* [#27](../../jackson-datatypes-misc/issues/27): Deserializing a JSON Merge Patch fails when the input is not a JSON object

### Changes, other

#### [Jackson-jr](../../jackson-jr)

* [#98](../../jackson-jr/issues/98): `module-info.java` of `jr-stree` refers to module `com.fasterxml.jackson.jr.ob.api`, which is not defined
