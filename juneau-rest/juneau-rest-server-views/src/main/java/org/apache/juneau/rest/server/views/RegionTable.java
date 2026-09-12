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

import static org.apache.juneau.bean.html5.HtmlBuilder.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.StringUtils.escapeForScript;

import java.util.*;

import org.apache.juneau.bean.html5.*;
import org.apache.juneau.marshall.marshaller.*;

/**
 * Builds the server-rendered delivery tree for a {@link RegionDef} &mdash; an <b>empty</b>
 * {@code data-juneau-region} container plus <b>one sidecar per host</b> carrying every region descriptor that host
 * declares.
 *
 * <h5 class='section'>The container carries identity; the sidecar carries configuration:</h5>
 * <p>
 * The container is deliberately empty and deliberately attribute-only &mdash; id, type, and the handshake stamp and
 * nothing else. Configuration (the populator name, {@code dataUrl}, {@code params}, {@code renderer}, {@code lazy},
 * {@code refreshMs}, the field catalog) rides the sidecar, for the same reason {@code PAGE_META} does: an attribute
 * per descriptor key would put author strings in attribute position on every host, and a JSON envelope is the shape
 * the client already parses. The attributes are exactly what the runtime scans for, mirroring the existing
 * {@code data-juneau-page} / {@code data-juneau-view} / {@code data-juneau-card} markers.
 *
 * <p>
 * <b>One sidecar per host, not per region.</b> One {@code <script>} per card in a twelve-card grid is twelve parses;
 * the existing pattern is one sidecar per view / page / header / bar, and this follows it.
 *
 * <h5 class='section'>Two sidecar forms, and why the second exists:</h5>
 * <p>
 * {@link #sidecar(String, List)} stamps a document-unique HTML {@code id}, for an ordinary once-per-response host
 * (a page, a card grid). {@link #detailSidecar(List)} ships {@code id}-less and is found by attribute instead,
 * because a row-detail sidecar lives inside the {@code <template data-juneau-row-detail>} that is <b>cloned per
 * expanded row</b> &mdash; a stamped {@code id} would collide across every open panel. This is exactly the split
 * {@link BarSlotTable#sidecar(BarSlot)} / {@link BarSlotTable#detailSidecar(BarSlot)} already makes, for the same
 * reason.
 *
 * @since 10.0.0
 */
public class RegionTable {

	/** Marker attribute carrying a region's own id: {@code data-juneau-region}. */
	public static final String REGION_ATTR = "data-juneau-region";

	/** Attribute carrying the region's type ({@link RegionDef#TYPE_ROW_DETAIL} and friends). */
	public static final String REGION_TYPE_ATTR = "data-juneau-region-type";

	/**
	 * Attribute carrying the region's {@linkplain RegionDef#populate populator name}:
	 * {@code data-juneau-region-populate}.
	 *
	 * <p>
	 * <b>This attribute is how the populator name reaches the runtime at all, so omitting it fails silently in the
	 * worst possible way.</b> {@code juneau-regions.js}'s {@code mintRegion} reads the name with
	 * {@code el.getAttribute("data-juneau-region-populate")}; a container without it yields {@code null}, which
	 * resolves to the <b>default</b> populator rather than to an error. So a page whose author registered a named
	 * populate would paint the framework default instead, with no console error, no error pane, and a rendered
	 * result that looks plausible &mdash; the author's populate simply never runs.
	 *
	 * <p>
	 * Deliberately an <b>attribute</b> rather than a sidecar member, unlike most of a region's configuration. The
	 * name is needed to decide <i>which function to call</i> at mint time, before any per-host sidecar has been
	 * matched to this container; identity-and-dispatch data rides the element, and everything else rides the sidecar.
	 */
	public static final String REGION_POPULATE_ATTR = "data-juneau-region-populate";

	/** Attribute carrying {@link RegionDef#CONTRACT_VERSION} on the region container (the per-host handshake stamp). */
	public static final String REGION_CONTRACT_ATTR = "data-juneau-region-contract";

	/**
	 * Attribute carrying the enclosing response's CSRF token on a region host that is <b>not</b> a {@code <table>}
	 * (fork F22) &mdash; deliberately {@link ViewTable#CSRF_ATTR}, the <b>same</b> attribute the view table already
	 * stamps, not a region-specific second name.
	 *
	 * <p>
	 * A region that performs a write has no token source of its own: the existing stamp lives on the view
	 * {@code <table>}, and a card-body or tab-body region is not beneath one. Rather than mint a second token or
	 * teach every populate to hunt for the table's, the host that emits a non-table region stamps the same
	 * per-response token it already holds.
	 *
	 * <p>
	 * Reusing the existing attribute name is what makes this cost <b>zero client change</b>: {@code ctx.write(...)}
	 * already resolves its token with {@code el.closest("[data-juneau-csrf]")}, so a region container carrying this
	 * attribute is found by the code that is already there. A region-specific name would have required teaching that
	 * resolver a second selector, and the two would then have been able to disagree.
	 *
	 * <p>
	 * A row-detail region <b>is</b> beneath the view table and therefore does <b>not</b> carry this attribute: it
	 * inherits the table's by {@code closest(...)}, and a second stamp on the same subtree is the hazard rather than
	 * the fix for it.
	 */
	public static final String REGION_CSRF_ATTR = ViewTable.CSRF_ATTR;

	/** Attribute the {@code id}-less {@link #detailSidecar(List)} form is found by. */
	public static final String REGION_META_ATTR = "data-juneau-region-meta";

