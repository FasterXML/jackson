package org.codehaus.jackson.map.ser.impl;

import java.util.*;

import org.codehaus.jackson.map.ser.*;

/**
 * Simple {@link FilterProvider} implementation that just stores
 * direct id-to-filter mapping.
 */
public class SimpleFilterProvider extends FilterProvider
{
    /**
     * Mappings from ids to filters.
     */
    protected final Map<String,BeanPropertyFilter> _filtersById;

    public SimpleFilterProvider() {
        _filtersById = new HashMap<String,BeanPropertyFilter>();
    }

    /**
     * @param mapping Mapping from id to filter; used as is, no copy is made.
     */
    public SimpleFilterProvider(Map<String,BeanPropertyFilter> mapping) {
        _filtersById = new HashMap<String,BeanPropertyFilter>();
    }

    public SimpleFilterProvider addFilter(String id, BeanPropertyFilter filter) {
        _filtersById.put(id, filter);
        return this;
    }

    public BeanPropertyFilter removeFilter(String id) {
        return _filtersById.remove(id);
    }
    
    @Override
    public BeanPropertyFilter findFilter(Object filterId)
    {
        return _filtersById.get(filterId);
    }

}
