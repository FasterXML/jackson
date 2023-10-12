## Public releases

### Under development

* [3.0](Jackson-Release-3.0): under development; not expected to be released before Q3 of 2023
* [2.16](Jackson-Release-2.16): under development since May 2023

### Open branches

These are branches for which new releases are planned.

* [2.15](Jackson-Release-2.15): open branch (2.15 was released in April 2023)
* [2.14](Jackson-Release-2.14): open branch (2.14 was released in November 2022)
* [2.13](Jackson-Release-2.13): nominally open branch: 2.13.5 likely the last full patch set; micro-patches possible
    * Last branch where JDK 6 is baseline for streaming `jackson-core`
    * Last branch for Android SDK 21+ (SDK 26 minimum for 2.14)
    * To be closed no earlier than end of 2023 (after 2.16.0 release)

### Closed branches

These are branches for which new releases are no longer planned: it is possible that in some rare cases micro-patches of components might get released.

* [2.12](Jackson-Release-2.12) 
    * Closed on 2022-04 when 2.15.0 released
* [2.11](Jackson-Release-2.11):
    * Closed on 2022-11 when 2.14.0 released
* [2.10](Jackson-Release-2.10)
    * Closed on 2021-09-30 when `2.13.0` was released
* [2.9](Jackson-Release-2.9)
    * Closed on 2021-01-06 after release of `jackson-databind` micro-patch `2.9.10.8`
* [2.8](Jackson-Release-2.8)
* [2.7](Jackson-Release-2.7)
* [2.6](Jackson-Release-2.6)
* [2.5](Jackson-Release-2.5)
* [2.4](Jackson-Release-2.4)
* [2.3](Jackson-Release-2.3)
* [2.2]
* [2.1]
* [2.0]

### Legacy

Jackson 1.x branches are not supported: source code can be found from

https://github.com/FasterXML/jackson-1

but there is no currently working mechanism for making new releases (original sources were migrated from Codehaus SVN, build used Ant with Maven task which does not appear to work with Sonatype OSS Nexus any more).

## General

Jackson follows [Apache versioning](https://apr.apache.org/versioning.html) convention, and similar to [Semantic Versioning](http://semver.org/) from external user perspective.
That is:

1. Major version upgrades (only one so far, 1.x -> 2.x) can include all kinds of changes. However, we do this so that:
    * Neither Java nor Maven package names are reused (that is, we use different packages): this allows different major versions to co-exist
2. Minor version upgrades can contain additions, new methods, and may deprecate existing functionality
    * Our goal is to support all existing **public** functionality (public methods, constants) with minor versions
    * We do reserve the right to remove deprecated public methods as long as they have existed in deprecated form for at least two minor versions. We try to minimize such removals, but they are nonetheless made in cases where deprecated methods are problematic for future development (can not be supported, or prevent fixing other issues)
3. Patch releases need to fully replaceable and have no changes to source or binary compatibility

### Public vs Internal APIs

Public API in this context means methods and class override mechanism meant for end-users to use.
Generally this means public methods and fields, but does not necessarily include all method overrides: that is, sub-classing implementations allows one to use both public and internal APIs.
Unfortunately it is difficult to give hard and fast rules on this division, as there are currently no mechanisms to indicate which methods fall into which category.

Further, parts of Internal API are designed to be used by third-party Jackson extension components; both ones developed by Jackson core team (but that are not one of 3 core components) and ones developed by others. It would be good to have specific term for such "semi-public API", but as of now there is no official term.

But the important difference regarding compatibility is:

* Only Public API follows Apache/SemVer versioning.

### Internal API versioning

Internal API -- parts that can be used by extensions modules, but usually NOT called by other application code -- will offer reduced guarantees regarding compatibility.
Specifically, the main user-facing guarantee is:

* Extension modules are ONLY guaranteed to work with core components that have same minor version.
    * Jackson team will try to minimize changes that break compatibility, so that it is OFTEN (but not always) possible to use a LATER minor version of core components with an extension module: for example, `Joda` module version 2.3 with `jackson-core` and `jackson-databind` version 2.4.
    * Unfortunately it is not possible to known in advance what the compatibility changes in Internal API are, so it is not possible to define forward-looking rules before actual releases of new minor versions.

So why is there looser definition between Internal than External APIs? Simply because in many cases, extending functionality, and even some of bug fixes require changing of internal call sequences or argument passing. Without allowing for such changes in parts of code that are not end-user facing, we would need to do more frequent full major version upgrades. And since our firm belief is that Major Releases should go with both Maven and Java package name change (to allow for co-existence of different versions) -- belief supported by experience from relatively nice 1.x to 2.x upgrades -- these changes are much more disruptive than minor upgrades.

Having said that, what Jackson team tries to do with regard to Internal APIs is this:

* We will try to maintain Internal API compatibility between ADJACENT Minor Versions, so that extension modules of an earlier minor versions SHOULD be usable with the next minor release of core components.
    * For example, `Guava` extension module version of 2.3 should be usable with `jackson-core` and `jackson-databind` versions of 2.4
    * Note that Jackson core components (annotations, streaming, databind) MUST have same minor version.

So, if you encounter a case where this looser invariant is violated (upgrading of core components to the next minor version breaks extension modules of current minor version), you should report this.
We can not guarantee it can be fixed -- there have been cases where unfortunately it is very difficult to make this happen -- but in most cases it is possible.

## Older releases (from Old Jackson Wiki)

For full listing of all old releases, check out [Old Jackson Wiki](http://wiki.fasterxml.com/JacksonDocumentation). Here are quick links for convenience (note -- for notes on patch versions, go to major version notes, follow links from there)

* 1.x: 
    * [1.9](JacksonRelease1.9)
    * [1.8](JacksonRelease1.8)
    * [1.7](JacksonRelease1.7)
    * [1.6](JacksonRelease1.6)
    * [1.5](JacksonRelease1.5)
    * [1.4](JacksonRelease1.4)
    * [1.3](JacksonRelease1.3)
    * [1.2](JacksonRelease1.2)
    * [1.1](JacksonRelease1.1)
    * [1.0](Jackson-Release-1.0)
