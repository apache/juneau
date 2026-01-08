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
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.Objects;

import org.apache.juneau.commons.reflect.*;

/**
 * Utility class for dependency injection that resolves method, constructor, and field parameters from a bean store.
 *
 * <p>
 * This class provides functionality for dependency injection by analyzing executable parameters (constructors and methods)
 * and field types, then resolving their values from a {@link BeanStore}.  It supports both manual parameter resolution
 * and automatic field/method injection via annotations.
 *
 * <h5 class='section'>Supported Parameter Types:</h5>
 * <ul class='spaced-list'>
 * 	<li><b>Single beans</b> - Parameters of type <c>T</c> are resolved by looking up a bean of that type in the bean store.
 * 	<li><b>Optional beans</b> - Parameters of type <c>Optional&lt;T&gt;</c> are resolved to <c>Optional.empty()</c> if no bean is found.
 * 	<li><b>Arrays</b> - Parameters of type <c>T[]</c> are populated with all beans of type <c>T</c> from the bean store.
 * 	<li><b>Lists</b> - Parameters of type <c>List&lt;T&gt;</c> are populated with all beans of type <c>T</c> from the bean store.
 * 		<br><b>Note:</b> Only the <c>List</c> interface is supported, not concrete implementations like <c>ArrayList</c> or <c>LinkedList</c>.
 * 	<li><b>Sets</b> - Parameters of type <c>Set&lt;T&gt;</c> are populated with all beans of type <c>T</c> from the bean store.
 * 		<br><b>Note:</b> Only the <c>Set</c> interface is supported, not concrete implementations like <c>HashSet</c> or <c>LinkedHashSet</c>.
 * 	<li><b>Maps</b> - Parameters of type <c>Map&lt;String,T&gt;</c> are populated with all beans of type <c>T</c> keyed by bean name.
 * 		<br><b>Note:</b> Only the <c>Map</c> interface is supported, not concrete implementations like <c>HashMap</c> or <c>LinkedHashMap</c>.
 * </ul>
 *
 * <h5 class='section'>Bean Qualifiers:</h5>
 * <p>
 * Parameters and fields can be annotated with {@link org.apache.juneau.annotation.Named @Named} or {@code javax.inject.Qualifier @Qualifier}
 * to specify which named bean should be injected.  For collections, arrays, and maps, qualifiers are ignored and all beans
 * of the element type are included.
 *
 * <h5 class='section'>Field and Method Injection:</h5>
 * <p>
 * The {@link #injectBeans(Object, BeanStore)} method performs automatic injection into fields and methods annotated with
 * <c>@Inject</c> or <c>@Autowired</c> (matched by simple class name).  This provides Spring-like dependency injection
 * functionality.
 *
 * <h5 class='section'>Additional Bean Sources:</h5>
 * <p>
 * All parameter resolution methods support an optional <c>otherBeans</c> varargs parameter that provides
 * additional bean instances to use if not found in the bean store.  This is useful for passing in
 * context-specific beans or temporary instances that aren't stored in the main bean store.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Bean store with multiple services</jc>
 * 	BeanStore <jv>beanStore</jv> = BasicBeanStore2.<jsm>create</jsm>()
 * 		.addBean(MyService.<jk>class</jk>, <jk>new</jk> MyService1(), <js>"service1"</js>)
 * 		.addBean(MyService.<jk>class</jk>, <jk>new</jk> MyService2(), <js>"service2"</js>)
 * 		.build();
 *
 * 	<jc>// Constructor with various parameter types</jc>
 * 	<jk>public</jk> MyClass(
 * 		MyService <jv>primary</jv>,  <jc>// Gets unnamed bean or first found</jc>
 * 		<ja>@Named</ja>(<js>"service1"</js>) MyService <jv>named</jv>,  <jc>// Gets bean named "service1"</jc>
 * 		Optional&lt;MyService&gt; <jv>optional</jv>,  <jc>// Gets Optional.of(bean) or Optional.empty()</jc>
 * 		List&lt;MyService&gt; <jv>allServices</jv>,  <jc>// Gets all MyService beans</jc>
 * 		Map&lt;String,MyService&gt; <jv>servicesMap</jv>  <jc>// Gets all MyService beans keyed by name</jc>
 * 	) { ... }
 *
 * 	<jc>// Resolve parameters manually</jc>
 * 	ConstructorInfo <jv>constructor</jv> = ClassInfo.<jsm>of</jsm>(MyClass.<jk>class</jk>).getConstructor(...);
 * 	Object[] <jv>params</jv> = InjectUtils.<jsm>getParameters</jsm>(<jv>constructor</jv>, <jv>beanStore</jv>, <jk>null</jk>);
 * 	MyClass <jv>instance</jv> = <jv>constructor</jv>.newInstance(<jv>params</jv>);
 *
 * 	<jc>// Or use convenience method</jc>
 * 	MyClass <jv>instance</jv> = InjectUtils.<jsm>invoke</jsm>(<jv>constructor</jv>, <jv>beanStore</jv>, <jk>null</jk>);
 *
 * 	<jc>// Or use automatic injection</jc>
 * 	MyClass <jv>instance</jv> = <jk>new</jk> MyClass();
 * 	InjectUtils.<jsm>injectBeans</jsm>(<jv>instance</jv>, <jv>beanStore</jv>);
 *
 * 	<jc>// Or provide additional beans not in store</jc>
 * 	MyService <jv>tempService</jv> = <jk>new</jk> MyService();
 * 	MyClass <jv>instance2</jv> = InjectUtils.<jsm>invoke</jsm>(<jv>constructor</jv>, <jv>beanStore</jv>, <jk>null</jk>, <jv>tempService</jv>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link BeanStore}
 * 	<li class='jc'>{@link BasicBeanStore2}
 * </ul>
 */
