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
package org.apache.juneau.console;

import java.util.*;

/**
 * Mixin for element builders that can carry a client object-model block: handler actions, lifecycle bindings and
 * handler subscriptions.  Implementers supply {@link #modelBlock()} and {@link #self()}.
 *
 * <h2 class='section'>Example:</h2>
 * <p class='bjava'>
 *   CardSpec.<jsm>of</jsm>(<js>"html"</js>, <js>"orders"</js>)
 *     .action(<js>"archive"</js>, <js>"archive"</js>, <js>"orders.archive"</js>)
 *     .on(<js>"rendered"</js>, <js>"orders.wire"</js>)
 *     .subscribe(<js>"selection:orders"</js>, <js>"orders.onSelect"</js>);
 * </p>
 *
 * @param <T> The builder type.
 * @since 10.0.0
 */
public interface Handled<T> {

	/**
	 * Returns the element's model block.
	 *
	 * @return The block; never <jk>null</jk>.
	 */
	ModelBlock modelBlock();

	/**
	 * Returns this builder.
	 *
	 * @return This object.
	 */
	T self();

	/**
	 * Adds a click action calling a named handler.
	 *
	 * @param id The action id.
	 * @param icon The icon name.
	 * @param handler The handler name.
	 * @return This object.
	 */
	default T action(String id, String icon, String handler) {
		return action(ModelAction.of(id).icon(icon).handler(handler));
	}

	/**
	 * Adds a click action with a payload.
	 *
	 * @param id The action id.
	 * @param icon The icon name.
	 * @param handler The handler name.
	 * @param payload The payload.
	 * @return This object.
	 */
	default T action(String id, String icon, String handler, Map<String,Object> payload) {
		return action(ModelAction.of(id).icon(icon).handler(handler).payload(payload));
	}

	/**
	 * Adds a fully specified action.
	 *
	 * @param a The action.
	 * @return This object.
	 */
	default T action(ModelAction a) {
		modelBlock().action(a);
		return self();
	}

	/**
	 * Binds a lifecycle event to a named handler.
	 *
	 * @param event The event.
	 * @param handler The handler name.
	 * @return This object.
	 */
	default T on(String event, String handler) {
		return on(event, HandlerRef.named(handler));
	}

	/**
	 * Binds a lifecycle event.
	 *
	 * @param event The event.
	 * @param handler The handler.
	 * @return This object.
	 */
	default T on(String event, HandlerRef handler) {
		modelBlock().on(event, handler);
		return self();
	}

	/**
	 * Subscribes a named handler to a topic.
	 *
	 * @param topic The topic; may hold {@code {rowKey}}, {@code {tabId}}, {@code {cardId}}.
	 * @param handler The handler name.
	 * @return This object.
	 */
	default T subscribe(String topic, String handler) {
		return subscribe(topic, HandlerRef.named(handler));
	}

	/**
	 * Subscribes a handler to a topic.
	 *
	 * @param topic The topic.
	 * @param handler The handler.
	 * @return This object.
	 */
	default T subscribe(String topic, HandlerRef handler) {
		modelBlock().subscribe(topic, handler);
		return self();
	}
}
