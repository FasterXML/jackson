package org.codehaus.jackson.map.module;

import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.Module;

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
    
    /*
    /**********************************************************
    /* Life-cycle: create, configure
    /**********************************************************
     */
    
    public SimpleModule(String name, Version version)
    {
        _name = name;
        _version = version;
    }

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

    public <T> SimpleModule addDeserializer(Class<T> type, JsonDeserializer<? extends T> deser)
    {
        if (_deserializers == null) {
            _deserializers = new SimpleDeserializers();
        }
        _deserializers.addDeserializer(type, deser);
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
    }

    @Override
    public Version version() {
        return _version;
    }
}
