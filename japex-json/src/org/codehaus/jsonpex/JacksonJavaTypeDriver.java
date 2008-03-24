package org.codehaus.jsonpex;

import java.io.*;
import org.codehaus.jackson.*;
import org.codehaus.jackson.map.JavaTypeMapper;

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
        JavaTypeMapper mapper = new JavaTypeMapper();
        try {
            mInputStream.reset();            
            
            // Parser could be created in the prepare phase too
            JsonParser jp = mJsonFactory.createJsonParser(mInputStream);
            Object ob = mapper.read(jp);
            jp.close();
            mHashCode = ob.hashCode(); // just to get some non-optimizable number
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
