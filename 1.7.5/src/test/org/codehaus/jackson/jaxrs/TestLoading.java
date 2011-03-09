package org.codehaus.jackson.jaxrs;

import org.codehaus.jackson.map.ObjectMapper;

/**
 * Trivially simple test to ensure that JAX-RS piece can be loaded
 * in.
 */
public class TestLoading extends main.BaseTest
{
    final static class Bean {
        public int x = 3;
    }

    /**
     * Test for ensuring loading works ok, without needing XC module
     * (which means JUnit setup has to be ensure those classes
     * are not in...)
     */
    public void testLoading() throws Exception
    {
        JacksonJsonProvider prov = new JacksonJsonProvider();
        // alas, can not map 'real' MediaType, due to some deps within jax-rs api impl
        ObjectMapper m = prov.locateMapper(Bean.class, null);
        assertNotNull(m);
    }

    public void testEnsureXcMissing() throws Exception
    {
        /* 01-Sep-2010, tatus: Skip if not running from Ant/cli:
         */
        if (runsFromAnt()) {
            JacksonJaxbJsonProvider prov = new JacksonJaxbJsonProvider();
            try {
                // should fail here...
                prov.locateMapper(Bean.class, null);
                fail("Expected exception due to missing 'xc' module");
            } catch (NoClassDefFoundError e) {
                // as per [JACKSON-243], JVM messages differ here, should still have class name tho:
                verifyException(e, "JaxbAnnotationIntrospector");
            }
        }
    }
}
