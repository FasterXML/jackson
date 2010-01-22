package org.codehaus.jackson.map.jsontype.impl;

import java.io.IOException;
import java.util.HashMap;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.TypeDeserializer;
import org.codehaus.jackson.map.annotate.JsonTypeInfo;
import org.codehaus.jackson.type.JavaType;

/**
 * @since 1.5
 * @author tatus
 */
public abstract class TypeDeserializerBase extends TypeDeserializer
{
    protected final TypeConverter _typeConverter;

    protected final JavaType _baseType;

    /**
     * For efficient operation we will lazily build mappings from type ids
     * to actual deserializers, once needed.
     */
    protected final HashMap<String,JsonDeserializer<Object>> _deserializers;
    
    protected TypeDeserializerBase(JavaType baseType, TypeConverter conv)
    {
        _baseType = baseType;
        _typeConverter = conv;
        _deserializers = new HashMap<String,JsonDeserializer<Object>>();
    }

    @Override
    public abstract JsonTypeInfo.As getTypeInclusion();
    
    @Override
    public final JsonTypeInfo.Id getTypeId() { return _typeConverter.idType(); }

    // base implementation returns null; ones that use property name need to override
    @Override
    public String propertyName() { return null; }

    public String baseTypeName() { return _baseType.getRawClass().getName(); }

    /*
     ************************************************************
     * Helper methods for sub-classes
     ************************************************************
     */

    protected final JsonDeserializer<Object> _findDeserializer(DeserializationContext ctxt, String typeId)
        throws IOException, JsonProcessingException
    {
        JsonDeserializer<Object> deser;
        synchronized (_deserializers) {
            deser = _deserializers.get(typeId);
            if (deser == null) {
                JavaType type = _typeConverter.typeFromString(typeId, _baseType);
                deser = ctxt.getDeserializerProvider().findValueDeserializer(ctxt.getConfig(), type, null, null);
                _deserializers.put(typeId, deser);
            }
        }
        return deser;
    }
    
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
         /**
          * Method for converting given type identifier into actual
          * class.
          * 
          * @param typeId String serialized type identifier
          *
          * @return Class the type identifier identifies
          * 
          * @throws IllegalArgumentException If no class can be located
          *    for given type id
          */
         public abstract JavaType typeFromString(String typeId, JavaType baseType)
             throws IllegalArgumentException;

         public abstract JsonTypeInfo.Id idType();
     }

     /**
      * Converter that produces fully-qualified class names as type ids.
      */
     public static class ClassNameConverter extends TypeConverter
     {
         public final static ClassNameConverter instance = new ClassNameConverter();

         public JsonTypeInfo.Id idType() { return JsonTypeInfo.Id.CLASS; }

         public JavaType typeFromString(String typeId, JavaType baseType)
             throws IllegalArgumentException
         {
             try {
                 Class<?> cls = Class.forName(typeId);
                 return baseType.narrowBy(cls);
             } catch (ClassNotFoundException e) {
                 throw new IllegalArgumentException("Invalid type id '"+typeId+"' (for id type 'Id.class'): no such class found");
             } catch (Exception e) {
                 throw new IllegalArgumentException("Invalid type id '"+typeId+"' (for id type 'Id.class'): "+e.getMessage());
             }
         }
     }

     /**
      * Converter that produces "minimal" class names (unique suffixes, relative
      * to fully-qualified base class name) as type ids.
      */
     public final static class MinimalClassNameConverter extends ClassNameConverter
     {
         /**
          * Package name of the base class, to be used for determining common
          * prefix that can be omitted from included type id.
          * Does include the trailing dot.
          */
         protected final String _basePackageName;

         protected MinimalClassNameConverter(JavaType baseType)
         {
             String base = baseType.getRawClass().getName();
             int ix = base.lastIndexOf('.');
             if (ix < 0) { // can this ever occur?
                 _basePackageName = "";
             } else {
                 // note: no trailing dot
                 _basePackageName = base.substring(0, ix);
             }
         }

         public JsonTypeInfo.Id idType() { return JsonTypeInfo.Id.MINIMAL_CLASS; }

         @Override
         public JavaType typeFromString(String typeId, JavaType baseType)
             throws IllegalArgumentException
         {
             if (typeId.startsWith(".")) {
                 StringBuilder sb = new StringBuilder(typeId.length() + _basePackageName.length());
                 if  (_basePackageName.length() == 0) {
                     // no package; must remove leading '.' from id
                     sb.append(typeId.substring(1));
                 } else {
                     // otherwise just concatenate package, with leading-dot-partial name
                     sb.append(_basePackageName).append(typeId);
                 }
                 typeId = sb.toString();
             }
             return super.typeFromString(typeId, baseType);
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
         public JavaType typeFromString(String typeId, JavaType baseType)
             throws IllegalArgumentException
         {
             throw new IllegalStateException("Not Yet Implemented");
         }
     }    
}