public class InjectUtils {

	/**
	 * Returns a comma-delimited list of parameter types that are missing from the bean store.
	 *
	 * <p>
	 * Analyzes the parameters of the specified constructor and checks if all required beans are available
	 * in the bean store or in the <c>otherBeans</c> parameter.
	 *
	 * <p>
	 * The following parameter types are considered optional and are not checked:
	 * <ul>
	 * 	<li>Parameters of type <c>Optional&lt;T&gt;</c>
	 * 	<li>Parameters of type <c>T[]</c>, <c>List&lt;T&gt;</c>, <c>Set&lt;T&gt;</c>, or <c>Map&lt;String,T&gt;</c>
	 * 	<li>The first parameter if it matches the <c>enclosingInstance</c> object type (for non-static inner classes)
	 * </ul>
	 *
	 * <p>
	 * If a parameter has a {@link org.apache.juneau.annotation.Named @Named} or {@code @Qualifier} annotation,
	 * the method checks for a bean with that specific name in the bean store.  Otherwise, it checks for an unnamed bean
	 * of the parameter type in the bean store, and if not found, checks the <c>otherBeans</c> parameter.
	 *
	 * @param ci The constructor to analyze.
	 * @param beanStore The bean store to check for beans.
	 * @param enclosingInstance The outer class instance for non-static inner class constructors.
	 * 	If the first parameter type matches this object's type, it is used as the first parameter and not checked in the bean store.
	 * 	Can be <jk>null</jk> for regular classes or static inner classes.
	 * @param otherBeans Optional additional bean instances to check if not found in the bean store.
	 * 	These are checked after the bean store but before marking the parameter as missing.
	 * @return A comma-delimited, sorted list of missing parameter types (e.g., <js>"String,Integer"</js>),
	 * 	or <jk>null</jk> if all required parameters are available.
	 */
	public static String getMissingParameters(ConstructorInfo ci, BeanStore beanStore, Object enclosingInstance, Object...otherBeans) {
		// @formatter:off
		return nullIfEmpty(
			ci.getParameters()
				.stream()
				.map(x -> getMissingConstructorParameter(x, beanStore, enclosingInstance, otherBeans))
				.filter(Objects::nonNull)
				.sorted()
				.collect(joining(","))
		);
		// @formatter:on
	}

	/**
	 * Returns a comma-delimited list of parameter types that are missing from the bean store.
	 *
	 * <p>
	 * Analyzes the parameters of the specified method and checks if all required beans are available
	 * in the bean store or in the <c>otherBeans</c> parameter.
	 *
	 * <p>
	 * The following parameter types are considered optional and are not checked:
	 * <ul>
	 * 	<li>Parameters of type <c>Optional&lt;T&gt;</c>
	 * 	<li>Parameters of type <c>T[]</c>, <c>List&lt;T&gt;</c>, <c>Set&lt;T&gt;</c>, or <c>Map&lt;String,T&gt;</c>
	 * </ul>
	 *
	 * <p>
	 * If a parameter has a {@link org.apache.juneau.annotation.Named @Named} or {@code @Qualifier} annotation,
	 * the method checks for a bean with that specific name in the bean store.  Otherwise, it checks for an unnamed bean
	 * of the parameter type in the bean store, and if not found, checks the <c>otherBeans</c> parameter.
	 *
	 * @param mi The method to analyze.
	 * @param beanStore The bean store to check for beans.
	 * @param otherBeans Optional additional bean instances to check if not found in the bean store.
	 * 	These are checked after the bean store but before marking the parameter as missing.
	 * @return A comma-delimited, sorted list of missing parameter types (e.g., <js>"String,Integer"</js>),
	 * 	or <jk>null</jk> if all required parameters are available.
	 */
	public static String getMissingParameters(MethodInfo mi, BeanStore beanStore, Object...otherBeans) {
		// @formatter:off
		return nullIfEmpty(
			mi.getParameters()
				.stream()
				.map(x -> getMissingParameter(x, beanStore, otherBeans))
				.filter(Objects::nonNull)
				.sorted()
				.collect(joining(","))
		);
		// @formatter:on
	}

