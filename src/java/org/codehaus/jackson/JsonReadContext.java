package org.codehaus.jackson;

import org.codehaus.jackson.impl.JsonParserBase;
import org.codehaus.jackson.util.CharTypes;

/**
 * Context object is used to keep track of relative logical position
 * of the current event when parsing.
 */
public abstract class JsonReadContext
{
    protected final static int INT_COLON = ':';
    protected final static int INT_COMMA = ',';

    /**
     * Type of the context
     */
    public enum Type {
        ROOT, OBJECT, ARRAY;
    }

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

    protected final JsonReadContext mParent;

    /**
     * Index of the currently processed entry. Starts with -1 to signal
     * that no entries have been started, and gets advanced each
     * time a new entry is started, either by encountering an expected
     * separator, or with new values if no separators are expected
     * (the case for root context).
     */
    protected int mIndex;

    // // // Location information (minus source reference)

    long mTotalChars;

    int mLineNr;
    int mColumnNr;

    /*
    //////////////////////////////////////////////////
    // Simple instance reuse slots; speed up things
    // a bit (10-15%) for docs with lots of small
    // arrays/objects
    //////////////////////////////////////////////////
     */

    JsonReadContext mChildArray = null;

    JsonReadContext mChildObject = null;

    /*
    //////////////////////////////////////////////////
    // Life-cycle
    //////////////////////////////////////////////////
     */

    public JsonReadContext(JsonReadContext parent, JsonParserBase ir)
    {
        mParent = parent;
        mIndex = -1;
        mTotalChars = ir.getTokenCharacterOffset();
        mLineNr = ir.getTokenLineNr();
        mColumnNr = ir.getTokenColumnNr();
    }

    private final void resetLocation(JsonParserBase ir)
    {
        mIndex = -1;
        mTotalChars = ir.getTokenCharacterOffset();
        mLineNr = ir.getTokenLineNr();
        mColumnNr = ir.getTokenColumnNr();
    }

    // // // Factory methods

    public static JsonReadContext createRootContext(JsonParserBase ir)
    {
        return new RootRContext(ir);
    }

    public final JsonReadContext createChildArrayContext(JsonParserBase ir)
    {
        JsonReadContext ctxt = mChildArray;
        if (ctxt == null) {
            mChildArray = ctxt = new ArrayRContext(this, ir);
        } else {
            ctxt.resetLocation(ir);
        }
        return ctxt;
    }

    public final JsonReadContext createChildObjectContext(JsonParserBase ir)
    {
        JsonReadContext ctxt = mChildObject;
        if (ctxt == null) {
            mChildObject = ctxt = new ObjectRContext(this, ir);
        } else {
            ctxt.resetLocation(ir);
        }
        return ctxt;
    }

    // // // Shared API

    public final JsonReadContext getParent() { return mParent; }

    public final boolean isRoot() { return mParent == null; }

    /**
     * @return Number of entries that are complete and started.
     */
    public final int getEntryCount()
    {
        return mIndex+1;
    }

    /**
     * @return Index of the currently processed entry, if any
     */
    public final int getCurrentIndex()
    {
        return (mIndex < 0) ? 0 : mIndex;
    }

    /**
     * @return Location pointing to the point where the context
     *   start marker was found
     */
    public final JsonLocation getStartLocation(Object srcRef)
    {
        return new JsonLocation(srcRef, mTotalChars, mLineNr, mColumnNr);
    }

    // // // API sub-classes are to implement

    public abstract Type getType();
    public abstract boolean isArray();
    public abstract boolean isObject();

    public final String getTypeDesc() { return getType().toString(); }

    public abstract int handleSeparator(int ch);

    public abstract String getCurrentName();

    // // // Internally used abstract methods

    protected abstract void appendDesc(StringBuilder sb);

    /**
     * Method only to be called in the object context
     */
    public void setCurrentName(String name) {
        throw new IllegalStateException("Can not call setCurrentName() for "+getTypeDesc());
    }

