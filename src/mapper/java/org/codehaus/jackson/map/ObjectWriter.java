package org.codehaus.jackson.map;

import java.io.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.io.SegmentedStringWriter;
import org.codehaus.jackson.map.introspect.VisibilityChecker;
import org.codehaus.jackson.map.jsontype.TypeResolverBuilder;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.util.ByteArrayBuilder;
import org.codehaus.jackson.util.DefaultPrettyPrinter;
import org.codehaus.jackson.util.MinimalPrettyPrinter;

/**
 * Builder object that can be used for per-serialization configuration of
 * serialization parameters, such as JSON View and root type to use.
 * Uses "fluent" (aka builder) pattern so that instances are immutable
 * (and thus fully thread-safe with no external synchronization);
 * new instances are constructed for different configurations.
 * Instances are initially constructed by {@link ObjectMapper} and can be
 * reused in completely thread-safe manner with no explicit synchronization
 * 
 * @author tatu
 * @since 1.5
 */
public class ObjectWriter
{
    /**
     * We need to keep track of explicit disabling of pretty printing;
     * easiest to do by a token value.
     */
    protected final static PrettyPrinter NULL_PRETTY_PRINTER = new MinimalPrettyPrinter();
    
    /*
    /**********************************************************
    /* Immutable configuration from ObjectMapper
    /**********************************************************
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
    
    // Support for polymorphic types:
    protected final TypeResolverBuilder<?> _defaultTyper;

    // Configurable visibility limits
    protected final VisibilityChecker<?> _visibilityChecker;

    /**
     * To allow for dynamic enabling/disabling of pretty printing,
     * pretty printer can be optionally configured for writer
     * as well
     */
    protected final PrettyPrinter _prettyPrinter;
    
    /*
    /**********************************************************
    /* Configuration that can be changed during building
    /**********************************************************
     */   
    
    /**
     * View to use for serialization
     */
    protected final Class<?> _serializationView;

    /**
     * Specified root serialization type to use; can be same
     * as runtime type, but usually one of its super types
     */
    protected final JavaType _rootType;
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    /**
     * Constructor used by {@link ObjectMapper} for initial instantiation
     */
    protected ObjectWriter(ObjectMapper mapper, 
            Class<?> view, JavaType rootType, PrettyPrinter pp)
    {
        _defaultTyper = mapper._defaultTyper;
        _visibilityChecker = mapper._visibilityChecker;
        // must make a copy at this point, to prevent further changes from trickling down
        _config = mapper._serializationConfig.createUnshared(_defaultTyper, _visibilityChecker);
        _config.setSerializationView(view);

        _provider = mapper._serializerProvider;
        _serializerFactory = mapper._serializerFactory;

        _jsonFactory = mapper._jsonFactory;
        
        _serializationView = view;
        _rootType = rootType;
        _prettyPrinter = pp;
    }

    /**
     * Copy constructor used for building variations.
     */
    protected ObjectWriter(ObjectWriter base, SerializationConfig config,
            Class<?> view, JavaType rootType, PrettyPrinter pp)
    {
        _config = config;
        _provider = base._provider;
        _serializerFactory = base._serializerFactory;

        _jsonFactory = base._jsonFactory;
        _defaultTyper = base._defaultTyper;
        _visibilityChecker = base._visibilityChecker;
        
        _serializationView = view;
        _rootType = rootType;
        _prettyPrinter = pp;
    }
    
    /**
     * Method that will construct a new instance that uses specified
     * serialization view for serialization (with null basically disables
     * view processing)
     */
    public ObjectWriter withView(Class<?> view)
    {
        if (view == _serializationView) return this;
        // View is included in config, must make immutable version
        SerializationConfig config = _config.createUnshared(_defaultTyper, _visibilityChecker);
        config.setSerializationView(view);
        return new ObjectWriter(this, config, view, _rootType, _prettyPrinter);
    }    
    
    /**
     * Method that will construct a new instance that uses specific type
     * as the root type for serialization, instead of runtime dynamic
     * type of the root object itself.
     */
    public ObjectWriter withType(JavaType rootType)
    {
        if (rootType == _rootType) return this;
        // type is stored here, no need to make a copy of config
        return new ObjectWriter(this, _config, _serializationView, rootType, _prettyPrinter);
    }    

