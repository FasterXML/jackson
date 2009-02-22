package org.codehaus.jackson.annotate;

/**
 * Marker class used with annotations to indicate "no class". This is
 * a silly but necessary work-around -- annotations can not take nulls
 * as either default or explicit values. Hence for class values we must
 * explicitly use a bogus placeholder to denote equivalent of
 * "no class" (for which 'null' is usually the natural choice).
 */
public final class NoClass
{
    private NoClass() { }
}

