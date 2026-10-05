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
package org.apache.juneau.rest.server.arg;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.commons.httppart.*;
import org.apache.juneau.httppart.bean.*;

/**
 * Resolves and caches the public no-arg constructor and per-getter setter {@link Method}s for a concrete
 * (non-interface) {@link org.apache.juneau.http.Request @Request} bean class.
 *
 * <p>
 * {@link RequestBeanArg} resolves one of these once, when the {@code @RestOp} is registered (servlet init), instead of the historical per-request {@code Class.getMethod} lookup in
 * {@code RestRequest.newRequestBean}. A getter with no matching setter, or a bean with no public no-arg constructor,
 * now fails registration with an {@link IllegalArgumentException} naming the bean, the getter and the expected setter
 * &mdash; not only the requests that happen to supply that parameter.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// A concrete request bean: public no-arg constructor plus a setter for every annotated getter.</jc>
 * 	<ja>@Request</ja>
 * 	<jk>public class</jk> MyQuery {
 * 		<jk>private</jk> String <jf>foo</jf>;
 * 		<ja>@Query</ja> <jk>public</jk> String getFoo() { <jk>return</jk> <jf>foo</jf>; }
 * 		<jk>public void</jk> setFoo(String <jv>value</jv>) { <jf>foo</jf> = <jv>value</jv>; }
 * 	}
 *
 * 	RequestBeanMeta <jv>meta</jv> = RequestBeanMeta.<jsm>create</jsm>(MyQuery.<jk>class</jk>, <jv>annotations</jv>);
 * 	RequestBeanSetters <jv>setters</jv> = RequestBeanSetters.<jsm>resolve</jsm>(MyQuery.<jk>class</jk>, <jv>meta</jv>);
 * 	MyQuery <jv>bean</jv> = <jv>setters</jv>.newInstance();
 * 	<jv>setters</jv>.set(<jv>bean</jv>, <jv>meta</jv>.getProperty(<js>"getFoo"</js>).getGetter(), <js>"value"</js>);
 * </p>
 *
 * @since 10.0.0
 */
public final class RequestBeanSetters {

	private static final Map<HttpPartType,String> ANNOTATION_NAMES = Map.of(
		HttpPartType.HEADER, "@Header",
		HttpPartType.QUERY, "@Query",
		HttpPartType.FORMDATA, "@FormData",
		HttpPartType.PATH, "@Path",
		HttpPartType.BODY, "@Content"
	);

	/**
	 * Resolves the constructor and setters for a concrete {@link org.apache.juneau.http.Request @Request} bean class.
	 *
	 * @param beanClass The bean class. Must not be an interface.
	 * @param meta The metadata gathered from the class's annotated getters. Must not be <jk>null</jk>.
	 * @return A new {@link RequestBeanSetters}.
	 * @throws IllegalArgumentException If {@code beanClass} has no public no-arg constructor, or if any annotated
	 * 	getter has no matching setter.
	 */
	public static RequestBeanSetters resolve(Class<?> beanClass, RequestBeanMeta meta) {
		Constructor<?> ctor;
		try {
			ctor = beanClass.getConstructor();
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException(
				String.format("Request bean %s must have a public no-arg constructor.", beanClass.getName()));
		}
		var setters = new LinkedHashMap<String,Method>();
		for (var pm : meta.getProperties()) {
			var getter = pm.getGetter();
			var setterName = "set" + getter.getName().substring(getter.getName().startsWith("is") ? 2 : 3);
			try {
				setters.put(getter.getName(), beanClass.getMethod(setterName, getter.getReturnType()));
			} catch (NoSuchMethodException e) {
				throw new IllegalArgumentException(
					String.format("Request bean %s has %s getter %s() with no matching setter %s(%s).",
						beanClass.getName(), ANNOTATION_NAMES.getOrDefault(pm.getPartType(), "@" + pm.getPartType()), getter.getName(), setterName,
						getter.getReturnType().getSimpleName()));
			}
		}
		return new RequestBeanSetters(ctor, setters);
	}

	private final Constructor<?> ctor;
	private final Map<String,Method> setters;

	private RequestBeanSetters(Constructor<?> ctor, Map<String,Method> setters) {
		this.ctor = ctor;
		this.setters = setters;
	}

	/**
	 * Instantiates the bean via its public no-arg constructor.
	 *
	 * @param <T> The bean type.
	 * @return A new, empty bean instance.
	 * @throws ReflectiveOperationException If instantiation fails.
	 */
	@SuppressWarnings({
		"unchecked" // newInstance() casts the constructed bean to the caller's T.
	})
	public <T> T newInstance() throws ReflectiveOperationException {
		return (T)ctor.newInstance();
	}

	/**
	 * Invokes the setter matching the specified getter on the specified bean.
	 *
	 * @param bean The bean to populate. Must not be <jk>null</jk>.
	 * @param getter The getter identifying the property, as returned by {@link RequestBeanPropertyMeta#getGetter()}.
	 * @param value The value to set. Callers skip absent (<jk>null</jk>) values rather than passing them here.
	 * @throws ReflectiveOperationException If the setter invocation fails.
	 */
	public void set(Object bean, Method getter, Object value) throws ReflectiveOperationException {
		setters.get(getter.getName()).invoke(bean, value);
	}
}
