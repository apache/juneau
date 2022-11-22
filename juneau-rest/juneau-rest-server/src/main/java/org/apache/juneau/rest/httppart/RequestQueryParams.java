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
import static org.apache.juneau.common.internal.ArgUtils.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.httppart.HttpPartType.*;
import static java.util.Optional.*;
import static java.util.stream.Collectors.*;

import java.util.*;
import java.util.stream.*;

import org.apache.http.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.objecttools.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.part.*;

/**
 * Represents the query parameters in an HTTP request.
 *
 * <p>
 * 	The {@link RequestQueryParams} object is the API for accessing the GET query parameters of an HTTP request.
 * 	It can be accessed by passing it as a parameter on your REST Java method:
 * </p>
 * <p class='bjava'>
 * 	<ja>@RestPost</ja>(...)
 * 	<jk>public</jk> Object myMethod(RequestQueryParams <jv>query</jv>) {...}
 * </p>
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@RestPost</ja>(...)
 * 	<jk>public</jk> Object myMethod(RequestQueryParams <jv>query</jv>) {
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
 * 	<li class='jc'>{@link RequestQueryParams}
 * 	<ul class='spaced-list'>
 * 		<li>Methods for retrieving query parameters:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RequestQueryParams#contains(String) contains(String)}
 * 			<li class='jm'>{@link RequestQueryParams#containsAny(String...) containsAny(String...)}
 * 			<li class='jm'>{@link RequestQueryParams#get(Class) get(Class)}
 * 			<li class='jm'>{@link RequestQueryParams#get(String) get(String)}
 * 			<li class='jm'>{@link RequestQueryParams#getAll(String) getAll(String)}
 * 			<li class='jm'>{@link RequestQueryParams#getFirst(String) getFirst(String)}
 * 			<li class='jm'>{@link RequestQueryParams#getLast(String) getLast(String)}
 * 			<li class='jm'>{@link RequestQueryParams#getSearchArgs() getSearchArgs()}
 * 			<li class='jm'>{@link RequestQueryParams#getViewArgs() getViewArgs()}
 * 			<li class='jm'>{@link RequestQueryParams#getSortArgs() getSortArgs()}
 * 			<li class='jm'>{@link RequestQueryParams#getPageArgs() getPageArgs()}
 * 		</ul>
 * 		<li>Methods overridding query parameters:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RequestQueryParams#add(NameValuePair...) add(NameValuePair...)}
 * 			<li class='jm'>{@link RequestQueryParams#add(String,Object) add(String,Object)}
 * 			<li class='jm'>{@link RequestQueryParams#addDefault(List) addDefault(List)}
 * 			<li class='jm'>{@link RequestQueryParams#addDefault(NameValuePair...) addDefault(NameValuePair...)}
 * 			<li class='jm'>{@link RequestQueryParams#addDefault(String,String) addDefault(String,String)}
 * 			<li class='jm'>{@link RequestQueryParams#remove(String) remove(String)}
 * 			<li class='jm'>{@link RequestQueryParams#set(NameValuePair...) set(NameValuePair...)}
 * 			<li class='jm'>{@link RequestQueryParams#set(String,Object) set(String,Object)}
 * 		</ul>
 * 		<li>Other methods:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RequestQueryParams#asQueryString() asQueryString()}
 * 			<li class='jm'>{@link RequestQueryParams#copy() copy()}
 * 			<li class='jm'>{@link RequestQueryParams#isEmpty() isEmpty()}
 * 		</ul>
 * 	</ul>
 * </ul>
 *
 * <p>
 * Entries are stored in a case-sensitive map unless overridden via the constructor.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link RequestQueryParam}
 * 	<li class='ja'>{@link org.apache.juneau.http.annotation.Query}
 * 	<li class='ja'>{@link org.apache.juneau.http.annotation.HasQuery}
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.HttpParts">HTTP Parts</a>
 * </ul>
 */
public class RequestQueryParams extends ArrayList<RequestQueryParam> {

