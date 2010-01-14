package org.codehaus.jackson.map.jsontype;

import java.util.*;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.annotate.JsonTypeName;

import static org.codehaus.jackson.map.annotate.JsonTypeInfo.*;

public class TestTypedSerialization
    extends BaseMapTest
{
    /*
     ****************************************************** 
     * Helper types
     ****************************************************** 
     */

    /**
     * Polymorphic base class
     */
    @JsonTypeInfo(use=Id.CLASS, include=As.PROPERTY)
    static class Animal {
        public String name;
        
        protected Animal(String n)  { name = n; }
    }

    @JsonTypeName("doggie")
    static class Dog extends Animal
    {
        public int boneCount;
        
        private Dog() { super(null); }
        public Dog(String name, int b) {
            super(name);
            boneCount = b;
        }
    }
    
    @JsonTypeName("kitty")
    static class Cat extends Animal
    {
        public String furColor;
        
        private Cat() { super(null); }
        public Cat(String name, String c) {
            super(name);
            furColor = c;
        }
    }

    /*
     ****************************************************** 
     * Unit tests
     ****************************************************** 
     */
    
    /**
     * First things first, let's ensure we can serialize using
     * class name, written as main-level property name
     */
    public void testSimpleClassAsProperty() throws Exception
    {
        Map<String,Object> result = writeAndMap(new Cat("Beelzebub", "tabby"));
        assertEquals(3, result.size());
        assertEquals("Beelzebub", result.get("name"));
        assertEquals("tabby", result.get("furColor"));
        // should we try customized class name?
        String classProp = Id.CLASS.getDefaultPropertyName();
        assertEquals(Cat.class.getName(), result.get(classProp));
    }
}


