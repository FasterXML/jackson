package org.codehaus.jackson.map.ext;

import java.util.*;
import javax.xml.datatype.*;
import javax.xml.namespace.QName;

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
    public Collection<Map.Entry<Class<?>,JsonSerializer<?>>> provide() {
        HashMap<Class<?>,JsonSerializer<?>> sers = new HashMap<Class<?>,JsonSerializer<?>>();
        ToStringSerializer tss = ToStringSerializer.instance;
        sers.put(Duration.class, tss);
        sers.put(XMLGregorianCalendar.class, tss);
        sers.put(QName.class, tss);
        return sers.entrySet();
    }
}
