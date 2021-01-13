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
 * See {@link BeanFactory#beanCreateMethodFinder(Class, Object)} for usage.
 *
 * @param <T> The bean type being created.
 */
public class BeanCreateMethodFinder<T> {

	private Class<T> beanType;
	private final Object resource;
	private final BeanFactory beanFactory;

	private MethodInfo method;
	private Object[] args;

	private Supplier<T> def = ()->null;

	BeanCreateMethodFinder(Class<T> beanType, Object resource, BeanFactory beanFactory) {
		this.beanType = beanType;
		this.resource = resource;
		this.beanFactory = beanFactory;
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
	 * 	<li>The bean factory must contain beans for all parameter types.
	 * 	<li>The bean factory may contain beans for all {@link Optional} parameter types.
	 * </ul>
	 *
	 * <p>
	 * This method can be called multiple times with different method names or required parameters until a match is found.
	 * <br>Once a method is found, subsequent calls to this method will be no-ops.
	 *
	 * See {@link BeanFactory#beanCreateMethodFinder(Class, Object)} for usage.
	 *
	 * @param methodName The method name.
	 * @param requiredParams Optional required parameters.
	 * @return This object (for method chaining).
	 */
	public BeanCreateMethodFinder<T> find(String methodName, Class<?>...requiredParams) {
		if (method == null) {
			ClassInfo ci = ClassInfo.of(resource);
			for (MethodInfo m : ci.getPublicMethods()) {
				if (m.isAll(NOT_DEPRECATED) && m.hasReturnType(beanType) && m.getSimpleName().equals(methodName) && (!m.hasAnnotation(BeanIgnore.class))) {
					List<ClassInfo> missing = beanFactory.getMissingParamTypes(m.getParamTypes());
					if (missing.isEmpty() && m.hasAllArgs(requiredParams)) {
						this.method = m;
						this.args = beanFactory.getParams(m.getParamTypes());
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
	 * @return This object (for method chaining).
	 */
	public BeanCreateMethodFinder<T> thenFind(String methodName, Class<?>...requiredParams) {
		return find(methodName, requiredParams);
	}

	/**
	 * A default value to return if no matching methods were found.
	 *
	 * @param def The default value.  Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public BeanCreateMethodFinder<T> withDefault(T def) {
		return withDefault(()->def);
	}

	/**
	 * A default value to return if no matching methods were found.
	 *
	 * @param def The default value.
	 * @return This object (for method chaining).
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
}