	/**
	 * Resolves and returns parameter values from the bean store for the specified constructor.
	 *
	 * <p>
	 * For each parameter in the constructor, this method:
	 * <ul>
	 * 	<li>If the first parameter type matches the <c>enclosingInstance</c> object type, uses the <c>enclosingInstance</c> object
	 * 		(for non-static inner class constructors).
	 * 	<li>If the parameter is a collection/array/map type, collects all beans of the element type.
	 * 	<li>Otherwise, looks up a single bean by type and optional qualifier name in the bean store.
	 * 	<li>If not found in the bean store, checks the <c>otherBeans</c> parameter.
	 * </ul>
	 *
	 * <h5 class='section'>Parameter Resolution:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>Single beans</b> - Resolved using {@link BeanStore#getBean(Class)} or {@link BeanStore#getBean(Class, String)}.
	 * 		If not found, checks <c>otherBeans</c> for a compatible instance.
	 * 		Throws {@link ExecutableException} if not found in either location.
	 * 	<li><b>Optional beans</b> - Wrapped in <c>Optional</c>, or <c>Optional.empty()</c> if not found.
	 * 		Never throws an exception.
	 * 	<li><b>Arrays</b> - All beans of the element type are collected into an array (may be empty).
	 * 		Never throws an exception.
	 * 	<li><b>Lists</b> - All beans of the element type are collected into a <c>List</c> (may be empty).
	 * 		Never throws an exception.
	 * 	<li><b>Sets</b> - All beans of the element type are collected into a <c>LinkedHashSet</c> (may be empty).
	 * 		Never throws an exception.
	 * 	<li><b>Maps</b> - All beans of the value type are collected into a <c>LinkedHashMap</c> keyed by bean name (may be empty).
	 * 		Never throws an exception.
	 * </ul>
	 *
	 * @param ci The constructor to get parameters for.
	 * @param beanStore The bean store to resolve beans from.
	 * @param enclosingInstance The outer class instance for non-static inner class constructors.
	 * 	If the first parameter type matches this object's type, it is used as the first parameter value.
	 * 	Can be <jk>null</jk> for regular classes or static inner classes.
	 * @param otherBeans Optional additional bean instances to use if not found in the bean store.
	 * 	These are checked after the bean store but before throwing an exception.
	 * @return An array of parameter values in the same order as the constructor parameters.
	 * @throws ExecutableException If a required parameter (non-Optional, non-collection) cannot be resolved
	 * 	from the bean store or <c>otherBeans</c>.
	 */
	public static Object[] getParameters(ConstructorInfo ci, BeanStore beanStore, Object enclosingInstance, Object...otherBeans) {
		return ci.getParameters().stream().map(x -> getConstructorParameter(x, beanStore, enclosingInstance, otherBeans)).toArray();
	}

	/**
	 * Resolves and returns parameter values from the bean store for the specified method.
	 *
	 * <p>
	 * For each parameter in the method, this method:
	 * <ul>
	 * 	<li>If the parameter is a collection/array/map type, collects all beans of the element type.
	 * 	<li>Otherwise, looks up a single bean by type and optional qualifier name in the bean store.
	 * 	<li>If not found in the bean store, checks the <c>otherBeans</c> parameter.
	 * </ul>
	 *
	 * <h5 class='section'>Parameter Resolution:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>Single beans</b> - Resolved using {@link BeanStore#getBean(Class)} or {@link BeanStore#getBean(Class, String)}.
	 * 		If not found, checks <c>otherBeans</c> for a compatible instance.
	 * 		Never throws an exception.
	 * 	<li><b>Optional beans</b> - Wrapped in <c>Optional</c>, or <c>Optional.empty()</c> if not found.
	 * 		Never throws an exception.
	 * 	<li><b>Arrays</b> - All beans of the element type are collected into an array (may be empty).
	 * 		Never throws an exception.
	 * 	<li><b>Lists</b> - All beans of the element type are collected into a <c>List</c> (may be empty).
	 * 		Never throws an exception.
	 * 	<li><b>Sets</b> - All beans of the element type are collected into a <c>LinkedHashSet</c> (may be empty).
	 * 		Never throws an exception.
	 * 	<li><b>Maps</b> - All beans of the value type are collected into a <c>LinkedHashMap</c> keyed by bean name (may be empty).
	 * 		Never throws an exception.
	 * </ul>
	 *
	 * @param mi The method to get parameters for.
	 * @param beanStore The bean store to resolve beans from.
	 * @param otherBeans Optional additional bean instances to use if not found in the bean store.
	 * 	These are checked after the bean store but before throwing an exception.
	 * @return An array of parameter values in the same order as the method parameters.
	 * @throws ExecutableException If a required parameter (non-Optional, non-collection) cannot be resolved
	 * 	from the bean store or <c>otherBeans</c>.
	 */
	public static Object[] getParameters(MethodInfo mi, BeanStore beanStore, Object...otherBeans) {
		return mi.getParameters().stream().map(x -> getParameter(x, beanStore, otherBeans)).toArray();
	}

