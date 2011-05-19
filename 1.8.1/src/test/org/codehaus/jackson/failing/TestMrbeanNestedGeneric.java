package org.codehaus.jackson.failing;

import java.util.List;

import org.codehaus.jackson.map.BaseMapTest;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.mrbean.MrBeanModule;
import org.codehaus.jackson.type.TypeReference;

/**
 * Tests to verify whether generic declarations are properly handled by Mr Bean.
 * Currently (1.8) this is not the case, and fix is non-trivial; not impossible,
 * just quite difficult.
 */
public class TestMrbeanNestedGeneric extends BaseMapTest
{
    // For [JACKSON-479]
    public interface ResultWrapper<T> {
        T getValue();
    }
    
    public interface Results<T> {
        Long getTotal();
        List<T> getRecords();
    }
    public interface Dog {
        String getName();
        String getBreed();
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */
    
    // For [JACKSON-479]
    public void testTypeReferenceNestedGeneric() throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new MrBeanModule());
        final String JSON = "{\"value\":{\"breed\":\"Poodle\",\"name\":\"Rekku\"}}";

        final ResultWrapper<Dog> result = mapper.readValue(JSON, new TypeReference<ResultWrapper<Dog>>() { });
        Object ob = result.getValue();
        assertEquals(Dog.class, ob.getClass());
    }

    // For [JACKSON-479]
    public void testTypeReferenceNestedGenericList() throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new MrBeanModule());

        final String JSON = "{\"records\":[{\"breed\":\"Mountain Cur\",\"name\":\"Fido\"}],\n"
            +"\"total\":1}";
        
        final Results<Dog> result = mapper.readValue(JSON, new TypeReference<Results<Dog>>() { });

        List<?> records = result.getRecords();
        assertEquals(1, records.size());
        assertEquals(Dog.class, records.get(0).getClass());
    }

}
