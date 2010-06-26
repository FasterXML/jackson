package org.codehaus.jackson.map.deser;

import org.codehaus.jackson.annotate.JsonBackReference;
import org.codehaus.jackson.annotate.JsonManagedReference;
import org.codehaus.jackson.map.*;

import org.codehaus.jackson.map.BaseMapTest;

public class TestParentChildReferences
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Test classes, enums
    /**********************************************************
     */

    static class TreeNode
    {
        public String name;
        
        // Reference back to parent; reference, ignored during ser,
        // re-constructed during deser
        @JsonBackReference
        public TreeNode parent;

        // Reference that is serialized normally during ser, back
        // reference within pointed-to instance assigned to point to
        // referring bean ("this")
        @JsonManagedReference
        public TreeNode child;

        public TreeNode() { this(null); }
        public TreeNode(String n) { name = n; }
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */
    
    public void testSimpleRefs() throws Exception
    {
        TreeNode root = new TreeNode("root");
        TreeNode child = new TreeNode("kid");
        ObjectMapper mapper = new ObjectMapper();
        root.child = child;
        child.parent = root;
        
        String json = mapper.writeValueAsString(root);
        
        TreeNode resultNode = mapper.readValue(json, TreeNode.class);
        assertEquals("root", resultNode.name);
        TreeNode resultChild = resultNode.child;
        assertNotNull(resultChild);
        assertEquals("kid", resultChild.name);
        assertSame(resultChild.parent, resultNode);
    }
}