	/**
	 * Returns <jk>true</jk> if the bean store has all required parameters for the specified constructor.
	 *
	 * <p>
	 * This method performs the same checks as {@link #getMissingParameters(ConstructorInfo, BeanStore, Object, Object...)} but
	 * returns a boolean instead of a list of missing types.
	 *
	 * <p>
	 * The following parameter types are considered optional and are not checked:
	 * <ul>
	 * 	<li>Parameters of type <c>Optional&lt;T&gt;</c>
	 * 	<li>Parameters of type <c>T[]</c>, <c>List&lt;T&gt;</c>, <c>Set&lt;T&gt;</c>, or <c>Map&lt;String,T&gt;</c>
	 * 	<li>The first parameter if it matches the <c>enclosingInstance</c> object type (for non-static inner classes)
	 * </ul>
	 *
	 * <p>
	 * If a parameter has a {@link org.apache.juneau.annotation.Named @Named} or {@code @Qualifier} annotation,
	 * the method checks for a bean with that specific name in the bean store.  Otherwise, it checks for an unnamed bean
	 * of the parameter type in the bean store, and if not found, checks the <c>otherBeans</c> parameter.
	 *
	 * @param ci The constructor to check.
	 * @param beanStore The bean store to check for beans.
	 * @param enclosingInstance The outer class instance for non-static inner class constructors.
	 * 	If the first parameter type matches this object's type, it is used as the first parameter and not checked in the bean store.
	 * 	Can be <jk>null</jk> for regular classes or static inner classes.
	 * @param otherBeans Optional additional bean instances to check if not found in the bean store.
	 * 	These are checked after the bean store but before marking the parameter as missing.
	 * @return <jk>true</jk> if all required parameters are available in the bean store or <c>otherBeans</c>, <jk>false</jk> otherwise.
	 */
	public static boolean hasAllParameters(ConstructorInfo ci, BeanStore beanStore, Object enclosingInstance, Object...otherBeans) {
		return ci.getParameters().stream().map(x -> hasConstructorParameter(x, beanStore, enclosingInstance, otherBeans)).allMatch(x -> x);
	}

	/**
	 * Returns <jk>true</jk> if the bean store has all required parameters for the specified method.
	 *
	 * <p>
	 * This method performs the same checks as {@link #getMissingParameters(MethodInfo, BeanStore, Object...)} but
	 * returns a boolean instead of a list of missing types.
	 *
	 * <p>
	 * The following parameter types are considered optional and are not checked:
	 * <ul>
	 * 	<li>Parameters of type <c>Optional&lt;T&gt;</c>
	 * 	<li>Parameters of type <c>T[]</c>, <c>List&lt;T&gt;</c>, <c>Set&lt;T&gt;</c>, or <c>Map&lt;String,T&gt;</c>
	 * </ul>
	 *
	 * <p>
	 * If a parameter has a {@link org.apache.juneau.annotation.Named @Named} or {@code @Qualifier} annotation,
	 * the method checks for a bean with that specific name in the bean store.  Otherwise, it checks for an unnamed bean
	 * of the parameter type in the bean store, and if not found, checks the <c>otherBeans</c> parameter.
	 *
	 * @param mi The method to check.
	 * @param beanStore The bean store to check for beans.
	 * @param otherBeans Optional additional bean instances to check if not found in the bean store.
	 * 	These are checked after the bean store but before marking the parameter as missing.
	 * @return <jk>true</jk> if all required parameters are available in the bean store or <c>otherBeans</c>, <jk>false</jk> otherwise.
	 */
	public static boolean hasAllParameters(MethodInfo mi, BeanStore beanStore, Object...otherBeans) {
		return mi.getParameters().stream().map(x -> hasParameter(x, beanStore, otherBeans)).allMatch(x -> x);
	}

