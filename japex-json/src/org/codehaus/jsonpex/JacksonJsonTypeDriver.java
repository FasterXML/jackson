package org.codehaus.jsonpex;

import java.io.*;
import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;

import com.sun.japex.*;

/**
 * @author Tatu Saloranta
 */
public class JacksonJsonTypeDriver extends BaseJsonDriver
{
    JsonFactory mJsonFactory;
    
    public JacksonJsonTypeDriver() { super(); }

    @Override
    public void initializeDriver() {
        mJsonFactory = new JsonFactory();
    }   
    
    @Override
    public void run(TestCase testCase) {
        TreeMapper mapper = new TreeMapper();
        try {
            mInputStream.reset();            
            // Parser could be created in the prepare phase too
            JsonParser jp = mJsonFactory.createJsonParser(mInputStream);
            JsonNode n = mapper.readTree(jp);
            jp.close();
            mHashCode = n.hashCode(); // just to get some non-optimizable number
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
