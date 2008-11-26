package org.codehaus.jackson.util;

import java.math.BigDecimal;
import java.util.ArrayList;

/**
 * TextBuffer is a class similar to {@link StringBuffer}, with
 * following differences:
 *<ul>
 *  <li>TextBuffer uses segments character arrays, to avoid having
 *     to do additional array copies when array is not big enough.
 *     This means that only reallocating that is necessary is done only once:
 *     if and when caller
 *     wants to access contents in a linear array (char[], String).
 *    </li>
*  <li>TextBuffer can also be initialized in "shared mode", in which
*     it will just act as a wrapper to a single char array managed
*     by another object (like parser that owns it)
 *    </li>
 *  <li>TextBuffer is not synchronized.
 *    </li>
 * </ul>
 */
public final class TextBuffer
{
    final static char[] NO_CHARS = new char[0];

    // // // Configuration:

    private final BufferRecycler _allocator;

    // // // Shared read-only input buffer:

    /**
     * Shared input buffer; stored here in case some input can be returned
     * as is, without being copied to collector's own buffers. Note that
     * this is read-only for this Objet.
     */
    private char[] _inputBuffer;

    /**
     * Character offset of first char in input buffer; -1 to indicate
     * that input buffer currently does not contain any useful char data
     */
    private int _inputStart;

    private int _inputLen;

    // // // Internal non-shared collector buffers:

    /**
     * List of segments prior to currently active segment.
     */
    private ArrayList<char[]> _segments;

    /**
     * Flag that indicates whether _seqments is non-empty
     */
    private boolean _hasSegments = false;

    // // // Currently used segment; not (yet) contained in _seqments

    /**
     * Amount of characters in segments in {@link _segments}
     */
    private int _segmentSize;

    private char[] _currentSegment;

    /**
     * Number of characters in currently active (last) segment
     */
    private int _currentSize;

    // // // Temporary caching for Objects to return

    /**
     * String that will be constructed when the whole contents are
     * needed; will be temporarily stored in case asked for again.
     */
    private String _resultString;

    private char[] _resultArray;

    /*
    //////////////////////////////////////////////
    // Life-cycle
    //////////////////////////////////////////////
     */

    public TextBuffer(BufferRecycler allocator)
    {
        _allocator = allocator;
    }

    /**
     * Method called to indicate that the underlying buffers should now
     * be recycled if they haven't yet been recycled. Although caller
     * can still use this text buffer, it is not advisable to call this
     * method if that is likely, since next time a buffer is needed,
     * buffers need to reallocated.
     * Note: calling this method automatically also clears contents
     * of the buffer.
     */
    public void releaseBuffers()
    {
        if (_allocator != null && _currentSegment != null) {
            // First, let's get rid of all but the largest char array
            resetWithEmpty();
            // And then return that array
            char[] buf = _currentSegment;
            _currentSegment = null;
            _allocator.releaseCharBuffer(BufferRecycler.CharBufferType.TEXT_BUFFER, buf);
        }
    }

    /**
     * Method called to clear out any content text buffer may have, and
     * initializes buffer to use non-shared data.
     */
    public void resetWithEmpty()
    {
        _inputBuffer = null;
        _inputStart = -1; // indicates shared buffer not used
        _inputLen = 0;

        _resultString = null;
        _resultArray = null;

        // And then reset internal input buffers, if necessary:
        if (_hasSegments) {
            clearSegments();
        }
        _currentSize = 0;
    }

    /**
     * Method called to initialize the buffer with a shared copy of data;
     * this means that buffer will just have pointers to actual data. It
     * also means that if anything is to be appended to the buffer, it
     * will first have to unshare it (make a local copy).
     */
    public void resetWithShared(char[] buf, int start, int len)
    {
        // First, let's clear intermediate values, if any:
        _resultString = null;
        _resultArray = null;

        // Then let's mark things we need about input buffer
        _inputBuffer = buf;
        _inputStart = start;
        _inputLen = len;

        // And then reset internal input buffers, if necessary:
        if (_hasSegments) {
            clearSegments();
        }
    }

    public void resetWithCopy(char[] buf, int start, int len)
    {
        _inputBuffer = null;
        _inputStart = -1; // indicates shared buffer not used
        _inputLen = 0;

        _resultString = null;
        _resultArray = null;

        // And then reset internal input buffers, if necessary:
        if (_hasSegments) {
            clearSegments();
        } else if (_currentSegment == null) {
            _currentSegment = allocBuffer(len);
        }
        _currentSize = _segmentSize = 0;
        append(buf, start, len);
    }

