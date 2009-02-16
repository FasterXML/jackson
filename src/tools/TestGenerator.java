

import java.io.*;
import java.math.BigDecimal;

import org.codehaus.jackson.*;

public class TestGenerator
{
    private TestGenerator() { }

    public static void main(String[] args)
        throws Exception
    {
        StringWriter strw = new StringWriter();
        JsonGenerator jg = new JsonFactory().createJsonGenerator(strw);

        jg.writeStartObject();
        jg.writeFieldName("pi");
        //jg.writeNumber(new BigDecimal("1.23"));
        jg.writeNumber(new BigDecimal("1.23"));
        jg.writeFieldName("binary");
        byte[] data = "Test string!".getBytes("UTF-8");
        jg.writeBinary(data);

        // what happens if we leave this out?
        //jg.writeEndObject();

        jg.close();

        System.out.println("Result: <"+strw.toString()+">");
    }
}

