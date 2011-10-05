package org.codehaus.jackson.map.ser;

import java.io.*;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.*;

public class TestViews2 extends BaseMapTest
{

    /*
    /************************************************************************ 
    /* Tests
    /************************************************************************ 
     */
    
  public void testDataBindingUsage( ) throws Exception
  {
    ObjectMapper objectMapper = createObjectMapper( null );
    String result = serializeWithObjectMapper(new ComplexTestData( ), Views.View.class, objectMapper );
    assertEquals(-1, result.indexOf( "nameHidden" ));
  }

  public void testDataBindingUsageWithoutView( ) throws Exception
  {
    ObjectMapper objectMapper = createObjectMapper( null );
    String json = serializeWithObjectMapper(new ComplexTestData( ), null, objectMapper);
    assertTrue(json.indexOf( "nameHidden" ) > 0);
  }

  /*
  /************************************************************************ 
  /* Helper  methods
  /************************************************************************ 
   */

  @SuppressWarnings("deprecation")
  private ObjectMapper createObjectMapper(Class<?> viewClass)
  {
    ObjectMapper objectMapper = new ObjectMapper( );
    objectMapper.configure( SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false );
    objectMapper.getSerializationConfig( ).setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL );
    objectMapper.configure( SerializationConfig.Feature.DEFAULT_VIEW_INCLUSION, false );
//    objectMapper.getSerializationConfig( ).disable( SerializationConfig.Feature.DEFAULT_VIEW_INCLUSION );
//    objectMapper.getSerializationConfig( ).setSerializationView( viewClass );
    return objectMapper;
  }
  
  
  @SuppressWarnings("deprecation")
  private String serializeWithObjectMapper(Object object, Class<? extends Views.View> view, ObjectMapper objectMapper )
      throws IOException
  {
    objectMapper.getSerializationConfig( ).setSerializationView( view );
    return objectMapper.writeValueAsString(object );
  }

  /*
  /************************************************************************ 
  /* Helper classes
  /************************************************************************ 
   */

  static class Views
  {
    public interface View { }
    public interface ExtendedView  extends View { }
  }
  
  static class ComplexTestData
  {
    String nameNull = null;

    String nameComplex = "complexValue";

    String nameComplexHidden = "nameComplexHiddenValue";

    SimpleTestData testData = new SimpleTestData( );

    SimpleTestData[] testDataArray = new SimpleTestData[] { new SimpleTestData( ), null };

    @JsonView( Views.View.class )
    public String getNameNull()
    {
      return nameNull;
    }

    public void setNameNull( String nameNull )
    {
      this.nameNull = nameNull;
    }

    @JsonView( Views.View.class )
    public String getNameComplex()
    {
      return nameComplex;
    }

    public void setNameComplex( String nameComplex )
    {
      this.nameComplex = nameComplex;
    }

    public String getNameComplexHidden()
    {
      return nameComplexHidden;
    }

    public void setNameComplexHidden( String nameComplexHidden )
    {
      this.nameComplexHidden = nameComplexHidden;
    }

    @JsonView( Views.View.class )
    public SimpleTestData getTestData()
    {
      return testData;
    }

    public void setTestData( SimpleTestData testData )
    {
      this.testData = testData;
    }

    @JsonView( Views.View.class )
    public SimpleTestData[] getTestDataArray()
    {
      return testDataArray;
    }

    public void setTestDataArray( SimpleTestData[] testDataArray )
    {
      this.testDataArray = testDataArray;
    }
  }

  static class SimpleTestData
  {
    String name = "shown";

    String nameHidden = "hidden";

    @JsonView( Views.View.class )
    public String getName()
    {
      return name;
    }

    public void setName( String name )
    {
      this.name = name;
    }

    public String getNameHidden( )
    {
      return nameHidden;
    }

    public void setNameHidden( String nameHidden )
    {
      this.nameHidden = nameHidden;
    }
  }

}