    /* 26-Nov-2008, tatu: not used currently; if not used in near future,
     *   let's just delete it.
     */
    /*
    public void resetWithString(String str)
    {
        // First things first, let's reset the buffer

        _inputBuffer = null;
        _inputStart = -1; // indicates shared buffer not used
        _inputLen = 0;

        _resultString = str;
        _resultArray = null;

        int len = str.length();

        if (_hasSegments) {
            _currentSegment = _segments.get(_segments.size() - 1);
            _segments.clear();
        } else if (_currentSegment == null) {
            _currentSegment = allocBuffer(len);
        }

        // Ok, but does the String fit? If not, need to realloc
        if (_currentSegment.length < len) {
            _currentSegment = new char[len];
        }
        str.getChars(0, len, _currentSegment, 0);
        _currentSize = len;
    }
    */

    private final char[] allocBuffer(int needed)
    {
        return _allocator.allocCharBuffer(BufferRecycler.CharBufferType.TEXT_BUFFER, needed);
    }

    private final void clearSegments()
    {
        _hasSegments = false;
        /* Let's start using _last_ segment from list; for one, it's
         * the biggest one, and it's also most likely to be cached
         */
        _currentSegment = _segments.get(_segments.size() - 1);
        _segments.clear();
        _currentSize = _segmentSize = 0;
    }

    /*
    //////////////////////////////////////////////
    // Accessors for implementing StAX interface:
    //////////////////////////////////////////////
     */

    /**
     * @return Number of characters currently stored by this collector
     */
    public int size() {
        if (_inputStart >= 0) { // shared copy from input buf
            return _inputLen;
        }
        // local segmented buffers
        return _segmentSize + _currentSize;
    }

    public int getTextOffset()
    {
        /* Only shared input buffer can have non-zero offset; buffer
         * segments start at 0, and if we have to create a combo buffer,
         * that too will start from beginning of the buffer
         */
        return (_inputStart >= 0) ? _inputStart : 0;
    }

    public char[] getTextBuffer()
    {
        // Are we just using shared input buffer?
        if (_inputStart >= 0) {
            return _inputBuffer;
        }
        // Nope; but does it fit in just one segment?
        if (!_hasSegments) {
            return _currentSegment;
        }
        // Nope, need to have/create a non-segmented array and return it
        return contentsAsArray();
    }

    /*
    //////////////////////////////////////////////
    // Accessors:
    //////////////////////////////////////////////
     */

    public String contentsAsString()
    {
        if (_resultString == null) {
            // Has array been requested? Can make a shortcut, if so:
            if (_resultArray != null) {
                _resultString = new String(_resultArray);
            } else {
                // Do we use shared array?
                if (_inputStart >= 0) {
                    if (_inputLen < 1) {
                        return (_resultString = "");
                    }
                    _resultString = new String(_inputBuffer, _inputStart, _inputLen);
                } else { // nope... need to copy
                    // But first, let's see if we have just one buffer
                    int segLen = _segmentSize;
                    int currLen = _currentSize;
                    
                    if (segLen == 0) { // yup
                        _resultString = (currLen == 0) ? "" : new String(_currentSegment, 0, currLen);
                    } else { // no, need to combine
                        StringBuilder sb = new StringBuilder(segLen + currLen);
                        // First stored segments
                        if (_segments != null) {
                            for (int i = 0, len = _segments.size(); i < len; ++i) {
                                char[] curr = _segments.get(i);
                                sb.append(curr, 0, curr.length);
                            }
                        }
                        // And finally, current segment:
                        sb.append(_currentSegment, 0, _currentSize);
                        _resultString = sb.toString();
                    }
                }
            }
        }
        return _resultString;
    }
 
    public char[] contentsAsArray()
    {
        char[] result = _resultArray;
        if (result == null) {
            _resultArray = result = buildResultArray();
        }
        return result;
    }

