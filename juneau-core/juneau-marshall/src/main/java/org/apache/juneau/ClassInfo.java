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
package org.apache.juneau;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.internal.*;

/**
 * Utility class for introspecting information about a class.
 */
public class ClassInfo {

	private final Type type;
	private final Class<?> c;
	private Map<Class<?>,Optional<Annotation>> annotationMap;
	private Map<Class<?>,List<?>> annotationsMap;
	private Map<Class<?>,List<?>> annotationsPfMap;
	private Optional<ClassInfo> parent;
	private ClassInfo[] interfaces;

	private static final Map<Type,ClassInfo> CACHE = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param t The class type.
	 */
	public ClassInfo(Type t) {
		this.type = t;
		this.c = ClassUtils.toClass(t);
	}

	/**
	 * Same as using the constructor, but returns <jk>null</jk> if the type is <jk>null</jk>.
	 *
	 * @param t The class type.
	 * @return The constructed class info.
	 */
	public static ClassInfo create(Type t) {
		if (t == null)
			return null;
		return new ClassInfo(t);
	}

	/**
	 * Returns the cached instance of the specified type.
	 *
	 * @param t The class type.
	 * @return The cached class info, or <jk>null</jk> if the type is <jk>null</jk>.
	 */
	public synchronized static ClassInfo lookup(Type t) {
		if (t == null)
			return null;
		ClassInfo ci = CACHE.get(t);
		if (ci == null) {
			ci = create(t);
			CACHE.put(t, ci);
		}
		return ci;
	}

	/**
	 * Returns the wrapped class.
	 *
	 * @return The wrapped class.
	 */
	public Type getInner() {
		return type;
	}

	/**
	 * Returns the wrapped class.
	 *
	 * @return The wrapped class or <jk>null</jk> if it's not a class.
	 */
	public Class<?> getInnerClass() {
		return c;
	}

	/**
	 * Returns the parent class info.
	 *
	 * @return The parent class info, or <jk>null</jk> if the class has no parent.
	 */
	public synchronized ClassInfo getParent() {
		if (parent == null)
			parent = Optional.ofNullable(c == null ? null : create(c.getSuperclass()));
		return parent.isPresent() ? parent.get() : null;
	}

	/**
	 * Returns the interfaces info.
	 *
	 * @return The implemented interfaces info, or an empty array if the class has no interfaces.
	 */
	public synchronized ClassInfo[] getInterfaces() {
		if (interfaces == null) {
			interfaces = new ClassInfo[c == null ? 0 : c.getInterfaces().length];
			for (int i = 0; i < interfaces.length; i++)
				interfaces[i] = ClassInfo.create(c.getInterfaces()[i]);
		}
		return interfaces;
	}

	/**
	 * Finds the annotation of the specified type defined on this method.
	 *
	 * <p>
	 * If the annotation cannot be found on the immediate method, searches methods with the same
	 * signature on the parent classes or interfaces.
	 * <br>The search is performed in child-to-parent order.
	 *
	 * <p>
	 * If still not found, searches for the annotation on the return type of the method.
	 *
	 * @param a
	 * 	The annotation to search for.
	 * @return
	 * 	The annotation if found, or <jk>null</jk> if not.
	 */
	@SuppressWarnings("unchecked")
	public <T extends Annotation> T getAnnotation(Class<T> a) {
		Optional<Annotation> o = annotationMap().get(a);
		if (o == null) {
			o = Optional.ofNullable(findAnnotation(a));
			annotationMap().put(a, o);
		}
		return o.isPresent() ? (T)o.get() : null;
	}

	/**
	 * Returns <jk>true</jk> if this method has the specified annotation.
	 *
	 * @param a
	 * 	The annotation to search for.
	 * @return
	 * 	The <jk>true</jk> if annotation if found.
	 */
	public boolean hasAnnotation(Class<? extends Annotation> a) {
		return getAnnotation(a) != null;
	}

	private <T extends Annotation> T findAnnotation(Class<T> a) {
		if (c != null) {
			T t2 = getDeclaredAnnotation(a);
			if (t2 != null)
				return t2;

			ClassInfo sci = getParent();
			if (sci != null) {
				t2 = sci.getAnnotation(a);
				if (t2 != null)
					return t2;
			}

			for (ClassInfo c2 : getInterfaces()) {
				t2 = c2.getAnnotation(a);
				if (t2 != null)
					return t2;
			}
		}
		return null;

	}

	/**
	 * Returns all annotations of the specified type defined on the specified method.
	 *
	 * <p>
	 * Searches all methods with the same signature on the parent classes or interfaces
	 * and the return type on the method.
	 *
	 * @param a
	 * 	The annotation to search for.
	 * @return
	 * 	A list of all matching annotations found in child-to-parent order, or an empty list if none found.
	 */
	@SuppressWarnings("unchecked")
	public <T extends Annotation> List<T> getAnnotations(Class<T> a) {
		List<T> l = (List<T>)annotationsMap().get(a);
		if (l == null) {
			l = Collections.unmodifiableList(findAnnotations(a));
			annotationsMap().put(a, l);
		}
		return l;
	}

	/**
	 * Identical to {@link #getAnnotations(Class)} but returns the list in reverse (parent-to-child) order.
	 *
	 * @param a
	 * 	The annotation to search for.
	 * @return
	 * 	A list of all matching annotations found in parent-to-child order, or an empty list if none found.
	 */
	@SuppressWarnings("unchecked")
	public <T extends Annotation> List<T> getAnnotationsParentFirst(Class<T> a) {
		List<T> l = (List<T>)annotationsPfMap().get(a);
		if (l == null) {
			l = new ArrayList<>(getAnnotations(a));
			Collections.reverse(l);
			l = Collections.unmodifiableList(l);
			annotationsPfMap().put(a, l);
		}
		return l;
	}

	private <T extends Annotation> List<T> findAnnotations(Class<T> a) {
		List<T> l = new LinkedList<>();
		ClassUtils.appendAnnotations(a, type, l);
		return l;
	}

	private synchronized Map<Class<?>,Optional<Annotation>> annotationMap() {
		if (annotationMap == null)
			annotationMap = new ConcurrentHashMap<>();
		return annotationMap;
	}

	private synchronized Map<Class<?>,List<?>> annotationsMap() {
		if (annotationsMap == null)
			annotationsMap = new ConcurrentHashMap<>();
		return annotationsMap;
	}

	private synchronized Map<Class<?>,List<?>> annotationsPfMap() {
		if (annotationsPfMap == null)
			annotationsPfMap = new ConcurrentHashMap<>();
		return annotationsPfMap;
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
	 * @return The annotation, or <jk>null</jk> if not found.
	 */
	@SuppressWarnings("unchecked")
	public <T extends Annotation> T getDeclaredAnnotation(Class<T> a) {
		if (c != null)
			for (Annotation a2 : c.getDeclaredAnnotations())
				if (a2.annotationType() == a)
					return (T)a2;
		return null;
	}
}
