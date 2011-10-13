package org.codehaus.jackson.map;

import static org.codehaus.jackson.map.PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES;

import java.util.Arrays;
import java.util.List;

import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.map.TestNamingStrategy.PersonBean;
import org.junit.Test;

/**
 * Unit tests to verify functioning of 
 * {@link PropertyNamingStrategy#CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES} 
 * inside the context of an ObjectMapper.
 * CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES was added in Jackson 1.9, 
 * as per [JACKSON-598].
 * 
 * @since 1.9
 */
public class TestStdNamingStrategies extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper types
    /**********************************************************
     */

    @JsonPropertyOrder({"www", "some_url", "some_uris"})
    static class Acronyms
    {
        public String WWW;
        public String someURL;
        public String someURIs;
        
        public Acronyms() {this(null, null, null);}
        public Acronyms(String WWW, String someURL, String someURIs)
        {
            this.WWW = WWW;
            this.someURL = someURL;
            this.someURIs = someURIs;
        }
    }
    
    @JsonPropertyOrder({"from_user", "user", "from$user", "from7user", "_"})
    static class UnchangedNames
    {
        public String from_user;
        public String _user;
        public String from$user;
        public String from7user;
        public String _;
        
        public UnchangedNames() {this(null, null, null, null, null);}
        public UnchangedNames(String from_user, String _user, String from$user, String from7user, String _)
        {
            this.from_user = from_user;
            this._user = _user;
            this.from$user = from$user;
            this.from7user = from7user;
            this._ = _;
        }
    }
    
    @JsonPropertyOrder({"results", "user", "__", "$_user"})
    static class OtherNonStandardNames
    {
        public String Results;
        public String _User;
        public String ___;
        public String $User;
        
        public OtherNonStandardNames() {this(null, null, null, null);}
        public OtherNonStandardNames(String Results, String _User, String ___, String $User)
        {
            this.Results = Results;
            this._User = _User;
            this.___ = ___;
            this.$User = $User;
        }
    }
    
    /*
    /**********************************************************
    /* Set up
    /**********************************************************
     */

    public static List<Object[]> NAME_TRANSLATIONS = Arrays.asList(new Object[][] {
                {null, null},
                {"", ""},
                {"a", "a"},
                {"abc", "abc"},
                {"1", "1"},
                {"123", "123"},
                {"1a", "1a"},
                {"a1", "a1"},
                {"$", "$"},
                {"$a", "$a"},
                {"a$", "a$"},
                {"$_a", "$_a"},
                {"a_$", "a_$"},
                {"a$a", "a$a"},
                {"$A", "$_a"},
                {"$_A", "$_a"},
                {"_", "_"},
                {"__", "_"},
                {"___", "__"},
                {"A", "a"},
                {"A1", "a1"},
                {"1A", "1_a"},
                {"_a", "a"},
                {"_A", "a"},
                {"a_a", "a_a"},
                {"a_A", "a_a"},
                {"A_A", "a_a"},
                {"A_a", "a_a"},
                {"WWW", "www"},
                {"someURI", "some_uri"},
                {"someURIs", "some_uris"},
                {"Results", "results"},
                {"_Results", "results"},
                {"_results", "results"},
                {"__results", "_results"},
                {"__Results", "_results"},
                {"___results", "__results"},
                {"___Results", "__results"},
                {"userName", "user_name"},
                {"user_name", "user_name"},
                {"user__name", "user__name"},
                {"UserName", "user_name"},
                {"User_Name", "user_name"},
                {"User__Name", "user__name"},
                {"_user_name", "user_name"},
                {"_UserName", "user_name"},
                {"_User_Name", "user_name"},
                {"UGLY_NAME", "ugly_name"},
                {"_Bars", "bars" }
    });
    
    private ObjectMapper mapper;
    
    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
    }
    
    /*
    /**********************************************************
    /* Test methods for CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES
    /**********************************************************
     */

    /**
     * Unit test to verify translations of 
     * {@link PropertyNamingStrategy#CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES} 
     * outside the context of an ObjectMapper.
     * CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES was added in Jackson 1.9, 
     * as per [JACKSON-598].
     */
    @Test
    public void testLowerCaseStrategyStandAlone()
    {
        for (Object[] pair : NAME_TRANSLATIONS) {
            String translatedJavaName = PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES.nameForField(null, null,
                    (String) pair[0]);
            assertEquals((String) pair[1], translatedJavaName);
        }
    }
    
    public void testLowerCaseTranslations() throws Exception
    {
        // First serialize
        String json = mapper.writeValueAsString(new PersonBean("Joe", "Sixpack", 42));
        assertEquals("{\"first_name\":\"Joe\",\"last_name\":\"Sixpack\",\"age\":42}", json);
        
        // then deserialize
        PersonBean result = mapper.readValue(json, PersonBean.class);
        assertEquals("Joe", result.firstName);
        assertEquals("Sixpack", result.lastName);
        assertEquals(42, result.age);
    }
    
    public void testLowerCaseAcronymsTranslations() throws Exception
    {
        // First serialize
        String json = mapper.writeValueAsString(new Acronyms("world wide web", "http://jackson.codehaus.org", "/path1/,/path2/"));
        assertEquals("{\"www\":\"world wide web\",\"some_url\":\"http://jackson.codehaus.org\",\"some_uris\":\"/path1/,/path2/\"}", json);
        
        // then deserialize
        Acronyms result = mapper.readValue(json, Acronyms.class);
        assertEquals("world wide web", result.WWW);
        assertEquals("http://jackson.codehaus.org", result.someURL);
        assertEquals("/path1/,/path2/", result.someURIs);
    }

    public void testLowerCaseOtherNonStandardNamesTranslations() throws Exception
    {
        // First serialize
        String json = mapper.writeValueAsString(new OtherNonStandardNames("Results", "_User", "___", "$User"));
        assertEquals("{\"results\":\"Results\",\"user\":\"_User\",\"__\":\"___\",\"$_user\":\"$User\"}", json);
        
        // then deserialize
        OtherNonStandardNames result = mapper.readValue(json, OtherNonStandardNames.class);
        assertEquals("Results", result.Results);
        assertEquals("_User", result._User);
        assertEquals("___", result.___);
        assertEquals("$User", result.$User);
    }

    public void testLowerCaseUnchangedNames() throws Exception
    {
        // First serialize
        String json = mapper.writeValueAsString(new UnchangedNames("from_user", "_user", "from$user", "from7user", "_"));
        assertEquals("{\"from_user\":\"from_user\",\"user\":\"_user\",\"from$user\":\"from$user\",\"from7user\":\"from7user\",\"_\":\"_\"}", json);
        
        // then deserialize
        UnchangedNames result = mapper.readValue(json, UnchangedNames.class);
        assertEquals("from_user", result.from_user);
        assertEquals("_user", result._user);
        assertEquals("from$user", result.from$user);
        assertEquals("from7user", result.from7user);
        assertEquals("_", result._);
    }
}
