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
import static org.apache.juneau.rest.server.views.ConsoleOutputChecks.*;

import java.util.*;

/**
 * The author-facing declaration of a console-output region, compiled to a {@link RegionDef}.
 *
 * <p>
 * {@link #toRegion()} produces a region with {@code populate("console-output")}, opts that populator in with
 * {@code allowPopulators("console-output")}, and carries every setting as a flat {@code params} scalar. It never
 * sets {@code dataUrl} or {@code refreshMs} on the region: the console owns its own append-only polling loop.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	RegionDef <jv>region</jv> = ConsoleOutputDef.<jsm>create</jsm>(<js>"build-log"</js>)
 * 		.linesUrl(<js>"/jobs/7/lines"</js>)
 * 		.downloadUrl(<js>"/jobs/7/download"</js>)
 * 		.title(<js>"Build output"</js>)
 * 		.toRegion();
 * </p>
 *
 * @since 10.0.0
 */
public class ConsoleOutputDef {

	/** The populator name. */
	public static final String POPULATOR = "console-output";

	/** The refresh floor in milliseconds. */
	public static final long MIN_REFRESH_MS = 1000;

	/** The largest {@code tail} and {@code earlierLimit}. */
	public static final int MAX_LINES_PER_REQUEST = 10_000;

	/** The path prefix used by {@link #forMixin(String, String, String)}; equals {@code ConsoleOutputMixin.CONSOLE_OUTPUT_PREFIX}. */
	public static final String MIXIN_PREFIX = "/juneau-console-output";

	/** The param keys, in emission order. */
	public static final List<String> KEYS = List.of("linesUrl", "downloadUrl", "refreshMs", "tail", "earlierLimit", "rows",
		"compact", "anchorPrefix", "showTime", "markers", "title");

	private static final Set<String> TYPES = Set.of(RegionDef.TYPE_CARD_BODY, RegionDef.TYPE_TAB_BODY, RegionDef.TYPE_ROW_DETAIL);
	private static final Set<String> MARKER_MODES = Set.of("dim", "hide", "show");

	/** Region id. */
	public final String id;
	/** Region type; defaults to {@link RegionDef#TYPE_CARD_BODY}. */
	public String type = RegionDef.TYPE_CARD_BODY;
	/** Lines endpoint path template (required). */
	public String linesUrl;
	/** Download endpoint path template. */
	public String downloadUrl;
	/** Poll interval in milliseconds. */
	public Long refreshMs;
	/** Initial tail size, 0 to load from the start. */
	public Integer tail;
	/** {@code limit} of each earlier request. */
	public Integer earlierLimit;
	/** Visible rows. */
	public Integer rows;
	/** Compact mode. */
	public Boolean compact;
	/** Row id prefix. */
	public String anchorPrefix;
	/** Show {@code instant} in the gutter. */
	public Boolean showTime;
	/** Initial marker mode: {@code dim}, {@code hide} or {@code show}. */
	public String markers;
	/** Console header title. */
	public String title;

	private ConsoleOutputDef(String id) {
		this.id = id;
	}

	/**
	 * Creates a declaration.
	 *
	 * @param id The region id.
	 * @return A new declaration.
	 */
	public static ConsoleOutputDef create(String id) {
		return new ConsoleOutputDef(id);
	}

	/**
	 * Creates a declaration whose URLs point at a {@code ConsoleOutputMixin} mounted at {@code mountPath}.
	 *
	 * @param id The region id.
	 * @param mountPath The resource mount path, for example {@code "/petstore"}.
	 * @param logIdTemplate {@code "{id}"} (the client row-id placeholder) or a literal log id.
	 * @return A new declaration.
	 */
	public static ConsoleOutputDef forMixin(String id, String mountPath, String logIdTemplate) {
		if (! ("{id}".equals(logIdTemplate) || isLogId(logIdTemplate)))
			throw iaex("ConsoleOutputDef '%s' logIdTemplate must be \"{id}\" or match [A-Za-z0-9_-]{1,128}; got '%s'.", id, clip(logIdTemplate));
		var mount = mountPath == null ? "" : mountPath;
		while (mount.endsWith("/"))
			mount = mount.substring(0, mount.length() - 1);
		var base = mount + MIXIN_PREFIX + "/" + logIdTemplate;
		return create(id).linesUrl(base + "/lines").downloadUrl(base + "/download");
	}

