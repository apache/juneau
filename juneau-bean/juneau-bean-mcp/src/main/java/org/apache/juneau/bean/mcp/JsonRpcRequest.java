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
 * JSON-RPC 2.0 request envelope.
 *
 * <p>
 * The {@code id} field may be a {@link String}, integral {@link Number}, or {@code null} (notification).
 */
@Bean
public class JsonRpcRequest {

	private String jsonrpc;
	private Object id;
	private String method;
	private Object params;

	/**
	 * JSON-RPC protocol version (typically {@value org.apache.juneau.bean.mcp.McpProtocol#JSON_RPC_2_0}).
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
	public JsonRpcRequest setJsonrpc(String value) {
		jsonrpc = value;
		return this;
	}

	/**
	 * Request identifier (string, integer, or {@code null} for notifications).
	 *
	 * @return The id, or {@code null}.
	 */
	public Object getId() {
		return id;
	}

	/**
	 * Sets the request id.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public JsonRpcRequest setId(Object value) {
		id = value;
		return this;
	}

	/**
	 * JSON-RPC method name (for example {@link McpMethods#TOOLS_LIST}).
	 *
	 * @return The method, or {@code null} if not set.
	 */
	public String getMethod() {
		return method;
	}

	/**
	 * Sets the JSON-RPC method name.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public JsonRpcRequest setMethod(String value) {
		method = value;
		return this;
	}

	/**
	 * Method parameters (object, array, or {@code null}).
	 *
	 * @return The params payload, or {@code null}.
	 */
	public Object getParams() {
		return params;
	}

	/**
	 * Sets method parameters.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public JsonRpcRequest setParams(Object value) {
		params = value;
		return this;
	}
}
