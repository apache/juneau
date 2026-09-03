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

import org.apache.juneau.commons.bean.*;

/**
 * A ribbon/toolbar action descriptor in the {@code VIEW_META} wire contract (design doc §6.5).
 *
 * <p>
 * A single bean discriminated by {@link #type}; only the fields relevant to a given action type are populated, and
 * the serializer omits the rest (null-valued properties are dropped).  Build actions via the static factory methods
 * ({@link #export(String...)}, {@link #refresh()}, {@link #columnSearchToggle()}, {@link #pausePolling()},
 * {@link #collapseAll()}, {@link #option(String)}, {@link #optionGroup(String)}, {@link #divider()}).
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link ViewDef}
 * </ul>
 *
 * @since 10.0.0
 */
@BeanType(properties="type,buttons,optional,id,title,group,column,value,param,persist,symbol,color,deselectable,options")
@SuppressWarnings("java:S1845") // Fluent-builder setters intentionally mirror field names (Juneau DSL convention).
public class RibbonAction {

	/** The discriminator: {@code export}/{@code refresh}/{@code columnSearchToggle}/{@code pausePolling}/{@code collapseAll}/{@code option}/{@code optionGroup}/{@code divider}. */
	public String type;

	/** For {@code export}: the always-on button ids (e.g. {@code ["copy","csv"]}). */
	public List<String> buttons;

	/** For {@code export}: feature-detected optional button ids (e.g. {@code ["excel","pdf"]}). */
	public List<String> optional;

	/** The action id (for {@code option}/{@code optionGroup}). */
	public String id;

	/** The display title. */
	public String title;

	/**
	 * An opaque grouping key (visual-parity design doc §4.A).  Adjacent actions sharing the same non-<jk>null</jk>
	 * {@code group} value render as ONE segmented ribbon cluster (shared borders, rounded only on the outer ends) -
	 * see {@code juneau-ribbon.js}'s {@code buildRibbon(...)}.  An {@code export} action's own resolved buttons are
	 * always clustered together even when {@code group} is unset (one action, one visual cluster); this field
	 * exists so UNRELATED, adjacent actions (e.g. a {@code columnSearchToggle} and a column-scoped {@code option})
	 * can be clustered into one ribbon too.  A {@code divider} always breaks an open cluster.
	 */
	public String group;

	/** For a column-scoped {@code option}: the target column key. */
	public String column;

	/** For an {@code option}: the value contributed to the query when active. */
	public String value;

	/** For a custom (non-column-scoped) {@code option}: the request param name the endpoint must parse. */
	public String param;

	/** Whether the {@code option} toggle state is persisted to {@code localStorage}. */
	public Boolean persist;

	/** Optional glyph/symbol hint. */
	public String symbol;

	/** Optional color hint. */
	public String color;

	/** For {@code optionGroup}: whether the select-one cluster can be fully deselected. */
	public Boolean deselectable;

	/** For {@code optionGroup}: the member options. */
	public List<Opt> options;

	/**
	 * A member option of an {@code optionGroup} (design doc §6.5).
	 *
	 * @since 10.0.0
	 */
	@BeanType(properties="id,title,column,value,param,symbol,color")
	public static class Opt {

		/** The option id. */
		public String id;

		/** The display title. */
		public String title;

		/** The target column key (column-scoped option). */
		public String column;

		/** The value contributed to the query when active. */
		public String value;

		/** The custom request param name (non-column-scoped option). */
		public String param;

		/** Optional glyph/symbol hint. */
		public String symbol;

		/** Optional color hint. */
		public String color;

		/**
		 * Creates an option with the specified id.
		 *
		 * @param id The option id.  Must not be <jk>null</jk>.
		 * @return A new {@link Opt}.
		 */
		public static Opt of(String id) {
			var o = new Opt();
			o.id = id;
			return o;
		}

		/**
		 * Sets the display title.
		 *
		 * @param value The new value.
		 * @return This object.
		 */
		public Opt title(String value) {
			title = value;
			return this;
		}

		/**
		 * Sets the target column key.
		 *
		 * @param value The new value.
		 * @return This object.
		 */
		public Opt column(String value) {
			column = value;
			return this;
		}

		/**
		 * Sets the contributed value.
		 *
		 * @param value The new value.
		 * @return This object.
		 */
		public Opt value(String value) {
			this.value = value;
			return this;
		}

		/**
		 * Sets the custom request param name.
		 *
		 * @param value The new value.
		 * @return This object.
		 */
		public Opt param(String value) {
			param = value;
			return this;
		}

		/**
		 * Sets the color hint.
		 *
		 * @param value The new value.
		 * @return This object.
		 */
		public Opt color(String value) {
			color = value;
			return this;
		}

