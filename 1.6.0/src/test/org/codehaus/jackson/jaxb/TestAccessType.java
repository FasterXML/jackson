package org.codehaus.jackson.jaxb;

import javax.xml.bind.annotation.*;

import org.codehaus.jackson.map.*;

/**
 * Unit test(s) written for [JACKSON-303]; we should be able to detect setter
 * even though it is not annotated, because there is matching annotated getter.
 */
public class TestAccessType
    extends BaseJaxbTest
{
    /*
    /**********************************************************
    /* Helper beans
    /**********************************************************
     */

    @XmlRootElement(name = "model")
    @XmlAccessorType(XmlAccessType.NONE)
    public static class Model
    {
       protected String name;

       @XmlElement
       public String getName() {
          return name;
       }

       public void setName(String name) {
          this.name = name;
       }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

     public void testXmlElementTypeDeser() throws Exception
     {
         ObjectMapper mapper = getJaxbMapper();

         Model originalModel = new Model();
         originalModel.setName("Foobar");
         String json = mapper.writeValueAsString(originalModel);
         Model newModel = null;
         try {
             newModel = mapper.readValue(json, Model.class);
         } catch (Exception ie) {
             fail("Failed to deserialize: "+ie.getMessage());
         }
         if (!"Foobar".equals(newModel.name)) {
             fail("Failed, JSON == '"+json+"')");
         }
     }
}
