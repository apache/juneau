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
package org.apache.juneau.rest.server.views;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.juneau.marshall.collections.*;

/**
 * One ribbon (toolbar) entry above a table.
 *
 * <p>
 * The ribbon runtime ({@code juneau-ribbon.js}) implements every behavior; this type only names it and carries its
 * parameters.  {@link #toMap()} produces the same {@code {type, ...}} entry the runtime reads from {@code viewDef.ribbon}.
 *
 * <p>
 * Setters are not gated per item type, but {@link #toMap()} fails loudly rather than emit a key the runtime ignores
 * for that type (for example {@code form} on a {@code refresh}, or {@code title} on an {@code optionGroup}), and
 * checks the invariants the runtime cannot: a top-level {@code option} needs {@code column} or {@code param} (exactly
 * one) plus a {@code value}, and a group's {@code default} must name a member.  Group members take only
 * {@code id}, {@code title}, {@code symbol}, {@code column}, {@code param} and {@code value}.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jv>table</jv>.ribbon(
 * 		RibbonItem.<jsm>collapseAll</jsm>(),
 * 		RibbonItem.<jsm>export</jsm>(<js>"copy"</js>, <js>"csv"</js>).optional(<js>"excel"</js>, <js>"pdf"</js>),
 * 		RibbonItem.<jsm>option</jsm>(<js>"dropped-only"</js>).title(<js>"Dropped only"</js>).column(<js>"status"</js>).value(<js>"$eq(DROPPED)"</js>).persist(<jk>true</jk>),
 * 		RibbonItem.<jsm>optionGroup</jsm>(<js>"phase"</js>,
 * 			RibbonItem.<jsm>option</jsm>(<js>"pending"</js>).column(<js>"phase"</js>).value(<js>"$in(Waiting)"</js>),
 * 			RibbonItem.<jsm>option</jsm>(<js>"done"</js>).column(<js>"phase"</js>).value(<js>"$in(Completed)"</js>)
 * 		).defaultOption(<js>"pending"</js>),
 * 		RibbonItem.<jsm>dialog</jsm>(<js>"add"</js>).title(<js>"Add repo"</js>).form(<js>"/rest/skill-repos/add-modal"</js>)
 * 			.endpoint(<js>"/rest/skill-repos/new"</js>).method(RowAction.Method.<jsf>POST</jsf>)
 * 			.onSuccess(RowAction.OnSuccess.<jsf>REDRAW</jsf>),
 * 		RibbonItem.<jsm>refresh</jsm>());
 *
 * 	RibbonItem.<jsm>refresh</jsm>().title(<js>"Refresh tasks"</js>).target(<js>"tasks"</js>);
 * 	RibbonItem.<jsm>publish</jsm>(<js>"Focus east"</js>, <js>"app.region-picked"</js>, Map.<jsm>of</jsm>(<js>"region"</js>, <js>"east"</js>));
 * </p>
 *
 * @since 10.0.0
 */
public final class RibbonItem {

	private static final List<String> EXPORT_BUTTONS = List.of("copy", "csv", "print", "excel", "pdf");
	private static final List<String> EXPORT_OPTIONAL = List.of("excel", "pdf");
	private static final List<String> APPEARANCES = List.of("icon");

	// The keys juneau-ribbon.js reads for each item type (everything else is ignored there, so toMap() rejects it).
	private static final Set<String> COMMON = Set.of("type", "id", "title", "group", "appearance", "symbol");
	private static final Map<String,Set<String>> ALLOWED = Map.of(
		"refresh", union(COMMON, "target"),
		"collapseAll", union(COMMON, "target"),
		"pausePolling", union(COMMON, "target"),
		"dialog", union(COMMON, "form", "endpoint", "method", "onSuccess"),
		"export", Set.of("type", "group", "appearance", "buttons", "optional"),
		"option", union(COMMON, "column", "param", "value", "persist", "default", "target"),
		"optionGroup", Set.of("type", "id", "appearance", "persist", "default", "deselectable", "options", "target"),
		"publish", union(COMMON, "topic", "payload")
	);
	private static final java.util.regex.Pattern CARD_ID = java.util.regex.Pattern.compile("^[A-Za-z][A-Za-z0-9_-]{0,63}$");
	private static final Set<String> MEMBER_ALLOWED = Set.of("id", "title", "symbol", "column", "param", "value");

