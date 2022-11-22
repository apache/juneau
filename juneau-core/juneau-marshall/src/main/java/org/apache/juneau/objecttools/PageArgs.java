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
package org.apache.juneau.objecttools;

import static java.util.Optional.*;

/**
 * Arguments passed to {@link ObjectPaginator}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.ObjectTools">Overview &gt; juneau-marshall &gt; Object Tools</a>
 * </ul>
 */
public class PageArgs {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Static creator.
	 *
	 * @param position The zero-indexed position to start the page on.
	 * @param limit The number of rows to return.
	 *
	 * @return A new {@link PageArgs} object.
	 */
	public static PageArgs create(Integer position, Integer limit) {
		if (position == null && limit == null) return null;
		return new PageArgs(ofNullable(position).orElse(0), ofNullable(limit).orElse(-1));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	final int position, limit;

	/**
	 * Constructor.
	 *
	 * @param position The zero-indexed position to start the page on.
	 * @param limit The number of rows to return.
	 */
	public PageArgs(int position, int limit) {
		this.position = position;
		this.limit = limit;
	}

	/**
	 * Returns the number of rows to return.
	 *
	 * @return The number of rows to return.
	 */
	public int getLimit() {
		return limit;
	}

	/**
	 * Returns the zero-indexed position to start the page on.
	 *
	 * @return The zero-indexed position to start the page on.
	 */
	public int getPosition() {
		return position;
	}
}
