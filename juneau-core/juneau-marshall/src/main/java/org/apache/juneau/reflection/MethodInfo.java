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
import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;

/**
 * Utility class for introspecting information about a method.
 */
public class MethodInfo {

	private final Method method;
	private final MethodParamInfo[] params;
	private List<Method> matching;
	private Map<Class<?>,Optional<Annotation>> annotationMap;
	private Map<Class<?>,List<?>> annotationsMap;
	private Map<Class<?>,List<?>> annotationsPfMap;
	private ClassInfo returnTypeInfo;
	private ClassInfo[] exceptionInfos;

	/**
	 * Constructor.
	 *
	 * @param m The method being wrapped.
	 */
	public MethodInfo(Method m) {
		this.method = m;
		params = new MethodParamInfo[m.getParameterCount()];
		for (int i = 0; i < m.getParameterCount(); i++)
			params[i] = new MethodParamInfo(this, i);
	}

	/**
	 * Returns the wrapped method.
	 *
	 * @return The wrapped method.
	 */
	public Method getInner() {
		return method;
	}

	/**
	 * Returns the parameters defined on this method.
	 *
	 * @return An array of parameter information, never <jk>null</jk>.
	 */
	public MethodParamInfo[] getParams() {
		return params;
	}

	/**
	 * Returns parameter information at the specified index.
	 *
	 * @param index The parameter index.
	 * @return The parameter information, never <jk>null</jk>.
	 */
	public MethodParamInfo getParam(int index) {
		return params[index];
	}

	/**
	 * Finds all declared methods with the same name and arguments on all superclasses and interfaces.
	 *
	 * @return
	 * 	All matching methods including this method itself.
	 * 	<br>Methods are ordered from child-to-parent order.
	 */
	public List<Method> getMatching() {
		if (matching == null)
			matching = Collections.unmodifiableList(findMatching(new ArrayList<>(), method, method.getDeclaringClass()));
		return matching;
	}

	private static List<Method> findMatching(List<Method> l, Method m, Class<?> c) {
		for (Method m2 : c.getDeclaredMethods())
			if (m.getName().equals(m2.getName()) && Arrays.equals(m.getParameterTypes(), m2.getParameterTypes()))
				l.add(m2);
		Class<?> pc = c.getSuperclass();
		if (pc != null)
			findMatching(l, m, pc);
		for (Class<?> ic : c.getInterfaces())
			findMatching(l, m, ic);
		return l;
	}

	/**
	 * Returns the {@link ClassInfo} object associated with the return type on this method.
	 *
	 * @return The {@link ClassInfo} object associated with the return type on this method.
	 */
	public synchronized ClassInfo getReturnTypeInfo() {
		if (returnTypeInfo == null)
			returnTypeInfo = ClassInfo.lookup(method.getReturnType());
		return returnTypeInfo;
	}

	/**
	 * Returns the {@link ClassInfo} objects associated with the exception types on this method.
	 *
	 * @return The {@link ClassInfo} objects associated with the exception types on this method.
	 */
	public synchronized ClassInfo[] getExceptionInfos() {
		if (exceptionInfos == null) {
			Class<?>[] exceptionTypes = method.getExceptionTypes();
			exceptionInfos = new ClassInfo[exceptionTypes.length];
			for (int i = 0; i < exceptionTypes.length; i++)
				exceptionInfos[i] = ClassInfo.lookup(exceptionTypes[i]);
		}
		return exceptionInfos;
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

	@SuppressWarnings("unchecked")
	private <T extends Annotation> T findAnnotation(Class<T> a) {
		List<Method> methods = getMatching();
		for (Method m2 : methods)
			for (Annotation a2 :  m2.getAnnotations())
				if (a.isInstance(a2))
					return (T)a2;
		Type t = method.getGenericReturnType();
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

	/**
	 * Asserts that the specified method that's annotated with the specified annotation cannot also be annotated with other annotations.
	 *
	 * @param a The annotation known to exist on the method.
	 * @param c The annotations that cannot be present on the method.
	 * @throws InvalidAnnotationException
	 */
	@SafeVarargs
	public final void assertNoAnnotations(Class<? extends Annotation> a, Class<? extends Annotation>...c) throws InvalidAnnotationException {
		for (Class<? extends Annotation> cc : c)
			if (hasAnnotation(cc))
				throw new InvalidAnnotationException("@{0} annotation cannot be used in a @{1} bean.  Method=''{2}''", cc.getSimpleName(), a.getSimpleName(), method);
	}

	@SuppressWarnings("unchecked")
	private <T extends Annotation> List<T> findAnnotations(Class<T> a) {
		List<T> l = new ArrayList<>();
		List<Method> methods = getMatching();
		for (Method m2 : methods)
			for (Annotation a2 :  m2.getAnnotations())
				if (a.isInstance(a2))
					l.add((T)a2);
		Type t = method.getGenericReturnType();
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
}
