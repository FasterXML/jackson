package org.codehaus.jackson.util;

import main.BaseTest;

import java.util.*;

import org.codehaus.jackson.map.util.*;

public class TestObjectBuffer
    extends BaseTest
{
    /**
     * First a test that treats results as plain old Object[]
     */
    public void testUntyped()
    {
        _testObjectBuffer(null);
    }

    public void testTyped()
    {
        _testObjectBuffer(Integer.class);
    }

    /*
    //////////////////////////////////////////////////
    // Helper methods
    //////////////////////////////////////////////////
     */

    private void _testObjectBuffer(Class<?> clz)
    {
        int[] SIZES = new int[] {
            3, 19, 99, 1007, 79000, 256001
        };

        // Let's loop separately for reused instance, new instance
        for (int reuse = 0; reuse < 2; ++reuse) {
            ObjectBuffer buf = (reuse == 0) ? null : new ObjectBuffer();

            // then distinct sizes
            for (int sizeIndex = 0; sizeIndex < SIZES.length; ++sizeIndex) {
                int size = SIZES[sizeIndex];
                Random r = new Random(size);
                ObjectBuffer thisBuf = (buf == null) ? new ObjectBuffer() : buf;
                Object[] chunk = thisBuf.resetAndStart();
                int ix = 0;

                for (int i = 0; i < size; ++i) {
                    if (ix >= chunk.length) {
                        chunk = thisBuf.appendCompletedChunk(chunk);
                        ix = 0;
                    }
                    chunk[ix++] = Integer.valueOf(r.nextInt());
                }

                Object[] result;
                
                if (clz == null) {
                    result = thisBuf.completeAndClearBuffer(chunk, ix);
                } else {
                    result = thisBuf.completeAndClearBuffer(chunk, ix, clz);
                }
                assertEquals(size, result.length);

                r = new Random(size);
                for (int i = 0; i < size; ++i) {
                    assertEquals(r.nextInt(), ((Integer) result[i]).intValue());
                }
            }
        }
    }
}
