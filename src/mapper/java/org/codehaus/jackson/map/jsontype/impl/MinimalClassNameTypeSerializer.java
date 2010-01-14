package org.codehaus.jackson.map.jsontype.impl;

import org.codehaus.jackson.map.annotate.JsonTypeInfo;

/**
 * Type serializer that includes delta between fully qualified name
 * of the type base class and fully-qualified name of instance class,
 * iff it can be represented as a simple suffix, starting with dot.
 * Specifically, if the two classes are in same Java package, this
 * would mean dot-prefixed local class name without package names.
 * Leading dot is included to indicate partial class name, since
 * it is also possible that there is no common package prefix: if
 * so, the fully-qualified class name is used instead (which does
 * not have leading dot and can thus be uniquely recognized).
] * 
 * @since 1.5
 * @author tatus
 */
public class MinimalClassNameTypeSerializer extends TypeSerializerBase
{
    /**
     * Package name of the base class, to be used for determining common
     * prefix that can be omitted from included type id.
     * Does include the trailing dot.
     */
    protected final String _basePackageName;

    protected MinimalClassNameTypeSerializer(JsonTypeInfo.As includeAs, String propName,
            Class<?> baseClass)
    {
        super(includeAs, propName);
        String base = baseClass.getName();
        int ix = base.lastIndexOf('.');
        if (ix < 0) { // can this ever occur?
            _basePackageName = ".";
        } else {
            _basePackageName = base.substring(0, ix+1);
        }
    }

    @Override
    protected final String typeAsString(Object value) {
        String n = value.getClass().getName();
        if (n.startsWith(_basePackageName)) {
            // note: we will leave the leading dot in there
            return n.substring(_basePackageName.length()-1);
        }
        return n;
    }
}