	private final String type;
	private String id;
	private String title;
	private String group;
	private String appearance;
	private String symbol;
	private String form;
	private String endpoint;
	private String method;
	private String onSuccess;
	private List<String> buttons;
	private List<String> optional;
	private String column;
	private String param;
	private String value;
	private Boolean persist;
	private Object dflt;  // Boolean for an option, String (member id) for an optionGroup.
	private Boolean deselectable;
	private List<RibbonItem> options;
	private String target;
	private String topic;
	private Map<String,Object> payload;

	private RibbonItem(String type) {
		this.type = type;
	}

	/**
	 * The ribbon's reload-the-table button.
	 *
	 * @return A new {@link RibbonItem}.
	 */
	public static RibbonItem refresh() {
		return new RibbonItem("refresh");
	}

	/**
	 * The ribbon's collapse-all-detail-rows button.
	 *
	 * @return A new {@link RibbonItem}.
	 */
	public static RibbonItem collapseAll() {
		return new RibbonItem("collapseAll");
	}

	/**
	 * The ribbon's pause/resume auto-refresh toggle.
	 *
	 * @return A new {@link RibbonItem}.
	 */
	public static RibbonItem pausePolling() {
		return new RibbonItem("pausePolling");
	}

	/**
	 * The ribbon's export menu, offering the named buttons.
	 *
	 * <p>
	 * A button here is required: if its library is missing at runtime (JSZip for {@code excel}, pdfMake for
	 * {@code pdf}) it renders disabled and logs an error.  Use {@link #optional(String...)} for buttons that should
	 * simply be omitted when their library is absent.  The menu may consist of optional buttons alone, but
	 * {@link #toMap()} rejects one with no buttons at all.
	 *
	 * @param buttons The required buttons, each one of {@code copy}, {@code csv}, {@code print}, {@code excel} or
	 * 	{@code pdf}.
	 * @return A new {@link RibbonItem}.
	 * @throws IllegalArgumentException If {@code buttons} names a button not in that set.
	 */
	public static RibbonItem export(String...buttons) {
		for (var b : buttons)
			checkOneOf(b, EXPORT_BUTTONS, "export button");
		var r = new RibbonItem("export");
		r.buttons = buttons.length == 0 ? null : List.of(buttons);
		return r;
	}

	/**
	 * A ribbon button that opens a modal dialog.
	 *
	 * @param actionId This item's stable id (also the dialog's logical name).  Must not be <jk>null</jk> or blank.
	 * @return A new {@link RibbonItem}.
	 * @throws IllegalArgumentException If {@code actionId} is <jk>null</jk> or blank.
	 */
	public static RibbonItem dialog(String actionId) {
		if (actionId == null || actionId.isBlank())
			throw iaex("RibbonItem dialog action id must not be null or blank.");
		var r = new RibbonItem("dialog");
		r.id = actionId;
		return r;
	}

	/**
	 * A server-query toggle button.
	 *
	 * <p>
	 * Set exactly one of {@link #column(String)} (a BeanQuery {@code $}-expression {@link #value(String) value}
	 * against that column) or {@link #param(String)} (a URL query parameter).  Checked by {@link #toMap()}.
	 *
	 * @param id The toggle's stable id.  Must not be <jk>null</jk> or blank.
	 * @return A new {@link RibbonItem}.
	 * @throws IllegalArgumentException If {@code id} is <jk>null</jk> or blank.
	 */
	public static RibbonItem option(String id) {
		if (id == null || id.isBlank())
			throw iaex("RibbonItem option id must not be null or blank.");
		var r = new RibbonItem("option");
		r.id = id;
		return r;
	}

