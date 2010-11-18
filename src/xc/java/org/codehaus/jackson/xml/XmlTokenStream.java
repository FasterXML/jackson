package org.codehaus.jackson.xml;

import java.io.IOException;
import javax.xml.stream.*;

import org.codehaus.jackson.JsonLocation;
import org.codehaus.stax2.XMLStreamLocation2;
import org.codehaus.stax2.XMLStreamReader2;
import org.codehaus.stax2.ri.Stax2ReaderAdapter;

/**
 * Simple helper class used on top of STAX {@link XMLStreamReader} to further
 * abstract out all irrelevant details, and to expose equivalent of flat token
 * stream with no "fluff" tokens (comments, processing instructions, mixed
 * content) all of which is just to simplify
 * actual higher-level conversion to JSON tokens
 * 
 * @since 1.7
 */
public class XmlTokenStream
{
    public final static int XML_START_ELEMENT = 1;
    public final static int XML_END_ELEMENT = 2;
    public final static int XML_ATTRIBUTE_NAME = 3;
    public final static int XML_ATTRIBUTE_VALUE = 4;
    public final static int XML_TEXT = 5;
    public final static int XML_END = 6;

    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    final protected XMLStreamReader2 _xmlReader;

    final protected Object _sourceReference;
    
    /*
    /**********************************************************
    /* Parsing state
    /**********************************************************
     */

    protected int _currentState;

    protected int _attributeCount;
    
    /**
     * Index of the next attribute of the current START_ELEMENT
     * to return (as field name and value pair), if any; -1
     * when no attributes to return
     */
    protected int _nextAttributeIndex = 0;

    protected String _localName;

    protected String _namespaceURI;

    protected String _textValue;
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    public XmlTokenStream(XMLStreamReader xmlReader, Object sourceRef)
    {
        _sourceReference = sourceRef;
        // Let's ensure we point to START_ELEMENT...
        if (xmlReader.getEventType() != XMLStreamConstants.START_ELEMENT) {
            throw new IllegalArgumentException("Invalid XMLStreamReader passed: should be pointing to START_ELEMENT ("
                    +XMLStreamConstants.START_ELEMENT+"), instead got "+xmlReader.getEventType());
        }
        _xmlReader = Stax2ReaderAdapter.wrapIfNecessary(xmlReader);
        _currentState = XML_START_ELEMENT;
        _localName = _xmlReader.getLocalName();
        _namespaceURI = _xmlReader.getNamespaceURI();
    }

    /*
    /**********************************************************
    /* Public API
    /**********************************************************
     */

    public int next() throws IOException 
    {
        try {
            return _next();
        } catch (XMLStreamException e) {
            StaxUtil.throwXmlAsIOException(e);
            return -1;
        }
    }

    public void skipEndElement() throws IOException
    {
        try {
            int type = _next();
            if (type != XML_END_ELEMENT) {
                throw new IOException("Expected END_ELEMENT, got event of type "+type);
            }
        } catch (XMLStreamException e) {
            StaxUtil.throwXmlAsIOException(e);
        }
    }

    public int getCurrentToken() { return _currentState; }
    
    public String getLocalName() { return _localName; }
    public String getNamespaceURI() { return _namespaceURI; }
    public boolean hasAttributes() {
        return (_currentState == XML_START_ELEMENT) && (_attributeCount > 0);
    }
    
    public String getText()
    {
        if (_currentState == XML_ATTRIBUTE_VALUE
                || _currentState == XML_TEXT) {
            return _textValue;
        }
        return null;
    }
    
    public void closeCompletely() throws IOException
    {
        try {
            _xmlReader.closeCompletely();
        } catch (XMLStreamException e) {
            StaxUtil.throwXmlAsIOException(e);
        }
    }

    public void close() throws IOException
    {
        try {
            _xmlReader.close();
        } catch (XMLStreamException e) {
            StaxUtil.throwXmlAsIOException(e);
        }
    }

