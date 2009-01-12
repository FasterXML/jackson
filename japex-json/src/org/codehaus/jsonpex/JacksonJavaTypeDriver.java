package org.codehaus.jsonpex;

import java.io.*;
import org.codehaus.jackson.*;
import org.codehaus.jackson.map.ObjectMapper;

import com.sun.japex.*;

/**
 * @author Tatu Saloranta
 */
public class JacksonJavaTypeDriver extends BaseJsonDriver
{
    JsonFactory mJsonFactory;
    
    public JacksonJavaTypeDriver() { super(); }

    @Override
    public void initializeDriver() {
        mJsonFactory = new JsonFactory();
    }   
    
    @Override
    public void run(TestCase testCase) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            mInputStream.reset();            
            
            // Parser could be created in the prepare phase too
            JsonParser jp = mJsonFactory.createJsonParser(mInputStream);
            // By passing Object.class, we'll get Maps/Lists/wrappers:
            Object ob = mapper.readValue(jp, Object.class);
            jp.close();
            mHashCode = ob.hashCode(); // just to get some non-optimizable number
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
