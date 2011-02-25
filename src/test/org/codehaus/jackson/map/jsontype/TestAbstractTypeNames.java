package org.codehaus.jackson.map.jsontype;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.annotate.JsonTypeInfo.As;
import org.codehaus.jackson.annotate.JsonTypeInfo.Id;
import org.codehaus.jackson.map.BaseMapTest;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

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
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    public void testEmptyCollection() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
        User friend = new DefaultUser("Joe Hildebrandt", null);
        User coworker = new DefaultEmployee("Richard Nasr",null,"MDA");
        List<User>friends = new ArrayList<User>();
        friends.add(friend);
        friends.add(coworker);
        User user = new DefaultEmployee("John Vanspronssen", friends, "MDA");
        String json = mapper.writeValueAsString(user);

        User result = mapper.readValue(json, User.class);
        assertNotNull(result);
    }    
}
