# Jackson Project FAQ

## General

### What is the License?

[Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0) for Jackson 2.0 and later.
Jackson 1.x was dual licensed so that user could choose either `Apache License 2.0` or [LGPL 2.1](https://www.gnu.org/licenses/old-licenses/lgpl-2.1.en.html)

### Why do source files NOT contain license and/or copyright information

Some projects use (and require use of) per-file comment header which indicates license details
and copyright assignments. Jackson project does not do this: we believe this is redundant
and serves no useful purpose. Instead, the license information is contained in multiple places:

* In `src/main/resources/META-INF/LICENSE`, so that it gets included in
    * Binary (jar)
    * Source archives (jar / zip)
* `pom.xml` of the project (in some case parent pom)
* Included in project Wiki pages

### Is there (commercial) support available?

Yes! Starting with version 2.10 (released around end of September 2019), primary mechanism is through
[Tidelift](https://tidelift.com) subscriptions. You can subscribe to any number of Jackson components, and each component repo links to specific subscription: most common one being
[Tidelift subscription for 'jackson-databind'](https://tidelift.com/subscription/pkg/maven-com-fasterxml-jackson-core-jackson-databind?utm_source=maven-com-fasterxml-jackson-core-jackson-databind&utm_medium=referral&utm_campaign=readme)
