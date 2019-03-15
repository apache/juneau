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
package org.apache.juneau.pojotools;

import static java.util.Collections.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.util.*;

/**
 * Encapsulates arguments for the {@link PojoSorter} class.
 */
public class SortArgs {

	private final Map<String,Boolean> sort;

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
	public SortArgs(String...sortArgs) {
		this(Arrays.asList(sortArgs));
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
		Map<String,Boolean> sort = new LinkedHashMap<>();
		for (String s : sortArgs) {
			boolean isDesc = false;
			if (endsWith(s, '-', '+')) {
				isDesc = endsWith(s, '-');
				s = s.substring(0, s.length()-1);
			}
			sort.put(s, isDesc);
		}
		this.sort = unmodifiableMap(sort);
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
