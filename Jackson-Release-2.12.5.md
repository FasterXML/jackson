Patch version of [2.12](Jackson-Release-2.12), released on 27-Aug-2021.
It may be the last full patch release for 2.12 minor version.

Following fixes will be included in this patch release.

### Changes, core

#### [Streaming](../../jackson-core)

* [#712](../../jackson-core/issues/712): (partial) Optimize array allocation by JsonStringEncoder
* [#713](../../jackson-core/issues/713): Add back accidentally removed `JsonStringEncoder` related methods in `BufferRecyclers` (like `getJsonStringEncoder()`)

#### [Databind](../../jackson-databind)

* [#3220](../../jackson-databind/issues/3220): (regression) Factory method generic type resolution does not use Class-bound type parameter

### Changes, other modules

#### [Blackbird](../../jackson-modules-base)

* [#141](../../jackson-modules-base/issues/141): Blackbird fails to deserialize varargs array