    /* 26-Nov-2008, tatu: not used currently; if not used in near future,
     *   let's just delete it.
     */
    /*
    public int contentsToArray(int srcStart, char[] dst, int dstStart, int len) {

        // Easy to copy from shared buffer:

        if (_inputStart >= 0) {

            int amount = _inputLen - srcStart;
            if (amount > len) {
                amount = len;
            } else if (amount < 0) {
                amount = 0;
            }
            if (amount > 0) {
                System.arraycopy(_inputBuffer, _inputStart+srcStart,
                                 dst, dstStart, amount);
            }
            return amount;
        }

        // Could also check if we have array, but that'd only help with
        // braindead clients that get full array first, then segments...
        // which hopefully aren't that common
        // Copying from segmented array is bit more involved:
        int totalAmount = 0;
        if (_segments != null) {
            for (int i = 0, segc = _segments.size(); i < segc; ++i) {
                char[] segment = _segments.get(i);
                int segLen = segment.length;
                int amount = segLen - srcStart;
                if (amount < 1) { // nothing from this segment?
                    srcStart -= segLen;
                    continue;
                }
                if (amount >= len) { // can get rest from this segment?
                    System.arraycopy(segment, srcStart, dst, dstStart, len);
                    return (totalAmount + len);
                }
                // Can get some from this segment, offset becomes zero:
                System.arraycopy(segment, srcStart, dst, dstStart, amount);
                totalAmount += amount;
                dstStart += amount;
                len -= amount;
                srcStart = 0;
            }
        }
        // Need to copy anything from last segment?
        if (len > 0) {
            int maxAmount = _currentSize - srcStart;
            if (len > maxAmount) {
                len = maxAmount;
            }
            if (len > 0) { // should always be true
                System.arraycopy(_currentSegment, srcStart, dst, dstStart, len);
                totalAmount += len;
            }
        }
        return totalAmount;
    }
    */

    /**
     * Method that will stream contents of this buffer into specified
     * Writer.
     */
    /* 26-Nov-2008, tatu: not used currently; if not used in near future,
     *   let's just delete it.
     */
    /*
    public int rawContentsTo(Writer w)
        throws IOException
    {
        // Let's first see if we have created helper objects:
        if (_resultArray != null) {
            w.write(_resultArray);
            return _resultArray.length;
        }
        if (_resultString != null) {
            w.write(_resultString);
            return _resultString.length();
        }

        // Do we use shared array?
        if (_inputStart >= 0) {
            if (_inputLen > 0) {
                w.write(_inputBuffer, _inputStart, _inputLen);
            }
            return _inputLen;
        }
        // Nope, need to do full segmented output
        int rlen = 0;
        if (_segments != null) {
            for (int i = 0, len = _segments.size(); i < len; ++i) {
                char[] ch = _segments.get(i);
                w.write(ch);
                rlen += ch.length;
            }
        }
        if (_currentSize > 0) {
            w.write(_currentSegment, 0, _currentSize);
            rlen += _currentSize;
        }
        return rlen;
    }
    */

    /**
     * Convenience method for converting contents of the buffer
     * into a {@link BigDecimal}.
     */
    public BigDecimal contentsAsDecimal()
        throws NumberFormatException
    {
        // Already got a pre-cut array?
        if (_resultArray != null) {
            return new BigDecimal(_resultArray);
        }
        // Or a shared buffer?
        if (_inputStart >= 0) {
            return new BigDecimal(_inputBuffer, _inputStart, _inputLen);
        }
        // Or if not, just a single buffer (the usual case)
        if (_segmentSize == 0) {
            return new BigDecimal(_currentSegment, 0, _currentSize);
        }
        // If not, let's just get it aggregated...
        return new BigDecimal(contentsAsArray());
    }

    /**
     * Convenience method for converting contents of the buffer
     * into a Double value.
     */
    public double contentsAsDouble()
        throws NumberFormatException
    {
        return Double.parseDouble(contentsAsString());
    }

    /*
    //////////////////////////////////////////////
    // Public mutators:
    //////////////////////////////////////////////
     */

    /**
     * Method called to make sure that buffer is not using shared input
     * buffer; if it is, it will copy such contents to private buffer.
     */
    public void ensureNotShared() {
        if (_inputStart >= 0) {
            unshare(16);
        }
    }

    public void append(char[] c, int start, int len)
    {
        // Can't append to shared buf (sanity check)
        if (_inputStart >= 0) {
            unshare(len);
        }
        _resultString = null;
        _resultArray = null;

        // Room in current segment?
        char[] curr = _currentSegment;
        int max = curr.length - _currentSize;
            
        if (max >= len) {
            System.arraycopy(c, start, curr, _currentSize, len);
            _currentSize += len;
        } else {
            // No room for all, need to copy part(s):
            if (max > 0) {
                System.arraycopy(c, start, curr, _currentSize, max);
                start += max;
                len -= max;
            }
            // And then allocate new segment; we are guaranteed to now
            // have enough room in segment.
            expand(len); // note: curr != _currentSegment after this
            System.arraycopy(c, start, _currentSegment, 0, len);
            _currentSize = len;
        }
    }

    /*
    //////////////////////////////////////////////
    // Raw access, for high-performance use:
    //////////////////////////////////////////////
     */

