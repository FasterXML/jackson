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
    /* Unit tests
    /**********************************************************
     */

    public void testAsClassProperty() throws Exception
    {
        XmlMapper mapper = new XmlMapper();
        String xml = mapper.writeValueAsString(new SubTypeWithClassProperty("Foobar"));

        Object result = mapper.readValue(xml, BaseTypeWithClassProperty.class);
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
        XmlMapper mapper = new XmlMapper();
        String xml = mapper.writeValueAsString(new SubTypeWithClassArray("Foobar"));

        Object result = mapper.readValue(xml, BaseTypeWithClassArray.class);
        assertNotNull(result);
        assertEquals(SubTypeWithClassArray.class, result.getClass());
        assertEquals("Foobar", ((SubTypeWithClassArray) result).name);
    }

    // Hmmh. Does not yet quite work either, since we do not properly force
    // array context when writing...
    public void testAsWrappedClassArray() throws Exception
    {
        XmlMapper mapper = new XmlMapper();
        String xml = mapper.writeValueAsString(new ClassArrayWrapper("Foobar"));
System.out.println("XML == "+xml);        

        ClassArrayWrapper result = mapper.readValue(xml, ClassArrayWrapper.class);
        assertNotNull(result);
        assertEquals(SubTypeWithClassArray.class, result.wrapped.getClass());
        assertEquals("Foobar", ((SubTypeWithClassArray) result.wrapped).name);
    }
        */
        
    // Only works if NOT an inner class ("$" in inner class throws a wrench)...
    public void testAsClassObject() throws Exception
    {
        XmlMapper mapper = new XmlMapper();
        String xml = mapper.writeValueAsString(new SubTypeWithClassObject("Foobar"));

        Object result = mapper.readValue(xml, BaseTypeWithClassObject.class);
        assertNotNull(result);
        assertEquals(SubTypeWithClassObject.class, result.getClass());
        assertEquals("Foobar", ((SubTypeWithClassObject) result).name);
    }
}

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.WRAPPER_OBJECT)
class BaseTypeWithClassObject { }

class SubTypeWithClassObject extends BaseTypeWithClassObject {
    public String name;

    public SubTypeWithClassObject() { }
    public SubTypeWithClassObject(String s) { name = s; }
}


