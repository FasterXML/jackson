package org.codehaus.jackson.map.jsontype;

import org.codehaus.jackson.map.annotate.JsonSubTypes;
import org.codehaus.jackson.map.annotate.JsonSubTypes.Type;
import org.codehaus.jackson.map.jsontype.TestTypedSerialization.Cat;
import org.codehaus.jackson.map.jsontype.TestTypedSerialization.Dog;

/**
 * Separate tests for verifying that "type name" type id mechanism
 * works.
 * 
 * @author tatu
 */
public class TestTypeNames {

    /*
     ****************************************************** 
     * Helper types
     ****************************************************** 
     */
    
    @JsonSubTypes({
        @Type(value=Dog.class, name="doggy"),
        @Type(Cat.class) /* defaults to "TestTypedNames$Cat" then */
    })
    static class Animal
    {
        public String name;
    }

    static class Dog extends Animal
    {
        public int ageInYears;
    }

    @JsonSubTypes({
        @Type(value=MaineCoon.class, name="hairyBeast")
    })
    abstract static class Cat extends Animal {
        public boolean purrs;
    }

    static class MaineCoon extends Cat { }


    /*
     ****************************************************** 
     * Unit tests
     ****************************************************** 
     */
    
}
