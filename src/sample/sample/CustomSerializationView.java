package sample;
// no package, i.e. at root of sample/extra

import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.introspect.BasicBeanDescription;
import org.codehaus.jackson.map.ser.BeanPropertyWriter;
import org.codehaus.jackson.map.ser.BeanSerializerBuilder;
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
         * @param w Original unmodified bean property writer that
         *   we delegate some calls to
         */
        public UpperCasingWriter(BeanPropertyWriter w) {
            // use "copy constructor" to get defaults
            super(w);
            _writer = w;
        }

        public UpperCasingWriter(BeanPropertyWriter w, JsonSerializer<Object> ser) {
            super(w, ser);
            _writer = w;
        }
        
        @Override
        public void serializeAsField(Object bean, JsonGenerator jgen, SerializerProvider prov)
            throws Exception
        {
            // We know the type (although interface can't expose it)
            String value = ((ViewBean) bean).name;
            // Convert nulls to "", otherwise upper case
            value = (value == null) ? "" : value.toUpperCase();
            jgen.writeStringField("name", value);
        }

        @Override
        public BeanPropertyWriter withSerializer(JsonSerializer<Object> ser)
        {
            return new UpperCasingWriter(_writer, ser);
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

        /**
         * Here we will modify serializer such that it has two modes:
         * default handling when no JsonView is enabled; and other (custom)
         * when viess are enabled. Note that we could also just have forced
         * serialization for all cases.
         */
        @Override
        protected void processViews(SerializationConfig config, BeanSerializerBuilder builder)
        {
            // Let's use default serializer modification as the baseline
            super.processViews(config, builder);
            
            /* And only change handling of that one bean (more likely,
             * you would want to handle all classes in a package, or with
             * some name -- this would be less work than having separate
             * custom serializer for all classes)
             */
            BasicBeanDescription beanDesc = builder.getBeanDescription();
            if (beanDesc.getBeanClass() == ViewBean.class) {
                List<BeanPropertyWriter> props = builder.getProperties();
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
                // Important: update builder with filtered property definitions
                builder.setFilteredProperties(writers);
            }
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
        /* note: View class being passed is irrelevant since we customize handling)
         * Also: setting view would not be necessary if we just completely
         * overrode handling (we didn't, mostly to show more flexible
         * approach: conceivably you could have another way for passing
         * your own view id system, using ThreadLocal or some other
         * configurable part of <code>SerializationConfig</code> or
         * <code>SerializerProvider</code>)
         */
        /* note: if we wanted use 'writeValueAsString', would have to call
         * 'mapper.getSerializationConfig().setSerializationView(...)' first
         */
        String json = mapper.writerWithView(String.class).writeValueAsString(bean);
        System.out.println("With custom serializer: "+json);
    }
}
