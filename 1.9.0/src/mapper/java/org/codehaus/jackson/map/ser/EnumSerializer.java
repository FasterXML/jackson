package org.codehaus.jackson.map.ser;

import org.codehaus.jackson.map.annotate.JacksonStdImpl;
import org.codehaus.jackson.map.util.EnumValues;

/**
 * @deprecated Since 1.9 use {@link org.codehaus.jackson.map.ser.std.EnumSerializer}
 */
@Deprecated
@JacksonStdImpl
public class EnumSerializer
    extends org.codehaus.jackson.map.ser.std.EnumSerializer
{
    public EnumSerializer(EnumValues v) {
        super(v);
    }
}
