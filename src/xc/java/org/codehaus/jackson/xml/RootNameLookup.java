package org.codehaus.jackson.xml;

import javax.xml.namespace.QName;

import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.MapperConfig;
import org.codehaus.jackson.map.introspect.AnnotatedClass;
import org.codehaus.jackson.map.introspect.BasicBeanDescription;
import org.codehaus.jackson.map.type.ClassKey;
import org.codehaus.jackson.map.util.LRUMap;
import org.codehaus.jackson.type.JavaType;

/**
 * Helper class used for efficiently finding root element name used with
 * XML serializations.
 * 
 * @since 1.7
 */
public class RootNameLookup
{
    /**
     * For efficient operation, let's try to minimize number of times we
     * need to introspect root element name to use.
     */
    protected final LRUMap<ClassKey,QName> _rootNames = new LRUMap<ClassKey,QName>(40, 200);

    public RootNameLookup() { }

    public QName findRootName(JavaType rootType, MapperConfig<?> config)
    {
        return findRootName(rootType.getRawClass(), config);
    }

    public QName findRootName(Class<?> rootType, MapperConfig<?> config)
    {
        ClassKey key = new ClassKey(rootType);
        QName name;
        synchronized (_rootNames) {
            name = _rootNames.get(key);
            BasicBeanDescription beanDesc = (BasicBeanDescription) config.introspectClassAnnotations(rootType);
            AnnotationIntrospector intr = config.getAnnotationIntrospector();
            AnnotatedClass ac = beanDesc.getClassInfo();
            if (name == null) {
                String localName = intr.findRootName(ac);
                // No answer so far? Let's just default to using simple class name
                if (localName == null) {
                    // Should we strip out enclosing class tho? For now, nope:
                    localName = rootType.getSimpleName();
                    name = new QName("", localName);
                } else {
                    // Otherwise let's see if there's namespace, too
                    String ns = intr.findNamespace(ac);
                    if (ns == null) { // some QName impls barf on nulls...
                        ns = "";
                    }
                    name = new QName(ns, localName);
                }
                _rootNames.put(key, name);
            }
        }
        return name;
    }
}
