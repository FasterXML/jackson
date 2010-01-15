package org.codehaus.jackson.map.jsontype.impl;

import org.codehaus.jackson.map.TypeSerializer;
import org.codehaus.jackson.map.annotate.JsonTypeInfo;

/**
 * @since 1.5
 * @author tatus
 */
public abstract class TypeSerializerBase extends TypeSerializer
{
    protected final TypeConverter _typeConverter;
    
    protected TypeSerializerBase(TypeConverter conv)
    {
        _typeConverter = conv;
    }

    @Override
    public abstract JsonTypeInfo.As getTypeInclusion();

    @Override
    public JsonTypeInfo.Id getTypeId() { return _typeConverter.idType(); }

    // base implementation returns null; ones that use property name need to override
    @Override
    public String propertyName() { return null; }

    /*
    ************************************************************
    * Helper classes
    ************************************************************
     */

    /**
     * Interface used for concrete type information converters
     * that convert Java class to JSON String based type ids.
     */
    public abstract static class TypeConverter {
        public abstract String typeAsString(Object value);
        public abstract JsonTypeInfo.Id idType();
    }

    /**
     * Converter that produces fully-qualified class names as type ids.
     */
    public final static class ClassNameConverter extends TypeConverter
    {
        public final static ClassNameConverter instance = new ClassNameConverter();

        public JsonTypeInfo.Id idType() { return JsonTypeInfo.Id.CLASS; }
        public String typeAsString(Object value) {
            return value.getClass().getName();
        }
    }

    /**
     * Converter that produces "minimal" class names (unique suffixes, relative
     * to fully-qualified base class name) as type ids.
     */
    public final static class MinimalClassNameConverter extends TypeConverter
    {
        /**
         * Package name of the base class, to be used for determining common
         * prefix that can be omitted from included type id.
         * Does include the trailing dot.
         */
        protected final String _basePackageName;

        protected MinimalClassNameConverter(Class<?> baseClass)
        {
            String base = baseClass.getName();
            int ix = base.lastIndexOf('.');
            if (ix < 0) { // can this ever occur?
                _basePackageName = ".";
            } else {
                _basePackageName = base.substring(0, ix+1);
            }
        }

        public JsonTypeInfo.Id idType() { return JsonTypeInfo.Id.MINIMAL_CLASS; }

        @Override
        public final String typeAsString(Object value) {
            String n = value.getClass().getName();
            if (n.startsWith(_basePackageName)) {
                // note: we will leave the leading dot in there
                return n.substring(_basePackageName.length()-1);
            }
            return n;
        }
    }

    /**
     * Converter that uses externally configured (but simple) mapping
     * from classes to type names. In case of an unrecognized class,
     * will revert to using simple non-qualified class name as
     * type name.
     */
    public final static class TypeNameConverter extends TypeConverter
    {
        public JsonTypeInfo.Id idType() { return JsonTypeInfo.Id.NAME; }

        // !!! @TODO
        @Override
        public final String typeAsString(Object value) {
            // !!! Placeholder impl
            String n = value.getClass().getName();
            int ix = n.lastIndexOf('.');
            return (ix < 0) ? n : n.substring(ix+1);
        }
    }
}
