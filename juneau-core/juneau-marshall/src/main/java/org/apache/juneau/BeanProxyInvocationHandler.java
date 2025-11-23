/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau;

import static org.apache.juneau.common.reflect.ReflectionUtils.*;
import static org.apache.juneau.common.utils.ThrowableUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.json.*;

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
	private Map<String,Object> beanProps;		// The map of property names to bean property values.

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
	@Override /* Overridden from InvocationHandler */
	public Object invoke(Object proxy, Method method, Object[] args) {
		var mi = info(method);
		if (mi.hasName("equals") && mi.hasParameterTypes(java.lang.Object.class)) {
			Object arg = args[0];
			if (arg == null)
				return false;
			if (proxy == arg)
				return true;
			if (proxy.getClass() == arg.getClass()) {
				InvocationHandler ih = Proxy.getInvocationHandler(arg);
				if (ih instanceof BeanProxyInvocationHandler ih2) {
					return this.beanProps.equals(ih2.beanProps);
				}
			}
			BeanMap<Object> bean = this.meta.ctx.toBeanMap(arg);
			return this.beanProps.equals(bean);
		}

		if (mi.hasName("hashCode") && mi.getParameterCount() == 0)
			return Integer.valueOf(this.beanProps.hashCode());

		if (mi.hasName("toString") && mi.getParameterCount() == 0)
			return Json5Serializer.DEFAULT.toString(this.beanProps);

		String prop = this.meta.getterProps.get(method);
		if (nn(prop))
			return this.beanProps.get(prop);

		prop = this.meta.setterProps.get(method);
		if (nn(prop)) {
			this.beanProps.put(prop, args[0]);
			return null;
		}

		throw unsupportedOp("Unsupported bean method.  method=''{0}''", method);
	}
}