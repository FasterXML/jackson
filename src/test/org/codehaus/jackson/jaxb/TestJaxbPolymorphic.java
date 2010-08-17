package org.codehaus.jackson.jaxb;

import java.util.*;
import javax.xml.bind.annotation.*;

import org.codehaus.jackson.map.ObjectMapper;

/**
 * Tests for handling of type-related JAXB annotations 
 *
 * @since 1.5
 * 
 * @author Tatu Saloranta
 * @author Ryan Heaton
 */
public class TestJaxbPolymorphic 
    extends BaseJaxbTest
{
    /*
    /**********************************************************
    /* Helper beans
    /**********************************************************
     */

     static class Bean 
     {
         @XmlElements({
                 @XmlElement(type=Buffalo.class, name="beefalot"),
                 @XmlElement(type=Whale.class, name="whale")
         })
         public Animal animal;

         @XmlElementRefs({
                 @XmlElementRef(type=Emu.class),
                 @XmlElementRef(type=Cow.class)
         })
         public Animal other;

         public Bean() { }
         public Bean(Animal a) { animal = a; }
     }

     static class ArrayBean 
     {
         @XmlElements({
                 @XmlElement(type=Buffalo.class, name="b"),
                 @XmlElement(type=Whale.class, name="w")
         })
         public Animal[] animals;

         @XmlElementRefs({
                 @XmlElementRef(type=Emu.class),
                 @XmlElementRef(type=Cow.class)
         })
         public Animal[] otherAnimals;

         public ArrayBean() { }
         public ArrayBean(Animal... a) {
             animals = a;
         }
     }
     
     static abstract class Animal {
         public String nickname;

         protected Animal(String n) { nickname = n; }
     }

     static class Buffalo extends Animal {
         public String hairColor;

         public Buffalo() { this(null, null); }
         public Buffalo(String name, String hc) {
             super(name);
             hairColor = hc;
         }
     }

     static class Whale extends Animal {
         public int weightInTons;
         public Whale() { this(null, 0); }
         public Whale(String n, int w) {
             super(n);
             weightInTons = w;
         }
     }

     @XmlRootElement
     static class Emu extends Animal {
         public String featherColor;
         public Emu() { this(null, null); }
         public Emu(String n, String w) {
             super(n);
             featherColor = w;
         }
     }

     @XmlRootElement (name="moo")
     static class Cow extends Animal {
         public int weightInPounds;
         public Cow() { this(null, 0); }
         public Cow(String n, int w) {
             super(n);
             weightInPounds = w;
         }
     }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

     /**
      * First a simple test with non-collection field
      */
     @SuppressWarnings("unchecked")
     public void testSinglePolymorphic() throws Exception
     {
         ObjectMapper mapper = getJaxbMapper();
         Bean input = new Bean(new Buffalo("Billy", "brown"));
         String str = mapper.writeValueAsString(input);
         // First: let's verify output looks like what we expect:
         Map<String,Object> map = mapper.readValue(str, Map.class);
         assertEquals(2, map.size());
         Map<String,Object> map2 = (Map<String,Object>) map.get("animal");
         assertNotNull(map2);
         // second level, should have type info as WRAPPER_OBJECT
         assertEquals(1, map2.size());
         assertTrue(map2.containsKey("beefalot"));
         Map<String,Object> map3 = (Map<String,Object>) map2.get("beefalot");
         assertEquals(2, map3.size());
         // good enough, let's deserialize
         
         Bean result = mapper.readValue(str, Bean.class);
         Animal a = result.animal;
         assertNotNull(a);
         assertEquals(Buffalo.class, a.getClass());
         assertEquals("Billy", a.nickname);
         assertEquals("brown", ((Buffalo) a).hairColor);
     }

     public void testPolymorphicArray() throws Exception
     {
         ObjectMapper mapper = getJaxbMapper();
         Animal a1 = new Buffalo("Bill", "grey");
         Animal a2 = new Whale("moe", 3000);
         ArrayBean input = new ArrayBean(a1, null, a2);
         String str = mapper.writeValueAsString(input);
         ArrayBean result = mapper.readValue(str, ArrayBean.class);
         assertEquals(3, result.animals.length);
         a1 = result.animals[0];
         assertNull(result.animals[1]);
         a2 = result.animals[2];
         assertNotNull(a1);
         assertNotNull(a2);
         assertEquals(Buffalo.class, a1.getClass());
         assertEquals(Whale.class, a2.getClass());
         assertEquals("Bill", a1.nickname);
         assertEquals("grey", ((Buffalo) a1).hairColor);

         assertEquals("moe", a2.nickname);
         assertEquals(3000, ((Whale)a2).weightInTons); 
     }

     public void testPolymorphicArrayElementRef() throws Exception
     {
         ObjectMapper mapper = getJaxbMapper();
         Animal a1 = new Emu("Bill", "grey");
         Animal a2 = new Cow("moe", 3000);
         ArrayBean input = new ArrayBean();
         input.otherAnimals = new Animal[]{a1, null, a2};
         String str = mapper.writeValueAsString(input);
         ArrayBean result = mapper.readValue(str, ArrayBean.class);
         assertEquals(3, result.otherAnimals.length);
         a1 = result.otherAnimals[0];
         assertNull(result.otherAnimals[1]);
         a2 = result.otherAnimals[2];
         assertNotNull(a1);
         assertNotNull(a2);
         assertEquals(Emu.class, a1.getClass());
         assertEquals(Cow.class, a2.getClass());
         assertEquals("Bill", a1.nickname);
         assertEquals("grey", ((Emu) a1).featherColor);

         assertEquals("moe", a2.nickname);
         assertEquals(3000, ((Cow)a2).weightInPounds);
     }
}
