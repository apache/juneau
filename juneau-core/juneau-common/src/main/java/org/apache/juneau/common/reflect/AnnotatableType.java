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
 * Enum representing the type of reflective object.
 *
 * <p>
 * Used to identify the specific type of reflective wrapper (ClassInfo, MethodInfo, FieldInfo, etc.)
 * for runtime dispatch and polymorphic handling.
 */
public enum AnnotatableType {
	/** Represents a {@link Class} wrapped in {@link ClassInfo}. */
	CLASS,
	/** Represents a {@link java.lang.reflect.Method} wrapped in {@link MethodInfo}. */
	METHOD,
	/** Represents a {@link java.lang.reflect.Field} wrapped in {@link FieldInfo}. */
	FIELD,
	/** Represents a {@link java.lang.Package} wrapped in {@link PackageInfo}. */
	PACKAGE,
	/** Represents a {@link java.lang.reflect.Constructor} wrapped in {@link ConstructorInfo}. */
	CONSTRUCTOR,
	/** Represents a {@link java.lang.reflect.Parameter} wrapped in {@link ParameterInfo}. */
	PARAMETER
}
