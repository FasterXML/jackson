package org.codehaus.jsonpex;

import java.io.*;

import com.sdicons.json.parser.JSONParser;

import com.sun.japex.*;

/**
 * @author cowtowncoder@yahoo.com
 */
public class JsonToolsDriver extends BaseJsonDriver
{
    public JsonToolsDriver() { super(); }

    @Override
    public void initializeDriver() {
        // No factories for JsonTools
    }   
    
    @Override
    public void run(TestCase testCase)
    {
        try {
            mInputStream.reset();

            // Json-tools accepts streams, yay!
            JSONParser jp = new JSONParser(new ByteArrayInputStream(mInputData), "byte stream");
            /* Hmmmh. Will we get just one object for the whole thing?
             * Or a stream? Seems like just one
             */
            //while ((ob = jp.nextValue()) != null) { ; }
            Object ob = jp.nextValue();
            mHashCode = ob.hashCode(); // just to get some non-optimizable number
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
