package org.codehaus.jackson.map;

/**
 * Interface used to decide which properties are to be included for views,
 * when serializing or deserializing instances using a view.
 * 
 * @since 1.4
 */
public interface JsonViewResolver
{
    /**
     * Method called to figure out whether given property should be included in specified
     * view when processing instance of specified class (as per processing definition:
     * not necessarily actual runtime class instance, if static class information is to
     * be used).
     * 
     * @param propertyClass
     * @param propertyName
     * @param view
     * @return
     */
    public boolean includePropertyForView(Class<?> propertyClass, String propertyName, Class<?> view);
}
