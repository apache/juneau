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

import static org.apache.juneau.commons.reflect.ReflectionUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.json.*;

/**
 * Provides an {@link InvocationHandler} for creating dynamic proxy instances of bean interfaces.
 *
 * <p>
 * This class enables the creation of bean instances from interfaces without requiring concrete implementations.
 * When the {@code useInterfaceProxies} setting is enabled in {@link BeanContext}, this handler is used to create
 * proxy instances that implement bean interfaces.
 *
 * <p>
 * The handler stores bean property values in an internal map and intercepts method calls to:
 * <ul>
 * 	<li><b>Getter methods</b> - Returns values from the internal property map</li>
 * 	<li><b>Setter methods</b> - Stores values in the internal property map</li>
 * 	<li><b>{@code equals(Object)}</b> - Compares property maps, with special handling for other proxy instances</li>
 * 	<li><b>{@code hashCode()}</b> - Returns hash code based on the property map</li>
 * 	<li><b>{@code toString()}</b> - Serializes the property map to JSON</li>
 * </ul>
 *
 * <p>
 * When comparing two proxy instances using {@code equals()}, if both are created with {@code BeanProxyInvocationHandler},
 * the comparison is optimized by directly comparing their internal property maps rather than converting to {@link BeanMap}.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Define an interface</jc>
 * 	<jk>public interface</jk> Person {
 * 		<jk>String</jk> <jsm>getName</jsm>();
 * 		<jk>void</jk> <jsm>setName</jsm>(<jk>String</jk> name);
 * 		<jk>int</jk> <jsm>getAge</jsm>();
 * 		<jk>void</jk> <jsm>setAge</jsm>(<jk>int</jk> age);
 * 	}
 *
 * 	<jc>// Create a proxy instance</jc>
 * 	<jk>var</jk> bc = <jsm>BeanContext</jsm>.<jsm>create</jsm>().<jsm>useInterfaceProxies</jsm>().<jsm>build</jsm>();
 * 	<jk>var</jk> person = bc.<jsm>getClassMeta</jsm>(Person.<jk>class</jk>).<jsm>newInstance</jsm>();
 *
 * 	<jc>// Use it like a regular bean</jc>
 * 	person.<jsm>setName</jsm>(<js>"John"</js>);
 * 	person.<jsm>setAge</jsm>(25);
 * 	<jk>var</jk> name = person.<jsm>getName</jsm>(); <jc>// Returns "John"</jc>
 * </p>
 *
 * @param <T> The interface class type
 * @see BeanContext#isUseInterfaceProxies()
 * @see BeanMeta#getBeanProxyInvocationHandler()
 * @see Proxy#newProxyInstance(ClassLoader, Class[], InvocationHandler)
 */
public class BeanProxyInvocationHandler<T> implements InvocationHandler {

	private final BeanMeta<T> meta;						// The BeanMeta for this instance
	private Map<String,Object> beanProps;		// The map of property names to bean property values.

	/**
	 * Constructor.
	 *
	 * @param meta The bean metadata for the interface. Must not be <jk>null</jk>.
	 */
	public BeanProxyInvocationHandler(BeanMeta<T> meta) {
		this.meta = meta;
		this.beanProps = new HashMap<>();
	}

	/**
	 * Handles method invocations on the proxy instance.
	 *
	 * <p>
	 * This method intercepts all method calls on the proxy and routes them appropriately:
	 * <ul>
	 * 	<li>If the method is {@code equals(Object)}, compares property maps</li>
	 * 	<li>If the method is {@code hashCode()}, returns the hash code of the property map</li>
	 * 	<li>If the method is {@code toString()}, serializes the property map to JSON</li>
	 * 	<li>If the method is a getter (identified via {@link BeanMeta#getGetterProps()}), returns the property value</li>
	 * 	<li>If the method is a setter (identified via {@link BeanMeta#getSetterProps()}), stores the property value</li>
	 * 	<li>Otherwise, throws {@link UnsupportedOperationException}</li>
	 * </ul>
	 *
	 * <p>
	 * The {@code equals()} method has special optimization: when comparing two proxy instances created with this handler,
	 * it directly compares their internal property maps using {@link Proxy#getInvocationHandler(Object)} to access the
	 * other instance's handler.
	 *
	 * @param proxy The proxy instance on which the method was invoked
	 * @param method The method that was invoked
	 * @param args The arguments passed to the method, or <jk>null</jk> if no arguments
	 * @return The return value of the method invocation
	 * @throws UnsupportedOperationException If the method is not a supported bean method (getter, setter, equals, hashCode, or toString)
	 */
	@Override /* Overridden from InvocationHandler */
	public Object invoke(Object proxy, Method method, Object[] args) {
		var mi = info(method);
		if (mi.hasName("equals") && mi.hasParameterTypes(Object.class)) {
			var arg = args[0];
			if (arg == null)
				return false;
			if (proxy == arg)
				return true;
			if (eq(proxy.getClass(), arg.getClass())) {
				var ih = Proxy.getInvocationHandler(arg);
				if (ih instanceof BeanProxyInvocationHandler ih2) {
					return beanProps.equals(ih2.beanProps);
				}
			}
			return eq(beanProps, meta.getBeanContext().toBeanMap(arg));
		}

		if (mi.hasName("hashCode") && mi.getParameterCount() == 0)
			return Integer.valueOf(this.beanProps.hashCode());

		if (mi.hasName("toString") && mi.getParameterCount() == 0)
			return Json5Serializer.DEFAULT.toString(this.beanProps);

		var prop = meta.getGetterProps().get(method);
		if (nn(prop))
			return beanProps.get(prop);

		prop = meta.getSetterProps().get(method);
		if (nn(prop)) {
			beanProps.put(prop, args[0]);
			return null;
		}

		throw unsupportedOp("Unsupported bean method.  method=''{0}''", method);
	}
}