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
package org.apache.juneau.commons.reflect;

import java.lang.reflect.*;

/**
 * Interface for setting values on bean objects, supporting both method setters and direct field access.
 *
 * <p>
 * This interface provides a unified abstraction for setting values on objects, whether through
 * setter methods or direct field access. It's used by frameworks that need to set bean properties
 * without knowing whether the property uses a setter method or a public field.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>Unified interface - works with both setter methods and fields
 * 	<li>Exception handling - wraps reflection exceptions in {@link ExecutableException}
 * 	<li>Type-agnostic - works with any object and value type
 * </ul>
 *
 * <h5 class='section'>Use Cases:</h5>
 * <ul class='spaced-list'>
 * 	<li>Bean property setting in frameworks
 * 	<li>Data binding and mapping operations
 * 	<li>Dependency injection frameworks
 * 	<li>Object construction and initialization
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Create setter from method</jc>
 * 	Method <jv>setterMethod</jv> = MyClass.<jk>class</jk>.getMethod(<js>"setName"</js>, String.<jk>class</jk>);
 * 	Setter <jv>setter</jv> = <jk>new</jk> Setter.MethodSetter(<jv>setterMethod</jv>);
 *
 * 	<jc>// Create setter from field</jc>
 * 	Field <jv>field</jv> = MyClass.<jk>class</jk>.getField(<js>"name"</js>);
 * 	Setter <jv>fieldSetter</jv> = <jk>new</jk> Setter.FieldSetter(<jv>field</jv>);
 *
 * 	<jc>// Use setter</jc>
 * 	MyClass <jv>obj</jv> = <jk>new</jk> MyClass();
 * 	<jv>setter</jv>.set(<jv>obj</jv>, <js>"John"</js>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link ExecutableException} - Exception thrown by setters
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonReflect">juneau-common-reflect</a>
 * </ul>
 */
public interface Setter {

	/**
	 * Implementation of Setter that sets values directly on a field.
	 *
	 * <p>
	 * This implementation uses {@link Field#set(Object, Object)} to set field values directly,
	 * bypassing any setter methods. The field must be accessible (public or made accessible via
	 * {@link Field#setAccessible(boolean)}).
	 */
	static class FieldSetter implements Setter {

		private final Field f;

		public FieldSetter(Field f) {
			this.f = f;
		}

		@Override /* Overridden from Setter */
		public void set(Object object, Object value) throws ExecutableException {
			try {
				f.set(object, value);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				throw new ExecutableException(e);
			}
		}
	}

	/**
	 * Implementation of Setter that sets values by invoking a setter method.
	 *
	 * <p>
	 * This implementation uses {@link Method#invoke(Object, Object...)} to call a setter method
	 * on the target object. The method must be accessible (public or made accessible via
	 * {@link Method#setAccessible(boolean)}).
	 */
	static class MethodSetter implements Setter {

		private final Method m;

		public MethodSetter(Method m) {
			this.m = m;
		}

		@Override /* Overridden from Setter */
		public void set(Object object, Object value) throws ExecutableException {
			try {
				m.invoke(object, value);
			} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
				throw new ExecutableException(e);
			}
		}
	}

	/**
	 * Sets the value on the specified object using this setter.
	 *
	 * <p>
	 * For method setters, this invokes the setter method with the specified value.
	 * For field setters, this sets the field value directly.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Setter <jv>setter</jv> = ...;
	 * 	MyClass <jv>obj</jv> = <jk>new</jk> MyClass();
	 * 	<jv>setter</jv>.set(<jv>obj</jv>, <js>"value"</js>);
	 * </p>
	 *
	 * @param object The object on which to set the value. Must not be <jk>null</jk>.
	 * @param value The value to set. Can be <jk>null</jk>.
	 * @throws ExecutableException If an error occurs while setting the value (e.g., field/method not accessible,
	 *                             type mismatch, or invocation target exception).
	 */
	void set(Object object, Object value) throws ExecutableException;
}