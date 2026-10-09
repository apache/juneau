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
 * Row-detail panel for a table: the per-row GET endpoint, title template, icon and the row-detail region.
 *
 * <p>
 * {@link #CONTRACT_VERSION} and {@link #response(Object)} describe the JSON that endpoint returns.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jv>table</jv>.detail(RowDetail.<jsm>create</jsm>(<js>"/rest/work/data/{id}"</js>)
 * 		.title(<js>"{workId}"</js>).icon(<js>"search"</js>)
 * 		.region(RegionDef.<jsm>create</jsm>(<js>"work-detail"</js>).populate(<js>"work-detail"</js>)
 * 			.allowPopulators(<js>"work-detail"</js>)));
 *
 * 	<jc>// The matching endpoint:</jc>
 * 	<ja>@RestGet</ja>(path=<js>"/data/{id}"</js>)
 * 	<jk>public</jk> JsonMap detail(<ja>@Path</ja> String <jv>id</jv>) {
 * 		<jk>return</jk> RowDetail.<jsm>response</jsm>(<jv>store</jv>.fields(<jv>id</jv>));
 * 	}
 * </p>
 *
 * @since 10.0.0
 */
public final class RowDetail {

	/** The contract version for the detail GET response (the card fragment itself carries no version). */
	public static final String CONTRACT_VERSION = "1";

	private final String endpoint;
	private String title;
	private String icon;
	private RegionDef region;

	private RowDetail(String endpoint) {
		this.endpoint = endpoint;
	}

	/**
	 * Starts a row-detail definition for the given per-row GET endpoint.
	 *
	 * @param endpoint A same-origin path template carrying the literal {@code {id}} row-id placeholder (the client substitutes only that), e.g. {@code "/rest/work/data/{id}"}.
	 * 	Must not be <jk>null</jk> or blank.
	 * @return A new {@link RowDetail}.
	 * @throws IllegalArgumentException If {@code endpoint} is not a same-origin path with the literal {@code {id}} placeholder.
	 */
	public static RowDetail create(String endpoint) {
		if (! RegionDef.isSafeDetailEndpoint(endpoint) || ! endpoint.contains("{id}"))
			throw iaex("RowDetail endpoint '%s' is not a safe detail endpoint (same-origin path with an {id} placeholder).", endpoint);
		return new RowDetail(endpoint);
	}

	/**
	 * Sets the detail panel's title template.
	 *
	 * @param template A row-data interpolation template, e.g. {@code "{workId}"}.  Can be <jk>null</jk> to unset.
	 * @return This object.
	 */
	public RowDetail title(String template) {
		title = template;
		return this;
	}

	/**
	 * Sets the detail panel's icon.
	 *
	 * @param name A glyph name resolved by the shared icon registry.  Can be <jk>null</jk> to unset.
	 * @return This object.
	 */
	public RowDetail icon(String name) {
		icon = name;
		return this;
	}

	/**
	 * Sets the detail panel's region.
	 *
	 * @param value The region.  Its {@link RegionDef#type} defaults to {@link RegionDef#TYPE_ROW_DETAIL} and its
	 * 	{@link RegionDef#dataUrl} defaults to this detail's own endpoint, applied at {@link #toMap()} time onto a
	 * 	copy; this method never mutates {@code value} itself.
	 * @return This object.
	 */
	public RowDetail region(RegionDef value) {
		region = value;
		return this;
	}

	/**
	 * Builds the detail GET endpoint's own JSON response (not the card fragment).
	 *
	 * @param fields The row's detail fields, a map (or bean) in whatever shape the detail region's populator/renderer expects.
	 * @return {@code {contractVersion, fields}}.
	 */
	public static JsonMap response(Object fields) {
		var m = new JsonMap();
		m.put("contractVersion", CONTRACT_VERSION);
		m.put("fields", fields);
		return m;
	}

	/**
	 * Builds this detail's card-body entry: {@code {endpoint, title?, icon?, region}}.
	 *
	 * <p>
	 * Builds from a copy: reads {@link RegionDef#toContractMap()} off the caller's {@link #region(RegionDef) region}
	 * into a fresh map, applies the {@code type}/{@code dataUrl} defaults onto that copy, and strips the copy's
	 * {@code contractVersion} key (a card fragment never nests a {@code contractVersion}, since the client stamps it
	 * on mount).  The caller's {@link RegionDef} instance is never written to.
	 *
	 * @return The detail entry.
	 * @throws IllegalArgumentException If no region was set.
	 */
	public JsonMap toMap() {
		if (region == null)
			throw iaex("RowDetail '%s' requires a region.", endpoint);
		var m = new JsonMap();
		m.put("endpoint", endpoint);
		if (title != null)
			m.put("title", title);
		if (icon != null)
			m.put("icon", icon);
		var regionMap = new LinkedHashMap<>(region.toContractMap());
		regionMap.remove("contractVersion");
		regionMap.putIfAbsent("type", RegionDef.TYPE_ROW_DETAIL);
		regionMap.putIfAbsent("dataUrl", endpoint);
		m.put("region", regionMap);
		return m;
	}
}
