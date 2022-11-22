// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau;

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.util.*;

/**
 * Represents a version string such as <js>"1.2"</js> or <js>"1.2.3"</js>
 *
 * <p>
 * Used to compare version numbers.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class Version implements Comparable<Version> {

	private int[] parts;

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	A string of the form <js>"#.#..."</js> where there can be any number of parts.
	 * 	<br>Valid values:
	 * 	<ul>
	 * 		<li><js>"1.2"</js>
	 * 		<li><js>"1.2.3"</js>
	 * 		<li><js>"0.1"</js>
	 * 		<li><js>".1"</js>
	 * 	</ul>
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static Version of(String value) {
		if (value == null)
			return null;
		return new Version(value);
	}


	/**
	 * Constructor
	 *
	 * @param value
	 * 	A string of the form <js>"#.#..."</js> where there can be any number of parts.
	 * 	<br>Valid values:
	 * 	<ul>
	 * 		<li><js>"1.2"</js>
	 * 		<li><js>"1.2.3"</js>
	 * 		<li><js>"0.1"</js>
	 * 		<li><js>".1"</js>
	 * 	</ul>
	 * 	Any parts that are not numeric are interpreted as {@link Integer#MAX_VALUE}
	 */
	public Version(String value) {
		if (isEmpty(value))
			value = "0";
		String[] sParts = split(value, '.');
		parts = new int[sParts.length];
		for (int i = 0; i < sParts.length; i++) {
			try {
				parts[i] = sParts[i].isEmpty() ? 0 : Integer.parseInt(sParts[i]);
			} catch (NumberFormatException e) {
				parts[i] = Integer.MAX_VALUE;
			}
		}
	}

	/**
	 * Returns the version part at the specified zero-indexed value.
	 *
	 * @param index The index of the version part.
	 * @return The version part, never <jk>null</jk>.
	 */
	public Optional<Integer> getPart(int index) {
		if (index < 0 || parts.length <= index)
			return empty();
		return optional(parts[index]);
	}

	/**
	 * Returns the major version part (i.e. part at index 0).
	 *
	 * @return The version part, never <jk>null</jk>.
	 */
	public Optional<Integer> getMajor() {
		return getPart(0);
	}

	/**
	 * Returns the minor version part (i.e. part at index 1).
	 *
	 * @return The version part, never <jk>null</jk>.
	 */
	public Optional<Integer> getMinor() {
		return getPart(1);
	}

	/**
	 * Returns the maintenance version part (i.e. part at index 2).
	 *
	 * @return The version part, never <jk>null</jk>.
	 */
	public Optional<Integer> getMaintenance() {
		return getPart(2);
	}

	/**
	 * Returns <jk>true</jk> if the specified version is at least this version.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jsm>assertTrue</jsm>(Version.<jsm>of</jsm>(<js>"1.2"</js>).isAtLeast(Version.<jsm>of</jsm>(<js>"1"</js>)));
	 * 	<jsm>assertFalse</jsm>(Version.<jsm>of</jsm>(<js>"1.2"</js>).isAtLeast(Version.<jsm>of</jsm>(<js>"2"</js>)));
	 * 	<jsm>assertTrue</jsm>(Version.<jsm>of</jsm>(<js>"1.2"</js>).isAtLeast(Version.<jsm>of</jsm>(<js>"1.2.3"</js>)));
	 * 	<jsm>assertFalse</jsm>(Version.<jsm>of</jsm>(<js>"1.2.0"</js>).isAtLeast(Version.<jsm>of</jsm>(<js>"1.2.3"</js>)));
	 * </p>
	 *
	 * @param v The version to compare to.
	 * @return <jk>true</jk> if the specified version is at least this version.
	 */
	public boolean isAtLeast(Version v) {
		return isAtLeast(v, false);
	}


	/**
	 * Returns <jk>true</jk> if the specified version is at least this version.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jsm>assertTrue</jsm>(Version.<jsm>of</jsm>(<js>"1.2.3"</js>).isAtLeast(Version.<jsm>of</jsm>(<js>"1.2.3"</js>, <jk>false</jk>)));
	 * 	<jsm>assertFalse</jsm>(Version.<jsm>of</jsm>(<js>"1.2.3"</js>).isAtLeast(Version.<jsm>of</jsm>(<js>"1.2.3"</js>, <jk>true</jk>)));
	 * </p>
	 *
	 * @param v The version to compare to.
	 * @param exclusive Match down-to-version but not including.
	 * @return <jk>true</jk> if the specified version is at least this version.
	 */
	public boolean isAtLeast(Version v, boolean exclusive) {
		for (int i = 0; i < Math.min(parts.length, v.parts.length); i++) {
			int c = v.parts[i] - parts[i];
			if (c > 0)
				return false;
			else if (c < 0)
				return true;
		}
		for (int i = parts.length; i < v.parts.length; i++)
			if (v.parts[i] != 0)
				return false;
		return ! exclusive;
	}

	/**
	 * Returns <jk>true</jk> if the specified version is at most this version.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jsm>assertFalse</jsm>(Version.<jsm>of</jsm>(<js>"1.2.3"</js>).isAtMost(Version.<jsm>of</jsm>(<js>"1"</js>)));
	 * 	<jsm>assertTrue</jsm>(Version.<jsm>of</jsm>(<js>"1.2.3"</js>).isAtMost(Version.<jsm>of</jsm>(<js>"2"</js>)));
	 * 	<jsm>assertTrue</jsm>(Version.<jsm>of</jsm>(<js>"1.2.3"</js>).isAtMost(Version.<jsm>of</jsm>(<js>"1.2"</js>)));
	 * 	<jsm>assertFalse</jsm>(Version.<jsm>of</jsm>(<js>"1.2.3"</js>).isAtMost(Version.<jsm>of</jsm>(<js>"1.2.0"</js>)));
	 * </p>
	 *
	 * @param v The version to compare to.
	 * @return <jk>true</jk> if the specified version is at most this version.
	 */
	public boolean isAtMost(Version v) {
		return isAtMost(v, false);
	}

	/**
	 * Returns <jk>true</jk> if the specified version is at most this version.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jsm>assertTrue</jsm>(Version.<jsm>of</jsm>(<js>"1.2.3"</js>).isAtMost(Version.<jsm>of</jsm>(<js>"1.2.3"</js>, <jk>false</jk>)));
	 * 	<jsm>assertFalse</jsm>(Version.<jsm>of</jsm>(<js>"1.2.3"</js>).isAtMost(Version.<jsm>of</jsm>(<js>"1.2.3"</js>, <jk>true</jk>)));
	 * </p>
	 *
	 * @param v The version to compare to.
	 * @param exclusive Match up-to-version but not including.
	 * @return <jk>true</jk> if the specified version is at most this version.
	 */
	public boolean isAtMost(Version v, boolean exclusive) {
		for (int i = 0; i < Math.min(parts.length, v.parts.length); i++) {
			int c = parts[i] - v.parts[i];
			if (c > 0)
				return false;
			else if (c < 0)
				return true;
		}
		for (int i = parts.length; i < v.parts.length; i++)
			if (v.parts[i] > 0)
				return true;
		return ! exclusive;
	}

	/**
	 * Returns <jk>true</jk> if the specified version is equal to this version.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jsm>assertTrue</jsm>(Version.<jsm>of</jsm>(<js>"1.2.3"</js>).isEqualsTo(Version.<jsm>of</jsm>(<js>"1.2.3"</js>)));
	 * 	<jsm>assertTrue</jsm>(Version.<jsm>of</jsm>(<js>"1.2.3"</js>).isEqualsTo(Version.<jsm>of</jsm>(<js>"1.2"</js>)));
	 * </p>
	 *
	 * @param v The version to compare to.
	 * @return <jk>true</jk> if the specified version is equal to this version.
	 */
	public boolean equals(Version v) {
		for (int i = 0; i < Math.min(parts.length, v.parts.length); i++)
			if (v.parts[i] - parts[i] != 0)
				return false;
		return true;
	}

	@Override /* Object */
	public boolean equals(Object o) {
		return o instanceof Version && equals((Version)o);
	}

	@Override /* Object */
	public String toString() {
		return join(parts, '.');
	}

	@Override
	public int compareTo(Version v) {
		for (int i = 0; i < Math.min(parts.length, v.parts.length); i++) {
			int c = parts[i] - v.parts[i];
			if (c != 0)
				return c;
		}
		return parts.length - v.parts.length;
	}
}
