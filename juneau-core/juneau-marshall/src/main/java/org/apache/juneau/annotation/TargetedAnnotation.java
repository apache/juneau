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
package org.apache.juneau.annotation;

import java.lang.annotation.*;
import java.lang.reflect.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.reflect.*;

/**
 * An implementation of an annotation that has an <code>on</code> value targeting classes/methods/fields/constructors.
 */
public class TargetedAnnotation {

	private String[] on = new String[0];

	/**
	 * The targets this annotation applies to.
	 *
	 * @return The targets this annotation applies to.
	 */
	public String[] on() {
		return on;
	}

	/**
	 * Appends the targets this annotation applies to.
	 *
	 * @param value The values to append.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public TargetedAnnotation on(String...value) {
		for (String v : value)
			on = ArrayUtils.append(on, v);
		return this;
	}

	/**
	 * Implements the {@link Annotation#annotationType()} method for child classes.
	 *
	 * @return This class.
	 */
	@SuppressWarnings("unchecked")
	public Class<? extends Annotation> annotationType() {
		return (Class<? extends Annotation>) getClass();
	}

	/**
	 * An implementation of an annotation that can be applied to classes.
	 */
	public static class OnClass extends TargetedAnnotation {

		private Class<?>[] onClass = new Class[0];

		/**
		 * The target classes this annotation applies to.
		 *
		 * @return The target classes this annotation applies to.
		 */
		public Class<?>[] onClass() {
			return onClass;
		}

		/**
		 * Appends the classes that this annotation applies to.
		 *
		 * @param value The values to append.
		 * @return This object (for method chaining).
		 */
		@SuppressWarnings("unchecked")
		@FluentSetter
		public TargetedAnnotation onClass(Class<?>...value) {
			for (Class<?> v : value)
				onClass = ArrayUtils.append(onClass, v);
			return this;
		}

		/**
		 * Appends the classes that this annotation applies to.
		 *
		 * @param value The values to append.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public TargetedAnnotation on(Class<?>...value) {
			for (Class<?> v : value)
				on(v.getName());
			return this;
		}
	}

	/**
	 * An implementation of an annotation that can be applied to classes, methods, and fields.
	 */
	public static class OnClassMethodField extends OnClass {

		/**
		 * Appends the methods that this annotation applies to.
		 *
		 * @param value The values to append.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public TargetedAnnotation on(Method...value) {
			for (Method v : value)
				on(MethodInfo.of(v).getFullName());
			return this;
		}

		/**
		 * Appends the fields that this annotation applies to.
		 *
		 * @param value The values to append.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public TargetedAnnotation on(Field...value) {
			for (Field v : value)
				on(v.getName());
			return this;
		}
	}

	/**
	 * An implementation of an annotation that can be applied to methods and fields.
	 */
	public static class OnMethodField extends TargetedAnnotation {

		/**
		 * Appends the methods that this annotation applies to.
		 *
		 * @param value The values to append.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public TargetedAnnotation on(Method...value) {
			for (Method v : value)
				on(MethodInfo.of(v).getFullName());
			return this;
		}

		/**
		 * Appends the fields that this annotation applies to.
		 *
		 * @param value The values to append.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public TargetedAnnotation on(Field...value) {
			for (Field v : value)
				on(v.getName());
			return this;
		}
	}

	/**
	 * An implementation of an annotation that can be applied to classes, methods, fields, and constructors.
	 */
	public static class OnClassMethodFieldConstructor extends OnClassMethodField {

		/**
		 * Appends the constructors that this annotation applies to.
		 *
		 * @param value The values to append.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public TargetedAnnotation on(Constructor<?>...value) {
			for (Constructor<?> v : value)
				on(ConstructorInfo.of(v).getFullName());
			return this;
		}
	}

	/**
	 * An implementation of an annotation that can be applied to constructors.
	 */
	public static class OnConstructor extends TargetedAnnotation {

		/**
		 * Appends the constructors that this annotation applies to.
		 *
		 * @param value The values to append.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public TargetedAnnotation on(Constructor<?>...value) {
			for (Constructor<?> v : value)
				on(ConstructorInfo.of(v).getFullName());
			return this;
		}
	}
}
