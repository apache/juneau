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

import org.apache.juneau.marshall.collections.*;

/**
 * The open set of server-side card type handlers. {@link #standard()} holds the built-ins ({@code html}),
 * every {@link CardTypeHandler} found through {@link java.util.ServiceLoader} (this is how
 * console-ui-freemarker contributes {@code datatables} and {@code console-output}), and falls back to the
 * generic passthrough for unknown types. {@link #standard()} is immutable; a host that adds its own types
 * works on a {@link #copy()}, so one host's types never leak into another's.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 *   CardTypeRegistry <jv>r</jv> = CardTypeRegistry.<jsm>standard</jsm>().copy()
 *     .add(<jk>new</jk> KpiCardType())          <jc>// E-21 bad name, E-22 reserved, E-20 duplicate</jc>
 *     .build();
 *   JsonMap <jv>card</jv> = <jv>r</jv>.toCard(<jv>source</jv>);   <jc>// base keys + fragment, reserved-key check</jc>
 * </p>
 *
 * @since 10.0.0
 */
public final class CardTypeRegistry {

	/**
	 * Reserved type names: {@code html} (built in), {@code datatables} and {@code console-output}
	 * and {@code run-view} (console-ui-freemarker), {@code chart} (reserved for the planned chart card; no handler yet).
	 */
	public static final Set<String> RESERVED = Set.of("html", "datatables", "console-output", "run-view", "chart");

	/** The one simple class name allowed to bind each reserved type (E-22's identity check). */
	private static final Map<String,String> RESERVED_OWNER_SIMPLE_NAMES = Map.of(
		"html", "HtmlCardType",
		"datatables", "DatatablesCardType",
		"console-output", "ConsoleOutputCardType",
		"run-view", "RunViewCardType",
		"chart", "ChartCardType"
	);

	/** Reserved body keys (E-23); {@code visibleWhen} is deliberately excluded — admitted in any card's body. */
	private static final Set<String> BASE_KEYS = Set.of("id", "type", "title", "src", "template", "ref");

	private static final CardTypeRegistry STANDARD = buildStandard();

	private final Map<String,CardTypeHandler> handlers;
	private final CardTypeHandler generic = new GenericCardType();

	private CardTypeRegistry(Map<String,CardTypeHandler> handlers) {
		this.handlers = handlers;
	}

	private static CardTypeRegistry buildStandard() {
		var b = new Builder();
		b.add(new HtmlCardType());
		for (var h : ServiceLoader.load(CardTypeHandler.class))
			b.add(h);
		return b.build();
	}

	/** @return The standard registry: {@code html} plus every ServiceLoader-discovered handler. */
	public static CardTypeRegistry standard() {
		return STANDARD;
	}

	/** @return A {@link Builder} pre-seeded with this registry's handlers, to add more on top. */
	public Builder copy() {
		var b = new Builder();
		b.handlers.putAll(handlers);
		return b;
	}

	/**
	 * @param type The card type name.
	 * @return The registered handler for {@code type}, or the generic passthrough when none is registered.
	 */
	public CardTypeHandler handler(String type) {
		return handlers.getOrDefault(type, generic);
	}

	/**
	 * @param type The card type name.
	 * @return <jk>true</jk> if a handler is registered for {@code type} (not counting the generic fallback).
	 */
	public boolean isRegistered(String type) {
		return handlers.containsKey(type);
	}

	/** @return Every registered type name. */
	public Set<String> types() {
		return Collections.unmodifiableSet(handlers.keySet());
	}

	/**
	 * The single server path for both {@code <@card>} and C3's {@code PageSpec}: validates the body (E-23),
	 * dispatches to {@link #handler(String)}, and merges the fragment flat onto the card's base keys.
	 *
	 * @param source The authored card.
	 * @return The card object: base keys plus the handler's fragment, flat.
	 * @throws IllegalArgumentException E-23 if the body sets a reserved key; or from the handler itself.
	 */
	public JsonMap toCard(CardSource source) {
		Object visibleWhen = null;
		if (source.hasJsonBody()) {
			var body = source.json();
			for (var key : body.keySet()) {
				if (BASE_KEYS.contains(key))
					throw source.error("body key '%s' is reserved; set it as an attribute.", key);
			}
			visibleWhen = body.get("visibleWhen");
		}

		var frag = handler(source.type()).toFragment(source);

		var card = new JsonMap();
		card.put("id", source.id());
		card.put("type", source.type());
		if (source.title() != null)
			card.put("title", source.title());
		if (source.src() != null)
			card.put("src", source.src());
		if (source.template() != null)
			card.put("template", source.template());
		if (visibleWhen != null)
			card.put("visibleWhen", visibleWhen);
		card.putAll(frag);
		return card;
	}

	/** Builds a {@link CardTypeRegistry} on top of an existing one, or from scratch. */
	public static final class Builder {
		private final Map<String,CardTypeHandler> handlers = new LinkedHashMap<>();

		/**
		 * Registers a handler.
		 *
		 * @param h The handler.
		 * @return This object.
		 * @throws IllegalArgumentException E-21 if {@link CardTypeHandler#type()} doesn't match the name pattern;
		 * 	E-22 if it names a reserved type this class doesn't own; E-20 if the type is already registered.
		 */
		public Builder add(CardTypeHandler h) {
			var type = h.type();
			if (type == null || ! type.matches("^[a-z][a-z0-9-]{0,31}$"))
				throw iaex("Card type '%s' must match ^[a-z][a-z0-9-]{0,31}$.", type);
			if (RESERVED.contains(type) && ! h.getClass().getSimpleName().equals(RESERVED_OWNER_SIMPLE_NAMES.get(type)))
				throw iaex("Card type '%s' is reserved and cannot be replaced; reserved: '%s'.",
					type, String.join(", ", new TreeSet<>(RESERVED)));
			if (handlers.containsKey(type))
				throw iaex("Card type '%s' is already registered by '%s'.", type, handlers.get(type).getClass().getName());
			handlers.put(type, h);
			return this;
		}

		/** @return The built, immutable {@link CardTypeRegistry}. */
		public CardTypeRegistry build() {
			return new CardTypeRegistry(Map.copyOf(handlers));
		}
	}

	/** The generic passthrough: a JSON5 body becomes the fragment verbatim; a markup body becomes a template. */
	private static final class GenericCardType implements CardTypeHandler {
		@Override public String type() { return "*"; } // never registered through Builder.add; unused as a key.
		@Override public JsonMap toFragment(CardSource source) {
			if (source.hasJsonBody())
				return source.json();
			var frag = new JsonMap();
			if (source.body() != null && ! source.body().isEmpty())
				frag.put("template", source.captureTemplate());
			return frag;
		}
	}
}
