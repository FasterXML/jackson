# Jackson 3 Migration Guide

> This guide aims to "connect-the-dots" for Jackson 3 migration.
> Meaning actual content should be referred to the original documentations.

## Highlights

1. New group-id (Maven/Gradle) and Java package (com.fasterxml.jackson -> tools.jackson). 
2. Shared `annotations` module between Jackson 2 and 3
   - "Shared" mean using same version
   - [See discussion](https://github.com/FasterXML/jackson-future-ideas/discussions/90) for rationale behind
2. Jackson 2 to 3 would be `ObjectMapper` and `JsonFactory` would now be fully immutable. Then so instances are constructed using Builder pattern
   - Only "vanilla" instantiation of `ObjectMapper` is left, which is `new ObjectMapper()`
3. All issues resolved for 3.0: https://github.com/FasterXML/jackson/wiki/Jackson-Release-3.0

## High-level conversation overflow, including

1. Original overall planning doc : [JSTEP-1](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-1)
   - need to change import statements
   - Change to Java packages as per
2. Changes to default settings (esp various XxxFeatures) -- [JSTEP-2](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-2)
   - may need to override defaults, or
   - `builderWithJackson2Defaults()` to stick to legacy configuration settings (though highly recommended not to)
3. Renaming of Classes, Methods as per [JSTEP-6](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-6)
   - need to update names
4. All @Deprecated methods are removed from 3.0 -- convert as per 2.x version JavaDocs
   - 2.20.0 as of writing
5. Changes to default settings (esp various XxxFeatures) -- [JSTEP-2](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-2)
   - may need to override defaults, or
   - `builderWithJackson2Defaults()` to stick to legacy configuration settings (though highly recommended not to)
6. Immutable JsonFactory / TokenStreamFactory: convert direct configuration with Builder alternatives
7. Immutable ObjectMapper: convert direct configuration with Builder alternatives

## Discussions and References

1. [Jackson-future-ideas](https://github.com/FasterXML/jackson-future-ideas/wiki) Github repo contains majority of planning and discussion for Jackson 3 preparation. It includes...
2. [JSTEP](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP) (Jackson STrategic Enhancement Proposals)
   - See all JSTEP's documentations for all changes. 
3. [Github Discussions section](https://github.com/FasterXML/jackson-future-ideas/discussions) in the same repo as JSTEP
   - see first (but not last) discussion [here](https://github.com/FasterXML/jackson-future-ideas/discussions/72) as example