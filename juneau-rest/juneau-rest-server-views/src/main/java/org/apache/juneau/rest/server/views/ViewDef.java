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
import java.util.concurrent.atomic.*;
import java.util.logging.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.rest.server.converter.*;
import org.apache.juneau.rest.server.datatables.*;
import org.apache.juneau.rest.server.widgets.*;

/**
 * The top-level, declarative "rich DataTables view" definition &mdash; the root of the frozen {@code VIEW_META}
 * JSON wire contract (design doc §6.10).
 *
 * <p>
 * A {@link ViewDef} is an ordinary Juneau bean built via a small fluent builder ({@link #create(String)} + chained
 * setters + {@link #build()}) and serialized with the repo's canonical compact JSON marshaller.  The
 * {@code @BeanType(properties=...)} contract pins both the <b>set</b> and the <b>order</b> of emitted keys so the
 * sidecar the server writes and the {@code juneau-views.js} runtime consumes stay wire-stable.
 *
 * <p>
 * {@code rowActions} (the per-row action menu; see {@link #rowActions(RowAction...)}) is a {@code VIEW_META} wire
 * field.  Row-details structure ({@link #details(RowDetailDef)}) is Java-only (a {@code <template>} sibling, never
 * a sidecar key).  The remaining reserved catalog fields ({@code catalog}, {@code format}, ...) are <b>not</b>
 * part of this MVP builder and are therefore omitted from the serialized contract (design doc §6.10 reserved
 * stubs) &mdash; omitted, not emitted as {@code null}.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	ViewDef <jv>view</jv> = ViewDef.<jsm>create</jsm>(<js>"releases"</js>)
 * 		.rowType(Release.<jk>class</jk>)
 * 		.dataMode(DataMode.<jsf>SERVER</jsf>)
 * 		.dataUrl(<js>"servlet:/releases/data"</js>)
 * 		.defaultOrder(<js>"date"</js>, Dir.<jsf>DESC</jsf>)
 * 		.columns(Column.<jsm>of</jsm>(<js>"name"</js>).title(<js>"Name"</js>))
 * 		.build();
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link Column}
 * 	<li class='jc'>{@link RibbonAction}
 * 	<li class='jc'>{@link RowClassRule}
 * 	<li class='jc'>{@link RowAction}
 * 	<li class='jc'>{@link RowDetailDef}
 * 	<li class='jc'>{@link ColumnConfig}
 * </ul>
 *
 * @since 10.0.0
 */
@BeanType(properties="contractVersion,id,rowType,dataMode,dataUrl,defaultOrder,columns,ribbon,rowClassRules,rowActions,pollIntervalMs,columnConfig")
@SuppressWarnings({
	"java:S1845" // Fluent-builder setters intentionally mirror field names (Juneau DSL convention).
})
public class ViewDef {

	/** The frozen contract version.  Bumped only on a breaking wire change. */
	public static final String CONTRACT_VERSION = "4";

	/**
	 * The minimum honored polling interval, in milliseconds.
	 *
	 * <p>
	 * A declared {@link #poll(long)} interval below this floor is clamped up to it rather than honored as
	 * configured &mdash; a declarable interval with no floor lets a consumer configure a self-inflicted load
	 * problem on a server-side-query table.  Enforced here (server-side) rather than only in
	 * {@code juneau-views.js} so the clamp is a single, easily-tested source of truth and a stale/cached client
	 * script can't be tricked into honoring a sub-floor value the server never actually declared.
	 *
	 * <p>
	 * This is a public alias, equal by construction, of the toolkit-wide floor
	 * {@link org.apache.juneau.commons.http.SafePathTemplate#MIN_POLL_INTERVAL_MS} &mdash; the single commons source
	 * of truth every polling widget shares.
	 */
	public static final long MIN_POLL_INTERVAL_MS = org.apache.juneau.commons.http.SafePathTemplate.MIN_POLL_INTERVAL_MS;

	/**
	 * How the table sources its rows.
	 *
	 * <p>
	 * Each constant carries the lowercase wire token emitted for the {@code dataMode} field.
	 */
	public enum DataMode {

