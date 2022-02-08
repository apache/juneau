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
package org.apache.juneau.reflect;

import static org.apache.juneau.internal.ConsumerUtils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;

/**
 * Lightweight utility class for introspecting information about a method parameter.
 * {@review}
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public final class ParamInfo {

	private final ExecutableInfo eInfo;
	private final Parameter p;
	private final int index;
	private volatile Map<Class<?>,Optional<Annotation>> annotationMap;

	//-----------------------------------------------------------------------------------------------------------------
	// Instantiation
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 *
	 * @param eInfo The constructor or method wrapper.
	 * @param p The parameter being wrapped.
	 * @param index The parameter index.
	 */
	protected ParamInfo(ExecutableInfo eInfo, Parameter p, int index) {
		this.eInfo = eInfo;
		this.p = p;
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
	 * @return The method that this parameter belongs to, or <jk>null</jk> if it belongs to a constructor.
	 */
	public MethodInfo getMethod() {
		return eInfo.isConstructor() ? null : (MethodInfo)eInfo;
	}

	/**
	 * Returns the constructor that this parameter belongs to.
	 *
	 * @return The constructor that this parameter belongs to, or <jk>null</jk> if it belongs to a method.
	 */
	public ConstructorInfo getConstructor() {
		return eInfo.isConstructor() ? (ConstructorInfo)eInfo : null;
	}

	/**
	 * Returns the class type of this parameter.
	 *
	 * @return The class type of this parameter.
	 */
	public ClassInfo getParameterType() {
		return eInfo.getParamType(index);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotations
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Consumers the matching parameter annotations declared on this parameter.
	 *
	 * @param type The annotation type.
	 * @param predicate The predicate.
	 * @param consumer The consumer.
	 * @return This object.
	 */
	public <A extends Annotation> ParamInfo getDeclaredAnnotations(Class<A> type, Predicate<A> predicate, Consumer<A> consumer) {
		for (Annotation a : eInfo.getParameterAnnotations(index))
			consume(type, predicate, consumer, a);
		return this;
	}

	/**
	 * Returns the specified parameter annotation declared on this parameter.
	 *
	 * @param type
	 * 	The annotation to look for.
	 * @param <A>
	 * 	The annotation type.
	 * @return The specified parameter annotation declared on this parameter, or <jk>null</jk> if not found.
	 */
	public <A extends Annotation> A getDeclaredAnnotation(Class<A> type) {
		if (type != null)
			for (Annotation a : eInfo.getParameterAnnotations(index))
				if (type.isInstance(a))
					return type.cast(a);
		return null;
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
	 * @param type
	 * 	The annotation to look for.
	 * @return
	 * 	The annotation if found, or <jk>null</jk> if not.
	 */
	@SuppressWarnings("unchecked")
	public <A extends Annotation> A getAnnotation(Class<A> type) {
		Optional<Annotation> o = annotationMap().get(type);
		if (o == null) {
			o = Optional.ofNullable(findAnnotation(type));
			annotationMap().put(type, o);
		}
		return o.isPresent() ? (A)o.get() : null;
	}

	/**
	 * Returns <jk>true</jk> if this parameter has the specified annotation.
	 *
	 * @param type
	 * 	The annotation to look for.
	 * @return
	 * 	The <jk>true</jk> if annotation if found.
	 */
	public <A extends Annotation> boolean hasAnnotation(Class<A> type) {
		return getAnnotation(type) != null;
	}

	private <A extends Annotation> A findAnnotation(Class<A> type) {
		if (eInfo.isConstructor()) {
			for (Annotation a2 : eInfo.getParameterAnnotations(index))
				if (type.isInstance(a2))
					return type.cast(a2);
			return eInfo.getParamType(index).unwrap(Value.class,Optional.class).getAnnotation(type);
		}
		MethodInfo mi = (MethodInfo)eInfo;
		Value<A> v = Value.empty();
		mi.getMatchingParentFirst(x -> true, x -> x.getParameterAnnotations(index, type, y -> true, y -> v.set(y)));
		return v.orElseGet(() -> eInfo.getParamType(index).unwrap(Value.class,Optional.class).getAnnotation(type));
	}

	/**
	 * Consumes all matching annotations of the specified type defined on this parameter.
	 *
	 * <p>
	 * Searches all methods with the same signature on the parent classes or interfaces
	 * and the return type on the method.
	 * <p>
	 * Results are in parent-to-child order.
	 *
	 * @param type The annotation to look for.
	 * @param predicate The predicate.
	 * @param consumer The consumer.
	 * @return This object.
	 */
	public <A extends Annotation> ParamInfo getAnnotations(Class<A> type, Predicate<A> predicate, Consumer<A> consumer) {
		return getAnnotations(AnnotationProvider.DEFAULT, type, predicate, consumer);
	}

	/**
	 * Returns the first annotation of the specified type defined on this method parameter that matches the specified predicate.
	 *
	 * <p>
	 * Searches all methods with the same signature on the parent classes or interfaces
	 * and the return type on the method.
	 * <p>
	 * Results are in parent-to-child order.
	 *
	 * @param type The annotation to look for.
	 * @param predicate The predicate.
	 * @return A list of all matching annotations found or an empty list if none found.
	 */
	@SuppressWarnings("unchecked")
	public <A extends Annotation> A getAnnotation(Class<A> type, Predicate<A> predicate) {
		if (eInfo.isConstructor) {
			ClassInfo ci = eInfo.getParamType(index).unwrap(Value.class,Optional.class);
			A o = ci.getAnnotation(type, predicate);
			if (o != null)
				return o;
			for (Annotation a2 : eInfo.getParameterAnnotations(index))
				if (passes(type, predicate, a2))
					return (A)a2;
		} else {
			MethodInfo mi = (MethodInfo)eInfo;
			ClassInfo ci = eInfo.getParamType(index).unwrap(Value.class,Optional.class);
			A o = ci.getAnnotation(type, predicate);
			if (o != null)
				return o;
			Value<A> v = Value.empty();
			mi.getMatchingParentFirst(x -> true, x -> x.getParameterAnnotations(index, type, predicate, y -> v.set(y)));
			return v.orElse(null);
		}
		return null;
	}

	private <A extends Annotation> ParamInfo getAnnotations(AnnotationProvider ap, Class<A> a, Predicate<A> predicate, Consumer<A> consumer) {
		if (eInfo.isConstructor) {
			ClassInfo ci = eInfo.getParamType(index).unwrap(Value.class,Optional.class);
			Annotation[] annotations = eInfo.getParameterAnnotations(index);
			ci.getAnnotations(ap, a, predicate, consumer);
			for (Annotation a2 : annotations)
				consume(a, predicate, consumer, a2);
		} else {
			MethodInfo mi = (MethodInfo)eInfo;
			ClassInfo ci = eInfo.getParamType(index).unwrap(Value.class,Optional.class);
			ci.getAnnotations(ap, a, predicate, consumer);
			mi.getMatchingParentFirst(x -> true, x -> x.getParameterAnnotations(index, a, predicate, consumer));
		}
		return this;
	}

	private synchronized Map<Class<?>,Optional<Annotation>> annotationMap() {
		if (annotationMap == null) {
			synchronized(this) {
				annotationMap = new ConcurrentHashMap<>();
			}
		}
		return annotationMap;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns <jk>true</jk> if this object passes the specified predicate test.
	 *
	 * @param predicate The predicate.
	 * @return <jk>true</jk> if this object passes the specified predicate test.
	 */
	public boolean matches(Predicate<ParamInfo> predicate) {
		return passes(predicate, this);
	}

	/**
	 * Consumes this object if the specified predicate test passes.
	 *
	 * @param predicate The predicate.
	 * @param consumer The consumer.
	 * @return This object.
	 */
	public ParamInfo accept(Predicate<ParamInfo> predicate, Consumer<ParamInfo> consumer) {
		if (matches(predicate))
			consumer.accept(this);
		return this;
	}

	/**
	 * Returns <jk>true</jk> if the parameter type is an exact match for the specified class.
	 *
	 * @param c The type to check.
	 * @return <jk>true</jk> if the parameter type is an exact match for the specified class.
	 */
	public boolean isType(Class<?> c) {
		return getParameterType().is(c);
	}

	/**
	 * Returns <jk>true</jk> if the parameter has a name provided by the class file.
	 *
	 * @return <jk>true</jk> if the parameter has a name provided by the class file.
	 */
	public boolean hasName() {
		return p.isNamePresent() || p.isAnnotationPresent(Name.class);
	}

	/**
	 * Returns the name of the parameter.
	 *
	 * <p>
	 * If the parameter's name is present, then this method returns the name provided by the class file.
	 * Otherwise, this method synthesizes a name of the form argN, where N is the index of the parameter in the descriptor of the method which declares the parameter.
	 *
	 * @return The name of the parameter.
	 * @see Parameter#getName()
	 */
	public String getName() {
		Name n = p.getAnnotation(Name.class);
		if (n != null)
			return n.value();
		if (p.isNamePresent())
			return p.getName();
		return null;
	}

	/**
	 * Returns <jk>true</jk> if this parameter can accept the specified value.
	 *
	 * @param value The value to check.
	 * @return <jk>true</jk> if this parameter can accept the specified value.
	 */
	public boolean canAccept(Object value) {
		return getParameterType().isInstance(value);
	}

	@Override
	public String toString() {
		return (eInfo.getSimpleName()) + "[" + index + "]";
	}
}
