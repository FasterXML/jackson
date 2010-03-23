package org.codehaus.jackson.sym;

/**
 * Base class for tokenized names (key strings in objects) that have
 * been tokenized from byte-based input sources (like
 * {@link java.io.InputStream}.
 *
 * @author Tatu Saloranta
 */
public abstract class Name
{
    protected final String mName;

    protected final int mHashCode;

    protected Name(String name, int hashCode) {
        mName = name;
        mHashCode = hashCode;
    }

    public String getName() { return mName; }

    /*
    //////////////////////////////////////////////////////////
    // Methods for package/core parser
    //////////////////////////////////////////////////////////
     */

    public abstract boolean equals(int quad1);

    public abstract boolean equals(int quad1, int quad2);

    public abstract boolean equals(int[] quads, int qlen);

    /*
    //////////////////////////////////////////////////////////
    // Overridden standard methods
    //////////////////////////////////////////////////////////
     */

    @Override
        public String toString() { return mName; }

    @Override
        public final int hashCode() { return mHashCode; }

    @Override
        public boolean equals(Object o)
    {
        // Canonical instances, can usually just do identity comparison
        return (o == this);
    }
}