		/** Server-side processing: DataTables posts draw/search/order params and the endpoint returns a page. */
		SERVER("server"),

		/** Client-side processing: the full row set is delivered once and DataTables paginates in-browser. */
		CLIENT("client");

		private final String wire;

		DataMode(String wire) {
			this.wire = wire;
		}

		/**
		 * Returns the lowercase wire token for this mode.
		 *
		 * @return The wire token (e.g. <c>"server"</c>).
		 */
		public String wire() {
			return wire;
		}
	}

	/**
	 * A sort direction.
	 *
	 * <p>
	 * Each constant carries the lowercase wire token emitted for an order entry's {@code dir} field.
	 */
	public enum Dir {

		/** Ascending. */
		ASC("asc"),

		/** Descending. */
		DESC("desc");

		private final String wire;

		Dir(String wire) {
			this.wire = wire;
		}

		/**
		 * Returns the lowercase wire token for this direction.
		 *
		 * @return The wire token (e.g. <c>"desc"</c>).
		 */
		public String wire() {
			return wire;
		}
	}

	/**
	 * A single default-order entry: which column to sort by and in which direction.
	 *
	 * @since 10.0.0
	 */
	@BeanType(properties="data,dir")
	public static class OrderEntry {

		/** The column data key to sort by. */
		public String data;

		/** The sort direction wire token (see {@link Dir#wire()}). */
		public String dir;

		/**
		 * Creates an order entry.
		 *
		 * @param data The column data key.  Must not be <jk>null</jk>.
		 * @param dir The sort direction.  Must not be <jk>null</jk>.
		 * @return A new {@link OrderEntry}.
		 */
		public static OrderEntry of(String data, Dir dir) {
			var e = new OrderEntry();
			e.data = data;
			e.dir = dir.wire();
			return e;
		}
	}

	/** The frozen contract version discriminator (always {@value #CONTRACT_VERSION} for this contract). */
	public String contractVersion = CONTRACT_VERSION;

	/** The stable view id (also the {@code localStorage} state-key root). */
	public String id;

	/** Diagnostic: the simple name of the row bean type this view renders. */
	public String rowType;

	/** How the table sources rows (wire token; see {@link DataMode#wire()}). */
	public String dataMode;

	/** The URL the client hits for row data. */
	public String dataUrl;

	/** The initial sort order. */
	public List<OrderEntry> defaultOrder;

	/** The column descriptors, in display order. */
	public List<Column> columns;

	/** The ribbon/toolbar actions, in display order. */
	public List<RibbonAction> ribbon;

	/** The declarative row-decorator rules. */
	public List<RowClassRule> rowClassRules;

	/**
	 * The per-row action descriptors, in menu order; omitted from the wire when unset (no row menu).
	 *
	 * <p>
	 * Each {@link RowAction} declares a mutating request the {@code juneau-views.js} runtime renders as a row-menu
	 * item and submits with the auto-embedded CSRF token (see {@link RowAction} for the frozen wire schema and
	 * the fail-closed submit contract).
	 */
	public List<RowAction> rowActions;

	/**
	 * The row-details expander definition; <b>not</b> a {@code VIEW_META} wire field (omitted from
	 * {@code @BeanType}).  When set, {@link ViewTable} emits a {@code <template data-juneau-row-detail>} sibling
	 * and the client expands via GET.
	 */
	public RowDetailDef details;

	/**
	 * Author-declared server-side scalar values interpolated into this view's chrome (titles/labels) as
	 * <js>"$FV{name}"</js> at serve time; <b>not</b> a {@code VIEW_META} wire field (Java-only, like
	 * {@link #details}, because lambda providers never marshal).  When set, {@link ViewTable} resolves the declared
	 * {@code $FV} chrome against a per-response sibling session, so the painted chrome and the VIEW_META sidecar
	 * carry the same resolved strings.
	 */
	public ServerValues serverValues;

