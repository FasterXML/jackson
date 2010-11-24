package org.codehaus.jackson.map.jsontype;

import java.util.*;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.annotate.JsonSubTypes.Type;
import org.codehaus.jackson.annotate.JsonTypeInfo.As;
import org.codehaus.jackson.annotate.JsonTypeInfo.Id;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonSerialize;
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
    
    /*
     ****************************************************** 
     * Unit tests
     ****************************************************** 
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
        String json = mapper.typedWriter(TypeFactory.parametricType(ContainerWithGetter.class, Animal.class)).writeValueAsString(c2);
        if (json.indexOf("\"object-type\":\"doggy\"") < 0) {
            fail("polymorphic type not kept, result == "+json+"; should contain 'object-type':'...'");
        }
    }

    @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@classAttr1")
    private static class MyClass {
            public List<MyParam<?>> items = new ArrayList<MyParam<?>>();
    }

//    @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@classAttr2")
    private static class MyParam<T> {
        @SuppressWarnings("unused")
        public T value = null;
    }

    private static class SomeObject {
        @SuppressWarnings("unused")
        public String someValue = UUID.randomUUID().toString();
    }
    
    public void testJackson387() throws Exception
    {
        ObjectMapper om = new ObjectMapper();
        om.enableDefaultTyping( ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT, JsonTypeInfo.As.PROPERTY );
        om.getSerializationConfig().setSerializationInclusion( JsonSerialize.Inclusion.NON_NULL );
        om.getSerializationConfig().set( SerializationConfig.Feature.INDENT_OUTPUT, true );

        MyClass mc = new MyClass();

        MyParam<Integer> moc1 = new MyParam<Integer>();
        moc1.value = 1;

        MyParam<String> moc2 = new MyParam<String>();
        moc2.value = "valueX";

        MyParam<SomeObject> moc3 = new MyParam<SomeObject>();
        SomeObject so = new SomeObject();
        so.someValue = "xxxxxx"; 
        moc3.value = so;

        List<SomeObject> colist = new ArrayList<SomeObject>();
        colist.add( new SomeObject() );
        colist.add( new SomeObject() );
        colist.add( new SomeObject() );
        MyParam<List<SomeObject>> moc4 = new MyParam<List<SomeObject>>();
        moc4.value = colist;

        mc.items.add( moc1 );
        mc.items.add( moc2 );
        mc.items.add( moc3 );
        mc.items.add( moc4 );

        String str = om.writeValueAsString( mc );

        System.out.println( str );

        MyClass mc2 = om.readValue( str, MyClass.class );
        assertNotNull(mc2);
    }
}
