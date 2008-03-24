package org.codehaus.jackson.sym;

/**
 * Specialized implementation of PName: can be used for short Strings
 * that consists of 9 to 12 bytes. It's the longest special purpose
 * implementaion; longer ones are expressed using {@link NameN}.
 */
public final class Name3
    extends Name
{
    final int mQuad1;
    final int mQuad2;
    final int mQuad3;

    Name3(String name, int hash, int q1, int q2, int q3)
    {
        super(name, hash);
        mQuad1 = q1;
        mQuad2 = q2;
        mQuad3 = q3;
    }

    public boolean equals(int quad1, int quad2)
    {
        // Implies quad length < 3, never matches
        return false;
    }

    public boolean equals(int[] quads, int qlen)
    {
        return (qlen == 3)
            && (quads[0] == mQuad1)
            && (quads[1] == mQuad2)
            && (quads[2] == mQuad3);
    }

    public int getFirstQuad() {
        return mQuad1;
    }

    public int getLastQuad() {
        return mQuad3;
    }

    public int getQuad(int index) {
        if (index < 2) {
            return (index == 0) ? mQuad1 : mQuad2;
        }
        // Whatever would be returned for invalid index is arbitrary, so:
        return mQuad3;
    }

    public int sizeInQuads() { return 3; }
}
