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
package org.apache.juneau.rest.server.console;

import org.apache.juneau.marshall.collections.*;

/**
 * The built-in {@code html} card type: markup-only (JSON5 bodies are E-25), with an optional body auto-captured
 * as a {@code <template data-card="{id}">}. A bare {@code html} card (no body) emits an empty fragment — the
 * shell paints an empty {@code .jc-card} and nothing more.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 *   CardSource <jv>s</jv> = CardSource.<jsm>create</jsm>(<js>"html"</js>, <js>"welcome"</js>)
 *     .title(<js>"Welcome"</js>)
 *     .body(<js>"&lt;p&gt;Hello!&lt;/p&gt;"</js>)
 *     .templateSink(<jv>page</jv>::captureTemplate)
 *     .build();
 *   JsonMap <jv>frag</jv> = <jk>new</jk> HtmlCardType().toFragment(<jv>s</jv>);  <jc>// {"template":"welcome"}</jc>
 * </p>
 *
 * @since 10.0.0
 */
public final class HtmlCardType implements CardTypeHandler {

	@Override
	public String type() {
		return "html";
	}

	@Override
	public JsonMap toFragment(CardSource source) {
		if (source.hasJsonBody()) {
			var body = source.body();
			var preview = body == null ? "" : (body.length() <= 40 ? body : body.substring(0, 40));
			throw source.error("type='%s' requires a %s body; got '%s'.", "html", "markup", preview);
		}
		var frag = new JsonMap();
		if (source.body() != null && ! source.body().isEmpty())
			frag.put("template", source.captureTemplate());
		return frag;
	}
}
