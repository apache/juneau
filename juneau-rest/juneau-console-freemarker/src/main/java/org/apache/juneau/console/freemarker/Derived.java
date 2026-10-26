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
package org.apache.juneau.console.freemarker;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.console.*;

/**
 * Declares a derived data source: transforms run in the browser on each new frame of another source, published on
 * <c>data:&lt;as&gt;</c>.
 *
 * <p>
 * The built-in transforms are <c>bucket</c>, <c>filter</c>, <c>groupBy</c> and <c>topN</c>; app transforms are
 * registered under dotted names.  A string arg that is exactly <c>{token}</c> is filled from params.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jv>page</jv>.data(Derived.<jsm>from</jsm>(<js>"orgRequests"</js>)
 * 		.transform(<js>"groupBy"</js>, JsonMap.<jsm>of</jsm>(<js>"by"</js>, List.<jsm>of</jsm>(<js>"reason"</js>),
 * 			<js>"measures"</js>, List.<jsm>of</jsm>(<js>"count"</js>)))
 * 		.as(<js>"noqcByReason"</js>));
 * </p>
 *
 * @since 10.0.0
 */
public final class Derived {

	private final String from;
	private String id;
	private final List<JsonMap> transforms = new ArrayList<>();
	private final List<Subscription> params = new ArrayList<>();

	private Derived(String from) {
		Topics.data(from);
		this.from = DataSourceDef.checkedId(from);
	}

	/**
	 * @param source The input source id.
	 * @return A new declaration.
	 * @throws IllegalArgumentException E-40 if the id is not a valid topic key.
	 */
	public static Derived from(String source) {
		return new Derived(source);
	}

	/**
	 * Appends a transform.
	 *
	 * @param name A registered transform (bare built-in or dotted app name).
	 * @param args The transform's arguments, or <jk>null</jk>.  The map is copied one level deep; nested values are
	 * 	shared, so do not mutate them after the call.
	 * @return This object.
	 */
	public Derived transform(String name, JsonMap args) {
		DataSourceDef.extensionName(name, "transform");
		var t = JsonMap.of("use", name);
		t.put("args", args == null ? new JsonMap() : new JsonMap(args));
		transforms.add(t);
		return this;
	}

	/** @param subs Subscriptions with role <c>params</c>. @return This object. */
	public Derived params(Subscription...subs) {
		for (var s : subs) {
			if (s == null)
				throw iaex(DataSourceDef.E76, id != null ? id : from, "subscription is null");
			params.add(s);
		}
		return this;
	}

	/**
	 * Names the output.
	 *
	 * @param value The derived source id; the topic is <c>data:&lt;value&gt;</c>.
	 * @return This object.
	 */
	public Derived as(String value) {
		Topics.data(value);
		if (value.equals(from))
			throw iaex(DataSourceDef.E76, value, "a derived source cannot read its own topic");
		id = DataSourceDef.checkedId(value);
		return this;
	}

	/** @return The output id, or <jk>null</jk> before {@link #as(String)}. */
	public String id() { return id; }

	/**
	 * The <c>contract.dataSources[]</c> entry.
	 *
	 * @return A new map.
	 * @throws IllegalStateException If {@link #as(String)} was not called.
	 */
	public JsonMap toMap() {
		if (id == null)
			throw isex("Derived.from('%s'): as(id) not called", from);
		var subs = new ArrayList<Object>();
		for (var s : params) {
			var m = s.toMap();
			if (! "params".equals(m.get("as")))
				throw iaex(DataSourceDef.E76, id,
					f("subscription to '%s' has role '%s'; a derived source accepts params", s.topic(), m.get("as")));
			subs.add(m);
		}
		var copies = transforms.stream().map(t -> {
			var c = new JsonMap(t);
			c.put("args", new JsonMap((JsonMap)t.get("args")));
			return c;
		}).toList();
		return JsonMap.of("id", id, "kind", "derived", "from", from, "transforms", copies, "subscribes", subs);
	}
}
