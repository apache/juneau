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
package org.apache.juneau.bean.rfc7807;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.net.*;
import java.util.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.marshall.*;

/**
 * <a class="doclink" href="https://www.rfc-editor.org/rfc/rfc7807">RFC 7807 &mdash; Problem Details for HTTP APIs</a>
 * bean.
 *
 * <p>
 * Represents the canonical {@code application/problem+json} document. The five standard members defined by
 * <a class="doclink" href="https://www.rfc-editor.org/rfc/rfc7807#section-3.1">&sect;3.1</a> are modelled as typed
 * properties; arbitrary <a class="doclink" href="https://www.rfc-editor.org/rfc/rfc7807#section-3.2">&sect;3.2</a>
 * extension members flatten into the top-level JSON object via a {@link BeanProp @BeanProp("*")} triplet.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	Problem <jv>problem</jv> = <jk>new</jk> Problem()
 * 		.setType(<jk>new</jk> URI(<js>"https://example.com/probs/out-of-credit"</js>))
 * 		.setTitle(<js>"You do not have enough credit."</js>)
 * 		.setStatus(403)
 * 		.setDetail(<js>"Your current balance is 30, but that costs 50."</js>)
 * 		.setInstance(<jk>new</jk> URI(<js>"/account/12345/msgs/abc"</js>))
 * 		.set(<js>"balance"</js>, 30)
 * 		.set(<js>"accounts"</js>, <jsm>list</jsm>(<js>"/account/12345"</js>, <js>"/account/67890"</js>));
 *
 * 	String <jv>json</jv> = JsonSerializer.<jsf>DEFAULT</jsf>.write(<jv>problem</jv>);
 * </p>
 *
 * <h5 class='section'>Notes:</h5>
 * <ul class='spaced-list'>
 * 	<li>The {@code type} field is nullable. {@link #getType()} returns the raw value (possibly {@code null});
 * 		{@link #getTypeOrDefault()} returns the spec default {@code "about:blank"} per
 * 		<a class="doclink" href="https://www.rfc-editor.org/rfc/rfc7807#section-3.1">RFC 7807 &sect;3.1</a> when the
 * 		field is unset. The field is never eagerly defaulted, so a fresh {@code Problem} round-trips without a
 * 		synthetic {@code "type":"about:blank"} on the wire.
 * 	<li>RFC 7807 was obsoleted by <a class="doclink" href="https://www.rfc-editor.org/rfc/rfc9457">RFC 9457</a> in
 * 		July 2023, but the data model and the {@code application/problem+json} IANA registration are unchanged.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanRfc7807">juneau-bean-rfc7807</a>
 * </ul>
 */
@Marshalled
public class Problem {

	/**
	 * The default {@code type} URI defined by
	 * <a class="doclink" href="https://www.rfc-editor.org/rfc/rfc7807#section-3.1">RFC 7807 &sect;3.1</a> for problem
	 * documents that omit the {@code type} member.
	 */
	public static final URI DEFAULT_TYPE = URI.create("about:blank");

	private URI type;
	private String title;
	private Integer status;
	private String detail;
	private URI instance;
	private Map<String,Object> extra;

	/**
	 * Convenience factory for the common &quot;build a problem from a status code + title + detail&quot; call site.
	 *
	 * <p>
	 * Populates only the {@code status}, {@code title}, and {@code detail} members. Callers wire from
	 * {@link Throwable}s or HTTP exception types themselves &mdash; for example:
	 * <p class='bjava'>
	 * 	Problem.<jsm>fromStatus</jsm>(<jv>e</jv>.getStatusCode(), <jv>e</jv>.getStatusLine().getReasonPhrase(), <jv>e</jv>.getMessage());
	 * </p>
	 *
	 * @param status The HTTP status code (e.g. {@code 404}).
	 * @param title A short, human-readable summary of the problem type. Can be <jk>null</jk>.
	 * @param detail A human-readable explanation specific to this occurrence. Can be <jk>null</jk>.
	 * @return A new {@link Problem} populated with the supplied values.
	 */
	public static Problem fromStatus(int status, String title, String detail) {
		return new Problem().setStatus(status).setTitle(title).setDetail(detail);
	}

	/**
	 * The <a class="doclink" href="https://www.rfc-editor.org/rfc/rfc7807#section-3.1">type</a> URI that identifies
	 * the problem type.
	 *
	 * <p>
	 * Returns the raw field value, which may be <jk>null</jk> when the {@code type} member is absent. Use
	 * {@link #getTypeOrDefault()} to get the spec default {@code "about:blank"} for absent values.
	 *
	 * @return The {@code type} URI, or <jk>null</jk> if not set.
	 */
	public URI getType() {
		return type;
	}

