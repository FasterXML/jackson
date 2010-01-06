package org.codehaus.jackson.map.deser;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.*;

/**
 * Unit tests for verifying that it is possible to annotate
 * various kinds of things with {@link @JsonCreator} annotation.
 */
public class TestPolymorphicCreators
    extends BaseMapTest
{
    /*
    **********************************************
    * Helper beans
    **********************************************
     */

    static class Animal
    {
	// All animals have names, for our demo purposes...
	public String name;

	protected Animal() { }

	/**
	 * Creator method that can instantiate instances of
	 * appropriate polymoprphic type
	 */
        @JsonCreator
	public static Animal create(@JsonProperty("type") String type)
	{
	    if ("dog".equals(type)) {
		return new Dog();
	    }
	    if ("cat".equals(type)) {
		return new Cat();
	    }
	    throw new IllegalArgumentException("No such animal type ('"+type+"')");
	}
    }

    static class Dog extends Animal
    {
	double barkVolume; // in decibels
	public Dog() { }
	public void setBarkVolume(double v) { barkVolume = v; }
    }

    static class Cat extends Animal
    {
	boolean likesCream;
	public int lives;
	public Cat() { }
	public void setLikesCream(boolean likesCreamSurely) { likesCream = likesCreamSurely; }
    }

    /*
    **********************************************
    * Actual tests
    **********************************************
     */

    /**
     * Simple test to verify that it is possible to implement polymorphic
     * deserialization manually.
     */
    public void testManualPolymorphic() throws Exception
    {
	ObjectMapper mapper = new ObjectMapper();
	// first, a dog, start with type
	Animal animal = mapper.readValue("{ \"type\":\"dog\", \"name\":\"Fido\", \"barkVolume\" : 95.0 }", Animal.class);
	assertEquals(Dog.class, animal.getClass());
	assertEquals("Fido", animal.name);
	assertEquals(95.0, ((Dog) animal).barkVolume);

	// Then cat; shuffle order to mandate buffering
	animal = mapper.readValue("{ \"likesCream\":true, \"name\" : \"Venla\", \"type\":\"cat\" }", Animal.class);
	assertEquals(Cat.class, animal.getClass());
	assertEquals("Venla", animal.name);
	// bah, of course cats like cream. But let's ensure Jackson won't mess with laws of nature!
	assertTrue(((Cat) animal).likesCream);


	// and finally, lactose-intolerant, but otherwise robust super-cat:
	animal = mapper.readValue("{ \"name\" : \"Macavity\", \"type\":\"cat\", \"lives\":18, \"likesCream\":false }", Animal.class);
	assertEquals(Cat.class, animal.getClass());
	assertEquals("Macavity", animal.name); // ... there's no one like Macavity!
	Cat cat = (Cat) animal;
	assertEquals(18, cat.lives);
	// ok, he can't drink dairy products. Let's verify:
	assertEquals(false, cat.likesCream);
    }
}
