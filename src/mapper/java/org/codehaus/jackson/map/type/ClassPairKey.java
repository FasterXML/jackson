package org.codehaus.jackson.map.type;

/**
 * Key class, used as an versatile and efficient key that
 * consists of one or two Classes,
 * usually combination of runtime and declared types.
 *<p>
 * Specialized key class is used for two reasons: first is
 * performance (pre-computing hash code), and the other
 * ability to reuse key if properly synchronized.
 *<p>
 * Note: since class is not immutable, caller must
 * know what it is doing, if changing field values  (Map
 * keys can never be modified that way for example)
 */
public final class ClassPairKey
{
     Class<?> _runtimeType;
     Class<?> _declaredType;

    /**
     * Let's cache hash code straight away, since we are
     * almost certain to need it.
     */
    int _hashCode;

    public ClassPairKey(Class<?> c1, Class<?> c2) {
        _runtimeType = c1;
        _declaredType = c2;
        reset(c1, c2);
    }

    public void reset(Class<?> runtimeType, Class<?> declaredType) {
        _hashCode = (runtimeType == null) ? 0 : runtimeType.getName().hashCode();
        if (declaredType != null) {
            _hashCode += declaredType.getName().hashCode();
        }
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
        ClassPairKey other = (ClassPairKey) o;

        return (other._runtimeType == _runtimeType) && (other._declaredType == _declaredType);
    }

    @Override public int hashCode() { return _hashCode; }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ClassPair(runtime:");
        sb.append((_runtimeType == null) ? "-" : _runtimeType.getName());
        sb.append(", declared:");
        sb.append((_declaredType == null) ? "-" : _declaredType.getName());
        sb.append(')');
        return sb.toString();        
    }
}
