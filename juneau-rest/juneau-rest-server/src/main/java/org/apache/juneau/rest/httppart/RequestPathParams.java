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
import org.apache.juneau.rest.util.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.part.*;

/**
 * Represents the path parameters in an HTTP request.
 *
 *  <p>
 * 	The {@link RequestPathParams} object is the API for accessing the matched variables
 * 	and remainder on the URL path.
 * </p>
 * <p class='bjava'>
 * 	<ja>@RestPost</ja>(...)
 * 	<jk>public</jk> Object myMethod(RequestPathParams <jv>path</jv>) {...}
 * </p>
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@RestPost</ja>(..., path=<js>"/{foo}/{bar}/{baz}/*"</js>)
 * 	<jk>public void</jk> doGet(RequestPathParams <jv>path</jv>) {
 * 		<jc>// Example URL:  /123/qux/true/quux</jc>
 *
 * 		<jk>int</jk> <jv>foo</jv> = <jv>path</jv>.get(<js>"foo"</js>).asInteger().orElse(0);  <jc>// =123</jc>
 * 		String <jv>bar</jv> = <jv>path</jv>.get(<js>"bar"</js>).orElse(<jk>null</jk>);  <jc>// =qux</jc>
 * 		<jk>boolean</jk> <jv>baz</jv> = <jv>path</jv>.get(<js>"baz"</js>).asBoolean().orElse(<jk>false</jk>);  <jc>// =true</jc>
 * 		String <jv>remainder</jv> = <jv>path</jv>.getRemainder();  <jc>// =quux</jc>
 * 	}
 * </p>
 *
 * <p>
 * 	Some important methods on this class are:
 * </p>
 * <ul class='javatree'>
 * 	<li class='jc'>{@link RequestPathParams}
 * 	<ul class='spaced-list'>
 * 		<li>Methods for retrieving path parameters:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RequestPathParams#contains(String) contains(String)}
 * 			<li class='jm'>{@link RequestPathParams#containsAny(String...) containsAny(String...)}
 * 			<li class='jm'>{@link RequestPathParams#get(Class) get(Class)}
 * 			<li class='jm'>{@link RequestPathParams#get(String) get(String)}
 * 			<li class='jm'>{@link RequestPathParams#getAll(String) getAll(String)}
 * 			<li class='jm'>{@link RequestPathParams#getFirst(String) getFirst(String)}
 * 			<li class='jm'>{@link RequestPathParams#getLast(String) getLast(String)}
 * 			<li class='jm'>{@link RequestPathParams#getRemainder() getRemainder()}
 * 			<li class='jm'>{@link RequestPathParams#getRemainderUndecoded() getRemainderUndecoded()}
 * 		</ul>
 * 		<li>Methods overridding path parameters:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RequestPathParams#add(NameValuePair...) add(NameValuePair...)}
 * 			<li class='jm'>{@link RequestPathParams#add(String,Object) add(String,Object)}
 * 			<li class='jm'>{@link RequestPathParams#addDefault(List) addDefault(List)}
 * 			<li class='jm'>{@link RequestPathParams#addDefault(NameValuePair...) addDefault(NameValuePair...)}
 * 			<li class='jm'>{@link RequestPathParams#remove(String) remove(String)}
 * 			<li class='jm'>{@link RequestPathParams#set(NameValuePair...) set(NameValuePair...)}
 * 			<li class='jm'>{@link RequestPathParams#set(String,Object) set(String,Object)}
 * 		</ul>
 * 		<li>Other methods:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RequestPathParams#copy() copy()}
 * 			<li class='jm'>{@link RequestPathParams#isEmpty() isEmpty()}
 * 		</ul>
 * 	</ul>
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link RequestPathParam}
 * 	<li class='ja'>{@link org.apache.juneau.http.annotation.Path}
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.HttpParts">HTTP Parts</a>
 * </ul>
*/
public class RequestPathParams extends ArrayList<RequestPathParam> {

	private static final long serialVersionUID = 1L;

	private final RestRequest req;
	private boolean caseSensitive;
	private HttpPartParserSession parser;
	private final VarResolverSession vs;

