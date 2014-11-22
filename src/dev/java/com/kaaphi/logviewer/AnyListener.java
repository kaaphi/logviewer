package com.kaaphi.logviewer;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

import org.apache.log4j.Logger;


public class AnyListener {
	private static final Logger log = Logger.getLogger(AnyListener.class);
	
	@SuppressWarnings("unchecked")
	public static <T> T create(Class<T> klass) {
		return (T) Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), new Class[] {klass}, new LoggingInvokationHandler());
	}
	
	private static class LoggingInvokationHandler implements InvocationHandler {
		public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable {
			log.debug(String.format("%s(%s)", method.getName(), Arrays.toString(args)));
			return null;
		}
	}
}
