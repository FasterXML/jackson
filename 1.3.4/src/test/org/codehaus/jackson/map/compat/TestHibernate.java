package org.codehaus.jackson.map.compat;

import main.BaseTest;

import java.io.*;
import java.lang.reflect.Method;
import java.util.*;

import org.hibernate.repackage.cglib.proxy.Enhancer;
import org.hibernate.repackage.cglib.proxy.MethodInterceptor;
import org.hibernate.repackage.cglib.proxy.MethodProxy;

import org.codehaus.jackson.map.*;

/**
 * Basic tests covering Hibernate-compatibility features.
 */
public class TestHibernate
    extends BaseTest
{
    /*
    //////////////////////////////////////////////
    // Helper classes
    //////////////////////////////////////////////
     */

    interface BeanInterfaceHib {
        public int getX();
    }

    /*
    //////////////////////////////////////////////
    // Tests
    //////////////////////////////////////////////
     */

    /*
     * Unit test to test [JACKSON-177]
     */
    public void testHibernateCglib() throws Exception
    {
    /*
      // 26-Oct-2009, tatu: Alas, won't run well from Ant, at least without restarting JVM... better comment out for now

        Enhancer enh = new Enhancer();
        enh.setInterfaces(new Class[] { BeanInterfaceHib.class });
        enh.setCallback(new MethodInterceptor() {
                public Object intercept(Object obj, Method method,
                                        Object[] args, MethodProxy proxy)
                    throws Throwable
                {
                    if ("getX".equals(method.getName ())) {
                        return Integer.valueOf(13);
                    }
                    return proxy.invokeSuper(obj, args);
                }
            });
        BeanInterfaceHib bean = (BeanInterfaceHib) enh.create();
        ObjectMapper mapper = new ObjectMapper();
        Map<String,Object> result = writeAndMap(mapper, bean);
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(13), result.get("x"));
    */
    }

    /*
    //////////////////////////////////////////////
    // Helper methods
    //////////////////////////////////////////////
     */

    @SuppressWarnings("unchecked")
	private Map<String,Object> writeAndMap(ObjectMapper m, Object value)
        throws IOException
    {
        StringWriter sw = new StringWriter();
        m.writeValue(sw, value);
        return (Map<String,Object>) m.readValue(sw.toString(), Object.class);
    }
}

