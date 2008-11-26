package org.codehaus.jackson.map.node;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.codehaus.jackson.map.JsonNode;

/**
 * This intermediate base class is used for all container nodes,
 * specifically, array and object nodes.
 */
public abstract class ContainerNode
    extends JsonNode
{
    protected ContainerNode() { }

    @Override
    public boolean isContainerNode() { return true; }

    @Override
    public String getValueAsText() { return null; }

    /*
    ////////////////////////////////////////////////////
    // Methods reset as abstract to force real implementation
    ////////////////////////////////////////////////////
     */

    public abstract int size();

    public abstract JsonNode getElementValue(int index);

    public abstract JsonNode getFieldValue(String fieldName);

    public abstract void appendElement(JsonNode node);

    public abstract void insertElement(int index, JsonNode value);

    public abstract JsonNode removeElement(int index);

    public abstract JsonNode removeElement(String fieldName);

    public abstract JsonNode setElement(int index, JsonNode value);

    public abstract JsonNode setElement(String fieldName, JsonNode value);

    /*
    ////////////////////////////////////////////////////
    // Implementations of convenience methods
    ////////////////////////////////////////////////////
     */

    /*
    ////////////////////////////////////////////////////
    // Helper classes
    ////////////////////////////////////////////////////
     */

    protected static class NoNodesIterator
        implements Iterator<JsonNode>
    {
        final static NoNodesIterator sInstance = new NoNodesIterator();

        private NoNodesIterator() { }

        public static NoNodesIterator instance() { return sInstance; }

        public boolean hasNext() { return false; }
        public JsonNode next() { throw new NoSuchElementException(); }

        public void remove() {
            // could as well throw IllegalOperationException?
            throw new IllegalStateException();
        }
    }

    protected static class NoStringsIterator
        implements Iterator<String>
    {
        final static NoStringsIterator sInstance = new NoStringsIterator();

        private NoStringsIterator() { }

        public static NoStringsIterator instance() { return sInstance; }

        public boolean hasNext() { return false; }
        public String next() { throw new NoSuchElementException(); }

        public void remove() {
            // could as well throw IllegalOperationException?
            throw new IllegalStateException();
        }
    }
}
