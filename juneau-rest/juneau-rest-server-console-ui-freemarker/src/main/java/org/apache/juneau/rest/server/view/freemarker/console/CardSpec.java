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
package org.apache.juneau.rest.server.view.freemarker.console;

import java.util.*;

/**
 * Type-agnostic card entry in the page contract: {@code id}, {@code type}, optional {@code src},
 * {@code title}, {@code template}, plus a type-specific body map. C2 extends the card grammar.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	CardSpec <jv>c</jv> = CardSpec.<jsm>html</jsm>(<js>"intro"</js>).title(<js>"Welcome"</js>).template(<js>"intro"</js>);
 * 	CardSpec <jv>t</jv> = CardSpec.<jsm>of</jsm>(<js>"datatables"</js>, <js>"releases"</js>)
 * 		.body(<js>"table"</js>, <js>"/rest/releases/data"</js>);
 * 	PageCapture.<jsm>of</jsm>(<jv>env</jv>).addCard(<jv>c</jv>, <js>"&lt;p&gt;Hello&lt;/p&gt;"</js>);
 * </p>
 *
 * @since 10.0.0
 */
public final class CardSpec {

	private final String type;
	private final String id;
	private String title;
	private String src;
	private String template;
	private boolean bare;
	private final Map<String,Object> body = new LinkedHashMap<>();

	private CardSpec(String type, String id) {
		this.type = type;
		this.id = id;
	}

	/** @param id The card id. @return A new {@code html} card. */
	public static CardSpec html(String id) {
		return new CardSpec("html", id);
	}

	/** @param type The card type. @param id The card id. @return A new card. */
	public static CardSpec of(String type, String id) {
		return new CardSpec(type, id);
	}

	/** @param v The card title. @return This object. */
	public CardSpec title(String v) { title = v; return this; }

	/** @param v A same-origin URL the card loads. @return This object. */
	public CardSpec src(String v) { src = v; return this; }

	/** @param templateId The id of a {@code <template data-card>} element. @return This object. */
	public CardSpec template(String templateId) { template = templateId; return this; }

	/** @param v Whether an html card is inserted without the {@code .jc-card} wrapper. @return This object. */
	public CardSpec bare(boolean v) { bare = v; return this; }

	/** @param key A type-specific field. @param value Its value. @return This object. */
	public CardSpec body(String key, Object value) { body.put(key, value); return this; }

	/** @return The id. */
	public String id() { return id; }

	/** @return The type. */
	public String type() { return type; }

	/** @return The template id, or <jk>null</jk>. */
	String template() { return template; }

	/** @return The contract entry, common fields first. */
	public Map<String,Object> toMap() {
		var m = new LinkedHashMap<String,Object>();
		m.put("id", id);
		m.put("type", type);
		if (title != null) m.put("title", title);
		if (src != null) m.put("src", src);
		if (template != null) m.put("template", template);
		if (bare) m.put("bare", true);
		m.putAll(body);
		return m;
	}
}
