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
package org.apache.juneau.rest.httppart;

import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static java.util.stream.Collectors.*;
import static org.apache.juneau.common.internal.ArgUtils.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.httppart.HttpPartType.*;

import java.util.*;
import java.util.stream.*;

import org.apache.http.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.header.*;

/**
 * Represents the headers in an HTTP request.
 *
 * <p>
 * 	The {@link RequestHeaders} object is the API for accessing the headers of an HTTP request.
 * 	It can be accessed by passing it as a parameter on your REST Java method:
 * </p>
 * <p class='bjava'>
 * 	<ja>@RestPost</ja>(...)
 * 	<jk>public</jk> Object myMethod(RequestHeaders <jv>headers</jv>) {...}
 * </p>
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
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
 * 			<li class='jm'>{@link RequestHeaders#contains(String) contains(String)}
 * 			<li class='jm'>{@link RequestHeaders#containsAny(String...) containsAny(String...)}
 * 			<li class='jm'>{@link RequestHeaders#get(Class) get(Class)}
 * 			<li class='jm'>{@link RequestHeaders#get(String) get(String)}
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
 * 			<li class='jm'>{@link RequestHeaders#remove(String) remove(String)}
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
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link RequestHeader}
 * 	<li class='ja'>{@link org.apache.juneau.http.annotation.Header}
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.HttpParts">HTTP Parts</a>
 * </ul>
 */
public class RequestHeaders extends ArrayList<RequestHeader> {

	private static final long serialVersionUID = 1L;

	private final RestRequest req;
	private boolean caseSensitive;
	private final VarResolverSession vs;

	private HttpPartParserSession parser;

	/**
	 * Constructor.
	 *
	 * @param req The request creating this bean.
	 * @param query The query parameters on the request (used for overloaded header values).
	 * @param caseSensitive Whether case-sensitive name matching is enabled.
	 */
	public RequestHeaders(RestRequest req, RequestQueryParams query, boolean caseSensitive) {
		this.req = req;
		this.caseSensitive = caseSensitive;
		this.vs = req.getVarResolverSession();

		for (Enumeration<String> e = req.getHttpServletRequest().getHeaderNames(); e.hasMoreElements();) {
			String name = e.nextElement();
			for (Enumeration<String> ve = req.getHttpServletRequest().getHeaders(name); ve.hasMoreElements();) {
				add(new RequestHeader(req, name, ve.nextElement()));
			}
		}

		// Parameters defined on the request URL overwrite existing headers.
		Set<String> allowedHeaderParams = req.getContext().getAllowedHeaderParams();
		query.forEach(p -> {
			String name = p.getName();
			String key = key(name);
			if (allowedHeaderParams.contains(key) || allowedHeaderParams.contains("*")) {
				set(name, p.getValue());
			}
		});
	}

	/**
	 * Copy constructor.
	 */
	private RequestHeaders(RequestHeaders copyFrom) {
		req = copyFrom.req;
		caseSensitive = copyFrom.caseSensitive;
		parser = copyFrom.parser;
		addAll(copyFrom);
		vs = copyFrom.vs;
	}

	/**
	 * Subset constructor.
	 */
	private RequestHeaders(RequestHeaders copyFrom, String...names) {
		this.req = copyFrom.req;
		caseSensitive = copyFrom.caseSensitive;
		parser = copyFrom.parser;
		vs = copyFrom.vs;
		for (String n : names)
			copyFrom.stream().filter(x -> eq(x.getName(), n)).forEach(x -> add(x));
	}

	/**
	 * Sets the parser to use for part values.
	 *
	 * @param value The new value for this setting.
	 * @return This object.
	 */
	public RequestHeaders parser(HttpPartParserSession value) {
		this.parser = value;
		forEach(x -> x.parser(parser));
		return this;
	}

