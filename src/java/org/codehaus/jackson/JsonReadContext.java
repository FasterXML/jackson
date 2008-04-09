package org.codehaus.jackson;

import org.codehaus.jackson.util.CharTypes;

/**
 * Context object is used to keep track of relative logical position
 * of the current event when parsing, as well as current state regarding
 * separators and field name/value sequencing.
 *<p>
 * Note: implementation is bit ugly, as sub-classing is replaced by
 * state variables. This is done due to performance benefits; essentially
 * this allows for more aggeressive inlining by JVM.
 */
public final class JsonReadContext
{
    protected final static int INT_COLON = ':';
    protected final static int INT_COMMA = ',';

    private final static int TYPE_ROOT = 0;
    private final static int TYPE_ARRAY = 1;
    private final static int TYPE_OBJECT = 2;

    /*
    ////////////////////////////////////////////////////
    // Return codes for methods that verify which separator
    // is used for which kind of scope.
    // Reason for using ints over enums is that enum handling
    // appears slower than int handling for switch statements
    ////////////////////////////////////////////////////
     */

    public final static int HANDLED_EXPECT_NAME = 0;
    public final static int HANDLED_EXPECT_VALUE = 1;
    public final static int MISSING_COMMA = 2;
    public final static int MISSING_COLON = 3;
    public final static int NOT_EXP_SEPARATOR_NEED_VALUE = 4;
    public final static int NOT_EXP_SEPARATOR_NEED_NAME = 5;

    protected int mType;

    /**
     * Index of the currently processed entry. Starts with -1 to signal
     * that no entries have been started, and gets advanced each
     * time a new entry is started, either by encountering an expected
     * separator, or with new values if no separators are expected
     * (the case for root context).
     */
    protected int mIndex;

    // // // Location information (minus source reference)

    //long mTotalChars;

    protected int mLineNr;
    protected int mColumnNr;

    protected final JsonReadContext mParent;

    protected String mCurrentName;

    /*
    //////////////////////////////////////////////////
    // Simple instance reuse slots; speeds up things
    // a bit (10-15%) for docs with lots of small
    // arrays/objects (for which allocation was
    // visible in profile stack frames)
    //////////////////////////////////////////////////
     */

    JsonReadContext mChild = null;

    /*
    //////////////////////////////////////////////////
    // Life-cycle
    //////////////////////////////////////////////////
     */

    public JsonReadContext(int type, JsonReadContext parent,
                           int lineNr, int colNr)
    {
        mType = type;
        mParent = parent;
        mIndex = -1;
        mLineNr = lineNr;
        mColumnNr = colNr;
    }

    private final void reset(int type, int lineNr, int colNr)
    {
        mType = type;
        mIndex = -1;
        mLineNr = lineNr;
        mColumnNr = colNr;
        mCurrentName = null;
    }

    // // // Factory methods

    public static JsonReadContext createRootContext(int lineNr, int colNr)
    {
        return new JsonReadContext(TYPE_ROOT, null, lineNr, colNr);
    }

    public final JsonReadContext createChildArrayContext(int lineNr, int colNr)
    {
        JsonReadContext ctxt = mChild;
        if (ctxt == null) {
            return (mChild = new JsonReadContext(TYPE_ARRAY, this, lineNr, colNr));
        }
        ctxt.reset(TYPE_ARRAY, lineNr, colNr);
        return ctxt;
    }

    public final JsonReadContext createChildObjectContext(int lineNr, int colNr)
    {
        JsonReadContext ctxt = mChild;
        if (ctxt == null) {
            return (mChild = new JsonReadContext(TYPE_OBJECT, this, lineNr, colNr));
        }
        ctxt.reset(TYPE_OBJECT, lineNr, colNr);
        return ctxt;
    }

    // // // Accessors:

    public final JsonReadContext getParent() { return mParent; }

    /**
     * @return Number of entries that are complete and started.
     */
    public final int getEntryCount()
    {
        if (mType == TYPE_OBJECT) {
            return (mIndex >> 1) + 1;
        }
        return mIndex+1;
    }

    /**
     * @return Index of the currently processed entry, if any
     */
    public final int getCurrentIndex()
    {
        if (mIndex < 0) {
            return 0;
        }
        if (mType == TYPE_OBJECT) {
            return mIndex >> 1;
        }
        return mIndex;
    }

    /**
     * @return Location pointing to the point where the context
     *   start marker was found
     */
    public final JsonLocation getStartLocation(Object srcRef)
    {
        /* We don't keep track of offsets at this level (only
         * reader does)
         */
        long totalChars = -1L;

        return new JsonLocation(srcRef, totalChars, mLineNr, mColumnNr);
    }

    public final boolean isArray() { return mType == TYPE_ARRAY; }
    public final boolean isRoot() { return mType == TYPE_ROOT; }
    public final boolean isObject() { return mType == TYPE_OBJECT; }

    public final String getTypeDesc() {
        switch (mType) {
        case TYPE_ROOT: return "ROOT";
        case TYPE_ARRAY: return "ARRAY";
        case TYPE_OBJECT: return "OBJECT";
        }
        return "?";
    }

    public final String getCurrentName() { return mCurrentName; }

    // // // Workflow:

    public int handleSeparator(int ch)
    {
        int ix = ++mIndex;
        if (mType == TYPE_OBJECT) {
            if (ix == 0) {
                return NOT_EXP_SEPARATOR_NEED_NAME;
            }
            if ((ix & 1) == 0) { // expect name
                // Other than first, must get comma first
                if (ch == INT_COMMA) {
                    return HANDLED_EXPECT_NAME;
                }
                return MISSING_COMMA;
            }
            // Nope, need value
            if (ch == INT_COLON) {
                return HANDLED_EXPECT_VALUE;
            }
            return MISSING_COLON;
        }

        if (mType == TYPE_ARRAY) {
            // New entry, first or not?
            if (ix == 0) {
                return NOT_EXP_SEPARATOR_NEED_VALUE;
            }
            // Other than first, must get comma first
            if (ch == INT_COMMA) {
                return HANDLED_EXPECT_VALUE;
            }
            return MISSING_COMMA;
        }
        // Starting of a new entry is implied in root context
        return NOT_EXP_SEPARATOR_NEED_VALUE;
    }

    // // // Internally used abstract methods

    public void setCurrentName(String name)
    {
        mCurrentName = name;
    }

    // // // Overridden standard methods

    /**
     * Overridden to provide developer readable "JsonPath" representation
     * of the context.
     */
    public final String toString()
    {
        StringBuilder sb = new StringBuilder(64);
        switch (mType) {
        case TYPE_ROOT:
            sb.append("/");
            break;
        case TYPE_ARRAY:
            sb.append('[');
            sb.append(getCurrentIndex());
            sb.append(']');
            break;
        case TYPE_OBJECT:
            sb.append('{');
            if (mCurrentName != null) {
                sb.append('"');
                CharTypes.appendQuoted(sb, mCurrentName);
                sb.append('"');
            } else {
                sb.append('?');
            }
            sb.append(']');
            break;
        }
        return sb.toString();
    }
}
