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
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;

/**
 * Lightweight utility class for introspecting information about a constructor.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
@FluentSetters
public final class ConstructorInfo extends ExecutableInfo implements Comparable<ConstructorInfo> {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Convenience method for instantiating a {@link ConstructorInfo};
	 *
	 * @param declaringClass The class that declares this method.
	 * @param c The constructor being wrapped.
	 * @return A new {@link ConstructorInfo} object, or <jk>null</jk> if the method was null;
	 */
	public static ConstructorInfo of(ClassInfo declaringClass, Constructor<?> c) {
		if (c == null)
			return null;
		return ClassInfo.of(declaringClass).getConstructorInfo(c);
	}

	/**
	 * Convenience method for instantiating a {@link ConstructorInfo};
	 *
	 * @param c The constructor being wrapped.
	 * @return A new {@link ConstructorInfo} object, or <jk>null</jk> if the method was null;
	 */
	public static ConstructorInfo of(Constructor<?> c) {
		if (c == null)
			return null;
		return ClassInfo.of(c.getDeclaringClass()).getConstructorInfo(c);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final Constructor<?> c;

	/**
	 * Constructor.
	 *
	 * @param declaringClass The class that declares this method.
	 * @param c The constructor being wrapped.
	 */
	protected ConstructorInfo(ClassInfo declaringClass, Constructor<?> c) {
		super(declaringClass, c);
		this.c = c;
	}

	/**
	 * Returns the wrapped method.
	 *
	 * @param <T> The inner class type.
	 * @return The wrapped method.
	 */
	@SuppressWarnings("unchecked")
	public <T> Constructor<T> inner() {
		return (Constructor<T>)c;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotations
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Finds the annotation of the specified type defined on this constructor.
	 *
	 * @param <A> The annotation type to look for.
	 * @param type The annotation to look for.
	 * @return The annotation if found, or <jk>null</jk> if not.
	 */
	public final <A extends Annotation> A getAnnotation(Class<A> type) {
		return getAnnotation(AnnotationProvider.DEFAULT, type);
	}

	/**
	 * Finds the annotation of the specified type defined on this constructor.
	 *
	 * @param <A> The annotation type to look for.
	 * @param annotationProvider The annotation provider.
	 * @param type The annotation to look for.
	 * @return The first annotation found, or <jk>null</jk> if it doesn't exist.
	 */
	public final <A extends Annotation> A getAnnotation(AnnotationProvider annotationProvider, Class<A> type) {
		Value<A> t = Value.empty();
		annotationProvider.forEachAnnotation(type, c, x -> true, x -> t.set(x));
		return t.orElse(null);
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation is present on this constructor.
	 *
	 * @param <A> The annotation type to look for.
	 * @param type The annotation to look for.
	 * @return <jk>true</jk> if the specified annotation is present on this constructor.
	 */
	public final <A extends Annotation> boolean hasAnnotation(Class<A> type) {
		return hasAnnotation(AnnotationProvider.DEFAULT, type);
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation is present on this constructor.
	 *
	 * @param <A> The annotation type to look for.
	 * @param annotationProvider The annotation provider.
	 * @param type The annotation to look for.
	 * @return <jk>true</jk> if the specified annotation is present on this constructor.
	 */
	public final <A extends Annotation> boolean hasAnnotation(AnnotationProvider annotationProvider, Class<A> type) {
		return annotationProvider.firstAnnotation(type, c, x -> true) != null;
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation is not present on this constructor.
	 *
	 * @param <A> The annotation type to look for.
	 * @param annotationProvider The annotation provider.
	 * @param type The annotation to look for.
	 * @return <jk>true</jk> if the specified annotation is not present on this constructor.
	 */
	public final <A extends Annotation> boolean hasNoAnnotation(AnnotationProvider annotationProvider, Class<A> type) {
		return ! hasAnnotation(annotationProvider, type);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns <jk>true</jk> if this object passes the specified predicate test.
	 *
	 * @param test The test to perform.
	 * @return <jk>true</jk> if this object passes the specified predicate test.
	 */
	public boolean matches(Predicate<ConstructorInfo> test) {
		return test(test, this);
	}

	/**
	 * Performs an action on this object if the specified predicate test passes.
	 *
	 * @param test A test to apply to determine if action should be executed.  Can be <jk>null</jk>.
	 * @param action An action to perform on this object.
	 * @return This object.
	 */
	public ConstructorInfo accept(Predicate<ConstructorInfo> test, Consumer<ConstructorInfo> action) {
		if (matches(test))
			action.accept(this);
		return this;
	}

	/**
	 * Returns <jk>true</jk> if this constructor can accept the specified arguments in the specified order.
	 *
	 * @param args The arguments to check.
	 * @return <jk>true</jk> if this constructor can accept the specified arguments in the specified order.
	 */
	public boolean canAccept(Object...args) {
		Class<?>[] pt = c.getParameterTypes();
		if (pt.length != args.length)
			return false;
		for (int i = 0; i < pt.length; i++)
			if (! pt[i].isInstance(args[i]))
				return false;
		return true;
	}

	/**
	 * Shortcut for calling the new-instance method on the underlying constructor.
	 *
	 * @param <T> The constructor class type.
	 * @param args the arguments used for the method call.
	 * 	<br>Extra parameters are ignored.
	 * 	<br>Missing parameters are set to null.
	 * @return The object returned from the constructor.
	 * @throws ExecutableException Exception occurred on invoked constructor/method/field.
	 */
	public <T> T invokeFuzzy(Object...args) throws ExecutableException {
		return invoke(ClassUtils.getMatchingArgs(c.getParameterTypes(), args));
	}

	/**
	 * Shortcut for calling the new-instance method on the underlying constructor.
	 *
	 * @param <T> The constructor class type.
	 * @param args the arguments used for the method call.
	 * @return The object returned from the constructor.
	 * @throws ExecutableException Exception occurred on invoked constructor/method/field.
	 */
	@SuppressWarnings("unchecked")
	public <T> T invoke(Object...args) throws ExecutableException {
		try {
			return (T)c.newInstance(args);
		} catch (InvocationTargetException e) {
			throw new ExecutableException(e.getTargetException());
		} catch (Exception e) {
			throw new ExecutableException(e);
		}
	}

	/**
	 * Makes constructor accessible if it matches the visibility requirements, or returns <jk>null</jk> if it doesn't.
	 *
	 * <p>
	 * Security exceptions thrown on the call to {@link Constructor#setAccessible(boolean)} are quietly ignored.
	 *
	 * @param v The minimum visibility.
	 * @return
	 * 	The same constructor if visibility requirements met, or <jk>null</jk> if visibility requirement not
	 * 	met or call to {@link Constructor#setAccessible(boolean)} throws a security exception.
	 */
	public ConstructorInfo accessible(Visibility v) {
		if (v.transform(c) == null)
			return null;
		return this;
	}

	@Override
	public int compareTo(ConstructorInfo o) {
		int i = getSimpleName().compareTo(o.getSimpleName());
		if (i == 0) {
			i = getParamCount() - o.getParamCount();
			if (i == 0) {
				for (int j = 0; j < getParamCount() && i == 0; j++) {
					Class<?>[] tpt = _getRawParamTypes(), opt = o._getRawParamTypes();
					i = tpt[j].getName().compareTo(opt[j].getName());
				}
			}
		}
		return i;
	}

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.reflect.ExecutableInfo */
	public ConstructorInfo accessible() {
		super.accessible();
		return this;
	}

	// </FluentSetters>
}
