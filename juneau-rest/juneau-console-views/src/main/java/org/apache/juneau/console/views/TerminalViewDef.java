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
import static org.apache.juneau.terminal.report.TerminalViewChecks.clip;

import java.util.*;
import org.apache.juneau.terminal.report.*;

/**
 * The author-facing declaration of a terminal-view region, compiled to a {@link RegionDef}.
 *
 * <p>
 * {@link #toRegion()} produces a region with {@code populate("terminal-view")}, opts that populator in with
 * {@code allowPopulators("terminal-view")}, and carries every setting as a flat {@code params} scalar.  It never sets
 * {@code dataUrl} or {@code refreshMs} on the region: the terminal view owns its own polling loop.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	RegionDef <jv>region</jv> = TerminalViewDef.<jsm>create</jsm>(<js>"run"</js>)
 * 		.eventsUrl(<js>"/runs/42/events"</js>)
 * 		.title(<js>"Run 42"</js>)
 * 		.toRegion();
 * </p>
 */
@SuppressWarnings({
	"java:S1192", // Repeated literals are contract wire keys; named constants would obscure the contract shape.
	"java:S1845" // Fluent-builder setters intentionally mirror field names (Juneau DSL convention)
})
public class TerminalViewDef {

	/** The populator name. */
	public static final String POPULATOR = "terminal-view";

	/** The refresh floor in milliseconds. */
	public static final long MIN_REFRESH_MS = TerminalViewChecks.MIN_REFRESH_MS;

	/** The default poll interval in milliseconds. */
	public static final long DEFAULT_REFRESH_MS = 2000;

	/** The path prefix used by {@link #forMixin(String, String, String)}; equals {@link TerminalViewMixin#RUN_VIEW_PREFIX}. */
	public static final String MIXIN_PREFIX = TerminalViewMixin.RUN_VIEW_PREFIX;

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
	/** Raw-console link template with one {@code {offset}} placeholder. */
	public String rawHref;

	private TerminalViewDef(String id) {
		this.id = id;
	}

	/**
	 * Creates a declaration.
	 *
	 * @param id The region id.
	 * @return A new declaration.
	 */
	public static TerminalViewDef create(String id) {
		return new TerminalViewDef(id);
	}

	/**
	 * Creates a declaration whose events URL points at a {@link TerminalViewMixin} mounted at {@code mountPath}.
	 *
	 * @param id The region id.
	 * @param mountPath The resource mount path, for example {@code "/petstore"}.
	 * @param runIdTemplate {@code "{id}"} (the client row-id placeholder) or a literal run id.
	 * @return A new declaration.
	 */
	public static TerminalViewDef forMixin(String id, String mountPath, String runIdTemplate) {
		if (! ("{id}".equals(runIdTemplate) || TerminalViewChecks.isRunId(runIdTemplate)))
			throw iaex("TerminalViewDef '%s' runIdTemplate must be \"{id}\" or match [A-Za-z0-9_-]{1,128}; got '%s'.", id, clip(runIdTemplate));
		var mount = mountPath == null ? "" : mountPath;
		while (mount.endsWith("/"))
			mount = mount.substring(0, mount.length() - 1);
		return create(id).eventsUrl(mount + MIXIN_PREFIX + "/" + runIdTemplate + "/events");
	}

	/**
	 * Creates a declaration from a parsed map (the console-ui card body).
	 *
	 * @param id The card id.
	 * @param map The {@code terminalView} object; keys must be a subset of {@link #KEYS}.
	 * @return A new declaration (not yet validated).
	 */
	public static TerminalViewDef fromMap(String id, Map<String,?> map) {
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
				default -> throw iaex("TerminalViewDef '%s' unknown key '%s'; allowed: %s.", id, clip(k), KEYS);
			}
		}
		return d;
	}

	private static String string(String id, String k, Object v) {
		if (! (v instanceof String s))
			throw iaex("TerminalViewDef '%s' %s must be a string.", id, k);
		return s;
	}

	private static long integer(String id, String k, Object v) {
		if (v instanceof Integer || v instanceof Long || v instanceof Short || v instanceof Byte)
			return ((Number)v).longValue();
		if (v instanceof Number n && n.doubleValue() == Math.rint(n.doubleValue()) && Math.abs(n.doubleValue()) < 1e15)
			return n.longValue();
		throw iaex("TerminalViewDef '%s' %s must be an integer.", id, k);
	}

	private static boolean bool(String id, String k, Object v) {
		if (! (v instanceof Boolean b))
			throw iaex("TerminalViewDef '%s' %s must be a boolean.", id, k);
		return b;
	}

	/** @param value The region type. @return This object. */
	public TerminalViewDef type(String value) { type = value; return this; }
	/** @param value The events endpoint path template. @return This object. */
	public TerminalViewDef eventsUrl(String value) { eventsUrl = value; return this; }
	/** @param value The poll interval; clamped to {@link #MIN_REFRESH_MS}. @return This object. */
	public TerminalViewDef refreshMs(long value) { refreshMs = value; return this; }
	/** @param value Whether to poll. @return This object. */
	public TerminalViewDef poll(boolean value) { poll = value; return this; }
	/** @param value Compact mode. @return This object. */
	public TerminalViewDef compact(boolean value) { compact = value; return this; }
	/** @param value The accessible name and heading. @return This object. */
	public TerminalViewDef title(String value) { title = value; return this; }
	/** @param value The raw-console link template, with one {@code {offset}}. @return This object. */
	public TerminalViewDef rawHref(String value) { rawHref = value; return this; }

	/**
	 * Validates this declaration (fail-closed).
	 *
	 * @return This object.
	 * @throws IllegalArgumentException If a rule is broken.
	 */
	public TerminalViewDef validate() {
		if (ib(id))
			throw iaex("TerminalViewDef id must not be blank.");
		if (! TYPES.contains(type))
			throw iaex("TerminalViewDef '%s' type must be one of card-body|tab-body|row-detail; got '%s'.", id, clip(type));
		if (eventsUrl == null) {
			if (poll)
				throw iaex("TerminalViewDef '%s' eventsUrl is required unless poll is false.", id);
		} else {
			if (! RegionDef.isSafeDetailEndpoint(eventsUrl))
				throw iaex("TerminalViewDef '%s' eventsUrl must be a same-origin path (no scheme, no leading '//', no '..' segment); got '%s'.",
					id, clip(eventsUrl));
			if (eventsUrl.contains("{id}") && ! RegionDef.TYPE_ROW_DETAIL.equals(type))
				throw iaex("TerminalViewDef '%s' eventsUrl: {id} is only allowed on a row-detail region.", id);
		}
		if (refreshMs <= 0)
			throw iaex("TerminalViewDef '%s' refreshMs must be > 0; got %s.", id, refreshMs);
		if (rawHref != null && ! TerminalViewChecks.isRawHrefTemplate(rawHref))
			throw iaex("TerminalViewDef '%s' rawHref must contain exactly one {offset} and be a same-origin path or fragment; got '%s'.", id, clip(rawHref));
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
