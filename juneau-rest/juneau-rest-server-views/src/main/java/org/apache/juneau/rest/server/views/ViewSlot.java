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
import org.apache.juneau.marshall.cp.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.widgets.*;

/**
 * JSON factory and {@code SLOT_META} wire bean for a table mounted into an empty HTML slot.
 *
 * <p>
 * This is the serializer + request-time chrome resolver for the table-in-slot path.  Apps keep authoring
 * {@link ViewDef}; this type wraps that VIEW_META ({@link ViewDef#CONTRACT_VERSION} {@code "4"}) plus extras
 * that today live as HTML attrs/templates.  It is <b>not</b> a page DSL, not an HTML table emitter (that remains
 * {@link ViewTable}), and it is <b>not</b> the JS helper {@code paintViewSlot} in {@code juneau-helpers.js} (a
 * detail-field value painter).  The prose name {@code DETAIL_SLOT} in this javadoc is the nested envelope object
 * &mdash; it is not {@link BarSlotTable#DETAIL_SLOT_CLASS}.
 *
 * <p>
 * CSRF is never a JSON field.  The client copies {@code data-juneau-csrf} (and {@code data-juneau-csrf-header}
 * when present) from a shell ancestor onto the constructed table.
 *
 * @since 10.0.0
 */
@BeanType(properties="contractVersion,view,layout,savedViewsBase,selection,bulk,detail,quickStats,rows")
@SuppressWarnings({
	"java:S1845" // Fluent-builder setters intentionally mirror field names (Juneau DSL convention).
})
public final class ViewSlot {

	/** The frozen slot-envelope contract version.  Independent of {@link ViewDef#CONTRACT_VERSION}. */
	public static final String CONTRACT_VERSION = "1";

	/** Keys whose string values may be {@code servlet:} URLs on a request-bearing envelope. */
	private static final Set<String> URL_KEYS = Set.of("dataUrl", "endpoint", "form", "href", "refreshUrl");

	/** Always {@value #CONTRACT_VERSION} for this contract. */
	public String contractVersion = CONTRACT_VERSION;

	/** Today's VIEW_META schema (handshake {@code "4"}), as a snapshot map. */
	public Map<String,Object> view;

	/** Always {@link ViewTable#LAYOUT_WIDE} in v1. */
	public String layout = ViewTable.LAYOUT_WIDE;

	/** Saved-views REST base, omitted when the view has no column-config or the request cannot resolve it. */
	public String savedViewsBase;

	/** Selection wire, omitted when the caller did not pass a {@link SelectionDef}. */
	public Selection selection;

	/** Bulk-actions snapshot ({@code contractVersion} + {@code actions} only), omitted when unset. */
	public Map<String,Object> bulk;

	/** DETAIL_SLOT wire, omitted when {@link ViewDef#details} is unset. */
	public Detail detail;

	/** QuickStats bean, omitted when unset. */
	public QuickStats quickStats;

	/** Client-mode fixture rows, omitted in production {@code dataUrl} views. */
	public List<?> rows;

	private ViewSlot() {}

	/**
	 * Request-bearing envelope: resolves {@code $FV}/{@code $L} and {@code servlet:} URLs.
	 *
	 * @param req The current request.  Must not be <jk>null</jk>.
	 * @param viewDef The view.  Must not be <jk>null</jk>.
	 * @return A new {@link ViewSlot}.
	 */
	public static ViewSlot envelope(RestRequest req, ViewDef viewDef) {
		if (req == null)
			throw iaex("ViewSlot.envelope: req must not be null.");
		return envelope(req, viewDef, null, null, null);
	}

	/**
	 * Request-bearing envelope with selection.
	 *
	 * @param req The current request.  Must not be <jk>null</jk>.
	 * @param viewDef The view.  Must not be <jk>null</jk>.
	 * @param selection The selection opt-in.  Must not be <jk>null</jk>.
	 * @return A new {@link ViewSlot}.
	 */
	public static ViewSlot envelope(RestRequest req, ViewDef viewDef, SelectionDef selection) {
		if (req == null)
			throw iaex("ViewSlot.envelope: req must not be null.");
		if (selection == null)
			throw iaex("ViewSlot.envelope: selection must not be null.");
		return envelope(req, viewDef, selection, null, null);
	}

	/**
	 * Request-bearing envelope with bulk mutation (selection is taken from the bulk def).
	 *
	 * @param req The current request.  Must not be <jk>null</jk>.
	 * @param viewDef The view.  Must not be <jk>null</jk>.
	 * @param bulkMutate The bulk opt-in.  Must not be <jk>null</jk>.
	 * @return A new {@link ViewSlot}.
	 */
	public static ViewSlot envelope(RestRequest req, ViewDef viewDef, BulkMutateDef bulkMutate) {
		if (req == null)
			throw iaex("ViewSlot.envelope: req must not be null.");
		if (bulkMutate == null)
			throw iaex("ViewSlot.envelope: bulkMutate must not be null.");
		return envelope(req, viewDef, null, bulkMutate, null);
	}

