# Jackson 3 Migration Guide

## Key-Points

1. Biggest change from Jackson 2 to 3 would be `ObjectMapper` and `JsonFactory` would now be fully immutable.
    - Only "vanilla" instantiation of `ObjectMapper` is left, which is `new ObjectMapper()`
2. All issues resolved for 3.0: https://github.com/FasterXML/jackson/wiki/Jackson-Release-3.0

## High-level conversation overflow, including

1. Original overall planning doc : [JSTEP-1](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-1)
   - need to change import statements
   - Change to Java packages as per
2. Changes to default settings (esp various XxxFeatures) -- [JSTEP-2](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-2)
    - may need to override defaults, or
    - `builderWithJackson2Defaults()` to stick to legacy configuration settings (though highly recommended not to)
3. Renaming of Classes, Methods as per [JSTEP-6](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-6)
    - need to update names
3. All @Deprecated methods are removed from 3.0 -- convert as per 2.x version JavaDocs
    - 2.20.0 as of writing
4. Changes to default settings (esp various XxxFeatures) -- [JSTEP-2](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-2)
   - may need to override defaults, or
   - `builderWithJackson2Defaults()` to stick to legacy configuration settings (though highly recommended not to)
5. Immutable JsonFactory / TokenStreamFactory: convert direct configuration with Builder alternatives
6. Immutable ObjectMapper: convert direct configuration with Builder alternatives

## Discussions and References

1. [Jackson-future-ideas](https://github.com/FasterXML/jackson-future-ideas/wiki) Github repo contains majority of planning and discussion for Jackson 3 preparation. It includes...
2. [JSTEP](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP) (Jackson STrategic Enhancement Proposals)
3. [Github Discussions section](https://github.com/FasterXML/jackson-future-ideas/discussions) in the same repo as JSTEP
   - see first (but not last) discussion [here](https://github.com/FasterXML/jackson-future-ideas/discussions/72) as example

## Some changes to note

- `ObjectMapper.copy()` is unnecessary as mappers are immutable. If needed, `mapper.rebuild().build();` can be used.
- `PropertyNamingStrategyBase` would be removed from 2.20 version -- as per comments, see [databind#2715](https://github.com/FasterXML/jackson-databind/issues/2715) for reasons for removal -- possible nasty crash.
    - Replacement for both 2.x and 3.0 is PropertyNamingStrategies.NamingBase
- The `MapperFeature.AUTO_DETECT_....` constants are removed -- visibility configuration via `ObjectMapper.builder().changeDefaultVisibility(...)`
    - check out unit `jackson-databind` tests for usage.
