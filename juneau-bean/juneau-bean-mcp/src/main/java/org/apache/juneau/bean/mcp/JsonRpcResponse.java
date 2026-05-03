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
package org.apache.juneau.bean.mcp;

import org.apache.juneau.annotation.*;

/**
 * JSON-RPC 2.0 success or error response envelope.
 *
 * <p>
 * Exactly one of {@code result} or {@code error} should be present for a valid response.
 */
@Bean
public class JsonRpcResponse {

	private String jsonrpc;
	private Object id;
	private Object result;
	private JsonRpcError error;

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
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public JsonRpcResponse setJsonrpc(String value) {
		jsonrpc = value;
		return this;
	}

	/**
	 * Correlates to the {@link JsonRpcRequest#getId() request id}.
	 *
	 * @return The id, or {@code null}.
	 */
	public Object getId() {
		return id;
	}

	/**
	 * Sets the response id.
	 *
	 * @param value The new value.
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
	 * @param value The new value.
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
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public JsonRpcResponse setError(JsonRpcError value) {
		error = value;
		return this;
	}
}
