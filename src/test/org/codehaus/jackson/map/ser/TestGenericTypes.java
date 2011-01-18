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
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    public void testIssue1() throws Exception
    {
        Person1 p1 = new Person1("John");
        p1.setAccount(new Key<Account>(new Account("something", 42L)));
        
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(p1);
        System.out.println(json);
    }

    public void test2() throws Exception
    {
        Person2 p2 = new Person2("John");
        List<Key<Account>> accounts = new ArrayList<Key<Account>>();
        accounts.add(new Key<Account>(new Account("a", 42L)));
        accounts.add(new Key<Account>(new Account("b", 43L)));
        accounts.add(new Key<Account>(new Account("c", 44L)));
        p2.setAccounts(accounts);
        
        ObjectMapper mapper = new ObjectMapper();               
        String json = mapper.writeValueAsString(p2);
        System.out.println(json);
    }
}

