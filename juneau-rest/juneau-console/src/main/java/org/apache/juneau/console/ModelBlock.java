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
package org.apache.juneau.console;

import java.util.*;
import java.util.regex.*;

/**
 * The model block of one element: handler actions, lifecycle bindings and handler subscriptions.
 *
 * <p>
 * Builders that implement {@link Handled} own one of these.  Every mutator validates at once and throws
 * {@link IllegalArgumentException} naming the element.  These checks duplicate the client rules, which are the authority.
 *
 * @since 10.0.0
 */
public final class ModelBlock {

	private static final Pattern TEMPLATE = Pattern.compile("\\{(rowKey|tabId|cardId)}");
	private static final Pattern ANY_BRACE = Pattern.compile("[{}]");

	private static final Map<String,Set<String>> EVENTS = Map.of(
		"page", Set.of("loaded"),
		"card", Set.of("loaded", "rendered"),
		"table", Set.of("loaded", "rendered"),
		"row", Set.of("rendered", "expanded", "collapsed"),
		"rowdetail", Set.of("loaded", "rendered"),
		"detailtab", Set.of("loaded", "rendered", "activated"),
		"ribbonitem", Set.of("activated"),
		"pagetab", Set.of("activated"),
		"view", Set.of("activated"),
		"chart", Set.of("loaded", "rendered"));

	private static final Map<String,Set<String>> TEMPLATE_SCOPE = Map.of(
		"rowKey", Set.of("row", "rowdetail", "detailtab"),
		"tabId", Set.of("detailtab"));

	private final String kind;
	private final String elementId;
	private final Map<String,ModelAction> actions = new LinkedHashMap<>();
	private final Map<String,HandlerRef> on = new LinkedHashMap<>();
	private final List<Map.Entry<String,HandlerRef>> subscribes = new ArrayList<>();

	/**
	 * Creates a block.
	 *
	 * @param kind The element kind: page, card, table, row, rowdetail, detailtab, ribbonitem, pagetab, view or chart.
	 * @param elementId The element id, used in messages.
	 * @throws IllegalArgumentException If the kind is unknown.
	 */
	public ModelBlock(String kind, String elementId) {
		if (! EVENTS.containsKey(kind))
			throw new IllegalArgumentException("Unknown element kind '" + kind + "': " + new TreeSet<>(EVENTS.keySet()));
		this.kind = kind;
		this.elementId = elementId;
	}

	private IllegalArgumentException err(String msg) {
		return new IllegalArgumentException(kind + " '" + elementId + "': " + msg);
	}

	/**
	 * Adds an action.
	 *
	 * @param a The action; must have a handler.
	 * @return This object.
	 * @throws IllegalArgumentException If the kind takes no actions, the id repeats, or no handler is set.
	 */
	public ModelBlock action(ModelAction a) {
		if ("page".equals(kind))
			throw err("a page takes no actions.");
		if (a.handlerRef() == null)
			throw err("action '" + a.id() + "' has no handler.");
		if (actions.containsKey(a.id()))
			throw err("duplicate action id '" + a.id() + "'.");
		actions.put(a.id(), a);
		return this;
	}

	/**
	 * Binds a lifecycle event.
	 *
	 * @param event A documented event of this kind.
	 * @param handler The handler.
	 * @return This object.
	 * @throws IllegalArgumentException If the event is not documented for this kind.
	 */
	public ModelBlock on(String event, HandlerRef handler) {
		if (! EVENTS.get(kind).contains(event))
			throw err("event '" + event + "' is not one of " + new TreeSet<>(EVENTS.get(kind)) + ".");
		on.put(event, Objects.requireNonNull(handler, "handler"));
		return this;
	}

