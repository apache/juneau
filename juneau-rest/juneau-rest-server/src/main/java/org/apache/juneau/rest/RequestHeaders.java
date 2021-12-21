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

import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.internal.ThrowableUtils.*;
import static java.util.Collections.*;
import static java.util.stream.Collectors.*;
import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.httppart.HttpPartType.*;

import java.util.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.header.*;

/**
 * Represents the headers in an HTTP request.
 *
 * <p>
 * 	The {@link RequestHeaders} object is the API for accessing the headers of an HTTP request.
 * 	It can be accessed by passing it as a parameter on your REST Java method:
 * </p>
 * <p class='bcode w800'>
 * 	<ja>@RestPost</ja>(...)
 * 	<jk>public</jk> Object myMethod(RequestHeaders <jv>headers</jv>) {...}
 * </p>
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bcode w800'>
 * 	<ja>@RestPost</ja>(...)
 * 	<jk>public</jk> Object myMethod(RequestHeaders <jv>headers</jv>) {
 *
 * 		<jc>// Add a default value.</jc>
 * 		<jv>headers</jv>.addDefault(<js>"ETag"</js>, <jsf>DEFAULT_UUID</jsf>);
 *
 * 		<jc>// Get a header value as a POJO.</jc>
 * 		UUID <jv>etag</jv> = <jv>headers</jv>.get(<js>"ETag"</js>).as(UUID.<jk>class</jk>).get();
 *
 * 		<jc>// Get a header as a standard HTTP part.</jc>
 * 		ContentType <jv>contentType</jv> = <jv>headers</jv>.get(ContentType.<jk>class</jk>).orElse(ContentType.<jsf>TEXT_XML</jsf>);
 * 	}
 * </p>
 *
 * <p>
 * 	Some important methods on this class are:
 * </p>
 * <ul class='javatree'>
 * 	<li class='jc'>{@link RequestHeaders}
 * 	<ul class='spaced-list'>
 * 		<li>Methods for retrieving headers:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RequestHeaders#contains(String...) contains(String...)}
 * 			<li class='jm'>{@link RequestHeaders#containsAny(String...) containsAny(String...)}
 * 			<li class='jm'>{@link RequestHeaders#get(Class) get(Class)}
 * 			<li class='jm'>{@link RequestHeaders#get(String) get(String)}
 * 			<li class='jm'>{@link RequestHeaders#getAll() getAll()}
 * 			<li class='jm'>{@link RequestHeaders#getAll(String) getAll(String)}
 * 			<li class='jm'>{@link RequestHeaders#getFirst(String) getFirst(String)}
 * 			<li class='jm'>{@link RequestHeaders#getLast(String) getLast(String)}
 * 		</ul>
 * 		<li>Methods overridding headers:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RequestHeaders#add(Header...) add(Header...)}
 * 			<li class='jm'>{@link RequestHeaders#add(String, Object) add(String, Object)}
 * 			<li class='jm'>{@link RequestHeaders#addDefault(Header...) addDefault(Header...)}
 * 			<li class='jm'>{@link RequestHeaders#addDefault(List) addDefault(List)}
 * 			<li class='jm'>{@link RequestHeaders#addDefault(String,String) addDefault(String,String)}
 * 			<li class='jm'>{@link RequestHeaders#remove(Header...) remove(Header...)}
 * 			<li class='jm'>{@link RequestHeaders#remove(String...) remove(String...)}
 * 			<li class='jm'>{@link RequestHeaders#set(Header...) set(Header...)}
 * 			<li class='jm'>{@link RequestHeaders#set(String,Object) set(String,Object)}
 * 		</ul>
 * 		<li>Other methods:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RequestHeaders#copy() copy()}
 * 			<li class='jm'>{@link RequestHeaders#isEmpty() isEmpty()}
 * 			<li class='jm'>{@link RequestHeaders#subset(String...) subset(String...)}
 * 		</ul>
 * 	</ul>
 * </ul>
 *
 * <p>
 * Entries are stored in a case-insensitive map unless overridden via the constructor.
 *
 * <ul class='seealso'>
 * 	<li class='jc'>{@link RequestHeader}
 * 	<li class='ja'>{@link org.apache.juneau.http.annotation.Header}
 * 	<li class='link'>{@doc TODO}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public class RequestHeaders {

	private final RestRequest req;
	private final boolean caseSensitive;
	private final VarResolverSession vs ;

	private HttpPartParserSession parser;

	private List<RequestHeader> list = new LinkedList<>();
	private Map<String,List<RequestHeader>> map = new TreeMap<>();

	RequestHeaders(RestRequest req, RequestQueryParams query, boolean caseSensitive) {
		this.req = req;
		this.caseSensitive = caseSensitive;
		this.vs = req.getVarResolverSession();

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

		// Parameters defined on the request URL overwrite existing headers.
		Set<String> allowedHeaderParams = req.getContext().getAllowedHeaderParams();
		for (RequestQueryParam p : query.getAll()) {
			String name = p.getName();
			String key = key(name);
			if (allowedHeaderParams.contains(key) || allowedHeaderParams.contains("*")) {
				set(name, p.getValue());
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
		vs = copyFrom.vs;
	}

	/**
	 * Subset constructor.
	 */
	private RequestHeaders(RequestHeaders copyFrom, Map<String,List<RequestHeader>> headerMap) {
		this.req = copyFrom.req;
		map.putAll(headerMap);
		list = headerMap.values().stream().flatMap(List::stream).collect(toList());
		parser = copyFrom.parser;
		caseSensitive = copyFrom.caseSensitive;
		vs = copyFrom.vs;
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
	 * @return This object.
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
				RequestHeader x = new RequestHeader(req, name, vs.resolve(p.getValue()));
				list.add(x);
				map.put(key, AList.of(x));
			}
		}
		return this;
	}

	/**
	 * Adds default entries to these headers.
	 *
	 * <p>
	 * Similar to {@link #set(String, Object)} but doesn't override existing values.
	 *
	 * @param pairs The default entries.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public RequestHeaders addDefault(Header...pairs) {
		return addDefault(Arrays.asList(pairs));
	}

	/**
	 * Adds a default entry to the request headers.
	 *
	 * @param name The name.
	 * @param value The value.
	 * @return This object.
	 */
	public RequestHeaders addDefault(String name, String value) {
		return addDefault(BasicStringHeader.of(name, value));
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
	 * @return This object.
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
	 * @return This object.
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
	 * @return This object.
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
	 * @return This object.
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
	 * @return This object.
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
	 * @return This object.
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

		return new RequestHeaders(this, m);
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
	 * Returns the condensed header with the specified name.
	 *
	 * @param name The header name.
	 * @return The header, never <jk>null</jk>.
	 */
	public RequestHeader get(String name) {
		List<RequestHeader> l = getAll(name);
		if (l.isEmpty())
			return new RequestHeader(req, name, null).parser(parser);
		if (l.size() == 1)
			return l.get(0);
		StringBuilder sb = new StringBuilder(128);
		for (int i = 0, j = l.size(); i < j; i++) {
			if (i > 0)
				sb.append(", ");
			sb.append(l.get(i).getValue());
		}
		return new RequestHeader(req, name, sb.toString()).parser(parser);
	}

	/**
	 * Returns the header as the specified bean type.
	 *
	 * <p>
	 * Type must have a name specified via the {@link org.apache.juneau.http.annotation.Header} annotation
	 * and a public constructor that takes in either <c>value</c> or <c>name,value</c> as strings.
	 *
	 * @param type The bean type to create.
	 * @return The bean, never <jk>null</jk>.
	 */
	public <T> Optional<T> get(Class<T> type) {
		ClassMeta<T> cm = req.getBeanSession().getClassMeta(type);
		String name = HttpParts.getName(HEADER, cm).orElseThrow(()->runtimeException("@Header(name) not found on class {0}", className(type)));
		return get(name).as(type);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
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
