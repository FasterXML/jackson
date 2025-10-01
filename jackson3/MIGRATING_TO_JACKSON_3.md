# Jackson 3 Migration Guide

> This guide aims to "connect-the-dots" for Jackson 2.x to 3.x migration.
> Mostly references documentation in other repos.

## Highlights

1. New group-id (Maven/Gradle) and Java package (`com.fasterxml.jackson` -> `tools.jackson`)
   - except for `jackson-annotations` (see below)
2. `jackson-annotations` module from Jackson 2.x still used with 3.x
   - Initial Jackson 3.0 uses `jackson-annotations` `2.20`
   - [See discussion](https://github.com/FasterXML/jackson-future-ideas/discussions/90) for rationale
3. `ObjectMapper` and `JsonFactory` are fully immutable in 3.x: instances are constructed using Builder pattern
   - (instantiation of "vanilla" `ObjectMapper` is left, so `new ObjectMapper()` can still be used if default settings are fine)
4. Full list of all issues resolved for 3.0: https://github.com/FasterXML/jackson/wiki/Jackson-Release-3.0

## High-level conversation overflow, including

1. Original overall planning doc: [JSTEP-1](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-1)
   - Need to change import statements due to change in Java package (`com.fasterxml.jackson` -> `tools.jackson` -- EXCEPT not for `jackson-annotations`)
2. Changes to default settings (esp. various XxxFeatures): [JSTEP-2](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-2)
   - may need to override defaults if existing 2.x behavior preferred
   - `JsonMapper.builderWithJackson2Defaults()` may be used to use some of legacy configuration settings (cannot change all defaults but can help migration)
3. Renaming of Classes, Methods as per [JSTEP-6](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-6)
   - need to update Class, Method names
   - Javadocs in Jackson `2.20` updated for some Classes to point to upcoming 3.0 Class/Method names
4. All `@Deprecated` methods (as of 2.20.0) are removed from 3.0
   - Javadocs in Jackson `2.20` updated to indicate replacement where possible
6. Immutable factories
    a. `ObjectMapper`: convert direct configuration with Builder alternatives (`JsonMapper.builder().enable(...).build()`)
    b. `JsonFactory` / `TokenStreamFactory`: convert direct configuration with Builder alternatives (`JsonFactory.builder().enable(...).build()`)

## Discussions and References

1. [Jackson-future-ideas](https://github.com/FasterXML/jackson-future-ideas/wiki) Github repo contains majority of planning and discussion for Jackson 3 preparation. It includes:
    a. [JSTEP](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP) (Jackson STrategic Enhancement Proposals)
         - See all JSTEP's documentations for all changes. 
    b. [Github Discussions section](https://github.com/FasterXML/jackson-future-ideas/discussions) in the same repo as JSTEP
   c. see first (but not last) discussion [here](https://github.com/FasterXML/jackson-future-ideas/discussions/72) as example
