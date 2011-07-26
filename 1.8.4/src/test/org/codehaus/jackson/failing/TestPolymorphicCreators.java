package org.codehaus.jackson.failing;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.*;

/**
 * Unit tests for verifying that it is possible to annotate
 * various kinds of things with {@link JsonCreator} annotation.
 */
public class TestPolymorphicCreators
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper beans
    /**********************************************************
     */

    abstract static class AbstractRoot
    {
        private final String opt;

        private AbstractRoot(String opt) {
            this.opt = opt;
        }

        @JsonCreator
        public static final AbstractRoot make(@JsonProperty("which") int which,
            @JsonProperty("opt") String opt) {
            if(1 == which) {
                return new One(opt);
            }
            throw new RuntimeException("cannot instantiate " + which);
        }

        abstract public int getWhich();

        public final String getOpt() {
                return opt;
        }
    }

    static final class One extends AbstractRoot {
            private One(String opt) {
                    super(opt);
            }

            @Override public int getWhich() {
                    return 1;
            }
    }
    
    /*
    **********************************************
    * Actual tests
    **********************************************
     */

    public void testManualPolymorphicWithNumbered() throws Exception
    {
         final ObjectMapper m = new ObjectMapper();
         final ObjectWriter w = m.typedWriter(AbstractRoot.class);
         final ObjectReader r = m.reader(AbstractRoot.class);

        AbstractRoot input = AbstractRoot.make(1, "oh hai!");
        String json = w.writeValueAsString(input);
        AbstractRoot result = r.readValue(json);
        assertNotNull(result);
        assertEquals("oh hai!", result.getOpt());
    }
}
