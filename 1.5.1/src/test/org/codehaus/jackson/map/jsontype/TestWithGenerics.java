package org.codehaus.jackson.map.jsontype;

import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.annotate.JsonSubTypes.Type;
import org.codehaus.jackson.annotate.JsonTypeInfo.As;
import org.codehaus.jackson.annotate.JsonTypeInfo.Id;
import org.codehaus.jackson.map.*;

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

    static class Container2<T extends Animal> {
        public T animal;

        public Container2(T a) { animal = a; }
    }

    /*
     ****************************************************** 
     * Unit tests
     ****************************************************** 
     */

    public void testWrapper() throws Exception
    {
        Dog dog = new Dog("medor", 3);
        Container2<Animal> c2 = new Container2<Animal>(dog);
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(c2);
        if (json.indexOf("\"object-type\":\"doggy\"") < 0) {
            fail("polymorphic type not kept, result == "+json+"; should contain 'object-type':'...'");
        }
    }
}
