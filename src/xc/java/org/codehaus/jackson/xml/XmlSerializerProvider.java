package org.codehaus.jackson.xml;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.SerializerFactory;
import org.codehaus.jackson.map.ser.StdSerializerProvider;
import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.xml.util.RootNameLookup;

import javax.xml.namespace.QName;

/**
 * We need to override some parts of {@link org.codehaus.jackson.map.SerializerProvider}
 * implementation to handle oddities of XML output, like "extra" root element.
 * 
 * @since 1.7
 */
public class XmlSerializerProvider extends StdSerializerProvider
{
    /**
     * If all we get to serialize is a null, there's no way to figure out
     * expected root name; so let's just default to something like "<null>"...
     */
    protected final static QName ROOT_NAME_FOR_NULL = new QName("null");
    
    protected final RootNameLookup _rootNameLookup;
    
    protected XmlSerializerProvider(RootNameLookup rootNames)
    {
        super();
        _rootNameLookup = rootNames;
    }

    protected XmlSerializerProvider(SerializationConfig config, XmlSerializerProvider src,
            SerializerFactory f)
    {
        super(config, src, f);
        _rootNameLookup  = src._rootNameLookup;
    }
    
    /*
    /**********************************************************
    /* Overridden methods
    /**********************************************************
     */

    @Override
    protected StdSerializerProvider createInstance(SerializationConfig config, SerializerFactory jsf)
    {
        return new XmlSerializerProvider(config, this, jsf);
    }
    
    @Override
    protected  void _serializeValue(JsonGenerator jgen, Object value)
        throws IOException, JsonProcessingException
    {
        QName rootName = (value == null) ? ROOT_NAME_FOR_NULL
                : _rootNameLookup.findRootName(value.getClass(), _config);
        ((ToXmlGenerator) jgen).setNextName(rootName);
        super._serializeValue(jgen, value);
    }

    @Override
    protected  void _serializeValue(JsonGenerator jgen, Object value, JavaType rootType)
        throws IOException, JsonProcessingException
    {
        QName rootName = _rootNameLookup.findRootName(rootType, _config);
        ((ToXmlGenerator) jgen).setNextName(rootName);
        super._serializeValue(jgen, value, rootType);
    }
}
