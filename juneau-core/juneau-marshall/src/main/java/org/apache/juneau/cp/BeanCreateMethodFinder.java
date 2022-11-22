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

import static org.apache.juneau.common.internal.ArgUtils.*;

import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.reflect.*;

/**
 * Utility class for creating beans through creator methods.
 *
 * <p>
 * Used for finding and invoking methods on an object that take in arbitrary parameters and returns bean instances.
 *
 * <p>
 * This class is instantiated through the following methods:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link BeanStore}
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link BeanStore#createMethodFinder(Class)}
 * 			<li class='jm'>{@link BeanStore#createMethodFinder(Class,Class)}
 * 			<li class='jm'>{@link BeanStore#createMethodFinder(Class,Object)}
 * 		</ul>
 * 	</li>
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// The bean we want to create.</jc>
 * 	<jk>public class</jk> A {}
 *
 * 	<jc>// The bean that has a creator method for the bean above.</jc>
 * 	<jk>public class</jk> B {
 *
 * 		<jc>// Creator method.</jc>
 * 		<jc>// Bean store must have a C bean and optionally a D bean.</jc>
 * 		<jk>public</jk> A createA(C <jv>c</jv>, Optional&lt;D&gt; <jv>d</jv>) {
 * 			<jk>return new</jk> A(<jv>c</jv>, <jv>d</jv>.orElse(<jk>null</jk>));
 * 		}
 * 	}
 *
 * 	<jc>// Instantiate the bean with the creator method.</jc>
 * 	B <mv>b</mv> = <jk>new</jk> B();
 *
 *  <jc>// Create a bean store with some mapped beans.</jc>
 * 	BeanStore <mv>beanStore</mv> = BeanStore.<jsm>create</jsm>().addBean(C.<jk>class</jk>, <jk>new</jk> C());
 *
 * 	<jc>// Instantiate the bean using the creator method.</jc>
 * 	A <mv>a</mv> = <mv>beanStore</mv>
 * 		.createMethodFinder(A.<jk>class</jk>, <mv>b</mv>)  <jc>// Looking for creator for A on b object.</jc>
 * 		.find(<js>"createA"</js>)                         <jc>// Look for method called "createA".</jc>
 * 		.thenFind(<js>"createA2"</js>)                    <jc>// Then look for method called "createA2".</jc>
 * 		.withDefault(()-&gt;<jk>new</jk> A())                        <jc>// Optionally supply a default value if method not found.</jc>
 * 		.run();                                  <jc>// Execute.</jc>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link BeanStore}
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
		this.beanType = assertArgNotNull("beanType", beanType);
		this.resource = assertArgNotNull("resource", resource);
		this.resourceClass = resource.getClass();
		this.beanStore = BeanStore.of(beanStore, resource);
	}

	BeanCreateMethodFinder(Class<T> beanType, Class<?> resourceClass, BeanStore beanStore) {
		this.beanType = assertArgNotNull("beanType", beanType);
		this.resource = null;
		this.resourceClass = assertArgNotNull("resourceClass", resourceClass);
		this.beanStore = BeanStore.of(beanStore);
	}

	/**
	 * Adds a bean to the lookup for parameters.
	 *
	 * @param <T2> The bean type.
	 * @param c The bean type.
	 * @param t The bean.
	 * @return This object.
	 */
	public <T2> BeanCreateMethodFinder<T> addBean(Class<T2> c, T2 t) {
		beanStore.addBean(c, t);
		return this;
	}

	/**
	 * Find the method matching the specified predicate.
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
	 * @param filter The predicate to apply.
	 * @return This object.
	 */
	public BeanCreateMethodFinder<T> find(Predicate<MethodInfo> filter) {
		if (method == null) {
			method = ClassInfo.of(resourceClass).getPublicMethod(
				x -> x.isNotDeprecated()
				&& x.hasReturnType(beanType)
				&& x.hasNoAnnotation(BeanIgnore.class)
				&& filter.test(x)
				&& beanStore.hasAllParams(x)
				&& (x.isStatic() || resource != null)
			);
			if (method != null)
				args = beanStore.getParams(method);
		}
		return this;
	}

	/**
	 * Shortcut for calling <c>find(<jv>x</jv> -&gt; <jv>x</jv>.hasName(<jv>methodName</jv>))</c>.
	 *
	 * @param methodName The method name to match.
	 * @return This object.
	 */
	public BeanCreateMethodFinder<T> find(String methodName) {
		return find(x -> x.hasName(methodName));
	}

	/**
	 * Identical to {@link #find(Predicate)} but named for fluent-style calls.
	 *
	 * @param filter The predicate to apply.
	 * @return This object.
	 */
	public BeanCreateMethodFinder<T> thenFind(Predicate<MethodInfo> filter) {
		return find(filter);
	}

	/**
	 * Identical to {@link #find(Predicate)} but named for fluent-style calls.
	 *
	 * @param methodName The method name to match.
	 * @return This object.
	 */
	public BeanCreateMethodFinder<T> thenFind(String methodName) {
		return find(methodName);
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
	public T run() throws ExecutableException {
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
		if (t != null)
			consumer.accept(t);
		return t;
	}
}
