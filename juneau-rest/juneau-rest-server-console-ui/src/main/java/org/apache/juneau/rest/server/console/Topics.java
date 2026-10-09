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
package org.apache.juneau.rest.server.console;

import java.util.*;
import java.util.regex.*;

/**
 * Builds framework topic names so callers do not hand-format "family:key" strings.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 *   Topics.<jsm>selection</jsm>(<js>"changes"</js>);   <jc>// "selection:changes"</jc>
 *   Topics.<jsm>cmd</jsm>(<js>"tasks"</js>);           <jc>// "cmd:tasks"</jc>
 *   Topics.<jsm>job</jsm>(<jv>ref</jv>.id());          <jc>// "job:j-91"</jc>
 * </p>
 *
 * @since 10.0.0
 */
public final class Topics {

	/** The framework families, in the order {@code JuneauViews.bus.FRAMEWORK_FAMILIES} lists them (pinned by a test). */
	public static final Set<String> FRAMEWORK_FAMILIES = Collections.unmodifiableSet(new LinkedHashSet<>(List.of(
		"card", "selection", "filter", "redraw", "detail", "bulk", "cmd", "probe", "job", "badge", "bridge")));

	// The schema §5.7 grammar.  One copy: Subscription, TopicDecl, BridgeDecl and BusWiringValidator read these, and
	// Topics_Test pins them against juneau-page.schema.json.
	private static final String CUSTOM = "[a-z][a-z0-9-]{0,31}\\.[a-z][a-z0-9-]{0,31}";
	private static final String KEY = "[A-Za-z0-9_.-]{1,128}";
	static final Pattern TOPIC = Pattern.compile("^(" + String.join("|", FRAMEWORK_FAMILIES) + "|" + CUSTOM + ")(:" + KEY + ")?$");
	static final Pattern TOPIC_DECL = Pattern.compile("^(" + CUSTOM + "(:(" + KEY + "|\\*))?|job:\\*)$");
	static final Pattern PUBLICATION = Pattern.compile("^" + CUSTOM + "(:(" + KEY + "|\\*))?$");
	static final Pattern ROLE = Pattern.compile("^[a-z][a-z0-9-]{0,31}$");
	static final Pattern MAP_PATH = Pattern.compile("^[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*$");
	/** Builder-time bridge session: same-origin path (no {@code //} or {@code /\}) or a {@code servlet:} / {@code context:} URI resolved at render. */
	static final Pattern SESSION = Pattern.compile("^(/(?![/\\\\])|servlet:/|context:/)\\S*$");
	static final Pattern ID = Pattern.compile("^[A-Za-z][A-Za-z0-9_-]{0,63}$");
	private static final Pattern KEY_ONLY = Pattern.compile("^" + KEY + "$");

	static final String E40 = "invalid topic '%s': expected family[:key] (see the topic syntax)";
	static final String E45_FRAMEWORK = "'%s' is a framework family; framework topics are implicit and may not be declared";
	static final String E45_NAMESPACE = "custom topic family '%s' must be namespaced (e.g. 'app.%s')";

	private Topics() {}

	/** @param cardId The card id. @return {@code card:<cardId>}. */
	public static String card(String cardId) { return framework("card", cardId); }
	/** @param cardId The card id. @return {@code selection:<cardId>}. */
	public static String selection(String cardId) { return framework("selection", cardId); }
	/** @param cardId The card id. @return {@code filter:<cardId>}. */
	public static String filter(String cardId) { return framework("filter", cardId); }
	/** @param cardId The card id. @return {@code redraw:<cardId>}. */
	public static String redraw(String cardId) { return framework("redraw", cardId); }
	/** @param cardId The card id. @return {@code detail:<cardId>}. */
	public static String detail(String cardId) { return framework("detail", cardId); }
	/** @param cardId The card id. @return {@code bulk:<cardId>}. */
	public static String bulk(String cardId) { return framework("bulk", cardId); }
	/** @param cardId The card id. @return {@code cmd:<cardId>}. */
	public static String cmd(String cardId) { return framework("cmd", cardId); }
	/** @param groupId The probe group id. @return {@code probe:<groupId>}. */
	public static String probe(String groupId) { return framework("probe", groupId); }
	/** @param jobId The async job id. @return {@code job:<jobId>}. */
	public static String job(String jobId) { return framework("job", jobId); }
	/** @param badgeId The badge id. @return {@code badge:<badgeId>}. */
	public static String badge(String badgeId) { return framework("badge", badgeId); }
	/** @param bridgeId The bridge id. @return {@code bridge:<bridgeId>}. */
	public static String bridge(String bridgeId) { return framework("bridge", bridgeId); }

	/**
	 * @param ns The namespace, for example {@code ssc}.
	 * @param name The name within it, for example {@code focus}.
	 * @return {@code ns.name}.
	 * @throws IllegalArgumentException E-45 when {@code ns} is blank; E-40 when a part is malformed.
	 */
	public static String custom(String ns, String name) {
		if (ns == null || ns.isBlank())
			throw iae(E45_NAMESPACE, name, name);
		var t = ns + "." + name;
		if (! TOPIC_DECL.matcher(t).matches())
			throw iae(E40, t);
		return t;
	}

	/**
	 * @param ns The namespace.
	 * @param name The name within it.
	 * @param key The key, or {@code *} for a declaration pattern.
	 * @return {@code ns.name:key}.
	 * @throws IllegalArgumentException E-45 / E-40 as for {@link #custom(String, String)}, or E-40 on a bad key.
	 */
	public static String custom(String ns, String name, String key) {
		var t = custom(ns, name) + ":" + key;
		if (! TOPIC_DECL.matcher(t).matches())
			throw iae(E40, t);
		return t;
	}

	private static String framework(String family, String key) {
		var t = family + ":" + key;
		if (key == null || ! KEY_ONLY.matcher(key).matches())
			throw iae(E40, t);
		return t;
	}

	/** Throws E-40 unless {@code topic} is a concrete topic. */
	static void checkTopic(String topic) {
		if (topic == null || ! TOPIC.matcher(topic).matches())
			throw iae(E40, topic);
	}

	/** @return The text before the first {@code :}, or the whole topic. */
	static String family(String topic) {
		var i = topic.indexOf(':');
		return i < 0 ? topic : topic.substring(0, i);
	}

	static IllegalArgumentException iae(String format, Object... args) {
		return new IllegalArgumentException(String.format(format, args));
	}
}
