## Acknowledgments for Jackson 2.12 release

This page outlines some of contributors that contributed to [Jackson 2.12](Jackson-Release-2.12): it is not an exhaustive list but summarizes some of larger features. List is no almost certainly incomplete (apologies to anyone whose contribution is accidentally left out) as well as subjective -- no feature is insignificant, and omission from here (intentional or accidental) is not meant as value judgment.

###  Module maintainers

Special thank you notes to following module maintainers (existing and new) for their contributions (in alphabetic module order):

* Blackbird (NEW in 2.12!): [Steven Schlansker](https://github.com/stevenschlansker) (@stevenschlansker) contributed a replacement module for Afterburner called [Blackbird](https://github.com/FasterXML/jackson-modules-base/blob/master/blackbird/README.md) (clever name, even!)
    * Designed module specifically work better with newer (past-Java 8) JVMs, like JVM 11 and JVM 14
* Java 8 date/time: [Michael O'Keeffe](https://github.com/kupci) (kupci@github)
    * Helped unify Date/Time handling significantly for 2.12 by fixing issues, reviewing PRs.
* Kotlin: [Drew Stephens](https://github.com/dinomite) (@dinomite) and [Vyacheslav Artemyev](https://github.com/viartemev) (@viartemev)
    * Further improved Kotlin module to use new extension points introduced by core databind module
* Scala: [PJ Fanning](https://github.com/pjfanning) (@pjfanning)
    * Fixed many long-standing issues with Scala module to "catch up" with core databind
    * Has handled release responsibilities for the module since previous maintainers moved on

### Module metadata improvements

Jackson 2.12 significantly improves metadata included with modules, to support more advanced dependency management. Special thank you to following contributors:

* [Jendrik Johannes](https://github.com/jjohannes) (@jjohannes): contributed Gradle Module Metadata improvements (like [databind#2726](https://github.com/FasterXML/jackson-databind/issues/2726))
    * Can significantly improve dependency version handling with Gradle 6, see [this blog post](https://blog.gradle.org/alignment-with-gradle-module-metadata)
* [Marc Magon](https://github.com/GedMarc) (@GedMarc): contributed further improvements to Java Module System (JPMS) metadata (`module-info.class`) regarding dependencies to JavaEE dependencies (JAXB, JAX-WS)

### "Big" New Features

Jackson 2.12 contains support for many highly-requested features (now tagged with `most-wanted` label); things that users have waited for years in some cases (and less in others :) ). Here are the Most Wanted features, from oldest to newest:

#### `@JsonTypeInfo(use=DEDUCTION)` (type inference for polymorphic deserialization)

* [Marc Carter](https://github.com/drekbour) (@drekbour) provided the PR for the VERY OLDEST open feature request
    * Impressively simple solution to complicated problem, solution for which had evaded us for a... while. :)
    * See [databind#43](https://github.com/FasterXML/jackson-databind/issues/43) for details

#### `@JsonIncludeProperties`

* [Baptiste Pernet](https://github.com/sp4ce) (@sp4ce) provided the PR for this long-time favorite feature request by users -- basically reverse of `@JsonIgnoreProperties` (opt-in vs opt-out)
    * See [databind#1296](https://github.com/FasterXML/jackson-databind/issues/1296) for details

#### Annotation-less 1-arg Creator method (finally)

* [Lovro Pandžić](https://github.com/lpandzic) (@lpandzic) helped get this perennial favorite feature request to finally be implemented in 2.12 (after many close misses)
    * See [databind#1498](https://github.com/FasterXML/jackson-databind/issues/1498) for details

#### Java 14 Record (`java.lang.Record`) support

* [Gunnar Morling](https://github.com/gunnarmorling) (@gunnarmorling) and [Youri Bonnaffé](https://github.com/youribonnaffe) (@youribonnaffe)
    * Provided initial PR for support, tests, and guidance on improvements.
    * See [databind#2709](https://github.com/FasterXML/jackson-databind/issues/2709) for details

### Notable Fixes

* [Carter Kozak](https://github.com/carterkozak) (@carterkozak) for important fixes to Static Factory generic type coercion (for example, [databind#2895](https://github.com/FasterXML/jackson-databind/issues/2895)), related areas
    * Particularly helpful was testing during Release Candidates, including integration tests
