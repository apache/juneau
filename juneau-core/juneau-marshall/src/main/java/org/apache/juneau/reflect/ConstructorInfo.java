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

import static org.apache.juneau.internal.CollectionUtils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;

/**
 * Lightweight utility class for introspecting information about a constructor.
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@source}
 * </ul>
 */
@FluentSetters
public final class ConstructorInfo extends ExecutableInfo implements Comparable<ConstructorInfo> {

	private final Constructor<?> c;

	//-----------------------------------------------------------------------------------------------------------------
	// Instantiation
	//-----------------------------------------------------------------------------------------------------------------

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
	 * Convenience method for instantiating a {@link ConstructorInfo};
	 *
	 * @param declaringClass The class that declares this method.
	 * @param c The constructor being wrapped.
	 * @return A new {@link ConstructorInfo} object, or <jk>null</jk> if the method was null;
	 */
	public static ConstructorInfo of(ClassInfo declaringClass, Constructor<?> c) {
		if (c == null)
			return null;
		return new ConstructorInfo(declaringClass, c);
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
		return new ConstructorInfo(ClassInfo.of(c.getDeclaringClass()), c);
	}

	/**
	 * Returns the wrapped method.
	 *
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
	 * @param a
	 * 	The annotation to search for.
	 * @return
	 * 	The annotation if found, or <jk>null</jk> if not.
	 */
	public final <T extends Annotation> T getAnnotation(Class<T> a) {
		return getAnnotation(a, MetaProvider.DEFAULT);
	}

	/**
	 * Finds the annotation of the specified type defined on this constructor.
	 *
	 * @param a
	 * 	The annotation to search for.
	 * @param mp
	 * 	The meta provider for looking up annotations on classes/methods/fields.
	 * @return
	 * 	The first annotation found, or <jk>null</jk> if it doesn't exist.
	 */
	public final <T extends Annotation> T getAnnotation(Class<T> a, MetaProvider mp) {
		return last(mp.getAnnotations(a, c));
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation is present on this constructor.
	 *
	 * @param a The annotation to check for.
	 * @return <jk>true</jk> if the specified annotation is present on this constructor.
	 */
	public final boolean hasAnnotation(Class<? extends Annotation> a) {
		return getAnnotation(a) != null;
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Shortcut for calling the new-instance method on the underlying constructor.
	 *
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
