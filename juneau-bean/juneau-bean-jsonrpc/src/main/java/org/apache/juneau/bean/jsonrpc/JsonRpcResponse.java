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
package org.apache.juneau.bean.jsonrpc;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.marshall.*;

/**
 * JSON-RPC 2.0 success or error response envelope.
 *
 * <p>
 * Exactly one of {@code result} or {@code error} should be present for a valid response.
 */
@Marshalled
public class JsonRpcResponse {

	private static final String JSONRPC_2_0 = "2.0";

	private String jsonrpc;
	private Object id;
	private Object result;
	private JsonRpcError error;
	private Object meta;

	/**
	 * JSON-RPC protocol version.
	 *
	 * @return The version token, or {@code null} if not set.
	 */
	public String getJsonrpc() {
		return jsonrpc;
	}

	/**
	 * Sets the JSON-RPC protocol version.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public JsonRpcResponse setJsonrpc(String value) {
		jsonrpc = value;
		return this;
	}

	/**
	 * Correlates to the {@link JsonRpcRequest#getId() request id}.
	 *
	 * @return The id, or {@code null} if not set.
	 */
	public Object getId() {
		return id;
	}

	/**
	 * Sets the response id.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public JsonRpcResponse setId(Object value) {
		id = value;
		return this;
	}

	/**
	 * Result payload on success.
	 *
	 * @return The result, or {@code null} if not set.
	 */
	public Object getResult() {
		return result;
	}

	/**
	 * Sets the result payload.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public JsonRpcResponse setResult(Object value) {
		result = value;
		return this;
	}

	/**
	 * Error payload on failure.
	 *
	 * @return The error, or {@code null} if not set.
	 */
	public JsonRpcError getError() {
		return error;
	}

	/**
	 * Sets the error payload.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public JsonRpcResponse setError(JsonRpcError value) {
		error = value;
		return this;
	}

	/**
	 * Opaque extension metadata.
	 *
	 * @return The metadata value, or <jk>null</jk> if absent.
	 */
	@BeanProp("_meta")
	public Object getMeta() {
		return meta;
	}

	/**
	 * Sets opaque extension metadata.
	 *
	 * @param value Any JSON-compatible value. Can be <jk>null</jk>.
	 * @return This object.
	 */
	@BeanProp("_meta")
	public JsonRpcResponse setMeta(Object value) {
		meta = value;
		return this;
	}

	/**
	 * Tests whether a JSON-RPC id identifies a notification (a request that must not be answered).
	 *
	 * <p>
	 * A JSON-RPC request with no {@code id} is a notification; the server performs the work and returns
	 * no response body.
	 *
	 * @param id The request id. Can be <jk>null</jk>.
	 * @return <jk>true</jk> if the id is <jk>null</jk>.
	 */
	public static boolean notification(Object id) {
		return id == null;
	}

	/**
	 * Creates a JSON-RPC 2.0 success response.
	 *
	 * @param id The request id to correlate against. Can be <jk>null</jk>.
	 * @param result The result payload. Can be <jk>null</jk>.
	 * @return A new response object. Never <jk>null</jk>.
	 */
	public static JsonRpcResponse ok(Object id, Object result) {
		return new JsonRpcResponse()
			.setJsonrpc(JSONRPC_2_0)
			.setId(id)
			.setResult(result);
	}

	/**
	 * Creates a JSON-RPC 2.0 error response with no structured error data.
	 *
	 * @param id The request id to correlate against. Can be <jk>null</jk>.
	 * @param code The JSON-RPC error code.
	 * @param message The error message. Can be <jk>null</jk>.
	 * @return A new response object. Never <jk>null</jk>.
	 */
	public static JsonRpcResponse errorResponse(Object id, int code, String message) {
		return errorResponse(id, code, message, null);
	}

	/**
	 * Creates a JSON-RPC 2.0 error response.
	 *
	 * @param id The request id to correlate against. Can be <jk>null</jk>.
	 * @param code The JSON-RPC error code.
	 * @param message The error message. Can be <jk>null</jk>.
	 * @param data Optional structured error data. Can be <jk>null</jk> to leave the property unset.
	 * @return A new response object. Never <jk>null</jk>.
	 */
	public static JsonRpcResponse errorResponse(Object id, int code, String message, Object data) {
		return new JsonRpcResponse()
			.setJsonrpc(JSONRPC_2_0)
			.setId(id)
			.setError(new JsonRpcError().setCode(code).setMessage(message).setData(data));
	}
}
