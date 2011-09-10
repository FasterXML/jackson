package org.codehaus.jackson.map.jsontype;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.annotate.JsonTypeInfo.As;
import org.codehaus.jackson.annotate.JsonTypeInfo.Id;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.ObjectMapper.DefaultTyping;

/**
 * Unit tests for checking how combination of interfaces, implementation
 * classes are handled, with respect to type names.
 * 
 * @since 1.8
 */
public class TestAbstractTypeNames  extends BaseMapTest
{
    @JsonTypeName("Employee")
    public interface Employee extends User {
        public abstract String getEmployer();
    }

    @JsonTypeInfo(use=Id.NAME, include=As.PROPERTY, property="userType")
    @JsonTypeName("User")
    @JsonSubTypes({ @JsonSubTypes.Type(value=Employee.class,name="Employee") })
    public interface User {
            public abstract String getName();
            public abstract List<User> getFriends();
    }

    @JsonTypeName("Employee")
    static class DefaultEmployee extends DefaultUser implements Employee
    {
        private String _employer;

        @JsonCreator
        public DefaultEmployee(@JsonProperty("name") String name,
                @JsonProperty("friends") List<User> friends,
                @JsonProperty("employer") String employer) {
            super(name, friends);
            _employer = employer;
        }

        @Override
        public String getEmployer() {
            return _employer;
        }
    }

    @JsonTypeInfo(use=Id.NAME, include=As.PROPERTY, property="userType")
    @JsonTypeName("User")
    @JsonSubTypes({ @JsonSubTypes.Type(value=DefaultEmployee.class,name="Employee") })
    static class DefaultUser implements User
    {
        private String _name;
        private List<User> _friends;

        @JsonCreator
        public DefaultUser(@JsonProperty("name") String name,
                @JsonProperty("friends") List<User> friends)
        {
            super();
            _name = name;
            _friends = friends;
        }

        @Override public String getName() {
            return _name;
        }

        @Override public List<User> getFriends() {
            return _friends;
        }
    }

    static class BaseValue {
        public int value = 42;

        public int getValue() { return value; }
    }

    final static class BeanWithAnon {
        public BaseValue bean = new BaseValue() {
            @Override
            public String toString() { return "sub!"; }
        };
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    // Testing [JACKSON-498], partial fix
    public void testEmptyCollection() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
        List<User>friends = new ArrayList<User>();
        friends.add(new DefaultUser("Joe Hildebrandt", null));
        friends.add(new DefaultEmployee("Richard Nasr",null,"MDA"));

        User user = new DefaultEmployee("John Vanspronssen", friends, "MDA");
        String json = mapper.writeValueAsString(user);

        /* 24-Feb-2011, tatu: For now let's simply require registration of
         *   concrete subtypes; can't think of a way to avoid that for now
         */
        mapper = new ObjectMapper();
        mapper.registerSubtypes(DefaultEmployee.class);
        mapper.registerSubtypes(DefaultUser.class);
        
        User result = mapper.readValue(json, User.class);
        assertNotNull(result);
        assertEquals(DefaultEmployee.class, result.getClass());

        friends = result.getFriends();
        assertEquals(2, friends.size());
        assertEquals(DefaultUser.class, friends.get(0).getClass());
        assertEquals(DefaultEmployee.class, friends.get(1).getClass());
    }
    
    // [JACKSON-584]: change anonymous non-static inner type into static type:
    public void testInnerClassWithType() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enableDefaultTyping(DefaultTyping.NON_FINAL);
        String json = mapper.writeValueAsString(new BeanWithAnon());
        BeanWithAnon result = mapper.readValue(json, BeanWithAnon.class);
        assertEquals(BeanWithAnon.class, result.getClass());
    }
}
