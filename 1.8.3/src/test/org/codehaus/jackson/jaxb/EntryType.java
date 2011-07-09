package org.codehaus.jackson.jaxb;

/**
 * @author Ryan Heaton
 */
public class EntryType<K, V> {

  private K key;
  private V value;

  public EntryType() {
  }

  public EntryType(K key, V value) {
    this.key = key;
    this.value = value;
  }

  public K getKey() {
    return key;
  }

  public void setKey(K key) {
    this.key = key;
  }

  public V getValue() {
    return value;
  }

  public void setValue(V value) {
    this.value = value;
  }
}
