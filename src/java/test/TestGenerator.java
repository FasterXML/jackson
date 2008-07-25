package test;

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
        jg.writeEndObject();

        jg.close();

        System.out.println("Result: <"+strw.toString()+">");
    }
}

