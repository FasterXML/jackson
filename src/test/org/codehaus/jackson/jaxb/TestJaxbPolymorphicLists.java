package org.codehaus.jackson.jaxb;

import java.util.*;
import javax.xml.bind.annotation.*;

import org.codehaus.jackson.jaxb.TestJaxbPolymorphic.Animal;
import org.codehaus.jackson.jaxb.TestJaxbPolymorphic.Buffalo;
import org.codehaus.jackson.jaxb.TestJaxbPolymorphic.Whale;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * Tests for handling of type-related JAXB annotations with collection (List)
 * types.
 *
 * @since 1.5
 */
public class TestJaxbPolymorphicLists
    extends BaseJaxbTest
{
    /*
     **************************************************************
     * Helper beans
     **************************************************************
     */

     static class ListBean 
     {
         @XmlElements({
                 @XmlElement(type=Buffalo.class, name="beefalot"),
                 @XmlElement(type=Whale.class, name="whale")
         })
         public List<Animal> animals;

         @XmlElements({
             @XmlElement(type=Whale.class, name="whale")
         })
		 public List<Animal> critters;

         public ListBean() { }
         public ListBean(Animal... a) {
             animals = new ArrayList<Animal>();
             for (Animal an : a) {
                 animals.add(an);
             }
         }
     }

     static class Leopard extends Animal {
    	 public Leopard() { super("Lez"); }
     }
     
     /*
      **************************************************************
      * Tests
      **************************************************************
      */
     
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

     
}
