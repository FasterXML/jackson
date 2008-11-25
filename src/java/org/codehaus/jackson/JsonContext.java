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

/**
 * Shared base class for read and write contexts.
 */
public abstract class JsonContext
{
    // // // Type constants used internally

    protected final static int TYPE_ROOT = 0;
    protected final static int TYPE_ARRAY = 1;
    protected final static int TYPE_OBJECT = 2;

    protected int _type;

    /**
     * Index of the currently processed entry. Starts with -1 to signal
     * that no entries have been started, and gets advanced each
     * time a new entry is started, either by encountering an expected
     * separator, or with new values if no separators are expected
     * (the case for root context).
     */
    protected int _index;

    /*
    //////////////////////////////////////////////////
    // Life-cycle
    //////////////////////////////////////////////////
     */

    public JsonContext(int type)
    {
        _type = type;
        _index = -1;
    }

    /*
    //////////////////////////////////////////////////
    // Public API, accessors
    //////////////////////////////////////////////////
     */

    public abstract JsonContext getParent();

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

    /**
     * @return Number of entries that are complete and started.
     */
    public final int getEntryCount()
    {
        return (_index < 1) ? 0 : _index;
    }

    /**
     * @return Index of the currently processed entry, if any
     */
    public final int getCurrentIndex()
    {
        return (_index < 0) ? 0 : _index;
    }

}
