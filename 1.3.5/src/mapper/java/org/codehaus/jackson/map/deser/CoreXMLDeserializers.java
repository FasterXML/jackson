package org.codehaus.jackson.map.deser;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.codehaus.jackson.map.DeserializationContext;

/**
 * Container deserializers that handle "core" XML types: ones included in standard
 * JDK 1.5. Types are directly needed by JAXB, and are thus supported within core
 * mapper package, not in "xc" package.
 *
 * @since 1.3
 */
public class CoreXMLDeserializers
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
    // Concrete types
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
}