    public char[] getCurrentSegment()
    {
        /* Since the intention of the caller is to directly add stuff into
         * buffers, we should NOT have anything in shared buffer... ie. may
         * need to unshare contents.
         */
        if (_inputStart >= 0) {
            unshare(1);
        } else {
            char[] curr = _currentSegment;
            if (curr == null) {
                _currentSegment = allocBuffer(0);
            } else if (_currentSize >= curr.length) {
                // Plus, we better have room for at least one more char
                expand(1);
            }
        }
        return _currentSegment;
    }

    public char[] emptyAndGetCurrentSegment()
    {
        resetWithEmpty();
        char[] curr = _currentSegment;
        if (curr == null) {
            _currentSegment = curr = allocBuffer(0);
        }
        return curr;
    }

    public int getCurrentSegmentSize() {
        return _currentSize;
    }

    public void setCurrentLength(int len) {
        _currentSize = len;
    }

    public char[] finishCurrentSegment()
    {
        if (_segments == null) {
            _segments = new ArrayList<char[]>();
        }
        _hasSegments = true;
        _segments.add(_currentSegment);
        int oldLen = _currentSegment.length;
        _segmentSize += oldLen;
        // Let's grow segments by 50%
        char[] curr = new char[oldLen + (oldLen >> 1)];
        _currentSize = 0;
        _currentSegment = curr;
        return curr;
    }

    /**
     * Method called to expand size of the current segment, to
     * accomodate for more contiguous content. Usually only
     * used when parsing tokens like names.
     */
    public char[] expandCurrentSegment()
    {
        char[] curr = _currentSegment;
        // Let's just double right away
        int len = curr.length;
        _currentSegment = new char[len + len];
        System.arraycopy(curr, 0, _currentSegment, 0, len);
        return _currentSegment;
    }

    /*
    //////////////////////////////////////////////
    // Standard methods:
    //////////////////////////////////////////////
     */

    /**
     * Note: calling this method may not be as efficient as calling
     * {@link #contentsAsString}, since it's not guaranteed that resulting
     * String is cached.
     */
    public String toString() {
         return contentsAsString();
    }

    /*
    //////////////////////////////////////////////
    // Internal methods:
    //////////////////////////////////////////////
     */

    /**
     * Method called if/when we need to append content when we have been
     * initialized to use shared buffer.
     */
    private void unshare(int needExtra)
    {
        int sharedLen = _inputLen;
        _inputLen = 0;
        char[] inputBuf = _inputBuffer;
        _inputBuffer = null;
        int start = _inputStart;
        _inputStart = -1;

        // Is buffer big enough, or do we need to reallocate?
        int needed = sharedLen+needExtra;
        if (_currentSegment == null || needed > _currentSegment.length) {
            _currentSegment = allocBuffer(needed);
        }
        if (sharedLen > 0) {
            System.arraycopy(inputBuf, start, _currentSegment, 0, sharedLen);
        }
        _segmentSize = 0;
        _currentSize = sharedLen;
    }

    /**
     * Method called when current segment is full, to allocate new
     * segment.
     */
    private void expand(int minNewSegmentSize)
    {
        // First, let's move current segment to segment list:
        if (_segments == null) {
            _segments = new ArrayList<char[]>();
        }
        char[] curr = _currentSegment;
        _hasSegments = true;
        _segments.add(curr);
        _segmentSize += curr.length;
        int oldLen = curr.length;
        // Let's grow segments by 50% minimum
        int sizeAddition = oldLen >> 1;
        if (sizeAddition < minNewSegmentSize) {
            sizeAddition = minNewSegmentSize;
        }
        curr = new char[oldLen + sizeAddition];
        _currentSize = 0;
        _currentSegment = curr;
    }

    private char[] buildResultArray()
    {
        if (_resultString != null) { // Can take a shortcut...
            return _resultString.toCharArray();
        }
        char[] result;
        
        // Do we use shared array?
        if (_inputStart >= 0) {
            if (_inputLen < 1) {
                return NO_CHARS;
            }
            result = new char[_inputLen];
            System.arraycopy(_inputBuffer, _inputStart, result, 0,
                             _inputLen);
        } else { // nope 
            int size = size();
            if (size < 1) {
                return NO_CHARS;
            }
            int offset = 0;
            result = new char[size];
            if (_segments != null) {
                for (int i = 0, len = _segments.size(); i < len; ++i) {
                    char[] curr = (char[]) _segments.get(i);
                    int currLen = curr.length;
                    System.arraycopy(curr, 0, result, offset, currLen);
                    offset += currLen;
                }
            }
            System.arraycopy(_currentSegment, 0, result, offset, _currentSize);
        }
        return result;
    }
}
