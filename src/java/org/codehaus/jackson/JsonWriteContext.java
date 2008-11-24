package org.codehaus.jackson;

/**
 * Context object is used to keep track of relative logical position
 * of the current event when generating json content.
 */
public abstract class JsonWriteContext
{
    /**
     * Type of the context
     */
    public enum Type {
        ROOT, OBJECT, ARRAY;
    }

    // // // Return values for writeValue()

    public final static int STATUS_OK_AS_IS = 0;
    public final static int STATUS_OK_AFTER_COMMA = 1;
    public final static int STATUS_OK_AFTER_COLON = 2;
    public final static int STATUS_OK_AFTER_SPACE = 3; // in root context
    public final static int STATUS_EXPECT_VALUE = 4;
    public final static int STATUS_EXPECT_NAME = 5;

    protected final JsonWriteContext _parent;

    /**
     * Index of the currently processed entry. Starts with -1 to signal
     * that no entries have been started, and gets advanced each
     * time a new entry is started.
     */
    protected int _index;

    /*
    //////////////////////////////////////////////////
    // Simple instance reuse slots; speed up things
    // a bit (10-15%) for docs with lots of small
    // arrays/objects
    //////////////////////////////////////////////////
     */

    JsonWriteContext _childArray = null;

    JsonWriteContext _childObject = null;

    /*
    //////////////////////////////////////////////////
    // Life-cycle
    //////////////////////////////////////////////////
     */

    public JsonWriteContext(JsonWriteContext parent)
    {
        _parent = parent;
        _index = -1;
    }

    // // // Factory methods

    public static JsonWriteContext createRootContext()
    {
        return new RootWContext();
    }

    public final JsonWriteContext createChildArrayContext()
    {
        JsonWriteContext ctxt = _childArray;
        if (ctxt == null) {
            _childArray = ctxt = new ArrayWContext(this);
        } else { // need to reset settings; parent is already ok
            ctxt._index = -1;
        }
        return ctxt;
    }

    public final JsonWriteContext createChildObjectContext()
    {
        JsonWriteContext ctxt = _childObject;
        if (ctxt == null) {
            _childObject = ctxt = new ObjectWContext(this);
        } else { // need to reset settings; parent is already ok
            ctxt._index = -1;
        }
        return ctxt;
    }

    // // // Shared API

    public final JsonWriteContext getParent() { return _parent; }

    public final boolean isRoot() { return _parent == null; }

    public final int getEntryCount()
    {
        return _index+1;
    }

    /**
     * @return Index of the currently processed entry, if any
     */
    public final int getCurrentIndex()
    {
        return (_index < 0) ? 0 : _index;
    }

    // // // API sub-classes are to implement

    public abstract Type getType();

    /**
     * Method that writer is to call before it writes a field name.
     *
     * @return Index of the field entry (0-based)
     */
    public abstract int writeFieldName(String name);

    public abstract int writeValue();

    public boolean inArray() { return false; }

    public boolean inObject() { return false; }

    // // // Internally used abstract methods

    protected abstract void appendDesc(StringBuilder sb);

    // // // Overridden standard methods

    /**
     * Overridden to provide developer writeable "JsonPath" representation
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
final class RootWContext
    extends JsonWriteContext
{
    public RootWContext()
    {
        super(null);
    }

    public Type getType() { return Type.ROOT; }

    public String getCurrentName() { return null; }

    public int writeFieldName(String name)
    {
        return STATUS_EXPECT_VALUE;
    }

    public int writeValue()
    {
        // No commas within root context, but need space
        ++_index;
        return (_index == 0) ? STATUS_OK_AS_IS : STATUS_OK_AFTER_SPACE;
    }

    protected void appendDesc(StringBuilder sb)
    {
        sb.append("/");
    }
}

final class ArrayWContext
    extends JsonWriteContext
{
    public ArrayWContext(JsonWriteContext parent)
    {
        super(parent);
    }

    public Type getType() { return Type.ARRAY; }

    public String getCurrentName() { return null; }

    public int writeFieldName(String name)
    {
        return STATUS_EXPECT_VALUE;
    }

    public int writeValue()
    {
        int ix = _index;
        ++_index;
        return (ix < 0) ? STATUS_OK_AS_IS : STATUS_OK_AFTER_COMMA;
    }

    public boolean inArray() { return true; }

    protected void appendDesc(StringBuilder sb)
    {
        sb.append('[');
        sb.append(getCurrentIndex());
        sb.append(']');
    }
}

final class ObjectWContext
    extends JsonWriteContext
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

    public ObjectWContext(JsonWriteContext parent)
    {
        super(parent);
        mCurrentName = null;
        mExpectValue = false;
    }

    public Type getType() { return Type.OBJECT; }

    public String getCurrentName() { return mCurrentName; }

    public int writeFieldName(String name)
    {
        if (mCurrentName != null) { // just wrote a name...
            return STATUS_EXPECT_VALUE;
        }
        mCurrentName = name;
        return (_index < 0) ? STATUS_OK_AS_IS : STATUS_OK_AFTER_COMMA;
    }

    public int writeValue()
    {
        if (mCurrentName == null) {
            return STATUS_EXPECT_NAME;
        }
        mCurrentName = null;
        ++_index;
        return STATUS_OK_AFTER_COLON;
    }

    public boolean inObject() { return true; }

    protected void appendDesc(StringBuilder sb)
    {
        sb.append('{');
        if (mCurrentName != null) {
            sb.append('"');
            // !!! TODO: Name chars should be escaped?
            sb.append(mCurrentName);
            sb.append('"');
        } else {
            sb.append('?');
        }
        sb.append(']');
    }
}