	/**
	 * Sets case sensitivity for names in this list.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public RequestHeaders caseSensitive(boolean value) {
		this.caseSensitive = value;
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
			Stream<RequestHeader> l = stream(name);
			boolean hasAllBlanks = l.allMatch(x -> StringUtils.isEmpty(x.getValue()));
			if (hasAllBlanks) {
				removeAll(getAll(name));
				add(new RequestHeader(req, name, vs.resolve(p.getValue())));
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
		return addDefault(alist(pairs));
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
		add(new RequestHeader(req, name, stringify(value)).parser(parser));
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
		for (Header h : headers)
			if (h != null)
				add(h.getName(), h.getValue());
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
		set(new RequestHeader(req, name, stringify(value)).parser(parser));
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
	 * Remove header by name.
	 *
	 * @param name The header names.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public RequestHeaders remove(String name) {
		assertArgNotNull("name", name);
		removeIf(x -> eq(x.getName(), name));
		return this;
	}

	/**
	 * Returns a copy of this object but only with the specified header names copied.
	 *
	 * @param names The list to include in the copy.
	 * @return A new list object.
	 */
	public RequestHeaders subset(String...names) {
		return new RequestHeaders(this, names);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Convenience getters.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns <jk>true</jk> if the header with the specified name is present.
	 *
	 * @param name The header name.  Must not be <jk>null</jk>.
	 * @return <jk>true</jk> if the header with the specified name is present.
	 */
	public boolean contains(String name) {
		return stream(name).findAny().isPresent();
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
			if (stream(n).findAny().isPresent())
				return true;
		return false;
	}

	/**
	 * Returns all headers with the specified name.
	 *
	 * @param name The header name.
	 * @return The list of all headers with matching names.  Never <jk>null</jk>.
	 */
	public List<RequestHeader> getAll(String name) {
		return stream(name).collect(toList());
	}

	/**
	 * Returns all headers with the specified name.
	 *
	 * @param name The header name.
	 * @return The stream of all headers with matching names.  Never <jk>null</jk>.
	 */
	public Stream<RequestHeader> stream(String name) {
		return stream().filter(x -> eq(x.getName(), name));
	}

	/**
	 * Returns all headers in sorted order.
	 *
	 * @return The stream of all headers in sorted order.
	 */
	public Stream<RequestHeader> getSorted() {
		Comparator<RequestHeader> x;
		if (caseSensitive)
			x = (x1,x2) -> x1.getName().compareTo(x2.getName());
		else
			x = (x1,x2) -> String.CASE_INSENSITIVE_ORDER.compare(x1.getName(), x2.getName());
		return stream().sorted(x);
	}

	/**
	 * Returns all the unique header names in this list.
	 * @return The list of all unique header names in this list.
	 */
	public List<String> getNames() {
		return stream().map(x -> x.getName()).map(x -> caseSensitive ? x : x.toLowerCase()).distinct().collect(toList());
	}

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
		return stream(name).findFirst().orElseGet(()->new RequestHeader(req, name, null).parser(parser));
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
		Value<RequestHeader> v = Value.empty();
		stream(name).forEach(x -> v.set(x));
		return v.orElseGet(() -> new RequestHeader(req, name, null).parser(parser));
	}

	/**
	 * Returns the condensed header with the specified name.
	 *
	 * <p>
	 * If multiple headers are present, they will be combined into a single comma-delimited list.
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
	 * @param <T> The bean type to create.
	 * @param type The bean type to create.
	 * @return The bean, never <jk>null</jk>.
	 */
	public <T> Optional<T> get(Class<T> type) {
		ClassMeta<T> cm = req.getBeanSession().getClassMeta(type);
		String name = HttpParts.getName(HEADER, cm).orElseThrow(()->new BasicRuntimeException("@Header(name) not found on class {0}", className(type)));
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

	private String key(String name) {
		return caseSensitive ? name : name.toLowerCase();
	}

	private boolean eq(String s1, String s2) {
		if (caseSensitive)
			return StringUtils.eq(s1, s2);
		return StringUtils.eqic(s1, s2);
	}

	@Override /* Object */
	public String toString() {
		JsonMap m = new JsonMap();
		for (String n : getNames())
			m.put(n, get(n).asString().orElse(null));
		return m.asJson();
	}
}
