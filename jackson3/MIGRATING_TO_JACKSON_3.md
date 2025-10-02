# Jackson 3 Migration Guide

This guide aims to "Connect the Dots" for Jackson 2.x to 3.x migration, helping developers by outlining the process. It is not a comprehensive guide, or check list.

Guide mostly references documentation in other repos and provides a high-level summary with appropriate links.

## Overview of Major Changes

1. New Maven group-id and Java package: `tools.jackson` (2.x used `com.fasterxml.jackson`)
    - See [JSTEP-1](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-1) for details
    - Exception: `jackson-annotations`: 2.x version still used with 3.x, so no group-id/Java package change
         - See [this discussion](https://github.com/FasterXML/jackson-future-ideas/discussions/90) for rationale
         - Jackson 3.0 uses `jackson-annotations` `2.20`
         - "Exception to Exception": annotations within `jackson-databind` WILL move to new Java package (so, `tools.jackson.databind.DatabindException`)
2. All `@Deprecated` (as of 2.20) methods, fields and classes are removed from 3.0
    - Javadocs in Jackson `2.20` updated to indicate replacements where available (incomplete: PRs welcome for more!)
3. Renaming of Core Entities (classes), methods, fields
    - See [JSTEP-6](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-6) for rationale, reference to notable renamings
    - Javadocs in Jackson `2.20` updated to indicate new names where available (incomplete: PRs welcome for more!)
4. Changes to Default Configuration Settings (esp. various XxxFeatures)
    - See [JSTEP-2](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-2) for rationale, the set of changes made
5. Immutability of `ObjectMapper`, `JsonFactory`
    - `ObjectMapper` and `JsonFactory` (and their sub-types) are fully immutable in 3.x: instances to be  constructed using Builder pattern
6. Unchecked exceptions: all Jackson exceptions now `RuntimeException`s (unchecked)
    - [JSTEP-4](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-4)  explains rationale, changes
    - Base exception (`JsonProcessingException` in 2.x, renamed as `JacksonException`) now extends `RuntimeException` and NOT `IOException` (like 2.x did)

For the full list of all issues resolved for 3.0, see [Jackson 3.0 Release Notes](https://github.com/FasterXML/jackson/wiki/Jackson-Release-3.0).

## High-level conversion overflow

Starting from the high-level change list, we can see the need for following changes:

1. Maven group id, Java package change
   - Need to update build files (`pom.xml`, `build.gradle`) to use new group id (`com.fasterxml.jackson.core` -> `tools.jackson.core` and so on)
   - Need to change import statements due to change in Java package (`com.fasterxml.jackson` -> `tools.jackson` -- EXCEPT not for `jackson-annotations`)
2. `@Deprecated` method, field, class removal:
   - Need to replace with non-Deprecated alternatives, as per `2.20` Javadocs updated to indicate replacement where possible
   - See later Section for a set of common cases
3. Renaming of Core Entities (classes), methods, fields
   - Need to change references to use new name (including `import` statements): `2.20` Javadocs updated to indicate replacement where possible
   - [JSTEP-6](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-6) includes a list (likely incomplete) of renamed things as well
4. Changes to Default Configuration Settings
   - MAY need to override some defaults (where existing 2.x behavior preferred) -- but most changes are to settings developers prefer so unlikely to need to change all
       - `JsonMapper.builderWithJackson2Defaults()` may be used to use some of legacy configuration settings (cannot change all defaults but can help migration)
    - [JSTEP-2](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-2) lists all default changes
5. Immutability of `ObjectMapper`, `JsonFactory`
    - `ObjectMapper`/`JsonMapper`: convert direct configuration with Builder alternatives: `JsonMapper.builder().enable(...).build()`
    - `JsonFactory` / `TokenStreamFactory`: convert direct configuration with Builder alternatives:  `JsonFactory.builder().enable(...).build()`
6. Unchecked exceptions
    - May require changes to handling: catching Jackson exceptions no longer required (but may catch of course)
    - No need to declare `throws` clause for Jackson calls
    - Base exceptions renamed; specifically:
        - `JsonProcessingException` -> `JacksonException`
        - `JsonMappingException` -> `DatabindException`

## Discussions and References

1. [Jackson-future-ideas](https://github.com/FasterXML/jackson-future-ideas/wiki) Github repo contains majority of planning and discussion for Jackson 3 preparation. It includes:
    a. [JSTEP](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP) (Jackson STrategic Enhancement Proposals)
         - See all JSTEP's documentations for all changes. 
    b. [Github Discussions section](https://github.com/FasterXML/jackson-future-ideas/discussions) in the same repo as JSTEP
   c. see first (but not last) discussion [here](https://github.com/FasterXML/jackson-future-ideas/discussions/72) as example

## Detailed Conversion Guidelines

### New Maven group-id and Java package

(TO BE WRITTEN)

### Deprecated method/field/class removal

(TO BE WRITTEN)

### Core entity (class), method, field renamin

(TO BE WRITTEN)

### Default Config Setting changes

(TO BE WRITTEN)

### Immutability of `ObjectMapper`, `JsonFactory`

(TO BE WRITTEN)

### Unchecked Exceptions

(TO BE WRITTEN)