		/**
		 * Sets the symbol hint.
		 *
		 * @param value The new value.
		 * @return This object.
		 */
		public Opt symbol(String value) {
			symbol = value;
			return this;
		}
	}

	/**
	 * Creates an {@code export} action with the specified always-on button ids.
	 *
	 * @param buttons The always-on button ids (MVP: {@code "copy"}, {@code "csv"}).
	 * @return A new {@link RibbonAction}.
	 */
	public static RibbonAction export(String...buttons) {
		var a = new RibbonAction();
		a.type = "export";
		a.buttons = l(buttons);
		return a;
	}

	/**
	 * Sets the feature-detected optional export button ids (e.g. {@code "excel"}, {@code "pdf"}).
	 *
	 * @param optional The optional button ids.
	 * @return This object.
	 */
	public RibbonAction optional(String...optional) {
		this.optional = l(optional);
		return this;
	}

	/**
	 * Creates a {@code refresh} action (re-draw / reload the table).
	 *
	 * @return A new {@link RibbonAction}.
	 */
	public static RibbonAction refresh() {
		var a = new RibbonAction();
		a.type = "refresh";
		return a;
	}

	/**
	 * Creates a {@code columnSearchToggle} action (show/hide per-column search inputs).
	 *
	 * @return A new {@link RibbonAction}.
	 */
	public static RibbonAction columnSearchToggle() {
		var a = new RibbonAction();
		a.type = "columnSearchToggle";
		return a;
	}

	/**
	 * Creates a {@code pausePolling} action &mdash; a stateful pause/resume toggle over this view's poll timer.
	 *
	 * <p>
	 * View-local and non-mutating, like {@link #refresh()} and {@link #columnSearchToggle()}: it holds the table
	 * still so the operator can read or type without a tick redrawing underneath them, and holds it until they
	 * press it again.  While paused the staleness indicator reads {@code "Paused - updated 42s ago"} with the age
	 * still advancing &mdash; a suspended poll must never be able to pass for a healthy one, or for a broken one.
	 *
	 * <p>
	 * Only meaningful on a view that declares {@link ViewDef#poll(long)}; on any other view the client renders no
	 * button at all rather than an inert one.  The toggle survives a column-config Apply (the view stays paused,
	 * and the rebuilt button comes back pressed), and it is independent of
	 * {@link ViewDef#pausePollingWhileEditing()} &mdash; resuming does not override an editor that is still open.
	 *
	 * @return A new {@link RibbonAction}.
	 */
	public static RibbonAction pausePolling() {
		var a = new RibbonAction();
		a.type = "pausePolling";
		return a;
	}

	/**
	 * Creates a {@code collapseAll} action (Foundry WORK-P0063 toolbar follow-up, {@code WORK-J0507}) &mdash;
	 * collapses every currently-open row-detail expansion in this view's table back down in one click.
	 *
	 * <p>
	 * View-local and non-mutating, like {@link #refresh()}/{@link #columnSearchToggle()}/{@link #pausePolling()}:
	 * it only tears down open detail rows in the CALLING table (never reaches into a nested table's own child
	 * rows, which - if it has its own toolbar - would need its own {@code collapseAll} click), and contributes
	 * nothing to the server query ({@link #toQueryParams(ViewDef)} skips it like every other non-query-contributing
	 * action type).
	 *
	 * @return A new {@link RibbonAction}.
	 */
	public static RibbonAction collapseAll() {
		var a = new RibbonAction();
		a.type = "collapseAll";
		return a;
	}

	/**
	 * Creates an {@code option} action (a server-query toggle) with the specified id.
	 *
	 * @param id The option id.  Must not be <jk>null</jk>.
	 * @return A new {@link RibbonAction}.
	 */
	public static RibbonAction option(String id) {
		var a = new RibbonAction();
		a.type = "option";
		a.id = id;
		return a;
	}

	/**
	 * Creates an {@code optionGroup} action (a select-one cluster) with the specified id.
	 *
	 * @param id The group id.  Must not be <jk>null</jk>.
	 * @return A new {@link RibbonAction}.
	 */
	public static RibbonAction optionGroup(String id) {
		var a = new RibbonAction();
		a.type = "optionGroup";
		a.id = id;
		return a;
	}

	/**
	 * Creates a {@code divider} action (a visual separator).
	 *
	 * @return A new {@link RibbonAction}.
	 */
	public static RibbonAction divider() {
		var a = new RibbonAction();
		a.type = "divider";
		return a;
	}