	/**
	 * The optional at-a-glance figures strip painted above this table's toolbar; <b>not</b> a {@code VIEW_META} wire
	 * field (omitted from {@code @BeanType}, like {@link #details}).
	 *
	 * <p>
	 * Server-painted once by {@link ViewTable} and carried by its own {@link QuickStats#CONTRACT_VERSION}, so adding a
	 * strip to a view cannot bump {@link #CONTRACT_VERSION}.  Display-only: {@link QuickStats} has no action,
	 * endpoint, or refresh surface to emit, and it is never a {@code $FV} interpolation host.
	 */
	public QuickStats quickStats;

	/**
	 * The declared table-refresh polling interval, in milliseconds; omitted from the wire when unset (no polling).
	 * Never below {@link #MIN_POLL_INTERVAL_MS} &mdash; see {@link #poll(long)}.
	 */
	public Long pollIntervalMs;

	/**
	 * The opt-in column-configurator settings; omitted from the wire when unset (no chooser).
	 *
	 * <p>
	 * Presence alone &mdash; even an empty {@link ColumnConfig}) &mdash; enables the View-only column chooser for
	 * this view; there is no separate {@code enabled} flag.  Appended last in the {@code @BeanType} order so an
	 * existing golden fixture only ever gains a trailing key when this is set.
	 */
	public ColumnConfig columnConfig;

	/**
	 * The row bean type, retained (non-serialized) so {@link #build()} can auto-seed {@link #columns} from
	 * {@link DataTablesColumns#of(Class)} when no explicit columns were declared.  Not a wire field.
	 */
	private Class<?> rowTypeClass;

	/**
	 * Starts a new {@link ViewDef} builder with the specified stable view id.
	 *
	 * @param id The stable view id.  Must not be <jk>null</jk> or blank.
	 * @return A new mutable {@link ViewDef} to chain builder calls on.
	 */
	public static ViewDef create(String id) {
		if (id == null || id.isBlank())
			throw iaex("ViewDef id must not be null or blank.");
		var v = new ViewDef();
		v.id = id;
		return v;
	}

	/**
	 * Sets the row bean type; its {@link Class#getSimpleName() simple name} drives the {@link #rowType} diagnostic.
	 *
	 * @param value The row bean type.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public ViewDef rowType(Class<?> value) {
		rowType = value.getSimpleName();
		rowTypeClass = value;
		return this;
	}

	/**
	 * Sets how the table sources its rows.
	 *
	 * @param value The data mode.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public ViewDef dataMode(DataMode value) {
		dataMode = value.wire();
		return this;
	}

	/**
	 * Sets the URL the client hits for row data.
	 *
	 * @param value The data URL.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public ViewDef dataUrl(String value) {
		dataUrl = value;
		return this;
	}

	/**
	 * Sets the initial sort order to a single column/direction entry.
	 *
	 * @param data The column data key to sort by.  Must not be <jk>null</jk>.
	 * @param dir The sort direction.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public ViewDef defaultOrder(String data, Dir dir) {
		defaultOrder = l(OrderEntry.of(data, dir));
		return this;
	}

	/**
	 * Sets the column descriptors.
	 *
	 * @param value The columns, in display order.
	 * @return This object.
	 */
	public ViewDef columns(Column...value) {
		columns = l(value);
		return this;
	}

	/**
	 * Sets the ribbon/toolbar actions.
	 *
	 * @param value The actions, in display order.
	 * @return This object.
	 */
	public ViewDef ribbon(RibbonAction...value) {
		ribbon = l(value);
		return this;
	}

	/**
	 * Declares that this table's data should be polled (re-fetched) on the given interval.
	 *
	 * <p>
	 * A value below {@link #MIN_POLL_INTERVAL_MS} is silently clamped up to the floor rather than rejected, so a
	 * consumer that fat-fingers a too-aggressive interval gets a safe table rather than a build-time failure.
	 * The client ({@code juneau-views.js}) re-fetches on this interval via a plain interval timer &mdash; a
	 * separate mechanism from any streaming/SSE transport &mdash; pauses while the tab or page is not visible,
	 * shows a per-table last-refreshed age, and never overwrites a row that is currently in-flight from a write
	 * (that row is left stale until the write's own result repaints it).
	 *
	 * @param intervalMs The desired polling interval, in milliseconds.  Must be positive.
	 * @return This object.
	 */
	public ViewDef poll(long intervalMs) {
		if (intervalMs <= 0)
			throw iaex("ViewDef.poll(...) interval must be positive.");
		pollIntervalMs = Math.max(intervalMs, MIN_POLL_INTERVAL_MS);
		return this;
	}

