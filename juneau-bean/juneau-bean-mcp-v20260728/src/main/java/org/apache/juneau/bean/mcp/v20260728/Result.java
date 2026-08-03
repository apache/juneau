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
 * CRTP base for every successful MCP {@code 2026-07-28} result, carrying the required {@code resultType}
 * discriminator and optional {@link ResultMeta} under the reserved {@code _meta} wire key.
 *
 * <p>
 * The {@code 2026-07-28} schema declares {@code resultType} as an open string union: {@code "complete"} and
 * {@code "input_required"} ({@link InputRequiredResult}, MRTR / SEP-2322) are the values this bean model
 * permits. Only {@code "complete"} is emitted by any dispatch path today; {@code "input_required"} becomes
 * reachable once MRTR pause/resume dispatch wiring is in place. Parsing remains lossless regardless and
 * accepts any string a peer might send. The field therefore defaults to {@code "complete"} rather than being
 * validated against a closed enumeration.
 *
 * <p>
 * {@link CacheableResult} extends this base so the five cacheable list/read results inherit {@code resultType}
 * and {@code _meta} alongside their cache hints. Every other concrete result bean ({@link CallToolResult},
 * {@link GetPromptResult}, {@link CompleteResult}, and {@link PingResult}) extends this base directly.
 *
 * <p>
 * The CRTP type parameter lets each concrete subclass's fluent setters return its own concrete type for method
 * chaining, without duplicating the {@code resultType} / {@code _meta} accessors in every subclass.
 *
 * @param <T> The concrete subclass, for fluent-setter self-typing.
 */
public abstract class Result<T extends Result<T>> {

	private String resultType = "complete";
	private ResultMeta meta;

	/**
	 * Result-type discriminator.
	 *
	 * <p>
	 * Defaults to {@code "complete"}. The schema defines this as an open string union, so parsing accepts any
	 * string losslessly.
	 *
	 * @return The result type. Never <jk>null</jk> on a server-constructed instance.
	 */
	public String getResultType() {
		return resultType;
	}

	/**
	 * Sets the result-type discriminator.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings({
		"unchecked" // CRTP subclasses bind T to their own concrete type.
	})
	public T setResultType(String value) {
		resultType = value;
		return (T)this;
	}

	/**
	 * Result metadata: server identity plus any echoed trace context.
	 *
	 * @return The metadata, or {@code null} if not set.
	 */
	@BeanProp("_meta")
	public ResultMeta getMeta() {
		return meta;
	}

	/**
	 * Sets the result metadata.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	@BeanProp("_meta")
	@SuppressWarnings({
		"unchecked" // CRTP subclasses bind T to their own concrete type.
	})
	public T setMeta(ResultMeta value) {
		meta = value;
		return (T)this;
	}
}
