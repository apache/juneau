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

import static org.apache.juneau.internal.ClassUtils.*;

import java.lang.reflect.*;

/**
 * Defines class/field/method visibilities.
 *
 * <p>
 * Used to specify minimum levels of visibility when detecting bean classes, methods, and fields.
 *
 * <p>
 * Used in conjunction with the following bean context properties:
 * <ul class='javatree'>
 * 	<li class='jm'>{@link BeanContext.Builder#beanConstructorVisibility(Visibility)}
 * 	<li class='jm'>{@link BeanContext.Builder#beanClassVisibility(Visibility)}
 * 	<li class='jm'>{@link BeanContext.Builder#beanFieldVisibility(Visibility)}
 * 	<li class='jm'>{@link BeanContext.Builder#beanMethodVisibility(Visibility)}
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public enum Visibility {

	/** Ignore all */
	NONE,

	/** Include only <jk>public</jk> classes/fields/methods. */
	PUBLIC,

	/** Include only <jk>public</jk> or <jk>protected</jk> classes/fields/methods. */
	PROTECTED,

	/** Include all but <jk>private</jk> classes/fields/methods. */
	DEFAULT,

	/** Include all classes/fields/methods. */
	PRIVATE;

	/**
	 * Identifies if the specified mod matches this visibility.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jsf>PUBLIC</jsf>.isVisible(MyPublicClass.<jk>class</jk>.getModifiers()); <jc>//true</jc>
	 * 	<jsf>PUBLIC</jsf>.isVisible(MyPrivateClass.<jk>class</jk>.getModifiers()); <jc>//false</jc>
	 * 	<jsf>PRIVATE</jsf>.isVisible(MyPrivateClass.<jk>class</jk>.getModifiers()); <jc>//true</jc>
	 * 	<jsf>NONE</jsf>.isVisible(MyPublicClass.<jk>class</jk>.getModifiers()); <jc>//false</jc>
	 * </p>
	 *
	 * @param mod The modifier from the object being tested (e.g. results from {@link Class#getModifiers()}.
	 * @return <jk>true</jk> if this visibility matches the specified modifier attribute.
	 */
	public boolean isVisible(int mod) {
		switch(this) {
			case NONE: return false;
			case PRIVATE: return true;
			case DEFAULT: return ! Modifier.isPrivate(mod);
			case PROTECTED: return Modifier.isProtected(mod) || Modifier.isPublic(mod);
			default: return Modifier.isPublic(mod);
		}
	}

	/**
	 * Shortcut for <c>isVisible(x.getModifiers());</c>
	 *
	 * @param x The class to check.
	 * @return <jk>true</jk> if the class is at least as visible as this object.
	 */
	public boolean isVisible(Class<?> x) {
		return isVisible(x.getModifiers());
	}

	/**
	 * Shortcut for <c>isVisible(x.getModifiers());</c>
	 *
	 * @param x The constructor to check.
	 * @return <jk>true</jk> if the constructor is at least as visible as this object.
	 */
	public boolean isVisible(Executable x) {
		return isVisible(x.getModifiers());
	}

	/**
	 * Shortcut for <c>isVisible(x.getModifiers());</c>
	 *
	 * @param x The field to check.
	 * @return <jk>true</jk> if the field is at least as visible as this object.
	 */
	public boolean isVisible(Field x) {
		return isVisible(x.getModifiers());
	}

	/**
	 * Makes constructor accessible if it matches the visibility requirements, or returns <jk>null</jk> if it doesn't.
	 *
	 * <p>
	 * Security exceptions thrown on the call to {@link Constructor#setAccessible(boolean)} are quietly ignored.
	 *
	 * @param <T> The class type.
	 * @param x The constructor.
	 * @return
	 * 	The same constructor if visibility requirements met, or <jk>null</jk> if visibility requirement not
	 * 	met or call to {@link Constructor#setAccessible(boolean)} throws a security exception.
	 */
	public <T> Constructor<T> transform(Constructor<T> x) {
		if (x == null)
			return null;
		if (isVisible(x))
			if (! setAccessible(x))
				return null;
		return x;
	}

	/**
	 * Makes method accessible if it matches the visibility requirements, or returns <jk>null</jk> if it doesn't.
	 *
	 * <p>
	 * Security exceptions thrown on the call to {@link Method#setAccessible(boolean)} are quietly ignored.
	 *
	 * @param x The method.
	 * @return
	 * 	The same method if visibility requirements met, or <jk>null</jk> if visibility requirement not
	 * 	met or call to {@link Method#setAccessible(boolean)} throws a security exception.
	 */
	public Method transform(Method x) {
		if (x == null)
			return null;
		if (isVisible(x))
			if (! setAccessible(x))
				return null;
		return x;
	}

	/**
	 * Makes field accessible if it matches the visibility requirements, or returns <jk>null</jk> if it doesn't.
	 *
	 * <p>
	 * Security exceptions thrown on the call to {@link Field#setAccessible(boolean)} are quietly ignored.
	 *
	 * @param x The field.
	 * @return
	 * 	The same field if visibility requirements met, or <jk>null</jk> if visibility requirement not
	 * 	met or call to {@link Field#setAccessible(boolean)} throws a security exception.
	 */
	public Field transform(Field x) {
		if (x == null)
			return null;
		if (isVisible(x))
			if (! setAccessible(x))
				return null;
		return x;
	}
}