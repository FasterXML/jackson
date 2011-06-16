package org.codehaus.jackson.jaxb;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Map;

/**
 * @author Ryan Heaton
 */
public class ObjectContainingAMap {

  private Map<String, String> myMap;

  @XmlJavaTypeAdapter( MapAdapter.class )
  public Map<String, String> getMyMap() {
    return myMap;
  }

  public void setMyMap(Map<String, String> myMap) {
    this.myMap = myMap;
  }
}
