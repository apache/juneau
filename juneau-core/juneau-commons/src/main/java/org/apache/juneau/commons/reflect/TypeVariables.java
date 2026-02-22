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
package org.apache.juneau.commons.reflect;

import java.lang.reflect.*;
import java.util.*;

/**
 * Encapsulates type variable implementations for a class hierarchy.
 *
 * <p>
 * This class provides a type-safe wrapper around type variable implementations,
 * using {@link List} instead of arrays for better API consistency.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Given class hierarchy:</jc>
 * 	<jk>public static class</jk> BeanA&lt;T&gt; {
 * 		<jk>public</jk> T <jf>x</jf>;
 * 	}
 * 	<jk>public static class</jk> BeanB <jk>extends</jk> BeanA&lt;Integer&gt; {...}
 *
 * 	<jc>// Create TypeVariables from BeanB</jc>
 * 	TypeVariables <jv>tv</jv> = TypeVariables.<jsm>of</jsm>(BeanB.<jk>class</jk>);
 *
 * 	<jc>// Get the type variable implementation for BeanA</jc>
 * 	Class&lt;?&gt; <jv>impl</jv> = <jv>tv</jv>.get(BeanA.<jk>class</jk>, 0);  <jc>// Returns Integer.class</jc>
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li>{@link #of(Type)}
 * </ul>
 */
public class TypeVariables {

	private final Map<Class<?>,List<Class<?>>> map;

	/**
	 * Creates an empty TypeVariables instance.
	 *
	 * @return A new empty TypeVariables instance.
	 */
	public static TypeVariables empty() {
		return new TypeVariables(new LinkedHashMap<>());
	}

	/**
	 * Creates a TypeVariables instance from the specified type.
	 *
	 * <p>
	 * Recursively determines the classes represented by parameterized types in the class hierarchy of the specified
	 * type.
	 *
	 * <p>
	 * For example, given the following classes...
	 * <p class='bjava'>
	 * 	<jk>public static class</jk> BeanA&lt;T&gt; {
	 * 		<jk>public</jk> T <jf>x</jf>;
	 * 	}
	 * 	<jk>public static class</jk> BeanB <jk>extends</jk> BeanA&lt;Integer&gt; {...}
	 * </p>
	 * <p>
	 * 	...calling this method on {@code BeanB.class} will return a {@link TypeVariables} instance indicating
	 * 	that the {@code T} parameter on the BeanA class is implemented with an {@code Integer}:
	 * <p class='bcode'>
	 * 	{BeanA.class:[Integer.class]}
	 * </p>
	 *
	 * <h5 class='section'>Known Limitations:</h5>
	 * <p>
	 * This code doesn't currently properly handle the following situation with nested generic bounds:
	 * <p class='bjava'>
	 * 	<jk>public static class</jk> BeanB&lt;T <jk>extends</jk> Number&gt; <jk>extends</jk> BeanA&lt;>;
	 * 	<jk>public static class</jk> BeanC <jk>extends</jk> BeanB&lt;Integer&gt;;
	 * </p>
	 *
	 * <p>
	 * When called on {@code BeanC}, the type variable will be resolved as {@code Number}, not {@code Integer}.
	 * This limitation exists because the intermediate type parameter bound information is lost during type resolution
	 *
	 * @param type The type to analyze.
	 * @return A new TypeVariables instance.
	 */
	public static TypeVariables of(Type type) {
		Map<Class<?>, List<Class<?>>> m = new LinkedHashMap<>();
		findTypeVarImpls(type, m);
		return fromMap(m);
	}

	/**
	 * Recursively determines the classes represented by parameterized types in the class hierarchy.
	 *
	 * @param t The type we're recursing.
	 * @param m Where the results are loaded.
	 */
	@SuppressWarnings({
		"java:S3776" // Cognitive complexity acceptable for type variable implementation discovery
	})
	private static void findTypeVarImpls(Type t, Map<Class<?>, List<Class<?>>> m) {
		if (t instanceof Class<?> c) {
			findTypeVarImpls(c.getGenericSuperclass(), m);
			for (var ci : c.getGenericInterfaces())
				findTypeVarImpls(ci, m);
		} else if (t instanceof ParameterizedType t2) {
			var rt = t2.getRawType();
			if (rt instanceof Class<?> rt2) {
				var gImpls = t2.getActualTypeArguments();
				var gTypes = new ArrayList<Class<?>>(gImpls.length);
				for (var gt : gImpls) {
					if (gt instanceof Class<?> c)
						gTypes.add(c);
					else if (gt instanceof TypeVariable<?> tv) {
						for (var upperBound : tv.getBounds())
							if (upperBound instanceof Class upperBound2) {
								gTypes.add(upperBound2);
								break;
							}
					}
				}
				m.put(rt2, gTypes);
				findTypeVarImpls(t2.getRawType(), m);
			}
		}
	}

	/**
	 * Creates a TypeVariables instance from a map of lists.
	 *
	 * <p>
	 * Package-private factory method for internal use.
	 *
	 * @param map The map of class to type variable implementations.
	 * @return A new TypeVariables instance.
	 */
	static TypeVariables fromMap(Map<Class<?>, List<Class<?>>> map) {
		return new TypeVariables(map);
	}

	/**
	 * Constructor.
	 *
	 * @param map The map of class to type variable implementations.
	 */
	TypeVariables(Map<Class<?>, List<Class<?>>> map) {
		this.map = map;
	}