	/** HTML {@code id} prefix for the {@link #sidecar(String, List)} form. */
	public static final String SIDECAR_ID_PREFIX = "juneau-region:";

	/** The {@code class} on every region container. */
	public static final String REGION_CLASS = "juneau-region";

	private RegionTable() {}

	/**
	 * Emits one empty region container.
	 *
	 * <p>
	 * In-table region HTML ({@link ViewTable}'s detail template).  Page slots: author HTML plus
	 * {@code JuneauViews.regions.mount({ id: populatorName })}.  Not {@code @Deprecated}.
	 * </p>
	 *
	 * @param region The region. Must not be <jk>null</jk>.
	 * @return An empty {@code <div class='juneau-region'>} carrying identity, type and the handshake stamp.
	 */
	public static Div of(RegionDef region) {
		return of(region, null);
	}

	/**
	 * Emits one empty region container, optionally carrying the non-table host's CSRF stamp (F22).
	 *
	 * <p>
	 * In-table region HTML ({@link ViewTable}'s detail template).  Page slots: author HTML plus
	 * {@code JuneauViews.regions.mount({ id: populatorName })}.  Not {@code @Deprecated}.
	 * </p>
	 *
	 * @param region The region. Must not be <jk>null</jk>.
	 * @param csrfToken The enclosing response's token for a non-table host, or <jk>null</jk> to omit the stamp
	 * 	&mdash; which is the correct choice for a row-detail region, since it sits beneath the view table's own
	 * 	stamp and must not carry a second one.
	 * @return An empty {@code <div class='juneau-region'>}.
	 */
	public static Div of(RegionDef region, String csrfToken) {
		if (region == null)
			throw iaex("region must not be null.");
		if (region.id == null || region.id.isBlank())
			throw iaex("RegionDef id must not be null or blank.");
		var d = div()
			.class_(REGION_CLASS)
			.attr(REGION_ATTR, region.id)
			.attr(REGION_CONTRACT_ATTR, RegionDef.CONTRACT_VERSION);
		if (region.type != null)
			d.attr(REGION_TYPE_ATTR, region.type);
		// Only when the author named one: an ABSENT attribute means "use the default populator", which is exactly
		// what mintRegion infers from a null read.  Stamping a blank would be a third state nobody handles.
		if (region.populate != null && ! region.populate.isBlank())
			d.attr(REGION_POPULATE_ATTR, region.populate);
		if (csrfToken != null && ! csrfToken.isBlank())
			d.attr(REGION_CSRF_ATTR, csrfToken);
		return d;
	}

	/**
	 * Emits the per-host sidecar with a document-unique HTML {@code id}.
	 *
	 * <p>
	 * No in-table keeper in this module's production emit path; leftover page-slot callers migrate to
	 * {@code JuneauViews.regions.mount}.  Not {@code @Deprecated}.
	 * </p>
	 *
	 * @param hostId The host's own id (a page id, a card-grid id); the sidecar's {@code id} is
	 * 	{@link #SIDECAR_ID_PREFIX} + this. Must not be <jk>null</jk> or blank.
	 * @param regions Every region this host declares, in emit order. Must not be <jk>null</jk> or empty.
	 * @return The {@code <script type='application/json'>} sidecar.
	 */
	public static Script sidecar(String hostId, List<RegionDef> regions) {
		if (hostId == null || hostId.isBlank())
			throw iaex("hostId must not be null or blank.");
		var s = sidecar(regions);
		s.id(SIDECAR_ID_PREFIX + hostId);
		return s;
	}

	/**
	 * Emits the {@code id}-less, attribute-found sidecar for a host whose subtree is cloned per row.
	 *
	 * <p>
	 * See this class's Javadoc: a row-detail sidecar lives inside the cloned
	 * {@code <template data-juneau-row-detail>}, so a stamped {@code id} would collide across every open panel.
	 *
	 * <p>
	 * In-table region HTML ({@link ViewTable}'s detail template).  Not {@code @Deprecated}.
	 * </p>
	 *
	 * @param regions Every region this host declares, in emit order. Must not be <jk>null</jk> or empty.
	 * @return The {@code <script type='application/json'>} sidecar, carrying {@link #REGION_META_ATTR}.
	 */
	public static Script detailSidecar(List<RegionDef> regions) {
		var s = sidecar(regions);
		s.attr(REGION_META_ATTR, "1");
		return s;
	}

	private static Script sidecar(List<RegionDef> regions) {
		if (regions == null || regions.isEmpty())
			throw iaex("regions must not be null or empty.");
		var json = escapeForScript(Json.of(buildMeta(regions)));
		return script().type("application/json").text(rawText(json));
	}

	/**
	 * Builds the sidecar envelope: the contract version once, then one descriptor per region in emit order.
	 *
	 * @param regions The regions, in emit order. Must not be <jk>null</jk> or empty.
	 * @return An ordered {@link java.util.Map} ready for {@code Json.of(...)}.
	 */
	public static java.util.Map<String,Object> buildMeta(List<RegionDef> regions) {
		if (regions == null || regions.isEmpty())
			throw iaex("regions must not be null or empty.");
		var descriptors = new ArrayList<java.util.Map<String,Object>>();
		for (var r : regions) {
			if (r == null)
				throw iaex("regions entry must not be null.");
			descriptors.add(r.toContractMap());
		}
		var m = new LinkedHashMap<String,Object>();
		m.put("contractVersion", RegionDef.CONTRACT_VERSION);
		m.put("regions", descriptors);
		return m;
	}
}
