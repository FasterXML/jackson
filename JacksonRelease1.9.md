# Jackson Release: 1.9

Version 1.9 was released October 4, 2011, about 6 months after [1.8](JacksonRelease1.8).

The biggest general feature implemented was a complete rewrite of Bean Property introspection logic (see JACKSON-242 below), which conceptually binds distinct accessors (getter, setter, field, constructor parameter) of a logical property, and merges all relevant annotations. This is good in that it removes need for redundant annotations when renaming or ignoring properties, or defining type information.

In addition to multiple other features, the main focus was to plan for upcoming [2.0](Jackson-Release-2.0), which is likely to be less backwards-compatible release. The goal was to try to add deprecation warnings as early as possible, to try to minimize eventual changes needed to upgrade code relies on 1.9, to use 2.0. As a result, number of deprecated methods and classes increased significantly.

The last bigger general change was the introduction of `ValueInstantatior`s (see below), which finally allows for custom handling of POJO creation, above and beyond ability to use `@JsonCreator` annotation.

## Related

* [Jackson 1.9 feature overview](http://www.cowtowncoder.com/blog/archives/2011/10/entry_463.html) (at Cowtalk)

## Implemented features, Major

 * JACKSON-132: Allow inlining/unwrapping of child objects using `@JsonUnwrapped`.
 * JACKSON-242: Rewrite property introspection part of framework to combine getter/setter/field annotations
 * JACKSON-406: Allow injection of values during deserialization.
 * JACKSON-453: Support for 'external type id' by adding `@JsonTypeInfo.As.EXTERNAL_PROPERTY` to indicate that property should be added at parent (i.e. as sibling to value being typed)
 * JACKSON-580: Allow registering instantiators (`ValueInstantiator`) for types (classes)
      * Related: JACKSON-633: add `@JsonValueInstantiator` to specify instantiator for a class.

## Implemented features, Medium

* JACKSON-517: Create "mini-core" (`jackson-mini-VERSION.jar`) jar that trims down as much weight from standard jackson-core jar as possible
* JACKSON-558: Implement `DeserializationConfig.Feature.UNWRAP_ROOT_VALUE` (as counterpart to `SerializationConfig.Feature.WRAP_ROOT_VALUE`)
* JACKSON-578: Add support to mark JAX-RS methods to return a specific `JsonView`
* JACKSON-594: Support deserializing of non-static inner classes
* JACKSON-595: Terse(r) Visibility Config: add ObjectMapper.`setVisibility()`
* JACKSON-598: Add set of standard naming-strategy implementations
* JACKSON-602: Add `JsonSerialize.Inclusion.NON_EMPTY` option
* JACKSON-614: Add JsonTypeInfo.defaultSubType property to indicate type to use if class id/name missing

## Implemented features, Minor

 * JACKSON-254: Add `SerializationConfig.Feature.WRITE_EMPTY_JSON_ARRAYS`, to allow suppressing serialization of empty Collections/arrays.
 * JACKSON-531: Comparing actual and default value (for `JsonSerialize.Inclusion.NON_DEFAULT`) should check array contents
* JACKSON-584: Serialize type info for non-static anonymous inner classes as that of declared (static) type
* JACKSON-593: Add more convenience methods in ObjectMapper for reading values as JSON trees `readTree(URL)`, `readTree(byte[])`)
* JACKSON-606: Add proper support for serializing Java Date values as Map keys
* JACKSON-620: Allow empty String to mean null Map, Collection, array, if 'DeserializationConfig.Feature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT' enabled
* JACKSON-621: Add new fluent method, VisibilityChecker.with(Visibility)
* JACKSON-648: Add 'ObjectMapper.writer(DateFormat)', 'ObjectWriter.withDateFormat()' to allow per-call override of the date format to use (or serialize-as-timestamp if null passed)
* JACKSON-650: Add 'SimpleFilterProvider.setFailOnUnknownId()' to disable throwing exception on missing filter id.
* JACKSON-652: Add `DeserializationConfig.Feature]].USE_JAVA_ARRAY_FOR_JSON_ARRAY`
* JACKSON-653: Add efficient `JsonParser.nextFieldName()` and `JsonParser.nextXxxValue()` methods.
* JACKSON-666: Add `SerializationConfig.Feature.REQUIRE_SETTERS_FOR_GETTERS` to only auto-detect getters for which there is a setter (or field, constructor parameter)

## Planned features that were deferred

Following features were considered for inclusion, but ended up getting deferred:

* JACKSON-275: Support multi-argument setters (to allow co-constraints)
* JACKSON-437: Support to set logical property with value of type id that was used for deserialization
* JACKSON-469: Support "builder pattern" for deserialization, @JsonCreator
* JACKSON-490: Allow subclasses to "inherit" @JsonCreator annotation from superclass constructor
* JACKSON-592: Allow inlining/unwrapping of value from single-component JSON array
