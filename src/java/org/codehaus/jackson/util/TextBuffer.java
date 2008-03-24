package org.codehaus.jackson.util;

import java.io.*;
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

    private final BufferRecycler mAllocator;

    // // // Shared read-only input buffer:

    /**
     * Shared input buffer; stored here in case some input can be returned
     * as is, without being copied to collector's own buffers. Note that
     * this is read-only for this Objet.
     */
    private char[] mInputBuffer;

    /**
     * Character offset of first char in input buffer; -1 to indicate
     * that input buffer currently does not contain any useful char data
     */
    private int mInputStart;

    private int mInputLen;

    // // // Internal non-shared collector buffers:

    /**
     * List of segments prior to currently active segment.
     */
    private ArrayList<char[]> mSegments;


    // // // Currently used segment; not (yet) contained in mSegments

    /**
     * Amount of characters in segments in {@link mSegments}
     */
    private int mSegmentSize;

    private char[] mCurrentSegment;

    /**
     * Number of characters in currently active (last) segment
     */
    private int mCurrentSize;

    // // // Temporary caching for Objects to return

    /**
     * String that will be constructed when the whole contents are
     * needed; will be temporarily stored in case asked for again.
     */
    private String mResultString;

    private char[] mResultArray;

    /*
    //////////////////////////////////////////////
    // Life-cycle
    //////////////////////////////////////////////
     */

    public TextBuffer(BufferRecycler allocator)
    {
        mAllocator = allocator;
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
        if (mAllocator != null && mCurrentSegment != null) {
            // First, let's get rid of all but the largest char array
            resetWithEmpty();
            // And then return that array
            char[] buf = mCurrentSegment;
            mCurrentSegment = null;
            mAllocator.releaseCharBuffer(BufferRecycler.CharBufferType.TEXT_BUFFER, buf);
        }
    }

    /**
     * Method called to clear out any content text buffer may have, and
     * initializes buffer to use non-shared data.
     */
    public void resetWithEmpty()
    {
        mInputBuffer = null;
        mInputStart = -1; // indicates shared buffer not used
        mInputLen = 0;

        mResultString = null;
        mResultArray = null;

        // And then reset internal input buffers, if necessary:
        if (mSegments != null && mSegments.size() > 0) {
            /* Let's start using _last_ segment from list; for one, it's
             * the biggest one, and it's also most likely to be cached
             */
            mCurrentSegment = mSegments.get(mSegments.size() - 1);
            mSegments.clear();
            mSegmentSize = 0;
        }
        mCurrentSize = 0;
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
        mResultString = null;
        mResultArray = null;

        // Then let's mark things we need about input buffer
        mInputBuffer = buf;
        mInputStart = start;
        mInputLen = len;

        // And then reset internal input buffers, if necessary:
        if (mSegments != null && mSegments.size() > 0) {
            /* Let's start using _last_ segment from list; for one, it's
             * the biggest one, and it's also most likely to be cached
             */
            mCurrentSegment = mSegments.get(mSegments.size() - 1);
            mSegments.clear();
            mCurrentSize = mSegmentSize = 0;
        }
    }

    public void resetWithCopy(char[] buf, int start, int len)
    {
        mInputBuffer = null;
        mInputStart = -1; // indicates shared buffer not used
        mInputLen = 0;

        mResultString = null;
        mResultArray = null;

        // And then reset internal input buffers, if necessary:
        if (mSegments != null && mSegments.size() > 0) {
            /* Let's start using last segment from list; for one, it's
             * the biggest one, and it's also most likely to be cached
             */
            mCurrentSegment = mSegments.get(mSegments.size() - 1);
            mSegments.clear();
        }
        mCurrentSize = mSegmentSize = 0;
        append(buf, start, len);
    }

    public void resetWithString(String str)
    {
        // First things first, let's reset the buffer

        mInputBuffer = null;
        mInputStart = -1; // indicates shared buffer not used
        mInputLen = 0;

        mResultString = str;
        mResultArray = null;

        int len = str.length();

        if (mSegments != null && mSegments.size() > 0) {
            mCurrentSegment = mSegments.get(mSegments.size() - 1);
            mSegments.clear();
        } else if (mCurrentSegment == null) {
            mCurrentSegment = allocBuffer(len);
        }

        // Ok, but does the String fit? If not, need to realloc
        if (mCurrentSegment.length < len) {
            mCurrentSegment = new char[len];
        }
        str.getChars(0, len, mCurrentSegment, 0);
    }

    private final char[] allocBuffer(int needed)
    {
        return mAllocator.allocCharBuffer(BufferRecycler.CharBufferType.TEXT_BUFFER, needed);
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
        if (mInputStart >= 0) { // shared copy from input buf
            return mInputLen;
        }
        // local segmented buffers
        return mSegmentSize + mCurrentSize;
    }

    public int getTextOffset()
    {
        /* Only shared input buffer can have non-zero offset; buffer
         * segments start at 0, and if we have to create a combo buffer,
         * that too will start from beginning of the buffer
         */
        return (mInputStart >= 0) ? mInputStart : 0;
    }

    public char[] getTextBuffer()
    {
        // Are we just using shared input buffer?
        if (mInputStart >= 0) {
            return mInputBuffer;
        }
        // Nope; but does it fit in just one segment?
        if (mSegments == null || mSegments.size() == 0) {
            return mCurrentSegment;
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
        if (mResultString == null) {
            // Has array been requested? Can make a shortcut, if so:
            if (mResultArray != null) {
                mResultString = new String(mResultArray);
            } else {
                // Do we use shared array?
                if (mInputStart >= 0) {
                    if (mInputLen < 1) {
                        return (mResultString = "");
                    }
                    mResultString = new String(mInputBuffer, mInputStart, mInputLen);
                } else { // nope... need to copy
                    // But first, let's see if we have just one buffer
                    int segLen = mSegmentSize;
                    int currLen = mCurrentSize;
                    
                    if (segLen == 0) { // yup
                        mResultString = (currLen == 0) ? "" : new String(mCurrentSegment, 0, currLen);
                    } else { // no, need to combine
                        StringBuilder sb = new StringBuilder(segLen + currLen);
                        // First stored segments
                        if (mSegments != null) {
                            for (int i = 0, len = mSegments.size(); i < len; ++i) {
                                char[] curr = mSegments.get(i);
                                sb.append(curr, 0, curr.length);
                            }
                        }
                        // And finally, current segment:
                        sb.append(mCurrentSegment, 0, mCurrentSize);
                        mResultString = sb.toString();
                    }
                }
            }
        }
        return mResultString;
    }
 
    public char[] contentsAsArray()
    {
        char[] result = mResultArray;
        if (result == null) {
            mResultArray = result = buildResultArray();
        }
        return result;
    }

    public int contentsToArray(int srcStart, char[] dst, int dstStart, int len) {

        // Easy to copy from shared buffer:
        if (mInputStart >= 0) {

            int amount = mInputLen - srcStart;
            if (amount > len) {
                amount = len;
            } else if (amount < 0) {
                amount = 0;
            }
            if (amount > 0) {
                System.arraycopy(mInputBuffer, mInputStart+srcStart,
                                 dst, dstStart, amount);
            }
            return amount;
        }

        /* Could also check if we have array, but that'd only help with
         * braindead clients that get full array first, then segments...
         * which hopefully aren't that common
         */

        // Copying from segmented array is bit more involved:
        int totalAmount = 0;
        if (mSegments != null) {
            for (int i = 0, segc = mSegments.size(); i < segc; ++i) {
                char[] segment = mSegments.get(i);
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
            int maxAmount = mCurrentSize - srcStart;
            if (len > maxAmount) {
                len = maxAmount;
            }
            if (len > 0) { // should always be true
                System.arraycopy(mCurrentSegment, srcStart, dst, dstStart, len);
                totalAmount += len;
            }
        }

        return totalAmount;
    }

    /**
     * Method that will stream contents of this buffer into specified
     * Writer.
     */
    public int rawContentsTo(Writer w)
        throws IOException
    {
        // Let's first see if we have created helper objects:
        if (mResultArray != null) {
            w.write(mResultArray);
            return mResultArray.length;
        }
        if (mResultString != null) {
            w.write(mResultString);
            return mResultString.length();
        }

        // Do we use shared array?
        if (mInputStart >= 0) {
            if (mInputLen > 0) {
                w.write(mInputBuffer, mInputStart, mInputLen);
            }
            return mInputLen;
        }
        // Nope, need to do full segmented output
        int rlen = 0;
        if (mSegments != null) {
            for (int i = 0, len = mSegments.size(); i < len; ++i) {
                char[] ch = mSegments.get(i);
                w.write(ch);
                rlen += ch.length;
            }
        }
        if (mCurrentSize > 0) {
            w.write(mCurrentSegment, 0, mCurrentSize);
            rlen += mCurrentSize;
        }
        return rlen;
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
        if (mInputStart >= 0) {
            unshare(16);
        }
    }

    public void append(char[] c, int start, int len)
    {
        // Can't append to shared buf (sanity check)
        if (mInputStart >= 0) {
            unshare(len);
        }
        mResultString = null;
        mResultArray = null;

        // Room in current segment?
        char[] curr = mCurrentSegment;
        int max = curr.length - mCurrentSize;
            
        if (max >= len) {
            System.arraycopy(c, start, curr, mCurrentSize, len);
            mCurrentSize += len;
        } else {
            // No room for all, need to copy part(s):
            if (max > 0) {
                System.arraycopy(c, start, curr, mCurrentSize, max);
                start += max;
                len -= max;
            }
            // And then allocate new segment; we are guaranteed to now
            // have enough room in segment.
            expand(len); // note: curr != mCurrentSegment after this
            System.arraycopy(c, start, mCurrentSegment, 0, len);
            mCurrentSize = len;
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
        if (mInputStart >= 0) {
            unshare(1);
        } else {
            char[] curr = mCurrentSegment;
            if (curr == null) {
                mCurrentSegment = allocBuffer(0);
            } else if (mCurrentSize >= curr.length) {
                // Plus, we better have room for at least one more char
                expand(1);
            }
        }
        return mCurrentSegment;
    }

    public int getCurrentSegmentSize() {
        return mCurrentSize;
    }

    public void setCurrentLength(int len) {
        mCurrentSize = len;
    }

    public char[] finishCurrentSegment()
    {
        if (mSegments == null) {
            mSegments = new ArrayList<char[]>();
        }
        mSegments.add(mCurrentSegment);
        int oldLen = mCurrentSegment.length;
        mSegmentSize += oldLen;
        // Let's grow segments by 50%
        char[] curr = new char[oldLen + (oldLen >> 1)];
        mCurrentSize = 0;
        mCurrentSegment = curr;
        return curr;
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
        int sharedLen = mInputLen;
        mInputLen = 0;
        char[] inputBuf = mInputBuffer;
        mInputBuffer = null;
        int start = mInputStart;
        mInputStart = -1;

        // Is buffer big enough, or do we need to reallocate?
        int needed = sharedLen+needExtra;
        if (mCurrentSegment == null || needed > mCurrentSegment.length) {
            mCurrentSegment = allocBuffer(needed);
        }
        if (sharedLen > 0) {
            System.arraycopy(inputBuf, start, mCurrentSegment, 0, sharedLen);
        }
        mSegmentSize = 0;
        mCurrentSize = sharedLen;
    }

    /**
     * Method called when current segment is full, to allocate new
     * segment.
     */
    private void expand(int minNewSegmentSize)
    {
        // First, let's move current segment to segment list:
        if (mSegments == null) {
            mSegments = new ArrayList<char[]>();
        }
        char[] curr = mCurrentSegment;
        mSegments.add(curr);
        mSegmentSize += curr.length;
        int oldLen = curr.length;
        // Let's grow segments by 50% minimum
        int sizeAddition = oldLen >> 1;
        if (sizeAddition < minNewSegmentSize) {
            sizeAddition = minNewSegmentSize;
        }
        curr = new char[oldLen + sizeAddition];
        mCurrentSize = 0;
        mCurrentSegment = curr;
    }

    private char[] buildResultArray()
    {
        if (mResultString != null) { // Can take a shortcut...
            return mResultString.toCharArray();
        }
        char[] result;
        
        // Do we use shared array?
        if (mInputStart >= 0) {
            if (mInputLen < 1) {
                return NO_CHARS;
            }
            result = new char[mInputLen];
            System.arraycopy(mInputBuffer, mInputStart, result, 0,
                             mInputLen);
        } else { // nope 
            int size = size();
            if (size < 1) {
                return NO_CHARS;
            }
            int offset = 0;
            result = new char[size];
            if (mSegments != null) {
                for (int i = 0, len = mSegments.size(); i < len; ++i) {
                    char[] curr = (char[]) mSegments.get(i);
                    int currLen = curr.length;
                    System.arraycopy(curr, 0, result, offset, currLen);
                    offset += currLen;
                }
            }
            System.arraycopy(mCurrentSegment, 0, result, offset, mCurrentSize);
        }
        return result;
    }
}
