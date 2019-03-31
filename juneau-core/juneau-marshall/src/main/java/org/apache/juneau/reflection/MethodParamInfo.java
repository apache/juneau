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

import static org.apache.juneau.internal.CollectionUtils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.annotation.*;

/**
 * Lightweight utility class for introspecting information about a method parameter.
 */
@BeanIgnore
public final class MethodParamInfo {

	private final MethodInfo methodInfo;
	private final ConstructorInfo constructorInfo;
	private final boolean isConstructor;
	private int index;
	private Map<Class<?>,Optional<Annotation>> annotationMap = new ConcurrentHashMap<>();

	//-----------------------------------------------------------------------------------------------------------------
	// Instantiation.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 *
	 * @param methodInfo The method wrapper.
	 * @param index The parameter index.
	 */
	protected MethodParamInfo(MethodInfo methodInfo, int index) {
		this.methodInfo = methodInfo;
		this.constructorInfo = null;
		this.isConstructor = false;
		this.index = index;
	}

	/**
	 * Constructor.
	 *
	 * @param constructorInfo The constructor wrapper.
	 * @param index The parameter index.
	 */
	protected MethodParamInfo(ConstructorInfo constructorInfo, int index) {
		this.methodInfo = null;
		this.constructorInfo = constructorInfo;
		this.isConstructor = true;
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
		return isConstructor ? null : methodInfo.inner();
	}

	/**
	 * Returns the method that this parameter belongs to.
	 *
	 * @return The method that this parameter belongs to.
	 */
	public Constructor<?> getConstructor() {
		return isConstructor ? constructorInfo.inner() : null;
	}

	/**
	 * Returns the class type of this parameter.
	 *
	 * @return The class type of this parameter.
	 */
	public Class<?> getParameterType() {
		return isConstructor ? constructorInfo.getParameterType(index).inner() : methodInfo.getParameterType(index).inner();
	}

	/**
	 * Returns the generic class type of this parameter.
	 *
	 * @return The generic class type of this parameter.
	 */
	public Type getGenericParameterType() {
		return isConstructor ? constructorInfo.getParameterType(index).innerType() : methodInfo.getParameterType(index).innerType();
	}

	/**
	 * Returns the generic class type of this parameter.
	 *
	 * @return The generic class type of this parameter.
	 */
	public ClassInfo getGenericParameterTypeInfo() {
		return isConstructor ? constructorInfo.getParameterType(index) : methodInfo.getParameterType(index);
	}

	/**
	 * Returns the parameter annotations defined on this parameter.
	 *
	 * @return The parameter annotations defined on this parameter.
	 */
	public Annotation[] getParameterAnnotations() {
		return isConstructor ? constructorInfo.getParameterAnnotations(index) : methodInfo.getParameterAnnotations(index);
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
		if (isConstructor) {
			for (Annotation a2 : constructorInfo.getParameterAnnotations(index))
				if (a.isInstance(a2))
					return (T)a2;
			return constructorInfo.getParameterType(index).resolved().getAnnotation(a);
		}
		for (Method m2 : methodInfo.getMatching())
			for (Annotation a2 :  m2.getParameterAnnotations()[index])
				if (a.isInstance(a2))
					return (T)a2;
		return methodInfo.getParameterType(index).resolved().getAnnotation(a);
	}

	/**
	 * Returns all annotations of the specified type defined on this method parameter.
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
	public <T extends Annotation> List<T> getAnnotations(Class<T> a) {
		return getAnnotations(a, false);
	}

	/**
	 * Identical to {@link #getAnnotations(Class)} but optionally returns the list in reverse (parent-to-child) order.
	 *
	 * @param a
	 * 	The annotation to search for.
	 * @param parentFirst If <jk>true</jk>, results are in parent-to-child order.
	 * @return
	 * 	A list of all matching annotations found or an empty list if none found.
	 */
	public <T extends Annotation> List<T> getAnnotations(Class<T> a, boolean parentFirst) {
		return appendAnnotations(new ArrayList<>(), a, parentFirst);
	}

	/**
	 * Finds and appends the specified annotation on the specified method and methods onsuperclasses/interfaces to the specified
	 * list.
	 *
	 * <p>
	 * Results are ordered in child-to-parent order.
	 *
	 * @param l The list of annotations.
	 * @param a The annotation.
	 * @return The same list.
	 */
	public <T extends Annotation> List<T> appendAnnotations(List<T> l, Class<T> a) {
		return appendAnnotations(l, a, false);
	}

	/**
	 * Finds and appends the specified annotation on the specified class and superclasses/interfaces to the specified
	 * list.
	 *
	 * @param l The list of annotations.
	 * @param a The annotation.
	 * @param parentFirst If <jk>true</jk>, results are ordered in parent-to-child order.
	 * @return The same list.
	 */
	@SuppressWarnings("unchecked")
	public <T extends Annotation> List<T> appendAnnotations(List<T> l, Class<T> a, boolean parentFirst) {
		if (isConstructor) {
			ClassInfo ci = constructorInfo.getParameterType(index).resolved();
			Annotation[] annotations = constructorInfo.getParameterAnnotations(index);
			if (parentFirst) {
				ci.appendAnnotationsParentFirst(l, a);
				for (Annotation a2 : annotations)
					if (a.isInstance(a2))
						l.add((T)a2);
			} else {
				for (Annotation a2 : annotations)
					if (a.isInstance(a2))
						l.add((T)a2);
				ci.appendAnnotations(l, a);
			}
		} else {
			List<Method> methods = methodInfo.getMatching();
			ClassInfo ci = methodInfo.getParameterType(index).resolved();
			if (parentFirst) {
				ci.appendAnnotationsParentFirst(l, a);
				for (Method m2 : iterable(methods, true))
					for (Annotation a2 :  m2.getParameterAnnotations()[index])
						if (a.isInstance(a2))
							l.add((T)a2);
			} else {
				for (Method m2 : methods)
					for (Annotation a2 :  m2.getParameterAnnotations()[index])
						if (a.isInstance(a2))
							l.add((T)a2);
				ci.appendAnnotations(l, a);
			}
		}
		return l;
	}

	private synchronized Map<Class<?>,Optional<Annotation>> annotationMap() {
		if (annotationMap == null)
			annotationMap = new ConcurrentHashMap<>();
		return annotationMap;
	}

	@Override
	public String toString() {
		return (isConstructor ? getConstructor().getName() : getMethod().getName()) + "[" + index + "]";
	}
}
