package org.codehaus.jackson.map.node;

import java.io.IOException;
import java.util.*;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonNode;

/**
 */
public final class ArrayNode
    extends ContainerNode
{
    ArrayList<JsonNode> mChildren;

    public ArrayNode() { }

    @Override
    public boolean isArray() { return true; }

    @Override
    public int size()
    {
        return (mChildren == null) ? 0 : mChildren.size();
    }

    @Override
        public JsonNode getElementValue(int index)
    {
        if (index >= 0 && (mChildren != null) && index < mChildren.size()) {
            return mChildren.get(index);
        }
        return null;
    }

    @Override
        public JsonNode getFieldValue(String fieldName) { return null; }

    @Override
    public Iterator<JsonNode> getElements()
    {
        return (mChildren == null) ? NoNodesIterator.instance() : mChildren.iterator();
    }

    @Override
        public JsonNode getPath(String fieldName) { return MissingNode.getInstance(); }

    @Override
    public JsonNode getPath(int index)
    {
        if (index >= 0 && (mChildren != null) && index < mChildren.size()) {
            return mChildren.get(index);
        }
        return MissingNode.getInstance();
    }

    public void appendElement(JsonNode node)
    {
        if (mChildren == null) {
            mChildren = new ArrayList<JsonNode>();
        }
        mChildren.add(node);
    }

    public void insertElement(int index, JsonNode value)
    {
        if (mChildren == null) {
            mChildren = new ArrayList<JsonNode>();
            mChildren.add(value);
            return;
        }

        if (index < 0) {
            mChildren.add(0, value);
        } else if (index >= mChildren.size()) {
            mChildren.add(value);
        } else {
            mChildren.add(index, value);
        }
    }

    public JsonNode removeElement(int index)
    {
        if (index >= 0 && (mChildren != null) && index < mChildren.size()) {
            return mChildren.remove(index);
        }
        return null;
    }

    public JsonNode removeElement(String fieldName)
    {
        return reportNoObjectMods();
    }

    public JsonNode setElement(int index, JsonNode value)
    {
        if (mChildren == null || index < 0 || index >= mChildren.size()) {
            throw new IndexOutOfBoundsException("Illegal index "+index+", array size "+size());
        }
        return mChildren.set(index, value);
    }

    public JsonNode setElement(String fieldName, JsonNode value)
    {
        return reportNoObjectMods();
    }

    public void writeTo(JsonGenerator jg)
        throws IOException, JsonGenerationException
    {
        jg.writeStartArray();
        if (mChildren != null) {
            for (JsonNode n : mChildren) {
                n.writeTo(jg);
            }
        }
        jg.writeEndArray();
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
        ArrayNode other = (ArrayNode) o;
        if (mChildren == null) {
            return other.mChildren == null;
        }
        return other.sameChildren(mChildren);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(16 + (size() << 4));
        sb.append('[');
        if (mChildren != null) {
            for (int i = 0, len = mChildren.size(); i < len; ++i) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(mChildren.get(i).toString());
            }
        }
        sb.append(']');
        return sb.toString();
    }

    /*
    ////////////////////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////////////////////
     */

    private boolean sameChildren(ArrayList<JsonNode> otherChildren)
    {
        int len = otherChildren.size();
        if (mChildren.size() != len) {
            return false;
        }
        for (int i = 0; i < len; ++i) {
            if (!mChildren.get(i).equals(otherChildren.get(i))) {
                return false;
            }
        }
        return true;
    }
}
