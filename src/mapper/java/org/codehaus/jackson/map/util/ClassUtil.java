package org.codehaus.jackson.map.util;

import java.lang.reflect.*;
import java.util.*;

public final class ClassUtil
{
    /*
    /***************************************************
    /* Methods that deal with inheritance
    /***************************************************
     */

    /**
     * Method that will find all sub-classes and implemented interfaces
     * of a given class or interface. Classes are listed in order of
     * precedence, starting with the immediate super-class, followed by
     * interfaces class directly declares to implemented, and then recursively
     * followed by parent of super-class and so forth.
     * Note that <code>Object.class</code> is not included in the list
     * regardless of whether <code>endBefore</code> argument is defined
     *
     * @param endBefore Super-type to NOT include in results, if any; when
     *    encountered, will be ignored (and no super types are checked).
     */
    public static List<Class<?>> findSuperTypes(Class<?> cls, Class<?> endBefore)
    {
        /* We don't expect to get huge collections, thus overhead of a
         * Set seems unnecessary.
         */
        ArrayList<Class<?>> result = new ArrayList<Class<?>>();
        _addSuperTypes(cls, endBefore, result, false);
        return result;
    }

    private static void _addSuperTypes(Class<?> cls, Class<?> endBefore, ArrayList<Class<?>> result, boolean addClassItself)
    {
        if (cls == endBefore || cls == null || cls == Object.class) {
            return;
        }
        if (addClassItself) {
            if (result.contains(cls)) { // already added, no need to check supers
                return;
            }
            result.add(cls);
        }
        for (Class<?> intCls : cls.getInterfaces()) {
            _addSuperTypes(intCls, endBefore, result, true);
        }
        _addSuperTypes(cls.getSuperclass(), endBefore, result, true);
    }
    
    /*
    /***************************************************
    /* Class type detection methods
    /***************************************************
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
        /* As per [JACKSON-187], GAE seems to throw SecurityExceptions
         * here and there... and GAE itself has a bug, too
         * (see []). Bah.
         */
        try {
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
        }
        catch (SecurityException e) { }
        catch (NullPointerException e) { }
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

    /**
     * Helper method that checks if given class is a concrete one;
     * that is, not an interface or abstract class.
     */
    public static boolean isConcrete(Class<?> type)
    {
        int mod = type.getModifiers();
        return (mod & (Modifier.INTERFACE | Modifier.ABSTRACT)) == 0;
    }

    public static boolean isCollectionMapOrArray(Class<?> type)
    {
        if (type.isArray()) return true;
        if (Collection.class.isAssignableFrom(type)) return true;
        if (Map.class.isAssignableFrom(type)) return true;
        return false;
    }
    
    /*
    /***************************************************
    /* Method type detection methods
    /***************************************************
     */

    public static boolean hasGetterSignature(Method m)
    {
        // First: static methods can't be getters
        if (Modifier.isStatic(m.getModifiers())) {
            return false;
        }
        // Must take no args
        Class<?>[] pts = m.getParameterTypes();
        if (pts != null && pts.length != 0) {
            return false;
        }
        // Can't be a void method
        if (Void.TYPE == m.getReturnType()) {
            return false;
        }
        // Otherwise looks ok:
        return true;
    }

    /*
    /***************************************************
    /* Exception handling
    /***************************************************
     */

    /**
     * Method that can be used to find the "root cause", innermost
     * of chained (wrapped) exceptions.
     */
    public static Throwable getRootCause(Throwable t)
    {
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return t;
    }

    public static void throwAsIAE(Throwable t)
    {
        throwAsIAE(t, t.getMessage());
    }

    public static void throwAsIAE(Throwable t, String msg)
    {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new IllegalArgumentException(msg, t);
    }

    public static void unwrapAndThrowAsIAE(Throwable t)
    {
        throwAsIAE(getRootCause(t));
    }

    public static void unwrapAndThrowAsIAE(Throwable t, String msg)
    {
        throwAsIAE(getRootCause(t), msg);
    }

