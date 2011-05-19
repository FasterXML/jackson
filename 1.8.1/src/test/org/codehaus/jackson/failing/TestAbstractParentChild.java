package org.codehaus.jackson.failing;

import org.codehaus.jackson.annotate.JsonBackReference;
import org.codehaus.jackson.annotate.JsonManagedReference;
import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.annotate.JsonTypeName;
import org.codehaus.jackson.annotate.JsonTypeInfo.Id;
import org.codehaus.jackson.map.BaseMapTest;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * Currently (1.8) parent/child dependencies do not work in
 * combination with abstract types; they should, but fixing this
 * require major changes to handling of both features.
 */
public class TestAbstractParentChild extends BaseMapTest
{
    @JsonTypeInfo(use=Id.NAME)
    @JsonSubTypes({@JsonSubTypes.Type(ConcreteNode.class)})
    static abstract class AbstractNode
    {
        public String id;
        
        @JsonManagedReference public AbstractNode next;
        @JsonBackReference public AbstractNode prev;
    }

    @JsonTypeName("concrete")
    static class ConcreteNode extends AbstractNode {
        public ConcreteNode() { }
        public ConcreteNode(String id) { this.id = id; }
    }
    
    /* 22-Sep-2010, tatu: This is for [JACKSON-368]. Easy to reproduce the issue,
     *   but alas not nearly as easy to resolve. Problem is that AbstractDeserializer
     *   has little knowledge of actual type, and so linkage can not be made statically.
     */
    public void testAbstract() throws Exception
    {
        AbstractNode parent = new ConcreteNode("p");
        AbstractNode child = new ConcreteNode("c");
        parent.next = child;
        child.prev = parent;

        // serialization ought to be ok
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(parent);

        AbstractNode root = null;
        try {
            root = mapper.readValue(json, AbstractNode.class);
        } catch  (IllegalArgumentException e) {
            fail("Did not expected an exception; got: "+e.getMessage());
        }
        assertEquals(ConcreteNode.class, root.getClass());
        assertEquals("p", root.id);
        assertNull(root.prev);
        AbstractNode leaf = root.next;
        assertNotNull(leaf);
        assertEquals("c", leaf.id);
        assertSame(parent, leaf.prev);
    }
}