	/**
	 * A group of mutually exclusive {@link #option(String) option} toggles.
	 *
	 * <p>
	 * A member may omit {@code column}/{@code param}/{@code value}; selecting it then applies no filter (an "All"
	 * member).
	 *
	 * @param id The group's stable id.  Must not be <jk>null</jk> or blank.
	 * @param members The members, each created by {@link #option(String)}.  Must not be empty.
	 * @return A new {@link RibbonItem}.
	 * @throws IllegalArgumentException If {@code id} is <jk>null</jk> or blank, or {@code members} is empty or
	 * 	holds anything other than an {@code option}.
	 */
	public static RibbonItem optionGroup(String id, RibbonItem...members) {
		if (id == null || id.isBlank())
			throw iaex("RibbonItem optionGroup id must not be null or blank.");
		if (members.length == 0)
			throw iaex("RibbonItem optionGroup '%s' requires at least one option.", id);
		for (var m : members)
			if (! "option".equals(m.type))
				throw iaex("RibbonItem optionGroup '%s' member must be an option, not '%s'.", id, m.type);
		var r = new RibbonItem("optionGroup");
		r.id = id;
		r.options = List.of(members);
		return r;
	}

	/**
	 * A ribbon button that publishes a message when clicked.  The topic must be a custom topic declared in the
	 * page's {@code topics} or a card's {@code publishes}, or {@code cmd:<cardId>}; the console's wiring check
	 * enforces that.  Rendered only on a console page (no bus, no button).
	 *
	 * @param title The button label.
	 * @param topic The topic to publish.
	 * @param payload A JSON-serializable payload, or <jk>null</jk> for <code>{}</code>.
	 * @return A new {@link RibbonItem}.
	 * @throws IllegalArgumentException If {@code title} or {@code topic} is <jk>null</jk> or blank.
	 */
	public static RibbonItem publish(String title, String topic, Map<String,Object> payload) {
		if (title == null || title.isBlank())
			throw iaex("RibbonItem publish requires a title.");
		if (topic == null || topic.isBlank())
			throw iaex("RibbonItem publish '%s' requires a topic.", title);
		var i = new RibbonItem("publish");
		i.title = title;
		i.topic = topic;
		i.payload = payload == null ? null : new LinkedHashMap<>(payload);
		return i;
	}

	/**
	 * Sends this item's command to another card instead of its own table.  Accepted on {@code refresh},
	 * {@code collapseAll}, {@code pausePolling}, {@code option} and {@code optionGroup}; {@link #toMap()} rejects it
	 * elsewhere.
	 *
	 * @param cardId The target card id; the console's wiring check verifies it names a card that handles the op.
	 * @return This object.
	 * @throws IllegalArgumentException If {@code cardId} is not a valid card id.
	 */
	public RibbonItem target(String cardId) {
		if (cardId == null || ! CARD_ID.matcher(cardId).matches())
			throw iaex("RibbonItem target '%s' must match ^[A-Za-z][A-Za-z0-9_-]{0,63}$.", cardId);
		target = cardId;
		return this;
	}

	/**
	 * Sets this item's title/label text.
	 *
	 * @param v The title.  Can be <jk>null</jk> to unset.
	 * @return This object.
	 */
	public RibbonItem title(String v) {
		title = v;
		return this;
	}

	/**
	 * Clusters this item with adjacent items sharing the same group name.
	 *
	 * @param v The group name.  Can be <jk>null</jk> to unset.
	 * @return This object.
	 */
	public RibbonItem group(String v) {
		group = v;
		return this;
	}

	/**
	 * Sets how this item's button is drawn.
	 *
	 * @param v The appearance; currently only {@code icon}.  Can be <jk>null</jk> to unset.
	 * @return This object.
	 * @throws IllegalArgumentException If {@code v} is not a known appearance.
	 */
	public RibbonItem appearance(String v) {
		if (v != null)
			checkOneOf(v, APPEARANCES, "appearance");
		appearance = v;
		return this;
	}

	/**
	 * Sets the icon name overriding this item's default icon.
	 *
	 * @param v The icon name.  Can be <jk>null</jk> to unset.
	 * @return This object.
	 */
	public RibbonItem symbol(String v) {
		symbol = v;
		return this;
	}

