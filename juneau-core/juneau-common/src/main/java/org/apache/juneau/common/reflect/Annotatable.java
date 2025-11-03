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

/**
 * Interface for all annotatable wrapper classes.
 *
 * <p>
 * This interface provides a common type for all wrappers around Java reflection objects,
 * allowing polymorphic handling of different annotatable types (Class, Method, Field, Constructor, Parameter, Package).
 *
 * <p>
 * Implementers include:
 * <ul>
 * 	<li>{@link ClassInfo} - Wraps {@link Class}
 * 	<li>{@link MethodInfo} - Wraps {@link java.lang.reflect.Method}
 * 	<li>{@link FieldInfo} - Wraps {@link java.lang.reflect.Field}
 * 	<li>{@link ConstructorInfo} - Wraps {@link java.lang.reflect.Constructor}
 * 	<li>{@link ParameterInfo} - Wraps {@link java.lang.reflect.Parameter}
 * 	<li>{@link PackageInfo} - Wraps {@link java.lang.Package}
 * </ul>
 */
public interface Annotatable {

	/**
	 * Returns the type of this annotatable object.
	 *
	 * @return The type of annotatable object this represents.
	 */
	AnnotatableType getAnnotatableType();

	/**
	 * Returns the class info associated with this annotatable element.
	 *
	 * <p>
	 * Returns the declaring class from whichever context this annotatable belongs to.
	 *
	 * @return The class info, or <jk>null</jk> if this is a package.
	 */
	ClassInfo getClassInfo();

	/**
	 * Returns a human-readable name for this annotatable element.
	 *
	 * <p>
	 * The name format depends on the type of annotatable:
	 * <ul>
	 * 	<li>{@link AnnotatableType#CLASS CLASS} - Simple class name
	 * 	<li>{@link AnnotatableType#METHOD METHOD} - Short method name (with parameter types)
	 * 	<li>{@link AnnotatableType#FIELD FIELD} - Field name
	 * 	<li>{@link AnnotatableType#CONSTRUCTOR CONSTRUCTOR} - Short constructor name (with parameter types)
	 * 	<li>{@link AnnotatableType#PARAMETER PARAMETER} - Parameter name
	 * 	<li>{@link AnnotatableType#PACKAGE PACKAGE} - Package name
	 * </ul>
	 *
	 * @return The annotatable name.
	 */
	String getAnnotatableName();
}
