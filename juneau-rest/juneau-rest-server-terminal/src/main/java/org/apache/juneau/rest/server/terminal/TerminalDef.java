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
package org.apache.juneau.rest.server.terminal;

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.rest.server.runreport.RunViewChecks.*;

import java.util.*;

import org.apache.juneau.rest.server.views.*;

/**
 * The options of a {@code terminal} card: where its bytes and (optionally) its step events come from.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	TerminalDef <jv>def</jv> = TerminalDef.<jsm>forMixin</jsm>(<js>"build"</js>, <js>"/runs"</js>, <js>"r42"</js>)
 * 		.eventsUrl(<js>"/runs/juneau-run-view/r42/events"</js>)
 * 		.title(<js>"Build"</js>)
 * 		.validate();
 * </p>
 *
 * @since 10.0.0
 */
public class TerminalDef {

	/** The keys {@link #fromMap(String, Map)} accepts, in {@link #toMap()} order. */
	public static final List<String> KEYS = List.of("bytesUrl", "eventsUrl", "refreshMs", "title", "tailBytes");

	/** The tail the card replays on its first load when {@link #tailBytes} is not set: 2 MiB. */
	public static final long DEFAULT_TAIL_BYTES = 2L * 1024 * 1024;

	/** The largest {@link #tailBytes}: 1 GiB. */
	public static final long MAX_TAIL_BYTES = 1L << 30;

	/** The card id. */
	public final String id;
	/** The bytes endpoint path. */
	public String bytesUrl;
	/** The run-view events endpoint path, or <jk>null</jk> for no steps. */
	public String eventsUrl;
	/** The poll interval; clamped to {@link ConsoleOutputDef#MIN_REFRESH_MS}. */
	public Long refreshMs;
	/** The card header title. */
	public String title;
	/** The most bytes replayed on the first load. */
	public Long tailBytes;

	private TerminalDef(String id) {
		this.id = id;
	}

	/**
	 * Creates a declaration.
	 *
	 * @param id The card id.
	 * @return A new declaration.
	 */
	public static TerminalDef create(String id) {
		return new TerminalDef(id);
	}

	/**
	 * Creates a declaration whose {@link #bytesUrl} points at a {@link TerminalMixin} mounted at {@code mountPath}.
	 *
	 * @param id The card id.
	 * @param mountPath The resource mount path, for example {@code "/runs"}.
	 * @param terminalId The terminal id; must match {@code [A-Za-z0-9_-]{1,128}}.
	 * @return A new declaration.
	 */
	public static TerminalDef forMixin(String id, String mountPath, String terminalId) {
		if (! isRunId(terminalId))
			throw iaex("TerminalDef '%s' terminalId must match [A-Za-z0-9_-]{1,128}; got '%s'.", id, clip(terminalId));
		var mount = mountPath == null ? "" : mountPath;
		while (mount.endsWith("/"))
			mount = mount.substring(0, mount.length() - 1);
		return create(id).bytesUrl(mount + TerminalMixin.TERMINAL_PREFIX + "/" + terminalId + "/bytes");
	}

	/**
	 * Creates a declaration from a parsed map (the console-ui card body).
	 *
	 * @param id The card id.
	 * @param map The {@code terminal} object; keys must be a subset of {@link #KEYS}.
	 * @return A new declaration (not yet validated).
	 */
	public static TerminalDef fromMap(String id, Map<String,?> map) {
		var d = create(id);
		for (var e : map.entrySet()) {
			var k = e.getKey();
			var v = e.getValue();
			switch (k) {
				case "bytesUrl" -> d.bytesUrl = string(id, k, v);
				case "eventsUrl" -> d.eventsUrl = string(id, k, v);
				case "refreshMs" -> d.refreshMs = integer(id, k, v);
				case "title" -> d.title = string(id, k, v);
				case "tailBytes" -> d.tailBytes = integer(id, k, v);
				default -> throw iaex("TerminalDef '%s' unknown key '%s'; allowed: %s.", id, clip(k), KEYS);
			}
		}
		return d;
	}

	private static String string(String id, String k, Object v) {
		if (! (v instanceof String))
			throw iaex("TerminalDef '%s' %s must be a string.", id, k);
		return (String)v;
	}

	private static Long integer(String id, String k, Object v) {
		if (v instanceof Integer || v instanceof Long || v instanceof Short || v instanceof Byte)
			return ((Number)v).longValue();
		if (v instanceof Number n && n.doubleValue() == Math.rint(n.doubleValue()) && Math.abs(n.doubleValue()) < 1e15)
			return n.longValue();
		throw iaex("TerminalDef '%s' %s must be an integer.", id, k);
	}

	/** @param value The bytes endpoint path. @return This object. */
	public TerminalDef bytesUrl(String value) { bytesUrl = value; return this; }
	/** @param value The run-view events endpoint path. @return This object. */
	public TerminalDef eventsUrl(String value) { eventsUrl = value; return this; }
	/** @param value The poll interval; clamped to {@link ConsoleOutputDef#MIN_REFRESH_MS}. @return This object. */
	public TerminalDef refreshMs(long value) { refreshMs = value; return this; }
	/** @param value The card header title. @return This object. */
	public TerminalDef title(String value) { title = value; return this; }
	/** @param value The most bytes replayed on the first load, 1..{@value #MAX_TAIL_BYTES}. @return This object. */
	public TerminalDef tailBytes(long value) { tailBytes = value; return this; }

	/**
	 * Validates this declaration (fail-closed).
	 *
	 * @return This object.
	 * @throws IllegalArgumentException If a rule is broken.
	 */
	public TerminalDef validate() {
		if (id == null || id.isBlank())
			throw iaex("TerminalDef id must not be blank.");
		if (bytesUrl == null)
			throw iaex("TerminalDef '%s' bytesUrl is required.", id);
		checkUrl("bytesUrl", bytesUrl);
		if (eventsUrl != null)
			checkUrl("eventsUrl", eventsUrl);
		if (refreshMs != null && refreshMs <= 0)
			throw iaex("TerminalDef '%s' refreshMs must be > 0; got %s.", id, refreshMs);
		if (tailBytes != null && (tailBytes < 1 || tailBytes > MAX_TAIL_BYTES))
			throw iaex("TerminalDef '%s' tailBytes must be 1..%s; got %s.", id, MAX_TAIL_BYTES, tailBytes);
		return this;
	}

	private void checkUrl(String name, String url) {
		if (! isSameOriginPath(url))
			throw iaex("TerminalDef '%s' %s must be a same-origin path (no scheme, no leading '//', no '..' segment); got '%s'.",
				id, name, clip(url));
		if (url.contains("{"))
			throw iaex("TerminalDef '%s' %s must not contain a '{' placeholder; got '%s'.", id, name, clip(url));
	}

	/**
	 * Returns the set options in {@link #KEYS} order, with {@code refreshMs} clamped.
	 *
	 * @return A new ordered map.
	 */
	public Map<String,Object> toMap() {
		var m = new LinkedHashMap<String,Object>();
		put(m, "bytesUrl", bytesUrl);
		put(m, "eventsUrl", eventsUrl);
		put(m, "refreshMs", refreshMs == null ? null : Math.max(ConsoleOutputDef.MIN_REFRESH_MS, refreshMs));
		put(m, "title", title);
		put(m, "tailBytes", tailBytes);
		return m;
	}

	private static void put(Map<String,Object> m, String k, Object v) {
		if (v != null)
			m.put(k, v);
	}
}
