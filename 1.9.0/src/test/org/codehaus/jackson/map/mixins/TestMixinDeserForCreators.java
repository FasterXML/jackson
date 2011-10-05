package org.codehaus.jackson.map.mixins;

import java.io.*;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.*;

public class TestMixinDeserForCreators
    extends BaseMapTest
{
    /*
    ///////////////////////////////////////////////////////////
    // Helper bean classes
    ///////////////////////////////////////////////////////////
     */

    static class BaseClass
    {
        protected String _a;

        public BaseClass(String a) {
            _a = a+"...";
        }

        private BaseClass(String value, boolean dummy) {
            _a = value;
        }

        public static BaseClass myFactory(String a) {
            return new BaseClass(a+"X", true);
        }
    }

    static class BaseClassWithPrivateCtor
    {
        protected String _a;
        private BaseClassWithPrivateCtor(String a) {
            _a = a+"...";
        }

    }

    /**
     * Mix-in class that will effectively suppresses String constructor,
     * and marks a non-auto-detectable static method as factory method
     * as a creator.
     *<p>
     * Note that method implementations are not used for anything; but
     * we have to a class: interface won't do, as they can't have
     * constructors or static methods.
     */
    static class MixIn
    {
        @JsonIgnore protected MixIn(String s) { }

        @JsonCreator
        static BaseClass myFactory(String a) { return null; }
    }

    static class MixInForPrivate
    {
        @JsonCreator MixInForPrivate(String s) { }
    }

    static class StringWrapper {
        String _value;
        private StringWrapper(String s, boolean foo) { _value = s; }

        @SuppressWarnings("unused")
		private static StringWrapper create(String str) {
            return new StringWrapper(str, false);
        }
    }

    abstract static class StringWrapperMixIn {
        @JsonCreator static StringWrapper create(String str) { return null; }
    }

    /*
    ///////////////////////////////////////////////////////////
    // Unit tests
    ///////////////////////////////////////////////////////////
     */

    public void testForConstructor() throws IOException
    {
        ObjectMapper m = new ObjectMapper();
        m.getDeserializationConfig().addMixInAnnotations(BaseClassWithPrivateCtor.class, MixInForPrivate.class);
        BaseClassWithPrivateCtor result = m.readValue("\"?\"", BaseClassWithPrivateCtor.class);
        assertEquals("?...", result._a);
    }

    public void testForFactoryAndCtor() throws IOException
    {
        ObjectMapper m = new ObjectMapper();
        BaseClass result;

        // First: test default behavior: should use constructor
        result = m.readValue("\"string\"", BaseClass.class);
        assertEquals("string...", result._a);

        // Then with simple mix-in: should change to use the factory method
        m = new ObjectMapper();
        m.getDeserializationConfig().addMixInAnnotations(BaseClass.class, MixIn.class);
        result = m.readValue("\"string\"", BaseClass.class);
        assertEquals("stringX", result._a);
    }

    public void testFactoryMixIn() throws IOException
    {
        ObjectMapper m = new ObjectMapper();
        m.getDeserializationConfig().addMixInAnnotations(StringWrapper.class, StringWrapperMixIn.class);
        StringWrapper result = m.readValue("\"a\"", StringWrapper.class);
        assertEquals("a", result._value);
    }
}
