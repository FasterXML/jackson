package org.codehaus.jackson.jaxb;

import javax.xml.bind.annotation.*;

import org.codehaus.jackson.map.*;

/**
 * Unit test written for [JACKSON-303]
 */
public class TestAccessType
    extends BaseJaxbTest
{
    /*
     **************************************************************
     * Helper beans
     **************************************************************
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
     **************************************************************
     * Unit tests
     **************************************************************
     */

     public void testXmlElementTypeDeser() throws Exception
     {
         ObjectMapper mapper = getJaxbMapper();

         Model originalModel = new Model();
         originalModel.setName("Foobar");
         String json = mapper.writeValueAsString(originalModel);
//         System.out.println(json); // Outputs {"name":"Name"}
         Model newModel = mapper.readValue(json, Model.class);
         assertEquals("Foobar", newModel.name);
     }

}
