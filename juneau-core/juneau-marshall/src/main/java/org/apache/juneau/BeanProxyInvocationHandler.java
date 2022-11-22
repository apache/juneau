// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.json.*;
import org.apache.juneau.reflect.*;

/**
 * Provides an {@link InvocationHandler} for creating beans from bean interfaces.
 *
 * <p>
 * If the {@code useInterfaceProxies} setting is enabled in {@link BeanContext}, this is the class that creates
 * instances of beans from interfaces.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 *
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
		this.beanProps = new HashMap<>();
	}

	/**
	 * Implemented to handle the method called.
	 */
	@Override /* InvocationHandler */
	public Object invoke(Object proxy, Method method, Object[] args) {
		MethodInfo mi = MethodInfo.of(method);
		if (mi.hasName("equals") && mi.hasParamTypes(java.lang.Object.class)) {
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
			BeanMap<Object> bean = this.meta.ctx.toBeanMap(arg);
			return this.beanProps.equals(bean);
		}

		if (mi.hasName("hashCode") && mi.hasNoParams())
			return Integer.valueOf(this.beanProps.hashCode());

		if (mi.hasName("toString") && mi.hasNoParams())
			return Json5Serializer.DEFAULT.toString(this.beanProps);

		String prop = this.meta.getterProps.get(method);
		if (prop != null)
			return this.beanProps.get(prop);

		prop = this.meta.setterProps.get(method);
		if (prop != null) {
			this.beanProps.put(prop, args[0]);
			return null;
		}

		throw new UnsupportedOperationException("Unsupported bean method.  method='"+method+"'");
	}
}