	/**
	 * Request-bearing envelope with selection and bulk.  Reconciles the two exactly as {@link ViewTable} does:
	 * a caller-passed selection must be the same instance {@code bulkMutate} was constructed against.
	 *
	 * @param req The current request.  Must not be <jk>null</jk>.
	 * @param viewDef The view.  Must not be <jk>null</jk>.
	 * @param selection Optional extra selection handle; when non-<jk>null</jk> must be {@code bulkMutate.selection()}.
	 * @param bulkMutate The bulk opt-in.  Must not be <jk>null</jk>.
	 * @return A new {@link ViewSlot}.
	 */
	public static ViewSlot envelope(RestRequest req, ViewDef viewDef, SelectionDef selection, BulkMutateDef bulkMutate) {
		if (req == null)
			throw iaex("ViewSlot.envelope: req must not be null.");
		if (bulkMutate == null)
			throw iaex("ViewSlot.envelope: bulkMutate must not be null.");
		return envelope(req, viewDef, selection, bulkMutate, null);
	}

	/**
	 * Request-free envelope for tests and static JSON.  Does not resolve {@code $FV}, {@code $L}, or {@code servlet:}.
	 *
	 * @param viewDef The view.  Must not be <jk>null</jk>.
	 * @return A new {@link ViewSlot}.
	 */
	public static ViewSlot envelope(ViewDef viewDef) {
		return envelope(null, viewDef, null, null, null);
	}

	/**
	 * Request-free {@code $L} seam; twin of {@link ViewTable#of(Messages, ViewDef)}.  Does not resolve {@code $FV}
	 * or {@code servlet:}.
	 *
	 * @param messages A locale-bound bundle.  Must not be <jk>null</jk>.
	 * @param viewDef The view.  Must not be <jk>null</jk>.
	 * @return A new {@link ViewSlot}.
	 */
	public static ViewSlot envelope(Messages messages, ViewDef viewDef) {
		if (messages == null)
			throw iaex("ViewSlot.envelope: messages must not be null.");
		return envelope(null, viewDef, null, null, messages);
	}

	/**
	 * Supplies client-mode fixture rows.  Fails loud if the envelope's view already has a {@code dataUrl}.
	 *
	 * @param value The rows.  <jk>null</jk> clears them.
	 * @return This object.
	 */
	public ViewSlot rows(Collection<?> value) {
		if (value == null) {
			rows = null;
			return this;
		}
		if (viewHasDataUrl())
			throw iaex("ViewSlot.rows(...) cannot be set when view.dataUrl is set.");
		rows = snapshotList(value);
		return this;
	}

	private boolean viewHasDataUrl() {
		if (view == null)
			return false;
		var u = view.get("dataUrl");
		return u instanceof String s && ! s.isBlank();
	}

	private static ViewSlot envelope(RestRequest req, ViewDef viewDef, SelectionDef selection,
			BulkMutateDef bulkMutate, Messages messages) {
		if (viewDef == null)
			throw iaex("ViewSlot.envelope: viewDef must not be null.");
		viewDef.validate();
		var sel = selection;
		var bulk = bulkMutate;
		if (bulk != null) {
			if (sel != null && sel != bulk.selection())
				throw iaex("selection must be exactly bulkMutate.selection() when both are supplied; "
					+ "a BulkMutateDef can only render the SelectionDef it was constructed against.");
			sel = bulk.selection();
		}
		var resolvedSel = sel;
		var resolvedBulk = bulk;
		return ViewTable.withResolvedChrome(viewDef, req, messages, () ->
			assemble(req, viewDef, resolvedSel, resolvedBulk));
	}

	private static ViewSlot assemble(RestRequest req, ViewDef viewDef, SelectionDef selection,
			BulkMutateDef bulkMutate) {
		var slot = new ViewSlot();
		slot.view = snapshotMap(viewDef);
		slot.layout = ViewTable.LAYOUT_WIDE;
		if (viewDef.columnConfig != null && req != null) {
			var base = ViewTable.savedViewsBase(req);
			if (base != null && ! base.isBlank())
				slot.savedViewsBase = base;
		}
		if (selection != null)
			slot.selection = Selection.from(selection);
		if (bulkMutate != null)
			slot.bulk = snapshotMap(bulkMutate);
		if (viewDef.details != null)
			slot.detail = detailOf(viewDef, req, null);
		if (viewDef.quickStats != null)
			slot.quickStats = viewDef.quickStats;
		if (req != null)
			resolveServletUrls(slot, req);
		return slot;
	}

	private static Detail detailOf(ViewDef viewDef, RestRequest req, Messages messages) {
		return detailOf(viewDef.details, req, messages);
	}

	private static Detail detailOf(RowDetailDef d, RestRequest req, Messages messages) {
		var out = new Detail();
		out.contractVersion = RowDetailDef.CONTRACT_VERSION;
		out.endpoint = d.endpoint;
		if (d.title != null && ! d.title.isBlank())
			out.title = d.title;
		if (d.icon != null && ! d.icon.isBlank())
			out.icon = d.icon;
		if (d.isRegionBody())
			out.region = projectRegion(d);
		if (d.barSlot != null)
			out.barSlot = barSlotMap(d.barSlot);
		return out;
	}

