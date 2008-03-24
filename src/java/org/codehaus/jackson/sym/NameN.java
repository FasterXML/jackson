package org.codehaus.jackson.sym;

/**
 * Specialized implementation of PName: can be used for short Strings
 * that consists of 9 to 12 bytes. It's the longest special purpose
 * implementaion; longer ones are expressed using {@link NameN}.
 */
public final class NameN
    extends Name
{
    final int[] mQuads;
    final int mQuadLen;

    NameN(String name, int hash, int[] quads, int quadLen)
    {
        super(name, hash);
        mQuads = quads;
        mQuadLen = quadLen;
    }

    public boolean equals(int quad1, int quad2)
    {
        // Unlikely to match... but:
        if (mQuadLen < 3) {
            if (mQuadLen == 1) {
                return (mQuads[0] == quad1) && (quad2 == 0);
            }
            return (mQuads[0] == quad1) && (mQuads[1] == quad2);
        }
        return false;
    }

    public boolean equals(int[] quads, int qlen)
    {
        if (qlen == mQuadLen) {
            for (int i = 0; i < qlen; ++i) {
                if (quads[i] != mQuads[i]) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public int getFirstQuad() {
        return mQuads[0];
    }

    public int getLastQuad() {
        return mQuads[mQuadLen-1];
    }

    public int getQuad(int index)
    {
        return (index < mQuadLen) ? mQuads[index] : 0;
    }

    public int sizeInQuads() { return mQuadLen; }

}
