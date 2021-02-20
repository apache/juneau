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
package org.apache.juneau.rest;

import static org.apache.juneau.internal.StringUtils.*;
import static java.util.Collections.*;
import static org.apache.juneau.assertions.Assertions.*;

import java.time.*;
import java.util.*;

import org.apache.http.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.utils.*;
import org.apache.juneau.collections.*;

/**
 * Represents the query parameters on an HTTP request.
 */
public class RequestQueryParams {

	private final RestRequest req;
	private final boolean caseSensitive;
	private HttpPartParserSession parser;

	private List<RequestQueryParam> list = new LinkedList<>();
	private Map<String,List<RequestQueryParam>> map = new TreeMap<>();

	RequestQueryParams(RestRequest req, Map<String,String[]> query, boolean caseSensitive) {
		this.req = req;
		this.caseSensitive = caseSensitive;

		for (Map.Entry<String,String[]> e : query.entrySet()) {
			String name = e.getKey();
			String key = key(name);
			List<RequestQueryParam> l = new ArrayList<>();

			String[] values = e.getValue();
			if (values == null)
				values = new String[0];

			// Fix for behavior difference between Tomcat and WAS.
			// getParameter("foo") on "&foo" in Tomcat returns "".
			// getParameter("foo") on "&foo" in WAS returns null.
			if (values.length == 1 && values[0] == null)
				values[0] = "";

			for (String value : values) {
				RequestQueryParam p = new RequestQueryParam(req, name, value);
				list.add(p);
				l.add(p);
			}
			map.put(key, l);
		}
	}

	RequestQueryParams(RequestQueryParams copyFrom) {
		req = copyFrom.req;
		caseSensitive = copyFrom.caseSensitive;
		parser = copyFrom.parser;
		list.addAll(copyFrom.list);
		map.putAll(copyFrom.map);
	}

	RequestQueryParams parser(HttpPartParserSession parser) {
		this.parser = parser;
		for (RequestQueryParam p : list)
			p.parser(parser);
		return this;
	}

	/**
	 * Adds default entries to these parameters.
	 *
	 * <p>
	 * Similar to {@link #put(String, Object)} but doesn't override existing values.
	 *
	 * @param pairs
	 * 	The default entries.
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public RequestQueryParams addDefault(List<NameValuePair> pairs) {
		for (NameValuePair p : pairs) {
			String name = p.getName();
			String key = key(name);
			List<RequestQueryParam> l = map.get(key);
			boolean hasAllBlanks = l != null && l.stream().allMatch(x -> StringUtils.isEmpty(x.getValue()));
			if (l == null || hasAllBlanks) {
				if (hasAllBlanks)
					list.removeAll(l);
				RequestQueryParam x = new RequestQueryParam(req, name, p.getValue());
				list.add(x);
				map.put(key, AList.of(x));
			}
		}
		return this;
	}

	/**
	 * Returns the first parameter with the specified name.
	 *
	 * <p>
	 * Note that this method never returns <jk>null</jk> and that {@link RequestQueryParam#isPresent()} can be used
	 * to test for the existence of the parameter.
	 *
	 * @param name The parameter name.
	 * @return The parameter.  Never <jk>null</jk>.
	 */
	public RequestQueryParam getFirst(String name) {
		assertArgNotNull("name", name);
		List<RequestQueryParam> l = map.get(key(name));
		return (l == null || l.isEmpty() ? new RequestQueryParam(req, name, null).parser(parser) : l.get(0));
	}

	/**
	 * Returns the last parameter with the specified name.
	 *
	 * <p>
	 * Note that this method never returns <jk>null</jk> and that {@link RequestQueryParam#isPresent()} can be used
	 * to test for the existence of the parameter.
	 *
	 * @param name The parameter name.
	 * @return The parameter.  Never <jk>null</jk>.
	 */
	public RequestQueryParam getLast(String name) {
		assertArgNotNull("name", name);
		List<RequestQueryParam> l = map.get(key(name));
		return (l == null || l.isEmpty() ? new RequestQueryParam(req, name, null).parser(parser) : l.get(l.size()-1));
	}

