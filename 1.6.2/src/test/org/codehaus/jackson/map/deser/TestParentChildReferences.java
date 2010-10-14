package org.codehaus.jackson.map.deser;

import java.util.*;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.annotate.JsonTypeInfo.Id;
import org.codehaus.jackson.map.*;

import org.codehaus.jackson.map.BaseMapTest;

public class TestParentChildReferences
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Test classes
    /**********************************************************
     */

    /**
     * First, a simple 'tree': just parent/child linkage
     */
    static class SimpleTreeNode
    {
        public String name;
        
        // Reference back to parent; reference, ignored during ser,
        // re-constructed during deser
        @JsonBackReference
        public SimpleTreeNode parent;

        // Reference that is serialized normally during ser, back
        // reference within pointed-to instance assigned to point to
        // referring bean ("this")
        @JsonManagedReference
        public SimpleTreeNode child;

        public SimpleTreeNode() { this(null); }
        public SimpleTreeNode(String n) { name = n; }
    }

    /**
     * Then nodes with two separate linkages; parent/child
     * and prev/next-sibling
     */
    static class FullTreeNode
    {
        public String name;

        // parent-child links
        @JsonBackReference("parent")
        public FullTreeNode parent;
        @JsonManagedReference("parent")
        public FullTreeNode firstChild;

        // sibling-links
        @JsonManagedReference("sibling")
        public FullTreeNode next;
        @JsonBackReference("sibling")
        protected FullTreeNode prev;
        
        public FullTreeNode() { this(null); }
        public FullTreeNode(String name) {
            this.name = name;
        }
    }

    /**
     * Class for testing managed references via arrays
     */
    static class NodeArray
    {
        @JsonManagedReference("arr")
        public ArrayNode[] nodes;
    }

    static class ArrayNode
    {
        public String name;
        
        @JsonBackReference("arr")
        public NodeArray parent;

        public ArrayNode() { this(null); }
        public ArrayNode(String n) { name = n; }
    }
    
    /**
     * Class for testing managed references via Collections
     */
    static class NodeList
    {
        @JsonManagedReference
        public List<NodeForList> nodes;
    }

    static class NodeForList
    {
        public String name;
        
        @JsonBackReference
        public NodeList parent;

        public NodeForList() { this(null); }
        public NodeForList(String n) { name = n; }
    }
    
    static class NodeMap
    {
        @JsonManagedReference
        public Map<String,NodeForMap> nodes;
    }

    static class NodeForMap
    {
        public String name;
        
        @JsonBackReference
        public NodeMap parent;

        public NodeForMap() { this(null); }
        public NodeForMap(String n) { name = n; }
    }

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
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */
    
    public void testSimpleRefs() throws Exception
    {
        SimpleTreeNode root = new SimpleTreeNode("root");
        SimpleTreeNode child = new SimpleTreeNode("kid");
        ObjectMapper mapper = new ObjectMapper();
        root.child = child;
        child.parent = root;
        
        String json = mapper.writeValueAsString(root);
        
        SimpleTreeNode resultNode = mapper.readValue(json, SimpleTreeNode.class);
        assertEquals("root", resultNode.name);
        SimpleTreeNode resultChild = resultNode.child;
        assertNotNull(resultChild);
        assertEquals("kid", resultChild.name);
        assertSame(resultChild.parent, resultNode);
    }

    public void testFullRefs() throws Exception
    {
        FullTreeNode root = new FullTreeNode("root");
        FullTreeNode child1 = new FullTreeNode("kid1");
        FullTreeNode child2 = new FullTreeNode("kid2");
        ObjectMapper mapper = new ObjectMapper();
        root.firstChild = child1;
        child1.parent = root;
        child1.next = child2;
        child2.prev = child1;
        
        String json = mapper.writeValueAsString(root);
        
        FullTreeNode resultNode = mapper.readValue(json, FullTreeNode.class);
        assertEquals("root", resultNode.name);
        FullTreeNode resultChild = resultNode.firstChild;
        assertNotNull(resultChild);
        assertEquals("kid1", resultChild.name);
        assertSame(resultChild.parent, resultNode);

        // and then sibling linkage
        assertNull(resultChild.prev);
        FullTreeNode resultChild2 = resultChild.next;
        assertNotNull(resultChild2);
        assertEquals("kid2", resultChild2.name);
        assertSame(resultChild, resultChild2.prev);
        assertNull(resultChild2.next);
    }

    public void testArrayOfRefs() throws Exception
    {
        NodeArray root = new NodeArray();
        ArrayNode node1 = new ArrayNode("a");
        ArrayNode node2 = new ArrayNode("b");
        root.nodes = new ArrayNode[] { node1, node2 };
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(root);
        
        NodeArray result = mapper.readValue(json, NodeArray.class);
        ArrayNode[] kids = result.nodes;
        assertNotNull(kids);
        assertEquals(2, kids.length);
        assertEquals("a", kids[0].name);
        assertEquals("b", kids[1].name);
        assertSame(result, kids[0].parent);
        assertSame(result, kids[1].parent);
    }

    public void testListOfRefs() throws Exception
    {
        NodeList root = new NodeList();
        NodeForList node1 = new NodeForList("a");
        NodeForList node2 = new NodeForList("b");
        root.nodes = Arrays.asList(node1, node2);
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(root);
        
        NodeList result = mapper.readValue(json, NodeList.class);
        List<NodeForList> kids = result.nodes;
        assertNotNull(kids);
        assertEquals(2, kids.size());
        assertEquals("a", kids.get(0).name);
        assertEquals("b", kids.get(1).name);
        assertSame(result, kids.get(0).parent);
        assertSame(result, kids.get(1).parent);
    }

    public void testMapOfRefs() throws Exception
    {
        NodeMap root = new NodeMap();
        NodeForMap node1 = new NodeForMap("a");
        NodeForMap node2 = new NodeForMap("b");
        Map<String,NodeForMap> nodes = new HashMap<String, NodeForMap>();
        nodes.put("a1", node1);
        nodes.put("b2", node2);
        root.nodes = nodes;
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(root);
        
        NodeMap result = mapper.readValue(json, NodeMap.class);
        Map<String,NodeForMap> kids = result.nodes;
        assertNotNull(kids);
        assertEquals(2, kids.size());
        assertNotNull(kids.get("a1"));
        assertNotNull(kids.get("b2"));
        assertEquals("a", kids.get("a1").name);
        assertEquals("b", kids.get("b2").name);
        assertSame(result, kids.get("a1").parent);
        assertSame(result, kids.get("b2").parent);
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

        AbstractNode root = mapper.readValue(json, AbstractNode.class);
        assertEquals(ConcreteNode.class, root.getClass());
        assertEquals("p", root.id);
        assertNull(root.prev);
        AbstractNode leaf = root.next;
        assertNotNull(leaf);
        assertEquals("c", leaf.id);
        assertSame(parent, leaf.prev);
    }
}
