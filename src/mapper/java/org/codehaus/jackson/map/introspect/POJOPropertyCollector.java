package org.codehaus.jackson.map.introspect;

/**
 * Helper class used for aggregating information about a single
 * potential POJO property.
 * 
 * @since 1.9
 */
public class POJOPropertyCollector
{
    /**
     * Name of logical property
     */
    protected final String _name;

    protected Node<AnnotatedField> _fields;
    
    protected Node<AnnotatedParameter> _ctorParameters;
    
    protected Node<AnnotatedMethod> _getters;

    protected Node<AnnotatedMethod> _setters;

    public POJOPropertyCollector(String name) {
        _name = name;
    }

    /*
    /**********************************************************
    /* Data aggregation
    /**********************************************************
     */

    public void addField(AnnotatedField a) {
        _fields = new Node<AnnotatedField>(a, _fields);
    }

    public void addCtor(AnnotatedParameter a) {
        _ctorParameters = new Node<AnnotatedParameter>(a, _ctorParameters);
    }

    public void addGetter(AnnotatedMethod a) {
        _getters = new Node<AnnotatedMethod>(a, _getters);
    }

    public void addSetter(AnnotatedMethod a) {
        _setters = new Node<AnnotatedMethod>(a, _setters);
    }
    
    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    /**
     * Node used for creating simple linked lists to efficiently store small sets
     * of things.
     */
    private final static class Node<T>
    {
        public final T value;
        public final Node<T> next;
        
        public Node(T v, Node<T> n) {
            value = v;
            next = n;
        }
    }
}
