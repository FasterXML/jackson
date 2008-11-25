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
    extends JsonContext
{
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
        super(type);
        _type = type;
        _lineNr = lineNr;
        _columnNr = colNr;
    }

    /*
    //////////////////////////////////////////////////
    // Public API, accessors
    //////////////////////////////////////////////////
     */

    // note: co-variant
    @Override
    public abstract JsonReadContext getParent();

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
