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
package org.apache.juneau.commons.settings;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

/**
 * Tri-state result of a property lookup.
 *
 * <p>
 * This type disambiguates source lookup semantics:
 * <ul class='spaced-list'>
 * 	<li>{@link #missing()} - The key does not exist in the source.
 * 	<li>{@link #present(Optional)} with {@link Optional#empty()} - The key exists and resolves to a null value.
 * 	<li>{@link #present(Optional)} with a value - The key exists and resolves to a non-null value.
 * </ul>
 */
public final class PropertyLookupResult {

	private static final PropertyLookupResult MISSING = new PropertyLookupResult(false, oe());

	private final boolean present;
	private final Optional<String> value;

	private PropertyLookupResult(boolean present, Optional<String> value) {
		this.present = present;
		this.value = value;
	}

	/**
	 * Returns a result indicating the key is absent in this source.
	 *
	 * @return A missing result.
	 */
	public static PropertyLookupResult missing() {
		return MISSING;
	}

	/**
	 * Returns a result indicating the key is present with the specified value.
	 *
	 * @param value The value optional. Must not be <jk>null</jk>.
	 * @return A present result.
	 */
	public static PropertyLookupResult present(Optional<String> value) {
		return new PropertyLookupResult(true, Objects.requireNonNull(value));
	}

	/**
	 * Returns a result indicating the key is present with the specified value.
	 *
	 * @param value The value.  Can be <jk>null</jk>, in which case the result is present with an empty value.
	 * @return A present result.
	 */
	public static PropertyLookupResult present(String value) {
		return present(o(value));
	}

	/**
	 * Returns <jk>true</jk> if the key is present in this source.
	 *
	 * @return <jk>true</jk> if present.
	 */
	public boolean isPresent() {
		return present;
	}

	/**
	 * Returns the resolved value for present results.
	 *
	 * @return The value, never <jk>null</jk>.
	 */
	public Optional<String> value() {
		return value;
	}
}
