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

import static org.apache.juneau.common.utils.Utils.*;

import java.lang.reflect.*;

/**
 * Base class for reflection info classes that wrap {@link AccessibleObject}.
 *
 * <p>
 * This class provides common functionality for {@link FieldInfo}, {@link MethodInfo}, and {@link ConstructorInfo}
 * that mirrors the {@link AccessibleObject} API.
 *
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
}