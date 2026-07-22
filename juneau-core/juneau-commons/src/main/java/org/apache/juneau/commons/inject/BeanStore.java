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
package org.apache.juneau.commons.inject;

import static java.util.stream.Collectors.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.reflect.*;

/**
 * Spring-bean-like interface for looking up beans by type and name.
 *
 * <p>
 * This interface provides a minimal contract for bean lookup operations, similar to Spring's
 * <a class="doclink" href="https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/beans/factory/BeanFactory.html">BeanFactory</a> interface.
 *
 * <p>
 * Implementations of this interface can be backed by various sources:
 * <ul>
 * 	<li>Spring's <a class="doclink" href="https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/context/ApplicationContext.html">ApplicationContext</a>
 * 	<li>Custom in-memory stores
 * 	<li>Other dependency injection frameworks
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	BeanStore <jv>beanStore</jv> = ...;
 *
 * 	<jc>// Get bean by type</jc>
 * 	Optional&lt;MyService&gt; <jv>service</jv> = <jv>beanStore</jv>.getBean(MyService.<jk>class</jk>);
 *
 * 	<jc>// Get bean by name</jc>
 * 	Optional&lt;Object&gt; <jv>bean</jv> = <jv>beanStore</jv>.getBean(<js>"myBean"</js>);
 *
 * 	<jc>// Get bean by type and name</jc>
 * 	Optional&lt;MyService&gt; <jv>namedService</jv> = <jv>beanStore</jv>.getBean(MyService.<jk>class</jk>, <js>"primary"</js>);
 *
 * 	<jc>// Get all beans of a type</jc>
 * 	Map&lt;String, MyService&gt; <jv>allServices</jv> = <jv>beanStore</jv>.getBeansOfType(MyService.<jk>class</jk>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://docs.spring.io/spring-framework/reference/core/beans.html">Spring Framework - The IoC Container</a>
 * </ul>
 */
public interface BeanStore {

	/**
	 * Returns a bean of the specified type.
	 *
	 * <p>
	 * If multiple beans of the specified type exist, the behavior is implementation-dependent.
	 * Typically, implementations will return the primary bean or throw an exception.
	 *
	 * @param <T> The bean type.
	 * @param beanType The bean type.
	 * @return The bean, or {@link Optional#empty()} if no bean of the specified type exists.
	 */
	<T> Optional<T> getBean(Class<T> beanType);

	/**
	 * Returns a bean of the specified type and name.
	 *
	 * @param <T> The bean type.
	 * @param beanType The bean type.
	 * @param name The bean name.  Can be <jk>null</jk> for unnamed beans.
	 * @return The bean, or {@link Optional#empty()} if no bean of the specified type and name exists.
	 */
	<T> Optional<T> getBean(Class<T> beanType, String name);

	/**
	 * Returns all beans of the specified type, keyed by bean name.
	 *
	 * <p>
	 * The map keys are the bean names, and the values are the bean instances.
	 * If no beans of the specified type exist, returns an empty map.
	 *
	 * @param <T> The bean type.
	 * @param beanType The bean type.
	 * @return A map of bean names to bean instances.  Never <jk>null</jk>.
	 */
	<T> Map<String, T> getBeansOfType(Class<T> beanType);

	/**
	 * Returns <jk>true</jk> if this store contains at least one bean of the specified type.
	 *
	 * @param beanType The bean type.
	 * @return <jk>true</jk> if this store contains at least one bean of the specified type.
	 */
	boolean hasBean(Class<?> beanType);

	/**
	 * Returns <jk>true</jk> if this store contains a bean of the specified type and name.
	 *
	 * @param beanType The bean type.
	 * @param name The bean name.  Can be <jk>null</jk> for unnamed beans.
	 * @return <jk>true</jk> if this store contains a bean of the specified type and name.
	 */
	boolean hasBean(Class<?> beanType, String name);

	/**
	 * Convenience shortcut for instantiating a bean of the specified type using this store.
	 *
	 * <p>
	 * Equivalent to:
	 * <p class='bjava'>
	 * 	BeanInstantiator.<jsm>of</jsm>(<jv>type</jv>, <jk>this</jk>).run()
	 * </p>
	 *
	 * <p>
	 * Use this when you don't need any of {@link BeanInstantiator}'s configuration options
	 * (subtype, factory-method names, builder hooks, etc.).  For more control over instantiation,
	 * use {@link BeanInstantiator#of(Class, BeanStore)} directly.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Instantiate a bean using this store for parameter resolution</jc>
	 * 	MyService <jv>service</jv> = <jv>beanStore</jv>.instantiate(MyService.<jk>class</jk>);
	 * </p>
	 *
	 * @param <T> The bean type.
	 * @param type The bean type to instantiate.  Must not be <jk>null</jk>.
	 * @return The instantiated bean.
	 */
	default <T> T instantiate(Class<T> type) {
		return BeanInstantiator.of(type, this).run();
	}

