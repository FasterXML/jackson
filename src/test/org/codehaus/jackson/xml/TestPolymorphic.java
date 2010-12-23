package org.codehaus.jackson.xml;

import org.codehaus.jackson.annotate.JsonTypeInfo;

public class TestPolymorphic extends XmlTestBase
{
    /*
    /**********************************************************
    /* Helper types
    /**********************************************************
     */

    @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY)
    static class BaseTypeWithClassProperty { }

    static class SubTypeWithClassProperty extends BaseTypeWithClassProperty {
        public String name;

        public SubTypeWithClassProperty() { }
        public SubTypeWithClassProperty(String s) { name = s; }
    }
    
    @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.WRAPPER_ARRAY)
    static class BaseTypeWithClassArray { }

    static class SubTypeWithClassArray extends BaseTypeWithClassArray {
        public String name;

        public SubTypeWithClassArray() { }
        public SubTypeWithClassArray(String s) { name = s; }
    }

    /**
     * If not used as root element, need to use a wrapper
     */
    static class ClassArrayWrapper
    {
        public BaseTypeWithClassArray wrapped;

        public ClassArrayWrapper() { }
        public ClassArrayWrapper(String s) { wrapped = new SubTypeWithClassArray(s); }
    }

    /*
    /**********************************************************
    /* Set up
    /**********************************************************
     */

    protected XmlMapper _xmlMapper;

    // let's actually reuse XmlMapper to make things bit faster
    @Override
    public void setUp() throws Exception {
        super.setUp();
        _xmlMapper = new XmlMapper();
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    public void testAsClassProperty() throws Exception
    {
        String xml = _xmlMapper.writeValueAsString(new SubTypeWithClassProperty("Foobar"));

        Object result = _xmlMapper.readValue(xml, BaseTypeWithClassProperty.class);
        assertNotNull(result);
        assertEquals(SubTypeWithClassProperty.class, result.getClass());
        assertEquals("Foobar", ((SubTypeWithClassProperty) result).name);
    }
    
    /* 19-Dec-2010, tatu: Let's hold off these tests, due to issues with inclusions.
     */
    /*
    // Does not work since array wrapping is not explicitly forced (unlike with collection
    // property of a bean
    public void testAsClassArray() throws Exception
    {
        String xml = _xmlMapper.writeValueAsString(new SubTypeWithClassArray("Foobar"));

        Object result = _xmlMapper.readValue(xml, BaseTypeWithClassArray.class);
        assertNotNull(result);
        assertEquals(SubTypeWithClassArray.class, result.getClass());
        assertEquals("Foobar", ((SubTypeWithClassArray) result).name);
    }

    // Hmmh. Does not yet quite work either, since we do not properly force
    // array context when writing...
    public void testAsWrappedClassArray() throws Exception
    {
        String xml = _xmlMapper.writeValueAsString(new ClassArrayWrapper("Foobar"));

        ClassArrayWrapper result = _xmlMapper.readValue(xml, ClassArrayWrapper.class);
        assertNotNull(result);
        assertEquals(SubTypeWithClassArray.class, result.wrapped.getClass());
        assertEquals("Foobar", ((SubTypeWithClassArray) result.wrapped).name);
    }
        */
        
    // Only works if NOT an inner class ("$" in inner class throws a wrench)...
    /* 20-Dec-2010, tatu: Idiotic Eclipse-JUNIT tries to run tests on these.
     *   Better comment out for now.
     */
    /*
    public void testAsClassObject() throws Exception
    {
        String xml = _xmlMapper.writeValueAsString(new SubTypeWithClassObject("Foobar"));

        Object result = _xmlMapper.readValue(xml, BaseTypeWithClassObject.class);
        assertNotNull(result);
        assertEquals(SubTypeWithClassObject.class, result.getClass());
        assertEquals("Foobar", ((SubTypeWithClassObject) result).name);
    }
    */
}

/*
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.WRAPPER_OBJECT)
class BaseTypeWithClassObject {
}

class SubTypeWithClassObject extends BaseTypeWithClassObject {
    public String name;

    public SubTypeWithClassObject() { }
    public SubTypeWithClassObject(String s) { name = s; }
}
*/