	/**
	 * Sets the form-source URL supplying a dialog item's input fields.
	 *
	 * @param url The form URL.  Can be <jk>null</jk> to unset.
	 * @return This object.
	 */
	public RibbonItem form(String url) {
		form = url;
		return this;
	}

	/**
	 * Sets the URL a dialog item's form submits to.
	 *
	 * @param url The endpoint URL.  Can be <jk>null</jk> to unset.
	 * @return This object.
	 */
	public RibbonItem endpoint(String url) {
		endpoint = url;
		return this;
	}

	/**
	 * Sets the non-safe HTTP method a dialog item's form submits with.
	 *
	 * @param m The method.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public RibbonItem method(RowAction.Method m) {
		if (m == null)
			throw iaex("RibbonItem method must not be null.");
		method = m.wire();
		return this;
	}

	/**
	 * Sets what the runtime does with a successful dialog submission.
	 *
	 * @param s The success behavior.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public RibbonItem onSuccess(RowAction.OnSuccess s) {
		if (s == null)
			throw iaex("RibbonItem onSuccess must not be null.");
		onSuccess = s.wire();
		return this;
	}

	/**
	 * Adds export buttons that are omitted, rather than rendered disabled, when their library is absent.
	 *
	 * @param names The buttons, each {@code excel} or {@code pdf}.
	 * @return This object.
	 * @throws IllegalArgumentException If a name is not one of those, or is already a required button.
	 */
	public RibbonItem optional(String...names) {
		for (var n : names) {
			checkOneOf(n, EXPORT_OPTIONAL, "optional export button");
			if (buttons != null && buttons.contains(n))
				throw iaex("RibbonItem export button '%s' is both required and optional.", n);
		}
		optional = names.length == 0 ? null : List.of(names);
		return this;
	}

	/**
	 * Scopes an {@code option} to a table column.
	 *
	 * @param name The row-data column name.  Can be <jk>null</jk> to unset.
	 * @return This object.
	 */
	public RibbonItem column(String name) {
		column = name;
		return this;
	}

	/**
	 * Scopes an {@code option} to a URL query parameter.
	 *
	 * @param name The parameter name.  Can be <jk>null</jk> to unset.
	 * @return This object.
	 */
	public RibbonItem param(String name) {
		param = name;
		return this;
	}

	/**
	 * Sets an {@code option}'s value: a BeanQuery {@code $}-expression for a {@link #column(String) column}-scoped
	 * option, or the raw parameter value for a {@link #param(String) param}-scoped one.
	 *
	 * @param v The value.  Can be <jk>null</jk> to unset.
	 * @return This object.
	 */
	public RibbonItem value(String v) {
		value = v;
		return this;
	}

	/**
	 * Sets whether this option's (or group's) state is remembered across page loads.
	 *
	 * @param v Whether to persist.
	 * @return This object.
	 */
	public RibbonItem persist(boolean v) {
		persist = v;
		return this;
	}

	/**
	 * Marks an {@code option} as on until the user (or persisted state) says otherwise.
	 *
	 * @return This object.
	 */
	public RibbonItem defaultOn() {
		dflt = Boolean.TRUE;
		return this;
	}

	/**
	 * Selects an {@code optionGroup} member until the user (or persisted state) says otherwise.
	 *
	 * @param memberId The member's id; checked against the members by {@link #toMap()}.
	 * @return This object.
	 * @throws IllegalArgumentException If this item is a plain {@code option} (use {@link #defaultOn()}).
	 */
	public RibbonItem defaultOption(String memberId) {
		if ("option".equals(type))
			throw iaex("RibbonItem option '%s' cannot take defaultOption(); use defaultOn().", id);
		dflt = memberId;
		return this;
	}

	/**
	 * Lets an {@code optionGroup}'s selected member be clicked again to deselect it.
	 *
	 * @param v Whether the group is deselectable.
	 * @return This object.
	 */
	public RibbonItem deselectable(boolean v) {
		deselectable = v;
		return this;
	}

