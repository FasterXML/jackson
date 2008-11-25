package org.codehaus.jackson.impl;

import org.codehaus.jackson.*;

/**
 * Implementation of {@link JsonReadContext}, which also exposes
 * more complete API to the core implementation classes.
 */
public final class JsonReadContextImpl
    extends JsonReadContext
{
    // // // Configuration

    protected final JsonReadContextImpl _parent;

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
        super(type, lineNr, colNr);
        _parent = parent;
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

    public final JsonReadContextImpl getParent() { return _parent; }
    public final JsonReadContextImpl getParentImpl() { return _parent; }

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

}