	/**
	 * Injects beans from the bean store into the specified object's fields and methods.
	 *
	 * <p>
	 * This method performs field and method injection based on {@code @Inject} or {@code @Autowired} annotations.
	 *
	 * <h5 class='section'>Injectable Fields:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Must be annotated with {@code @Inject} or {@code @Autowired} (matched by simple class name)
	 * 	<li>Must not be final
	 * 	<li>May have any valid name
	 * 	<li>Supports the same parameter types as {@link #getParameters}: single beans, {@code Optional}, arrays, {@code List}, {@code Set}, {@code Map}
	 * </ul>
	 *
	 * <h5 class='section'>Injectable Methods:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Must be annotated with {@code @Inject} or {@code @Autowired} (matched by simple class name)
	 * 	<li>Must not be abstract
	 * 	<li>Must not declare type parameters
	 * 	<li>May return any type (return value is ignored)
	 * 	<li>May have any valid name
	 * 	<li>May accept zero or more dependencies as arguments
	 * 	<li>Supports the same parameter types as {@link #getParameters}: single beans, {@code Optional}, arrays, {@code List}, {@code Set}, {@code Map}
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Class with injectable fields and methods</jc>
	 * 	<jk>public</jk> <jk>class</jk> MyService {
	 * 		<ja>@Inject</ja>
	 * 		<jk>private</jk> MyDependency <jv>dependency</jv>;
	 *
	 * 		<ja>@Autowired</ja>
	 * 		<jk>private</jk> List&lt;MyService&gt; <jv>allServices</jv>;
	 *
	 * 		<ja>@Inject</ja>
	 * 		<jk>public</jk> <jk>void</jk> init(MyConfig <jv>config</jv>) {
	 * 			<jc>// Initialization logic</jc>
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Inject beans</jc>
	 * 	MyService <jv>service</jv> = <jk>new</jk> MyService();
	 * 	InjectUtils.<jsm>injectBeans</jsm>(<jv>service</jv>, <jv>beanStore</jv>);
	 * </p>
	 *
	 * @param <T> The bean type.
	 * @param bean The object to inject beans into.
	 * @param beanStore The bean store to resolve beans from.
	 * @return The same bean instance (for method chaining).
	 * @throws ExecutableException If any field or method cannot be injected (e.g., required bean not found in the bean store).
	 */
	public static <T> T injectBeans(T bean, BeanStore beanStore) {
		var type = ClassInfo.of(bean);

		// Inject into fields
		type.getAllFields().stream()
			.filter(x -> x.isNotFinal() && x.getAnnotations().stream().map(AnnotationInfo::getName).anyMatch(n -> eq(n, "Inject") || eq(n, "Autowired")))
			.forEach(x -> invoke(x, beanStore, bean));

		// Inject into methods
		type.getAllMethods().stream()
			.filter(x -> x.isNotAbstract() && x.getTypeParameters().length == 0 && x.getAnnotations().stream().map(AnnotationInfo::getName).anyMatch(n -> eq(n, "Inject") || eq(n, "Autowired")))
			.forEach(x -> invoke(x, beanStore, bean));

		return bean;
	}

	/**
	 * Resolves parameters from the bean store and invokes the specified constructor.
	 *
	 * <p>
	 * This is a convenience method that combines {@link #getParameters(ConstructorInfo, BeanStore, Object, Object...)} with
	 * {@link ConstructorInfo#newInstance(Object...)}.
	 *
	 * <p>
	 * The <c>enclosingInstance</c> parameter is used for non-static inner class constructors.  If the first parameter type
	 * matches this object's type, it is used as the first parameter value.  For regular constructors, this parameter
	 * is ignored if it doesn't match the first parameter type.
	 *
	 * @param <T> The return type of the constructor.
	 * @param constructor The constructor to invoke.
	 * @param beanStore The bean store to resolve parameters from.
	 * @param enclosingInstance The outer class instance for non-static inner classes (can be <jk>null</jk>).
	 * @param otherBeans Optional additional bean instances to use if not found in the bean store.
	 * @return The result of invoking the constructor.
	 * @throws ExecutableException If the constructor cannot be invoked or parameter resolution fails.
	 */
	public static <T> T invoke(ConstructorInfo constructor, BeanStore beanStore, Object enclosingInstance, Object...otherBeans) {
		var params = getParameters(constructor, beanStore, enclosingInstance, otherBeans);
		return constructor.accessible().newInstance(params);
	}

	/**
	 * Resolves the field value from the bean store and sets it on the specified object.
	 *
	 * <p>
	 * This method resolves the field value using the same logic as {@link #getParameters(MethodInfo, BeanStore, Object...)},
	 * supporting single beans, {@code Optional}, arrays, {@code List}, {@code Set}, and {@code Map}.
	 *
	 * <p>
	 * The field is made accessible before setting its value.
	 *
	 * @param <T> The bean type.
	 * @param field The field to inject into.
	 * @param beanStore The bean store to resolve the field value from.
	 * @param bean The object instance containing the field.
	 * @return The same bean instance (for method chaining).
	 */
	public static <T> T invoke(FieldInfo field, BeanStore beanStore, T bean) {
		field.accessible();
		field.set(bean, getFieldValue(field, beanStore));
		return bean;
	}

