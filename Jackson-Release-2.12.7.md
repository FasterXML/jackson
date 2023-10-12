Patch version of [2.12](Jackson-Release-2.12), released on May 26, 2022.
It will be the last full patch release for 2.12 series.

The main reason for releasing one more "full" patch release was to include the one CVE-related fix from micro-patch `2.12.6.1`.

### Changes, core

#### [Databind](../../jackson-databind)

* [#2816](../../jackson-databind/issues/2816):  Optimize UntypedObjectDeserializer wrt recursion (CVE-2020-36518)
