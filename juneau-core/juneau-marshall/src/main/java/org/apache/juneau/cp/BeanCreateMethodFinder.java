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
package org.apache.juneau.cp;

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.reflect.ReflectFlags.*;

import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.reflect.*;

/**
 * Used for finding methods on an object that take in arbitrary parameters and returns bean instances.
 *
 * See {@link BeanStore#createMethodFinder(Class, Object)} for usage.
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@source}
 * </ul>
 *
 * @param <T> The bean type being created.
 */
public class BeanCreateMethodFinder<T> {

	private Class<T> beanType;
	private final Class<?> resourceClass;
	private final Object resource;
	private final BeanStore beanStore;

	private MethodInfo method;
	private Object[] args;

	private Supplier<T> def = ()->null;

	BeanCreateMethodFinder(Class<T> beanType, Object resource, BeanStore beanStore) {
		this.beanType = beanType;
		this.resource = resource;
		this.resourceClass = resource.getClass();
		this.beanStore = BeanStore.of(beanStore, resource);
	}

	BeanCreateMethodFinder(Class<T> beanType, Class<?> resourceClass, BeanStore beanStore) {
		this.beanType = beanType;
		this.resource = null;
		this.resourceClass = resourceClass;
		this.beanStore = BeanStore.of(beanStore);
	}

	/**
	 * Adds a bean to the lookup for parameters.
	 *
	 * @param c The bean type.
	 * @param t The bean.
	 * @return This object.
	 */
	public <T2> BeanCreateMethodFinder<T> addBean(Class<T2> c, T2 t) {
		beanStore.addBean(c, t);
		return this;
	}

	/**
	 * Find the method matching the specified name and optionally having the specified required parameters present.
	 *
	 * <p>
	 * In order for the method to be used, it must adhere to the following restrictions:
	 * <ul>
	 * 	<li>The method must be public.
	 * 	<li>The method can be static.
	 * 	<li>The method name must match exactly.
	 * 	<li>The method must not be deprecated or annotated with {@link BeanIgnore}.
	 * 	<li>The method must have all parameter types specified in <c>requiredParams</c>.
	 * 	<li>The bean store must contain beans for all parameter types.
	 * 	<li>The bean store may contain beans for all {@link Optional} parameter types.
	 * </ul>
	 *
	 * <p>
	 * This method can be called multiple times with different method names or required parameters until a match is found.
	 * <br>Once a method is found, subsequent calls to this method will be no-ops.
	 *
	 * See {@link BeanStore#createMethodFinder(Class, Object)} for usage.
	 *
	 * @param methodName The method name.
	 * @param requiredParams Optional required parameters.
	 * @return This object.
	 */
	public BeanCreateMethodFinder<T> find(String methodName, Class<?>...requiredParams) {
		if (method == null) {
			ClassInfo ci = ClassInfo.of(resourceClass);
			for (MethodInfo m : ci.getPublicMethods()) {
				if (m.isAll(NOT_DEPRECATED) && m.hasReturnType(beanType) && m.getSimpleName().equals(methodName) && (!m.hasAnnotation(BeanIgnore.class))) {
					List<ClassInfo> missing = beanStore.getMissingParamTypes(m.getParams());
					if (missing.isEmpty() && m.hasAllArgs(requiredParams) && (m.isNotStatic() || resource != null)) {
						this.method = m;
						this.args = beanStore.getParams(m.getParams());
					}
				}
			}
		}
		return this;
	}

	/**
	 * Identical to {@link #find(String, Class...)} but named for fluent-style calls.
	 *
	 * @param methodName The method name.
	 * @param requiredParams Optional required parameters.
	 * @return This object.
	 */
	public BeanCreateMethodFinder<T> thenFind(String methodName, Class<?>...requiredParams) {
		return find(methodName, requiredParams);
	}

	/**
	 * A default value to return if no matching methods were found.
	 *
	 * @param def The default value.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	public BeanCreateMethodFinder<T> withDefault(T def) {
		return withDefault(()->def);
	}

	/**
	 * A default value to return if no matching methods were found.
	 *
	 * @param def The default value.
	 * @return This object.
	 */
	public BeanCreateMethodFinder<T> withDefault(Supplier<T> def) {
		assertArgNotNull("def", def);
		this.def = def;
		return this;
	}

	/**
	 * Executes the matched method and returns the result.
	 *
	 * @return The object returned by the method invocation, or the default value if method was not found.
	 * @throws ExecutableException If method invocation threw an exception.
	 */
	@SuppressWarnings("unchecked")
	public T run() throws ExecutableException  {
		if (method != null)
			return (T)method.invoke(resource, args);
		return def.get();
	}

	/**
	 * Same as {@link #run()} but also executes a consumer if the returned value was not <jk>null</jk>.
	 *
	 * @param consumer The consumer of the response.
	 * @return The object returned by the method invocation, or the default value if method was not found, or {@link Optional#empty()}.
	 * @throws ExecutableException If method invocation threw an exception.
	 */
	public T run(Consumer<? super T> consumer) throws ExecutableException {
		T t = run();
		Optional.ofNullable(t).ifPresent(consumer);
		return t;
	}

}
