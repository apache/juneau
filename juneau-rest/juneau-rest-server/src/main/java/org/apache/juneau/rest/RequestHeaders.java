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
import static java.util.stream.Collectors.*;
import static org.apache.juneau.assertions.Assertions.*;

import java.time.*;
import java.util.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.collections.*;

/**
 * Represents the headers in an HTTP request.
 *
 * <p>
 * Entries are stored in a case-insensitive map unless overridden via the constructor.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc RestmRequestHeaders}
 * </ul>
 */
public class RequestHeaders {

	private final RestRequest req;
	private final boolean caseSensitive;

	private HttpPartParserSession parser;

	private List<RequestHeader> list = new LinkedList<>();
	private Map<String,List<RequestHeader>> map = new TreeMap<>();

	RequestHeaders(RestRequest req, RequestQueryParams query, boolean caseSensitive) {
		this.req = req;
		this.caseSensitive = caseSensitive;

		for (Enumeration<String> e = req.getHttpServletRequest().getHeaderNames(); e.hasMoreElements();) {
			String name = e.nextElement();
			String key = key(name);
			List<RequestHeader> l = new ArrayList<>();
			for (Enumeration<String> ve = req.getHttpServletRequest().getHeaders(name); ve.hasMoreElements();) {
				RequestHeader h = new RequestHeader(req, name, ve.nextElement());
				list.add(h);
				l.add(h);
			}
			map.put(key, l);
		}

		Set<String> allowedHeaderParams = req.getContext().getAllowedHeaderParams();
		for (RequestQueryParam p : query.getAll()) {
			String name = p.getName();
			String key = key(name);
			if (allowedHeaderParams.contains(key) || allowedHeaderParams.contains("*")) {
				List<RequestHeader> l = map.get(key);
				if (l == null)
					l = new ArrayList<>();
				RequestHeader h = new RequestHeader(req, name, p.getValue());
				list.add(h);
				l.add(h);
				map.put(key, l);
			}
		}
	}

	/**
	 * Copy constructor.
	 */
	private RequestHeaders(RequestHeaders copyFrom) {
		req = copyFrom.req;
		caseSensitive = copyFrom.caseSensitive;
		parser = copyFrom.parser;
		list.addAll(copyFrom.list);
		map.putAll(copyFrom.map);
	}

	/**
	 * Subset constructor.
	 */
	private RequestHeaders(RestRequest req, Map<String,List<RequestHeader>> headerMap, HttpPartParserSession parser, boolean caseSensitive) {
		this.req = req;
		map.putAll(headerMap);
		list = headerMap.values().stream().flatMap(List::stream).collect(toList());
		this.parser = parser;
		this.caseSensitive = caseSensitive;
	}

