package org.codehaus.jackson.map.jsontype;

import java.util.*;

import org.junit.Assert;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.annotate.JsonTypeName;
import org.codehaus.jackson.annotate.JsonSubTypes.Type;
import org.codehaus.jackson.annotate.JsonTypeInfo.As;
import org.codehaus.jackson.annotate.JsonTypeInfo.Id;
import org.codehaus.jackson.map.BaseMapTest;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.type.TypeReference;

public class TestTypedContainerSerialization
	extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper types
    /**********************************************************
     */

    @JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "object-type")
    @JsonSubTypes( { @Type(value = Dog.class, name = "doggy"),
        @Type(value = Cat.class, name = "kitty") })
    static abstract class Animal {
	    public String name;

	    protected Animal(String n) {
	        name = n;
	    }
	}

	@JsonTypeName("doggie")
	static class Dog extends Animal {
		public int boneCount;

		public Dog() {
			super(null);
		}

		@JsonCreator
		public Dog(@JsonProperty("name") String name) {
			super(name);
		}

		public void setBoneCount(int i) {
			boneCount = i;
		}
	}

	@JsonTypeName("kitty")
	static class Cat extends Animal {
		public String furColor;

		public Cat() {
			super(null);
		}

		@JsonCreator
		public Cat(@JsonProperty("furColor") String c) {
			super(null);
			furColor = c;
		}

		public void setName(String n) {
			name = n;
		}
	}

	static class Container1 {
		Animal animal;

		public Animal getAnimal() {
			return animal;
		}

		public void setAnimal(Animal animal) {
			this.animal = animal;
		}
	}

	static class Container2<T extends Animal> {
		@JsonSerialize
		T animal;

		public T getAnimal() {
			return animal;
		}

		public void setAnimal(T animal) {
			this.animal = animal;
		}

	}

    @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
    static class Issue508A { }
    static class Issue508B extends Issue508A { }

    private final static ObjectMapper mapper = new ObjectMapper();

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */
	
    public void testIssue265() throws Exception
    {
		Dog dog = new Dog("medor");
		dog.setBoneCount(3);
		Container1 c1 = new Container1();
		c1.setAnimal(dog);
		String s1 = mapper.writeValueAsString(c1);
		Assert.assertTrue("polymorphic type info is kept (1)", s1
				.indexOf("\"object-type\":\"doggy\"") >= 0);
		Container2<Animal> c2 = new Container2<Animal>();
		c2.setAnimal(dog);
		String s2 = mapper.writeValueAsString(c2);
		Assert.assertTrue("polymorphic type info is kept (2)", s2
				.indexOf("\"object-type\":\"doggy\"") >= 0);
    }

    public void testIssue329() throws Exception
    {
            ArrayList<Animal> animals = new ArrayList<Animal>();
            animals.add(new Dog("Spot"));
            JavaType rootType = TypeFactory.defaultInstance().constructParametricType(Iterator.class, Animal.class);
            String json = mapper.writerWithType(rootType).writeValueAsString(animals.iterator());
            if (json.indexOf("\"object-type\":\"doggy\"") < 0) {
                fail("No polymorphic type retained, should be; JSON = '"+json+"'");
            }
    }

    public void testIssue508() throws Exception
    {
            List<List<Issue508A>> l = new ArrayList<List<Issue508A>>();
            List<Issue508A> l2 = new ArrayList<Issue508A>();
            l2.add(new Issue508A());
            l.add(l2);
            TypeReference<?> typeRef = new TypeReference<List<List<Issue508A>>>() {};
            String json = mapper.writerWithType(typeRef).writeValueAsString(l);

            List<?> output = mapper.readValue(json, typeRef);
            assertEquals(1, output.size());
            Object ob = output.get(0);
            assertTrue(ob instanceof List<?>);
            List<?> list2 = (List<?>) ob;
            assertEquals(1, list2.size());
            ob = list2.get(0);
            assertSame(Issue508A.class, ob.getClass());
    }
}
