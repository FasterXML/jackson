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
public abstract class JsonReadContext
{
    protected final static int INT_COLON = ':';
    protected final static int INT_COMMA = ',';

    protected final static int TYPE_ROOT = 0;
    protected final static int TYPE_ARRAY = 1;
    protected final static int TYPE_OBJECT = 2;

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

    protected int _type;

    /**
     * Index of the currently processed entry. Starts with -1 to signal
     * that no entries have been started, and gets advanced each
     * time a new entry is started, either by encountering an expected
     * separator, or with new values if no separators are expected
     * (the case for root context).
     */
    protected int _index;

    // // // Location information (minus source reference)

    //long mTotalChars;

    protected int _lineNr;
    protected int _columnNr;

    protected String _currentName;

    /*
    //////////////////////////////////////////////////
    // Life-cycle
    //////////////////////////////////////////////////
     */

    public JsonReadContext(int type, int lineNr, int colNr)
    {
        _type = type;
        _index = -1;
        _lineNr = lineNr;
        _columnNr = colNr;
    }

    /*
    //////////////////////////////////////////////////
    // Public API, accessors
    //////////////////////////////////////////////////
     */

    public abstract JsonReadContext getParent();

    /**
     * @return Number of entries that are complete and started.
     */
    public final int getEntryCount()
    {
        if (_type == TYPE_OBJECT) {
            return (_index >> 1) + 1;
        }
        return _index+1;
    }

    /**
     * @return Index of the currently processed entry, if any
     */
    public final int getCurrentIndex()
    {
        if (_index < 0) {
            return 0;
        }
        if (_type == TYPE_OBJECT) {
            return _index >> 1;
        }
        return _index;
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

        return new JsonLocation(srcRef, totalChars, _lineNr, _columnNr);
    }

    public final boolean isArray() { return _type == TYPE_ARRAY; }
    public final boolean isRoot() { return _type == TYPE_ROOT; }
    public final boolean isObject() { return _type == TYPE_OBJECT; }

    public final String getTypeDesc() {
        switch (_type) {
        case TYPE_ROOT: return "ROOT";
        case TYPE_ARRAY: return "ARRAY";
        case TYPE_OBJECT: return "OBJECT";
        }
        return "?";
    }

    public final String getCurrentName() { return _currentName; }

    // // // Overridden standard methods

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
