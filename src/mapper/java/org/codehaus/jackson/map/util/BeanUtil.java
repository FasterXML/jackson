package org.codehaus.jackson.map.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.introspect.AnnotatedClass;
import org.codehaus.jackson.map.introspect.BasicBeanDescription;

/**
 * Helper class that contains functionality needed by both serialization
 * and deserialization side.
 *
 * @since 1.9
 */
public class BeanUtil
{
    /**
     * Helper method to use for sorting bean properties, based on
     * ordering rules indicated by annotations, config features.
     * 
     * @param config Serialization/Deserialization configuration in effect
     * @param beanDesc Bean description
     * @param props Properties to sort if/as necessary
     * @param defaultSortByAlpha Whether properties should be (re)sorted alphabetically
     *   by default (unless overridden by type)
     */
    public static <T extends Named> List<T> sortProperties(MapperConfig<?> config,
            BasicBeanDescription beanDesc, List<T> props,
            boolean defaultSortByAlpha)
    {
        /* First, order by [JACKSON-90] (explicit ordering and/or alphabetic)
         * and then for [JACKSON-170] (implicitly order creator properties before others)
         */
        List<String> creatorProps = beanDesc.findCreatorPropertyNames();
        // Then how about explicit ordering?
        AnnotationIntrospector intr = config.getAnnotationIntrospector();
        AnnotatedClass ac = beanDesc.getClassInfo();
        String[] propertyOrder = intr.findSerializationPropertyOrder(ac);
        Boolean alpha = intr.findSerializationSortAlphabetically(ac);
        boolean sort;
        
        if (alpha == null) {
            sort = defaultSortByAlpha;
        } else {
            sort = alpha.booleanValue();
        }
        // no sorting? no need to shuffle, then
        if (!sort && creatorProps.isEmpty() && propertyOrder == null) {
            return props;
        }
        int size = props.size();
        Map<String,T> all;
        // Need to (re)sort alphabetically?
        if (sort) {
            all = new TreeMap<String,T>();
        } else {
            all = new LinkedHashMap<String,T>(size+size);
        }

        for (T w : props) {
            all.put(w.getName(), w);
        }
        Map<String,T> ordered = new LinkedHashMap<String,T>(size+size);
        // Ok: primarily by explicit order
        if (propertyOrder != null) {
            for (String name : propertyOrder) {
                T w = all.get(name);
                if (w != null) {
                    ordered.put(name, w);
                }
            }
        }
        // And secondly by sorting Creator properties before other unordered properties
        for (String name : creatorProps) {
            T w = all.get(name);
            if (w != null) {
                ordered.put(name, w);
            }
        }
        // And finally whatever is left (trying to put again will not change ordering)
        ordered.putAll(all);
        return new ArrayList<T>(ordered.values());
    }

}