	/**
	 * Adds a value-based row-decorator rule ({@link RowClassRule.Op#EQ eq}/{@link RowClassRule.Op#NE ne}).
	 *
	 * @param field The row field to test.  Must not be <jk>null</jk>.
	 * @param op The operator.  Must be {@link RowClassRule.Op#EQ} or {@link RowClassRule.Op#NE}.
	 * @param value The comparison value.  Must not be <jk>null</jk>.
	 * @param cssClass The CSS class to add to a matching row.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public ViewDef rowClassRule(String field, RowClassRule.Op op, Object value, String cssClass) {
		return addRowClassRule(RowClassRule.of(field, op, value, cssClass));
	}

	/**
	 * Adds a presence-based row-decorator rule ({@link RowClassRule.Op#PRESENT present}/{@link RowClassRule.Op#ABSENT absent}).
	 *
	 * @param field The row field to test.  Must not be <jk>null</jk>.
	 * @param op The operator.  Must be {@link RowClassRule.Op#PRESENT} or {@link RowClassRule.Op#ABSENT}.
	 * @param cssClass The CSS class to add to a matching row.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public ViewDef rowClassRule(String field, RowClassRule.Op op, String cssClass) {
		return addRowClassRule(RowClassRule.of(field, op, cssClass));
	}

	private ViewDef addRowClassRule(RowClassRule rule) {
		if (rowClassRules == null)
			rowClassRules = l();
		rowClassRules.add(rule);
		return this;
	}

	/**
	 * Declares the row-details expander.
	 *
	 * <p>
	 * Structure is a server-emitted {@code <template>}; field values arrive from {@link RowDetailDef#endpoint}.
	 * See {@link #details} &mdash; this is not a {@code VIEW_META} JSON field.
	 *
	 * @param value The row-detail definition.  May be <jk>null</jk> (no expander).
	 * @return This object.
	 */
	public ViewDef details(RowDetailDef value) {
		details = value;
		return this;
	}

	/**
	 * Declares the server-side scalar values interpolated into this view's chrome as <js>"$FV{name}"</js>.
	 *
	 * <p>
	 * See {@link #serverValues} &mdash; this is a Java-only builder field, not a {@code VIEW_META} JSON key.
	 *
	 * @param value The server-values declaration.  May be <jk>null</jk> (no {@code $FV} interpolation).
	 * @return This object.
	 */
	public ViewDef serverValues(ServerValues value) {
		serverValues = value;
		return this;
	}

	/**
	 * Declares the at-a-glance figures strip painted above this table's toolbar.
	 *
	 * <p>
	 * See {@link #quickStats} &mdash; this is a Java-only builder field, not a {@code VIEW_META} JSON key.
	 *
	 * @param value The quick-stats strip.  May be <jk>null</jk> (no strip).
	 * @return This object.
	 */
	public ViewDef quickStats(QuickStats value) {
		quickStats = value;
		return this;
	}

	/**
	 * Fail-closed bean validation.  When {@link #details} is set, delegates to
	 * {@link RowDetailDef#validate(List, String)} against this view's {@link #rowActions} and id; when
	 * {@link #serverValues} is set, cascades to {@link ServerValues#validate()}; when {@link #quickStats} is set,
	 * cascades to {@link QuickStats#validate()}.
	 *
	 * @throws IllegalArgumentException If this view (or its nested details / server values / quick stats) is not
	 * 	well-formed.
	 */
	public void validate() {
		if (details != null)
			details.validate(rowActions, id);
		if (serverValues != null)
			serverValues.validate();
		if (quickStats != null)
			quickStats.validate();
		if (columns != null) {
			var actionIds = new HashSet<String>();
			if (rowActions != null)
				for (var a : rowActions)
					if (a != null && a.id != null)
						actionIds.add(a.id);
			for (var c : columns) {
				if (c != null && c.render != null) {
					if (c.render.popover != null)
						c.render.popover.validate();
					if ("pill".equals(c.render.id))
						validatePill(c.render, actionIds);
				}
			}
		}
	}

