/**
 * Package info can be used to add "package annotations", so here we are...
 */
@javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters({
  @javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter(
    type = javax.xml.namespace.QName.class,
    value = org.codehaus.jackson.jaxb.TestJaxbAnnotationIntrospector.QNameAdapter.class
  )
})
package org.codehaus.jackson.jaxb;

