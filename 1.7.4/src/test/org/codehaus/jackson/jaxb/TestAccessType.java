package org.codehaus.jackson.jaxb;

import java.util.Date;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

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

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "LoggedActivity")
    public static class Bean288
    {
        @XmlElement(required = true, type = String.class)
        @XmlJavaTypeAdapter(MyAdapter.class)
        @XmlSchemaType(name = "date")
        public Date date;
    }

    public static class MyAdapter
        extends XmlAdapter<String, Date>
    {
        @Override
        public String marshal(Date arg) throws Exception {
            return "String="+arg.getTime();
        }
        @Override
        public Date unmarshal(String arg0) throws Exception {
            return new Date(Long.parseLong(arg0));
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

     public void testForJackson288() throws Exception
     {
         final long TIMESTAMP = 12345678L;
         ObjectMapper mapper = getJaxbMapper();
         Bean288 bean = mapper.readValue("{\"date\":"+TIMESTAMP+"}", Bean288.class);
         assertNotNull(bean);
         Date d = bean.date;
         assertNotNull(d);
         assertEquals(TIMESTAMP, d.getTime());
     }
}
