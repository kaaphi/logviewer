package com.kaaphi.logviewer.ui.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

public class Configuration {
	private List<Field> fields;
	
	public List<Field> getFields() {
		if(fields == null) {
			fields = new LinkedList<Field>();
			
			for(Field field : getClass().getFields()) {
				if(Modifier.isPublic(field.getModifiers()) &&
						ConfigurationObject.class.isAssignableFrom(field.getType())) {
					fields.add(field);
				}
			}
		}
		
		return fields;
	}
	
	public Properties store() {
		Properties props = new Properties();
		try {
			for(Field field : getFields()) {
				ConfigurationObject<?> obj;
				obj = (ConfigurationObject<?>)field.get(this);
				props.setProperty(field.getName(), obj.getAsString());
			}
		} catch (Throwable th) {
			throw new Error(th);
		}

		return props;
	}
	
	public void load(Properties props) {
		try {
			for(Field field : getFields()) {
				ConfigurationObject<?> obj;
				obj = (ConfigurationObject<?>)field.get(this);
				
				String value = props.getProperty(field.getName());
				if(value != null) {
					obj.setFromString(value);
				}
			}
		} catch (Throwable th) {
			throw new Error(th);
		}
	}
}