	/**
	 * Creates a declaration from a parsed map (the console-ui card body).
	 *
	 * @param id The card id.
	 * @param map The {@code output} object; keys must be a subset of {@link #KEYS}.
	 * @return A new declaration (not yet validated).
	 */
	public static ConsoleOutputDef fromMap(String id, Map<String,?> map) {
		var d = create(id);
		for (var e : map.entrySet()) {
			var k = e.getKey();
			var v = e.getValue();
			switch (k) {
				case "linesUrl" -> d.linesUrl = string(id, k, v);
				case "downloadUrl" -> d.downloadUrl = string(id, k, v);
				case "refreshMs" -> d.refreshMs = integer(id, k, v);
				case "tail" -> d.tail = (int)(long)integer(id, k, v);
				case "earlierLimit" -> d.earlierLimit = (int)(long)integer(id, k, v);
				case "rows" -> d.rows = (int)(long)integer(id, k, v);
				case "compact" -> d.compact = bool(id, k, v);
				case "anchorPrefix" -> d.anchorPrefix = string(id, k, v);
				case "showTime" -> d.showTime = bool(id, k, v);
				case "markers" -> d.markers = string(id, k, v);
				case "title" -> d.title = string(id, k, v);
				default -> throw iaex("ConsoleOutputDef '%s' unknown key '%s'; allowed: %s.", id, clip(k), KEYS);
			}
		}
		return d;
	}

	private static String string(String id, String k, Object v) {
		if (! (v instanceof String))
			throw iaex("ConsoleOutputDef '%s' %s must be a string.", id, k);
		return (String)v;
	}

	private static Long integer(String id, String k, Object v) {
		if (v instanceof Integer || v instanceof Long || v instanceof Short || v instanceof Byte)
			return ((Number)v).longValue();
		if (v instanceof Number n && n.doubleValue() == Math.rint(n.doubleValue()) && Math.abs(n.doubleValue()) < 1e15)
			return n.longValue();
		throw iaex("ConsoleOutputDef '%s' %s must be an integer.", id, k);
	}

	private static Boolean bool(String id, String k, Object v) {
		if (! (v instanceof Boolean))
			throw iaex("ConsoleOutputDef '%s' %s must be a boolean.", id, k);
		return (Boolean)v;
	}

	/** @param value The region type. @return This object. */
	public ConsoleOutputDef type(String value) { type = value; return this; }
	/** @param value The lines endpoint path template. @return This object. */
	public ConsoleOutputDef linesUrl(String value) { linesUrl = value; return this; }
	/** @param value The download endpoint path template. @return This object. */
	public ConsoleOutputDef downloadUrl(String value) { downloadUrl = value; return this; }
	/** @param value The poll interval; clamped to {@link #MIN_REFRESH_MS}. @return This object. */
	public ConsoleOutputDef refreshMs(long value) { refreshMs = value; return this; }
	/** @param value The initial tail size, 0..10000. @return This object. */
	public ConsoleOutputDef tail(int value) { tail = value; return this; }
	/** @param value The earlier-page limit, 1..10000. @return This object. */
	public ConsoleOutputDef earlierLimit(int value) { earlierLimit = value; return this; }
	/** @param value Visible rows, 3..200. @return This object. */
	public ConsoleOutputDef rows(int value) { rows = value; return this; }
	/** @param value Compact mode. @return This object. */
	public ConsoleOutputDef compact(boolean value) { compact = value; return this; }
	/** @param value The row id prefix. @return This object. */
	public ConsoleOutputDef anchorPrefix(String value) { anchorPrefix = value; return this; }
	/** @param value Show times in the gutter. @return This object. */
	public ConsoleOutputDef showTime(boolean value) { showTime = value; return this; }
	/** @param value Initial marker mode. @return This object. */
	public ConsoleOutputDef markers(String value) { markers = value; return this; }
	/** @param value The console header title. @return This object. */
	public ConsoleOutputDef title(String value) { title = value; return this; }

