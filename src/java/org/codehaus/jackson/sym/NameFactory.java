package org.codehaus.jackson.sym;

/**
 * Simple factory that can instantiate appropriate {@link Name}
 * instances, given input data to use for construction. The main reason
 * for a factory class here is just to insulate calling code from having
 * to know details of concrete implementations.
 */
public final class NameFactory
{
    private NameFactory() { }

    /*
    //////////////////////////////////////////////////////////
    // Public API
    //////////////////////////////////////////////////////////
     */

    public static Name construct(int hash, String name, int q1, int q2)
    {
        name = name.intern();
        if (q2 == 0) { // one quad only?
            return new Name1(name, hash, q1);
        }
        return new Name2(name, hash, q1, q2);
    }

    public static Name construct(int hash, String name, int[] quads, int qlen)
    {
        name = name.intern();
        if (qlen < 4) { // Need to check for 3 quad one, can do others too
            if (qlen == 3) {
                return new Name3(name, hash, quads[0], quads[1], quads[2]);
            }
            if (qlen == 2) {
                return new Name2(name, hash, quads[0], quads[1]);
            }
            return new Name1(name, hash, quads[0]);
        }
        // Otherwise, need to copy the incoming buffer
        int[] buf = new int[qlen];
        for (int i = 0; i < qlen; ++i) {
            buf[i] = quads[i];
        }
        return new NameN(name, hash, buf, qlen);
    }
}
