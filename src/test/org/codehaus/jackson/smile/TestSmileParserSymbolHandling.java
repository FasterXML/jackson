package org.codehaus.jackson.smile;

import java.io.*;

import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.sym.BytesToNameCanonicalizer;

/**
 * Unit tests for verifying that symbol handling works as planned, including
 * efficient reuse of names encountered during parsing.
 */
public class TestSmileParserSymbolHandling
	extends SmileTestBase
{
	public void testSimple() throws IOException
    {
		final String STR1 = "a";
		
    	byte[] data = _smileDoc("{ "+quote(STR1)+":1, \"foobar\":2, \"longername\":3 }");
    	SmileFactory f = new SmileFactory();
    	SmileParser p = _smileParser(f, data);
    	final BytesToNameCanonicalizer symbols1 = p._symbols;
    	assertEquals(0, symbols1.size());

    	
    	
    	assertEquals(JsonToken.START_OBJECT, p.nextToken());
    	assertEquals(JsonToken.FIELD_NAME, p.nextToken());
    	// field names are interned:
    	assertSame(STR1, p.getCurrentName());
    	assertEquals(1, symbols1.size());
    	assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
    	assertEquals(JsonToken.FIELD_NAME, p.nextToken());
    	assertSame("foobar", p.getCurrentName());
    	assertEquals(2, symbols1.size());
    	assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
    	assertEquals(JsonToken.FIELD_NAME, p.nextToken());
    	assertSame("longername", p.getCurrentName());
    	assertEquals(3, symbols1.size());
    	assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
    	assertEquals(JsonToken.END_OBJECT, p.nextToken());
    	assertNull(p.nextToken());
    	assertEquals(3, symbols1.size());
        p.close();

        // but let's verify that symbol table gets reused properly
    	p = _smileParser(f, data);
    	BytesToNameCanonicalizer symbols2 = p._symbols;
    	// symbol tables are not reused, but contents are:
    	assertNotSame(symbols1, symbols2);
    	assertEquals(3, symbols2.size());

    	assertEquals(JsonToken.START_OBJECT, p.nextToken());
    	assertEquals(JsonToken.FIELD_NAME, p.nextToken());
    	// field names are interned:
    	assertSame(STR1, p.getCurrentName());
    	assertEquals(3, symbols2.size());
    	assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
    	assertEquals(JsonToken.FIELD_NAME, p.nextToken());
    	assertSame("foobar", p.getCurrentName());
    	assertEquals(3, symbols2.size());
    	assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
    	assertEquals(JsonToken.FIELD_NAME, p.nextToken());
    	assertSame("longername", p.getCurrentName());
    	assertEquals(3, symbols2.size());
    	assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
    	assertEquals(JsonToken.END_OBJECT, p.nextToken());
    	assertNull(p.nextToken());
    	assertEquals(3, symbols2.size());
        p.close();

        assertEquals(3, symbols2.size());
    	p.close();
    }
}