	private static final long serialVersionUID = 1L;

	private final RestRequest req;
	private boolean caseSensitive;
	private final VarResolverSession vs;
	private HttpPartParserSession parser;

	/**
	 * Constructor.
	 *
	 * @param req The request creating this bean.
	 * @param query The raw parsed query parameter values.
	 * @param caseSensitive Whether case-sensitive name matching is enabled.
	 */
	public RequestQueryParams(RestRequest req, Map<String,String[]> query, boolean caseSensitive) {
		this.req = req;
		this.caseSensitive = caseSensitive;
		this.vs = req.getVarResolverSession();

		for (Map.Entry<String,String[]> e : query.entrySet()) {
			String name = e.getKey();

			String[] values = e.getValue();
			if (values == null)
				values = new String[0];

			// Fix for behavior difference between Tomcat and WAS.
			// getParameter("foo") on "&foo" in Tomcat returns "".
			// getParameter("foo") on "&foo" in WAS returns null.
			if (values.length == 1 && values[0] == null)
				values[0] = "";

			if (values.length == 0)
				values = new String[]{null};

			for (String value : values)
				add(new RequestQueryParam(req, name, value));
		}
	}

	/**
	 * Copy constructor.
	 */
	private RequestQueryParams(RequestQueryParams copyFrom) {
		req = copyFrom.req;
		caseSensitive = copyFrom.caseSensitive;
		parser = copyFrom.parser;
		addAll(copyFrom);
		vs = copyFrom.vs;
	}

