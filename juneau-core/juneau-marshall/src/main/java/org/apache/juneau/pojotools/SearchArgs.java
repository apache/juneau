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

import java.util.*;

import org.apache.juneau.internal.*;

/**
 * Encapsulates arguments for the {@link PojoSorter} class.
 */
public class SearchArgs {

	private final Map<String,String> search = new LinkedHashMap<>();


	/**
	 * Constructor.
	 *
	 * @param searchArgs Search arguments.
	 */
	public SearchArgs(String searchArgs) {
		this(Arrays.asList(StringUtils.split(searchArgs, ',')));
	}

	/**
	 * Constructor.
	 *
	 * @param searchArgs Search arguments.
	 */
	public SearchArgs(List<String> searchArgs) {
		for (String s : searchArgs) {
			int i = StringUtils.indexOf(s, '=', '>', '<');
			if (i == -1)
				throw new PatternException("Invalid search terms: ''{0}''", searchArgs);
			char c = s.charAt(i);
			append(s.substring(0, i).trim(), s.substring(c == '=' ? i+1 : i).trim());
		}
	}

	/**
	 * Appends the specified search argument.
	 *
	 * @param column The column name to search.
	 * @param searchTerm The search term.
	 * @return This object (for method chaining).
	 */
	public SearchArgs append(String column, String searchTerm) {
		this.search.put(column, searchTerm);
		return this;
	}

	/**
	 * The query search terms.
	 *
	 * <p>
	 * The search terms are key/value pairs consisting of column-names and search tokens.
	 *
	 * <p>
	 * It's up to implementers to decide the syntax and meaning of the search term.
	 *
	 * @return An unmodifiable map of query search terms.
	 */
	public Map<String,String> getSearch() {
		return search;
	}
}
