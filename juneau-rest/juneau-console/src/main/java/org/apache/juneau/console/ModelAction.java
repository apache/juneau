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
 * A click-triggered action on an element: an icon button that calls a handler.
 *
 * <h2 class='section'>Example:</h2>
 * <p class='bjava'>
 *   ModelAction.<jsm>of</jsm>(<js>"archive"</js>).icon(<js>"archive"</js>).label(<js>"Archive"</js>).handler(<js>"orders.archive"</js>);
 * </p>
 *
 * @since 10.0.0
 */
public final class ModelAction {

	private static final Pattern ID = Pattern.compile("^[A-Za-z0-9_.-]{1,128}$");

	private final String id;
	private String icon, label, tooltip, tone;
	private HandlerRef handler;
	private Map<String,Object> payload;

	private ModelAction(String id) {
		if (id == null || ! ID.matcher(id).matches())
			throw new IllegalArgumentException("Action id '" + id + "' must match " + ID.pattern());
		this.id = id;
	}

	/**
	 * Starts an action.
	 *
	 * @param id The action id, unique per element.
	 * @return A new action.
	 * @throws IllegalArgumentException If the id is malformed.
	 */
	public static ModelAction of(String id) {
		return new ModelAction(id);
	}

	/**
	 * Returns the action id.
	 *
	 * @return The id.
	 */
	public String id() {
		return id;
	}

	/**
	 * Sets the icon (a sprite name).
	 *
	 * @param v The icon.
	 * @return This object.
	 */
	public ModelAction icon(String v) {
		icon = v;
		return this;
	}

	/**
	 * Sets the visible label.
	 *
	 * @param v The label.
	 * @return This object.
	 */
	public ModelAction label(String v) {
		label = v;
		return this;
	}

	/**
	 * Sets the tooltip.
	 *
	 * @param v The tooltip.
	 * @return This object.
	 */
	public ModelAction tooltip(String v) {
		tooltip = v;
		return this;
	}

	/**
	 * Sets the tone.
	 *
	 * @param v The tone.
	 * @return This object.
	 */
	public ModelAction tone(String v) {
		tone = v;
		return this;
	}

	/**
	 * Sets the handler by name.
	 *
	 * @param name A registered handler name or a listed built-in ({@code juneau.refresh}).
	 * @return This object.
	 * @throws IllegalArgumentException If the name is malformed.
	 */
	public ModelAction handler(String name) {
		return handler(HandlerRef.named(name));
	}

	/**
	 * Sets the handler.
	 *
	 * @param ref The handler reference.
	 * @return This object.
	 */
	public ModelAction handler(HandlerRef ref) {
		handler = Objects.requireNonNull(ref, "handler");
		return this;
	}

	/**
	 * Sets the payload merged into the event payload.
	 *
	 * @param v The payload.
	 * @return This object.
	 */
	public ModelAction payload(Map<String,Object> v) {
		payload = v == null ? null : new LinkedHashMap<>(v);
		return this;
	}

	/**
	 * Returns the handler.
	 *
	 * @return The handler, or <jk>null</jk> if not yet set.
	 */
	HandlerRef handlerRef() {
		return handler;
	}

	/**
	 * Serializes to the contract shape.
	 *
	 * @return The map, with {@code "type":"handler"}.
	 * @throws IllegalStateException If no handler was set.
	 */
	public Map<String,Object> toMap() {
		if (handler == null)
			throw new IllegalStateException("Action '" + id + "' has no handler.");
		var m = new LinkedHashMap<String,Object>();
		m.put("type", "handler");
		m.put("id", id);
		if (icon != null)
			m.put("icon", icon);
		if (label != null)
			m.put("label", label);
		if (tooltip != null)
			m.put("tooltip", tooltip);
		if (tone != null)
			m.put("tone", tone);
		m.put("handler", handler.toJson());
		if (payload != null)
			m.put("payload", payload);
		return m;
	}
}
