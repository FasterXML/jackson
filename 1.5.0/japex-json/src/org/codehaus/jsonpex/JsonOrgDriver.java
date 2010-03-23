package org.codehaus.jsonpex;

import java.io.*;
import org.json.*;

import com.sun.japex.*;

/**
 * @author Santiago.PericasGeertsen@sun.com
 * @author Tatu Saloranta
 */
public class JsonOrgDriver extends JapexDriverBase {
    
    int mHashCode;
    byte[] mInputData;
    
    public JsonOrgDriver() { }

    @Override
    public void initializeDriver() {
        // All good as is
    }   
    
    @Override
    public void prepare(TestCase testCase) {
        String xmlFile = testCase.getParam("japex.inputFile");
        
        if (xmlFile == null) {
            throw new RuntimeException("japex.inputFile not specified");
        }
        
        try {
            // Load XML file to factor out I/O
            mInputData = Util.streamToByteArray(new FileInputStream(new File(xmlFile)));
        }        
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public void warmup(TestCase testCase) {
        run(testCase);
    }
    
    @Override
    public void run(TestCase testCase) {
        try {
            String input = new String(mInputData, "UTF-8");
            JSONTokener tok = new JSONTokener(input);
            Object ob = tok.nextValue();
            mHashCode = ob.hashCode(); // just to get some non-optimizable number
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public void finish(TestCase testCase) {
        // Set file size in KB on X axis
        testCase.setDoubleParam("japex.resultValueX", 
                mInputData.length / 1024.0);
        getTestSuite().setParam("japex.resultUnitX", "KB");
    }
    
}