	/**
	 * Resolves parameters from the bean store and invokes the specified method.
	 *
	 * <p>
	 * This is a convenience method that combines {@link #getParameters(MethodInfo, BeanStore, Object...)} with
	 * {@link MethodInfo#invoke(Object, Object...)}.
	 *
	 * <p>
	 * The <c>bean</c> parameter is the object instance on which to invoke the method (or <jk>null</jk> for static methods).
	 *
	 * @param <T> The return type of the method.
	 * @param method The method to invoke.
	 * @param beanStore The bean store to resolve parameters from.
	 * @param bean The object instance on which to invoke the method (or <jk>null</jk> for static methods).
	 * @param otherBeans Optional additional bean instances to use if not found in the bean store.
	 * @return The result of invoking the method.
	 * @throws ExecutableException If the method cannot be invoked or parameter resolution fails.
	 */
	public static <T> T invoke(MethodInfo method, BeanStore beanStore, Object bean, Object...otherBeans) {
		var params = getParameters(method, beanStore, otherBeans);
		return method.accessible().invoke(bean, params);
	}

	/**
	 * Resolves and creates a collection, array, or map containing all beans of the element type from the bean store.
	 *
	 * <p>
	 * Retrieves all beans of the element type using {@link BeanStore#getBeansOfType(Class)} and converts them
	 * to the appropriate collection type:
	 * <ul>
	 * 	<li>Arrays - Creates an array of the element type
	 * 	<li><c>List&lt;T&gt;</c> - Creates an <c>ArrayList</c> (only the <c>List</c> interface is supported, not concrete implementations)
	 * 	<li><c>Set&lt;T&gt;</c> - Creates a <c>LinkedHashSet</c> (preserves insertion order; only the <c>Set</c> interface is supported)
	 * 	<li><c>Map&lt;String,T&gt;</c> - Creates a <c>LinkedHashMap</c> keyed by bean name (only the <c>Map</c> interface is supported)
	 * </ul>
	 *
	 * <p>
	 * The <c>beanQualifier</c> parameter is ignored for collections, as all beans of the element type are included.
	 *
	 * @param pi The parameter info containing generic type information (can be <jk>null</jk> for fields).
	 * @param pt The type (unwrapped from <c>Optional</c>). For fields, this is already unwrapped.
	 * @param beanStore The bean store to retrieve beans from.
	 * @param beanQualifier The bean qualifier (ignored for collections/arrays/maps).
	 * @return The collection, array, or map containing all beans of the element type,
	 * 	or <jk>null</jk> if the type is not a supported collection type.
	 */
	private static Object getCollectionValue(ParameterInfo pi, ClassInfo pt, BeanStore beanStore, String beanQualifier) {
		if (! isCollectionType(pt))
			return null;

		var elementType = getElementType(pi, pt);
		if (elementType == null)
			return null;

		// Get all beans of the element type
		var beans = beanStore.getBeansOfType(elementType);

		// Handle Map<String,T> - getBeansOfType already returns Map<String,T>
		var inner = opt(pt.inner()).orElse(Object.class);
		if (Map.class.isAssignableFrom(inner)) {
			// Verify it's Map<String,T> and return the map directly
			Type parameterizedType;
			if (pi != null) {
				// For parameters, use ParameterInfo.getParameterizedType() and handle Optional unwrapping
				parameterizedType = pi.getParameterizedType();
				if (parameterizedType instanceof ParameterizedType pt2) {
					var rawType = pt2.getRawType();
					if (rawType instanceof Class<?> rawClass && rawClass == Optional.class) {
						var typeArgs = pt2.getActualTypeArguments();
						if (typeArgs.length > 0 && typeArgs[0] instanceof ParameterizedType nestedPt) {
							parameterizedType = nestedPt;
						} else {
							// Defensive check: if Optional<SomeClass> where SomeClass is not a ParameterizedType.
							// In practice, this is likely unreachable because:
							// - If pt is Map.class (raw), getElementType returns null and we return early
							// - If pt is Map<String,T> (parameterized), typeArgs[0] is a ParameterizedType
							// However, kept as a safety check for edge cases.
							return null;
						}
					}
				} else if (pt.innerType() instanceof ParameterizedType) {
					// If pt is already a parameterized type (e.g., Map<String,TestService> after unwrapping Optional),
					// use pt.innerType() as the parameterizedType
					parameterizedType = pt.innerType();
				} else {
					parameterizedType = null;
				}
			} else {
				// For fields, pt is already unwrapped from Optional, so use pt.innerType() directly
				parameterizedType = pt.innerType();
			}
			if (parameterizedType instanceof ParameterizedType pt2) {
				var typeArgs = pt2.getActualTypeArguments();
				if (typeArgs.length >= 2 && typeArgs[0] == String.class) {
					return new LinkedHashMap<>(beans);
				}
			}
			return null;
		}

		var values = new ArrayList<>(beans.values());

		// Convert to appropriate collection/array type
		if (pt.isArray()) {
			var array = Array.newInstance(elementType, values.size());
			for (var i = 0; i < values.size(); i++)
				Array.set(array, i, values.get(i));
			return array;
		}

		if (eq(inner, Set.class)) {
			return new LinkedHashSet<>(values);
		} else if (eq(inner, List.class)) {
			return values;
		}

		// Defensive check: return null if inner is null or not a supported collection type.
		return null;
	}

