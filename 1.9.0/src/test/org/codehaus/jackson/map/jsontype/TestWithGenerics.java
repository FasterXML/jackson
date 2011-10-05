package org.codehaus.jackson.map.jsontype;

import java.io.IOException;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.annotate.JsonSubTypes.Type;
import org.codehaus.jackson.annotate.JsonTypeInfo.As;
import org.codehaus.jackson.annotate.JsonTypeInfo.Id;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.introspect.BasicBeanDescription;
import org.codehaus.jackson.map.ser.BeanSerializerFactory;
import org.codehaus.jackson.map.type.TypeFactory;

public class TestWithGenerics extends BaseMapTest
{
    @JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "object-type")
    @JsonSubTypes( { @Type(value = Dog.class, name = "doggy") })
    static abstract class Animal {
        public String name;
    }    

    static class Dog extends Animal {
        public int boneCount;

        public Dog(String name, int b) {
            super();
            this.name = name;
            boneCount = b;
        }
    }

    static class ContainerWithGetter<T extends Animal> {
        private T animal;

        public ContainerWithGetter(T a) { animal = a; }

        public T getAnimal() { return animal; }
    }

    static class ContainerWithField<T extends Animal> {
        public T animal;

        public ContainerWithField(T a) { animal = a; }
    }
    
    // Beans for [JACKSON-387], [JACKSON-430]
    
    @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@classAttr1")
    static class MyClass {
        public List<MyParam<?>> params = new ArrayList<MyParam<?>>();
    }

    @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@classAttr2")
    static class MyParam<T>{
        public T value;

        public MyParam() { }
        public MyParam(T v) { value = v; }
    }

    private static class SomeObject {
        @SuppressWarnings("unused")
        public String someValue = UUID.randomUUID().toString();
    }
    
    // Beans for [JACKSON-430]
    
    static class CustomJsonSerializer extends JsonSerializer<Object>
        implements ResolvableSerializer
    {
        private final JsonSerializer<Object> beanSerializer;
    
        public CustomJsonSerializer( JsonSerializer<Object> beanSerializer ) { this.beanSerializer = beanSerializer; }
    
        @Override
        public void serialize( Object value, JsonGenerator jgen, SerializerProvider provider )
            throws IOException, JsonProcessingException
        {
            beanSerializer.serialize( value, jgen, provider );
        }
    
        @Override
        public Class<Object> handledType() { return beanSerializer.handledType(); }
    
        @Override
        public void serializeWithType( Object value, JsonGenerator jgen, SerializerProvider provider, TypeSerializer typeSer )
            throws IOException, JsonProcessingException
        {
            beanSerializer.serializeWithType( value, jgen, provider, typeSer );
        }

        @Override
        public void resolve(SerializerProvider provider) throws JsonMappingException
        {
            if (beanSerializer instanceof ResolvableSerializer) {
                ((ResolvableSerializer) beanSerializer).resolve(provider);
            }
        }
    }
    
    protected static class CustomJsonSerializerFactory extends BeanSerializerFactory
    {
        public CustomJsonSerializerFactory() { super(null); }

        @Override
        protected JsonSerializer<Object> constructBeanSerializer( SerializationConfig config, BasicBeanDescription beanDesc,
                BeanProperty property)
            throws JsonMappingException
        {                
            return new CustomJsonSerializer( super.constructBeanSerializer( config, beanDesc, property) );
        }
    }

    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    public void testWrapperWithGetter() throws Exception
    {
        Dog dog = new Dog("Fluffy", 3);
        String json = new ObjectMapper().writeValueAsString(new ContainerWithGetter<Animal>(dog));
        if (json.indexOf("\"object-type\":\"doggy\"") < 0) {
            fail("polymorphic type not kept, result == "+json+"; should contain 'object-type':'...'");
        }
    }

    public void testWrapperWithField() throws Exception
    {
        Dog dog = new Dog("Fluffy", 3);
        String json = new ObjectMapper().writeValueAsString(new ContainerWithField<Animal>(dog));
        if (json.indexOf("\"object-type\":\"doggy\"") < 0) {
            fail("polymorphic type not kept, result == "+json+"; should contain 'object-type':'...'");
        }
    }
    
    public void testWrapperWithExplicitType() throws Exception
    {
        Dog dog = new Dog("Fluffy", 3);
        ContainerWithGetter<Animal> c2 = new ContainerWithGetter<Animal>(dog);
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writerWithType(TypeFactory.defaultInstance().constructParametricType(ContainerWithGetter.class, Animal.class)).writeValueAsString(c2);
        if (json.indexOf("\"object-type\":\"doggy\"") < 0) {
            fail("polymorphic type not kept, result == "+json+"; should contain 'object-type':'...'");
        }
    }
    
    @SuppressWarnings("deprecation")
    public void testJackson387() throws Exception
    {
        ObjectMapper om = new ObjectMapper();
        om.enableDefaultTyping( ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT, JsonTypeInfo.As.PROPERTY );
        om.getSerializationConfig().setSerializationInclusion( JsonSerialize.Inclusion.NON_NULL );
        om.getSerializationConfig().set( SerializationConfig.Feature.INDENT_OUTPUT, true );

        MyClass mc = new MyClass();

        MyParam<Integer> moc1 = new MyParam<Integer>(1);
        MyParam<String> moc2 = new MyParam<String>("valueX");

        SomeObject so = new SomeObject();
        so.someValue = "xxxxxx"; 
        MyParam<SomeObject> moc3 = new MyParam<SomeObject>(so);

        List<SomeObject> colist = new ArrayList<SomeObject>();
        colist.add( new SomeObject() );
        colist.add( new SomeObject() );
        colist.add( new SomeObject() );
        MyParam<List<SomeObject>> moc4 = new MyParam<List<SomeObject>>(colist);

        mc.params.add( moc1 );
        mc.params.add( moc2 );
        mc.params.add( moc3 );
        mc.params.add( moc4 );

        String json = om.writeValueAsString( mc );
        
        MyClass mc2 = om.readValue(json, MyClass.class );
        assertNotNull(mc2);
        assertNotNull(mc2.params);
        assertEquals(4, mc2.params.size());
    }

    public void testJackson430() throws Exception
    {
        ObjectMapper om = new ObjectMapper();
//        om.getSerializationConfig().setSerializationInclusion( Inclusion.NON_NULL );
        om.setSerializerFactory( new CustomJsonSerializerFactory() );
        MyClass mc = new MyClass();
        mc.params.add(new MyParam<Integer>(1));

        String str = om.writeValueAsString( mc );
//        System.out.println( str );
        
        MyClass mc2 = om.readValue( str, MyClass.class );
        assertNotNull(mc2);
        assertNotNull(mc2.params);
        assertEquals(1, mc2.params.size());
    }
}