	/**
	 * Subscribes a handler to a bus topic.
	 *
	 * @param topic A topic; the key may hold {@code {rowKey}}, {@code {tabId}} or {@code {cardId}} where this kind provides them.
	 * @param handler The handler.
	 * @return This object.
	 * @throws IllegalArgumentException If the topic is invalid or a template is not available here.
	 */
	public ModelBlock subscribe(String topic, HandlerRef handler) {
		if ("page".equals(kind))
			throw err("a page takes no subscriptions.");
		if (topic == null)
			throw err("topic must not be null.");
		var m = TEMPLATE.matcher(topic);
		while (m.find()) {
			var scope = TEMPLATE_SCOPE.get(m.group(1));
			if (scope != null && ! scope.contains(kind))
				throw err("template {" + m.group(1) + "} is not available here.");
		}
		try {
			var concrete = TEMPLATE.matcher(topic).replaceAll("k");
			if (ANY_BRACE.matcher(concrete).find())
				throw new IllegalArgumentException("invalid topic '" + topic + "'");
			Topics.checkTopic(concrete);
		} catch (IllegalArgumentException e) {
			throw err(e.getMessage());
		}
		subscribes.add(Map.entry(topic, Objects.requireNonNull(handler, "handler")));
		return this;
	}

	/**
	 * Re-checks the whole block.
	 *
	 * @throws IllegalArgumentException If any entry is invalid.
	 */
	public void validate() {
		on.keySet().forEach(e -> {
			if (! EVENTS.get(kind).contains(e))
				throw err("event '" + e + "' is not one of " + new TreeSet<>(EVENTS.get(kind)) + ".");
		});
		actions.values().forEach(a -> {
			if (a.handlerRef() == null)
				throw err("action '" + a.id() + "' has no handler.");
		});
	}

	/**
	 * Appends every entry of another block to this one.
	 *
	 * @param other The block to copy from.
	 * @return This object.
	 */
	public ModelBlock addAll(ModelBlock other) {
		other.actions.values().forEach(this::action);
		other.on.forEach(this::on);
		other.subscribes.forEach(e -> subscribe(e.getKey(), e.getValue()));
		return this;
	}

	/**
	 * Returns whether the block holds nothing.
	 *
	 * @return <jk>true</jk> if empty.
	 */
	public boolean isEmpty() {
		return actions.isEmpty() && on.isEmpty() && subscribes.isEmpty();
	}

	/**
	 * Returns whether any entry uses an inline handler.
	 *
	 * @return <jk>true</jk> if an inline reference is present.
	 */
	public boolean hasInline() {
		return on.values().stream().anyMatch(HandlerRef::isInline)
			|| subscribes.stream().anyMatch(e -> e.getValue().isInline())
			|| actions.values().stream().anyMatch(a -> a.handlerRef().isInline());
	}

	/**
	 * Returns the actions in insertion order.
	 *
	 * @return The actions, as contract maps.
	 */
	public List<Map<String,Object>> actionMaps() {
		return actions.values().stream().map(ModelAction::toMap).toList();
	}

	/**
	 * Returns the element id.
	 *
	 * @return The id.
	 */
	public String elementId() {
		return elementId;
	}

	/**
	 * Returns the element kind.
	 *
	 * @return The kind.
	 */
	public String kind() {
		return kind;
	}

	/**
	 * Serializes to the contract {@code model} object.
	 *
	 * @return The map, or <jk>null</jk> when empty so that builders not using the model emit unchanged JSON.
	 */
	public Map<String,Object> toMap() {
		if (isEmpty())
			return null;
		var m = new LinkedHashMap<String,Object>();
		if (! actions.isEmpty())
			m.put("actions", actionMaps());
		if (! on.isEmpty()) {
			var o = new LinkedHashMap<String,Object>();
			on.forEach((k, v) -> o.put(k, v.toJson()));
			m.put("on", o);
		}
		if (! subscribes.isEmpty()) {
			var l = new ArrayList<Map<String,Object>>();
			for (var e : subscribes) {
				var s = new LinkedHashMap<String,Object>();
				s.put("topic", e.getKey());
				s.put("handler", e.getValue().toJson());
				l.add(s);
			}
			m.put("subscribes", l);
		}
		return m;
	}
}
