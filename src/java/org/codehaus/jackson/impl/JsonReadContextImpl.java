package org.codehaus.jackson.impl;

import org.codehaus.jackson.*;

/**
 * Implementation of {@link JsonReadContext}, which also exposes
 * more complete API to the other implementation classes.
 */
public final class JsonReadContextImpl
    extends JsonReadContext
{
    protected final static int INT_COLON = ':';
    protected final static int INT_COMMA = ',';

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

    public final JsonReadContext getParent() { return _parent; }

    /*
    //////////////////////////////////////////////////
    // Private/package accessors
    //////////////////////////////////////////////////
     */

    public final JsonReadContextImpl getParentImpl() { return _parent; }

    /*
    //////////////////////////////////////////////////
    // State changes
    //////////////////////////////////////////////////
     */

    public int handleSeparator(int ch)
    {
        int ix = ++_index;
        if (_type == TYPE_OBJECT) {
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

        if (_type == TYPE_ARRAY) {
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

    public void setCurrentName(String name)
    {
        _currentName = name;
    }

}
