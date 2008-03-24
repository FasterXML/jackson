package org.codehaus.jsonpex;

import java.io.*;
import org.codehaus.jackson.*;

import com.sun.japex.*;

/**
 * Test driver for accessing JSON via "raw" Jackson streaming
 * API. All data is accessed (to avoid favoring skip-through pattern,
 * which can not be used by tree model parsers), but the most
 * efficient accessors are used all event types.
 *
 * @author Santiago.PericasGeertsen@sun.com
 * @author Tatu Saloranta (cowtowncoder@yahoo.com)
 */
public class JacksonDriver extends BaseJsonDriver
{
    JsonFactory mJsonFactory;
    
    public JacksonDriver() { super(); }

    @Override
    public void initializeDriver() {
        try {
            mJsonFactory = new JsonFactory();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }   
    
    @Override
    public void run(TestCase testCase) {
        try {
            mInputStream.reset();            
            
            // Parser could be created in the prepare phase too
            JsonParser jp = mJsonFactory.createJsonParser(mInputStream);
            int total  = 0;
            JsonToken t;

            /* Let's exercise enough accessors to ensure all data is
             * processed; values themselves are irrelevant.
             */
            while ((t = jp.nextToken()) != null) {
                switch (t) {
                case VALUE_STRING:
                    {
                        char[] chars = jp.getTextCharacters();
                        int offset = jp.getTextOffset();
                        int len = jp.getTextLength();
                        total += offset + len;
                    }
                    break;
                case VALUE_NUMBER_INT:
                    total += jp.getIntValue();
                    break;
                case VALUE_NUMBER_FLOAT:
                    total += (int) jp.getDoubleValue();
                    break;

                case VALUE_TRUE:
                    total += 1;
                    break;
                case VALUE_FALSE:
                    total -= 1;
                    break;
                case VALUE_NULL:
                    ++total;
                    break;
                }
                ;
            }
            jp.close();
            mHashCode = total; // just to get some non-optimizable number
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
