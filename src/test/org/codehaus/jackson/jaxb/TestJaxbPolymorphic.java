package org.codehaus.jackson.jaxb;

import java.util.*;
import javax.xml.bind.annotation.*;

import org.codehaus.jackson.map.ObjectMapper;

/**
 * Tests for handling of type-related JAXB annotations 
 *
 * @since 1.5
 */
public class TestJaxbPolymorphic 
    extends BaseJaxbTest
{
    /*
     **************************************************************
     * Helper beans
     **************************************************************
     */

     static class Bean 
     {
         @XmlElements({
                 @XmlElement(type=Buffalo.class, name="beefalot"),
                 @XmlElement(type=Whale.class, name="whale")
         })
         public Animal animal;

         public Bean() { }
         public Bean(Animal a) { animal = a; }
     }

     static class ListBean 
     {
         @XmlElements({
                 @XmlElement(type=Buffalo.class, name="beefalot"),
                 @XmlElement(type=Whale.class, name="whale")
         })
         public List<Animal> animals;

         public ListBean() { }
         public ListBean(Animal... a) {
             animals = new ArrayList<Animal>();
             for (Animal an : a) {
                 animals.add(an);
             }
         }
     }

     static class ArrayBean 
     {
         @XmlElements({
                 @XmlElement(type=Buffalo.class, name="b"),
                 @XmlElement(type=Whale.class, name="w")
         })
         public Animal[] animals;

         public ArrayBean() { }
         public ArrayBean(Animal... a) {
             animals = a;
         }
     }

     static class MapBean 
     {
         @XmlElements({
                 @XmlElement(type=Buffalo.class, name="beefalot"),
                 @XmlElement(type=Whale.class, name="whale")
         })
         public Map<Integer,Animal> animals;

         public MapBean() {
             animals = new HashMap<Integer,Animal>();
         }

         public void add(Integer key, Animal value) { animals.put(key, value); }
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

     /*
      **************************************************************
      * Tests
      **************************************************************
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
         assertEquals(1, map.size());
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

     /**
      * And then a test for collection types
      */
     public void testPolymorphicList() throws Exception
     {
         ObjectMapper mapper = getJaxbMapper();
         ListBean input = new ListBean(new Whale("bluey", 150),
                 new Buffalo("Bob", "black")
         );
         String str = mapper.writeValueAsString(input);
         // Let's assume it's ok, and try deserialize right away:         
         ListBean result = mapper.readValue(str, ListBean.class);
         assertEquals(2, result.animals.size());
         Animal a1 = result.animals.get(0);
         assertNotNull(a1);
         assertEquals(Whale.class, a1.getClass());
         assertEquals("bluey", a1.nickname);
         assertEquals(150, ((Whale)a1).weightInTons); 
         Animal a2 = result.animals.get(1);
         assertNotNull(a2);
         assertEquals(Buffalo.class, a2.getClass());
         assertEquals("Bob", a2.nickname);
         assertEquals("black", ((Buffalo) a2).hairColor);
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

     public void testPolymorphicMap() throws Exception
     {
         ObjectMapper mapper = getJaxbMapper();
         Animal a = new Whale("Jaska", 3000);
         Animal b = new Whale("Arska", 2000);
         Animal c = new Whale("Pena", 1500);
         MapBean input = new MapBean();
         input.add(1, a);
         input.add(2, b);
         input.add(3, c);
         String str = mapper.writeValueAsString(input);

         MapBean result = mapper.readValue(str, MapBean.class);
         Map<Integer,Animal> map = result.animals;
         assertEquals(3, map.size());
         assertEquals("Jaska", ((Whale) map.get(Integer.valueOf(1))).nickname);
         assertEquals("Arska", ((Whale) map.get(Integer.valueOf(2))).nickname);
         assertEquals("Pena", ((Whale) map.get(Integer.valueOf(3))).nickname);
     }
}
