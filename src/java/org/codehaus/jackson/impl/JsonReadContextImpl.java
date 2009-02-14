package org.codehaus.jackson.impl;

import org.codehaus.jackson.*;
import org.codehaus.jackson.util.CharTypes;

/**
 * Implementation of {@link JsonStreamContext}, which also exposes
 * more complete API to the core implementation classes.
 */
public final class JsonReadContextImpl
    extends JsonStreamContext
{
    // // // Configuration

    protected final JsonReadContextImpl _parent;

    // // // Location information (minus source reference)

    //long mTotalChars;

    protected int _lineNr;
    protected int _columnNr;

    protected String _currentName;

    /*
    //////////////////////////////////////////////////
    // Simple instance reuse slots; speeds up things
    // a bit (10-15%) for docs with lots of small
    // arrays/objects (for which allocation was
    // visible in profile stack frames)
    //////////////////////////////////////////////////
     */

    JsonReadContextImpl _child = null;

    /*
    //////////////////////////////////////////////////
    // Instance construction, reuse
    //////////////////////////////////////////////////
     */

    public JsonReadContextImpl(JsonReadContextImpl parent,
                               int type, int lineNr, int colNr)
    {
        super(type);
        _parent = parent;
        _lineNr = lineNr;
        _columnNr = colNr;
    }

    protected final void reset(int type, int lineNr, int colNr)
    {
        _type = type;
        _index = -1;
        _lineNr = lineNr;
        _columnNr = colNr;
        _currentName = null;
    }

    // // // Factory methods

    public static JsonReadContextImpl createRootContext(int lineNr, int colNr)
    {
        return new JsonReadContextImpl(null, TYPE_ROOT, lineNr, colNr);
    }

    public final JsonReadContextImpl createChildArrayContext(int lineNr, int colNr)
    {
        JsonReadContextImpl ctxt = _child;
        if (ctxt == null) {
            return (_child = new JsonReadContextImpl(this, TYPE_ARRAY, lineNr, colNr));
        }
        ctxt.reset(TYPE_ARRAY, lineNr, colNr);
        return ctxt;
    }

    public final JsonReadContextImpl createChildObjectContext(int lineNr, int colNr)
    {
        JsonReadContextImpl ctxt = _child;
        if (ctxt == null) {
            return (_child = new JsonReadContextImpl(this, TYPE_OBJECT, lineNr, colNr));
        }
        ctxt.reset(TYPE_OBJECT, lineNr, colNr);
        return ctxt;
    }

    /*
    //////////////////////////////////////////////////
    // Abstract method implementation
    //////////////////////////////////////////////////
     */

    public final String getCurrentName() { return _currentName; }

    public final JsonReadContextImpl getParent() { return _parent; }

    /*
    //////////////////////////////////////////////////
    // Extended API
    //////////////////////////////////////////////////
     */

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

        return new JsonLocation(srcRef, totalChars, _lineNr, _columnNr);
    }

    /*
    //////////////////////////////////////////////////
    // State changes
    //////////////////////////////////////////////////
     */

    public final boolean expectComma()
    {
        /* Assumption here is that we will be getting a value (at least
         * before calling this method again), and
         * so will auto-increment index to avoid having to do another call
         */
        int ix = ++_index; // starts from -1
        return (_type != TYPE_ROOT && ix > 0);
    }

    public void setCurrentName(String name)
    {
        _currentName = name;
    }

    /*
    //////////////////////////////////////////////////
    // Overridden standard methods
    //////////////////////////////////////////////////
    */

    /**
     * Overridden to provide developer readable "JsonPath" representation
     * of the context.
     */
    public final String toString()
    {
        StringBuilder sb = new StringBuilder(64);
        switch (_type) {
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
            if (_currentName != null) {
                sb.append('"');
                CharTypes.appendQuoted(sb, _currentName);
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