	/**
	 * Returns all the parameters with the specified name.
	 *
	 * @param name The parameter name.
	 * @return The list of all parameters with the specified name, or an empty list if none are found.
	 */
	public List<RequestQueryParam> getAll(String name) {
		assertArgNotNull("name", name);
		List<RequestQueryParam> l = map.get(key(name));
		return unmodifiableList(l == null ? emptyList() : l);
	}

	/**
	 * Returns all the parameters on this request.
	 *
	 * @return All the parameters on this request.
	 */
	public List<RequestQueryParam> getAll() {
		return unmodifiableList(list);
	}

	/**
	 * Returns the last parameter with the specified name as a string.
	 *
	 * @param name The parameter name.
	 * @return The parameter value, or {@link Optional#empty()} if it doesn't exist.
	 */
	public RequestQueryParam get(String name) {
		return getLast(name);
	}

	/**
	 * Returns the last parameter with the specified name as a string.
	 *
	 * @param name The parameter name.
	 * @return The parameter value, or {@link Optional#empty()} if it doesn't exist.
	 */
	public Optional<String> getString(String name) {
		return getLast(name).asString();
	}

	/**
	 * Returns the last parameter with the specified name as an integer.
	 *
	 * @param name The parameter name.
	 * @return The parameter value, or {@link Optional#empty()} if it doesn't exist.
	 */
	public Optional<Integer> getInteger(String name) {
		return getLast(name).asInteger();
	}

	/**
	 * Returns the last parameter with the specified name as a boolean.
	 *
	 * @param name The parameter name.
	 * @return The parameter value, or {@link Optional#empty()} if it doesn't exist.
	 */
	public Optional<Boolean> getBoolean(String name) {
		return getLast(name).asBoolean();
	}

	/**
	 * Returns the last parameter with the specified name as a list from a comma-delimited string.
	 *
	 * @param name The parameter name.
	 * @return The parameter value, or {@link Optional#empty()} if it doesn't exist.
	 */
	public Optional<List<String>> getCsvArray(String name) {
		return getLast(name).asCsvArray();
	}

	/**
	 * Returns the last parameter with the specified name as a long.
	 *
	 * @param name The parameter name.
	 * @return The parameter value, or {@link Optional#empty()} if it doesn't exist.
	 */
	public Optional<Long> getLong(String name) {
		return getLast(name).asLong();
	}

	/**
	 * Returns the last parameter with the specified name as a boolean.
	 *
	 * @param name The parameter name.
	 * @return The parameter value, or {@link Optional#empty()} if it doesn't exist.
	 */
	public Optional<ZonedDateTime> getDate(String name) {
		return getLast(name).asDate();
	}

	/**
	 * Sets a parameter value.
	 *
	 * <p>
	 * This overwrites any previous value.
	 *
	 * @param name The parameter name.
	 * @param value The parameter value.
	 * @return This object (for method chaining).
	 */
	public RequestQueryParams put(String name, Object value) {
		assertArgNotNull("name", name);
		String key = key(name);
		RequestQueryParam p = new RequestQueryParam(req, name, stringify(value)).parser(parser);
		if (map.containsKey(key))
			list.removeIf(x->caseSensitive?x.getName().equals(name):x.getName().equalsIgnoreCase(name));
		list.add(p);
		map.put(key, AList.of(p));
		return this;
	}

	/**
	 * Adds a parameter value.
	 *
	 * <p>
	 * Parameter is added to the end of the parameters.
	 *
	 * @param name The parameter name.
	 * @param value The parameter value.
	 * @return This object (for method chaining).
	 */
	public RequestQueryParams add(String name, Object value) {
		assertArgNotNull("name", name);
		String key = key(name);
		RequestQueryParam h = new RequestQueryParam(req, name, stringify(value)).parser(parser);
		if (map.containsKey(key))
			map.get(key).add(h);
		else
			map.put(key, AList.of(h));
		list.add(h);
		return this;
	}

