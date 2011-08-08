package org.codehaus.jackson.map.ser;

/**
 * @deprecated Since 1.9 use {@link org.codehaus.jackson.map.ser.std.StdKeySerializer} instead
 */
@Deprecated
public final class StdKeySerializer
    extends org.codehaus.jackson.map.ser.std.StdKeySerializer
{
    final static StdKeySerializer instace = new StdKeySerializer();
}