	/**
	 * Subset constructor.
	 */
	private RequestQueryParams(RequestQueryParams copyFrom, String...names) {
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
	public RequestQueryParams parser(HttpPartParserSession value) {
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
	public RequestQueryParams caseSensitive(boolean value) {
		this.caseSensitive = value;
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Basic operations.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Adds default entries to these parameters.
	 *
	 * <p>
	 * Similar to {@link #set(String, Object)} but doesn't override existing values.
	 *
	 * @param pairs
	 * 	The default entries.
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object.
	 */
	public RequestQueryParams addDefault(List<? extends NameValuePair> pairs) {
		for (NameValuePair p : pairs) {
			String name = p.getName();
			Stream<RequestQueryParam> l = stream(name);
			boolean hasAllBlanks = l.allMatch(x -> StringUtils.isEmpty(x.getValue()));
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
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object.
	 */
	public RequestQueryParams addDefault(NameValuePair...pairs) {
		return addDefault(alist(pairs));
	}

	/**
	 * Adds a default entry to the query parameters.
	 *
	 * @param name The name.
	 * @param value The value.
	 * @return This object.
	 */
	public RequestQueryParams addDefault(String name, String value) {
		return addDefault(BasicStringPart.of(name, value));
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
	public RequestQueryParams add(String name, Object value) {
		assertArgNotNull("name", name);
		add(new RequestQueryParam(req, name, stringify(value)).parser(parser));
		return this;
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
	public RequestQueryParams add(NameValuePair...parameters) {
		assertArgNotNull("parameters", parameters);
		for (NameValuePair p : parameters)
			if (p != null)
				add(p.getName(), p.getValue());
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
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object.
	 */
	public RequestQueryParams set(String name, Object value) {
		assertArgNotNull("name", name);
		set(new RequestQueryParam(req, name, stringify(value)).parser(parser));
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
	public RequestQueryParams set(NameValuePair...parameters) {
		assertArgNotNull("headers", parameters);
		for (NameValuePair p : parameters)
			remove(p);
		for (NameValuePair p : parameters)
			add(p);
		return this;
	}

	/**
	 * Remove parameters.
	 *
	 * @param name The parameter names.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public RequestQueryParams remove(String name) {
		assertArgNotNull("name", name);
		removeIf(x -> eq(x.getName(), name));
		return this;
	}

	/**
	 * Returns a copy of this object but only with the specified param names copied.
	 *
	 * @param names The list to include in the copy.
	 * @return A new list object.
	 */
	public RequestQueryParams subset(String...names) {
		return new RequestQueryParams(this, names);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Convenience getters.
	//-----------------------------------------------------------------------------------------------------------------

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
		assertArgNotNull("names", names);
		for (String n : names)
			if (stream(n).findAny().isPresent())
				return true;
		return false;
	}

	/**
	 * Returns all the parameters with the specified name.
	 *
	 * @param name The parameter name.
	 * @return The list of all parameters with the specified name, or an empty list if none are found.
	 */
	public List<RequestQueryParam> getAll(String name) {
		return stream(name).collect(toList());
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
	 * Returns all headers in sorted order.
	 *
	 * @return The stream of all headers in sorted order.
	 */
	public Stream<RequestQueryParam> getSorted() {
		Comparator<RequestQueryParam> x;
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
		return stream(name).findFirst().orElseGet(()->new RequestQueryParam(req, name, null).parser(parser));
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
		Value<RequestQueryParam> v = Value.empty();
		stream(name).forEach(x -> v.set(x));
		return v.orElseGet(() -> new RequestQueryParam(req, name, null).parser(parser));
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
		StringBuilder sb = new StringBuilder(128);
		for (int i = 0, j = l.size(); i < j; i++) {
			if (i > 0)
				sb.append(", ");
			sb.append(l.get(i).getValue());
		}
		return new RequestQueryParam(req, name, sb.toString()).parser(parser);
	}

	/**
	 * Returns the query parameter as the specified bean type.
	 *
	 * <p>
	 * Type must have a name specified via the {@link org.apache.juneau.http.annotation.Query} annotation
	 * and a public constructor that takes in either <c>value</c> or <c>name,value</c> as strings.
	 *
	 * @param <T> The bean type to create.
	 * @param type The bean type to create.
	 * @return The bean, never <jk>null</jk>.
	 */
	public <T> Optional<T> get(Class<T> type) {
		ClassMeta<T> cm = req.getBeanSession().getClassMeta(type);
		String name = HttpParts.getName(QUERY, cm).orElseThrow(()->new BasicRuntimeException("@Query(name) not found on class {0}", className(type)));
		return get(name).as(type);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

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
		for (RequestQueryParam e : this) {
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
	 * Locates the search query argument ({@code &amp;s=}) in the query string and returns them as a {@link SearchArgs} object.
	 *
	 * @return
	 * 	A new {@link SearchArgs} object initialized with the query arguments, or {@link Optional#empty()} if not found.
	 */
	public Optional<SearchArgs> getSearchArgs() {
		return ofNullable(SearchArgs.create(get("s").asString().orElse(null)));
	}

	/**
	 * Locates the view query argument ({@code &amp;v=}) in the query string and returns them as a {@link ViewArgs} object.
	 *
	 * @return
	 * 	A new {@link ViewArgs} object initialized with the query arguments, or {@link Optional#empty()} if not found.
	 */
	public Optional<ViewArgs> getViewArgs() {
		return ofNullable(ViewArgs.create(get("v").asString().orElse(null)));
	}

	/**
	 * Locates the sort query argument ({@code &amp;o=}) in the query string and returns them as a {@link SortArgs} object.
	 *
	 * @return
	 * 	A new {@link SortArgs} object initialized with the query arguments, or {@link Optional#empty()} if not found.
	 */
	public Optional<SortArgs> getSortArgs() {
		return ofNullable(SortArgs.create(get("o").asString().orElse(null)));
	}

	/**
	 * Locates the position/limit query arguments ({@code &amp;p=}, {@code &amp;l=}) in the query string and returns them as a {@link PageArgs} object.
	 *
	 * @return
	 * 	A new {@link PageArgs} object initialized with the query arguments, or {@link Optional#empty()} if not found.
	 */
	public Optional<PageArgs> getPageArgs() {
		return ofNullable(PageArgs.create(get("p").asInteger().orElse(null), get("l").asInteger().orElse(null)));
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
