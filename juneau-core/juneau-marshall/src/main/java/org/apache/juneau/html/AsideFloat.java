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
package org.apache.juneau.html;

/**
 * Identifies possible float values for {@link HtmlDocSerializer.Builder#asideFloat(AsideFloat)}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.HtmlDetails">HTML Details</a>
 * </ul>
 */
public enum AsideFloat {

	/**
	 * Float right of main contents.
	 */
	LEFT,

	/**
	 * Float left of main contents.
	 */
	RIGHT,

	/**
	 * Float above main contents.
	 */
	TOP,

	/**
	 * Float below main contents.
	 */
	BOTTOM,

	/**
	 * Default value (defaults to {@link #LEFT}
	 */
	DEFAULT;

	/**
	 * Returns <jk>true</jk> if value matches this enum.
	 *
	 * @param value The value to check against.
	 * @return <jk>true</jk> if value matches this enum.
	 */
	public boolean is(AsideFloat value) {
		return this == value;
	}

	/**
	 * Returns <jk>true</jk> if any of the values match this enum.
	 *
	 * @param values The values to check against.
	 * @return <jk>true</jk> if value matches this enum.
	 */
	public boolean isAny(AsideFloat...values) {
		for (AsideFloat v : values)
			if (is(v))
				return true;
		return false;
	}
}