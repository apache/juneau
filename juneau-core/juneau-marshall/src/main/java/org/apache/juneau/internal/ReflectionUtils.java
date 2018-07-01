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
package org.apache.juneau.internal;

import static org.apache.juneau.internal.CollectionUtils.*;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * Reflection utilities.
 */
public final class ReflectionUtils {

	/**
	 * Returns <jk>true</jk> if the {@link #getAnnotation(Class, Method, int)} returns a value.
	 * 
	 * @param a The annotation to check for.
	 * @param m The method containing the parameter to check.
	 * @param index The parameter index.
	 * @return <jk>true</jk> if the {@link #getAnnotation(Class, Method, int)} returns a value.
	 */
	public static boolean hasAnnotation(Class<? extends Annotation> a, Method m, int index) {
		return getAnnotation(a, m, index) != null;
	}

	/**
	 * Returns the specified annotation if it exists on the specified parameter or parameter type class.
	 * 
	 * @param a The annotation to check for.
	 * @param m The method containing the parameter to check.
	 * @param index The parameter index.
	 * @return <jk>true</jk> if the {@link #getAnnotation(Class, Method, int)} returns a value.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Annotation> T getAnnotation(Class<T> a, Method m, int index) {
		for (Annotation a2 :  m.getParameterAnnotations()[index])
			if (a.isInstance(a2))
				return (T)a2;
		Type t = m.getGenericParameterTypes()[index];
		if (t instanceof Class) 
			return getAnnotation(a, (Class<?>)t);
		return null;
	}
	
	/**
	 * Similar to {@link Class#getAnnotation(Class)} except also searches annotations on interfaces.
	 * 
	 * @param <T> The annotation class type.
	 * @param a The annotation class.
	 * @param c The annotated class.
	 * @return The annotation, or <jk>null</jk> if not found.
	 */
	public static <T extends Annotation> T getAnnotation(Class<T> a, Class<?> c) {
		if (c == null)
			return null;

		T t = getDeclaredAnnotation(a, c);
		if (t != null)
			return t;

		t = getAnnotation(a, c.getSuperclass());
		if (t != null)
			return t;

		for (Class<?> c2 : c.getInterfaces()) {
			t = getAnnotation(a, c2);
			if (t != null)
				return t;
		}
		return null;
	}

	/**
	 * Returns the specified annotation only if it's been declared on the specified class.
	 * 
	 * <p>
	 * More efficient than calling {@link Class#getAnnotation(Class)} since it doesn't recursively look for the class
	 * up the parent chain.
	 * 
	 * @param <T> The annotation class type.
	 * @param a The annotation class.
	 * @param c The annotated class.
	 * @return The annotation, or <jk>null</jk> if not found.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Annotation> T getDeclaredAnnotation(Class<T> a, Class<?> c) {
		for (Annotation a2 : c.getDeclaredAnnotations())
			if (a2.annotationType() == a)
				return (T)a2;
		return null;
	}

	/**
	 * Returns all instances of the specified annotation on the specified class.
	 * 
	 * <p>
	 * Searches all superclasses and superinterfaces.
	 * Results are ordered child-to-parent.
	 * 
	 * @param <T> The annotation class type.
	 * @param a The annotation class type.
	 * @param c The class being searched.
	 * @return The found matches, or an empty array if annotation was not found.
	 */
	public static <T extends Annotation> List<T> findAnnotations(Class<T> a, Class<?> c) {
		List<T> l = new LinkedList<>();
		appendAnnotations(a, c, l);
		return l;
	}

	/**
	 * Same as {@link #findAnnotations(Class, Class)} but returns the list in parent-to-child order.
	 * 
	 * @param a The annotation class type.
	 * @param c The class being searched.
	 * @return The found matches, or an empty array if annotation was not found.
	 */
	public static <T extends Annotation> List<T> findAnnotationsParentFirst(Class<T> a, Class<?> c) {
		List<T> l = findAnnotations(a, c);
		Collections.reverse(l);
		return l;
	}

	/**
	 * Same as {@link #findAnnotations(Class, Class)} except returns the annotations as a map with the keys being the
	 * class on which the annotation was found.
	 * 
	 * <p>
	 * Results are ordered child-to-parent.
	 * 
	 * @param <T> The annotation class type.
	 * @param a The annotation class type.
	 * @param c The class being searched.
	 * @return The found matches, or an empty map if annotation was not found.
	 */
	public static <T extends Annotation> LinkedHashMap<Class<?>,T> findAnnotationsMap(Class<T> a, Class<?> c) {
		LinkedHashMap<Class<?>,T> m = new LinkedHashMap<>();
		findAnnotationsMap(a, c, m);
		return m;
	}

	/**
	 * Same as {@link #findAnnotationsMap(Class, Class)} except returns results in parent-to-child order.
	 * 
	 * @param <T> The annotation class type.
	 * @param a The annotation class type.
	 * @param c The class being searched.
	 * @return The found matches, or an empty map if annotation was not found.
	 */
	public static <T extends Annotation> LinkedHashMap<Class<?>,T> findAnnotationsMapParentFirst(Class<T> a, Class<?> c) {
		return CollectionUtils.reverse(findAnnotationsMap(a, c));
	}

	private static <T extends Annotation> void findAnnotationsMap(Class<T> a, Class<?> c, Map<Class<?>,T> m) {
		if (c == null)
			return;

		T t = getDeclaredAnnotation(a, c);
		if (t != null)
			m.put(c, t);

		findAnnotationsMap(a, c.getSuperclass(), m);

		for (Class<?> c2 : c.getInterfaces())
			findAnnotationsMap(a, c2, m);
	}

	/**
	 * Finds and appends the specified annotation on the specified class and superclasses/interfaces to the specified
	 * list.
	 * 
	 * @param a The annotation.
	 * @param c The class.
	 * @param l The list of annotations.
	 */
	public static <T extends Annotation> void appendAnnotations(Class<T> a, Class<?> c, List<T> l) {
		if (c == null)
			return;

		addIfNotNull(l, getDeclaredAnnotation(a, c));

		if (c.getPackage() != null)
			addIfNotNull(l, c.getPackage().getAnnotation(a));

		appendAnnotations(a, c.getSuperclass(), l);

		for (Class<?> c2 : c.getInterfaces())
			appendAnnotations(a, c2, l);
	}

	/**
	 * Similar to {@link Class#getResourceAsStream(String)} except looks up the parent hierarchy for the existence of
	 * the specified resource.
	 * 
	 * @param c The class to return the resource on.
	 * @param name The resource name.
	 * @return An input stream on the specified resource, or <jk>null</jk> if the resource could not be found.
	 */
	public static InputStream getResource(Class<?> c, String name) {
		if (name == null)
			return null;
		while (c != null) {
			InputStream is = c.getResourceAsStream(name);
			if (is != null)
				return is;
			c = c.getSuperclass();
		}
		return null;
	}
}

