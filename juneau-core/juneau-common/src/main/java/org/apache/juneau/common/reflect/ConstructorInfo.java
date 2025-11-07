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
package org.apache.juneau.common.reflect;

import java.lang.reflect.*;

import org.apache.juneau.common.utils.*;

/**
 * Lightweight utility class for introspecting information about a constructor.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class ConstructorInfo extends ExecutableInfo implements Comparable<ConstructorInfo>, Annotatable {
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

	@Override /* Overridden from ExecutableInfo */
	public ConstructorInfo accessible() {
		super.accessible();
		return this;
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

	@Override
	public int compareTo(ConstructorInfo o) {
		int i = getSimpleName().compareTo(o.getSimpleName());
		if (i == 0) {
			i = getParameterCount() - o.getParameterCount();
			if (i == 0) {
				var params = getParameters();
				var oParams = o.getParameters();
				for (int j = 0; j < params.size() && i == 0; j++) {
					i = params.get(j).getParameterType().getName().compareTo(oParams.get(j).getParameterType().getName());
				}
			}
		}
		return i;
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

	/**
	 * Shortcut for calling the new-instance method on the underlying constructor.
	 *
	 * @param <T> The constructor class type.
	 * @param args the arguments used for the method call.
	 * @return The object returned from the constructor.
	 * @throws ExecutableException Exception occurred on invoked constructor/method/field.
	 */
	@SuppressWarnings("unchecked")
	public <T> T newInstance(Object...args) throws ExecutableException {
		try {
			return (T)c.newInstance(args);
		} catch (InvocationTargetException e) {
			throw new ExecutableException(e.getTargetException());
		} catch (Exception e) {
			throw new ExecutableException(e);
		}
	}

	/**
	 * Shortcut for calling the new-instance method on the underlying constructor using lenient argument matching.
	 *
	 * <p>
	 * Lenient matching allows arguments to be matched to parameters based on parameter types.
	 * <br>Arguments can be in any order.
	 * <br>Extra arguments are ignored.
	 * <br>Missing arguments are set to <jk>null</jk>.
	 *
	 * @param <T> The constructor class type.
	 * @param args The arguments used for the constructor call.
	 * @return The object returned from the constructor.
	 * @throws ExecutableException Exception occurred on invoked constructor/method/field.
	 */
	public <T> T newInstanceLenient(Object...args) throws ExecutableException {
		return newInstance(ClassUtils.getMatchingArgs(c.getParameterTypes(), args));
	}
	//-----------------------------------------------------------------------------------------------------------------
	// Annotatable interface methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Annotatable */
	public AnnotatableType getAnnotatableType() {
		return AnnotatableType.CONSTRUCTOR_TYPE;
	}

	@Override /* Annotatable */
	public ClassInfo getClassInfo() {
		return getDeclaringClass();
	}

	@Override /* Annotatable */
	public String getAnnotatableName() {
		return getShortName();
	}
}