    /*
    /***************************************************
    /* Instantiation
    /***************************************************
     */

    /**
     * Method that can be called to try to create an instantiate of
     * specified type. Instantiation is done using default no-argument
     * constructor.
     *
     * @param canFixAccess Whether it is possible to try to change access
     *   rights of the default constructor (in case it is not publicly
     *   accessible) or not.
     *
     * @throws IllegalArgumentException If instantiation fails for any reason;
     *    except for cases where constructor throws an unchecked exception
     *    (which will be passed as is)
     */
    public static <T> T createInstance(Class<T> cls, boolean canFixAccess)
        throws IllegalArgumentException
    {
        Constructor<T> ctor = findConstructor(cls, canFixAccess);
        if (ctor == null) {
            throw new IllegalArgumentException("Class "+cls.getName()+" has no default (no arg) constructor");
        }
        try {
            return ctor.newInstance();
        } catch (Exception e) {
            ClassUtil.unwrapAndThrowAsIAE(e, "Failed to instantiate class "+cls.getName()+", problem: "+e.getMessage());
            return null;
        }
    }

    public static <T> Constructor<T> findConstructor(Class<T> cls, boolean canFixAccess)
        throws IllegalArgumentException
    {
        try {
            Constructor<T> ctor = cls.getDeclaredConstructor();
            if (canFixAccess) {
                checkAndFixAccess(ctor);
            } else {
                // Has to be public...
                if (!Modifier.isPublic(ctor.getModifiers())) {
                    throw new IllegalArgumentException("Default constructor for "+cls.getName()+" is not accessible (non-public?): not allowed to try modify access via Reflection: can not instantiate type");
                }
            }
            return ctor;
        } catch (NoSuchMethodException e) {
            ;
        } catch (Exception e) {
            ClassUtil.unwrapAndThrowAsIAE(e, "Failed to find default constructor of class "+cls.getName()+", problem: "+e.getMessage());
        }
        return null;
    }

    /*
    /***************************************************
    /* Access checking/handling methods
    /***************************************************
     */

    /**
     * Method called to check if we can use the passed method or constructor
     * (wrt access restriction -- public methods can be called, others
     * usually not); and if not, if there is a work-around for
     * the problem.
     */
    public static void checkAndFixAccess(Member member)
    {
        // We know all members are also accessible objects...
        AccessibleObject ao = (AccessibleObject) member;

        /* 14-Jan-2009, tatu: It seems safe and potentially beneficial to
         *   always to make it accessible (latter because it will force
         *   skipping checks we have no use for...), so let's always call it.
         */
        //if (!ao.isAccessible()) {
        try {
            ao.setAccessible(true);
        } catch (SecurityException se) {
            /* 17-Apr-2009, tatu: Related to [JACKSON-101]: this can fail on
             *    platforms like EJB and Google App Engine); so let's
             *    only fail if we really needed it...
             */
            if (!ao.isAccessible()) {
                Class<?> declClass = member.getDeclaringClass();
                throw new IllegalArgumentException("Can not access "+member+" (from class "+declClass.getName()+"; failed to set access: "+se.getMessage());
            }
        }
        //}
    }

    /*
    /***************************************************
    /* Enum type detection
    /***************************************************
     */

    /**
     * Helper method that can be used to dynamically figure out
     * enumeration type of given {@link EnumSet}, without having
     * access to its declaration.
     * Code is needed to work around design flaw in JDK.
     * 
     * @since 1.5
     */
    public static Class<? extends Enum<?>> findEnumType(EnumSet<?> s)
	{
    	// First things first: if not empty, easy to determine
    	if (!s.isEmpty()) {
    		return findEnumType(s.iterator().next());
    	}
    	// Otherwise need to locate using an internal field
    	return EnumTypeLocator.instance.enumTypeFor(s);
    }

