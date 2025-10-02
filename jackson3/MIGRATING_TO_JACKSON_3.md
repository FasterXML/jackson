# Jackson 3 Migration Guide

This guide aims to "connect-the-dots" for Jackson 2.x to 3.x migration.
It mostly references documentation in other repos and provides a high-level summary with appropriate links.

## Overview of Major Changes

1. New group-id (Maven/Gradle) and Java package (`com.fasterxml.jackson` -> `tools.jackson`)
    - See [JSTEP-1](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-1) for details
    - Exception: `jackson-annotations`: 2.x version still used with 3.x
         - See [this discussion](https://github.com/FasterXML/jackson-future-ideas/discussions/90) for rationale
         - Jackson 3.0 uses `jackson-annotations` `2.20`
2. All `@Deprecated` methods, fields and classes (as of 2.20.0) are removed from 3.0
    - Javadocs in Jackson `2.20` updated to indicate replacement where available
3. Major renaming of Classes, Methods as per [JSTEP-6](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-6)
   - Javadocs in Jackson `2.20` updated for some Classes to point to upcoming 3.0 Class/Method names
4. `ObjectMapper` and `JsonFactory` are fully immutable in 3.x: instances are constructed using Builder pattern
   - (instantiation of "vanilla" `ObjectMapper` is left, so `new ObjectMapper()` can still be used if default settings are fine)

For the full list of all issues resolved for 3.0, see [Jackson 3.0 Release Notes](https://github.com/FasterXML/jackson/wiki/Jackson-Release-3.0).

## High-level conversion overflow

1. Original overall planning doc: [JSTEP-1](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-1)
   - Need to change import statements due to change in Java package (`com.fasterxml.jackson` -> `tools.jackson` -- EXCEPT not for `jackson-annotations`)
2. All `@Deprecated` methods (as of 2.20.0) are removed from 3.0
   - Need Javadocs in Jackson `2.20` updated to indicate replacement where possible
3. Major renaming of Classes, Methods as per [JSTEP-6](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-6)
   - need to update Class, Method names
   - Javadocs in Jackson `2.20` updated for some Classes to point to upcoming 3.0 Class/Method names
2. Changes to default settings (esp. various XxxFeatures): [JSTEP-2](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-2)
   - may need to override defaults if existing 2.x behavior preferred
   - `JsonMapper.builderWithJackson2Defaults()` may be used to use some of legacy configuration settings (cannot change all defaults but can help migration)
6. Immutable factories
    a. `ObjectMapper`: convert direct configuration with Builder alternatives (`JsonMapper.builder().enable(...).build()`)
    b. `JsonFactory` / `TokenStreamFactory`: convert direct configuration with Builder alternatives (`JsonFactory.builder().enable(...).build()`)

## Discussions and References

1. [Jackson-future-ideas](https://github.com/FasterXML/jackson-future-ideas/wiki) Github repo contains majority of planning and discussion for Jackson 3 preparation. It includes:
    a. [JSTEP](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP) (Jackson STrategic Enhancement Proposals)
         - See all JSTEP's documentations for all changes. 
    b. [Github Discussions section](https://github.com/FasterXML/jackson-future-ideas/discussions) in the same repo as JSTEP
   c. see first (but not last) discussion [here](https://github.com/FasterXML/jackson-future-ideas/discussions/72) as example
