/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core;

import java.lang.reflect.*;
import java.util.*;

/**
 * Provides an {@link InvocationHandler} for creating beans from bean interfaces.
 * <p>
 * 	If the {@code useInterfaceProxies} setting is enabled in {@link BeanContext}, this
 * 	is the class that creates instances of beans from interfaces.
 *
 * @author Barry M. Caceres
 * @param <T> The interface class
 */
public class BeanProxyInvocationHandler<T> implements InvocationHandler {

	private final BeanMeta<T> meta;						// The BeanMeta for this instance
	private Map<String, Object> beanProps;		// The map of property names to bean property values.

	/**
	 * Constructs with the specified {@link BeanMeta}.
	 *
	 * @param meta The bean meta data.
	 */
	public BeanProxyInvocationHandler(BeanMeta<T> meta) {
		this.meta = meta;
		this.beanProps = new HashMap<String, Object>();
	}

	/**
	 * Implemented to handle the method called.
	 */
	@Override /* InvocationHandler */
	public Object invoke(Object proxy, Method method, Object[] args) {
		Class<?>[] paramTypes = method.getParameterTypes();
		if (method.getName().equals("equals") && (paramTypes.length == 1) && (paramTypes[0] == java.lang.Object.class)) {
			Object arg = args[0];
			if (arg == null)
				return false;
			if (proxy == arg)
				return true;
			if (proxy.getClass() == arg.getClass()) {
				InvocationHandler ih = Proxy.getInvocationHandler(arg);
				if (ih instanceof BeanProxyInvocationHandler) {
					return this.beanProps.equals(((BeanProxyInvocationHandler<?>)ih).beanProps);
				}
			}
			BeanMap<Object> bean = this.meta.ctx.forBean(arg);
			return this.beanProps.equals(bean);
		}

		if (method.getName().equals("hashCode") && (paramTypes.length == 0))
			return Integer.valueOf(this.beanProps.hashCode());

		if (method.getName().equals("toString") && (paramTypes.length == 0))
			return this.beanProps.toString();

		String prop = this.meta.getterProps.get(method);
		if (prop != null)
			return this.beanProps.get(prop);

		prop = this.meta.setterProps.get(method);
		if (prop != null) {
			this.beanProps.put(prop, args[0]);
			return null;
		}

		throw new UnsupportedOperationException("Unsupported bean method.  method=[ " + method + " ]");
	}
}