	/**
	 * Twin of {@code ViewTable.projectDetailRegion}: copy identity + populate + projected {@code dataUrl}, always
	 * {@code type=row-detail}.  Does not dump L11 descriptor fields and does not mutate the author's
	 * {@link RegionDef}.
	 */
	private static Region projectRegion(RowDetailDef d) {
		var r = d.region;
		var out = new Region();
		out.id = r.id;
		out.type = RegionDef.TYPE_ROW_DETAIL;
		if (r.populate != null && ! r.populate.isBlank())
			out.populate = r.populate;
		out.dataUrl = (r.dataUrl != null && ! r.dataUrl.isBlank()) ? r.dataUrl : d.endpoint;
		if (r.titleFields != null && ! r.titleFields.isEmpty())
			out.titleFields = r.titleFields;
		return out;
	}

	private static Map<String,Object> barSlotMap(BarSlot bar) {
		var m = snapshotMap(bar);
		if (bar.refreshUrl != null && ! bar.refreshUrl.isBlank())
			m.put("refreshUrl", bar.refreshUrl);
		return m;
	}

	@SuppressWarnings("unchecked")
	private static Map<String,Object> snapshotMap(Object bean) {
		return Json.to(Json.of(bean), Map.class);
	}

	@SuppressWarnings("unchecked")
	private static List<?> snapshotList(Object bean) {
		return Json.to(Json.of(bean), List.class);
	}

	private static void resolveServletUrls(ViewSlot slot, RestRequest req) {
		resolveUrls(slot.view, req);
		resolveUrls(slot.bulk, req);
		resolveUrls(slot.detail, req);
		if (slot.savedViewsBase != null)
			slot.savedViewsBase = resolveOne(slot.savedViewsBase, req);
	}

	@SuppressWarnings("unchecked")
	private static void resolveUrls(Object node, RestRequest req) {
		if (node == null)
			return;
		if (node instanceof Map<?,?> raw) {
			var m = (Map<Object,Object>) raw;
			for (var e : List.copyOf(m.entrySet())) {
				var v = e.getValue();
				if (v instanceof String s && URL_KEYS.contains(String.valueOf(e.getKey())))
					m.put(e.getKey(), resolveOne(s, req));
				else
					resolveUrls(v, req);
			}
			return;
		}
		if (node instanceof List<?> list) {
			for (var item : list)
				resolveUrls(item, req);
			return;
		}
		if (node instanceof Detail d) {
			d.endpoint = resolveOne(d.endpoint, req);
			resolveUrls(d.region, req);
			resolveUrls(d.barSlot, req);
			return;
		}
		if (node instanceof Region r) {
			r.dataUrl = resolveOne(r.dataUrl, req);
		}
	}

	private static String resolveOne(String value, RestRequest req) {
		if (value == null || ! value.startsWith("servlet:"))
			return value;
		return req.getUriResolver().resolve(value);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Wire nested types
	//-----------------------------------------------------------------------------------------------------------------

	/** Selection wire: {@code rowIdField} + {@code selectAll}.  Not {@code Json.of(SelectionDef)}. */
	@BeanType(properties="rowIdField,selectAll")
	public static final class Selection {
		/** Stable row-id field name. */
		public String rowIdField;
		/** Select-all header checkbox; defaults true. */
		public boolean selectAll = true;

		static Selection from(SelectionDef def) {
			var s = new Selection();
			s.rowIdField = def.rowIdField();
			s.selectAll = def.selectAll();
			return s;
		}
	}

	/** DETAIL_SLOT wire.  Not {@code Json.of(RowDetailDef)} (that bean carries {@code ServerValues} / {@code lock}). */
	@BeanType(properties="contractVersion,endpoint,title,icon,region,barSlot")
	public static final class Detail {
		/** {@link RowDetailDef#CONTRACT_VERSION}. */
		public String contractVersion;
		/** Expand GET path template. */
		public String endpoint;
		/** Header title template; omitted when unset. */
		public String title;
		/** Header icon name; omitted when unset. */
		public String icon;
		/** The one region this panel's body is. */
		public Region region;
		/** Optional detail bar slot snapshot. */
		public Map<String,Object> barSlot;
	}

	/** DETAIL_SLOT.region: identity + populate + {@code type=row-detail} + projected {@code dataUrl}. */
	@BeanType(properties="id,type,populate,dataUrl,titleFields")
	public static final class Region {
		/** Region id. */
		public String id;
		/** Always {@link RegionDef#TYPE_ROW_DETAIL} on this path. */
		public String type;
		/** Populator name; omitted when blank. */
		public String populate;
		/** Projected fetch URL template. */
		public String dataUrl;
		/** Header-title {@code {field}} allowlist; omitted when unset. */
		public List<String> titleFields;
	}
}
