package org.codehaus.jackson.map.node;

import java.io.IOException;
import java.util.*;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonNode;

/**
 */
public final class ObjectNode
    extends ContainerNode
{
    LinkedHashMap<String, JsonNode> mChildren = null;

    public ObjectNode() { }

    @Override
    public boolean isObject() { return true; }

    @Override
    public int size() {
        return (mChildren == null) ? 0 : mChildren.size();
    }

    @Override
        public JsonNode getElementValue(int index) { return null; }

    @Override
        public JsonNode getFieldValue(String fieldName)
    {
        if (mChildren != null) {
            return mChildren.get(fieldName);
        }
        return null;
    }

    @Override
    public Iterator<String> getFieldNames()
    {
        return (mChildren == null) ? NoStringsIterator.instance() : mChildren.keySet().iterator();
    }

    @Override
    public Iterator<JsonNode> getFieldValues()
    {
        return (mChildren == null) ? NoNodesIterator.instance() : mChildren.values().iterator();
    }

    @Override
    public JsonNode getPath(int index)
    {
        return MissingNode.getInstance();
    }

    @Override
        public JsonNode getPath(String fieldName)
    {
        if (mChildren != null) {
            JsonNode n = mChildren.get(fieldName);
            if (n != null) {
                return n;
            }
        }
        return MissingNode.getInstance();
    }

    public void appendElement(JsonNode node)
    {
        reportNoArrayMods();
    }

    public void insertElement(int index, JsonNode value)
    {
        reportNoArrayMods();
    }

    public JsonNode removeElement(int index)
    {
        return reportNoArrayMods();
    }

    public JsonNode removeElement(String fieldName)
    {
        if (mChildren != null) {
            return mChildren.remove(fieldName);
        }
        return null;
    }

    public JsonNode setElement(int index, JsonNode value)
    {
        return reportNoArrayMods();
    }

    public JsonNode setElement(String fieldName, JsonNode value)
    {
        if (mChildren == null) {
            mChildren = new LinkedHashMap<String, JsonNode>();
        }
        return mChildren.put(fieldName, value);
    }

    public void writeTo(JsonGenerator jg)
        throws IOException, JsonGenerationException
    {
        jg.writeStartObject();
        if (mChildren != null) {
            for (Map.Entry<String, JsonNode> en : mChildren.entrySet()) {
                jg.writeFieldName(en.getKey());
                en.getValue().writeTo(jg);
            }
        }
        jg.writeEndObject();
    }

    /*
    ////////////////////////////////////////////////////////
    // Standard methods
    ////////////////////////////////////////////////////////
     */

    public boolean equals(Object o)
    {
        if (o == this) {
            return true;
        }
        if (o.getClass() != getClass()) {
            return false;
        }
        ObjectNode other = (ObjectNode) o;
        if (other.size() != size()) {
            return false;
        }
        if (mChildren != null) {
            for (Map.Entry<String, JsonNode> en : mChildren.entrySet()) {
                String key = en.getKey();
                JsonNode value = en.getValue();

                JsonNode otherValue = other.getFieldValue(key);

                if (otherValue == null || !otherValue.equals(value)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(32 + (size() << 4));
        sb.append("{");
        if (mChildren != null) {
            int count = 0;
            for (Map.Entry<String, JsonNode> en : mChildren.entrySet()) {
                if (count > 0) {
                    sb.append(",");
                }
                ++count;
                TextNode.appendQuoted(sb, en.getKey());
                sb.append(':');
                sb.append(en.getValue().toString());
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
