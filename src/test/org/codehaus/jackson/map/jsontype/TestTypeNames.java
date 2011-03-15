package org.codehaus.jackson.map.jsontype;

import java.util.*;

import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.annotate.JsonTypeName;
import org.codehaus.jackson.annotate.JsonSubTypes.Type;
import org.codehaus.jackson.annotate.JsonTypeInfo.As;
import org.codehaus.jackson.annotate.JsonTypeInfo.Id;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.type.TypeFactory;

/**
 * Separate tests for verifying that "type name" type id mechanism
 * works.
 * 
 * @author tatu
 */
public class TestTypeNames extends BaseMapTest
{
    @SuppressWarnings("serial")
    static class AnimalMap extends LinkedHashMap<String,Animal> { }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    public void testSerialization() throws Exception
    {
        ObjectMapper m = new ObjectMapper();

        // Note: need to use wrapper array just so that we can define
        // static type on serialization. If we had root static types,
        // could use those; but at the moment root type is dynamic
        
        assertEquals("[{\"doggy\":{\"name\":\"Spot\",\"ageInYears\":3}}]",
                m.writeValueAsString(new Animal[] { new Dog("Spot", 3) }));
        assertEquals("[{\"MaineCoon\":{\"name\":\"Belzebub\",\"purrs\":true}}]",
                m.writeValueAsString(new Animal[] { new MaineCoon("Belzebub", true)}));
    }

    public void testRoundTrip() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        Animal[] input = new Animal[] {
                new Dog("Odie", 7),
                null,
                new MaineCoon("Piru", false),
                new Persian("Khomeini", true)
        };
        String json = m.writeValueAsString(input);
        List<Animal> output = m.readValue(json,
                TypeFactory.defaultInstance().constructCollectionType(ArrayList.class, Animal.class));
        assertEquals(input.length, output.size());
        for (int i = 0, len = input.length; i < len; ++i) {
            assertEquals("Entry #"+i+" differs, input = '"+json+"'",
                input[i], output.get(i));
        }
    }

    public void testRoundTripMap() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        AnimalMap input = new AnimalMap();
        input.put("venla", new MaineCoon("Venla", true));
        input.put("ama", new Dog("Amadeus", 13));
        String json = m.writeValueAsString(input);
        AnimalMap output = m.readValue(json, AnimalMap.class);
        assertEquals(input, output);
    }
}

/*
/**********************************************************
/* Helper types
/**********************************************************
 */

@JsonTypeInfo(use=Id.NAME, include=As.WRAPPER_OBJECT)
@JsonSubTypes({
    @Type(value=Dog.class, name="doggy"),
    @Type(Cat.class) /* defaults to "TestTypedNames$Cat" then */
})
class Animal
{
    public String name;


    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o == null) return false;
        if (o.getClass() != getClass()) return false;
        return name.equals(((Animal) o).name);
    }

}

class Dog extends Animal
{
    public int ageInYears;

    public Dog() { }
    public Dog(String n, int y) {
        name = n;
        ageInYears = y;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o)
            && ((Dog) o).ageInYears == ageInYears;
    }
}

@JsonSubTypes({
    @Type(MaineCoon.class),
    @Type(Persian.class)
})
abstract class Cat extends Animal {
    public boolean purrs;
    public Cat() { }
    public Cat(String n, boolean p) {
        name = n;
        purrs = p;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o) && ((Cat) o).purrs == purrs;
    }
}

/* uses default name ("MaineCoon") since there's no @JsonTypeName,
 * nor did supertype specify name
 */
class MaineCoon extends Cat {
    public MaineCoon() { super(); }
    public MaineCoon(String n, boolean p) {
        super(n, p);
    }
}

@JsonTypeName("persialaisKissa")
class Persian extends Cat {
    public Persian() { super(); }
    public Persian(String n, boolean p) {
        super(n, p);
    }
}

