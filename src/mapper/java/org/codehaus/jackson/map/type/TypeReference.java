package org.codehaus.jackson.map.type;

/**
 * This is the older deprecated version of
 * {@link org.codehaus.jackson.type.TypeReference}: please use the
 * newer version, as this one will get deleted before 1.0 release
 *
 * @deprecated
 */
@Deprecated
public abstract class TypeReference<T>
    extends org.codehaus.jackson.type.TypeReference<T>
{
    protected TypeReference() { super(); }
}

