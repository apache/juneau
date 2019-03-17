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
package org.apache.juneau.reflection;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;

/**
 * Utility class for introspecting information about a method parameter.
 */
public final class MethodParamInfo {

	private MethodInfo methodInfo;
	private int index;
	private Map<Class<?>,Optional<Annotation>> annotationMap = new ConcurrentHashMap<>();
	private Map<Class<?>,List<?>> annotationsMap = new ConcurrentHashMap<>();
	private Map<Class<?>,List<?>> annotationsPfMap = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param methodInfo The method wrapper.
	 * @param index The parameter index.
	 */
	protected MethodParamInfo(MethodInfo methodInfo, int index) {
		this.methodInfo = methodInfo;
		this.index = index;
	}

	/**
	 * Returns the index position of this parameter.
	 *
	 * @return The index position of this parameter.
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * Returns the method that this parameter belongs to.
	 *
	 * @return The method that this parameter belongs to.
	 */
	public Method getMethod() {
		return methodInfo.getInner();
	}

	/**
	 * Returns the class type of this parameter.
	 *
	 * @return The class type of this parameter.
	 */
	public Class<?> getParameterType() {
		return getMethod().getParameterTypes()[index];
	}

	/**
	 * Returns the generic class type of this parameter.
	 *
	 * @return The generic class type of htis parameter.
	 */
	public Type getGenericParameterType() {
		return methodInfo.getInner().getGenericParameterTypes()[index];
	}

	/**
	 * Returns the parameter annotations defined on this parameter.
	 *
	 * @return The parameter annotations defined on this parameter.
	 */
	public Annotation[] getParameterAnnotations() {
		return methodInfo.getInner().getParameterAnnotations()[index];
	}

	/**
	 * Finds the annotation of the specified type defined on this method parameter.
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
	 * Returns <jk>true</jk> if this parameter has the specified annotation.
	 *
	 * @param a
	 * 	The annotation to search for.
	 * @return
	 * 	The <jk>true</jk> if annotation if found.
	 */
	public boolean hasAnnotation(Class<? extends Annotation> a) {
		return getAnnotation(a) != null;
	}

	@SuppressWarnings("unchecked")
	private <T extends Annotation> T findAnnotation(Class<T> a) {
		List<Method> methods = methodInfo.getMatching();
		for (Method m2 : methods)
			for (Annotation a2 :  m2.getParameterAnnotations()[index])
				if (a.isInstance(a2))
					return (T)a2;
		Type t = methodInfo.getInner().getGenericParameterTypes()[index];
		if (Value.isType(t))
			return ClassInfo.lookup(Value.getParameterType(t)).getAnnotation(a);
		return ClassInfo.lookup(t).getAnnotation(a);
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

	@SuppressWarnings("unchecked")
	private <T extends Annotation> List<T> findAnnotations(Class<T> a) {
		List<T> l = new ArrayList<>();
		List<Method> methods = methodInfo.getMatching();
		for (Method m2 : methods)
			for (Annotation a2 :  m2.getParameterAnnotations()[index])
				if (a.isInstance(a2))
					l.add((T)a2);
		Type t = methodInfo.getInner().getGenericParameterTypes()[index];
		if (Value.isType(t))
			ClassUtils.appendAnnotations(a, Value.getParameterType(t), l);
		else
			ClassUtils.appendAnnotations(a, t, l);
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

	@Override
	public String toString() {
		return getMethod().getName() + "[" + index + "]";
	}
}
