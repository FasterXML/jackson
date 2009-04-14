package org.codehaus.jackson.impl;

import java.io.*;
import java.util.Arrays;

import org.codehaus.jackson.*;

public class DefaultPrettyPrinter
    implements PrettyPrinter
{
    // // // Config, indentation

    /**
     * By default, let's use only spaces to separate array values.
     */
    protected Indenter mArrayIndenter = new FixedSpaceIndenter();

    /**
     * By default, let's use linefeed-adding indenter for separate
     * object entries. We'll further configure indenter to use
     * system-specific linefeeds, and 2 spaces per level (as opposed to,
     * say, single tabs)
     */
    protected Indenter mObjectIndenter = new Lf2SpacesIndenter();

    // // // Config, other white space configuration

    /**
     * By default we will add spaces around colons used to
     * separate object fields and values.
     * If disabled, will not use spaces around colon.
     */
    protected boolean mSpacesInObjectEntries = true;

    // // // State:

    /**
     * Number of open levels of nesting. Used to determine amount of
     * indentation to use.
     */
    protected int mNesting = 0;

    /*
    ////////////////////////////////////////////////////////////
    // Life-cycle (construct, configure)
    ////////////////////////////////////////////////////////////
    */

    public DefaultPrettyPrinter() { }

    public void indentArraysWith(Indenter i)
    {
        mArrayIndenter = (i == null) ? new NopIndenter() : i;
    }

    public void indentObjectsWith(Indenter i)
    {
        mObjectIndenter = (i == null) ? new NopIndenter() : i;
    }

    public void spacesInObjectEntries(boolean b) { mSpacesInObjectEntries = b; }
    /*
    ////////////////////////////////////////////////////////////
    // PrettyPrinter impl
    ////////////////////////////////////////////////////////////
     */

    public void writeRootValueSeparator(JsonGenerator jg)
        throws IOException, JsonGenerationException
    {
        jg.writeRaw(' ');
    }

    public void writeStartObject(JsonGenerator jg)
        throws IOException, JsonGenerationException
    {
        jg.writeRaw('{');
        if (!mObjectIndenter.isInline()) {
            ++mNesting;
        }
    }

    public void beforeObjectEntries(JsonGenerator jg)
        throws IOException, JsonGenerationException
    {
        mObjectIndenter.writeIndentation(jg, mNesting);
    }

    /**
     * Method called after an object field has been output, but
     * before the value is output.
     *<p>
     * Default handling (without pretty-printing) will output a single
     * colon to separate the two. Pretty-printer is
     * to output a colon as well, but can surround that with other
     * (white-space) decoration.
     */
    public void writeObjectFieldValueSeparator(JsonGenerator jg)
        throws IOException, JsonGenerationException
    {
        if (mSpacesInObjectEntries) {
            jg.writeRaw(" : ");
        } else {
            jg.writeRaw(':');
        }
    }

    /**
     * Method called after an object entry (field:value) has been completely
     * output, and before another value is to be output.
     *<p>
     * Default handling (without pretty-printing) will output a single
     * comma to separate the two. Pretty-printer is
     * to output a comma as well, but can surround that with other
     * (white-space) decoration.
     */
    public void writeObjectEntrySeparator(JsonGenerator jg)
        throws IOException, JsonGenerationException
    {
        jg.writeRaw(',');
        mObjectIndenter.writeIndentation(jg, mNesting);
    }

    public void writeEndObject(JsonGenerator jg, int nrOfEntries)
        throws IOException, JsonGenerationException
    {
        if (!mObjectIndenter.isInline()) {
            --mNesting;
        }
        if (nrOfEntries > 0) {
            mObjectIndenter.writeIndentation(jg, mNesting);
        } else {
            jg.writeRaw(' ');
        }
        jg.writeRaw('}');
    }

    public void writeStartArray(JsonGenerator jg)
        throws IOException, JsonGenerationException
    {
        if (!mArrayIndenter.isInline()) {
            ++mNesting;
        }
        jg.writeRaw('[');
    }

    public void beforeArrayValues(JsonGenerator jg)
        throws IOException, JsonGenerationException
    {
        mArrayIndenter.writeIndentation(jg, mNesting);
    }

    /**
     * Method called after an array value has been completely
     * output, and before another value is to be output.
     *<p>
     * Default handling (without pretty-printing) will output a single
     * comma to separate the two. Pretty-printer is
     * to output a comma as well, but can surround that with other
     * (white-space) decoration.
     */
    public void writeArrayValueSeparator(JsonGenerator jg)
        throws IOException, JsonGenerationException
    {
        jg.writeRaw(',');
        mArrayIndenter.writeIndentation(jg, mNesting);
    }

    public void writeEndArray(JsonGenerator jg, int nrOfValues)
        throws IOException, JsonGenerationException
    {
        if (!mArrayIndenter.isInline()) {
            --mNesting;
        }
        if (nrOfValues > 0) {
            mArrayIndenter.writeIndentation(jg, mNesting);
        } else {
            jg.writeRaw(' ');
        }
        jg.writeRaw(']');
    }

    /*
    ////////////////////////////////////////////////////////////
    // Helper classes
    ////////////////////////////////////////////////////////////
     */

    /**
     * Dummy implementation that adds no indentation whatsoever
     */
    public static class NopIndenter
        implements Indenter
    {
        public NopIndenter() { }
        public void writeIndentation(JsonGenerator jg, int level) { }
        public boolean isInline() { return true; }
    }

    /**
     * This is a very simple indenter that only every adds a
     * single space for indentation. It is used as the default
     * indenter for array values.
     */
    public static class FixedSpaceIndenter
        implements Indenter
    {
        public FixedSpaceIndenter() { }

        public void writeIndentation(JsonGenerator jg, int level)
            throws IOException, JsonGenerationException
        {
            jg.writeRaw(' ');
        }

        public boolean isInline() { return true; }
    }

    /**
     * Default linefeed-based indenter uses system-specific linefeeds and
     * 2 spaces for indentation per level.
     */
    public static class Lf2SpacesIndenter
        implements Indenter
    {
        final static String SYSTEM_LINE_SEPARATOR;
        static {
            String lf = null;
            try {
                lf = System.getProperty("line.separator");
            } catch (Throwable t) { } // access exception?
            SYSTEM_LINE_SEPARATOR = (lf == null) ? "\n" : lf;
        }

        final static int SPACE_COUNT = 64;
        final static char[] SPACES = new char[SPACE_COUNT];
        static {
            Arrays.fill(SPACES, ' ');
        }

        public Lf2SpacesIndenter() { }

        public boolean isInline() { return false; }

        public void writeIndentation(JsonGenerator jg, int level)
            throws IOException, JsonGenerationException
        {
            jg.writeRaw(SYSTEM_LINE_SEPARATOR);
            level += level; // 2 spaces per level
            while (level > SPACE_COUNT) { // should never happen but...
                jg.writeRaw(SPACES, 0, SPACE_COUNT); 
                level -= SPACES.length;
            }
            jg.writeRaw(SPACES, 0, level);
        }
    }
}