	/**
	 * Builds this item's ribbon entry.
	 *
	 * @return The entry, {@code type} first, then only the keys that were set.
	 * @throws IllegalArgumentException If the item sets a key the runtime ignores for its type, a top-level
	 * 	{@code option} sets neither or both of {@code column} and {@code param} or no {@code value}, an
	 * 	{@code export} has no buttons, or a {@code default} does not suit the item (an {@code option} takes
	 * 	{@link #defaultOn()}; an {@code optionGroup} takes {@link #defaultOption(String)} naming a member), or a
	 * 	{@code target} is set on a type the runtime does not route.
	 */
	public JsonMap toMap() {
		return toMap(null);
	}

	private JsonMap toMap(RibbonItem parent) {
		var withType = parent == null;
		var m = new JsonMap();
		if (withType)
			m.put("type", type);
		if (id != null)
			m.put("id", id);
		if (title != null)
			m.put("title", title);
		if (group != null)
			m.put("group", group);
		if (appearance != null)
			m.put("appearance", appearance);
		if (symbol != null)
			m.put("symbol", symbol);
		if (topic != null)
			m.put("topic", topic);
		if (payload != null)
			m.put("payload", payload);
		if (target != null)
			m.put("target", target);
		if (form != null)
			m.put("form", form);
		if (endpoint != null)
			m.put("endpoint", endpoint);
		if (method != null)
			m.put("method", method);
		if (onSuccess != null)
			m.put("onSuccess", onSuccess);
		if (buttons != null)
			m.put("buttons", buttons);
		if (optional != null)
			m.put("optional", optional);
		if ("export".equals(type) && buttons == null && optional == null)
			throw iaex("RibbonItem export requires at least one button.");
		if ("option".equals(type))
			checkScope(withType);
		if (column != null)
			m.put("column", column);
		if (param != null)
			m.put("param", param);
		if (value != null)
			m.put("value", value);
		if (persist != null)
			m.put("persist", persist);
		if (dflt != null) {
			checkDefault();
			m.put("default", dflt);
		}
		if (deselectable != null)
			m.put("deselectable", deselectable);
		if (options != null) {
			var list = new JsonList();
			for (var o : options)
				list.add(o.toMap(this));
			m.put("options", list);
		}
		checkKeys(m, parent);
		return m;
	}

	private void checkKeys(JsonMap m, RibbonItem parent) {
		var allowed = parent == null ? ALLOWED.get(type) : MEMBER_ALLOWED;
		for (var k : m.keySet())
			if (! allowed.contains(k)) {
				if (parent == null)
					throw iaex("RibbonItem %s%s does not accept '%s'.", type, id == null ? "" : " '" + id + "'", k);
				throw iaex("RibbonItem optionGroup '%s' member '%s' does not accept '%s'.", parent.id, id, k);
			}
	}

	private void checkScope(boolean topLevel) {
		if (column != null && param != null)
			throw iaex("RibbonItem option '%s' sets both column '%s' and param '%s'.", id, column, param);
		if (topLevel && column == null && param == null)
			throw iaex("RibbonItem option '%s' sets neither column nor param.", id);
		if (topLevel && value == null)
			throw iaex("RibbonItem option '%s' requires a value.", id);
	}

	private void checkDefault() {
		if ("optionGroup".equals(type)) {
			var d = dflt;
			if (! (d instanceof String) || options.stream().noneMatch(o -> o.id.equals(d)))
				throw iaex("RibbonItem optionGroup '%s' default '%s' is not one of its options.", id, d);
		} else if ("option".equals(type) && ! Boolean.TRUE.equals(dflt)) {
			throw iaex("RibbonItem option '%s' default '%s' must be on; use defaultOn().", id, dflt);
		}
	}

	private static Set<String> union(Set<String> base, String...more) {
		var s = new LinkedHashSet<>(base);
		s.addAll(Arrays.asList(more));
		return Set.copyOf(s);
	}

	private static void checkOneOf(String v, List<String> allowed, String what) {
		if (! allowed.contains(v))
			throw iaex("RibbonItem %s '%s' is not one of '%s'.", what, v, String.join(", ", allowed));
	}
}
