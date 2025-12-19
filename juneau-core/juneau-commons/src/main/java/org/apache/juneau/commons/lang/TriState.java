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
package org.apache.juneau.commons.lang;

/**
 * Three-state enumeration for boolean-like values that can be explicitly set or unset.
 *
 * <p>
 * This enum is useful in scenarios where you need to distinguish between:
 * <ul>
 * 	<li>A value that is explicitly set to <jk>true</jk></li>
 * 	<li>A value that is explicitly set to <jk>false</jk></li>
 * 	<li>A value that is not set (should inherit from a parent/default)</li>
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// In an annotation</jc>
 * 	<jk>public @interface</jk> MyConfig {
 * 		TriState <jsm>enabled</jsm>() <jk>default</jk> TriState.<jsf>UNSET</jsf>;
 * 	}
 *
 * 	<jc>// Usage</jc>
 * 	<ja>@MyConfig</ja>(enabled = TriState.<jsf>TRUE</jsf>)  <jc>// Explicitly enabled</jc>
 * 	<ja>@MyConfig</ja>(enabled = TriState.<jsf>FALSE</jsf>) <jc>// Explicitly disabled</jc>
 * 	<ja>@MyConfig</ja>                                    <jc>// Not set, inherits default</jc>
 * </p>
 */
public enum TriState {
	/** Explicitly set to <jk>true</jk>. */
	TRUE,

	/** Explicitly set to <jk>false</jk>. */
	FALSE,

	/** Not set - should inherit from parent or use default value. */
	UNSET;

	/**
	 * Converts this TriState to a boolean value.
	 *
	 * <p>
	 * If this is {@link #UNSET}, returns the provided default value.
	 *
	 * @param defaultValue The default value to use if this is {@link #UNSET}.
	 * @return <jk>true</jk> if this is {@link #TRUE}, <jk>false</jk> if this is {@link #FALSE},
	 * 	or the provided default if this is {@link #UNSET}.
	 */
	public boolean toBoolean(boolean defaultValue) {
		return this == UNSET ? defaultValue : (this == TRUE);
	}

	/**
	 * Converts a boolean to a TriState.
	 *
	 * @param value The boolean value.
	 * @return {@link #TRUE} if <jk>true</jk>, {@link #FALSE} if <jk>false</jk>.
	 */
	public static TriState fromBoolean(boolean value) {
		return value ? TRUE : FALSE;
	}
}

