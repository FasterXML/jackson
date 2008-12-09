package org.codehaus.jackson.map.ser;

/**
 * Immutable key class, used as an efficient and accurate key
 * for locating per-class values, such as
 * {@link org.codehaus.jackson.map.JsonSerializer}s.
 *<p>
 * The reason for having a separate key class instead of
 * directly using {@link Class} as key is mostly
 * for efficiency when used as accessors within maps
 * (such as {@link java.util.HashMap}): the problem with
 * {link Class} is that its <code>hashCode</code> method
 * uses (slow) identity map.
 */
public final class ClassKey
    implements Comparable<ClassKey>
{
    final String _className;

    final Class<?> _class;

    /**
     * Let's cache hash code straight away, since we are
     * almost certain to need it.
     */
    final int _hashCode;

    public ClassKey(Class<?> clz)
    {
        _className = clz.getName();
        /* No way to easily get hash for class loader, but
         * shouldn't even need it.
         */
        _hashCode = _className.hashCode();
        _class = clz;
    }

    /*
    //////////////////////////////////////////////////
    // Comparable
    //////////////////////////////////////////////////
     */

    public int compareTo(ClassKey other)
    {
        // Just need to sort by name, ok to collide
        return _className.compareTo(other._className);
    }

    /*
    //////////////////////////////////////////////////
    // Standard methods
    //////////////////////////////////////////////////
     */

    @Override
        public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o.getClass() != getClass()) return false;
        ClassKey other = (ClassKey) o;

        /* 09-Dec-2008, tatu: Hmmh. Is it possible to have different Class object
         *   for same name + class loader combo? Let's assume answer is no: if this
         *   is wrong, will need to uncomment following functionality
         */
        /*
        return (other._className.equals(_className))
            && (other._class.getClassLoader() == _class.getClassLoader());
        */
        return other._class == _class;
    }

    @Override public int hashCode() { return _hashCode; }

    @Override public String toString() { return _className; }
    
}
