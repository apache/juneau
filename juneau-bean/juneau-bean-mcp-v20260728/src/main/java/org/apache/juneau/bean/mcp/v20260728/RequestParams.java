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
package org.apache.juneau.bean.mcp.v20260728;

import org.apache.juneau.commons.bean.*;

/**
 * CRTP base for MCP {@code 2026-07-28} request params carrying optional negotiation/trace metadata under the
 * reserved {@code _meta} wire key.
 *
 * <p>
 * Per the {@code 2026-07-28} schema, every v2 request places its metadata under {@code params._meta} rather than
 * beside the JSON-RPC envelope. The four concrete params beans ({@link CallToolRequest}, {@link GetPromptRequest},
 * {@link ReadResourceRequest}, and {@link CompleteRequest}) extend this base without gaining any wire-visible
 * members beyond their own existing payload fields plus the inherited {@code _meta}. {@link RequestParamsOnly} is
 * the concrete carrier for methods (list/ping/discovery) whose only common params member is {@code _meta}.
 *
 * <p>
 * This base is a lossless wire carrier only: it performs no negotiation validation. Metadata shape and negotiation
 * rules are owned by {@link RequestMeta} and the {@code 2026-07-28} adapter's dispatch validation.
 *
 * <p>
 * The CRTP type parameter lets each concrete subclass's {@link #setMeta(RequestMeta)} return its own concrete type
 * for fluent chaining, without duplicating the {@code _meta} accessor in every subclass.
 *
 * @param <T> The concrete subclass, for fluent-setter self-typing.
 */
public abstract class RequestParams<T extends RequestParams<T>> {

	private RequestMeta meta;

	/**
	 * Per-request negotiation, extension, and trace-context metadata.
	 *
	 * @return The metadata, or {@code null} if not set.
	 */
	@BeanProp("_meta")
	public RequestMeta getMeta() {
		return meta;
	}

	/**
	 * Sets the per-request metadata.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	@BeanProp("_meta")
	@SuppressWarnings({
		"unchecked" // CRTP subclasses bind T to their own concrete type.
	})
	public T setMeta(RequestMeta value) {
		meta = value;
		return (T)this;
	}
}