	/**
	 * Returns the supplier for an unnamed bean of the specified type.
	 *
	 * <p>
	 * If no supplier is found in this store, searches the parent store recursively.
	 *
	 * @param <T> The bean type.
	 * @param beanType The bean type.
	 * @return The supplier, or {@link Optional#empty()} if no supplier of the specified type exists.
	 */
	<T> Optional<Supplier<T>> getBeanSupplier(Class<T> beanType);

	/**
	 * Returns the supplier for a named bean of the specified type.
	 *
	 * <p>
	 * If no supplier with the specified name is found in this store, searches the parent store recursively.
	 *
	 * @param <T> The bean type.
	 * @param beanType The bean type.
	 * @param name The bean name.  Can be <jk>null</jk> for unnamed beans.
	 * @return The supplier, or {@link Optional#empty()} if no supplier of the specified type and name exists.
	 */
	<T> Optional<Supplier<T>> getBeanSupplier(Class<T> beanType, String name);

	/**
	 * Finds and invokes a factory method that produces a bean of type <c>beanType</c>.
	 *
	 * <p>
	 * Scans all public methods of the resource class identified by <c>onClassOrObject</c>:
	 * <ul>
	 * 	<li>If <c>onClassOrObject</c> is a {@link Class}, only <jk>static</jk> methods are eligible and
	 * 		the instance parameter passed to the invoked method is <jk>null</jk>.
	 * 	<li>If <c>onClassOrObject</c> is any other object, both instance and static methods on its class are
	 * 		eligible, and the object itself is passed as the receiver.
	 * </ul>
	 *
	 * <p>
	 * The first method satisfying all of the following is invoked:
	 * <ol>
	 * 	<li>Not deprecated.
	 * 	<li>Return type is assignment-compatible with <c>beanType</c>.
	 * 	<li>Accepted by <c>filter</c> (if non-<jk>null</jk>; otherwise any method qualifies).
	 * 	<li>All parameters can be resolved from this store plus any <c>extraBeans</c>.
	 * </ol>
	 *
	 * <p>
	 * The default implementation returns {@link Optional#empty()}.
	 * Override in concrete stores (see {@link BasicBeanStore}) to enable factory-method scanning.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Filter only</jc>
	 * 	<jv>beanStore</jv>.createBeanFromMethod(CallLogger.<jk>class</jk>, <jv>resource</jv>,
	 * 		RestContext::isBeanMethod)
	 * 		.ifPresent(<jv>creator</jv>::impl);
	 *
	 * 	<jc>// Filter + extra bean not yet in the store</jc>
	 * 	<jv>beanStore</jv>.createBeanFromMethod(EncoderSet.<jk>class</jk>, <jv>resource</jv>,
	 * 		RestContext::isBeanMethod, <jv>builder</jv>)
	 * 		.ifPresent(<jv>x</jv> -&gt; <jv>builder</jv>.impl(<jv>x</jv>));
	 *
	 * 	<jc>// No filter, no extra beans</jc>
	 * 	<jv>beanStore</jv>.createBeanFromMethod(CallLogger.<jk>class</jk>, <jv>resource</jv>);
	 * </p>
	 *
	 * @param <T> The bean type.
	 * @param beanType The type of bean to create.  Must not be <jk>null</jk>.
	 * @param onClassOrObject The object instance or {@link Class} whose public methods are searched.
	 * 	Must not be <jk>null</jk>.
	 * @param filter Optional predicate restricting which methods are eligible.  Can be <jk>null</jk> (any method qualifies).
	 * @param extraBeans Optional bean instances visible to parameter resolution for this call only.
	 * 	These are <em>not</em> registered in the store.
	 * @return The created bean wrapped in an {@link Optional}, or {@link Optional#empty()} if no matching
	 * 	factory method was found.
	 * @throws BeanCreationException If a matching method was found but threw an exception during invocation.
	 * @see BeanInstantiator BeanInstantiator — for instantiating a bean from its own constructors, builders, or factory methods
	 */
	default <T> Optional<T> createBeanFromMethod(Class<T> beanType, Object onClassOrObject, Predicate<MethodInfo> filter, Object... extraBeans) {
		return oe();
	}

	/**
	 * Convenience overload of {@link #createBeanFromMethod(Class, Object, Predicate, Object...)} with no filter and no extra beans.
	 *
	 * @param <T> The bean type.
	 * @param beanType The type of bean to create.  Must not be <jk>null</jk>.
	 * @param onClassOrObject The object instance or {@link Class} whose public methods are searched.
	 * 	Must not be <jk>null</jk>.
	 * @return The created bean wrapped in an {@link Optional}, or {@link Optional#empty()} if no matching
	 * 	factory method was found.
	 * @throws BeanCreationException If a matching method was found but threw an exception during invocation.
	 */
	default <T> Optional<T> createBeanFromMethod(Class<T> beanType, Object onClassOrObject) {
		return createBeanFromMethod(beanType, onClassOrObject, null);
	}

