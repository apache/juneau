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
package org.apache.juneau.commons.utils;

import static java.util.stream.Collectors.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.lang.reflect.ParameterizedType;
import java.util.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.reflect.*;

/**
 * Utility class for resolving method and constructor parameters from a bean store.
 *
 * <p>
 * This class provides functionality for dependency injection by analyzing executable parameters (constructors and methods)
 * and resolving their values from a {@link BeanStore}.
 *
 * <h5 class='section'>Supported Parameter Types:</h5>
 * <ul class='spaced-list'>
 * 	<li><b>Single beans</b> - Parameters of type <c>T</c> are resolved by looking up a bean of that type in the bean store.
 * 	<li><b>Optional beans</b> - Parameters of type <c>Optional&lt;T&gt;</c> are resolved to <c>Optional.empty()</c> if no bean is found.
 * 	<li><b>Arrays</b> - Parameters of type <c>T[]</c> are populated with all beans of type <c>T</c> from the bean store.
 * 	<li><b>Lists</b> - Parameters of type <c>List&lt;T&gt;</c> are populated with all beans of type <c>T</c> from the bean store.
 * 	<li><b>Sets</b> - Parameters of type <c>Set&lt;T&gt;</c> are populated with all beans of type <c>T</c> from the bean store.
 * 	<li><b>Maps</b> - Parameters of type <c>Map&lt;String,T&gt;</c> are populated with all beans of type <c>T</c> keyed by bean name.
 * </ul>
 *
 * <h5 class='section'>Bean Qualifiers:</h5>
 * <p>
 * Parameters can be annotated with {@link org.apache.juneau.annotation.Named @Named} or {@code javax.inject.Qualifier @Qualifier}
 * to specify which named bean should be injected.  For collections, arrays, and maps, qualifiers are ignored and all beans
 * of the element type are included.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Bean store with multiple services</jc>
 * 	BeanStore <jv>beanStore</jv> = BasicBeanStore.<jsm>create</jsm>()
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
 * 	<jc>// Resolve parameters</jc>
 * 	BeanUtils <jv>utils</jv> = <jk>new</jk> BeanUtils();
 * 	Object[] <jv>params</jv> = <jv>utils</jv>.getParams(constructor, <jv>beanStore</jv>, <jk>null</jk>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link BeanStore}
 * 	<li class='jc'>{@link BasicBeanStore}
 * </ul>
 */
public class BeanUtils {

	/**
	 * Returns a comma-delimited list of parameter types that are missing from the bean store.
	 *
	 * <p>
	 * Analyzes the parameters of the specified executable and checks if all required beans are available in the bean store.
	 *
	 * <p>
	 * The following parameter types are considered optional and are not checked:
	 * <ul>
	 * 	<li>Parameters of type <c>Optional&lt;T&gt;</c>
	 * 	<li>Parameters of type <c>T[]</c>, <c>List&lt;T&gt;</c>, <c>Set&lt;T&gt;</c>, or <c>Map&lt;String,T&gt;</c>
	 * 	<li>The first parameter if it matches the <c>bean</c> object type (for non-static inner classes)
	 * </ul>
	 *
	 * <p>
	 * If a parameter has a {@link org.apache.juneau.annotation.Named @Named} or {@code @Qualifier} annotation,
	 * the method checks for a bean with that specific name.  Otherwise, it checks for an unnamed bean of the parameter type.
	 *
	 * @param executable The constructor or method to analyze.
	 * @param beanStore The bean store to check for beans.
	 * @param bean The outer class instance for non-static inner class constructors/methods.
	 * 	If the first parameter type matches this object's type, it is used as the first parameter and not checked in the bean store.
	 * 	Can be <jk>null</jk> for regular classes or static inner classes.
	 * @return A comma-delimited, sorted list of missing parameter types (e.g., <js>"String,Integer"</js>),
	 * 	or <jk>null</jk> if all required parameters are available.
	 */
	public String getMissingParams(ExecutableInfo executable, BeanStore beanStore, Object bean) {
		var params = executable.getParameters();
		List<String> l = list();
		loop: for (int i = 0; i < params.size(); i++) {
			var pi = params.get(i);
			var pt = pi.getParameterType();
			if (i == 0 && nn(bean) && pt.isInstance(bean))
				continue loop;
			if (pt.is(Optional.class))
				continue loop;
			if (isCollectionType(pt))
				continue loop; // Collections and arrays are always satisfied (even if empty), so skip them
			var beanName = pi.getResolvedQualifier();  // Use @Named/@Qualified for bean injection
			var ptc = pt.unwrap(Optional.class).inner();
			if (beanName == null && ! beanStore.hasBean(ptc))
				l.add(pt.getNameSimple());
			if (nn(beanName) && ! beanStore.hasBean(ptc, beanName))
				l.add(pt.getNameSimple() + '@' + beanName);
		}
		return l.isEmpty() ? null : l.stream().sorted().collect(joining(","));
	}

