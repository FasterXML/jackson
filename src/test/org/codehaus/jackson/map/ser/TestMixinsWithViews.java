package org.codehaus.jackson.map.ser;

import java.io.*;
import java.util.*;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonView;

import org.codehaus.jackson.map.BaseMapTest;

public class TestMixinsWithViews
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper bean classes
    /**********************************************************
     */

    static class SimpleTestData
    {
        private String name = "shown";
        private String nameHidden = "hidden";

        public String getName() { return name; }
        public String getNameHidden( ) { return nameHidden; }

      public void setName( String name ) {
        this.name = name;
      }

      public void setNameHidden( String nameHidden ) {
        this.nameHidden = nameHidden;
      }
    }

    static class ComplexTestData
    {
      String nameNull = null;

      String nameComplex = "complexValue";

      String nameComplexHidden = "nameComplexHiddenValue";

      SimpleTestData testData = new SimpleTestData( );

      SimpleTestData[] testDataArray = new SimpleTestData[] { new SimpleTestData( ), null };

      public String getNameNull()
      {
        return nameNull;
      }

      public void setNameNull( String nameNull )
      {
        this.nameNull = nameNull;
      }

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

      public SimpleTestData getTestData()
      {
        return testData;
      }

      public void setTestData( SimpleTestData testData )
      {
        this.testData = testData;
      }

      public SimpleTestData[] getTestDataArray()
      {
        return testDataArray;
      }

      public void setTestDataArray( SimpleTestData[] testDataArray )
      {
        this.testDataArray = testDataArray;
      }
    }    

    public interface TestDataJAXBMixin
    {
      @JsonView( Views.View.class )
      String getName( );
    }

    public interface TestComplexDataJAXBMixin
    {
      @JsonView( Views.View.class )
      String getNameNull();

      @JsonView( Views.View.class )
      String getNameComplex();

      @JsonView( Views.View.class )
      String getNameComplexHidden();

      @JsonView( Views.View.class )
      SimpleTestData getTestData();

      @JsonView( Views.View.class )
      SimpleTestData[] getTestDataArray( );
    }

    static class Views {
        static class View { }
    }
    
    /*
    /**********************************************************
    /* Tests
    /**********************************************************
     */
    
    public void testDataBindingUsage( ) throws Exception
    {
      ObjectMapper objectMapper = createObjectMapper( );
      String json = serializeWithObjectMapper(new ComplexTestData( ), Views.View.class, objectMapper);
      assertTrue( json.indexOf( "nameHidden" ) == -1 );
      assertTrue( json.indexOf( "\"name\" : \"shown\"" ) > 0 );
    }    

    private ObjectMapper createObjectMapper( )
    {
      ObjectMapper objectMapper = new ObjectMapper( );
      objectMapper.configure( SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false );
      objectMapper.getSerializationConfig( ).setSerializationInclusion( JsonSerialize.Inclusion.NON_NULL );
      objectMapper.configure( SerializationConfig.Feature.DEFAULT_VIEW_INCLUSION, false );
      objectMapper.getSerializationConfig( ).setMixInAnnotations( createMixins( ) );
      return objectMapper;
    }

    private Map<Class<?>, Class<?>> createMixins( )
    {
      Map<Class<?>, Class<?>> sourceMixins = new HashMap<Class<?>, Class<?>>( );
      sourceMixins.put( SimpleTestData.class, TestDataJAXBMixin.class );
      sourceMixins.put( ComplexTestData.class, TestComplexDataJAXBMixin.class );
      return sourceMixins;
    }

    private String serializeWithObjectMapper(Object object, Class<?> view, ObjectMapper objectMapper ) throws IOException
    {
      ObjectWriter objectWriter = objectMapper.viewWriter(view).withDefaultPrettyPrinter( );
      return objectWriter.writeValueAsString(object);
    }
}