	RequestHeaders parser(HttpPartParserSession parser) {
		this.parser = parser;
		for (RequestHeader h : list)
			h.parser(parser);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Basic operations.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Adds default entries to these headers.
	 *
	 * <p>
	 * Similar to {@link #set(String, Object)} but doesn't override existing values.
	 *
	 * @param pairs The default entries.  Must not be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public RequestHeaders addDefault(List<Header> pairs) {
		assertArgNotNull("pairs", pairs);
		for (Header p : pairs) {
			String name = p.getName();
			String key = key(name);
			List<RequestHeader> l = map.get(key);
			boolean hasAllBlanks = l != null && l.stream().allMatch(x -> StringUtils.isEmpty(x.getValue()));
			if (l == null || hasAllBlanks) {
				if (hasAllBlanks)
					list.removeAll(l);
				RequestHeader x = new RequestHeader(req, name, p.getValue());
				list.add(x);
				map.put(key, AList.of(x));
			}
		}
		return this;
	}

	/**
	 * Returns all the headers with the specified name.
	 *
	 * @param name The header name.  Must not be <jk>null</jk>.
	 * @return The list of all headers with the specified name, or an empty list if none are found.
	 */
	public List<RequestHeader> getAll(String name) {
		assertArgNotNull("name", name);
		List<RequestHeader> l = map.get(key(name));
		return unmodifiableList(l == null ? emptyList() : l);
	}

	/**
	 * Returns all the headers in this request.
	 *
	 * @return All the headers in this request.
	 */
	public List<RequestHeader> getAll() {
		return unmodifiableList(list);
	}

	/**
	 * Returns <jk>true</jk> if the headers with the specified names are present.
	 *
	 * @param names The header names.  Must not be <jk>null</jk>.
	 * @return <jk>true</jk> if the headers with the specified names are present.
	 */
	public boolean contains(String...names) {
		assertArgNotNull("names", names);
		for (String n : names)
			if (! map.containsKey(key(n)))
				return false;
		return true;
	}

	/**
	 * Returns <jk>true</jk> if the header with any of the specified names are present.
	 *
	 * @param names The header names.  Must not be <jk>null</jk>.
	 * @return <jk>true</jk> if the header with any of the specified names are present.
	 */
	public boolean containsAny(String...names) {
		assertArgNotNull("names", names);
		for (String n : names)
			if (map.containsKey(key(n)))
				return true;
		return false;
	}

	/**
	 * Returns <jk>true</jk> if these headers are empty.
	 *
	 * @return <jk>true</jk> if these headers are empty.
	 */
	public boolean isEmpty() {
		return list.isEmpty();
	}

	/**
	 * Adds a request header value.
	 *
	 * <p>
	 * Header is added to the end.
	 * <br>Existing headers with the same name are not changed.
	 *
	 * @param name The header name.  Must not be <jk>null</jk>.
	 * @param value The header value.  Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public RequestHeaders add(String name, Object value) {
		assertArgNotNull("name", name);
		String key = key(name);
		RequestHeader h = new RequestHeader(req, name, stringify(value)).parser(parser);
		if (map.containsKey(key))
			map.get(key).add(h);
		else
			map.put(key, AList.of(h));
		list.add(h);
		return this;
	}

	/**
	 * Adds request header values.
	 *
	 * <p>
	 * Headers are added to the end.
	 * <br>Existing headers with the same name are not changed.
	 *
	 * @param headers The header objects.  Must not be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public RequestHeaders add(Header...headers) {
		assertArgNotNull("headers", headers);
		for (Header h : headers) {
			if (h != null)
				add(h.getName(), h.getValue());
		}
		return this;
	}

	/**
	 * Sets a request header value.
	 *
	 * <p>
	 * Header is added to the end.
	 * <br>Any previous headers with the same name are removed.
	 *
	 * @param name The header name.  Must not be <jk>null</jk>.
	 * @param value
	 * 	The header value.
	 * 	<br>Converted to a string using {@link Object#toString()}.
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public RequestHeaders set(String name, Object value) {
		assertArgNotNull("name", name);
		String key = key(name);
		remove(key);
		RequestHeader h = new RequestHeader(req, name, stringify(value)).parser(parser);
		map.put(key, AList.of(h));
		list.add(h);
		return this;
	}

	/**
	 * Sets request header values.
	 *
	 * <p>
	 * Headers are added to the end.
	 * <br>Any previous headers with the same name are removed.
	 *
	 * @param headers The header to set.  Must not be <jk>null</jk> or contain <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public RequestHeaders set(Header...headers) {
		assertArgNotNull("headers", headers);
		for (Header h : headers)
			remove(h);
		for (Header h : headers)
			add(h);
		return this;
	}

	/**
	 * Remove headers.
	 *
	 * @param name The header names.  Must not be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public RequestHeaders remove(String...name) {
		assertArgNotNull("name", name);
		for (String n : name) {
			String key = key(n);
			if (map.containsKey(key))
				list.removeAll(map.get(key));
			map.remove(key);
		}
		return this;
	}

	/**
	 * Remove headers.
	 *
	 * @param headers The headers to remove.  Must not be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public RequestHeaders remove(Header...headers) {
		for (Header h : headers)
			remove(h.getName());
		return this;
	}

	/**
	 * Returns a copy of this object but only with the specified header names copied.
	 *
	 * @param headers The list to include in the copy.
	 * @return A new list object.
	 */
	public RequestHeaders subset(String...headers) {
		Map<String,List<RequestHeader>> m = Arrays
			.asList(headers)
			.stream()
			.map(x -> key(x))
			.filter(map::containsKey)
			.collect(toMap(Function.identity(),map::get));

		return new RequestHeaders(req, m, parser, caseSensitive);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Convenience getters.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the first header with the specified name.
	 *
	 * <p>
	 * Note that this method never returns <jk>null</jk> and that {@link RequestHeader#isPresent()} can be used
	 * to test for the existence of the header.
	 *
	 * @param name The header name.  Must not be <jk>null</jk>.
	 * @return The header.  Never <jk>null</jk>.
	 */
	public RequestHeader getFirst(String name) {
		assertArgNotNull("name", name);
		List<RequestHeader> l = map.get(key(name));
		return (l == null || l.isEmpty() ? new RequestHeader(req, name, null).parser(parser) : l.get(0));
	}

	/**
	 * Returns the last header with the specified name.
	 *
	 * <p>
	 * Note that this method never returns <jk>null</jk> and that {@link RequestHeader#isPresent()} can be used
	 * to test for the existence of the header.
	 *
	 * @param name The header name.  Must not be <jk>null</jk>.
	 * @return The header.  Never <jk>null</jk>.
	 */
	public RequestHeader getLast(String name) {
		assertArgNotNull("name", name);
		List<RequestHeader> l = map.get(key(name));
		return (l == null || l.isEmpty() ? new RequestHeader(req, name, null).parser(parser) : l.get(l.size()-1));
	}

	/**
	 * Returns the last header with the specified name.
	 *
	 * <p>
	 * This is equivalent to {@link #getLast(String)}.
	 *
	 * @param name The header name.
	 * @return The header, never <jk>null</jk>.
	 */
	public RequestHeader get(String name) {
		return getLast(name);
	}

	/**
	 * Returns the last header with the specified name as a string.
	 *
	 * @param name The header name.
	 * @return The header value, or {@link Optional#empty()} if it doesn't exist.
	 */
	public Optional<String> getString(String name) {
		return getLast(name).asString();
	}

	/**
	 * Returns the last header with the specified name as an integer.
	 *
	 * @param name The header name.
	 * @return The header value, or {@link Optional#empty()} if it doesn't exist.
	 */
	public Optional<Integer> getInteger(String name) {
		return getLast(name).asInteger();
	}

	/**
	 * Returns the last header with the specified name as a boolean.
	 *
	 * @param name The header name.
	 * @return The header value, or {@link Optional#empty()} if it doesn't exist.
	 */
	public Optional<Boolean> getBoolean(String name) {
		return getLast(name).asBoolean();
	}

	/**
	 * Returns the last header with the specified name as a list from a comma-delimited string.
	 *
	 * @param name The header name.
	 * @return The header value, or {@link Optional#empty()} if it doesn't exist.
	 */
	public Optional<List<String>> getCsvArray(String name) {
		return getLast(name).asCsvArray();
	}

	/**
	 * Returns the last header with the specified name as a long.
	 *
	 * @param name The header name.
	 * @return The header value, or {@link Optional#empty()} if it doesn't exist.
	 */
	public Optional<Long> getLong(String name) {
		return getLast(name).asLong();
	}

	/**
	 * Returns the last header with the specified name as a boolean.
	 *
	 * @param name The header name.
	 * @return The header value, or {@link Optional#empty()} if it doesn't exist.
	 */
	public Optional<ZonedDateTime> getDate(String name) {
		return getLast(name).asDate();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Makes a copy of these parameters.
	 *
	 * @return A new parameters object.
	 */
	public RequestHeaders copy() {
		return new RequestHeaders(this);
	}

	/**
	 * Converts the headers to a readable string.
	 *
	 * @param sorted Sort the headers by name.
	 * @return A JSON string containing the contents of the headers.
	 */
	public String toString(boolean sorted) {
		OMap m = OMap.create();
		if (sorted) {
			for (List<RequestHeader> h1 : map.values())
				for (RequestHeader h2 : h1)
					m.append(h2.getName(), h2.getValue());
		} else {
			for (RequestHeader h : list)
				m.append(h.getName(), h.getValue());
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
