package org.codehaus.jackson.map.deser;

import org.codehaus.jackson.map.*;

public class TestInnerClass extends BaseMapTest
{
    /*
    /**********************************************************
    /* Value classes
    /**********************************************************
     */

    // [JACKSON-594]
    static class Dog
    {
      public String name;
      public Brain brain;

      public Dog() { }
      public Dog(String n, boolean thinking) {
          name = n;
          brain = new Brain();
          brain.isThinking = thinking;
      }
      
      // note: non-static
      public class Brain {
          public boolean isThinking;

          public String parentName() { return name; }
      }
    }

    /*
    /**********************************************************
    /* Tests
    /**********************************************************
     */

    public void testSimpleNonStaticInner() throws Exception
    {
        // Let's actually verify by first serializing, then deserializing back
        ObjectMapper mapper = new ObjectMapper();
        Dog input = new Dog("Smurf", true);
        String json = mapper.writeValueAsString(input);
//System.out.println("JSON = "+json);
        Dog output = mapper.readValue(json, Dog.class);
        assertEquals("Smurf", output.name);
        assertNotNull(output.brain);
        assertTrue(output.brain.isThinking);
        // and verify correct binding...
        assertEquals("Smurf", output.brain.parentName());
        output.name = "Foo";
        assertEquals("Foo", output.brain.parentName());
    }
}