	/**
	 * Sets the display title.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public RibbonAction title(String value) {
		title = value;
		return this;
	}

	/**
	 * Sets the grouping key (visual-parity design doc §4.A) - adjacent actions sharing the same key render as one
	 * segmented ribbon cluster.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public RibbonAction group(String value) {
		group = value;
		return this;
	}

	/**
	 * Sets the target column key (column-scoped option).
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public RibbonAction column(String value) {
		column = value;
		return this;
	}

	/**
	 * Sets the value contributed to the query when the option is active.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public RibbonAction value(String value) {
		this.value = value;
		return this;
	}

	/**
	 * Sets the custom request param name (non-column-scoped option).
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public RibbonAction param(String value) {
		param = value;
		return this;
	}

	/**
	 * Sets whether the option toggle state is persisted to {@code localStorage}.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public RibbonAction persist(boolean value) {
		persist = value;
		return this;
	}

	/**
	 * Sets the glyph/symbol hint.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public RibbonAction symbol(String value) {
		symbol = value;
		return this;
	}

	/**
	 * Sets the color hint.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public RibbonAction color(String value) {
		color = value;
		return this;
	}

	/**
	 * Marks an {@code optionGroup} as fully deselectable.
	 *
	 * @return This object.
	 */
	public RibbonAction deselectable() {
		deselectable = true;
		return this;
	}

	/**
	 * Sets the member options of an {@code optionGroup}.
	 *
	 * @param options The member options.
	 * @return This object.
	 */
	public RibbonAction options(Opt...options) {
		this.options = l(options);
		return this;
	}

	/**
	 * Maps a view's ribbon <b>option</b>/<b>optionGroup</b> toggles to the DataTables request parameters they
	 * contribute <b>when active</b> (design doc §6.5, Task B.5).
	 *
	 * <p>
	 * This is the pure, DOM-free counterpart of the {@code juneau-ribbon.js} contribution logic (so the two
	 * implementations share one testable fixture and can't drift), and covers two option shapes:
	 * <ul class='spaced-list'>
	 * 	<li><b>Column-scoped</b> ({@code option(...).column(key).value(v)}): resolves {@code key} to its column
	 * 		<i>index</i> in {@link ViewDef#columns} and contributes the DataTables per-column search param
	 * 		<c>columns[&lt;index&gt;][search][value]=v</c> &mdash; consumed by {@code DataTablesQueryProtocol}.
	 * 	<li><b>Custom-param</b> ({@code option(...).param(name).value(v)}): contributes <c>name=v</c> verbatim.  This is
	 * 		<b>the endpoint's responsibility to parse</b> &mdash; {@code DataTablesQueryProtocol} does <b>not</b> read
	 * 		custom (non-{@code columns[...]}/{@code search}/{@code order}/{@code start}/{@code length}) params.
	 * </ul>
	 *
	 * <p>
	 * An option carrying no {@code value} (or neither a {@code column} nor a {@code param}) contributes nothing;
	 * {@code refresh}/{@code columnSearchToggle}/{@code pausePolling}/{@code collapseAll}/{@code divider}/
	 * {@code export} actions are not query-contributing and are skipped.  {@code optionGroup} member options are
	 * mapped uniformly with top-level options.
	 *
	 * @param viewDef The built view whose ribbon + columns are read.  Must not be <jk>null</jk>.
	 * @return An insertion-ordered map of contributed request param name &rarr; value (possibly empty).
	 * @throws IllegalArgumentException If a column-scoped option references a column not present in {@code viewDef}.
	 */
	public static Map<String,String> toQueryParams(ViewDef viewDef) {
		Map<String,String> out = m();
		if (viewDef.ribbon != null)
			for (var a : viewDef.ribbon) {
				if ("option".equals(a.type))
					addOptionParam(out, viewDef, a.column, a.param, a.value);
				else if ("optionGroup".equals(a.type) && a.options != null)
					for (var o : a.options)
						addOptionParam(out, viewDef, o.column, o.param, o.value);
			}
		return out;
	}

	/** Adds one option's contributed param (column-scoped search value, else a custom param); a valueless option is a no-op. */
	private static void addOptionParam(Map<String,String> out, ViewDef viewDef, String column, String param, String value) {
		if (value == null)
			return;
		if (column != null)
			out.put("columns[" + columnIndex(viewDef, column) + "][search][value]", value);
		else if (param != null)
			out.put(param, value);
	}

	/** Resolves a column's {@code data} key to its zero-based index in the view's column list. */
	private static int columnIndex(ViewDef viewDef, String columnData) {
		var cols = viewDef.columns;
		if (cols != null)
			for (var i = 0; i < cols.size(); i++)
				if (columnData.equals(cols.get(i).data))
					return i;
		throw iaex("Ribbon option references unknown column '%s'.", columnData);
	}
}
