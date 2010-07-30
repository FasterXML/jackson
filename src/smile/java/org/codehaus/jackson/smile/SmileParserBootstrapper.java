package org.codehaus.jackson.smile;

import java.io.*;

import org.codehaus.jackson.JsonLocation;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.ObjectCodec;
import org.codehaus.jackson.io.IOContext;
import org.codehaus.jackson.sym.BytesToNameCanonicalizer;

/**
 * Simple bootstrapper version used with Smile format parser.
 */
public class SmileParserBootstrapper
{
    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    final IOContext _context;

    final InputStream _in;

    /*
    /**********************************************************
    /* Input buffering
    /**********************************************************
     */

    final byte[] _inputBuffer;

    private int _inputPtr;

    private int _inputEnd;

    /**
     * Flag that indicates whether buffer above is to be recycled
     * after being used or not.
     */
    private final boolean _bufferRecyclable;

    /*
    /**********************************************************
    /* Input location
    /**********************************************************
     */

    /**
     * Current number of input units (bytes or chars) that were processed in
     * previous blocks,
     * before contents of current input buffer.
     *<p>
     * Note: includes possible BOMs, if those were part of the input.
     */
    protected int _inputProcessed;

    /*
    /**********************************************************
    /* Data gathered
    /**********************************************************
     */

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    public SmileParserBootstrapper(IOContext ctxt, InputStream in)
    {
        _context = ctxt;
        _in = in;
        _inputBuffer = ctxt.allocReadIOBuffer();
        _inputEnd = _inputPtr = 0;
        _inputProcessed = 0;
        _bufferRecyclable = true;
    }

    public SmileParserBootstrapper(IOContext ctxt, byte[] inputBuffer, int inputStart, int inputLen)
    {
        _context = ctxt;
        _in = null;
        _inputBuffer = inputBuffer;
        _inputPtr = inputStart;
        _inputEnd = (inputStart + inputLen);
        // Need to offset this for correct location info
        _inputProcessed = -inputStart;
        _bufferRecyclable = false;
    }

    public SmileParser constructParser(int generalParserFeatures, int smileFeatures,
    		ObjectCodec codec, BytesToNameCanonicalizer rootByteSymbols)
        throws IOException, JsonParseException
    {
        boolean intern = JsonParser.Feature.INTERN_FIELD_NAMES.enabledIn(generalParserFeatures);
        BytesToNameCanonicalizer can = rootByteSymbols.makeChild(true, intern);
    	// We just need a single byte, really, to know if it starts with header
    	ensureLoaded(1);
        SmileParser p =  new SmileParser(_context, generalParserFeatures, smileFeatures,
        		codec, can, 
        		_in, _inputBuffer, _inputPtr, _inputEnd, _bufferRecyclable);
        boolean hadSig = false;
        if (_inputPtr < _inputEnd) { // only false for empty doc
            if (_inputBuffer[_inputPtr] == SmileConstants.HEADER_BYTE_1) {
                // need to ensure it gets properly handled so caller won't see the signature
                hadSig = p.handleSignature(true);
            }
    	}
    	if (!hadSig && (smileFeatures & SmileParser.Feature.REQUIRE_HEADER.getMask()) != 0) {
    	    // Ok, first, let's see if it looks like plain JSON...
    	    String msg;

    	    byte firstByte = (_inputPtr < _inputEnd) ? _inputBuffer[_inputPtr] : 0;
    	    if (firstByte == '{' || firstByte == '[') {
                msg = "Input does not start with Smile format header (first byte = 0x"
                    +Integer.toHexString(firstByte & 0xFF)+") -- rather, it starts with '"+((char) firstByte)
                    +"' (plain JSON input?) -- can not parse";
    	    } else {
                msg = "Input does not start with Smile format header (first byte = 0x"
                +Integer.toHexString(firstByte & 0xFF)+") and parser has REQUIRE_HEADER enabled: can not parse";
    	    }
    	    throw new JsonParseException(msg, JsonLocation.NA);
    	}
        return p;
    }

    /*
    /**********************************************************
    /* Internal methods, raw input access
    /**********************************************************
     */

    protected boolean ensureLoaded(int minimum)
        throws IOException
    {
        if (_in == null) { // block source; nothing more to load
            return false;
        }

        /* Let's assume here buffer has enough room -- this will always
         * be true for the limited used this method gets
         */
        int gotten = (_inputEnd - _inputPtr);
        while (gotten < minimum) {
            int count = _in.read(_inputBuffer, _inputEnd, _inputBuffer.length - _inputEnd);
            if (count < 1) {
                return false;
            }
            _inputEnd += count;
            gotten += count;
        }
        return true;
    }
}
