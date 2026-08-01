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
 * Opaque per-request MCP metadata carried under the JSON-RPC {@code params._meta} property.
 *
 * <p>
 * Every v2 request is independently negotiated from its own {@code _meta}; no handshake or session state exists.
 * Negotiation and identity keys use the exact {@code io.modelcontextprotocol/}-prefixed wire names mandated by the
 * {@code 2026-07-28} schema; W3C trace-context keys ({@code traceparent} / {@code tracestate} / {@code baggage})
 * are the schema's explicit exception and remain bare. Unrecognized metadata members round-trip losslessly, in
 * insertion order, through the {@code "*"} dynamic-property triplet ({@link #extraKeys()}, {@link #get(String)},
 * {@link #set(String, Object)}).
 *
 * <p>
 * The exact wire-key constants below are the single source of truth for negotiation/identity/trace metadata keys;
 * the {@code 2026-07-28} adapter, its validation messages, and bean/adapter tests consume these constants rather
 * than repeating the literal strings.
 */
@Marshalled
public class RequestMeta {

	/** Exact wire key for the required negotiated protocol version. */
	public static final String KEY_PROTOCOL_VERSION = "io.modelcontextprotocol/protocolVersion";

	/** Exact wire key for the optional client implementation identity. */
	public static final String KEY_CLIENT_INFO = "io.modelcontextprotocol/clientInfo";

	/** Exact wire key for the required client capability advertisement. */
	public static final String KEY_CLIENT_CAPABILITIES = "io.modelcontextprotocol/clientCapabilities";

	/** Exact wire key for the deprecated, accepted-but-ignored client log level. */
	public static final String KEY_LOG_LEVEL = "io.modelcontextprotocol/logLevel";

	/** Bare (unprefixed) W3C trace-context parent key. */
	public static final String KEY_TRACEPARENT = "traceparent";

	/** Bare (unprefixed) W3C trace-context state key. */
	public static final String KEY_TRACESTATE = "tracestate";

	/** Bare (unprefixed) W3C baggage key. */
	public static final String KEY_BAGGAGE = "baggage";

	private String protocolVersion;
	private Implementation clientInfo;
	private ClientCapabilities clientCapabilities;
	private String logLevel;
	private String traceparent;
	private String tracestate;
	private String baggage;
	private Map<String,Object> extra;

	/**
	 * Protocol version requested by the client.
	 *
	 * @return The protocol version, or {@code null} if not set.
	 */
	@BeanProp(KEY_PROTOCOL_VERSION)
	public String getProtocolVersion() {
		return protocolVersion;
	}

	/**
	 * Sets the protocol version.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	@BeanProp(KEY_PROTOCOL_VERSION)
	public RequestMeta setProtocolVersion(String value) {
		protocolVersion = value;
		return this;
	}

	/**
	 * Client implementation identity.
	 *
	 * <p>
	 * Optional (the schema's {@code SHOULD}, not {@code MUST}); validated only when present.
	 *
	 * @return The client info, or {@code null} if not set.
	 */
	@BeanProp(KEY_CLIENT_INFO)
	public Implementation getClientInfo() {
		return clientInfo;
	}

	/**
	 * Sets the client implementation identity.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	@BeanProp(KEY_CLIENT_INFO)
	public RequestMeta setClientInfo(Implementation value) {
		clientInfo = value;
		return this;
	}

	/**
	 * Client capability advertisement.
	 *
	 * @return The client capabilities, or {@code null} if not set.
	 */
	@BeanProp(KEY_CLIENT_CAPABILITIES)
	public ClientCapabilities getClientCapabilities() {
		return clientCapabilities;
	}

	/**
	 * Sets the client capability advertisement.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	@BeanProp(KEY_CLIENT_CAPABILITIES)
	public RequestMeta setClientCapabilities(ClientCapabilities value) {
		clientCapabilities = value;
		return this;
	}

	/**
	 * Deprecated client log level.
	 *
	 * <p>
	 * Accepted and parsed for backward compatibility, but otherwise ignored by the {@code 2026-07-28} adapter.
	 *
	 * @return The log level, or {@code null} if not set.
	 */
	@BeanProp(KEY_LOG_LEVEL)
	public String getLogLevel() {
		return logLevel;
	}

	/**
	 * Sets the deprecated client log level.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	@BeanProp(KEY_LOG_LEVEL)
	public RequestMeta setLogLevel(String value) {
		logLevel = value;
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
	public RequestMeta setTraceparent(String value) {
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
	public RequestMeta setTracestate(String value) {
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
	public RequestMeta setBaggage(String value) {
		baggage = value;
		return this;
	}

	/**
	 * Extension metadata member keyset.
	 *
	 * <p>
	 * Covers any metadata member other than the negotiation/identity/trace properties declared above. Preserves
	 * insertion order.
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
	public RequestMeta set(String property, Object value) {
		if (extra == null)
			extra = map();
		extra.put(property, value);
		return this;
	}
}
