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
 * A custom topic declaration, for a card's {@code publishes} or the page's top-level {@code topics}.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 *   <jc>// Published by this card's JS:</jc>
 *   <jv>card</jv>.publishes(TopicDecl.<jsm>of</jsm>(<js>"ssc.focus"</js>).retain(<jk>true</jk>));
 *
 *   <jc>// Published by a page script or the server:</jc>
 *   PageSpec.<jsm>create</jsm>().topic(TopicDecl.<jsm>of</jsm>(<js>"app.region-picked"</js>).retain(<jk>true</jk>)
 *     .publisher(TopicDecl.Publisher.<jsf>SCRIPT</jsf>));
 * </p>
 *
 * @since 10.0.0
 */
public final class TopicDecl {

	/** Who publishes a page-level topic. */
	public enum Publisher {
		/** A page script. */
		SCRIPT,
		/** The server, through a bridge (R-11). */
		SERVER,
		/** Only ribbon {@code publish} items. */
		RIBBON
	}

	private static final String JOB_ALL = "job:*";

	private final String topic;
	private Boolean retain;
	private Publisher publisher;

	private TopicDecl(String topic) {
		this.topic = topic;
	}

	/**
	 * @param topic A custom topic or {@code ns.name:*}; or {@code job:*}, which is only valid page-level with {@link Publisher#SERVER}.
	 * @return A new declaration.
	 * @throws IllegalArgumentException E-45 for a framework family or an un-namespaced family; E-40 on bad syntax.
	 */
	public static TopicDecl of(String topic) {
		if (topic == null)
			throw Topics.iae(Topics.E40, topic);
		var family = Topics.family(topic);
		if (Topics.FRAMEWORK_FAMILIES.contains(family) && ! JOB_ALL.equals(topic))
			throw Topics.iae(Topics.E45_FRAMEWORK, family);
		if (Topics.ROLE.matcher(family).matches() && ! Topics.FRAMEWORK_FAMILIES.contains(family))
			throw Topics.iae(Topics.E45_NAMESPACE, family, family);
		if (! Topics.TOPIC_DECL.matcher(topic).matches())
			throw Topics.iae(Topics.E40, topic);
		return new TopicDecl(topic);
	}

	/** @param v Whether the bus keeps the last value for late subscribers. Required. @return This object. */
	public TopicDecl retain(boolean v) {
		retain = v;
		return this;
	}

	/** @param p Who publishes the topic. Page-level only. @return This object. */
	public TopicDecl publisher(Publisher p) {
		publisher = Objects.requireNonNull(p, "publisher");
		return this;
	}

	/** @return The topic. */
	public String topic() {
		return topic;
	}

	/** @return The page-level {@code $defs/topicDecl} entry. */
	public JsonMap toMap() {
		if (JOB_ALL.equals(topic) && publisher != Publisher.SERVER)
			throw Topics.iae(Topics.E45_FRAMEWORK, "job");
		var m = base();
		if (publisher == null)
			throw Topics.iae("topic '%s': a page-level topic needs publisher(SCRIPT|SERVER|RIBBON)", topic);
		m.put("publisher", publisher.name().toLowerCase(Locale.ROOT));
		return m;
	}

	/** @return The card-level {@code $defs/publication} entry, for {@code CardSpec.publishes}. */
	public JsonMap toPublicationMap() {
		if (JOB_ALL.equals(topic))
			throw Topics.iae(Topics.E45_FRAMEWORK, "job");
		if (publisher != null)
			throw Topics.iae("topic '%s': publisher is page-level only; a card's publishes may not set it", topic);
		return base();
	}

	private JsonMap base() {
		if (retain == null)
			throw Topics.iae("topic '%s' is declared with retain=unset here; call retain(true) or retain(false)", topic);
		var m = new JsonMap();
		m.put("topic", topic);
		m.put("retain", retain);
		return m;
	}
}
