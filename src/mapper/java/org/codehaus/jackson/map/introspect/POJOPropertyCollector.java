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

    public void addField(AnnotatedField a, String ename, boolean visible, boolean ignored) {
        _fields = new Node<AnnotatedField>(a, _fields, ename, visible, ignored);
    }

    public void addCtor(AnnotatedParameter a, String ename, boolean visible, boolean ignored) {
        _ctorParameters = new Node<AnnotatedParameter>(a, _ctorParameters, ename, visible, ignored);
    }

    public void addGetter(AnnotatedMethod a, String ename, boolean visible, boolean ignored) {
        _getters = new Node<AnnotatedMethod>(a, _getters, ename, visible, ignored);
    }

    public void addSetter(AnnotatedMethod a, String ename, boolean visible, boolean ignored) {
        _setters = new Node<AnnotatedMethod>(a, _setters, ename, visible, ignored);
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

        public final String explicitName;
        public final boolean isVisible;
        public final boolean isMarkedIgnored;
        
        public Node(T v, Node<T> n,
                String explName, boolean visible, boolean ignored)
        {
            value = v;
            next = n;
            explicitName = explName;
            isVisible = visible;
            isMarkedIgnored = ignored;
        }
    }
}
