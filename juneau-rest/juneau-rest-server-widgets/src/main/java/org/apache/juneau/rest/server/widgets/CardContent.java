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
package org.apache.juneau.rest.server.widgets;

import static org.apache.juneau.commons.utils.Shorts.*;

/**
 * A raw-markup {@link CardBody} &mdash; first-party prose or already-rendered markup hosted inside a card, emitted
 * verbatim rather than escaped.
 *
 * <h5 class='section'>Ownership contract: template engine, trusted / first-party content only</h5>
 * <p>
 * This framework is a <b>template engine</b> on this path: <b>the caller is responsible for sanitizing
 * {@link #content} before setting it; the framework performs no sanitization and emits the string exactly as
 * given.</b> Nothing in this framework neutralizes {@code <script>}, {@code <style>}, inline event handlers, or
 * {@code url(...)} sinks in this value &mdash; this is the same raw-panel-body contract this toolkit already ships
 * for a page's tab/sub-tab panel content, applied here to a card body.
 *
 * <p>
 * Accordingly, {@link #content} MUST carry <b>trusted, first-party content only</b> &mdash; markup the application
 * itself authored or rendered from a trusted, non-attacker-controlled source (e.g. hand-written card prose, or
 * HTML rendered from a first-party markdown document). {@link #content} MUST NOT carry live/remote/attacker-
 * influenceable data (a request parameter, a third-party API response, any value derived from something an
 * untrusted party can influence). Pouring such data into this sink is stored XSS in the page's own trusted origin.
 * A card built from live data must use {@link CardFieldList} instead, whose field values are emitted as escaped
 * text children, never through this body &mdash; a build-gating scanner enforces that separation for this
 * framework's own sources (see the {@code RawContentSinkScanner} test-only guard in the views module's test tree).
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S1845" // Fluent-builder setters intentionally mirror field names (Juneau DSL convention).
})
public class CardContent implements CardBody {

	/**
	 * The raw markup body. Emitted <b>verbatim</b> &mdash; unescaped &mdash; by the card emitter via the html5
	 * {@code rawText(...)} primitive (a plain {@code String} child would instead be entity-escaped and would
	 * break markup-bearing prose). See this class's javadoc for the full ownership contract.
	 */
	public String content;

	/**
	 * Creates an empty raw-markup body.
	 *
	 * @return A new {@link CardContent}.
	 */
	public static CardContent create() {
		return new CardContent();
	}

	/**
	 * Sets the raw markup body (see this class's javadoc for the full ownership contract &mdash; trusted /
	 * first-party content only, emitted verbatim).
	 *
	 * @param value The raw markup.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public CardContent content(String value) {
		content = value;
		return this;
	}

	/**
	 * Fail-closed bean validation.
	 *
	 * @throws IllegalArgumentException If {@link #content} is not set.
	 */
	@Override /* CardBody */
	public void validate() {
		if (content == null)
			throw iaex("CardContent must declare content.");
	}
}