    /**
     * Method that will construct a new instance that uses specific type
     * as the root type for serialization, instead of runtime dynamic
     * type of the root object itself.
     */
    public ObjectWriter withType(Class<?> rootType)
    {
        return withType(TypeFactory.type(rootType));
    }

    /**
     * Method that will construct a new instance that will use specified pretty
     * printer (or, if null, will not do any pretty-printing)
     * 
     * @since 1.6
     */
    public ObjectWriter withPrettyPrinter(PrettyPrinter pp)
    {
        // since null would mean "don't care", need to use placeholder to indicate "disable"
        if (pp == null) {
            pp = NULL_PRETTY_PRINTER;
        }
        return new ObjectWriter(this, _config, _serializationView, _rootType, pp);
    }

    /**
     * Method that will construct a new instance that will use the default
     * pretty printer for serialization.
     * 
     * @since 1.6
     */
    public ObjectWriter withDefaultPrettyPrinter()
    {
        return withPrettyPrinter(new DefaultPrettyPrinter());
    }
    
    /*
    /**********************************************************
    /* Serialization methods; ones from ObjectCodec first
    /**********************************************************
     */

    /**
     * Method that can be used to serialize any Java value as
     * JSON output, using provided {@link JsonGenerator}.
     */
    public void writeValue(JsonGenerator jgen, Object value)
        throws IOException, JsonGenerationException, JsonMappingException
    {
        if (_config.isEnabled(SerializationConfig.Feature.CLOSE_CLOSEABLE) && (value instanceof Closeable)) {
            _writeCloseableValue(jgen, value, _config);
        } else {
            _provider.serializeValue(_config, jgen, value, _serializerFactory);
            jgen.flush();
        }
    }

    /*
    /**********************************************************
    /* Serialization methods, others
    /**********************************************************
     */

    /**
     * Method that can be used to serialize any Java value as
     * JSON output, written to File provided.
     */
    public void writeValue(File resultFile, Object value)
        throws IOException, JsonGenerationException, JsonMappingException
    {
        _configAndWriteValue(_jsonFactory.createJsonGenerator(resultFile, JsonEncoding.UTF8), value);
    }

    /**
     * Method that can be used to serialize any Java value as
     * JSON output, using output stream provided (using encoding
     * {@link JsonEncoding#UTF8}).
     *<p>
     * Note: method does not close the underlying stream explicitly
     * here; however, {@link JsonFactory} this mapper uses may choose
     * to close the stream depending on its settings (by default,
     * it will try to close it when {@link JsonGenerator} we construct
     * is closed).
     */
    public void writeValue(OutputStream out, Object value)
        throws IOException, JsonGenerationException, JsonMappingException
    {
        _configAndWriteValue(_jsonFactory.createJsonGenerator(out, JsonEncoding.UTF8), value);
    }

    /**
     * Method that can be used to serialize any Java value as
     * JSON output, using Writer provided.
     *<p>
     * Note: method does not close the underlying stream explicitly
     * here; however, {@link JsonFactory} this mapper uses may choose
     * to close the stream depending on its settings (by default,
     * it will try to close it when {@link JsonGenerator} we construct
     * is closed).
     */
    public void writeValue(Writer w, Object value)
        throws IOException, JsonGenerationException, JsonMappingException
    {
        _configAndWriteValue(_jsonFactory.createJsonGenerator(w), value);
    }

    /**
     * Method that can be used to serialize any Java value as
     * a String. Functionally equivalent to calling
     * {@link #writeValue(Writer,Object)} with {@link java.io.StringWriter}
     * and constructing String, but more efficient.
     */
    public String writeValueAsString(Object value)
        throws IOException, JsonGenerationException, JsonMappingException
    {        
        // alas, we have to pull the recycler directly here...
        SegmentedStringWriter sw = new SegmentedStringWriter(_jsonFactory._getBufferRecycler());
        _configAndWriteValue(_jsonFactory.createJsonGenerator(sw), value);
        return sw.getAndClear();
    }
    