	/**
	 * Converts this object to a query string.
	 *
	 * <p>
	 * Returned query string does not start with <js>'?'</js>.
	 *
	 * @return A new query string, or an empty string if this object is empty.
	 */
	public String asQueryString() {
		StringBuilder sb = new StringBuilder();
		for (RequestQueryParam e : list) {
			if (sb.length() > 0)
				sb.append("&");
			sb.append(urlEncode(e.getName())).append('=').append(urlEncode(e.getValue()));
		}
		return sb.toString();
	}

	/**
	 * Makes a copy of these parameters.
	 *
	 * @return A new parameters object.
	 */
	public RequestQueryParams copy() {
		return new RequestQueryParams(this);
	}

	/**
	 * Returns <jk>true</jk> if these parameters are empty.
	 *
	 * @return <jk>true</jk> if these parameters are empty.
	 */
	public boolean isEmpty() {
		return list.isEmpty();
	}

	/**
	 * Returns <jk>true</jk> if these parameters contain the specified name.
	 *
	 * @param name The parameter name.
	 * @return <jk>true</jk> if these parameters contain the specified name.
	 */
	public boolean containsName(String...name) {
		for (String n : name)
			if (map.containsKey(key(n)))
				return true;
		return false;
	}

	/**
	 * Locates the special search query arguments in the query and returns them as a {@link SearchArgs} object.
	 *
	 * <p>
	 * The query arguments are as follows:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		<js>"&amp;s="</js> - A comma-delimited list of column-name/search-token pairs.
	 * 		<br>Example: <js>"&amp;s=column1=foo*,column2=*bar"</js>
	 * 	<li>
	 * 		<js>"&amp;v="</js> - A comma-delimited list column names to view.
	 * 		<br>Example: <js>"&amp;v=column1,column2"</js>
	 * 	<li>
	 * 		<js>"&amp;o="</js> - A comma-delimited list column names to sort by.
	 * 		<br>Column names can be suffixed with <js>'-'</js> to indicate descending order.
	 * 		<br>Example: <js>"&amp;o=column1,column2-"</js>
	 * 	<li>
	 * 		<js>"&amp;p="</js> - The zero-index row number of the first row to display.
	 * 		<br>Example: <js>"&amp;p=100"</js>
	 * 	<li>
	 * 		<js>"&amp;l="</js> - The number of rows to return.
	 * 		<br><c>0</c> implies return all rows.
	 * 		<br>Example: <js>"&amp;l=100"</js>
	 * 	<li>
	 * 		<js>"&amp;i="</js> - The case-insensitive search flag.
	 * 		<br>Example: <js>"&amp;i=true"</js>
	 * </ul>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Whitespace is trimmed in the parameters.
	 * </ul>
	 *
	 * @return
	 * 	A new {@link SearchArgs} object initialized with the special search query arguments.
	 * 	<br>Returns <jk>null</jk> if no search arguments were found.
	 */
	public SearchArgs getSearchArgs() {
		if (containsName("s","v","o","p","l","i")) {
			return new SearchArgs.Builder()
				.search(getString("s").orElse(null))
				.view(getString("v").orElse(null))
				.sort(getString("o").orElse(null))
				.position(getInteger("p").orElse(null))
				.limit(getInteger("l").orElse(null))
				.ignoreCase(getBoolean("i").orElse(null))
				.build();
		}
		return null;
	}

	/**
	 * Converts the parameters to a readable string.
	 *
	 * @param sorted Sort the parameters by name.
	 * @return A JSON string containing the contents of the parameters.
	 */
	public String toString(boolean sorted) {
		OMap m = OMap.create();
		if (sorted) {
			for (List<RequestQueryParam> p1 : map.values())
				for (RequestQueryParam p2 : p1)
					m.append(p2.getName(), p2.getValue());
		} else {
			for (RequestQueryParam p : list)
				m.append(p.getName(), p.getValue());
		}
		return m.toString();
	}

	private String key(String name) {
		return caseSensitive ? name : name.toLowerCase();
	}

	@Override /* Object */
	public String toString() {
		return toString(false);
	}
}
