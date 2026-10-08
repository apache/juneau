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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;
import java.util.function.*;

import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.json5.*;
import org.apache.juneau.marshall.parser.*;

/**
 * One authored card handed to a {@link CardTypeHandler}: its attributes, its raw body, and helpers to parse the
 * body as JSON5 or capture it as a {@code <template data-card>}. Built by {@code <@card>} and by C3's
 * {@code PageSpec}; tests build it directly.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 *   CardSource <jv>s</jv> = CardSource.<jsm>create</jsm>(<js>"datatables"</js>, <js>"releases"</js>)
 *     .title(<js>"Releases"</js>)
 *     .body(<js>"{dataUrl:'/rest/releases/data', columns:[{key:'version', label:'Version'}]}"</js>)
 *     .build();
 *   JsonMap <jv>frag</jv> = CardTypeRegistry.<jsm>standard</jsm>().handler(<js>"datatables"</js>).toFragment(<jv>s</jv>);
 * </p>
 *
 * @since 10.0.0
 */
public final class CardSource {

	private final String type;
	private final String id;
	private final String title;
	private final String src;
	private final String template;
	private final String body;
	private final Map<String,Object> bodyMap;
	private final BiConsumer<String,String> templateSink;

	private boolean jsonParsed;
	private JsonMap cachedJson;

	private CardSource(Builder b) {
		type = b.type;
		id = b.id;
		title = b.title;
		src = b.src;
		template = b.template;
		body = b.body == null ? null : b.body.trim();
		bodyMap = b.bodyMap;
		templateSink = b.templateSink;
	}

	/**
	 * Starts building a {@link CardSource}.
	 *
	 * @param type The card type name. Must not be <jk>null</jk> or blank.
	 * @param id The card id. Must not be <jk>null</jk> or blank.
	 * @return A new {@link Builder}.
	 * @throws IllegalArgumentException If {@code type} or {@code id} is <jk>null</jk> or blank.
	 */
	public static Builder create(String type, String id) {
		return new Builder(type, id);
	}

	/** @return The card type name. */
	public String type() { return type; }

	/** @return The card id. */
	public String id() { return id; }

	/** @return The card title, or <jk>null</jk>. */
	public String title() { return title; }

	/** @return The card's data-source URL, or <jk>null</jk>. */
	public String src() { return src; }

	/** @return The raw, trimmed body, or <jk>null</jk> when absent. */
	public String body() { return body; }

	/** @return The {@code template=} attribute value, or <jk>null</jk>. Package-private: an internal wiring detail. */
	String template() { return template; }

	/**
	 * @return <jk>true</jk> when {@link #bodyMap(Map)} was used, or the raw body's first non-whitespace character
	 * 	is {@code '{'}.
	 */
	public boolean hasJsonBody() {
		if (bodyMap != null)
			return true;
		return body != null && ! body.isEmpty() && body.charAt(0) == '{';
	}

	/**
	 * Parses the body as JSON5 (cached after the first call).
	 *
	 * @return The parsed body, or the {@link #bodyMap(Map)} wrapped as a {@link JsonMap} when that was used.
	 * @throws IllegalArgumentException E-25 if the body is not JSON5 shaped; E-24 if it is but fails to parse.
	 */
	public JsonMap json() {
		if (jsonParsed)
			return cachedJson;
		jsonParsed = true;
		if (bodyMap != null) {
			cachedJson = new JsonMap();
			cachedJson.putAll(bodyMap);
			return cachedJson;
		}
		if (! hasJsonBody())
			throw error("type='%s' requires a %s body; got '%s'.", type, "JSON5 object", preview());
		try {
			cachedJson = Json5Parser.DEFAULT.read(body, JsonMap.class);
		} catch (ParseException ex) {
			throw error("Card JSON5 is invalid: %s", ex.getMessage());
		}
		return cachedJson;
	}

	/** @return The pre-built map passed to {@link Builder#bodyMap(Map)} (the C3 path), or <jk>null</jk>. */
	public Map<String,Object> bodyMap() { return bodyMap; }

	/**
	 * Registers the raw body as a {@code <template data-card="{id}">} via the builder's {@link Builder#templateSink}
	 * and returns the card's own id as the template name.
	 *
	 * @return {@link #id()}.
	 * @throws IllegalArgumentException If the body is absent/blank, or no template sink was registered.
	 */
	public String captureTemplate() {
		if (body == null || body.isEmpty())
			throw error("type='%s' requires a markup body; got none.", type);
		if (templateSink == null)
			throw error("type='%s' requires a markup body, but this CardSource has no template sink.", type);
		templateSink.accept(id, body);
		return id;
	}

	/**
	 * Builds an {@link IllegalArgumentException} whose message is prefixed with {@code "<@card id='%s'> "}.
	 *
	 * @param fmt A {@link String#format(String, Object...)} pattern, using {@code %s} placeholders.
	 * @param args The format arguments.
	 * @return A new, unthrown exception (callers {@code throw} it themselves).
	 */
	public IllegalArgumentException error(String fmt, Object... args) {
		var all = new Object[args.length + 1];
		all[0] = id;
		System.arraycopy(args, 0, all, 1, args.length);
		return iaex("<@card id='%s'> " + fmt, all);
	}

	private String preview() {
		if (body == null)
			return "";
		return body.length() <= 40 ? body : body.substring(0, 40);
	}

	/** Builds a {@link CardSource}. */
	public static final class Builder {
		private final String type;
		private final String id;
		private String title;
		private String src;
		private String template;
		private String body;
		private Map<String,Object> bodyMap;
		private BiConsumer<String,String> templateSink;

		private Builder(String type, String id) {
			if (type == null || type.isBlank())
				throw iaex("CardSource type must not be null or blank.");
			if (id == null || id.isBlank())
				throw iaex("CardSource id must not be null or blank.");
			this.type = type;
			this.id = id;
		}

		/** @param v The card title. @return This object. */
		public Builder title(String v) { title = v; return this; }

		/** @param v The card's data-source URL. @return This object. */
		public Builder src(String v) { src = v; return this; }

		/** @param v The name of an existing {@code <template data-card>}. @return This object. */
		public Builder template(String v) { template = v; return this; }

		/** @param raw The raw card body (JSON5 or markup). @return This object. */
		public Builder body(String raw) { body = raw; return this; }

		/** @param m A pre-built map in place of a JSON5 body (the C3 path). @return This object. */
		public Builder bodyMap(Map<String,Object> m) { bodyMap = m; return this; }

		/** @param sink Receives {@code (id, markup)} when {@link CardSource#captureTemplate()} runs. @return This object. */
		public Builder templateSink(BiConsumer<String,String> sink) { templateSink = sink; return this; }

		/** @return The built {@link CardSource}. */
		public CardSource build() { return new CardSource(this); }
	}
}
