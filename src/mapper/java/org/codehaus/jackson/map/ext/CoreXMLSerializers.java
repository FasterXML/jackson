package org.codehaus.jackson.map.ext;

import java.util.*;
import javax.xml.datatype.*;
import javax.xml.namespace.QName;
import org.w3c.dom.Node;

import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.ser.*;
import org.codehaus.jackson.map.util.Provider;

/**
 * Provider for serializers of XML types that are part of full JDK 1.5, but
 * that some alleged 1.5 platforms are missing (Android, GAE).
 * And for this reason these are added using more dynamic mechanism.
 *<p>
 * Note: since many of classes defined are abstract, caller must take
 * care not to just use straight equivalency check but rather consider
 * subclassing as well.
 *
 * @since 1.4
 */
public class CoreXMLSerializers
    implements Provider<Map.Entry<Class<?>,JsonSerializer<?>>>
{
    final static HashMap<Class<?>,JsonSerializer<?>> _serializers = new HashMap<Class<?>,JsonSerializer<?>>();
    /**
     * We will construct instances statically, during class loading, to try to
     * make things fail-fast, i.e. to catch problems as soon as possible.
     */
    static {
        ToStringSerializer tss = ToStringSerializer.instance;
        _serializers.put(Duration.class, tss);
        _serializers.put(XMLGregorianCalendar.class, tss);
        _serializers.put(QName.class, tss);
        _serializers.put(Node.class, new DOMSerializer());
    }
    
    public Collection<Map.Entry<Class<?>,JsonSerializer<?>>> provide() {
        return _serializers.entrySet();
    }
}
