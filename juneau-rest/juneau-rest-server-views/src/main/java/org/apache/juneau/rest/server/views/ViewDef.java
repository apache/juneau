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
import org.apache.juneau.rest.server.converter.*;
import org.apache.juneau.rest.server.datatables.*;

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
 * {@code details} (the row-details expander's field list) and {@code rowActions} (the per-row action menu; see
 * {@link #rowActions(RowAction...)}) are both implemented.  The remaining reserved catalog fields ({@code catalog},
 * {@code format}, ...) are <b>not</b> part of this MVP builder and are therefore omitted from the serialized
 * contract (design doc §6.10 reserved stubs) &mdash; omitted, not emitted as {@code null}.
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
 * </ul>
 *
 * @since 10.0.0
 */
@BeanType(properties="contractVersion,id,rowType,dataMode,dataUrl,defaultOrder,columns,ribbon,rowClassRules,rowActions,details,pollIntervalMs")
@SuppressWarnings("java:S1845") // Fluent-builder setters intentionally mirror field names (Juneau DSL convention).
public class ViewDef {

	/** The frozen contract version.  Bumped only on a breaking wire change. */
	public static final String CONTRACT_VERSION = "3";

	/**
	 * The minimum honored polling interval, in milliseconds.
	 *
	 * <p>
	 * A declared {@link #poll(long)} interval below this floor is clamped up to it rather than honored as
	 * configured &mdash; a declarable interval with no floor lets a consumer configure a self-inflicted load
	 * problem on a server-side-query table.  Enforced here (server-side) rather than only in
	 * {@code juneau-views.js} so the clamp is a single, easily-tested source of truth and a stale/cached client
	 * script can't be tricked into honoring a sub-floor value the server never actually declared.
	 */
	public static final long MIN_POLL_INTERVAL_MS = 5_000L;

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

	/**
	 * A single field projected into the row-details expander's body.
	 *
	 * <p>
	 * Client-rendered from the row's own already-fetched data by default &mdash; declaring a {@link #data} key
	 * reads a value the row already carries, so expanding a row never issues a request of its own. An optional
	 * server-render path for consumers that need it is not exposed by this type.
	 *
	 * @since 10.0.0
	 */
	@BeanType(properties="data,title")
	public static class DetailDef {

		/** The row bean-property / JSON key this field reads from the row's own data. */
		public String data;

		/** The label shown next to the value in the expanded detail panel. */
		public String title;

		/**
		 * Creates a detail field bound to the specified row data key.
		 *
		 * @param data The bean-property / JSON key.  Must not be <jk>null</jk>.
		 * @return A new {@link DetailDef}.
		 */
		public static DetailDef of(String data) {
			var d = new DetailDef();
			d.data = data;
			return d;
		}

		/**
		 * Sets the label shown next to the value in the expanded detail panel.
		 *
		 * @param value The new value.
		 * @return This object.
		 */
		public DetailDef title(String value) {
			title = value;
			return this;
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
	 * The row-details expander's field list; omitted from the wire when unset (no expander).
	 *
	 * <p>
	 * Rendered client-side from the row's own already-fetched data by default &mdash; declaring this does not
	 * add a request per expansion. Expanding a row does not survive a redraw: a sort, page change, search, or
	 * {@link #poll(long)} tick collapses any expanded row.
	 */
	public List<DetailDef> details;

	/**
	 * The declared table-refresh polling interval, in milliseconds; omitted from the wire when unset (no polling).
	 * Never below {@link #MIN_POLL_INTERVAL_MS} &mdash; see {@link #poll(long)}.
	 */
	public Long pollIntervalMs;

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
	 * Declares the row-details expander's field list.
	 *
	 * <p>
	 * Rendered client-side ({@code juneau-views.js}) from the row's own data by default; see {@link #details} for
	 * the no-extra-request and collapse-on-redraw notes.
	 *
	 * @param value The detail fields, in display order.
	 * @return This object.
	 */
	public ViewDef details(DetailDef...value) {
		details = l(value);
		return this;
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