    /**
     * Helper method that can be used to dynamically figure out
     * enumeration type of given {@link EnumSet}, without having
     * access to its declaration.
     * Code is needed to work around design flaw in JDK.
     * 
     * @since 1.5
     */
    public static Class<? extends Enum<?>> findEnumType(EnumMap<?,?> m)
    {
    	if (!m.isEmpty()) {
    		return findEnumType(m.keySet().iterator().next());
    	}
    	// Otherwise need to locate using an internal field
    	return EnumTypeLocator.instance.enumTypeFor(m);
    }

    /**
     * Helper method that can be used to dynamically figure out formal
     * enumeration type (class) for given enumeration. This is either
     * class of enum instance (for "simple" enumerations), or its
     * superclass (for enums with instance fields or methods)
     */
    @SuppressWarnings("unchecked")
	public static Class<? extends Enum<?>> findEnumType(Enum<?> en)
    {
		// enums with "body" are sub-classes of the formal type
    	Class<?> ec = en.getClass();
		if (ec.getSuperclass() != Enum.class) {
			ec = ec.getSuperclass();
		}
		return (Class<? extends Enum<?>>) ec;
    }

    /**
     * Helper method that can be used to dynamically figure out formal
     * enumeration type (class) for given class of an enumeration value.
     * This is either class of enum instance (for "simple" enumerations),
     * or its superclass (for enums with instance fields or methods)
     */
    @SuppressWarnings("unchecked")
	public static Class<? extends Enum<?>> findEnumType(Class<?> cls)
    {
		// enums with "body" are sub-classes of the formal type
		if (cls.getSuperclass() != Enum.class) {
			cls = cls.getSuperclass();
		}
		return (Class<? extends Enum<?>>) cls;
    }
    
    /*
    /***************************************************
    /* Helper classes
    /***************************************************
     */

    /**
     * Inner class used to contain gory details of how we can determine
     * details of instances of common JDK types like {@link EnumMap}s.
     */
    private static class EnumTypeLocator
    {
    	final static EnumTypeLocator instance = new EnumTypeLocator();

    	private final Field enumSetTypeField;
    	private final Field enumMapTypeField;
    	
    	private EnumTypeLocator() {
			/* JDK uses following fields to store information about actual Enumeration
			 * type for EnumSets, EnumMaps...
			 */
			enumSetTypeField = locateField(EnumSet.class, "elementType", Class.class);
			enumMapTypeField = locateField(EnumMap.class, "elementType", Class.class);
    	}

    	@SuppressWarnings("unchecked")
		public Class<? extends Enum<?>> enumTypeFor(EnumSet<?> set)
		{
    		if (enumSetTypeField != null) {
    			return (Class<? extends Enum<?>>) get(set, enumSetTypeField);
    		}
    		throw new IllegalStateException("Can not figure out type for EnumSet (odd JDK platform?)");
		}

    	@SuppressWarnings("unchecked")
		public Class<? extends Enum<?>> enumTypeFor(EnumMap<?,?> set)
		{
    		if (enumMapTypeField != null) {
    			return (Class<? extends Enum<?>>) get(set, enumMapTypeField);
    		}
    		throw new IllegalStateException("Can not figure out type for EnumMap (odd JDK platform?)");
		}
    	
    	private Object get(Object instance, Field field)
    	{
    		try {
    			return field.get(instance);
    		} catch (Exception e) {
    			throw new IllegalArgumentException(e);
    		}
    	}
    	
    	private static Field locateField(Class<?> fromClass, String expectedName, Class<?> type)
    	{
    		Field found = null;
    		// First: let's see if we can find exact match:
    		Field[] fields = fromClass.getDeclaredFields();
    		for (Field f : fields) {
    			if (expectedName.equals(f.getName()) && f.getType() == type) {
    				found = f;
    				break;
    			}
    		}
    		// And if not, if there is just one field with the type, that field
    		if (found == null) {
	    		for (Field f : fields) {
	    			if (f.getType() == type) {
	    				// If more than one, can't choose
	    				if (found != null) return null;
	    				found = f;
	    			}
	    		}
    		}
    		if (found != null) { // it's non-public, need to force accessible
    			try {
    				found.setAccessible(true);
    			} catch (Throwable t) { }
    		}
    		return found;
    	}
    }
}