	/**
	 * The closed dot-tone palette a {@code pill} understands on either host &mdash; the same five status tones
	 * {@link org.apache.juneau.rest.server.widgets.QuickStats} paints with, so one name means one colour everywhere.
	 *
	 * <p>
	 * The older {@code ok}/{@code warn}/{@code exceeds} vocabulary is gone: a tone is now one of
	 * {@code info}/{@code success}/{@code warning}/{@code error}/{@code neutral} and anything else fails closed.  Note
	 * this is unrelated to the {@code progress} renderer's {@code warn}/{@code exceeds} <b>threshold</b> meta keys,
	 * which are numeric comparison points rather than tones and are deliberately untouched.
	 */
	private static final Set<String> PILL_TONES = StatusTone.WIRE_TOKENS;

	/** One-shot guard so the ignored-{@code meta.select} notice logs only once per JVM. */
	private static final AtomicBoolean PILL_SELECT_LOGGED = new AtomicBoolean();

	private static final Logger LOG = Logger.getLogger(ViewDef.class.getName());

	/**
	 * Serving-path fail-closed check for a {@code pill} <b>column</b>: an {@code meta.action} must name a declared
	 * {@link #rowActions} id, an {@code meta.tone} (when present) must be one of {@link #PILL_TONES}, and an
	 * action-bound pill cannot also carry a {@code render.popover} (two competing click affordances on one cell).
	 *
	 * <p>
	 * An {@code meta.action} is <b>optional</b>: a pill with no metadata at all, or with metadata but no action, is a
	 * legal display-only chip and is not gated on anything.  An unrecognized {@code meta.select} is <b>ignored</b>
	 * rather than rejected &mdash; pills are not part of the selection protocol (that stays the checkbox column owned
	 * by {@link SelectionDef}) &mdash; but it is worth telling the author once that the key does nothing.
	 */
	private static void validatePill(Render render, Set<String> actionIds) {
		var meta = render.meta;
		if (meta == null)
			return;
		var action = meta.get("action");
		if (action != null && ! action.isBlank()) {
			if (! actionIds.contains(action))
				throw iaex("Pill column action '%s' is not declared on the view's rowActions.", action);
			if (render.popover != null)
				throw iaex("Pill column action '%s' cannot also set render.popover on the same cell.", action);
		}
		if (meta.containsKey("select") && PILL_SELECT_LOGGED.compareAndSet(false, true))
			LOG.log(Level.INFO, "A pill render declares meta.select, which is not a supported feature and is ignored. "
				+ "Row selection is the checkbox protocol declared with SelectionDef, not a pill affordance.");
		validatePillTone(meta.get("tone"), "Pill column");
	}

	/**
	 * Serving-path fail-closed check for a {@code pill} named on a <b>fill sink</b>
	 * ({@link DetailField#render(String)}).
	 *
	 * <p>
	 * A fill sink has no {@link #rowActions} in scope, so a {@code meta.action} there could never resolve to anything
	 * &mdash; it is rejected outright rather than silently painting a dead affordance.  The tone palette is the same
	 * closed set the cell path enforces.
	 *
	 * @param render The sink's render spec.  Must not be <jk>null</jk>.
	 * @param host A human identifier for the sink being validated, used in the failure message.
	 * @throws IllegalArgumentException If the sink pill declares an action or an off-palette tone.
	 */
	static void validateSinkPill(Render render, String host) {
		var meta = render.meta;
		if (meta == null)
			return;
		var action = meta.get("action");
		if (action != null && ! action.isBlank())
			throw iaex("Pill fill sink '%s' must not declare meta.action ('%s'): a fill sink has no rowActions in "
				+ "scope, so a sink pill is display-only.", host, action);
		validatePillTone(meta.get("tone"), "Pill fill sink '" + host + "'");
	}

