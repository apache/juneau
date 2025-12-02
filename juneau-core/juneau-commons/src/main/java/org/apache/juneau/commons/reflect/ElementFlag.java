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

/**
 * Enumeration of possible modifiers and attributes that can be present on classes, methods, fields, and constructors.
 *
 * <p>
 * This enum provides a comprehensive set of flags for identifying Java language modifiers (public, private, static, etc.)
 * and other attributes (synthetic, deprecated, etc.) that can be present on program elements. Each modifier has both
 * a positive flag (e.g., <c>PUBLIC</c>) and a negated flag (e.g., <c>NOT_PUBLIC</c>) for convenient filtering.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>Java modifiers - all standard Java modifiers (public, private, protected, static, final, etc.)
 * 	<li>Negated flags - each modifier has a corresponding NOT_* flag for filtering
 * 	<li>Non-modifier attributes - flags for synthetic, deprecated, bridge methods, etc.
 * 	<li>Type attributes - flags for identifying classes, interfaces, enums, records, annotations
 * </ul>
 *
 * <h5 class='section'>Use Cases:</h5>
 * <ul class='spaced-list'>
 * 	<li>Filtering classes, methods, fields by modifiers
 * 	<li>Identifying special attributes (synthetic, deprecated, bridge methods)
 * 	<li>Type checking (enum, record, annotation, interface)
 * 	<li>Building frameworks that need to analyze program element characteristics
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Check if a class is public</jc>
 * 	ClassInfo <jv>ci</jv> = ClassInfo.<jsm>of</jsm>(MyClass.<jk>class</jk>);
 * 	<jk>boolean</jk> <jv>isPublic</jv> = <jv>ci</jv>.hasFlag(ElementFlag.PUBLIC);
 *
 * 	<jc>// Filter methods by modifier</jc>
 * 	List&lt;MethodInfo&gt; <jv>staticMethods</jv> = <jv>ci</jv>.getMethods()
 * 		.stream()
 * 		.filter(<jv>m</jv> -&gt; <jv>m</jv>.hasFlag(ElementFlag.STATIC))
 * 		.toList();
 *
 * 	<jc>// Check for deprecated methods</jc>
 * 	<jk>boolean</jk> <jv>isDeprecated</jv> = <jv>method</jv>.hasFlag(ElementFlag.DEPRECATED);
 * </p>
 *
 * <h5 class='section'>Modifier Flags:</h5>
 * <p>
 * Standard Java modifiers: <c>PUBLIC</c>, <c>PRIVATE</c>, <c>PROTECTED</c>, <c>STATIC</c>, <c>FINAL</c>,
 * <c>SYNCHRONIZED</c>, <c>VOLATILE</c>, <c>TRANSIENT</c>, <c>NATIVE</c>, <c>ABSTRACT</c>, <c>STRICT</c>.
 * Each has a corresponding <c>NOT_*</c> flag.
 *
 * <h5 class='section'>Attribute Flags:</h5>
 * <p>
 * Non-modifier attributes: <c>ANNOTATION</c>, <c>ANONYMOUS</c>, <c>ARRAY</c>, <c>BRIDGE</c>, <c>CLASS</c>,
 * <c>CONSTRUCTOR</c>, <c>DEFAULT</c>, <c>DEPRECATED</c>, <c>ENUM</c>, <c>ENUM_CONSTANT</c>, <c>HAS_PARAMS</c>,
 * <c>LOCAL</c>, <c>MEMBER</c>, <c>NON_STATIC_MEMBER</c>, <c>PRIMITIVE</c>, <c>RECORD</c>, <c>SEALED</c>,
 * <c>SYNTHETIC</c>, <c>VARARGS</c>.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link ElementInfo} - Base class that uses these flags
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonsReflect">juneau-commons-reflect</a>
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