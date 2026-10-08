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
import static org.apache.juneau.rest.server.views.ConsoleOutputChecks.clip;

import java.util.*;
import org.apache.juneau.rest.server.runreport.*;

/**
 * The author-facing declaration of a run-view region, compiled to a {@link RegionDef}.
 *
 * <p>
 * {@link #toRegion()} produces a region with {@code populate("run-view")}, opts that populator in with
 * {@code allowPopulators("run-view")}, and carries every setting as a flat {@code params} scalar.  It never sets
 * {@code dataUrl} or {@code refreshMs} on the region: the run view owns its own polling loop.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	RegionDef <jv>region</jv> = RunViewDef.<jsm>create</jsm>(<js>"run"</js>)
 * 		.eventsUrl(<js>"/runs/42/events"</js>)
 * 		.title(<js>"Run 42"</js>)
 * 		.toRegion();
 * </p>
 */
public class RunViewDef {

	/** The populator name. */
	public static final String POPULATOR = "run-view";

	/** The refresh floor in milliseconds. */
	public static final long MIN_REFRESH_MS = ConsoleOutputDef.MIN_REFRESH_MS;

	/** The default poll interval in milliseconds. */
	public static final long DEFAULT_REFRESH_MS = 2000;

	/** The path prefix used by {@link #forMixin(String, String, String)}; equals {@link RunViewMixin#RUN_VIEW_PREFIX}. */
	public static final String MIXIN_PREFIX = RunViewMixin.RUN_VIEW_PREFIX;

	/** The param keys, in emission order. */
	public static final List<String> KEYS = List.of("eventsUrl", "refreshMs", "poll", "compact", "title", "rawHref");

	private static final Set<String> TYPES = Set.of(RegionDef.TYPE_CARD_BODY, RegionDef.TYPE_TAB_BODY, RegionDef.TYPE_ROW_DETAIL);

	/** Region id. */
	public final String id;
	/** Region type; defaults to {@link RegionDef#TYPE_CARD_BODY}. */
	public String type = RegionDef.TYPE_CARD_BODY;
	/** Events endpoint path template; required unless {@link #poll} is <jk>false</jk>. */
	public String eventsUrl;
	/** Poll interval in milliseconds. */
	public long refreshMs = DEFAULT_REFRESH_MS;
	/** Whether the region polls {@link #eventsUrl}; <jk>false</jk> makes a region that only takes appended events. */
	public boolean poll = true;
	/** Compact mode. */
	public boolean compact;
	/** Accessible name and heading. */
	public String title;
	/** Raw-console link template with one {@code {line}} placeholder. */
	public String rawHref;

	private RunViewDef(String id) {
		this.id = id;
	}

	/**
	 * Creates a declaration.
	 *
	 * @param id The region id.
	 * @return A new declaration.
	 */
	public static RunViewDef create(String id) {
		return new RunViewDef(id);
	}

	/**
	 * Creates a declaration whose events URL points at a {@link RunViewMixin} mounted at {@code mountPath}.
	 *
	 * @param id The region id.
	 * @param mountPath The resource mount path, for example {@code "/petstore"}.
	 * @param runIdTemplate {@code "{id}"} (the client row-id placeholder) or a literal run id.
	 * @return A new declaration.
	 */
	public static RunViewDef forMixin(String id, String mountPath, String runIdTemplate) {
		if (! ("{id}".equals(runIdTemplate) || RunViewChecks.isRunId(runIdTemplate)))
			throw iaex("RunViewDef '%s' runIdTemplate must be \"{id}\" or match [A-Za-z0-9_-]{1,128}; got '%s'.", id, clip(runIdTemplate));
		var mount = mountPath == null ? "" : mountPath;
		while (mount.endsWith("/"))
			mount = mount.substring(0, mount.length() - 1);
		return create(id).eventsUrl(mount + MIXIN_PREFIX + "/" + runIdTemplate + "/events");
	}