	/**
	 * Returns the {@code type} URI, defaulting to {@link #DEFAULT_TYPE} ({@code "about:blank"}) per
	 * <a class="doclink" href="https://www.rfc-editor.org/rfc/rfc7807#section-3.1">RFC 7807 &sect;3.1</a> when the
	 * field is <jk>null</jk>.
	 *
	 * <p>
	 * The underlying field is left untouched, so this method never causes a synthetic {@code "type":"about:blank"}
	 * to appear on the wire.
	 *
	 * @return The {@code type} URI, never <jk>null</jk>.
	 */
	@BeanIgnore
	public URI getTypeOrDefault() {
		return or(type, DEFAULT_TYPE);
	}

	/**
	 * Sets the {@code type} URI.
	 *
	 * @param value The new value. Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Problem setType(URI value) {
		type = value;
		return this;
	}

	/**
	 * The <a class="doclink" href="https://www.rfc-editor.org/rfc/rfc7807#section-3.1">title</a> &mdash; a short,
	 * human-readable summary of the problem type.
	 *
	 * @return The title, or <jk>null</jk> if not set.
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Sets the {@code title}.
	 *
	 * @param value The new value. Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Problem setTitle(String value) {
		title = value;
		return this;
	}

	/**
	 * The <a class="doclink" href="https://www.rfc-editor.org/rfc/rfc7807#section-3.1">status</a> &mdash; the HTTP
	 * status code originally generated by the origin server for this occurrence.
	 *
	 * <p>
	 * Modelled as {@link Integer} (boxed) because RFC 7807 declares {@code status} as OPTIONAL.
	 *
	 * @return The status code, or <jk>null</jk> if not set.
	 */
	public Integer getStatus() {
		return status;
	}

	/**
	 * Sets the {@code status} HTTP status code.
	 *
	 * @param value The new value. Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Problem setStatus(Integer value) {
		status = value;
		return this;
	}

	/**
	 * The <a class="doclink" href="https://www.rfc-editor.org/rfc/rfc7807#section-3.1">detail</a> &mdash; a
	 * human-readable explanation specific to this occurrence of the problem.
	 *
	 * @return The detail, or <jk>null</jk> if not set.
	 */
	public String getDetail() {
		return detail;
	}

	/**
	 * Sets the {@code detail}.
	 *
	 * @param value The new value. Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Problem setDetail(String value) {
		detail = value;
		return this;
	}

	/**
	 * The <a class="doclink" href="https://www.rfc-editor.org/rfc/rfc7807#section-3.1">instance</a> URI that
	 * identifies the specific occurrence of the problem.
	 *
	 * @return The {@code instance} URI, or <jk>null</jk> if not set.
	 */
	public URI getInstance() {
		return instance;
	}

	/**
	 * Sets the {@code instance} URI.
	 *
	 * @param value The new value. Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Problem setInstance(URI value) {
		instance = value;
		return this;
	}

	/**
	 * Generic <a class="doclink" href="https://www.rfc-editor.org/rfc/rfc7807#section-3.2">extension-member</a>
	 * keyset.
	 *
	 * @return All extension keys on this problem. Never <jk>null</jk>.
	 */
	@BeanProp("*")
	public Set<String> extraKeys() {
		return extra == null ? Collections.emptySet() : u(extra.keySet());
	}

	/**
	 * Generic extension-member getter.
	 *
	 * <p>
	 * Used by the bean framework to read arbitrary
	 * <a class="doclink" href="https://www.rfc-editor.org/rfc/rfc7807#section-3.2">&sect;3.2</a> extension members
	 * that flatten into the top-level JSON object.
	 *
	 * @param property The property name to retrieve. Can be <jk>null</jk> (a <jk>null</jk> key simply resolves to no value).
	 * @return The property value, or <jk>null</jk> if the property does not exist.
	 */
	@BeanProp("*")
	public Object get(String property) {
		return extra == null ? null : extra.get(property);
	}

	/**
	 * Generic extension-member setter.
	 *
	 * <p>
	 * Used by the bean framework to set arbitrary
	 * <a class="doclink" href="https://www.rfc-editor.org/rfc/rfc7807#section-3.2">&sect;3.2</a> extension members.
	 * The extension map is lazily initialised on first use.
	 *
	 * @param property The property name to set. Can be <jk>null</jk> ({@link LinkedHashMap} tolerates a <jk>null</jk> key).
	 * @param value The new value for the property. Can be <jk>null</jk>.
	 * @return This object.
	 */
	@BeanProp("*")
	public Problem set(String property, Object value) {
		if (extra == null)
			extra = new LinkedHashMap<>();
		extra.put(property, value);
		return this;
	}
}
