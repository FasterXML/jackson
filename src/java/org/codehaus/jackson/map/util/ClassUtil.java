package org.codehaus.jackson.map.util;

import java.lang.reflect.*;

public final class ClassUtil
{
    private ClassUtil() { }

    /*
    //////////////////////////////////////////////////////////
    // Type detection methods
    //////////////////////////////////////////////////////////
     */

    /**
     * @return Null if class might be a bean; type String (that identifies
     *   why it's not a bean) if not
     */
    public static String canBeABeanType(Class<?> type)
    {
        // First: language constructs that ain't beans:
        if (type.isAnnotation()) {
            return "annotation";
        }
        if (type.isArray()) {
            return "array";
        }
        if (type.isEnum()) {
            return "enum";
        }
        if (type.isPrimitive()) {
            return "primitive";
        }

        // Anything else? Seems valid, then
        return null;
    }

    public static String isLocalType(Class<?> type)
    {
        // one more: method locals, anonymous, are not good:
        if (type.getEnclosingMethod() != null) {
            return "local/anonymous";
        }

        /* But how about non-static inner classes? Can't construct
         * easily (theoretically, we could try to check if parent
         * happens to be enclosing... but that gets convoluted)
         */
        if (type.getEnclosingClass() != null) {
            if (!Modifier.isStatic(type.getModifiers())) {
                return "non-static member class";
            }
        }
        return null;
    }

    /**
     * Helper method used to weed out dynamic Proxy types; types that do
     * not expose concrete method API that we could use to figure out
     * automatic Bean (property) based serialization.
     */
    public static boolean isProxyType(Class<?> type)
    {
        // Then: well-known proxy (etc) classes
        if (Proxy.isProxyClass(type)) {
            return true;
        }
        String name = type.getName();
        // Hibernate uses proxies heavily as well:
        if (name.startsWith("net.sf.cglib.proxy.")
            || name.startsWith("org.hibernate.proxy.")) {
            return true;
        }
        // Not one of known proxies, nope:
        return false;
    }

    /*
    //////////////////////////////////////////////////////////
    // Access checking/handling methods
    //////////////////////////////////////////////////////////
     */

    /**
     * Method called to check if we can use the passed method or constructor
     * (wrt access restriction -- public methods can be called, others
     * usually not); and if not, if there is a work-around for
     * the problem.
     */
    public static void checkAndFixAccess(AccessibleObject obj, Class<?> declClass)
    {
        /* 14-Jan-2009, tatu: It seems safe and potentially beneficial to
         *   always to make it accessible (latter because it will force
         *   skipping checks we have no use for...), so let's always call it.
         */
        //if (!obj.isAccessible()) {
            try {
                obj.setAccessible(true);
            } catch (SecurityException se) {
                throw new IllegalArgumentException("Can not access "+obj+" (from class "+declClass.getName()+"; failed to set access: "+se.getMessage());
            }
            //}
    }

    /*
    //////////////////////////////////////////////////////////
    // Property name manging (getFoo -> foo)
    //////////////////////////////////////////////////////////
     */

    /**
     * Method called to figure out name of the property, given 
     * corresponding suggested name based on a method or field name.
     *
     * @param basename Name of accessor/mutator method, not including prefix
     *  ("get"/"is"/"set")
     */
    public static String manglePropertyName(String basename)
    {
        int len = basename.length();

        // First things first: empty basename is no good
        if (len == 0) {
            return null;
        }
        // otherwise, lower case initial chars
        StringBuilder sb = null;
        for (int i = 0; i < len; ++i) {
            char upper = basename.charAt(i);
            char lower = Character.toLowerCase(upper);
            if (upper == lower) {
                break;
            }
            if (sb == null) {
                sb = new StringBuilder(basename);
            }
            sb.setCharAt(i, lower);
        }
        return (sb == null) ? basename : sb.toString();
    }
}