	/**
	 * Creates a declaration from a parsed map (the console-ui card body).
	 *
	 * @param id The card id.
	 * @param map The {@code runView} object; keys must be a subset of {@link #KEYS}.
	 * @return A new declaration (not yet validated).
	 */
	public static RunViewDef fromMap(String id, Map<String,?> map) {
		var d = create(id);
		for (var e : map.entrySet()) {
			var k = e.getKey();
			var v = e.getValue();
			switch (k) {
				case "eventsUrl" -> d.eventsUrl = string(id, k, v);
				case "refreshMs" -> d.refreshMs = integer(id, k, v);
				case "poll" -> d.poll = bool(id, k, v);
				case "compact" -> d.compact = bool(id, k, v);
				case "title" -> d.title = string(id, k, v);
				case "rawHref" -> d.rawHref = string(id, k, v);
				default -> throw iaex("RunViewDef '%s' unknown key '%s'; allowed: %s.", id, clip(k), KEYS);
			}
		}
		return d;
	}

	private static String string(String id, String k, Object v) {
		if (! (v instanceof String s))
			throw iaex("RunViewDef '%s' %s must be a string.", id, k);
		return s;
	}

	private static long integer(String id, String k, Object v) {
		if (v instanceof Integer || v instanceof Long || v instanceof Short || v instanceof Byte)
			return ((Number)v).longValue();
		if (v instanceof Number n && n.doubleValue() == Math.rint(n.doubleValue()) && Math.abs(n.doubleValue()) < 1e15)
			return n.longValue();
		throw iaex("RunViewDef '%s' %s must be an integer.", id, k);
	}

	private static boolean bool(String id, String k, Object v) {
		if (! (v instanceof Boolean b))
			throw iaex("RunViewDef '%s' %s must be a boolean.", id, k);
		return b;
	}

	/** @param value The region type. @return This object. */
	public RunViewDef type(String value) { type = value; return this; }
	/** @param value The events endpoint path template. @return This object. */
	public RunViewDef eventsUrl(String value) { eventsUrl = value; return this; }
	/** @param value The poll interval; clamped to {@link #MIN_REFRESH_MS}. @return This object. */
	public RunViewDef refreshMs(long value) { refreshMs = value; return this; }
	/** @param value Whether to poll. @return This object. */
	public RunViewDef poll(boolean value) { poll = value; return this; }
	/** @param value Compact mode. @return This object. */
	public RunViewDef compact(boolean value) { compact = value; return this; }
	/** @param value The accessible name and heading. @return This object. */
	public RunViewDef title(String value) { title = value; return this; }
	/** @param value The raw-console link template, with one {@code {line}}. @return This object. */
	public RunViewDef rawHref(String value) { rawHref = value; return this; }

	/**
	 * Validates this declaration (fail-closed).
	 *
	 * @return This object.
	 * @throws IllegalArgumentException If a rule is broken.
	 */
	public RunViewDef validate() {
		if (id == null || id.isBlank())
			throw iaex("RunViewDef id must not be blank.");
		if (! TYPES.contains(type))
			throw iaex("RunViewDef '%s' type must be one of card-body|tab-body|row-detail; got '%s'.", id, clip(type));
		if (eventsUrl == null) {
			if (poll)
				throw iaex("RunViewDef '%s' eventsUrl is required unless poll is false.", id);
		} else {
			if (! RegionDef.isSafeDetailEndpoint(eventsUrl))
				throw iaex("RunViewDef '%s' eventsUrl must be a same-origin path (no scheme, no leading '//', no '..' segment); got '%s'.",
					id, clip(eventsUrl));
			if (eventsUrl.contains("{id}") && ! RegionDef.TYPE_ROW_DETAIL.equals(type))
				throw iaex("RunViewDef '%s' eventsUrl: {id} is only allowed on a row-detail region.", id);
		}
		if (refreshMs <= 0)
			throw iaex("RunViewDef '%s' refreshMs must be > 0; got %s.", id, refreshMs);
		if (rawHref != null && ! RunViewChecks.isRawHrefTemplate(rawHref))
			throw iaex("RunViewDef '%s' rawHref must contain exactly one {line} and be a same-origin path or fragment; got '%s'.", id, clip(rawHref));
		return this;
	}

	/**
	 * Returns the set params in {@link #KEYS} order, with {@code refreshMs} clamped.
	 *
	 * @return A new ordered map.
	 */
	public Map<String,Object> toMap() {
		var m = new LinkedHashMap<String,Object>();
		put(m, "eventsUrl", eventsUrl);
		put(m, "refreshMs", Math.max(MIN_REFRESH_MS, refreshMs));
		put(m, "poll", poll);
		put(m, "compact", compact);
		put(m, "title", title);
		put(m, "rawHref", rawHref);
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