	/**
	 * Returns <jk>true</jk> if this instance contains type variable implementations for the specified class.
	 *
	 * @param clazz The class to check.
	 * @return <jk>true</jk> if type variable implementations exist for the class.
	 */
	public boolean containsKey(Class<?> clazz) {
		return map.containsKey(clazz);
	}

	/**
	 * Returns the list of type variable implementations for the specified class.
	 *
	 * @param clazz The class to get type variable implementations for.
	 * @return The list of type variable implementations, or <jk>null</jk> if not found.
	 */
	public List<Class<?>> get(Class<?> clazz) {
		return map.get(clazz);
	}

	/**
	 * Returns the type variable implementation at the specified index for the given class.
	 *
	 * @param clazz The class to get the type variable implementation for.
	 * @param index The zero-based index of the type variable.
	 * @return The type variable implementation, or <jk>null</jk> if not found or index is out of bounds.
	 */
	public Class<?> get(Class<?> clazz, int index) {
		var list = map.get(clazz);
		if (list == null || index < 0 || index >= list.size())
			return null;
		return list.get(index);
	}

	/**
	 * Returns <jk>true</jk> if this instance is empty (contains no type variable implementations).
	 *
	 * @return <jk>true</jk> if this instance is empty.
	 */
	public boolean isEmpty() {
		return map.isEmpty();
	}

	/**
	 * Returns the number of classes with type variable implementations.
	 *
	 * @return The number of classes.
	 */
	public int size() {
		return map.size();
	}

	/**
	 * Converts a {@link Type} to a {@link Class} if possible.
	 *
	 * <p>
	 * This method resolves generic types, parameterized types, generic arrays, and type variables
	 * to their concrete class representations.
	 *
	 * <h5 class='section'>Type Resolution:</h5>
	 * <ul>
	 * 	<li><b>Class</b> - Returns the class as-is
	 * 	<li><b>ParameterizedType</b> - Returns the raw type class
	 * 	<li><b>GenericArrayType</b> - Creates and returns the array class
	 * 	<li><b>TypeVariable</b> - Resolves using type variable implementations, or returns <jk>null</jk> if not found
	 * </ul>
	 *
	 * @param t The type to resolve.
	 * @return The resolved class, or <jk>null</jk> if the type cannot be resolved to a class.
	 */
	@SuppressWarnings({
		"java:S3776" // Cognitive complexity acceptable for type resolution logic
	})
	public Class<?> resolve(Type t) {
		if (t instanceof Class<?> c)
			return c;

		if (t instanceof ParameterizedType pt)
			// A parameter (e.g. <String>.
			return (Class<?>)pt.getRawType();

		if (t instanceof GenericArrayType gat) {
			// An array parameter (e.g. <byte[]>).
			var gatct = gat.getGenericComponentType();

			if (gatct instanceof Class<?> gatct2)
				return Array.newInstance(gatct2, 0).getClass();

			if (gatct instanceof ParameterizedType gatct3)
				return Array.newInstance((Class<?>)gatct3.getRawType(), 0).getClass();

			if (gatct instanceof GenericArrayType gatct4) {
				var resolved = resolve(gatct4);
				return resolved != null ? Array.newInstance(resolved, 0).getClass() : null;
			}

			return null;

		} else if (t instanceof TypeVariable<?> tv) {
			String varName = tv.getName();
			int varIndex = -1;
			var gc = (Class<?>)tv.getGenericDeclaration();
			TypeVariable<?>[] tvv = gc.getTypeParameters();
			for (var i = 0; i < tvv.length; i++) {
				if (tvv[i].getName().equals(varName)) {
					varIndex = i;
				}
			}
			if (varIndex != -1) {
				// If we couldn't find a type variable implementation, that means
				// the type was defined at runtime (e.g. Bean b = new Bean<Foo>();)
				// in which case the type is lost through erasure.
				// Assume java.lang.Object as the type.
				return get(gc, varIndex);
			}
		}
		return null;
	}

	/**
	 * Static convenience method to resolve a type using type variable implementations.
	 *
	 * <p>
	 * This method can resolve types even when {@code typeVars} is <jk>null</jk> for non-type-variable types
	 * (e.g., {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}).
	 * For {@link TypeVariable} types, {@code typeVars} must not be <jk>null</jk>.
	 *
	 * @param type The type to resolve.
	 * @param typeVars The type variable implementations, or <jk>null</jk>.
	 * @return The resolved class, or <jk>null</jk> if the type cannot be resolved.
	 */
	public static Class<?> resolve(Type type, TypeVariables typeVars) {
		if (type instanceof Class<?> c)
			return c;

		if (type instanceof ParameterizedType pt)
			return (Class<?>)pt.getRawType();

		if (type instanceof GenericArrayType gat) {
			var gatct = gat.getGenericComponentType();

			if (gatct instanceof Class<?> gatct2)
				return Array.newInstance(gatct2, 0).getClass();

			if (gatct instanceof ParameterizedType gatct3)
				return Array.newInstance((Class<?>)gatct3.getRawType(), 0).getClass();

			if (gatct instanceof GenericArrayType gatct4) {
				var resolved = resolve(gatct4, typeVars);
				return resolved != null ? Array.newInstance(resolved, 0).getClass() : null;
			}

			return null;
		}

		if (type instanceof TypeVariable<?> tv) {
			if (typeVars == null)
				return null;
			return typeVars.resolve(tv);
		}

		return null;
	}
}

