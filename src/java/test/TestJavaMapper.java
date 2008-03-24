package test;

import java.io.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;

public class TestJavaMapper
{
    private TestJavaMapper() { }

    public static void main(String[] args)
        throws Exception
    {
        if (args.length != 1) {
            System.err.println("Usage: java test.TestJavaMapper <file>");
            System.exit(1);
        }
        FileInputStream in = new FileInputStream(new File(args[0]));
        JsonParser jp = new JsonFactory().createJsonParser(in);
        Object result = new JavaTypeMapper().read(jp);
        jp.close();
        System.out.println("Result: <"+result+">");
    }
}

