package org.codehaus.jackson;

/**
 * Context object is used to keep track of relative logical position
 * of the current event when generating json content.
 */
public abstract class JsonWriteContext
    extends JsonContext
{
    /*
    //////////////////////////////////////////////////
    // Life-cycle
    //////////////////////////////////////////////////
     */

    public JsonWriteContext(int type)
    {
        super(type);
    }

    /*
    //////////////////////////////////////////////////
    // Public API, accessors
    //////////////////////////////////////////////////
     */

    // note: co-variant
    @Override
    public abstract JsonWriteContext getParent();
}
