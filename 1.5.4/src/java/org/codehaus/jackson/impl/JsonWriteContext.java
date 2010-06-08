package org.codehaus.jackson.impl;

import org.codehaus.jackson.*;

/**
 * Extension of {@link JsonStreamContext}, which implements
 * core methods needed, and also exposes
 * more complete API to generator implementation classes.
 */
public abstract class JsonWriteContext
    extends JsonStreamContext
{
    // // // Return values for writeValue()

    public final static int STATUS_OK_AS_IS = 0;
    public final static int STATUS_OK_AFTER_COMMA = 1;
    public final static int STATUS_OK_AFTER_COLON = 2;
    public final static int STATUS_OK_AFTER_SPACE = 3; // in root context
    public final static int STATUS_EXPECT_VALUE = 4;
    public final static int STATUS_EXPECT_NAME = 5;

    protected final JsonWriteContext _parent;

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

    protected JsonWriteContext(int type, JsonWriteContext parent)
    {
        super(type);
        _parent = parent;
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

    // // // API sub-classes are to implement

    /**
     * Method that writer is to call before it writes a field name.
     *
     * @return Index of the field entry (0-based)
     */
    public abstract int writeFieldName(String name);

    public abstract int writeValue();

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
        super(TYPE_ROOT, null);
    }

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
        super(TYPE_ARRAY, parent);
    }

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
    protected String _currentName;

    /**
     * Flag to indicate that the context just received the
     * field name, and is to get a value next
     */
    protected boolean _expectValue;

    public ObjectWContext(JsonWriteContext parent)
    {
        super(TYPE_OBJECT, parent);
        _currentName = null;
        _expectValue = false;
    }

    public String getCurrentName() { return _currentName; }

    public int writeFieldName(String name)
    {
        if (_currentName != null) { // just wrote a name...
            return STATUS_EXPECT_VALUE;
        }
        _currentName = name;
        return (_index < 0) ? STATUS_OK_AS_IS : STATUS_OK_AFTER_COMMA;
    }

    public int writeValue()
    {
        if (_currentName == null) {
            return STATUS_EXPECT_NAME;
        }
        _currentName = null;
        ++_index;
        return STATUS_OK_AFTER_COLON;
    }

    protected void appendDesc(StringBuilder sb)
    {
        sb.append('{');
        if (_currentName != null) {
            sb.append('"');
            // !!! TODO: Name chars should be escaped?
            sb.append(_currentName);
            sb.append('"');
        } else {
            sb.append('?');
        }
        sb.append(']');
    }
}