	/**
	 * Resolves and returns parameter values from the bean store for the specified executable.
	 *
	 * <p>
	 * For each parameter in the executable, this method:
	 * <ul>
	 * 	<li>If the first parameter type matches the <c>bean</c> object type, uses the <c>bean</c> object
	 * 		(for non-static inner class constructors/methods).
	 * 	<li>If the parameter is a collection/array/map type, collects all beans of the element type.
	 * 	<li>Otherwise, looks up a single bean by type and optional qualifier name.
	 * </ul>
	 *
	 * <h5 class='section'>Parameter Resolution:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>Single beans</b> - Resolved using {@link BeanStore#getBean(Class)} or {@link BeanStore#getBean(Class, String)}.
	 * 	<li><b>Optional beans</b> - Wrapped in <c>Optional</c>, or <c>Optional.empty()</c> if not found.
	 * 	<li><b>Arrays</b> - All beans of the element type are collected into an array.
	 * 	<li><b>Lists</b> - All beans of the element type are collected into a <c>List</c>.
	 * 	<li><b>Sets</b> - All beans of the element type are collected into a <c>LinkedHashSet</c>.
	 * 	<li><b>Maps</b> - All beans of the value type are collected into a <c>LinkedHashMap</c> keyed by bean name.
	 * </ul>
	 *
	 * @param executable The constructor or method to get parameters for.
	 * @param beanStore The bean store to resolve beans from.
	 * @param bean The outer class instance for non-static inner class constructors/methods.
	 * 	If the first parameter type matches this object's type, it is used as the first parameter value.
	 * 	Can be <jk>null</jk> for regular classes or static inner classes.
	 * @return An array of parameter values in the same order as the executable parameters.
	 * 	May contain <jk>null</jk> values if a required bean is not found (for non-Optional parameters).
	 */
	public Object[] getParams(ExecutableInfo executable, BeanStore beanStore, Object bean) {
		var o = new Object[executable.getParameterCount()];
		for (var i = 0; i < executable.getParameterCount(); i++) {
			var pi = executable.getParameter(i);
			var pt = pi.getParameterType();
			if (i == 0 && nn(bean) && pt.isInstance(bean)) {
				o[i] = bean;
			} else {
				var beanQualifier = pi.getResolvedQualifier();
				var ptUnwrapped = pt.unwrap(Optional.class);

				// Handle collections and arrays
				var collectionValue = getCollectionValue(pi, ptUnwrapped, beanStore, beanQualifier);
				if (nn(collectionValue)) {
					o[i] = pt.is(Optional.class) ? Optional.of(collectionValue) : collectionValue;
					continue;
				}

				// Handle single bean
				var ptc = ptUnwrapped.inner();
				var o2 = beanQualifier == null ? beanStore.getBean(ptc) : beanStore.getBean(ptc, beanQualifier);
				o[i] = pt.is(Optional.class) ? o2 : o2.orElse(null);
			}
		}
		return o;
	}

	/**
	 * Resolves parameters from the bean store and invokes the specified executable.
	 *
	 * <p>
	 * This is a convenience method that combines {@link #getParams(ExecutableInfo, BeanStore, Object)} with
	 * {@link ConstructorInfo#newInstance(Object...)} or {@link MethodInfo#invoke(Object, Object...)}.
	 *
	 * <p>
	 * Parameter usage:
	 * <ul>
	 * 	<li><b>For constructors</b> - The <c>bean</c> parameter is passed as the <c>outer</c> parameter to
	 * 		{@link #getParams(ExecutableInfo, BeanStore, Object)}.  For non-static inner class constructors,
	 * 		this will be used as the first parameter (the outer class instance).  For regular constructors,
	 * 		this parameter is ignored if it doesn't match the first parameter type.
	 * 	<li><b>For methods</b> - The <c>bean</c> parameter is the object instance on which to invoke the method
	 * 		(or <jk>null</jk> for static methods).  It is also passed as the <c>outer</c> parameter to
	 * 		{@link #getParams(ExecutableInfo, BeanStore, Object)} for inner class handling.
	 * </ul>
	 *
	 * @param <T> The return type of the executable.
	 * @param executable The constructor or method to invoke.
	 * @param beanStore The bean store to resolve parameters from.
	 * @param bean For constructors: the outer class instance for non-static inner classes (can be <jk>null</jk>).
	 * 	For methods: the object instance on which to invoke the method (or <jk>null</jk> for static methods).
	 * @return The result of invoking the executable.
	 * @throws ExecutableException If the executable cannot be invoked or parameter resolution fails.
	 */
	public <T> T invoke(ExecutableInfo executable, BeanStore beanStore, Object bean) {
		var params = getParams(executable, beanStore, bean);
		if (executable instanceof ConstructorInfo ci)
			return ci.newInstance(params);
		return MethodInfo.class.cast(executable).invoke(bean, params);
	}