	/**
	 * Validates this declaration (fail-closed).
	 *
	 * @return This object.
	 * @throws IllegalArgumentException If a rule is broken.
	 */
	public ConsoleOutputDef validate() {
		if (id == null || id.isBlank())
			throw iaex("ConsoleOutputDef id must not be blank.");
		if (! TYPES.contains(type))
			throw iaex("ConsoleOutputDef '%s' type must be one of card-body|tab-body|row-detail; got '%s'.", id, clip(type));
		if (linesUrl == null)
			throw iaex("ConsoleOutputDef '%s' linesUrl is required.", id);
		checkUrl("linesUrl", linesUrl);
		if (downloadUrl != null)
			checkUrl("downloadUrl", downloadUrl);
		if (refreshMs != null && refreshMs <= 0)
			throw iaex("ConsoleOutputDef '%s' refreshMs must be > 0; got %s.", id, refreshMs);
		if (tail != null && (tail < 0 || tail > MAX_LINES_PER_REQUEST))
			throw iaex("ConsoleOutputDef '%s' tail must be 0..10000; got %s.", id, tail);
		if (earlierLimit != null && (earlierLimit < 1 || earlierLimit > MAX_LINES_PER_REQUEST))
			throw iaex("ConsoleOutputDef '%s' earlierLimit must be 1..10000; got %s.", id, earlierLimit);
		if (rows != null && (rows < 3 || rows > 200))
			throw iaex("ConsoleOutputDef '%s' rows must be 3..200; got %s.", id, rows);
		if (anchorPrefix != null && ! isAnchorPrefix(anchorPrefix))
			throw iaex("ConsoleOutputDef '%s' anchorPrefix must match ^[A-Za-z][A-Za-z0-9_-]{0,31}$; got '%s'.", id, clip(anchorPrefix));
		if (markers != null && ! MARKER_MODES.contains(markers))
			throw iaex("ConsoleOutputDef '%s' markers must be one of dim|hide|show; got '%s'.", id, clip(markers));
		return this;
	}

	private void checkUrl(String name, String url) {
		if (! RegionDef.isSafeDetailEndpoint(url))
			throw iaex("ConsoleOutputDef '%s' %s must be a same-origin path (no scheme, no leading '//', no '..' segment); got '%s'.",
				id, name, clip(url));
		if (url.contains("{id}") && ! RegionDef.TYPE_ROW_DETAIL.equals(type))
			throw iaex("ConsoleOutputDef '%s' %s: {id} is only allowed on a row-detail region.", id, name);
	}

	/**
	 * Returns the set params in {@link #KEYS} order, with {@code refreshMs} clamped.
	 *
	 * @return A new ordered map.
	 */
	public Map<String,Object> toMap() {
		var m = new LinkedHashMap<String,Object>();
		put(m, "linesUrl", linesUrl);
		put(m, "downloadUrl", downloadUrl);
		put(m, "refreshMs", refreshMs == null ? null : Math.max(MIN_REFRESH_MS, refreshMs));
		put(m, "tail", tail);
		put(m, "earlierLimit", earlierLimit);
		put(m, "rows", rows);
		put(m, "compact", compact);
		put(m, "anchorPrefix", anchorPrefix);
		put(m, "showTime", showTime);
		put(m, "markers", markers);
		put(m, "title", title);
		return m;
	}

	private static void put(Map<String,Object> m, String k, Object v) {
		if (v != null)
			m.put(k, v);
	}

	/**
	 * Validates and compiles this declaration.
	 *
	 * @return A validated region.
	 */
	public RegionDef toRegion() {
		validate();
		var r = RegionDef.create(id).type(type).populate(POPULATOR).allowPopulators(POPULATOR).params(toMap());
		r.validate();
		return r;
	}
}
