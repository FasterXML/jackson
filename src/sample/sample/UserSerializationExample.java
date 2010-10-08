package sample;
// no package, i.e. at root of sample/extra

import java.io.*;

import org.codehaus.jackson.map.*;

/**
 * A very simple example of full Jackson serialization.
 */
public class UserSerializationExample
{
    public static void main(String[] args)
            throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        StringWriter sw = new StringWriter();
        mapper.writeValue(sw, new User());
        System.out.println("--- JSON ---");
        System.out.println(sw.toString());
        System.out.println("--- /JSON ---");
    }
}

class User {
    public enum Gender { MALE, FEMALE };
    
    public Name getName() { return new Name(); }
    public Address getAddress() { return new Address(); }
    public boolean isVerified() { return true; }
    public Gender getGender() { return Gender.MALE; }
    public byte[] getUserImage() throws Exception { return "Foobar!".getBytes(); }
}
  
class Name {
    public Name() { }
    public String getFirst() { return "Santa"; }
    public String getLast() { return "Claus"; }
}

class Address {
    public Address() { }
    public String getStreet() { return "1 Deadend Street"; }
    public String getCity() { return "Mercer Island"; }
    public String getState() { return "WA"; }
    public int getZip() { return 98040; }
    public String getCountry() { return "US"; }
}