	private static Object getConstructorParameter(ParameterInfo pi, BeanStore beanStore, Object enclosingInstance, Object...otherBeans) {
		var pt = pi.getParameterType();
		if (pi.getIndex() == 0 && nn(enclosingInstance) && pt.isInstance(enclosingInstance))
			return enclosingInstance;
		return getParameter(pi, beanStore, otherBeans);
	}

	/**
	 * Extracts the element or value type from a collection, array, or map type.
	 *
	 * <p>
	 * For arrays, returns the component type.  For <c>List&lt;T&gt;</c> or <c>Set&lt;T&gt;</c>, returns <c>T</c>.
	 * For <c>Map&lt;String,T&gt;</c>, returns <c>T</c> (the value type).
	 *
	 * @param pi The parameter info containing generic type information (can be <jk>null</jk> for fields).
	 * @param pt The type (must be a collection, array, or map). For fields, this is already unwrapped from Optional.
	 * @return The element type (for arrays/collections) or value type (for maps),
	 * 	or <jk>null</jk> if the type is not a supported collection type or the element type cannot be determined.
	 */
	private static Class<?> getElementType(ParameterInfo pi, ClassInfo pt) {

		// Handle arrays
		if (pt.isArray()) {
			return pt.getComponentType().inner();
		}

		// Get the parameterized type
		Type parameterizedType;
		if (pi != null) {
			// For parameters, use ParameterInfo.getParameterizedType() and handle Optional unwrapping
			parameterizedType = pi.getParameterizedType();
			if (parameterizedType instanceof ParameterizedType pt2) {
				var rawType = pt2.getRawType();
				// If wrapped in Optional, unwrap it to get the nested type
				if (rawType instanceof Class<?> rawClass && rawClass == Optional.class) {
					var typeArgs = pt2.getActualTypeArguments();
					if (typeArgs.length > 0 && typeArgs[0] instanceof ParameterizedType nestedPt) {
						// Optional<List<T>> -> List<T>
						parameterizedType = nestedPt;
					} else {
						// Optional<SomeClass> - not a collection, return null
						return null;
					}
				}
			} else if (pt.innerType() instanceof ParameterizedType) {
				// If pt is already a parameterized type (e.g., List<TestService> after unwrapping Optional),
				// use pt.innerType() as the parameterizedType
				parameterizedType = pt.innerType();
			} else {
				parameterizedType = null;
			}
		} else {
			// For fields, pt is already unwrapped from Optional, so use pt.innerType() directly
			parameterizedType = pt.innerType();
		}

		// Handle List<T> or Set<T>
		var inner = opt(pt.inner()).orElse(Object.class);
		if (eq(inner, List.class) || eq(inner, Set.class)) {
			if (parameterizedType instanceof ParameterizedType pt2) {
				var typeArgs = pt2.getActualTypeArguments();
				if (typeArgs.length > 0 && typeArgs[0] instanceof Class<?> elementClass) {
					return elementClass;
				}
			}
		}

		// Handle Map<String,T> - extract value type (second type argument)
		if (eq(inner, Map.class)) {
			if (parameterizedType instanceof ParameterizedType pt2) {
				var typeArgs = pt2.getActualTypeArguments();
				// Verify key type is String and get value type
				if (typeArgs.length >= 2 && typeArgs[0] == String.class && typeArgs[1] instanceof Class<?> valueClass) {
					return valueClass;
				}
			}
		}

		return null;
	}

	private static Object getFieldValue(FieldInfo field, BeanStore beanStore) {
		var fieldType = field.getFieldType();

		// Find qualifier from @Named or @Qualifier annotation (same logic as ParameterInfo.getResolvedQualifier)
		var beanQualifier = field.getAnnotations().stream()
			.filter(ai -> ai.hasSimpleName("Named") || ai.hasSimpleName("Qualifier"))
			.map(ai -> ai.getValue().orElse(null))
			.filter(Objects::nonNull)
			.findFirst()
			.orElse(null);

		var ptUnwrapped = fieldType.unwrap(Optional.class);
		var collectionValue = getCollectionValue(null, ptUnwrapped, beanStore, beanQualifier);

		if (nn(collectionValue))
			return fieldType.is(Optional.class) ? Optional.of(collectionValue) : collectionValue;

		// Handle single bean
		var ptc = ptUnwrapped.inner();
		var o2 = beanQualifier == null ? beanStore.getBean(ptc) : beanStore.getBean(ptc, beanQualifier);
		
		if (fieldType.is(Optional.class))
			return o2;
		if (o2.isPresent())
			return o2.get();
		
		throw exex("Could not resolve value for field {0}", field);
	}

