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

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.marshall.*;

/**
 * Opaque per-result MCP metadata carried under the JSON-RPC {@code result._meta} property.
 *
 * <p>
 * Every successful v2 result carries server identity under the exact {@code io.modelcontextprotocol/}-prefixed
 * wire name mandated by the {@code 2026-07-28} schema. W3C trace-context keys ({@code traceparent} /
 * {@code tracestate} / {@code baggage}) are the schema's explicit exception and remain bare; they are populated
 * only when an enabled tracer echoes the server span context. Unrecognized metadata members round-trip
 * losslessly, in insertion order, through the {@code "*"} dynamic-property triplet ({@link #extraKeys()},
 * {@link #get(String)}, {@link #set(String, Object)}).
 *
 * <p>
 * {@link #KEY_SERVER_INFO} is the single source of truth for the result-identity wire key; the {@code
 * 2026-07-28} adapter and bean/adapter tests consume this constant rather than repeating the literal string.
 * The W3C trace-context keys are bare and therefore need no analogous constant: {@link RequestMeta} centralizes
 * {@link RequestMeta#KEY_TRACEPARENT}, {@link RequestMeta#KEY_TRACESTATE}, and {@link RequestMeta#KEY_BAGGAGE}
 * for the request side of the same carrier.
 */
@Marshalled
public class ResultMeta {

	/** Exact wire key for the server implementation identity carried in successful result metadata. */
	public static final String KEY_SERVER_INFO = "io.modelcontextprotocol/serverInfo";

	private Implementation serverInfo;
	private String traceparent;
	private String tracestate;
	private String baggage;
	private Map<String,Object> extra;

	/**
	 * Server implementation identity.
	 *
	 * @return The server info, or {@code null} if not set.
	 */
	@BeanProp(KEY_SERVER_INFO)
	public Implementation getServerInfo() {
		return serverInfo;
	}

	/**
	 * Sets the server implementation identity.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	@BeanProp(KEY_SERVER_INFO)
	public ResultMeta setServerInfo(Implementation value) {
		serverInfo = value;
		return this;
	}

	/**
	 * W3C trace-context parent (bare key; not renamed under the {@code io.modelcontextprotocol/} prefix).
	 *
	 * @return The traceparent value, or {@code null} if not set.
	 */
	public String getTraceparent() {
		return traceparent;
	}

	/**
	 * Sets the W3C trace-context parent.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ResultMeta setTraceparent(String value) {
		traceparent = value;
		return this;
	}

	/**
	 * W3C trace-context state (bare key; not renamed under the {@code io.modelcontextprotocol/} prefix).
	 *
	 * @return The tracestate value, or {@code null} if not set.
	 */
	public String getTracestate() {
		return tracestate;
	}

	/**
	 * Sets the W3C trace-context state.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ResultMeta setTracestate(String value) {
		tracestate = value;
		return this;
	}

	/**
	 * W3C baggage (bare key; not renamed under the {@code io.modelcontextprotocol/} prefix).
	 *
	 * @return The baggage value, or {@code null} if not set.
	 */
	public String getBaggage() {
		return baggage;
	}

	/**
	 * Sets the W3C baggage.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ResultMeta setBaggage(String value) {
		baggage = value;
		return this;
	}

	/**
	 * Extension metadata member keyset.
	 *
	 * <p>
	 * Covers any metadata member other than the identity/trace properties declared above. Preserves insertion
	 * order.
	 *
	 * @return All extension keys on this metadata object.  Never <jk>null</jk>.
	 */
	@BeanProp("*")
	public Set<String> extraKeys() {
		return extra == null ? Collections.emptySet() : u(extra.keySet());
	}

	/**
	 * Extension metadata member getter.
	 *
	 * @param property The property name.  Must not be <jk>null</jk>.
	 * @return The property value, or <jk>null</jk> if the property does not exist or is not set.
	 */
	@BeanProp("*")
	public Object get(String property) {
		return extra == null ? null : extra.get(property);
	}

	/**
	 * Extension metadata member setter.
	 *
	 * <p>
	 * The extension map is lazily initialized on first use and preserves insertion order across repeated calls.
	 *
	 * @param property The property name.  Must not be <jk>null</jk>.
	 * @param value The new value for the property.  Can be <jk>null</jk> (stored as <jk>null</jk>).
	 * @return This object (for method chaining).
	 */
	@BeanProp("*")
	public ResultMeta set(String property, Object value) {
		if (extra == null)
			extra = map();
		extra.put(property, value);
		return this;
	}
}
