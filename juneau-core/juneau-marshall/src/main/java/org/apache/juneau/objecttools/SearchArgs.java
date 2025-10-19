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
package org.apache.juneau.objecttools;

import static org.apache.juneau.common.utils.Utils.*;

import java.util.*;

import org.apache.juneau.common.utils.*;

/**
 * Arguments passed to {@link ObjectSearcher}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ObjectTools">Object Tools</a>
 * </ul>
 */
public class SearchArgs {
	/**
	 * Static creator.
	 *
	 * @param args List of search arguments.
	 * @return A new {@link SearchArgs} object.
	 */
	public static SearchArgs create(List<String> args) {
		if (args == null)
			return null;
		return new SearchArgs(args);
	}

	/**
	 * Static creator.
	 *
	 * @param args Comma-delimited list of search arguments.
	 * @return A new {@link SearchArgs} object.
	 */
	public static SearchArgs create(String args) {
		if (args == null)
			return null;
		return new SearchArgs(args);
	}

	private final Map<String,String> search = CollectionUtils2.map();

	/**
	 * Constructor.
	 *
	 * @param searchArgs Search arguments.
	 */
	public SearchArgs(List<String> searchArgs) {
		searchArgs.forEach(s -> {
			int i = StringUtils.indexOf(s, '=', '>', '<');
			if (i == -1)
				throw new PatternException("Invalid search terms: ''{0}''", searchArgs);
			char c = s.charAt(i);
			append(s.substring(0, i).trim(), s.substring(c == '=' ? i + 1 : i).trim());
		});
	}

	/**
	 * Constructor.
	 *
	 * @param searchArgs Search arguments.
	 */
	public SearchArgs(String searchArgs) {
		this(alist(StringUtils.splita(searchArgs)));
	}

	/**
	 * Appends the specified search argument.
	 *
	 * @param column The column name to search.
	 * @param searchTerm The search term.
	 * @return This object.
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
	public Map<String,String> getSearch() { return search; }
}