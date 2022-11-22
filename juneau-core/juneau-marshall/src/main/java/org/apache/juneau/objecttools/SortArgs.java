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

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.util.*;

/**
 * Arguments passed to {@link ObjectSorter}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.ObjectTools">Overview &gt; juneau-marshall &gt; Object Tools</a>
 * </ul>
 */
public class SortArgs {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Static creator.
	 *
	 * @param args
	 * 	Comma-delimited list of sort arguments.
	 * 	<br>Values are of the following forms:
	 * 	<ul>
	 * 		<li><js>"column"</js> - Sort column ascending.
	 * 		<li><js>"column+"</js> - Sort column ascending.
	 * 		<li><js>"column-"</js> - Sort column descending.
	 * 	</ul>
	 * @return A new {@link SortArgs} object.
	 */
	public static SortArgs create(String args) {
		if (args == null) return null;
		return new SortArgs(args);
	}

	/**
	 * Static creator.
	 *
	 * @param args
	 * 	Sort arguments.
	 * 	<br>Values are of the following forms:
	 * 	<ul>
	 * 		<li><js>"column"</js> - Sort column ascending.
	 * 		<li><js>"column+"</js> - Sort column ascending.
	 * 		<li><js>"column-"</js> - Sort column descending.
	 * 	</ul>
	 * @return A new {@link SortArgs} object.
	 */
	public static SortArgs create(List<String> args) {
		if (args == null) return null;
		return new SortArgs(args);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final Map<String,Boolean> sort;

	/**
	 * Constructor.
	 *
	 * @param sortArgs
	 * 	Comma-delimited list of sort arguments.
	 * 	<br>Values are of the following forms:
	 * 	<ul>
	 * 		<li><js>"column"</js> - Sort column ascending.
	 * 		<li><js>"column+"</js> - Sort column ascending.
	 * 		<li><js>"column-"</js> - Sort column descending.
	 * 	</ul>
	 */
	public SortArgs(String sortArgs) {
		this(alist(split(sortArgs)));
	}

	/**
	 * Constructor.
	 *
	 * @param sortArgs
	 * 	Sort arguments.
	 * 	<br>Values are of the following forms:
	 * 	<ul>
	 * 		<li><js>"column"</js> - Sort column ascending.
	 * 		<li><js>"column+"</js> - Sort column ascending.
	 * 		<li><js>"column-"</js> - Sort column descending.
	 * 	</ul>
	 */
	public SortArgs(Collection<String> sortArgs) {
		Map<String,Boolean> sort = map();
		sortArgs.forEach(s -> {
			boolean isDesc = false;
			if (endsWith(s, '-', '+')) {
				isDesc = endsWith(s, '-');
				s = s.substring(0, s.length()-1);
			}
			sort.put(s, isDesc);
		});
		this.sort = unmodifiable(sort);
	}

	/**
	 * The sort columns.
	 *
	 * <p>
	 * The sort columns are key/value pairs consisting of column-names and direction flags
	 * (<jk>false</jk> = ascending, <jk>true</jk> = descending).
	 *
	 * @return An unmodifiable ordered map of sort columns and directions.
	 */
	public Map<String,Boolean> getSort() {
		return sort;
	}
}
