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
 * Identifies possible modifiers and attributes on classes, methods, fields, and constructors.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public enum ElementFlag {

	// Java modifiers from java.lang.reflect.Modifier

	/** PUBLIC modifier */
	PUBLIC,

	/** NOT_PUBLIC (negated) */
	NOT_PUBLIC,

	/** PRIVATE modifier */
	PRIVATE,

	/** NOT_PRIVATE (negated) */
	NOT_PRIVATE,

	/** PROTECTED modifier */
	PROTECTED,

	/** NOT_PROTECTED (negated) */
	NOT_PROTECTED,

	/** STATIC modifier */
	STATIC,

	/** NOT_STATIC (negated) */
	NOT_STATIC,

	/** FINAL modifier */
	FINAL,

	/** NOT_FINAL (negated) */
	NOT_FINAL,

	/** SYNCHRONIZED modifier */
	SYNCHRONIZED,

	/** NOT_SYNCHRONIZED (negated) */
	NOT_SYNCHRONIZED,

	/** VOLATILE modifier */
	VOLATILE,

	/** NOT_VOLATILE (negated) */
	NOT_VOLATILE,

	/** TRANSIENT modifier */
	TRANSIENT,

	/** NOT_TRANSIENT (negated) */
	NOT_TRANSIENT,

	/** NATIVE modifier */
	NATIVE,

	/** NOT_NATIVE (negated) */
	NOT_NATIVE,

	/** INTERFACE modifier */
	INTERFACE,

	/** ABSTRACT modifier */
	ABSTRACT,

	/** NOT_ABSTRACT (negated) */
	NOT_ABSTRACT,

	/** STRICT modifier */
	STRICT,

	/** NOT_STRICT (negated) */
	NOT_STRICT,

	// Non-modifier attributes

	/** ANNOTATION (is an annotation type) */
	ANNOTATION,

	/** NOT_ANNOTATION (not an annotation type) */
	NOT_ANNOTATION,

	/** ANONYMOUS (is an anonymous class) */
	ANONYMOUS,

	/** NOT_ANONYMOUS (not an anonymous class) */
	NOT_ANONYMOUS,

	/** ARRAY (is an array type) */
	ARRAY,

	/** NOT_ARRAY (not an array type) */
	NOT_ARRAY,

	/** BRIDGE (is a bridge method) */
	BRIDGE,

	/** NOT_BRIDGE (not a bridge method) */
	NOT_BRIDGE,

	/** CLASS (is a class, not an interface) */
	CLASS,

	/** CONSTRUCTOR (is a constructor) */
	CONSTRUCTOR,

	/** NOT_CONSTRUCTOR (not a constructor) */
	NOT_CONSTRUCTOR,

	/** DEFAULT (is a default interface method) */
	DEFAULT,

	/** NOT_DEFAULT (not a default interface method) */
	NOT_DEFAULT,

	/** DEPRECATED (has @Deprecated annotation) */
	DEPRECATED,

	/** NOT_DEPRECATED (no @Deprecated annotation) */
	NOT_DEPRECATED,

	/** ENUM (is an enum type) */
	ENUM,

	/** NOT_ENUM (not an enum type) */
	NOT_ENUM,

	/** ENUM_CONSTANT (is an enum constant field) */
	ENUM_CONSTANT,

	/** NOT_ENUM_CONSTANT (not an enum constant field) */
	NOT_ENUM_CONSTANT,

	/** HAS_PARAMS (has parameters) */
	HAS_PARAMS,

	/** HAS_NO_PARAMS (has no parameters) */
	HAS_NO_PARAMS,

	/** LOCAL (is a local class) */
	LOCAL,

	/** NOT_LOCAL (not a local class) */
	NOT_LOCAL,

	/** MEMBER (is a member class) */
	MEMBER,

	/** NOT_MEMBER (not a member class) */
	NOT_MEMBER,

	/** NON_STATIC_MEMBER (is a non-static member class) */
	NON_STATIC_MEMBER,

	/** NOT_NON_STATIC_MEMBER (not a non-static member class) */
	NOT_NON_STATIC_MEMBER,

	/** PRIMITIVE (is a primitive type) */
	PRIMITIVE,

	/** NOT_PRIMITIVE (not a primitive type) */
	NOT_PRIMITIVE,

	/** RECORD (is a record type) */
	RECORD,

	/** NOT_RECORD (not a record type) */
	NOT_RECORD,

	/** SEALED (is a sealed class) */
	SEALED,

	/** NOT_SEALED (not a sealed class) */
	NOT_SEALED,

	/** SYNTHETIC (is compiler-generated) */
	SYNTHETIC,

	/** NOT_SYNTHETIC (not compiler-generated) */
	NOT_SYNTHETIC,

	/** VARARGS (has variable arity) */
	VARARGS,

	/** NOT_VARARGS (does not have variable arity) */
	NOT_VARARGS
}