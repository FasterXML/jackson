package org.codehaus.jackson.io;

import java.io.*;

import org.codehaus.jackson.util.BufferRecycler;

public class TestUTF8Writer
    extends main.BaseTest
{
    public void testSimple() throws Exception
    {
        BufferRecycler rec = new BufferRecycler();
        IOContext ctxt = new IOContext(rec, null, false);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        UTF8Writer w = new UTF8Writer(ctxt, out);

        String str = "AB\u00A0\u1AE9\uFFFC";
        char[] ch = str.toCharArray();

        // Let's write 3 times, using different methods
        w.write(str);

        w.append(ch[0]);
        w.write(ch[1]);
        w.write(ch, 2, 3);

        w.write(str, 0, str.length());
        w.close();

        // and thus should have 3 times contents
        byte[] data = out.toByteArray();
        assertEquals(3*10, data.length);
        String act = out.toString("UTF-8");
        assertEquals(15, act.length());

        assertEquals(3 * str.length(), act.length());
        assertEquals(str+str+str, act);
    }

    public void testFlushAfterClose() throws Exception
    {
        BufferRecycler rec = new BufferRecycler();
        IOContext ctxt = new IOContext(rec, null, false);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        UTF8Writer w = new UTF8Writer(ctxt, out);
        
        w.write('X');
        
        w.close();
        assertEquals(1, out.size());

        // and this ought to be fine...
        w.flush();
        // as well as some more...
        w.close();
        w.flush();
    }
}
