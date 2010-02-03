package org.codehaus.jackson.jaxb;

import java.util.*;
import javax.xml.bind.annotation.*;

import org.codehaus.jackson.map.ObjectMapper;

/**
 * Tests for handling of type-related JAXB annotations 
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

     static abstract class Animal {
         public String nickname;

         protected Animal() { }
         protected Animal(String n) { nickname = n; }
     }

     static class Buffalo extends Animal {
         public String hairColor;

         public Buffalo() { }
         public Buffalo(String name, String hc) {
             super(name);
             hairColor = hc;
         }
     }

     static class Whale extends Animal {
         int weightInTons;
         public Whale() { }
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
    public void testXmlElementTypeDeser() throws Exception
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
}
