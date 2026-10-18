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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;
import java.util.stream.*;

import org.apache.juneau.commons.http.*;
import org.apache.juneau.marshall.collections.*;

/**
 * The built-in {@code markers} decorator: vertical mark lines at x values from a list, a same-origin {@code src}, a
 * {@code data:} topic, or the current time.  The lines ride a carrier series {@code juneau-markers-<n>}.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	Markers.<jsm>from</jsm>(<js>"/rest/releases"</js>).at(<js>"date"</js>).label(<js>"name"</js>).onClick(Topics.<jsm>custom</jsm>(<js>"app"</js>, <js>"release"</js>));
 * 	Markers.<jsm>now</jsm>(<js>"Today"</js>);
 * </p>
 *
 * @since 10.0.0
 */
public final class Markers implements Decorator {

	private static final Set<String> STYLES = Set.of("solid", "dashed", "dotted");

	private final JsonMap m = JsonMap.of("use", "markers");

	private Markers() {}

	/** @param path A same-origin path returning rows. @return A new decorator. */
	public static Markers from(String path) {
		if (! SafePathTemplate.isSameOriginPath(path))
			throw iaex("Markers src must be a same-origin path; got '%s'.", path);
		var x = new Markers();
		x.m.put("src", path);
		return x;
	}

	/** @param rows Inline rows {@code {at, label?, meta?}}. @return A new decorator. */
	public static Markers values(List<? extends Map<String,?>> rows) {
		var x = new Markers();
		x.m.put("values", copyRows(rows));
		return x;
	}

	/** @param dataTopic A {@code data:} topic ({@code Topics.data(...)}). @return A new decorator. */
	public static Markers source(String dataTopic) {
		var x = new Markers();
		x.m.put("source", dataTopicOf("Markers", dataTopic));
		return x;
	}

	/** @param label The label of a mark line at the current time. @return A new decorator. */
	public static Markers now(String label) {
		var x = new Markers();
		x.m.put("now", JsonMap.of("label", label));
		return x;
	}

	/** @param field The row field holding the x value. @return This object. */
	public Markers at(String field) { m.put("at", field); return this; }

	/** @param field The row field holding the line label. @return This object. */
	public Markers label(String field) { m.put("label", field); return this; }

	/** @param v {@code solid}, {@code dashed} or {@code dotted}. @return This object. */
	public Markers style(String v) {
		if (! STYLES.contains(v))
			throw iaex("Markers style '%s' must be 'solid', 'dashed' or 'dotted'.", v);
		m.put("style", v);
		return this;
	}

	/** @param topic A custom topic the card publishes a click on a line to. @return This object. */
	public Markers onClick(String topic) { m.put("topic", topic); return this; }

	@Override
	public JsonMap toJson() { return new JsonMap(m); }

	static List<Map<String,Object>> copyRows(List<? extends Map<String,?>> rows) {
		if (rows == null)
			throw iaex("Decorator values must not be null.");
		return rows.stream().<Map<String,Object>>map(LinkedHashMap::new).collect(Collectors.toCollection(ArrayList::new));
	}

	static String dataTopicOf(String who, String topic) {
		if (topic == null || ! topic.startsWith("data:") || topic.length() == 5)
			throw iaex("%s source must be a data: topic; got '%s'.", who, topic);
		return topic;
	}
}
