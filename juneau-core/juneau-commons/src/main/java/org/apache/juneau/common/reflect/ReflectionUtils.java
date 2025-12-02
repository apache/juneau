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

/**
 * Utility class providing convenient static methods for creating reflection info objects.
 *
 * <p>
 * This class provides static factory methods that convert standard Java reflection objects
 * ({@link Class}, {@link Method}, {@link Field}, {@link Constructor}) to their corresponding
 * info wrapper objects ({@link ClassInfo}, {@link MethodInfo}, {@link FieldInfo}, {@link ConstructorInfo}).
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>Convenient factory methods - convert reflection objects to info wrappers
 * 	<li>Null-safe - methods handle <jk>null</jk> inputs gracefully
 * 	<li>Unified API - consistent method naming across all reflection types
 * </ul>
 *
 * <h5 class='section'>Use Cases:</h5>
 * <ul class='spaced-list'>
 * 	<li>Converting reflection objects to info wrappers in a consistent way
 * 	<li>Simplifying code that works with both reflection and info objects
 * 	<li>Providing a centralized location for reflection-to-info conversions
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Convert Class to ClassInfo</jc>
 * 	ClassInfo <jv>ci</jv> = ReflectionUtils.<jsm>info</jsm>(MyClass.<jk>class</jk>);
 *
 * 	<jc>// Convert Method to MethodInfo</jc>
 * 	Method <jv>m</jv> = MyClass.<jk>class</jk>.getMethod(<js>"myMethod"</js>);
 * 	MethodInfo <jv>mi</jv> = ReflectionUtils.<jsm>info</jsm>(<jv>m</jv>);
 *
 * 	<jc>// Convert Field to FieldInfo</jc>
 * 	Field <jv>f</jv> = MyClass.<jk>class</jk>.getField(<js>"myField"</js>);
 * 	FieldInfo <jv>fi</jv> = ReflectionUtils.<jsm>info</jsm>(<jv>f</jv>);
 *
 * 	<jc>// Convert Constructor to ConstructorInfo</jc>
 * 	Constructor&lt;?&gt; <jv>c</jv> = MyClass.<jk>class</jk>.getConstructor();
 * 	ConstructorInfo <jv>ci2</jv> = ReflectionUtils.<jsm>info</jsm>(<jv>c</jv>);
 * </p>
 *
 * <h5 class='section'>Null Handling:</h5>
 * <p>
 * All methods in this class handle <jk>null</jk> inputs gracefully, returning <jk>null</jk> if the
 * input is <jk>null</jk>. This makes it safe to use in scenarios where reflection objects may be <jk>null</jk>.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link ClassInfo} - Class introspection wrapper
 * 	<li class='jc'>{@link MethodInfo} - Method introspection wrapper
 * 	<li class='jc'>{@link FieldInfo} - Field introspection wrapper
 * 	<li class='jc'>{@link ConstructorInfo} - Constructor introspection wrapper
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonReflect">juneau-common-reflect</a>
 * </ul>
 */
public class ReflectionUtils {

	/**
	 * Returns the {@link ClassInfo} wrapper for the specified class.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	ClassInfo <jv>ci</jv> = ReflectionUtils.<jsm>info</jsm>(MyClass.<jk>class</jk>);
	 * </p>
	 *
	 * @param o The class to wrap. Can be <jk>null</jk>.
	 * @return The {@link ClassInfo} wrapper, or <jk>null</jk> if the input is <jk>null</jk>.
	 */
	public static final ClassInfo info(Class<?> o) {
		return ClassInfo.of(o);
	}

	/**
	 * Returns the {@link ConstructorInfo} wrapper for the specified constructor.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Constructor&lt;?&gt; <jv>c</jv> = MyClass.<jk>class</jk>.getConstructor();
	 * 	ConstructorInfo <jv>ci</jv> = ReflectionUtils.<jsm>info</jsm>(<jv>c</jv>);
	 * </p>
	 *
	 * @param o The constructor to wrap. Can be <jk>null</jk>.
	 * @return The {@link ConstructorInfo} wrapper, or <jk>null</jk> if the input is <jk>null</jk>.
	 */
	public static final ConstructorInfo info(Constructor<?> o) {
		return ConstructorInfo.of(o);
	}

	/**
	 * Returns the {@link FieldInfo} wrapper for the specified field.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Field <jv>f</jv> = MyClass.<jk>class</jk>.getField(<js>"myField"</js>);
	 * 	FieldInfo <jv>fi</jv> = ReflectionUtils.<jsm>info</jsm>(<jv>f</jv>);
	 * </p>
	 *
	 * @param o The field to wrap. Can be <jk>null</jk>.
	 * @return The {@link FieldInfo} wrapper, or <jk>null</jk> if the input is <jk>null</jk>.
	 */
	public static final FieldInfo info(Field o) {
		return FieldInfo.of(o);
	}

	/**
	 * Returns the {@link MethodInfo} wrapper for the specified method.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Method <jv>m</jv> = MyClass.<jk>class</jk>.getMethod(<js>"myMethod"</js>);
	 * 	MethodInfo <jv>mi</jv> = ReflectionUtils.<jsm>info</jsm>(<jv>m</jv>);
	 * </p>
	 *
	 * @param o The method to wrap. Can be <jk>null</jk>.
	 * @return The {@link MethodInfo} wrapper, or <jk>null</jk> if the input is <jk>null</jk>.
	 */
	public static final MethodInfo info(Method o) {
		return MethodInfo.of(o);
	}

	/**
	 * Returns the {@link ClassInfo} wrapper for the class of the specified object.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	MyClass <jv>obj</jv> = <jk>new</jk> MyClass();
	 * 	ClassInfo <jv>ci</jv> = ReflectionUtils.<jsm>info</jsm>(<jv>obj</jv>);
	 * </p>
	 *
	 * @param o The object whose class to wrap. Can be <jk>null</jk>.
	 * @return The {@link ClassInfo} wrapper for the object's class, or <jk>null</jk> if the input is <jk>null</jk>.
	 */
	public static final ClassInfo info(Object o) {
		return ClassInfo.of(o);
	}
}
