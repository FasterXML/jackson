package org.codehaus.jackson.map.node;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.codehaus.jackson.map.JsonNode;

/**
 * This intermediate base class is used for all container nodes,
 * specifically, array and object nodes.
 */
public abstract class ContainerNode
    extends JsonNode
    implements NodeCreator
{
    /**
     * We will keep a reference to the Object (usually TreeMapper)
     * that can construct instances of nodes to add to this container
     * node.
     */
    NodeCreator _nodeCreator;

    protected ContainerNode(NodeCreator nc)
    {
        _nodeCreator = nc;
    }

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

    public abstract JsonNode get(int index);

    public abstract JsonNode get(String fieldName);

    /*
    ////////////////////////////////////////////////////
    // NodeCreator implementation, just dispatch to
    // the real creator
    ////////////////////////////////////////////////////
     */

    public final ArrayNode arrayNode() { return _nodeCreator.arrayNode(); }
    public final ObjectNode objectNode() { return _nodeCreator.objectNode(); }
    public final NullNode nullNode() { return _nodeCreator.nullNode(); }

    public final BooleanNode booleanNode(boolean v) { return _nodeCreator.booleanNode(v); }

    public final NumericNode numberNode(byte v) { return _nodeCreator.numberNode(v); }
    public final NumericNode numberNode(short v) { return _nodeCreator.numberNode(v); }
    public final NumericNode numberNode(int v) { return _nodeCreator.numberNode(v); }
    public final NumericNode numberNode(long v) { return _nodeCreator.numberNode(v); }
    public final NumericNode numberNode(float v) { return _nodeCreator.numberNode(v); }
    public final NumericNode numberNode(double v) { return _nodeCreator.numberNode(v); }
    public final NumericNode numberNode(BigDecimal v) { return _nodeCreator.numberNode(v); }

    public final TextNode textNode(String text) { return _nodeCreator.textNode(text); }

    public final BinaryNode binaryNode(byte[] data) { return _nodeCreator.binaryNode(data); }
    public final BinaryNode binaryNode(byte[] data, int offset, int length) { return _nodeCreator.binaryNode(data, offset, length); }


    /*
    ////////////////////////////////////////////////////
    // Helper classes
    ////////////////////////////////////////////////////
     */

    protected static class NoNodesIterator
        implements Iterator<JsonNode>
    {
        final static NoNodesIterator instance = new NoNodesIterator();

        private NoNodesIterator() { }

        public static NoNodesIterator instance() { return instance; }

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
        final static NoStringsIterator instance = new NoStringsIterator();

        private NoStringsIterator() { }

        public static NoStringsIterator instance() { return instance; }

        public boolean hasNext() { return false; }
        public String next() { throw new NoSuchElementException(); }

        public void remove() {
            // could as well throw IllegalOperationException?
            throw new IllegalStateException();
        }
    }
}
