/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.utils;

import static com.ibm.juno.core.utils.CollectionUtils.*;

import java.io.*;
import java.lang.annotation.*;
import java.util.*;

/**
 * Reflection utilities.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class ReflectionUtils {

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
	 * <p>
	 * 	More efficient than calling {@link Class#getAnnotation(Class)} since it doesn't
	 * 	recursively look for the class up the parent chain.
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
	 * <p>
	 * Searches all superclasses and superinterfaces.
	 * <p>
	 * Results are ordered child-to-parent.
	 *
	 * @param <T> The annotation class type.
	 * @param a The annotation class type.
	 * @param c The class being searched.
	 * @return The found matches, or an empty array if annotation was not found.
	 */
	public static <T extends Annotation> List<T> findAnnotations(Class<T> a, Class<?> c) {
		List<T> l = new LinkedList<T>();
		appendAnnotations(a, c, l);
		return l;
	}

	/**
	 * Sames as {@link #findAnnotations(Class, Class)} except returns the annotations as a map
	 * with the keys being the class on which the annotation was found.
	 * <p>
	 * Results are ordered child-to-parent.
	 *
	 * @param <T> The annotation class type.
	 * @param a The annotation class type.
	 * @param c The class being searched.
	 * @return The found matches, or an empty array if annotation was not found.
	 */
	public static <T extends Annotation> LinkedHashMap<Class<?>,T> findAnnotationsMap(Class<T> a, Class<?> c) {
		LinkedHashMap<Class<?>,T> m = new LinkedHashMap<Class<?>,T>();
		findAnnotationsMap(a, c, m);
		return m;
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
	 * Finds and appends the specified annotation on the specified class and superclasses/interfaces to the specified list.
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
	 * Similar to {@link Class#getResourceAsStream(String)} except looks up the
	 * parent hierarchy for the existence of the specified resource.
	 *
	 * @param c The class to return the resource on.
	 * @param name The resource name.
	 * @return An input stream on the specified resource, or <jk>null</jk> if the resource could not be found.
	 */
	public static InputStream getResource(Class<?> c, String name) {
		while (c != null) {
			InputStream is = c.getResourceAsStream(name);
			if (is != null)
				return is;
			c = c.getSuperclass();
		}
		return null;
	}
}