    public JsonLocation getCurrentLocation() {
        return _extractLocation(_xmlReader.getLocationInfo().getCurrentLocation());
    }
    public JsonLocation getTokenLocation() {
        return _extractLocation(_xmlReader.getLocationInfo().getStartLocation());
    }

    /*
    /**********************************************************
    /* Internal methods, parsing
    /**********************************************************
     */

    private final int _next() throws XMLStreamException
    {
        switch (_currentState) {
        case XML_ATTRIBUTE_VALUE:
            ++_nextAttributeIndex;
            // fall through
        case XML_START_ELEMENT: // attributes to return?
            if (_nextAttributeIndex < _attributeCount) {
                _localName = _xmlReader.getAttributeLocalName(0);
                _namespaceURI = _xmlReader.getAttributeNamespace(0);
                _textValue = _xmlReader.getAttributeValue(0);
                return (_currentState = XML_ATTRIBUTE_NAME);
            }
            // otherwise need to find START/END_ELEMENT or text
            String text = _collectUntilTag();
            // If it's START_ELEMENT, ignore any text
            if (_xmlReader.getEventType() == XMLStreamReader.START_ELEMENT) {
                return _initStartElement();
            }
            // For END_ELEMENT we will return text, if any
            if (text != null) {
                _textValue = text;
                return (_currentState = XML_TEXT);
            }
            return (_currentState = XML_END_ELEMENT);
        case XML_ATTRIBUTE_NAME:
            // if we just returned name, will need to just send value next
            return (_currentState = XML_ATTRIBUTE_VALUE);
        case XML_TEXT:
            // text is always followed by END_ELEMENT
            return (_currentState = XML_END_ELEMENT);
        }

        // Ok: must be END_ELEMENT; see what tag we get (or end)
        switch (_skipUntilTag()) {
        case XMLStreamConstants.END_DOCUMENT:
            return (_currentState = XML_END);
        case XMLStreamConstants.END_ELEMENT:
            return (_currentState = XML_END_ELEMENT);
        }
        // START_ELEMENT...
        return _initStartElement();
    }
    
    private final String _collectUntilTag() throws XMLStreamException
    {
        String text = null;
        while (true) {
            switch (_xmlReader.next()) {
            case XMLStreamConstants.START_ELEMENT:
            case XMLStreamConstants.END_ELEMENT:
            case XMLStreamConstants.END_DOCUMENT:
                return text;
                // note: SPACE is ignorable (and seldom seen), not to be included
            case XMLStreamConstants.CHARACTERS:
            case XMLStreamConstants.CDATA:
                if (text == null) {
                    text = _xmlReader.getText();
                } else { // can be optimized in future, if need be:
                    text += _xmlReader.getText();
                }
                break;
            default:
                // any other type (proc instr, comment etc) is just ignored
            }
        }
    }

    private final int _skipUntilTag() throws XMLStreamException
    {
        while (true) {
            int type;
            switch (type = _xmlReader.next()) {
            case XMLStreamConstants.START_ELEMENT:
            case XMLStreamConstants.END_ELEMENT:
            case XMLStreamConstants.END_DOCUMENT:
                return type;
            default:
                // any other type (proc instr, comment etc) is just ignored
            }
        }
    }
    
    /*
    /**********************************************************
    /* Internal methods, other
    /**********************************************************
     */

    private final int _initStartElement() throws XMLStreamException
    {
        _nextAttributeIndex = 0;
        _attributeCount = _xmlReader.getAttributeCount();
        _localName = _xmlReader.getLocalName();
        _namespaceURI = _xmlReader.getNamespaceURI();
        _textValue = null;
        return (_currentState = XML_START_ELEMENT);
    }
    
    private JsonLocation _extractLocation(XMLStreamLocation2 location)
    {
        if (location == null) { // just for impls that might pass null...
            return new JsonLocation(_sourceReference, -1, -1, -1);
        }
        return new JsonLocation(_sourceReference,
                location.getCharacterOffset(),
                location.getLineNumber(),
                location.getColumnNumber());
    }
    
}
