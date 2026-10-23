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
package org.apache.juneau.rest.server.view.freemarker.console;

import static org.apache.juneau.commons.utils.CollectionUtils.*;

import java.util.*;
import java.util.regex.*;

/**
 * Save and restore of a dashboard's state topics.
 *
 * <p>
 * The listed topics are saved per browser under the namespace, 500 ms after the last change.  On load the first of
 * these that yields a value wins: an app link decoder, then a {@code #s=} share link (with {@link #shareLink()}), then
 * the saved state when its version matches, then each owner's default.  Bump {@link #version(int)} whenever a payload
 * shape changes; saved state of another version is deleted.
 * </p>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	PersistSpec.<jsm>page</jsm>(<js>"app.orders"</js>).version(1)
 * 		.topics(Topics.<jsm>filter</jsm>(<js>"range"</js>), Topics.<jsm>zoom</jsm>(<js>"daily"</js>))
 * 		.shareLink();
 * </p>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S1845" // Fluent-builder setters intentionally mirror field names (Juneau DSL convention)
})
public final class PersistSpec {

	private static final Pattern NS = Pattern.compile("^[a-z][a-z0-9.-]{0,63}$");
	private static final Pattern STATE_TOPIC = Pattern.compile("^(filter|zoom|legend):[A-Za-z][A-Za-z0-9_-]{0,63}$");

	private final String ns;
	private int version = 1;
	private final List<String> topics = new ArrayList<>();
	private boolean shareLink;
	private final List<String> decoders = new ArrayList<>();

	private PersistSpec(String ns) {
		this.ns = ns;
	}

	/**
	 * Creates a per-page persistence spec.
	 *
	 * @param ns The storage namespace, matching {@code ^[a-z][a-z0-9.-]{0,63}$}.
	 * @return A new spec.
	 */
	public static PersistSpec page(String ns) {
		return new PersistSpec(ns);
	}

	/**
	 * Sets the state version.
	 *
	 * @param value The version, at least 1.  Defaults to 1.
	 * @return This object.
	 */
	public PersistSpec version(int value) {
		version = value;
		return this;
	}

	/**
	 * Appends the topics to save and restore.
	 *
	 * @param values {@code filter:}, {@code zoom:} or {@code legend:} topics owned by members of the dashboard.
	 * @return This object.
	 */
	public PersistSpec topics(String...values) {
		topics.addAll(List.of(values));
		return this;
	}

	/**
	 * Enables {@code #s=} share links and the "Copy link" button.
	 *
	 * @return This object.
	 */
	public PersistSpec shareLink() {
		shareLink = true;
		return this;
	}

	/**
	 * Appends an app link decoder, registered in JS with {@code JuneauDashboard.decoder.register(name, fn)}.
	 *
	 * <p>
	 * The decoder must be registered before {@code juneau-charts.js} loads (from the {@code window.JuneauConsoleDashboard}
	 * pre-load queue in a head script, or from a toolkit pack), because restore runs when the first chart mounts.  A page
	 * {@code init} script is too late.
	 * </p>
	 *
	 * @param name The dotted decoder name.
	 * @return This object.
	 */
	public PersistSpec decoder(String name) {
		decoders.add(name);
		return this;
	}

	/** @return The topics, in order. */
	List<String> topics() {
		return topics;
	}

	/**
	 * Checks this spec, reporting E-77 under {@code dash}.
	 *
	 * @param dash The dashboard id.
	 */
	void check(String dash) {
		if (ns == null || ! NS.matcher(ns).matches())
			throw Section.e77(dash, "namespace '" + ns + "' does not match " + NS.pattern());
		if (version < 1)
			throw Section.e77(dash, "persist version " + version + " must be at least 1");
		if (topics.isEmpty())
			throw Section.e77(dash, "persist needs at least one topic");
		var seen = new HashSet<String>();
		for (var t : topics) {
			if (t == null || ! STATE_TOPIC.matcher(t).matches())
				throw Section.e77(dash, "persist topic '" + t + "' is not a filter:, zoom: or legend: state topic");
			if (! seen.add(t))
				throw Section.e77(dash, "persist lists topic '" + t + "' twice");
		}
		var names = new HashSet<String>();
		for (var d : decoders) {
			if (! Section.isDottedName(d))
				throw Section.e77(dash, "decoder name '" + d + "' is not a dotted extension name");
			if (! names.add(d))
				throw Section.e77(dash, "decoder '" + d + "' is listed twice");
		}
	}

	/** @return The persist JSON. */
	Map<String,Object> toMap() {
		Map<String,Object> m = map(
			"ns", ns,
			"version", version,
			"topics", List.copyOf(topics),
			"shareLink", shareLink,
			"decoders", List.copyOf(decoders)
		);
		return m;
	}
}
