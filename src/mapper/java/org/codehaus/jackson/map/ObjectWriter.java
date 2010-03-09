package org.codehaus.jackson.map;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.introspect.VisibilityChecker;
import org.codehaus.jackson.map.jsontype.TypeResolverBuilder;
import org.codehaus.jackson.map.ser.*;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.type.JavaType;

/**
 * Builder object that can be used for per-serialization configuration of
 * serialization parameters, such as JSON View and root type to use.
 * Uses "fluid" (aka builder) pattern so that instances are immutable
 * (and thus fully thread-safe); new instances are constructed for
 * different configurations.
 * Instances are initially constructed by {@link ObjectMapper}, and can
 * be similarly reused.
 * 
 * @author tatu
 * @since 1.5
 */
public class ObjectWriter
{
    /*
    /***************************************************
    /* Immutable configuration from ObjectMapper
    /***************************************************
     */

    /**
     * General serialization configuration settings
     */
    protected final SerializationConfig _config;
   
    protected final SerializerProvider _provider;

    protected final SerializerFactory _serializerFactory;

    /**
     * Factory used for constructing {@link JsonGenerator}s
     */
    protected final JsonFactory _jsonFactory;

    protected final JsonNodeFactory _nodeFactory;
    
    // Support for polymorphic types:
    protected TypeResolverBuilder<?> _defaultTyper;

    // Configurable visibility limits
    protected VisibilityChecker<?> _visibilityChecker;

    /*
    /***************************************************
    /* Configuration that can be changed during building
    /***************************************************
     */   
    
    /**
     * View to use for serialization
     */
    protected final Object _serializationView;

    /**
     * Specified root serialization type to use; can be same
     * as runtime type, but usually one of its super types
     */
    protected final JavaType _rootType;
    
    /*
    /***************************************************
    /* Life-cycle
    /***************************************************
     */

    /**
     * Constructor used by {@link ObjectMapper} for initial instantiation
     */
    protected ObjectWriter(ObjectMapper mapper,
            Object view, JavaType rootType)
    {
        _config = mapper._serializationConfig;
        _provider = mapper._serializerProvider;
        _serializerFactory = mapper._serializerFactory;

        _jsonFactory = mapper._jsonFactory;
        _nodeFactory = mapper._nodeFactory;
        _defaultTyper = mapper._defaultTyper;
        _visibilityChecker = mapper._visibilityChecker;
        
        _serializationView = view;
        _rootType = rootType;
    }

    /**
     * Copy constructor used for building variations.
     */
    protected ObjectWriter(ObjectWriter base, Object view, JavaType rootType)
    {
        _config = base._config;
        _provider = base._provider;
        _serializerFactory = base._serializerFactory;

        _jsonFactory = base._jsonFactory;
        _nodeFactory = base._nodeFactory;
        _defaultTyper = base._defaultTyper;
        _visibilityChecker = base._visibilityChecker;
        
        _serializationView = view;
        _rootType = rootType;
    }
    
    public ObjectWriter withView(Object view) {
        return new ObjectWriter(this, view, _rootType);
    }    
    
    public ObjectWriter withType(JavaType rootType)
    {
        return new ObjectWriter(this, _serializationView, rootType);
    }    

    public ObjectWriter withType(Class<?> rootType)
    {
        return withType(TypeFactory.type(rootType));
    }
    
    /*
    /***************************************************
    /* Serialization methods
    /***************************************************
     */
}