	/**
	 * Constructor.
	 *
	 * @param session The current HTTP request session.
	 * @param req The current HTTP request.
	 * @param caseSensitive Whether case-sensitive name matching is enabled.
	 */
	public RequestPathParams(RestSession session, RestRequest req, boolean caseSensitive) {
		this.req = req;
		this.caseSensitive = caseSensitive;
		this.vs = req.getVarResolverSession();

		// Add parameters from parent context if any.
		@SuppressWarnings("unchecked")
		Map<String,String> parentVars = (Map<String,String>)req.getAttribute("juneau.pathVars").orElse(Collections.emptyMap());
		for (Map.Entry<String,String> e : parentVars.entrySet())
			add(e.getKey(), e.getValue());

		UrlPathMatch pm = session.getUrlPathMatch();
		if (pm != null) {
			for (Map.Entry<String,String> e : pm.getVars().entrySet())
				add(e.getKey(), e.getValue());
			String r = pm.getRemainder();
			if (r != null) {
				add("/**", r);
				add("/*", urlDecode(r));
			}
		}
	}

	/**
	 * Copy constructor.
	 */
	private RequestPathParams(RequestPathParams copyFrom) {
		req = copyFrom.req;
		caseSensitive = copyFrom.caseSensitive;
		parser = copyFrom.parser;
		addAll(copyFrom);
		vs = copyFrom.vs;
	}

