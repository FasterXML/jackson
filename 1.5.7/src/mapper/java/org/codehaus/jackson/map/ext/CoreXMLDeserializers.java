package org.codehaus.jackson.map.ext;

import java.io.StringReader;
import java.util.*;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.deser.StdDeserializer;
import org.codehaus.jackson.map.deser.FromStringDeserializer;
import org.codehaus.jackson.map.util.Provider;

/**
 * Container deserializers that handle "core" XML types: ones included in standard
 * JDK 1.5. Types are directly needed by JAXB, and are thus supported within core
 * mapper package, not in "xc" package.
 *
 * @since 1.3
 */
public class CoreXMLDeserializers
    implements Provider<StdDeserializer<?>>
{
    /**
     * Data type factories are thread-safe after instantiation (and
     * configuration, if any); and since instantion (esp. implementation
     * introspection) can be expensive we better reuse the instance.
     */
    final static DatatypeFactory _dataTypeFactory;
    static {
        try {
            _dataTypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }
    
    /*
    ////////////////////////////////////////////////////////////////////////
    // Provider implementation
    ////////////////////////////////////////////////////////////////////////
    */

    /**
     * Method called by {@link org.codehaus.jackson.map.deser.BasicDeserializerFactory}
     * to register deserializers this class provides.
     */
    public Collection<StdDeserializer<?>> provide()
    {
        return Arrays.asList(new StdDeserializer<?>[] {
            new DurationDeserializer()
            ,new GregorianCalendarDeserializer()
            ,new QNameDeserializer()
            ,new DOMDocumentDeserializer()
            ,new DOMNodeDeserializer()
        });
    }
    
    /*
    ////////////////////////////////////////////////////////////////////////
    // Concrete deserializers
    ////////////////////////////////////////////////////////////////////////
    */

    public static class DurationDeserializer
        extends FromStringDeserializer<Duration>
    {
        public DurationDeserializer() { super(Duration.class); }
    
        protected Duration _deserialize(String value, DeserializationContext ctxt)
            throws IllegalArgumentException
        {
            return _dataTypeFactory.newDuration(value);
        }
    }

    public static class GregorianCalendarDeserializer
        extends FromStringDeserializer<XMLGregorianCalendar>
    {
        public GregorianCalendarDeserializer() { super(XMLGregorianCalendar.class); }
        
        protected XMLGregorianCalendar _deserialize(String value, DeserializationContext ctxt)
            throws IllegalArgumentException
        {
            return _dataTypeFactory.newXMLGregorianCalendar(value);
        }
    }

    public static class QNameDeserializer
        extends FromStringDeserializer<QName>
    {
        public QNameDeserializer() { super(QName.class); }
        
        protected QName _deserialize(String value, DeserializationContext ctxt)
            throws IllegalArgumentException
        {
            return QName.valueOf(value);
        }
    }

    /**
     * Base for serializers that allows parsing DOM Documents from JSON Strings.
     * Nominal type can be either {@link org.w3c.dom.Node} or
     * {@link org.w3c.dom.Document}.
     */
    abstract static class DOMDeserializer<T> extends FromStringDeserializer<T>
    {
        final static DocumentBuilderFactory _parserFactory;
        static {
            _parserFactory = DocumentBuilderFactory.newInstance();
            // yup, only cave men do XML without recognizing namespaces...
            _parserFactory.setNamespaceAware(true);
        }

        protected DOMDeserializer(Class<T> cls) { super(cls); }

        public abstract T _deserialize(String value, DeserializationContext ctxt);

        protected final Document parse(String value) throws IllegalArgumentException
        {
            try {
                return _parserFactory.newDocumentBuilder().parse(new InputSource(new StringReader(value)));
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse JSON String as XML: "+e.getMessage(), e);
            }
        }
    }
    
    public static class DOMNodeDeserializer extends DOMDeserializer<Node>
    {
        public DOMNodeDeserializer() { super(Node.class); }
        public Node _deserialize(String value, DeserializationContext ctxt) throws IllegalArgumentException {
            return parse(value);
        }
    }    

    public static class DOMDocumentDeserializer extends DOMDeserializer<Document>
    {
        public DOMDocumentDeserializer() { super(Document.class); }
        public Document _deserialize(String value, DeserializationContext ctxt) throws IllegalArgumentException {
            return parse(value);
        }
    }    
}