	/**
	 * Returns <jk>true</jk> if the bean store has all required parameters for the specified executable.
	 *
	 * <p>
	 * This method performs the same checks as {@link #getMissingParams(ExecutableInfo, BeanStore, Object)} but
	 * returns a boolean instead of a list of missing types.
	 *
	 * <p>
	 * The following parameter types are considered optional and are not checked:
	 * <ul>
	 * 	<li>Parameters of type <c>Optional&lt;T&gt;</c>
	 * 	<li>Parameters of type <c>T[]</c>, <c>List&lt;T&gt;</c>, <c>Set&lt;T&gt;</c>, or <c>Map&lt;String,T&gt;</c>
	 * 	<li>The first parameter if it matches the <c>bean</c> object type (for non-static inner classes)
	 * </ul>
	 *
	 * @param executable The constructor or method to check.
	 * @param beanStore The bean store to check for beans.
	 * @param bean The outer class instance for non-static inner class constructors/methods.
	 * 	If the first parameter type matches this object's type, it is used as the first parameter and not checked in the bean store.
	 * 	Can be <jk>null</jk> for regular classes or static inner classes.
	 * @return <jk>true</jk> if all required parameters are available in the bean store, <jk>false</jk> otherwise.
	 */
	public boolean hasAllParams(ExecutableInfo executable, BeanStore beanStore, Object bean) {
		loop: for (int i = 0; i < executable.getParameterCount(); i++) {
			var pi = executable.getParameter(i);
			var pt = pi.getParameterType();
			if (i == 0 && nn(bean) && pt.isInstance(bean))
				continue loop;
			if (pt.is(Optional.class))
				continue loop;
			if (isCollectionType(pt))
				continue loop; // Collections and arrays are always satisfied (even if empty)
			var beanQualifier = pi.getResolvedQualifier();
			var ptc = pt.unwrap(Optional.class).inner();
			if ((beanQualifier == null && ! beanStore.hasBean(ptc)) || (nn(beanQualifier) && ! beanStore.hasBean(ptc, beanQualifier)))
				return false;
		}
		return true;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helper methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns <jk>true</jk> if the specified type is a collection type that should be populated with multiple beans.
	 *
	 * <p>
	 * Supported collection types:
	 * <ul>
	 * 	<li>Arrays (<c>T[]</c>)
	 * 	<li><c>List&lt;T&gt;</c>
	 * 	<li><c>Set&lt;T&gt;</c>
	 * 	<li><c>Map&lt;String,T&gt;</c> (key type must be <c>String</c>)
	 * </ul>
	 *
	 * @param pt The parameter type to check.
	 * @return <jk>true</jk> if the type is a supported collection, array, or map type.
	 */
	@SuppressWarnings("static-method")
	private boolean isCollectionType(ClassInfo pt) {
		if (pt.isArray())
			return true;
		var inner = pt.inner();
		if (nn(inner)) {
			return List.class.isAssignableFrom(inner) || Set.class.isAssignableFrom(inner) || Map.class.isAssignableFrom(inner);
		}
		// Defensive check: inner() returns null for non-class types (e.g., TypeVariable, WildcardType).
		// In practice, this is unreachable for real parameter types since Parameter.getType() always returns a Class.
		// However, it's kept as a safety check for edge cases or future use.
		return false;
	}

	/**
	 * Extracts the element or value type from a collection, array, or map parameter type.
	 *
	 * <p>
	 * For arrays, returns the component type.  For <c>List&lt;T&gt;</c> or <c>Set&lt;T&gt;</c>, returns <c>T</c>.
	 * For <c>Map&lt;String,T&gt;</c>, returns <c>T</c> (the value type).
	 *
	 * @param pi The parameter info containing generic type information.
	 * @param pt The parameter type (must be a collection, array, or map).
	 * @return The element type (for arrays/collections) or value type (for maps),
	 * 	or <jk>null</jk> if the type is not a supported collection type or the element type cannot be determined.
	 */
	@SuppressWarnings("static-method")
	private Class<?> getElementType(ParameterInfo pi, ClassInfo pt) {
		// Handle arrays
		if (pt.isArray()) {
			return pt.getComponentType().inner();
		}

		// Get the parameterized type and unwrap Optional if present
		var parameterizedType = pi.getParameterizedType();
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
		}

		// Handle List<T> or Set<T>
		var inner = pt.inner();
		if (nn(inner) && (List.class.isAssignableFrom(inner) || Set.class.isAssignableFrom(inner))) {
			if (parameterizedType instanceof ParameterizedType pt2) {
				var typeArgs = pt2.getActualTypeArguments();
				if (typeArgs.length > 0 && typeArgs[0] instanceof Class<?> elementClass) {
					return elementClass;
				}
			}
		}

		// Handle Map<String,T> - extract value type (second type argument)
		if (nn(inner) && Map.class.isAssignableFrom(inner)) {
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

	/**
	 * Resolves and creates a collection, array, or map containing all beans of the element type from the bean store.
	 *
	 * <p>
	 * Retrieves all beans of the element type using {@link BeanStore#getBeansOfType(Class)} and converts them
	 * to the appropriate collection type:
	 * <ul>
	 * 	<li>Arrays - Creates an array of the element type
	 * 	<li><c>List&lt;T&gt;</c> - Creates an <c>ArrayList</c>
	 * 	<li><c>Set&lt;T&gt;</c> - Creates a <c>LinkedHashSet</c> (preserves insertion order)
	 * 	<li><c>Map&lt;String,T&gt;</c> - Creates a <c>LinkedHashMap</c> keyed by bean name
	 * </ul>
	 *
	 * <p>
	 * The <c>beanQualifier</c> parameter is ignored for collections, as all beans of the element type are included.
	 *
	 * @param pi The parameter info containing generic type information.
	 * @param pt The parameter type (unwrapped from <c>Optional</c>).
	 * @param beanStore The bean store to retrieve beans from.
	 * @param beanQualifier The bean qualifier (ignored for collections/arrays/maps).
	 * @return The collection, array, or map containing all beans of the element type,
	 * 	or <jk>null</jk> if the parameter type is not a supported collection type.
	 */
	private Object getCollectionValue(ParameterInfo pi, ClassInfo pt, BeanStore beanStore, String beanQualifier) {
		if (! isCollectionType(pt))
			return null;

		var elementType = getElementType(pi, pt);
		if (elementType == null)
			return null;

		// Get all beans of the element type
		var beans = beanStore.getBeansOfType(elementType);

		// Handle Map<String,T> - getBeansOfType already returns Map<String,T>
		var inner = pt.inner();
		if (nn(inner) && Map.class.isAssignableFrom(inner)) {
			// Verify it's Map<String,T> and return the map directly
			var parameterizedType = pi.getParameterizedType();
			// Unwrap Optional if present
			if (parameterizedType instanceof ParameterizedType pt2) {
				var rawType = pt2.getRawType();
				if (rawType instanceof Class<?> rawClass && rawClass == Optional.class) {
					var typeArgs = pt2.getActualTypeArguments();
					if (typeArgs.length > 0 && typeArgs[0] instanceof ParameterizedType nestedPt) {
						parameterizedType = nestedPt;
					} else {
						// Defensive check: if Optional<SomeClass> where SomeClass is not a ParameterizedType.
						// In practice, this is likely unreachable because:
						// - If pt is Map.class (raw), getElementType returns null and we return early (line 383)
						// - If pt is Map<String,T> (parameterized), typeArgs[0] is a ParameterizedType
						// However, kept as a safety check for edge cases.
						return null;
					}
				}
			} else if (pt.innerType() instanceof ParameterizedType) {
				// If pt is already a parameterized type (e.g., Map<String,TestService> after unwrapping Optional),
				// use pt.innerType() as the parameterizedType
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
			var array = java.lang.reflect.Array.newInstance(elementType, values.size());
			for (var i = 0; i < values.size(); i++)
				java.lang.reflect.Array.set(array, i, values.get(i));
			return array;
		}

		if (nn(inner)) {
			if (Set.class.isAssignableFrom(inner)) {
				return new LinkedHashSet<>(values);
			} else if (List.class.isAssignableFrom(inner)) {
				return values;
			}
		}

		// Defensive check: return null if inner is null or not a supported collection type.
		// In practice, this is likely unreachable because:
		// - isCollectionType only returns true for List, Set, Map, or array
		// - List and Set are handled above (lines 430-434)
		// - Map is handled earlier (lines 390-416)
		// - Arrays are handled earlier (lines 422-426)
		// - If inner() is null, isCollectionType returns false, so we return early (line 379)
		// However, kept as a safety check for edge cases.
		return null;
	}
}
