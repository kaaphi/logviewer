package com.kaaphi.logviewer.ui.util;

public abstract class ConfigurationObject<T> {
  private T value;

  public ConfigurationObject(T defaultValue) {
    this.value = defaultValue;
  }

  public T get() {
    return value;
  }

  public void set(T value) {
    this.value = value;
  }

  public String getAsString() {
    return to(value);
  }

  public void setFromString(String value) {
    this.value = from(value);
  }

  public abstract T from(String value);
  public abstract String to(T value);

  public static ConfigurationObject<String> createStringObject(String val) {
    return new ConfigurationObject<String>(val) {
      @Override
      public String from(String value) {
        return value;
      }

      @Override
      public String to(String value) {
        return value;
      }
    };
  }

  public static ConfigurationObject<Integer> createIntObject(Integer val) {
    return new ConfigurationObject<Integer>(val) {
      @Override
      public Integer from(String value) {
        return new Integer(value);
      }

      @Override
      public String to(Integer value) {
        return value.toString();
      }
    };
  }
}