	/**
	 * Subset constructor.
	 */
	private RequestPathParams(RequestPathParams copyFrom, String...names) {
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
	public RequestPathParams parser(HttpPartParserSession value) {
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
	public RequestPathParams caseSensitive(boolean value) {
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
	public RequestPathParams addDefault(List<NameValuePair> pairs) {
		for (NameValuePair p : pairs) {
			String name = p.getName();
			Stream<RequestPathParam> l = stream(name);
			boolean hasAllBlanks = l.allMatch(x -> StringUtils.isEmpty(x.getValue()));
			if (hasAllBlanks) {
				removeAll(getAll(name));
				add(new RequestPathParam(req, name, vs.resolve(p.getValue())));
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
	public RequestPathParams addDefault(NameValuePair...pairs) {
		return addDefault(alist(pairs));
	}

	/**
	 * Adds a default entry to the query parameters.
	 *
	 * @param name The name.
	 * @param value The value.
	 * @return This object.
	 */
	public RequestPathParams addDefault(String name, String value) {
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
	public RequestPathParams add(String name, Object value) {
		assertArgNotNull("name", name);
		add(new RequestPathParam(req, name, stringify(value)).parser(parser));
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
	public RequestPathParams add(NameValuePair...parameters) {
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
	public RequestPathParams set(String name, Object value) {
		assertArgNotNull("name", name);
		set(new RequestPathParam(req, name, stringify(value)).parser(parser));
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
	public RequestPathParams set(NameValuePair...parameters) {
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
	 * @param name The parameter name.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public RequestPathParams remove(String name) {
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
	public RequestPathParams subset(String...names) {
		return new RequestPathParams(this, names);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Convenience getters.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns <jk>true</jk> if the parameters with the specified name is present.
	 *
	 * @param name The parameter name.  Must not be <jk>null</jk>.
	 * @return <jk>true</jk> if the parameters with the specified name is present.
	 */
	public boolean contains(String name) {
		assertArgNotNull("names", name);
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
	public List<RequestPathParam> getAll(String name) {
		assertArgNotNull("name", name);
		return stream(name).collect(toList());
	}

	/**
	 * Returns all headers with the specified name.
	 *
	 * @param name The header name.
	 * @return The stream of all headers with matching names.  Never <jk>null</jk>.
	 */
	public Stream<RequestPathParam> stream(String name) {
		return stream().filter(x -> eq(x.getName(), name));
	}

	/**
	 * Returns all headers in sorted order.
	 *
	 * @return The stream of all headers in sorted order.
	 */
	public Stream<RequestPathParam> getSorted() {
		Comparator<RequestPathParam> x;
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
	 * Note that this method never returns <jk>null</jk> and that {@link RequestPathParam#isPresent()} can be used
	 * to test for the existence of the parameter.
	 *
	 * @param name The parameter name.
	 * @return The parameter.  Never <jk>null</jk>.
	 */
	public RequestPathParam getFirst(String name) {
		assertArgNotNull("name", name);
		return stream(name).findFirst().orElseGet(()->new RequestPathParam(req, name, null).parser(parser));
	}

	/**
	 * Returns the last parameter with the specified name.
	 *
	 * <p>
	 * Note that this method never returns <jk>null</jk> and that {@link RequestPathParam#isPresent()} can be used
	 * to test for the existence of the parameter.
	 *
	 * @param name The parameter name.
	 * @return The parameter.  Never <jk>null</jk>.
	 */
	public RequestPathParam getLast(String name) {
		assertArgNotNull("name", name);
		Value<RequestPathParam> v = Value.empty();
		stream(name).forEach(x -> v.set(x));
		return v.orElseGet(() -> new RequestPathParam(req, name, null).parser(parser));
	}

	/**
	 * Returns the last parameter with the specified name.
	 *
	 * <p>
	 * This is equivalent to {@link #getLast(String)}.
	 *
	 * @param name The parameter name.
	 * @return The parameter value, or {@link Optional#empty()} if it doesn't exist.
	 */
	public RequestPathParam get(String name) {
		List<RequestPathParam> l = getAll(name);
		if (l.isEmpty())
			return new RequestPathParam(req, name, null).parser(parser);
		if (l.size() == 1)
			return l.get(0);
		StringBuilder sb = new StringBuilder(128);
		for (int i = 0, j = l.size(); i < j; i++) {
			if (i > 0)
				sb.append(", ");
			sb.append(l.get(i).getValue());
		}
		return new RequestPathParam(req, name, sb.toString()).parser(parser);
	}

	/**
	 * Returns the path parameter as the specified bean type.
	 *
	 * <p>
	 * Type must have a name specified via the {@link org.apache.juneau.http.annotation.Path} annotation
	 * and a public constructor that takes in either <c>value</c> or <c>name,value</c> as strings.
	 *
	 * @param <T> The bean type to create.
	 * @param type The bean type to create.
	 * @return The bean, never <jk>null</jk>.
	 */
	public <T> Optional<T> get(Class<T> type) {
		ClassMeta<T> cm = req.getBeanSession().getClassMeta(type);
		String name = HttpParts.getName(PATH, cm).orElseThrow(()->new BasicRuntimeException("@Path(name) not found on class {0}", className(type)));
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
	public RequestPathParams copy() {
		return new RequestPathParams(this);
	}

	/**
	 * Returns the decoded remainder of the URL following any path pattern matches.
	 *
	 * <p>
	 * The behavior of path remainder is shown below given the path pattern "/foo/*":
	 * <table class='styled'>
	 * 	<tr>
	 * 		<th>URL</th>
	 * 		<th>Path Remainder</th>
	 * 	</tr>
	 * 	<tr>
	 * 		<td><c>/foo</c></td>
	 * 		<td><jk>null</jk></td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td><c>/foo/</c></td>
	 * 		<td><js>""</js></td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td><c>/foo//</c></td>
	 * 		<td><js>"/"</js></td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td><c>/foo///</c></td>
	 * 		<td><js>"//"</js></td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td><c>/foo/a/b</c></td>
	 * 		<td><js>"a/b"</js></td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td><c>/foo//a/b/</c></td>
	 * 		<td><js>"/a/b/"</js></td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td><c>/foo/a%2Fb</c></td>
	 * 		<td><js>"a/b"</js></td>
	 * 	</tr>
	 * </table>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// REST method</jc>
	 * 	<ja>@RestGet</ja>(<js>"/foo/{bar}/*"</js>)
	 * 	<jk>public</jk> String doGetById(RequestPathParams <jv>path</jv>, <jk>int</jk> <jv>bar</jv>) {
	 * 		<jk>return</jk> <jv>path</jv>.remainder().orElse(<jk>null</jk>);
	 * 	}
	 * </p>
	 *
	 * <p>
	 * The remainder can also be retrieved by calling <code>get(<js>"/**"</js>)</code>.
	 *
	 * @return The path remainder string.
	 */
	public RequestPathParam getRemainder() {
		return get("/*");

	}

	/**
	 * Same as {@link #getRemainder()} but doesn't decode characters.
	 *
	 * <p>
	 * The undecoded remainder can also be retrieved by calling <code>get(<js>"/*"</js>)</code>.
	 *
	 * @return The un-decoded path remainder.
	 */
	public RequestPathParam getRemainderUndecoded() {
		return get("/**");
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
