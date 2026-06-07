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
package org.apache.juneau.bean.rfc7807.adapter;

import org.apache.juneau.bean.rfc7807.*;
import org.apache.juneau.http.response.*;

/**
 * Static adapters that convert Juneau HTTP error types into
 * <a class="doclink" href="https://www.rfc-editor.org/rfc/rfc7807">RFC 7807</a> {@link Problem} beans.
 *
 * <p>
 * This adapter lives in {@code juneau-rest-common} (rather than the {@code juneau-bean-rfc7807} bean module) so the
 * bean module stays free of any {@code juneau-rest-common} dependency. The package is co-located with the bean's
 * package namespace so the cross-reference is discoverable from {@link Problem}'s javadoc.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>try</jk> {
 * 		...
 * 	} <jk>catch</jk> (NotFound <jv>e</jv>) {
 * 		Problem <jv>p</jv> = ProblemAdapters.<jsm>fromException</jsm>(<jv>e</jv>);
 * 		<jc>// p.getStatus() == 404, p.getTitle() == "Not Found", p.getDetail() == e.getMessage()</jc>
 * 	}
 * </p>
 *
 * <h5 class='section'>Notes:</h5>
 * <ul class='spaced-list'>
 * 	<li>{@code type} is intentionally left {@code null}. RFC 7807 &sect;3.1 defines {@code about:blank} as the default
 * 		when the field is absent; the bean serializes a {@code null} {@code type} as an omitted field rather than as
 * 		{@code "type":"about:blank"} on the wire (preserves the absent-vs-explicit distinction).
 * 	<li>{@code detail} is suppressed when the exception's message exactly matches the reason phrase &mdash; the bare
 * 		<c><jk>new</jk> NotFound()</c> case where the message would just echo {@code "Not Found"}.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link Problem}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanRfc7807">juneau-bean-rfc7807</a>
 * </ul>
 */
public final class ProblemAdapters {

	private ProblemAdapters() {}

	/**
	 * Builds a {@link Problem} from a {@link BasicHttpException} (or any subclass like {@code NotFound},
	 * {@code InternalServerError}, etc).
	 *
	 * <p>
	 * Mapping:
	 * <ul class='spaced-list'>
	 * 	<li>{@code status} &larr; {@link BasicHttpException#getStatusCode()}
	 * 	<li>{@code title} &larr; {@link org.apache.juneau.http.HttpStatusLine#getReasonPhrase()} (may be {@code null})
	 * 	<li>{@code detail} &larr; {@link BasicHttpException#getMessage()}, but only when it differs from the reason
	 * 		phrase; otherwise {@code null} so the bare <c><jk>new</jk> NotFound()</c> case omits a redundant
	 * 		{@code "detail":"Not Found"} field.
	 * 	<li>{@code type}, {@code instance} &larr; never set by this adapter (callers add them via the fluent setters
	 * 		on the returned bean if a {@code type} URI is appropriate).
	 * </ul>
	 *
	 * @param e The exception to adapt. May be <jk>null</jk>.
	 * @return A new {@link Problem} populated from the exception, or <jk>null</jk> if {@code e} is <jk>null</jk>.
	 */
	public static Problem fromException(BasicHttpException e) {
		if (e == null)
			return null;
		var statusLine = e.getStatusLine();
		var reasonPhrase = statusLine == null ? null : statusLine.getReasonPhrase();
		var message = e.getMessage();
		var detail = (message == null || message.equals(reasonPhrase)) ? null : message;
		return new Problem()
			.setStatus(e.getStatusCode())
			.setTitle(reasonPhrase)
			.setDetail(detail);
	}
}
