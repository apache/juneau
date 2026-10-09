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

import org.apache.juneau.marshall.collections.*;

/**
 * One entry of a card's {@code subscribes} list: a concrete topic wired to a role the card type implements.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 *   <jc>// Master/detail: the tasks table reloads with the selected change's id.</jc>
 *   CardSpec <jv>tasks</jv> = CardSpec.<jsm>of</jsm>(<js>"datatables"</js>, <js>"tasks"</js>)
 *     .subscribes(Subscription.<jsm>to</jsm>(Topics.<jsm>selection</jsm>(<js>"changes"</js>)).as(<js>"params"</js>)
 *       .map(<js>"changeId"</js>, <js>"ids.0"</js>).whenEmpty(WhenEmpty.<jsf>CLEAR</jsf>)
 *       .emptyText(<js>"Select a change to see its tasks."</js>));
 *
 *   <jc>// Linked filter, raw payload.</jc>
 *   Subscription.<jsm>to</jsm>(Topics.<jsm>filter</jsm>(<js>"changes"</js>)).as(<js>"filter"</js>);
 * </p>
 *
 * @since 10.0.0
 */
public final class Subscription {

	/** What a subscriber does when the payload is cleared or a mapped path is missing. */
	public enum WhenEmpty {
		/** Paint {@code emptyText} and skip the fetch (the default). */
		CLEAR,
		/** Leave the last render alone. */
		KEEP
	}

	private final String topic;
	private String role, emptyText;
	private WhenEmpty whenEmpty;
	private final Map<String,String> map = new LinkedHashMap<>();

	private Subscription(String topic) {
		this.topic = topic;
	}

	/**
	 * @param topic A concrete topic; no wildcards.
	 * @return A new subscription.
	 * @throws IllegalArgumentException E-40 on bad syntax.
	 */
	public static Subscription to(String topic) {
		Topics.checkTopic(topic);
		return new Subscription(topic);
	}

	/** @param role A role the card type accepts, matching {@code ^[a-z][a-z0-9-]{0,31}$}. @return This object. */
	public Subscription as(String role) {
		if (role == null || ! Topics.ROLE.matcher(role).matches())
			throw Topics.iae("subscription to '%s': role '%s' must match ^[a-z][a-z0-9-]{0,31}$", topic, role);
		this.role = role;
		return this;
	}

	/**
	 * Maps one payload path to a target; repeatable.
	 *
	 * @param target A {@code {token}} name for {@code params}, or a dotted field such as {@code columns.region} for {@code filter}.
	 * @param path A dotted path into the payload, with numeric segments for array indexes, such as {@code ids.0}.
	 * @return This object.
	 */
	public Subscription map(String target, String path) {
		if (target == null || ! Topics.MAP_PATH.matcher(target).matches())
			throw Topics.iae("subscription to '%s': map target '%s' must be a dotted name", topic, target);
		if (path == null || ! Topics.MAP_PATH.matcher(path).matches())
			throw Topics.iae("subscription to '%s': map path '%s' must be a dotted path such as 'ids.0'", topic, path);
		map.put(target, path);
		return this;
	}

	/** @param v {@link WhenEmpty#CLEAR} (the default when unset) or {@link WhenEmpty#KEEP}. @return This object. */
	public Subscription whenEmpty(WhenEmpty v) {
		whenEmpty = Objects.requireNonNull(v, "whenEmpty");
		return this;
	}

	/** @param v The text painted when the payload is empty; the shell's default otherwise. @return This object. */
	public Subscription emptyText(String v) {
		emptyText = v;
		return this;
	}

	/** @return The topic. */
	public String topic() {
		return topic;
	}

	/** @return The {@code $defs/subscription} entry. */
	public JsonMap toMap() {
		if (role == null)
			throw Topics.iae("subscription to '%s' needs as(role)", topic);
		var m = new JsonMap();
		m.put("topic", topic);
		m.put("as", role);
		if (! map.isEmpty()) {
			var mm = new JsonMap();
			mm.putAll(map);
			m.put("map", mm);
		}
		if (whenEmpty != null)
			m.put("whenEmpty", whenEmpty.name().toLowerCase(Locale.ROOT));
		if (emptyText != null)
			m.put("emptyText", emptyText);
		return m;
	}
}
