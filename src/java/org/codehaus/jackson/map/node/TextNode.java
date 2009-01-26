package org.codehaus.jackson.map.node;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.util.CharTypes;

/**
 * Value node that contains a text value.
 */
public final class TextNode
    extends ValueNode
{
    final static TextNode EMPTY_STRING_NODE = new TextNode("");

    final String mValue;

    public TextNode(String v) { mValue = v; }

    /**
     * Factory method that should be used to construct instances.
     * For some common cases, can reuse canonical instances: currently
     * this is the case for empty Strings, in future possible for
     * others as well. If null is passed, will return null.
     *
     * @return Resulting {@link TextNode} object, if <b>v</b>
     *   is NOT null; null if it is.
     */
    public static TextNode valueOf(String v)
    {
        if (v == null) {
            return null;
        }
        if (v.length() == 0) {
            return EMPTY_STRING_NODE;
        }
        return new TextNode(v);
    }

    @Override
    public boolean isTextual() { return true; }

    @Override
    public String getTextValue() {
        return mValue;
    }

    public String getValueAsText() {
        return mValue;
    }

    public void writeTo(JsonGenerator jg)
        throws IOException, JsonGenerationException
    {
        jg.writeString(mValue);
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o.getClass() != getClass()) { // final class, can do this
            return false;
        }
        return ((TextNode) o).mValue == mValue;
    }

    @Override
        public int hashCode() { return mValue.hashCode(); }

    /**
     * Different from other values, Strings need quoting
     */
    @Override
    public String toString()
    {
        int len = mValue.length();
        len = len + 2 + (len >> 4);
        StringBuilder sb = new StringBuilder(len);
        appendQuoted(sb, mValue);
        return sb.toString();
    }

    protected static void appendQuoted(StringBuilder sb, String content)
    {
        sb.append('"');
        CharTypes.appendQuoted(sb, content);
        sb.append('"');
    }
}
