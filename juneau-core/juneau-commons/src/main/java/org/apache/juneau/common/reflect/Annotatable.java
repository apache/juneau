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
	 * Returns a human-readable label for this annotatable element.
	 *
	 * <p>
	 * The label format depends on the type of annotatable:
	 * <ul>
	 * 	<li>{@link AnnotatableType#CLASS CLASS} - Simple class name (e.g., <js>"MyClass"</js>)
	 * 	<li>{@link AnnotatableType#METHOD METHOD} - Class and method with parameter types (e.g., <js>"MyClass.myMethod(String,int)"</js>)
	 * 	<li>{@link AnnotatableType#FIELD FIELD} - Class and field name (e.g., <js>"MyClass.myField"</js>)
	 * 	<li>{@link AnnotatableType#CONSTRUCTOR CONSTRUCTOR} - Class and constructor with parameter types (e.g., <js>"MyClass.MyClass(String)"</js>)
	 * 	<li>{@link AnnotatableType#PARAMETER PARAMETER} - Class, method/constructor, and parameter index (e.g., <js>"MyClass.myMethod[0]"</js>)
	 * 	<li>{@link AnnotatableType#PACKAGE PACKAGE} - Package name (e.g., <js>"com.example.package"</js>)
	 * </ul>
	 *
	 * @return The human-readable label for this annotatable element.
	 */
	String getLabel();
}