	private static String getMissingConstructorParameter(ParameterInfo pi, BeanStore beanStore, Object enclosingInstance, Object...otherBeans) {
		var pt = pi.getParameterType();
		if (pi.getIndex() == 0 && nn(enclosingInstance) && pt.isInstance(enclosingInstance))
			return null;
		return getMissingParameter(pi, beanStore, otherBeans);
	}

	private static String getMissingParameter(ParameterInfo pi, BeanStore beanStore, Object...otherBeans) {
		var pt = pi.getParameterType();
		if (pt.is(Optional.class) || isCollectionType(pt)) // Optional/Collection/array types are always satisfied (even if empty).
			return null;
		var bq = pi.getResolvedQualifier();  // Use @Named/@Qualified for bean injection
		if (nn(bq)) {
			if (! beanStore.hasBean(pt.inner(), bq))
				return pt.getNameSimple() + '@' + bq;
			return null;
		}
		if (beanStore.hasBean(pt.inner()))
			return null;
		for (var o : otherBeans)
			if (pi.canAccept(o))
				return null;
		return pt.getNameSimple();
	}

	private static Object getParameter(ParameterInfo pi, BeanStore beanStore, Object...otherBeans) {
		var pt = pi.getParameterType();
		var bq = pi.getResolvedQualifier();
		var ptu = pt.unwrap(Optional.class);

		// Handle collections and arrays
		var collectionValue = getCollectionValue(pi, ptu, beanStore, bq);
		if (nn(collectionValue))
			return pt.is(Optional.class) ? Optional.of(collectionValue) : collectionValue;

		// Handle single bean
		var ptc = ptu.inner();

		Optional<Object> r;

		if (nn(bq)) {
			r = beanStore.getBean(ptc, bq);
		} else {
			r = beanStore.getBean(ptc);
			if (r.isEmpty()) {
				for (var r2 : otherBeans)
					if (pi.canAccept(r2)) {
						r = opt(r2);
						break;
					}
			}
		}

		if (pt.is(Optional.class))
			return r;
		if (r.isPresent())
			return r.get();

		throw exex("Could not resolve value for parameter {0}", pi);
	}

	private static boolean hasConstructorParameter(ParameterInfo pi, BeanStore beanStore, Object enclosingInstance, Object...otherBeans) {
		var pt = pi.getParameterType();
		if (pi.getIndex() == 0 && nn(enclosingInstance) && pt.isInstance(enclosingInstance))
			return true;
		return hasParameter(pi, beanStore, otherBeans);
	}

	private static boolean hasParameter(ParameterInfo pi, BeanStore beanStore, Object...otherBeans) {
		var pt = pi.getParameterType();

		if (pt.is(Optional.class) || isCollectionType(pt)) // Optional/Collection/array types are always satisfied (even if empty).
			return true;

		var bq = pi.getResolvedQualifier();

		if (nn(bq))
			return beanStore.hasBean(pt.inner(), bq);

		if (beanStore.hasBean(pt.inner()))
			return true;

		for (var o : otherBeans)
			if (pi.canAccept(o))
				return true;
		return false;
	}

	/**
	 * Returns <jk>true</jk> if the specified type is a collection type that should be populated with multiple beans.
	 *
	 * <p>
	 * Supported collection types:
	 * <ul>
	 * 	<li>Arrays (<c>T[]</c>)
	 * 	<li><c>List&lt;T&gt;</c> (interface only, not concrete implementations like <c>ArrayList</c> or <c>LinkedList</c>)
	 * 	<li><c>Set&lt;T&gt;</c> (interface only, not concrete implementations like <c>HashSet</c> or <c>LinkedHashSet</c>)
	 * 	<li><c>Map&lt;String,T&gt;</c> (interface only, not concrete implementations like <c>HashMap</c> or <c>LinkedHashMap</c>)
	 * </ul>
	 *
	 * <p>
	 * <b>Note:</b> This matches Spring's behavior where only the base interfaces trigger automatic collection injection.
	 * Concrete implementations like <c>ArrayList&lt;T&gt;</c> are treated as regular bean types, not collection types.
	 *
	 * @param pt The parameter type to check.
	 * @return <jk>true</jk> if the type is a supported collection, array, or map type.
	 */
	private static boolean isCollectionType(ClassInfo pt) {
		if (pt.isArray())
			return true;
		var inner = opt(pt.inner()).orElse(Object.class);
		// Only match the exact interfaces, not their implementations (matches Spring's behavior)
		return eq(inner, List.class) || eq(inner, Set.class) || eq(inner, Map.class);
	}
}

