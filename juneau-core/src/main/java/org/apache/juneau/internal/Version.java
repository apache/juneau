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
package org.apache.juneau.internal;

import static org.apache.juneau.internal.StringUtils.*;

/**
 * Represents a version string such as <js>"1.2"</js> or <js>"1.2.3"</js>
 * <p>
 * Used to compare version numbers.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public class Version {

	private int[] parts;

	/**
	 * Constructor
	 *
	 * @param versionString - A string of the form <js>"#.#..."</js> where there can be any number of parts.
	 * 	<br>Valid values:
	 * 	<ul>
	 * 		<li><js>"1.2"</js>
	 * 		<li><js>"1.2.3"</js>
	 * 		<li><js>"0.1"</js>
	 * 		<li><js>".1"</js>
	 * 	</ul>
	 * 	Any parts that are not numeric are interpreted as {@link Integer#MAX_VALUE}
	 */
	public Version(String versionString) {
		if (isEmpty(versionString))
			versionString = "0";
		String[] sParts = split(versionString, '.');
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
	 * Returns <jk>true</jk> if the specified version is at least this version.
	 * <p>
	 * Note that the following is true:
	 * <p class='bcode'>
	 * 	boolean b;
	 * 	b = <jk>new</jk> Version(<js>"1.2"</js>).isAtLeast(<jk>new</jk> Version(<js>"1.2.3"</js>)); <jc>// == true </jc>
	 * 	b = <jk>new</jk> Version(<js>"1.2.0"</js>).isAtLeast(<jk>new</jk> Version(<js>"1.2.3"</js>)); <jc>// == false</jc>
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
	 * <p>
	 * Note that the following is true:
	 * <p class='bcode'>
	 * 	boolean b;
	 * 	b = <jk>new</jk> Version(<js>"1.2.3"</js>).isAtMost(<jk>new</jk> Version(<js>"1.2"</js>)); <jc>// == true </jc>
	 * 	b = <jk>new</jk> Version(<js>"1.2.3"</js>).isAtMost(<jk>new</jk> Version(<js>"1.2.0"</js>)); <jc>// == false</jc>
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
				return false;
		return ! exclusive;
	}

	/**
	 * Returns <jk>true</jk> if the specified version is equal to this version.
	 * <p>
	 * Note that the following is true:
	 * <p class='bcode'>
	 * 	boolean b;
	 * 	b = <jk>new</jk> Version(<js>"1.2.3"</js>).equals(<jk>new</jk> Version(<js>"1.2"</js>)); <jc>// == true </jc>
	 * 	b = <jk>new</jk> Version(<js>"1.2"</js>).equals(<jk>new</jk> Version(<js>"1.2.3"</js>)); <jc>// == true</jc>
	 * </p>
	 *
	 * @param v The version to compare to.
	 * @return <jk>true</jk> if the specified version is equal to this version.
	 */
	public boolean isEqualsTo(Version v) {
		for (int i = 0; i < Math.min(parts.length, v.parts.length); i++)
			if (v.parts[i] - parts[i] != 0)
				return false;
		return true;
	}

	@Override /* Object */
	public String toString() {
		return join(parts, '.');
	}
}
