package org.codehaus.jsonpex;

import java.io.*;

import com.sun.japex.*;

/**
 * Shared base class for driver implementations
 *
 * @author tatus (cowtowncoder@yahoo.com)
 * @author Santiago.PericasGeertsen@sun.com
 */
public class BaseJsonDriver extends JapexDriverBase
{
    protected ByteArrayInputStream mInputStream;

    protected byte[] mInputData;
    protected int mDataLen;
    
    protected int mHashCode;

    public BaseJsonDriver() { }
    
    @Override
    public void prepare(TestCase testCase) {
        String xmlFile = testCase.getParam("japex.inputFile");
        
        if (xmlFile == null) {
            throw new RuntimeException("japex.inputFile not specified");
        }
        
        try {
            // Load XML file to factor out I/O
            mInputData = Util.streamToByteArray(new FileInputStream(new File(xmlFile)));
            mDataLen = mInputData.length;
            mInputStream = new ByteArrayInputStream(mInputData);
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
    public void finish(TestCase testCase) {
        // Set file size in KB on X axis
        mInputStream.reset();
        testCase.setDoubleParam("japex.resultValueX", 
                mInputStream.available() / 1024.0);
        getTestSuite().setParam("japex.resultUnitX", "KB");

        /* 30-Sep-2007, tatus: Let's measure throughput in MBps,
         *   instead of tps
         */
        //getTestSuite().setParam("japex.resultUnit", "tps");
        getTestSuite().setParam("japex.resultUnit", "mbps");
    }
    
}