    /**
     * Method that can be used to serialize any Java value as
     * a byte array. Functionally equivalent to calling
     * {@link #writeValue(Writer,Object)} with {@link java.io.ByteArrayOutputStream}
     * and getting bytes, but more efficient.
     * Encoding used will be UTF-8.
     */
    public byte[] writeValueAsBytes(Object value)
        throws IOException, JsonGenerationException, JsonMappingException
    {        
        ByteArrayBuilder bb = new ByteArrayBuilder(_jsonFactory._getBufferRecycler());
        _configAndWriteValue(_jsonFactory.createJsonGenerator(bb, JsonEncoding.UTF8), value);
        byte[] result = bb.toByteArray();
        bb.release();
        return result;
    }
    
    /*
    /**********************************************************
    /* Other public methods
    /**********************************************************
     */

    public boolean canSerialize(Class<?> type)
    {
        return _provider.hasSerializerFor(_config, type, _serializerFactory);
    }

    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */
    
    /**
     * Method called to configure the generator as necessary and then
     * call write functionality
     */
    protected final void _configAndWriteValue(JsonGenerator jgen, Object value)
        throws IOException, JsonGenerationException, JsonMappingException
    {
        if (_prettyPrinter != null) {
            PrettyPrinter pp = _prettyPrinter;
            jgen.setPrettyPrinter((pp == NULL_PRETTY_PRINTER) ? null : pp);
        } else if (_config.isEnabled(SerializationConfig.Feature.INDENT_OUTPUT)) {
            jgen.useDefaultPrettyPrinter();
        }
        // [JACKSON-282]: consider Closeable
        if (_config.isEnabled(SerializationConfig.Feature.CLOSE_CLOSEABLE) && (value instanceof Closeable)) {
            _configAndWriteCloseable(jgen, value, _config);
            return;
        }
        boolean closed = false;
        try {
            if (_rootType == null) {
                _provider.serializeValue(_config, jgen, value, _serializerFactory);
            } else {
                _provider.serializeValue(_config, jgen, value, _rootType, _serializerFactory);                
            }
            closed = true;
            jgen.close();
        } finally {
            /* won't try to close twice; also, must catch exception (so it 
             * will not mask exception that is pending)
             */
            if (!closed) {
                try {
                    jgen.close();
                } catch (IOException ioe) { }
            }
        }
    }

    /**
     * Helper method used when value to serialize is {@link Closeable} and its <code>close()</code>
     * method is to be called right after serialization has been called
     */
    private final void _configAndWriteCloseable(JsonGenerator jgen, Object value, SerializationConfig cfg)
        throws IOException, JsonGenerationException, JsonMappingException
    {
        Closeable toClose = (Closeable) value;
        try {
            _provider.serializeValue(cfg, jgen, value, _serializerFactory);
            JsonGenerator tmpJgen = jgen;
            jgen = null;
            tmpJgen.close();
            Closeable tmpToClose = toClose;
            toClose = null;
            tmpToClose.close();
        } finally {
            /* Need to close both generator and value, as long as they haven't yet
             * been closed
             */
            if (jgen != null) {
                try {
                    jgen.close();
                } catch (IOException ioe) { }
            }
            if (toClose != null) {
                try {
                    toClose.close();
                } catch (IOException ioe) { }
            }
        }
    }
    
    /**
     * Helper method used when value to serialize is {@link Closeable} and its <code>close()</code>
     * method is to be called right after serialization has been called
     */
    private final void _writeCloseableValue(JsonGenerator jgen, Object value, SerializationConfig cfg)
        throws IOException, JsonGenerationException, JsonMappingException
    {
        Closeable toClose = (Closeable) value;
        try {
            _provider.serializeValue(cfg, jgen, value, _serializerFactory);
            jgen.flush();
            Closeable tmpToClose = toClose;
            toClose = null;
            tmpToClose.close();
        } finally {
            if (toClose != null) {
                try {
                    toClose.close();
                } catch (IOException ioe) { }
            }
        }
    }
}