    // // // Overridden standard methods

    /**
     * Overridden to provide developer readable "JsonPath" representation
     * of the context.
     */
    public final String toString()
    {
        StringBuilder sb = new StringBuilder(64);
        appendDesc(sb);
        return sb.toString();
    }
}

/**
 * Root context is simple, as only state it keeps is the index of
 * the currently active entry.
 */
final class RootRContext
    extends JsonReadContext
{
    public RootRContext(JsonParserBase ir)
    {
        super(null, ir);
    }

    public Type getType() { return Type.ROOT; }
    public boolean isArray() { return false; }
    public boolean isObject() { return false; }

    public String getCurrentName() { return null; }

    public int handleSeparator(int ch)
    {
        // Starting of a new entry is implied
        ++mIndex;
        return NOT_EXP_SEPARATOR_NEED_VALUE;
    }

    protected void appendDesc(StringBuilder sb)
    {
        sb.append("/");
    }
}

final class ArrayRContext
    extends JsonReadContext
{
    public ArrayRContext(JsonReadContext parent, JsonParserBase ir)
    {
        super(parent, ir);
    }

    public Type getType() { return Type.ARRAY; }
    public boolean isArray() { return true; }
    public boolean isObject() { return false; }

    public String getCurrentName() { return null; }

    /**
     * State handling for arrays is simple, the only consideration is
     * for the first entry, which does not take leading comma.
     */
    public int handleSeparator(int ch)
    {
        // New entry, first or not?
        int ix = mIndex;
        ++mIndex;
        if (ix < 0) {
            return NOT_EXP_SEPARATOR_NEED_VALUE;
        }
        // Other than first, must get comma first
        if (ch == INT_COMMA) {
            return HANDLED_EXPECT_VALUE;
        }
        return MISSING_COMMA;
    }

    protected void appendDesc(StringBuilder sb)
    {
        sb.append('[');
        sb.append(getCurrentIndex());
        sb.append(']');
    }
}

final class ObjectRContext
    extends JsonReadContext
{
    /**
     * Name of the field of which value is to be parsed.
     */
    protected String mCurrentName;

    /**
     * Flag to indicate that the context just received the
     * field name, and is to get a value next
     */
    protected boolean mExpectValue;

    public ObjectRContext(JsonReadContext parent, JsonParserBase ir)
    {
        super(parent, ir);
        mCurrentName = null;
        mExpectValue = false;
    }

    public Type getType() { return Type.OBJECT; }
    public boolean isArray() { return false; }
    public boolean isObject() { return true; }

    public String getCurrentName() { return mCurrentName; }

    /**
     * Objects ("maps") have the most complicated state handling, so
     * we get to return any of the constant, depending on exactly
     * where we are.
     */
    public int handleSeparator(int ch)
    {
        if (mExpectValue) { // have name, expecting ':' followed by value
            if (ch == INT_COLON) {
                mExpectValue = false;
                return HANDLED_EXPECT_VALUE;
            }
            return MISSING_COLON;
        }
        // New entry, entries start with name. But is it the first or not?
        if (mIndex < 0) { // First; no separator expected
            mExpectValue = true;
            return NOT_EXP_SEPARATOR_NEED_NAME;
        }
        // Other than first, must get comma first
        if (ch == INT_COMMA) {
            mExpectValue = true;
            return HANDLED_EXPECT_NAME;
        }
        return MISSING_COMMA;
    }

    @Override
    public void setCurrentName(String name)
    {
        mCurrentName = name;
        ++mIndex; // so that we can deal with comma
    }

    protected void appendDesc(StringBuilder sb)
    {
        sb.append('{');
        if (mCurrentName != null) {
            sb.append('"');
            CharTypes.appendQuoted(sb, mCurrentName);
            sb.append('"');
        } else {
            sb.append('?');
        }
        sb.append(']');
    }
}
