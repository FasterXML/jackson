package org.codehaus.jackson.map.ser;

import java.util.*;

import org.codehaus.jackson.map.BaseMapTest;
import org.codehaus.jackson.map.ObjectMapper;


/**
 * @since 1.7
 */
public class TestGenericTypes extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper types
    /**********************************************************
     */
    
    static class Account {
        private Long id;        
        private String name;
        
        public Account(String name, Long id) {
            this.id = id;
            this.name = name;
        }

        public String getName() { return name; }
        public Long getId() { return id; }
    }

    static class Key<T> {
        private final T id;
        
        public Key(T id) { this.id = id; }
        
        public T getId() { return id; }

        public <V> Key<V> getParent() { return null; }
    }
 
    static class Person1 {
        private Long id;
        private String name;
        private Key<Account> account;
        
        public Person1(String name) { this.name = name; }

        public String getName() {
                return name;
        }

        public Key<Account> getAccount() {
                return account;
        }

        public Long getId() {
                return id;
        }

        public void setAccount(Key<Account> account) {
            this.account = account;
        }    
    }

    static class Person2 {
        private Long id;
        private String name;
        private List<Key<Account>> accounts;
        
        public Person2(String name) {
                this.name = name;
        }

        public String getName() { return name; }
        public List<Key<Account>> getAccounts() { return accounts; }
        public Long getId() { return id; }

        public void setAccounts(List<Key<Account>> accounts) {
            this.accounts = accounts;
        }
    }

    static class GenericBogusWrapper<T> {
        public Element wrapped;

        public GenericBogusWrapper(T v) { wrapped = new Element(v); }

        class Element {
            public T value;
    
            public Element(T v) { value = v; }
        }
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    @SuppressWarnings("unchecked")
    public void testIssue468a() throws Exception
    {
        Person1 p1 = new Person1("John");
        p1.setAccount(new Key<Account>(new Account("something", 42L)));
        
        // First: ensure we can serialize (pre 1.7 this failed)
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(p1);

        // and then verify that results make sense
        Map<String,Object> map = mapper.readValue(json, Map.class);
        assertEquals("John", map.get("name"));
        Object ob = map.get("account");
        assertNotNull(ob);
        Map<String,Object> acct = (Map<String,Object>) ob;
        Object idOb = acct.get("id");
        assertNotNull(idOb);
        Map<String,Object> key = (Map<String,Object>) idOb;
        assertEquals("something", key.get("name"));
        assertEquals(Integer.valueOf(42), key.get("id"));
    }

    @SuppressWarnings("unchecked")
    public void testIssue468b() throws Exception
    {
        Person2 p2 = new Person2("John");
        List<Key<Account>> accounts = new ArrayList<Key<Account>>();
        accounts.add(new Key<Account>(new Account("a", 42L)));
        accounts.add(new Key<Account>(new Account("b", 43L)));
        accounts.add(new Key<Account>(new Account("c", 44L)));
        p2.setAccounts(accounts);

        // serialize without error:
        ObjectMapper mapper = new ObjectMapper();               
        String json = mapper.writeValueAsString(p2);

        // then verify output
        Map<String,Object> map = mapper.readValue(json, Map.class);
        assertEquals("John", map.get("name"));
        Object ob = map.get("accounts");
        assertNotNull(ob);
        List<?> acctList = (List<?>) ob;
        assertEquals(3, acctList.size());
        // ... might want to verify more, but for now that should suffice
    }

    /**
     * Issue [JACKSON-572] is about unbound type variables, usually resulting
     * from inner classes of generic classes (like Sets).
     */
    public void testUnboundIssue572() throws Exception
    {
        GenericBogusWrapper<Integer> list = new GenericBogusWrapper<Integer>(Integer.valueOf(7));
        String json = new ObjectMapper().writeValueAsString(list);
        assertEquals("{\"wrapped\":{\"value\":7}}", json);
    }
}

