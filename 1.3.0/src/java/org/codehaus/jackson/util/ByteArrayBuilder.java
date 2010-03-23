/* Jackson JSON-processor.
 *
 * Copyright (c) 2007- Tatu Saloranta, tatu.saloranta@iki.fi
 *
 * Licensed under the License specified in file LICENSE, included with
 * the source code and binary code bundles.
 * You may not use this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.jackson.util;

import java.util.*;

/**
 * Helper class that is similar to {@link java.io.ByteArrayOutputStream}
 * in usage, but more geared to Jackson use cases internally.
 * Specific changes include segment storage (no need to have linear
 * backing buffer, can avoid reallocs, copying), as well API
 * not based on {@link java.io.OutputStream}. In short, a very much
 * specialized builder object.
 */
public final class ByteArrayBuilder
{
    private final static byte[] NO_BYTES = new byte[0];
    
    /**
     * Size of the first block we will allocate.
     */
    private final static int INITIAL_BLOCK_SIZE = 500;
    
    /**
     * Maximum block size we will use for individual non-aggregated
     * blocks. Let's limit to using 256k chunks.
     */
    private final static int MAX_BLOCK_SIZE = (1 << 18);
    
    final static int DEFAULT_BLOCK_ARRAY_SIZE = 40;

    private LinkedList<byte[]> _pastBlocks = new LinkedList<byte[]>();
    
    /**
     * Number of bytes within byte arrays in {@link _pastBlocks}.
     */
    private int _pastLen;

    private byte[] _currBlock;

    private int _currBlockPtr;
    
    public ByteArrayBuilder() { this(INITIAL_BLOCK_SIZE); }

    public ByteArrayBuilder(int firstBlockSize) {
        _currBlock = new byte[firstBlockSize];
    }

    public void reset()
    {
        _pastLen = 0;
        _currBlockPtr = 0;

        if (!_pastBlocks.isEmpty()) {
            _currBlock = _pastBlocks.getLast();
            _pastBlocks.clear();
        }
    }
    
    public void append(int i)
    {
        byte b = (byte) i;
        if (_currBlockPtr < _currBlock.length) {
            _currBlock[_currBlockPtr++] = b;
        } else { // let's off-line the longer part
            _allocMoreAndAppend(b);
        }
    }

    public void appendTwoBytes(int b16)
    {
        if ((_currBlockPtr + 1) < _currBlock.length) {
            _currBlock[_currBlockPtr++] = (byte) (b16 >> 8);
            _currBlock[_currBlockPtr++] = (byte) b16;
        } else {
            append(b16 >> 8);
            append(b16);
        }
    }

    public void appendThreeBytes(int b24)
    {
        if ((_currBlockPtr + 2) < _currBlock.length) {
            _currBlock[_currBlockPtr++] = (byte) (b24 >> 16);
            _currBlock[_currBlockPtr++] = (byte) (b24 >> 8);
            _currBlock[_currBlockPtr++] = (byte) b24;
        } else {
            append(b24 >> 16);
            append(b24 >> 8);
            append(b24);
        }
    }

    /**
     * Method called when results are finalized and we can get the
     * full aggregated result buffer to return to the caller
     */
    public byte[] toByteArray()
    {
        int totalLen = _pastLen + _currBlockPtr;
        
        if (totalLen == 0) { // quick check: nothing aggregated?
            return NO_BYTES;
        }
        
        byte[] result = new byte[totalLen];
        int offset = 0;

        for (byte[] block : _pastBlocks) {
            int len = block.length;
            System.arraycopy(block, 0, result, offset, len);
            offset += len;
        }
        System.arraycopy(_currBlock, 0, result, offset, _currBlockPtr);
        offset += _currBlockPtr;
        if (offset != totalLen) { // just a sanity check
            throw new RuntimeException("Internal error: total len assumed to be "+totalLen+", copied "+offset+" bytes");
        }
        // Let's only reset if there's sizable use, otherwise will get reset later on
        if (!_pastBlocks.isEmpty()) {
            reset();
        }
        return result;
    }

    private void _allocMoreAndAppend(byte b)
    {
        _pastLen += _currBlock.length;

        /* Let's allocate block that's half the total size, except
         * never smaller than twice the initial block size.
         * The idea is just to grow with reasonable rate, to optimize
         * between minimal number of chunks and minimal amount of
         * wasted space.
         */
        int newSize = Math.max((_pastLen >> 1), (INITIAL_BLOCK_SIZE + INITIAL_BLOCK_SIZE));
        // plus not to exceed max we define...
        if (newSize > MAX_BLOCK_SIZE) {
            newSize = MAX_BLOCK_SIZE;
        }
        _pastBlocks.add(_currBlock);
        _currBlock = new byte[newSize];
        _currBlockPtr = 0;
        _currBlock[_currBlockPtr++] = b;
    }
}

