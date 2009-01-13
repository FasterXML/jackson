package org.codehaus.jackson.map.deser;

/**
 * Base class for specialized primitive array builders.
 */
public abstract class PrimitiveArrayBuilder
{
    /**
     * Let's start with small chunks; typical usage is for small arrays anyway.
     */
    final static int INITIAL_CHUNK_SIZE = 12;

    /**
     * Also: let's expand by doubling up until 64k chunks (which is 16k entries for
     * 32-bit machines)
     */
    final static int SMALL_CHUNK_SIZE = (1 << 14);

    /**
     * Let's limit maximum size of chunks we use; helps avoid excessive allocation
     * overhead for huge data sets.
     * For now, let's limit to quarter million entries, 1 meg chunks for 32-bit
     * machines.
     */
    final static int MAX_CHUNK_SIZE = (1 << 18);

    // // // Data storage

    Node _bufferHead;

    Node _bufferTail;

    /**
     * Number of total buffered entries in this buffer, counting all instances
     * within linked list formed by following {@link #_bufferHead}.
     */
    int _bufferedEntryCount;

    // // // Recycled instances of sub-classes

    // // // Life-cycle

    protected PrimitiveArrayBuilder() { }

    protected abstract Object _constructArray(int len);

    /**
     * @return Length of the next chunk to allocate
     */
    public int _append(Object fullChunk, int fullChunkLength)
    {
        Node next = new Node(fullChunk, fullChunkLength);
        if (_bufferHead == null) { // first chunk
            _bufferHead = _bufferTail = next;
        } else { // have something already
            _bufferTail.linkNext(next);
            _bufferTail = next;
        }
        int len = fullChunkLength;
        _bufferedEntryCount += len;
        // double the size for small chunks
        if (len < SMALL_CHUNK_SIZE) {
            len += len;
        } else { // but by +25% for larger (to limit overhead)
            len += (len >> 2);
        }
        return len;
    }

    /*
    ////////////////////////////////////////////////////////////////////////
    // Implementation classes
    ////////////////////////////////////////////////////////////////////////
     */

    public final static class BooleanBuilder
        extends PrimitiveArrayBuilder
    {
        boolean[] _freeBuffer;

        public boolean[] resetAndStart()
        {
            _reset();
            return (_freeBuffer == null) ? new boolean[INITIAL_CHUNK_SIZE] : _freeBuffer;
        }

        @Override
        protected boolean[]  _constructArray(int len) { return new boolean[len]; }

        public boolean[] appendCompletedChunk(boolean[] fullChunk)
        {
            int nextLen = _append(fullChunk, fullChunk.length);
            return new boolean[nextLen];
        }

        /*
        public boolean[] build()
        {
            boolean[] result = new boolean[32];
            Node n = new Node(result);
            int ptr = 0;
            ptr = _bufferHead.copyData(result, ptr);
            return result;
        }
        */

        protected void _reset()
        {
        /*
            // can we reuse the last (and thereby biggest) array for next time?
            if (_bufferTail != null) {
                _freeBuffer = _bufferTail.getData();
            }
            // either way, must discard current contents
            _bufferHead = _bufferTail = null;
            _bufferedEntryCount = 0;
        */
        }
    }
    
    /*
    ////////////////////////////////////////////////////////////////////////
    // Helper classes
    ////////////////////////////////////////////////////////////////////////
     */

    /**
     * For actual buffering beyond the current buffer, we can actually
     * use shared class which only deals with opaque "untyped" chunks.
     * This works because {@link java.lang.System#arraycopy} does not
     * take type; hence we can implement some aspects of primitive data
     * handling in generic fashion.
     */
    final static class Node
    {
        /**
         * Data stored in this node.
         */
        final Object _data;

        /**
         * Number entries in the (untyped) array. Offset is assumed to be 0.
         */
        final int _dataLength;

        Node _next;

        public Node(Object data, int dataLen)
        {
            _data = data;
            _dataLength = dataLen;
        }

        public int copyData(Object dst, int ptr)
        {
            System.arraycopy(_data, 0, dst, ptr, _dataLength);
            ptr += _dataLength;
            return ptr;
        }

        public Node next() { return _next; }

        public void linkNext(Node next)
        {
            if (_next != null) { // sanity check
                throw new IllegalStateException();
            }
            _next = next;
        }
    }
}