	/**
	 * Returns the implementation class registered for the specified bean type, if any.
	 *
	 * <p>
	 * Type bindings let callers register a default implementation class to be instantiated when an
	 * actual bean of <c>beanType</c> is needed.  Bindings are inherited through the parent chain.
	 *
	 * <p>
	 * The default implementation returns {@link Optional#empty()} (no binding).  Override in concrete
	 * stores (see {@link BasicBeanStore}) to enable type binding.
	 *
	 * @param <T> The bean type.
	 * @param beanType The bean type whose implementation class is being looked up.
	 * @return The implementation class, or {@link Optional#empty()} if no binding exists.
	 */
	default <T> Optional<Class<? extends T>> getBeanType(Class<T> beanType) {
		return oe();
	}

	/**
	 * Returns <jk>true</jk> if this store can satisfy every parameter of the specified executable.
	 *
	 * <p>
	 * Skips:
	 * <ul>
	 * 	<li>The first parameter when it is an instance of <c>outer</c> (inner-class outer reference).
	 * 	<li>Parameters typed as {@link Optional} (always considered satisfied).
	 * </ul>
	 *
	 * @param executable The constructor or method to inspect.  Must not be <jk>null</jk>.
	 * @param outer The outer object to use for inner-class param resolution.  Can be <jk>null</jk> (the outer-instance skip rule is then never applied).
	 * @return <jk>true</jk> if every required parameter can be resolved from this store.
	 */
	default boolean hasAllParams(ExecutableInfo executable, Object outer) {
		for (var i = 0; i < executable.getParameterCount(); i++) {
			var pi = executable.getParameter(i);
			var pt = pi.getParameterType();
			if ((i == 0 && nn(outer) && pt.isInstance(outer)) || pt.is(Optional.class))
				continue;
			var beanQualifier = pi.getResolvedQualifier();
			var ptc = pt.inner();
			if ((beanQualifier == null && ! hasBean(ptc)) || (nn(beanQualifier) && ! hasBean(ptc, beanQualifier)))
				return false;
		}
		return true;
	}

	/**
	 * Resolves every parameter of the specified executable from this store.
	 *
	 * <p>
	 * For {@link Optional}-typed parameters the wrapped {@link Optional} is returned directly; for all
	 * others the bean instance (or <jk>null</jk> when missing) is returned.  See {@link #hasAllParams}
	 * for the full skip rules.
	 *
	 * @param executable The constructor or method to resolve parameters for.  Must not be <jk>null</jk>.
	 * @param outer The outer object to use for inner-class param resolution.  Can be <jk>null</jk> (the outer-instance skip rule is then never applied).
	 * @return An array of resolved parameter values, one per parameter of <c>executable</c>.
	 */
	default Object[] getParams(ExecutableInfo executable, Object outer) {
		var o = new Object[executable.getParameterCount()];
		for (var i = 0; i < executable.getParameterCount(); i++) {
			var pi = executable.getParameter(i);
			var pt = pi.getParameterType();
			if (i == 0 && nn(outer) && pt.isInstance(outer)) {
				o[i] = outer;
			} else {
				var beanQualifier = pi.getResolvedQualifier();
				var ptc = pt.unwrap(Optional.class).inner();
				var o2 = beanQualifier == null ? getBean(ptc) : getBean(ptc, beanQualifier);
				o[i] = pt.is(Optional.class) ? o2 : o2.orElse(null);
			}
		}
		return o;
	}

	/**
	 * Returns a comma-delimited list of parameter type names that cannot be resolved from this store.
	 *
	 * <p>
	 * Intended for diagnostic error messages — see {@link #hasAllParams} for the exact match rules.
	 * Skipped parameters (outer-instance match, {@link Optional}) never appear in the result.
	 *
	 * @param executable The constructor or method to inspect.  Must not be <jk>null</jk>.
	 * @param outer The outer object to use for inner-class param resolution.  Can be <jk>null</jk> (the outer-instance skip rule is then never applied).
	 * @return A comma-delimited list of missing parameter type names, or <jk>null</jk> when none are missing.
	 */
	default String getMissingParams(ExecutableInfo executable, Object outer) {
		var params = executable.getParameters();
		List<String> l = list();
		for (var i = 0; i < params.size(); i++) {
			var pi = params.get(i);
			var pt = pi.getParameterType();
			if ((i == 0 && nn(outer) && pt.isInstance(outer)) || pt.is(Optional.class))
				continue;
			var beanName = pi.getResolvedQualifier();
			var ptc = pt.inner();
			if (beanName == null && ! hasBean(ptc))
				l.add(pt.getNameSimple());
			if (nn(beanName) && ! hasBean(ptc, beanName))
				l.add(pt.getNameSimple() + '@' + beanName);
		}
		return l.isEmpty() ? null : l.stream().sorted().collect(joining(","));
	}
}

