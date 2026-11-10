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
package org.apache.juneau.console.views;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;
import java.util.regex.*;

import org.apache.juneau.marshall.*;
import org.apache.juneau.http.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.server.*;

/**
 * Mixin that serves the bean-backed row details of a resource.
 *
 * <p>
 * An implementing resource lists its {@link RowDetail#fromBean(Class, java.util.function.Function) bean-backed
 * details} in {@link #rowDetails()}.  Each detail is served under its key at three endpoints: the detail
 * ({@code row-detail/{key}/{id}}), one tab ({@code row-detail/{key}/{id}/tab/{tabId}}) and the tab counts
 * ({@code row-detail/{key}/{id}/counts}).
 *
 * <p>
 * Only readable, non-ignored bean properties that the owning table shows (or the detail names explicitly) are ever
 * served.  Every failure &mdash; an unknown key, tab or row, a null lookup result or a malformed id &mdash; answers
 * the same empty {@code 404} with {@code Cache-Control: no-store}, so nothing about what exists can be learned
 * from the response.
 *
 * @since 10.0.0
 */
public interface RowDetailMixin {

	/** Grammar of a servable row id. */
	Pattern ROW_ID = Pattern.compile("[A-Za-z0-9._:-]{1,128}");

	/** Grammar of a servable detail key or tab id. */
	Pattern SEGMENT = Pattern.compile("[A-Za-z][A-Za-z0-9-]{0,63}");

	/**
	 * The bean-backed details this resource serves.
	 *
	 * @return The details.  Each must have a distinct key.
	 */
	Collection<RowDetail> rowDetails();

	/**
	 * Whether {@code id} is a servable row id.
	 *
	 * @param id The candidate id.
	 * @return <jk>true</jk> if the id matches the row-id grammar and is not {@code .} or {@code ..}.
	 */
	default boolean validRowId(String id) {
		return id != null && ROW_ID.matcher(id).matches() && ! ".".equals(id) && ! "..".equals(id);
	}

	/**
	 * Returns the details indexed by key.
	 *
	 * @return The index.
	 * @throws IllegalStateException If two details share a key.
	 */
	default Map<String,RowDetail> rowDetailIndex() {
		return indexOf(rowDetails());
	}

	private static Map<String,RowDetail> indexOf(Collection<RowDetail> details) {
		var m = new LinkedHashMap<String,RowDetail>();
		for (var d : details) {
			if (d.key() == null || d.lookup() == null)
				continue;
			if (m.put(d.key(), d) != null)
				throw isex("Two row details share the key '%s'.", d.key());
		}
		return m;
	}

	/**
	 * Serves one row's detail.
	 *
	 * @param key The detail key.
	 * @param id The row id.
	 * @param res The response.
	 * @return The detail, or an empty map with a {@code 404}.
	 */
	@RestGet(path="row-detail/{key}/{id}", summary="Row detail", swagger=@OpSwagger(ignore=true))
	default JsonMap rowDetail(@Path("key") String key, @Path("id") String id, RestResponse res) {
		var d = detail(key);
		var bean = bean(d, id);
		if (bean == null)
			return notFound(res);
		var names = new LinkedHashSet<String>(titleFields(d));
		names.addAll(firstFieldsTab(d));
		var out = RowDetail.response(project(d, bean, names));
		var meta = new JsonMap();
		meta.put("labels", labels(d, names));
		out.put("meta", meta);
		out.put("counts", counts(d, id));
		return out;
	}

	/**
	 * Serves one tab of a row's detail.
	 *
	 * @param key The detail key.
	 * @param id The row id.
	 * @param tabId The tab id.
	 * @param res The response.
	 * @return The tab's fields, or an empty map with a {@code 404}.
	 */
	@RestGet(path="row-detail/{key}/{id}/tab/{tabId}", summary="Row detail tab", swagger=@OpSwagger(ignore=true))
	default JsonMap rowDetailTab(@Path("key") String key, @Path("id") String id, @Path("tabId") String tabId, RestResponse res) {
		var d = detail(key);
		var bean = bean(d, id);
		if (bean == null || tabId == null || ! SEGMENT.matcher(tabId).matches())
			return notFound(res);
		for (var t : d.tabList())
			if (t.kind() == DetailTab.Kind.FIELDS && tabId.equals(t.resolvedId())) {
				var names = new LinkedHashSet<>(t.fieldNames());
				var out = RowDetail.response(project(d, bean, names));
				var meta = new JsonMap();
				meta.put("labels", labels(d, names));
				out.put("meta", meta);
				return out;
			}
		return notFound(res);
	}

	/**
	 * Serves the tab counts of a row.
	 *
	 * @param key The detail key.
	 * @param id The row id.
	 * @param res The response.
	 * @return The counts keyed by tab id, or an empty map with a {@code 404}.
	 */
	@RestGet(path="row-detail/{key}/{id}/counts", summary="Row detail counts", swagger=@OpSwagger(ignore=true))
	default JsonMap rowDetailCounts(@Path("key") String key, @Path("id") String id, RestResponse res) {
		var d = detail(key);
		if (bean(d, id) == null)
			return notFound(res);
		var out = new JsonMap();
		out.put("contractVersion", RowDetail.CONTRACT_VERSION);
		out.put("counts", counts(d, id));
		return out;
	}

	private RowDetail detail(String key) {
		return key == null || ! SEGMENT.matcher(key).matches() ? null : rowDetailIndex().get(key);
	}

	private Object bean(RowDetail d, String id) {
		if (d == null || ! validRowId(id))
			return null;
		try {
			return d.lookup().apply(id);
		} catch (RuntimeException e) {
			return null;
		}
	}

	private static JsonMap notFound(RestResponse res) {
		res.setStatus(404);
		res.setHeader("Cache-Control", "no-store");
		return new JsonMap();
	}

	private static List<String> titleFields(RowDetail d) {
		var out = new ArrayList<String>();
		if (d.titleTemplate() != null) {
			var m = Pattern.compile("\\{([A-Za-z_][A-Za-z0-9_]*)\\}").matcher(d.titleTemplate());
			while (m.find())
				out.add(m.group(1));
		}
		return out;
	}

	private static List<String> firstFieldsTab(RowDetail d) {
		for (var t : d.tabList())
			if (t.kind() == DetailTab.Kind.FIELDS)
				return t.fieldNames().isEmpty() ? d.resolvedFields() : t.fieldNames();
		return d.resolvedFields();
	}

	private static JsonMap project(RowDetail d, Object bean, Collection<String> names) {
		var ok = d.servableProperties();
		var bm = MarshallingContext.DEFAULT.toBeanMap(bean);
		var out = new JsonMap();
		for (var n : names)
			if (ok.contains(n))
				out.put(n, bm.get(n));
		return out;
	}

	private static JsonMap labels(RowDetail d, Collection<String> names) {
		var explicit = d.resolvedLabels();
		var out = new JsonMap();
		for (var n : names)
			out.put(n, RowDetail.resolveLabel(n, explicit.get(n), null, null));
		return out;
	}

	private static JsonMap counts(RowDetail d, String id) {
		var out = new JsonMap();
		for (var t : d.tabList())
			if (t.hasCounter())
				try {
					out.put(t.resolvedId(), t.count(id));
				} catch (RuntimeException e) {
					// a failed counter drops its own count only
				}
		return out;
	}
}
