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
package org.apache.juneau.rest.server.httppart;

import static org.apache.juneau.commons.httppart.HttpPartType.*;
import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.StringUtils.*;

import java.util.*;
import java.util.stream.*;

import org.apache.juneau.commons.collections.*;
import org.apache.juneau.commons.lang.*;
import org.apache.juneau.commons.svl.*;
import org.apache.juneau.commons.utils.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.HttpPart;
import org.apache.juneau.http.part.*;
import org.apache.juneau.marshall.httppart.*;
import org.apache.juneau.marshall.objecttools.*;
import org.apache.juneau.rest.server.*;

/**
 * Represents the query parameters in an HTTP request.
 *
 * <p>
 * 	The {@link RequestQueryParamList} object is the API for accessing the GET query parameters of an HTTP request.
 * 	It can be accessed by passing it as a parameter on your REST Java method:
 * </p>
 * <p class='bjava'>
 * 	<ja>@RestPost</ja>(...)
 * 	<jk>public</jk> Object myMethod(RequestQueryParamList <jv>query</jv>) {...}
 * </p>
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@RestPost</ja>(...)
 * 	<jk>public</jk> Object myMethod(RequestQueryParamList <jv>query</jv>) {
 *
 * 		<jc>// Get query parameters converted to various types.</jc>
 * 		<jk>int</jk> <jv>p1</jv> = <jv>query</jv>.get(<js>"p1"</js>).asInteger().orElse(0);
 * 		String <jv>p2</jv> = <jv>query</jv>.get(<js>"p2"</js>).orElse(<jk>null</jk>);
 * 		UUID <jv>p3</jv> = <jv>query</jv>.get(<js>"p3"</js>).as(UUID.<jk>class</jk>).orElse(<jk>null</jk>);
 * 	 }
 * </p>
 *
 * <p>
 * 	An important distinction between the behavior of this object and <l>HttpServletRequest.getParameter(String)</l> is
 * 	that the former will NOT load the content of the request on FORM POSTS and will only look at parameters
 * 	found in the query string.
 * 	This can be useful in cases where you're mixing GET parameters and FORM POSTS and you don't want to
 * 	inadvertently read the content of the request to get a query parameter.
 * </p>
 *
 * <p>
 * 	Some important methods on this class are:
 * </p>
 * <ul class='javatree'>
 * 	<li class='jc'>{@link RequestQueryParamList}
 * 	<ul class='spaced-list'>
 * 		<li>Methods for retrieving query parameters:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RequestQueryParamList#contains(String) contains(String)}
 * 			<li class='jm'>{@link RequestQueryParamList#containsAny(String...) containsAny(String...)}
 * 			<li class='jm'>{@link RequestQueryParamList#get(Class) get(Class)}
 * 			<li class='jm'>{@link RequestQueryParamList#get(String) get(String)}
 * 			<li class='jm'>{@link RequestQueryParamList#getAll(String) getAll(String)}
 * 			<li class='jm'>{@link RequestQueryParamList#getFirst(String) getFirst(String)}
 * 			<li class='jm'>{@link RequestQueryParamList#getLast(String) getLast(String)}
 * 			<li class='jm'>{@link RequestQueryParamList#getSearchArgs() getSearchArgs()}
 * 			<li class='jm'>{@link RequestQueryParamList#getViewArgs() getViewArgs()}
 * 			<li class='jm'>{@link RequestQueryParamList#getSortArgs() getSortArgs()}
 * 			<li class='jm'>{@link RequestQueryParamList#getPageArgs() getPageArgs()}
 * 		</ul>
 * 		<li>Methods overridding query parameters:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RequestQueryParamList#add(HttpPart...) add(HttpPart...)}
 * 			<li class='jm'>{@link RequestQueryParamList#add(String,Object) add(String,Object)}
 * 			<li class='jm'>{@link RequestQueryParamList#addDefault(List) addDefault(List)}
 * 			<li class='jm'>{@link RequestQueryParamList#addDefault(HttpPart...) addDefault(HttpPart...)}
 * 			<li class='jm'>{@link RequestQueryParamList#addDefault(String,String) addDefault(String,String)}
 * 			<li class='jm'>{@link RequestQueryParamList#remove(String) remove(String)}
 * 			<li class='jm'>{@link RequestQueryParamList#set(HttpPart...) set(HttpPart...)}
 * 			<li class='jm'>{@link RequestQueryParamList#set(String,Object) set(String,Object)}
 * 		</ul>
 * 		<li>Other methods:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RequestQueryParamList#asQueryString() asQueryString()}
 * 			<li class='jm'>{@link RequestQueryParamList#copy() copy()}
 * 			<li class='jm'>{@link RequestQueryParamList#isEmpty() isEmpty()}
 * 		</ul>
 * 	</ul>
 * </ul>
 *
 * <p>
 * Entries are stored in a case-sensitive map unless overridden via the constructor.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link RequestQueryParam}
 * 	<li class='ja'>{@link Query}
 * 	<li class='ja'>{@link HasQuery}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/HttpParts">HTTP Parts</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S115" // Constants use UPPER_snakeCase convention
})
public class RequestQueryParamList extends ArrayList<RequestQueryParam> {

	// Argument name constants for assertArgNotNull
	private static final String ARG_parameters = "parameters";
	private static final String ARG_name = "name";
	private static final String ARG_names = "names";
	private static final String ARG_headers = "headers";

	private static final long serialVersionUID = 1L;

	private final transient RestRequest req;
	private boolean caseSensitive;
	private final transient VarResolverSession vs;
	private transient HttpPartParserSession parser;

	/**
	 * Constructor.
	 *
	 * @param req The request creating this bean.  Must not be <jk>null</jk>.
	 * @param query The raw parsed query parameter values.  Must not be <jk>null</jk>.
	 * @param caseSensitive Whether case-sensitive name matching is enabled.
	 */
	public RequestQueryParamList(RestRequest req, Map<String,String[]> query, boolean caseSensitive) {
		this.req = req;
		this.caseSensitive = caseSensitive;
		this.vs = req.getVarResolverSession();

		for (var e : query.entrySet()) {
			var name = e.getKey();

			var values = e.getValue();
			if (values == null)
				values = new String[0];

			// Fix for behavior difference between Tomcat and WAS.
			// getParameter("foo") on "&foo" in Tomcat returns "".
			// getParameter("foo") on "&foo" in WAS returns null.
			if (values.length == 1 && values[0] == null)
				values[0] = "";

			if (values.length == 0)
				values = a((String)null);

			for (var value : values)
				add(new RequestQueryParam(req, name, value));
		}
	}

	/**
	 * Copy constructor.
	 */
	private RequestQueryParamList(RequestQueryParamList copyFrom) {
		req = copyFrom.req;
		caseSensitive = copyFrom.caseSensitive;
		parser = copyFrom.parser;
		addAll(copyFrom);
		vs = copyFrom.vs;
	}

	/**
	 * Subset constructor.
	 */
	private RequestQueryParamList(RequestQueryParamList copyFrom, String...names) {
		this.req = copyFrom.req;
		caseSensitive = copyFrom.caseSensitive;
		parser = copyFrom.parser;
		vs = copyFrom.vs;
		for (var n : names)
			copyFrom.stream().filter(x -> eq(x.getName(), n)).forEach(this::add);
	}

	/**
	 * Adds request parameter values.
	 *
	 * <p>
	 * Parameters are added to the end.
	 * <br>Existing parameters with the same name are not changed.
	 *
	 * @param parameters The parameter objects.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public RequestQueryParamList add(HttpPart...parameters) {
		assertArgNotNull(ARG_parameters, parameters);
		for (var p : parameters)
			if (nn(p))
				add(p.getName(), p.getValue());
		return this;
	}

	/**
	 * Adds a parameter value.
	 *
	 * <p>
	 * Parameter is added to the end.
	 * <br>Existing parameter with the same name are not changed.
	 *
	 * @param name The parameter name.  Must not be <jk>null</jk>.
	 * @param value The parameter value.
	 * @return This object.
	 */
	public RequestQueryParamList add(String name, Object value) {
		assertArgNotNull(ARG_name, name);
		add(new RequestQueryParam(req, name, s(value)).parser(parser));
		return this;
	}

	/**
	 * Adds default entries to these parameters.
	 *
	 * <p>
	 * Similar to {@link #set(String, Object)} but doesn't override existing values.
	 *
	 * @param pairs
	 * 	The default entries.
	 * 	<br>Can be <jk>null</jk> (no-op).
	 * @return This object.
	 */
	public RequestQueryParamList addDefault(List<? extends HttpPart> pairs) {
		if (pairs == null)
			return this;
		for (var p : pairs) {
			var name = p.getName();
			var l = stream(name);
			var hasAllBlanks = l.allMatch(x -> Shorts.ie(x.getValue()));
			if (hasAllBlanks) {
				removeAll(getAll(name));
				add(new RequestQueryParam(req, name, vs.resolve(p.getValue())));
			}
		}
		return this;
	}

	/**
	 * Adds default entries to these parameters.
	 *
	 * <p>
	 * Similar to {@link #set(String, Object)} but doesn't override existing values.
	 *
	 * @param pairs
	 * 	The default entries.
	 * 	<br>Can be <jk>null</jk> (no-op).
	 * @return This object.
	 */
	public RequestQueryParamList addDefault(HttpPart...pairs) {
		return addDefault(l(pairs));
	}

	/**
	 * Adds a default entry to the query parameters.
	 *
	 * @param name The name.
	 * @param value The value.
	 * @return This object.
	 */
	public RequestQueryParamList addDefault(String name, String value) {
		return addDefault(HttpStringPart.of(name, value));
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
		var sb = new StringBuilder();
		for (var e : this) {
			if (!sb.isEmpty())
				sb.append("&");
			sb.append(urlEncode(e.getName())).append('=').append(urlEncode(e.getValue()));
		}
		return sb.toString();
	}

	/**
	 * Sets case sensitivity for names in this list.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public RequestQueryParamList caseSensitive(boolean value) {
		caseSensitive = value;
		return this;
	}

	/**
	 * Returns <jk>true</jk> if the parameters with the specified name is present.
	 *
	 * @param name The parameter name.  Must not be <jk>null</jk>.
	 * @return <jk>true</jk> if the parameters with the specified names are present.
	 */
	public boolean contains(String name) {
		return stream(name).findAny().isPresent();
	}

	/**
	 * Returns <jk>true</jk> if the parameter with any of the specified names are present.
	 *
	 * @param names The parameter names.  Must not be <jk>null</jk>.
	 * @return <jk>true</jk> if the parameter with any of the specified names are present.
	 */
	public boolean containsAny(String...names) {
		assertArgNotNull(ARG_names, names);
		for (var n : names)
			if (stream(n).findAny().isPresent())
				return true;
		return false;
	}

	/**
	 * Makes a copy of these parameters.
	 *
	 * @return A new parameters object.
	 */
	public RequestQueryParamList copy() {
		return new RequestQueryParamList(this);
	}

	/**
	 * Returns the query parameter as the specified bean type.
	 *
	 * <p>
	 * Type must have a name specified via the {@link Query} annotation
	 * and a public constructor that takes in either <c>value</c> or <c>name,value</c> as strings.
	 *
	 * @param <T> The bean type to create.
	 * @param type The bean type to create.
	 * @return The bean, or {@link Optional#empty()} if the parameter is not present.
	 */
	public <T> Optional<T> get(Class<T> type) {
		var cm = req.getMarshallingSession().getClassMeta(type);
		var name = HttpParts.getName(QUERY, cm).orElseThrow(() -> rex("@Query(name) not found on class %s", cn(type)));
		return get(name).as(type);
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
	public RequestQueryParam get(String name) {
		List<RequestQueryParam> l = getAll(name);
		if (l.isEmpty())
			return new RequestQueryParam(req, name, null).parser(parser);
		if (l.size() == 1)
			return l.get(0);
		var sb = new StringBuilder(128);
		for (var i = 0; i < l.size(); i++) {
			if (i > 0)
				sb.append(", ");
			sb.append(l.get(i).getValue());
		}
		return new RequestQueryParam(req, name, sb.toString()).parser(parser);
	}

	/**
	 * Returns all the parameters with the specified name.
	 *
	 * @param name The parameter name.
	 * @return The list of all parameters with the specified name, or an empty list if none are found.
	 * 	<br>List is unmodifiable.
	 */
	public List<RequestQueryParam> getAll(String name) {
		return stream(name).toList();
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
		assertArgNotNull(ARG_name, name);
		return stream(name).findFirst().orElseGet(() -> new RequestQueryParam(req, name, null).parser(parser));
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
		assertArgNotNull(ARG_name, name);
		var v = Holder.<RequestQueryParam>empty();
		stream(name).forEach(v::set);
		return v.orElseGet(() -> new RequestQueryParam(req, name, null).parser(parser));
	}

	/**
	 * Returns all the unique header names in this list.
	 * @return The list of all unique header names in this list.
	 * 	<br>List is unmodifiable.
	 */
	public List<String> getNames() { return stream().map(RequestQueryParam::getName).map(x -> caseSensitive ? x : lcr(x)).distinct().toList(); }

	/**
	 * Locates the position/limit query arguments ({@code &amp;p=}, {@code &amp;l=}) in the query string and returns them as a {@link PageArgs} object.
	 *
	 * @return
	 * 	A new {@link PageArgs} object initialized with the query arguments, or {@link Optional#empty()} if not found.
	 */
	public Optional<PageArgs> getPageArgs() { return o(PageArgs.create(get("p").asInteger().orElse(null), get("l").asInteger().orElse(null))); }

	/**
	 * Locates the search query argument ({@code &amp;s=}) in the query string and returns them as a {@link SearchArgs} object.
	 *
	 * @return
	 * 	A new {@link SearchArgs} object initialized with the query arguments, or {@link Optional#empty()} if not found.
	 */
	public Optional<SearchArgs> getSearchArgs() { return o(SearchArgs.create(get("s").asString().orElse(null))); }

	/**
	 * Locates the sort query argument ({@code &amp;o=}) in the query string and returns them as a {@link SortArgs} object.
	 *
	 * @return
	 * 	A new {@link SortArgs} object initialized with the query arguments, or {@link Optional#empty()} if not found.
	 */
	public Optional<SortArgs> getSortArgs() { return o(SortArgs.create(get("o").asString().orElse(null))); }

	/**
	 * Returns all headers in sorted order.
	 *
	 * @return The stream of all headers in sorted order.
	 */
	public Stream<RequestQueryParam> getSorted() {
		Comparator<RequestQueryParam> x;
		if (caseSensitive)
			x = Comparator.comparing(RequestQueryParam::getName);
		else
			x = (x1, x2) -> String.CASE_INSENSITIVE_ORDER.compare(x1.getName(), x2.getName());
		return stream().sorted(x);
	}

	/**
	 * Locates the view query argument ({@code &amp;v=}) in the query string and returns them as a {@link ViewArgs} object.
	 *
	 * @return
	 * 	A new {@link ViewArgs} object initialized with the query arguments, or {@link Optional#empty()} if not found.
	 */
	public Optional<ViewArgs> getViewArgs() { return o(ViewArgs.create(get("v").asString().orElse(null))); }

	/**
	 * Sets the parser to use for part values.
	 *
	 * @param value The new value for this setting.
	 * @return This object.
	 */
	public RequestQueryParamList parser(HttpPartParserSession value) {
		parser = value;
		forEach(x -> x.parser(parser));
		return this;
	}

	/**
	 * Remove parameters.
	 *
	 * @param name The parameter names.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public RequestQueryParamList remove(String name) {
		assertArgNotNull(ARG_name, name);
		removeIf(x -> eq(x.getName(), name));
		return this;
	}

	/**
	 * Sets request header values.
	 *
	 * <p>
	 * Parameters are added to the end of the headers.
	 * <br>Any previous parameters with the same name are removed.
	 *
	 * @param parameters The parameters to set.  Must not be <jk>null</jk> or contain <jk>null</jk>.
	 * @return This object.
	 */
	public RequestQueryParamList set(HttpPart...parameters) {
		assertArgNotNull(ARG_headers, parameters);
		for (var p : parameters)
			remove(p.getName());
		for (var p : parameters)
			add(p);
		return this;
	}

	/**
	 * Sets a parameter value.
	 *
	 * <p>
	 * Parameter is added to the end.
	 * <br>Any previous parameters with the same name are removed.
	 *
	 * @param name The parameter name.  Must not be <jk>null</jk>.
	 * @param value
	 * 	The parameter value.
	 * 	<br>Converted to a string using {@link Object#toString()}.
	 * 	<br>Can be <jk>null</jk> (the parameter is set with a <jk>null</jk> value).
	 * @return This object.
	 */
	public RequestQueryParamList set(String name, Object value) {
		assertArgNotNull(ARG_name, name);
		set(new RequestQueryParam(req, name, s(value)).parser(parser));
		return this;
	}

	/**
	 * Returns all headers with the specified name.
	 *
	 * @param name The header name.
	 * @return The stream of all headers with matching names.  Never <jk>null</jk>.
	 */
	public Stream<RequestQueryParam> stream(String name) {
		return stream().filter(x -> eq(x.getName(), name));
	}

	/**
	 * Returns a copy of this object but only with the specified param names copied.
	 *
	 * @param names The list to include in the copy.
	 * @return A new list object.
	 */
	public RequestQueryParamList subset(String...names) {
		return new RequestQueryParamList(this, names);
	}

	protected FluentMap<String,Object> properties() {
		// @formatter:off
		var m = filteredBeanPropertyMap();
		for (var n : getNames())
			m.a(n, get(n).asString().orElse(null));
		return m;
		// @formatter:on
	}

	@Override
	public boolean equals(Object o) {
		return this == o || (o instanceof RequestQueryParamList other && super.equals(other));
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override /* Overridden from Object */
	public String toString() {
		return r(properties());
	}

	private boolean eq(String s1, String s2) {
		return Shorts.eq(! caseSensitive, s1, s2);  // NOAI
	}
}
