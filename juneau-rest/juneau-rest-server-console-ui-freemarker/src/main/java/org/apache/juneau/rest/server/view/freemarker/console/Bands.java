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

import org.apache.juneau.commons.http.*;
import org.apache.juneau.marshall.collections.*;

/**
 * The built-in {@code bands} decorator: shaded x ranges from a list, a same-origin {@code src}, a {@code data:} topic,
 * or the gaps in the first series ({@link #gaps()}).  The areas ride a carrier series {@code juneau-bands-<n>}.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	Bands.<jsm>from</jsm>(<js>"/rest/outages"</js>).start(<js>"from"</js>).end(<js>"to"</js>).label(<js>"Outage"</js>);
 * 	Bands.<jsm>gaps</jsm>().label(<js>"No data"</js>);
 * </p>
 *
 * @since 10.0.0
 */
public final class Bands implements Decorator {

	private final JsonMap m = JsonMap.of("use", "bands");

	private Bands() {}

	/** @param path A same-origin path returning rows. @return A new decorator. */
	public static Bands from(String path) {
		if (! SafePathTemplate.isSameOriginPath(path))
			throw iaex("Bands src must be a same-origin path; got '%s'.", path);
		var x = new Bands();
		x.m.put("src", path);
		return x;
	}

	/** @param rows Inline rows {@code {start, end, label?}}. @return A new decorator. */
	public static Bands values(List<? extends Map<String,?>> rows) {
		var x = new Bands();
		x.m.put("values", Markers.copyRows(rows));
		return x;
	}

	/** @param dataTopic A {@code data:} topic. @return A new decorator. */
	public static Bands source(String dataTopic) {
		var x = new Bands();
		x.m.put("source", Markers.dataTopicOf("Bands", dataTopic));
		return x;
	}

	/** Shades the runs of empty buckets in the first series. @return A new decorator. */
	public static Bands gaps() {
		var x = new Bands();
		x.m.put("auto", "gaps");
		return x;
	}

	/** @param field The row field holding the range start. @return This object. */
	public Bands start(String field) { m.put("start", field); return this; }

	/** @param field The row field holding the range end. @return This object. */
	public Bands end(String field) { m.put("end", field); return this; }

	/** @param v The band label. @return This object. */
	public Bands label(String v) { m.put("label", v); return this; }

	/** @param buckets The shortest gap shaded by {@link #gaps()}, at least 1. @return This object. */
	public Bands minGap(int buckets) {
		if (buckets < 1)
			throw iaex("Bands minGap '%s' must be at least 1.", buckets);
		m.put("minGap", buckets);
		return this;
	}

	/** @param topic A custom topic the card publishes a click on a band to. @return This object. */
	public Bands onClick(String topic) { m.put("topic", topic); return this; }

	@Override
	public JsonMap toJson() { return new JsonMap(m); }
}
