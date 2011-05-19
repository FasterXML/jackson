package org.codehaus.jackson.map.module;

import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.*;

/**
 * Simple {@link Module} implementation that allows registration
 * of serializers and deserializers, and bean serializer
 * and deserializer modifiers.
 * 
 * @since 1.7
 */
public class SimpleModule extends Module
{
    protected final String _name;
    protected final Version _version;
    
    protected SimpleSerializers _serializers = null;
    protected SimpleDeserializers _deserializers = null;

    protected SimpleSerializers _keySerializers = null;
    protected SimpleKeyDeserializers _keyDeserializers = null;

    /**
     * Lazily-constructed resolver used for storing mappings from
     * abstract classes to more specific implementing classes
     * (which may be abstract or concrete)
     */
    protected SimpleAbstractTypeResolver _abstractTypes = null;
    
    /*
    /**********************************************************
    /* Life-cycle: creation
    /**********************************************************
     */
    
    public SimpleModule(String name, Version version)
    {
        _name = name;
        _version = version;
    }

    /*
    /**********************************************************
    /* Configuration methods
    /**********************************************************
     */
    
    public SimpleModule addSerializer(JsonSerializer<?> ser)
    {
        if (_serializers == null) {
            _serializers = new SimpleSerializers();
        }
        _serializers.addSerializer(ser);
        return this;
    }
    
    public <T> SimpleModule addSerializer(Class<? extends T> type, JsonSerializer<T> ser)
    {
        if (_serializers == null) {
            _serializers = new SimpleSerializers();
        }
        _serializers.addSerializer(type, ser);
        return this;
    }

    public <T> SimpleModule addKeySerializer(Class<? extends T> type, JsonSerializer<T> ser)
    {
        if (_keySerializers == null) {
            _keySerializers = new SimpleSerializers();
        }
        _keySerializers.addSerializer(type, ser);
        return this;
    }
    
    public <T> SimpleModule addDeserializer(Class<T> type, JsonDeserializer<? extends T> deser)
    {
        if (_deserializers == null) {
            _deserializers = new SimpleDeserializers();
        }
        _deserializers.addDeserializer(type, deser);
        return this;
    }

    public SimpleModule addKeyDeserializer(Class<?> type, KeyDeserializer deser)
    {
        if (_keyDeserializers == null) {
            _keyDeserializers = new SimpleKeyDeserializers();
        }
        _keyDeserializers.addDeserializer(type, deser);
        return this;
    }

    /**
     * Lazily-constructed resolver used for storing mappings from
     * abstract classes to more specific implementing classes
     * (which may be abstract or concrete)
     */
    public <T> SimpleModule addAbstractTypeMapping(Class<T> superType,
            Class<? extends T> subType)
    {
        if (_abstractTypes == null) {
            _abstractTypes = new SimpleAbstractTypeResolver();
        }
        // note: addMapping() will verify arguments
        _abstractTypes = _abstractTypes.addMapping(superType, subType);
        return this;
    }
    
    /*
    /**********************************************************
    /* Module impl
    /**********************************************************
     */
    
    @Override
    public String getModuleName() {
        return _name;
    }

    @Override
    public void setupModule(SetupContext context)
    {
        if (_serializers != null) {
            context.addSerializers(_serializers);
        }
        if (_deserializers != null) {
            context.addDeserializers(_deserializers);
        }
        if (_keySerializers != null) {
            context.addKeySerializers(_keySerializers);
        }
        if (_keyDeserializers != null) {
            context.addKeyDeserializers(_keyDeserializers);
        }
        if (_abstractTypes != null) {
            context.addAbstractTypeResolver(_abstractTypes);
        }
    }

    @Override
    public Version version() {
        return _version;
    }
}
