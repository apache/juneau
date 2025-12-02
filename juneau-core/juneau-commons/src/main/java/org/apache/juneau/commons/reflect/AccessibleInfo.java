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

import static org.apache.juneau.commons.utils.Utils.*;

import java.lang.reflect.*;

/**
 * Abstract base class for reflection info classes that wrap {@link AccessibleObject}.
 *
 * <p>
 * This class extends {@link ElementInfo} to provide common functionality for reflection elements that can be made
 * accessible (fields, methods, and constructors). It mirrors the {@link AccessibleObject} API, allowing private
 * members to be accessed via reflection.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>Accessibility control - make private members accessible
 * 	<li>Security exception handling - gracefully handles security exceptions
 * 	<li>Accessibility checking - check if an element is accessible
 * 	<li>Fluent API - methods return <c>this</c> for method chaining
 * </ul>
 *
 * <h5 class='section'>Use Cases:</h5>
 * <ul class='spaced-list'>
 * 	<li>Accessing private fields, methods, or constructors
 * 	<li>Building frameworks that need to work with non-public members
 * 	<li>Testing scenarios where private members need to be accessed
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Make accessible</jc>
 * 	AccessibleInfo <jv>ai</jv> = ...;
 * 	<jv>ai</jv>.setAccessible();  <jc>// Makes private member accessible</jc>
 *
 * 	<jc>// Check accessibility</jc>
 * 	<jk>if</jk> (! <jv>ai</jv>.isAccessible()) {
 * 		<jv>ai</jv>.setAccessible();
 * 	}
 *
 * 	<jc>// Fluent API</jc>
 * 	<jv>ai</jv>.accessible();  <jc>// Returns this after making accessible</jc>
 * </p>
 *
 * <h5 class='section'>Security:</h5>
 * <p>
 * The {@link #setAccessible()} method attempts to make the element accessible and quietly ignores
 * {@link SecurityException} if the security manager denies access. This allows code to work in
 * both secure and non-secure environments.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link ElementInfo} - Base class for all reflection elements
 * 	<li class='jc'>{@link FieldInfo} - Field introspection
 * 	<li class='jc'>{@link MethodInfo} - Method introspection
 * 	<li class='jc'>{@link ConstructorInfo} - Constructor introspection
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonReflect">juneau-common-reflect</a>
 * </ul>
 */
public abstract class AccessibleInfo extends ElementInfo {

	private final AccessibleObject inner;

	/**
	 * Constructor.
	 *
	 * @param inner The {@link AccessibleObject} being wrapped.
	 */
	protected AccessibleInfo(AccessibleObject inner, int modifiers) {
		super(modifiers);
		this.inner = inner;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Accessibility
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns <jk>true</jk> if this object is accessible.
	 *
	 * <p>
	 * This method was added in Java 9. For earlier versions, this always returns <jk>false</jk>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Check if accessible without security checks</jc>
	 * 	<jk>if</jk> (!accessibleInfo.isAccessible()) {
	 * 		accessibleInfo.setAccessible();
	 * 	}
	 * </p>
	 *
	 * @return <jk>true</jk> if this object is accessible, <jk>false</jk> otherwise or if not supported.
	 */
	public boolean isAccessible() {
		try {
			return (boolean)AccessibleObject.class.getMethod("isAccessible").invoke(inner);
		} catch (@SuppressWarnings("unused") Exception ex) {
			return false;
		}
	}

	/**
	 * Attempts to call <code>x.setAccessible(<jk>true</jk>)</code> and quietly ignores security exceptions.
	 *
	 * @return <jk>true</jk> if call was successful.
	 */
	public boolean setAccessible() {
		try {
			if (nn(inner))
				inner.setAccessible(true);
			return true;
		} catch (@SuppressWarnings("unused") SecurityException e) {
			return false;
		}
	}
}