	/** The one tone check both pill hosts share, so a tone can never be legal on one host and not the other. */
	private static void validatePillTone(String tone, String what) {
		if (tone != null && ! tone.isBlank() && ! PILL_TONES.contains(tone))
			throw iaex("%s tone '%s' must be one of %s.", what, tone, String.join("|", PILL_TONES));
	}

	/**
	 * Declares the per-row action menu.
	 *
	 * <p>
	 * Each {@link RowAction} is a mutating request the {@code juneau-views.js} runtime renders as a row-menu item
	 * and submits with the process's CSRF token.  A {@link RowAction} can only carry a non-safe HTTP method
	 * ({@link RowAction.Method}), so a mutating action can never be declared against a safe (CSRF-exempt) method.
	 *
	 * @param value The row actions, in menu order.
	 * @return This object.
	 */
	public ViewDef rowActions(RowAction...value) {
		rowActions = l(value);
		return this;
	}

	/**
	 * Enables the View-only column chooser for this view by setting its column-configurator settings.
	 *
	 * @param value The column-configurator settings.  Presence alone enables the chooser; an empty
	 * 	{@link ColumnConfig#create()} suffices.
	 * @return This object.
	 */
	public ViewDef columnConfig(ColumnConfig value) {
		columnConfig = value;
		return this;
	}

	/**
	 * Finalizes the builder and returns the wire-ready {@link ViewDef}.
	 *
	 * <p>
	 * When no columns were declared via {@link #columns(Column...)} but a {@link #rowType(Class) row type} was set,
	 * the column list is auto-seeded from {@link DataTablesColumns#of(Class)} &mdash; one {@link Column} per readable
	 * bean property, in bean-property order, carrying the DataTables-native {@code data}/{@code title}/
	 * {@code orderable}/{@code searchable} fields.  Explicitly-declared columns are left untouched.
	 *
	 * @return This object.
	 */
	public ViewDef build() {
		if (columns == null && rowTypeClass != null)
			columns = seedColumns(rowTypeClass);
		return this;
	}

	/**
	 * Server-side wiring helper (design doc §6, Task B.5): builds the {@link QueryableSettings} bean that selects the
	 * DataTables server-side-processing protocol for this view's data op.
	 *
	 * <p>
	 * Register the returned bean in the data endpoint's resource (a {@code @Bean} factory method) alongside
	 * {@code @Rest(converters=ProtocolQueryable.class)}; the op then returns its row {@code List} and
	 * {@link ProtocolQueryable} parses the DataTables request, runs the shared query engine, and wraps the page in a
	 * {@link DataTablesResults} envelope.  Binding the protocol's {@code rowType} to this view's row type keeps
	 * array-index (positional) column resolution in sync with the view definition.
	 *
	 * <p>
	 * The settings must be registered <b>at the op/resource level</b> (a mixin can't inject config into a host's other
	 * endpoints).  Hand-wiring the equivalent
	 * <c>QueryableSettings.create().protocol(new DataTablesQueryProtocol(rowType)).build()</c> stays fully supported;
	 * this helper simply keeps the row type in one source of truth with the {@link ViewDef}.
	 *
	 * @return A new {@link QueryableSettings} whose protocol is a {@link DataTablesQueryProtocol} bound to this view's
	 * 	row type.
	 * @throws IllegalArgumentException If no {@link #rowType(Class) row type} was set on this view.
	 */
	public QueryableSettings queryableSettings() {
		if (rowTypeClass == null)
			throw iaex("ViewDef.queryableSettings() requires a rowType; call .rowType(...) before building the view.");
		return QueryableSettings.create().protocol(new DataTablesQueryProtocol(rowTypeClass)).build();
	}

	/** Decorates the {@link DataTablesColumns#of(Class)} descriptor maps into {@link Column} beans. */
	private static List<Column> seedColumns(Class<?> rowType) {
		var out = new ArrayList<Column>();
		for (var m : DataTablesColumns.of(rowType)) {
			out.add(Column.of((String) m.get("data"))
				.title((String) m.get("title"))
				.orderable(Boolean.TRUE.equals(m.get("orderable")))
				.searchable(Boolean.TRUE.equals(m.get("searchable"))));
		}
		return out;
	}
}
