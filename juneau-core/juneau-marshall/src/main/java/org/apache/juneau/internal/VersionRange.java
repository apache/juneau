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

import static org.apache.juneau.common.internal.StringUtils.*;

import org.apache.juneau.*;

/**
 * Represents an OSGi-style version range like <js>"1.2"</js> or <js>"[1.0,2.0)"</js>.
 *
 * <p>
 * The range can be any of the following formats:
 * <ul>
 * 	<li><js>"[0,1.0)"</js> = Less than 1.0.  1.0 and 1.0.0 does not match.
 * 	<li><js>"[0,1.0]"</js> = Less than or equal to 1.0.  Note that 1.0.1 will match.
 * 	<li><js>"1.0"</js> = At least 1.0.  1.0 and 2.0 will match.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class VersionRange {

	private final Version minVersion, maxVersion;
	private final boolean minExclusive, maxExclusive;

	/**
	 * Constructor.
	 *
	 * @param range The range string to parse.
	 */
	public VersionRange(String range) {
		range = range.trim();
		if (! range.isEmpty()) {
			char c1 = range.charAt(0), c2 = range.charAt(range.length()-1);
			int c = range.indexOf(',');
			if (c > -1 && (c1 == '[' || c1 == '(') && (c2 == ']' || c2 == ')')) {
				String v1 = range.substring(1, c), v2 = range.substring(c+1, range.length()-1);
				minVersion = new Version(v1);
				maxVersion = new Version(v2);
				minExclusive = c1 == '(';
				maxExclusive = c2 == ')';
			} else {
				minVersion = new Version(range);
				maxVersion = null;
				minExclusive = maxExclusive = false;
			}
		} else {
			minVersion = maxVersion = null;
			minExclusive = maxExclusive = false;
		}
	}

	/**
	 * Returns <jk>true</jk> if the specified version string matches this version range.
	 *
	 * @param v The version string (e.g. <js>"1.2.3"</js>)
	 * @return <jk>true</jk> if the specified version string matches this version range.
	 */
	public boolean matches(String v) {
		if (isEmpty(v))
			return (minVersion == null && maxVersion == null);
		Version ver = new Version(v);
		if ((minVersion != null && ! ver.isAtLeast(minVersion, minExclusive)) || (maxVersion != null && ! ver.isAtMost(maxVersion, maxExclusive)))
			return false;
		return true;
	}

	@Override /* Object */
	public String toString() {
		return (minExclusive ? "(" : "[") + minVersion + ',' + maxVersion + (maxExclusive ? ")" : "]");
	}
}
