package org.codehaus.jackson.version;

import org.codehaus.jackson.*;
import org.codehaus.jackson.impl.ReaderBasedParser;
import org.codehaus.jackson.impl.WriterBasedGenerator;
import org.codehaus.jackson.io.IOContext;
import org.codehaus.jackson.util.BufferRecycler;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.mrbean.AbstractTypeMaterializer;
import org.codehaus.jackson.smile.SmileFactory;
import org.codehaus.jackson.smile.SmileGenerator;
import org.codehaus.jackson.smile.SmileParser;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;

/**
 * Tests to verify [JACKSON-278]
 * 
 * @since 1.6
 */
public class TestVersions extends main.BaseTest
{
    public void testCoreVersions()
    {
        /* 01-Sep-2010, tatu: Somewhat of a dirty hack; let's only run when specific system
         *    property is set; and set that flag from Ant unit test. Why? To prevent running
         *    from Eclipse, where this would just fail
         */
        if (runsFromAnt()) {
            System.out.println("Note: running version tests (FROM_ANT=true)");
            assertVersion(new JsonFactory().version(), 1, 6);
            assertVersion(new ReaderBasedParser(getIOContext(), 0, null, null, null).version(), 1, 6);
            assertVersion(new WriterBasedGenerator(getIOContext(), 0, null, null).version(), 1, 6);
        }
    }

    public void testMapperVersions()
    {
        if (runsFromAnt()) {
            ObjectMapper mapper = new ObjectMapper();
            assertVersion(mapper.version(), 1, 6);
            assertVersion(mapper.writer().version(), 1, 6);
            assertVersion(mapper.reader().version(), 1, 6);
        }
    }

    public void testXcVersions()
    {
        if (runsFromAnt()) {
            assertVersion(new JaxbAnnotationIntrospector().version(), 1, 6);
        }
    }

    public void testJaxRsVersions()
    {
        if (runsFromAnt()) {
            assertVersion(new JacksonJsonProvider().version(), 1, 6);
        }
    }

    public void testSmileVersions()
    {
        if (runsFromAnt()) {
            assertVersion(new SmileFactory().version(), 1, 6);
            assertVersion(new SmileGenerator(getIOContext(), 0, 0, null, null).version(), 1, 6);
            assertVersion(new SmileParser(getIOContext(), 0, 0, null, null, null, null, 0, 0, false).version(), 1, 6);
        }
    }

    public void testMrBeanVersions()
    {
        if (runsFromAnt()) {
            assertVersion(new AbstractTypeMaterializer().version(), 1, 6);
        }
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */
    
    private void assertVersion(Version v, int major, int minor)
    {
        assertFalse("Should find version information (got "+v+")", v.isUknownVersion());
        assertEquals(major, v.getMajorVersion());
        assertEquals(minor, v.getMinorVersion());
        /* 01-Sep-2010, tatus: Check patch level initially; for maintenance versions,
         *   comment out.
         */
        assertEquals(0, v.getPatchLevel());
    }

    private IOContext getIOContext() {
        return new IOContext(new BufferRecycler(), null, false);
    }
}

