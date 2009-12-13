// no package, i.e. at root of sample/extra

import java.io.*;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.introspect.BasicBeanDescription;
import org.codehaus.jackson.map.ser.BeanSerializer;
import org.codehaus.jackson.map.ser.BeanPropertyWriter;
import org.codehaus.jackson.map.ser.CustomSerializerFactory;

/**
 * Example code to show how to make use of underlying mechanisms
 * used for implementing standard JsonView implementation, but
 * without using <code>\@JsonView</code> annotations.
 *<p>
 * In this case, we will just want to suppress output of one of
 * fields, and tranform value of another. Former could be done
 * using other (annotation-based) suppression methods.
 *<p>
 * Note that for the showed use case this is not the simplest
 * solution -- basic custom JsonSerializer implementation would be --
 * it is just to give an idea of kinds of things that can be
 * done, and one additional mechanism that allows only partially
 * custom handling. This is useful for more complex POJOs, where
 * most of default handling is acceptable.
 *
 * @since 1.4
 */
public class CustomSerializationView
{
    /*
    ***********************************************************
    * First helper classes we need
    ***********************************************************
     */

    /**
     * Simple value class handling of which we want to customize.
     */
    static class ViewBean
    {
        public String name;
        public String value;
        public String secret;

        public ViewBean(String name, String value, String secret) {
            this.name = name;
            this.value = value;
            this.secret = secret;
        }
    }

    /**
     * And then custom bean property writer that implements
     * custom serialization functionality for one of properties
     */
    static class UpperCasingWriter
        extends BeanPropertyWriter
    {
        final BeanPropertyWriter _writer;

        /**
         * @param Original unmodified bean property writer that
         *   we delegate some calls to
         */
        public UpperCasingWriter(BeanPropertyWriter w) {
            // use "copy constructor" to get defaults
            super(w);
            _writer = w;
        }

        public void serializeAsField(Object bean, JsonGenerator jgen, SerializerProvider prov)
            throws Exception
        {
            // We know the type (although interface can't expose it)
            String value = ((ViewBean) bean).name;
            // Convert nulls to "", otherwise upper case
            value = (value == null) ? "" : value.toUpperCase();
            jgen.writeStringField("name", value);
        }
    }

    /**
     * Custom bean factory that is needed to process bean property writers
     * to implement custom view handling
     */
    public static class CustomBeanFactory
        extends CustomSerializerFactory
                // (CSF extends BeanSerializerFactory)
    {
        public CustomBeanFactory() { }

        @Override
        protected BeanSerializer processViews(SerializationConfig config, BasicBeanDescription beanDesc,
                                              BeanSerializer ser, List<BeanPropertyWriter> props)
        {
            // Let's use default serializer modification as the baseline
            ser = super.processViews(config, beanDesc, ser, props);
            
            /* And only change handling of that one bean (more likely,
             * you would want to handle all classes in a package, or with
             * some name -- this would be less work than having separate
             * custom serializer for all classes)
             */
            if (beanDesc.classDescribed() == ViewBean.class) {
                BeanPropertyWriter[] writers = props.toArray(new BeanPropertyWriter[props.size()]);
                for (int i = 0; i < writers.length; ++i) {
                    String pname = writers[i].getName();
                    if ("secret".equals(pname)) {
                        // remove serializer, filters it out
                        writers[i] = null;
                    } else if ("name".equals(pname)) {
                        // This one we'll just upper case for fun
                        writers[i] = new UpperCasingWriter(writers[i]);
                    }
                }
                // Important: create new serializer with filtered writers!
                ser = ser.withFiltered(writers);
            }
            return ser;
        }
    }

    /*
    ***********************************************************
    * Then simple test code
    ***********************************************************
     */
        
    public static void main(String[] args) throws Exception
    {
        ViewBean bean = new ViewBean("mr bean", "goofy", "secret!");
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializerFactory(new CustomBeanFactory());

        // First, without enabling view handling:
        System.out.println("With default serializer: "+mapper.writeValueAsString(bean));

        // Then with customized view-handling
        StringWriter sw = new StringWriter();
        /* note: View class being passed is irrelevant since we customize handling)
         * Also: if we wanted use 'writeValueAsString', would have to
         * do 'mapper.getSerializationConfig().setSerializationView(...)'
         * separately.
         */
        mapper.writeValueUsingView(sw, bean, String.class);
        System.out.println("With custom serializer: "+sw.toString());
    }
}
