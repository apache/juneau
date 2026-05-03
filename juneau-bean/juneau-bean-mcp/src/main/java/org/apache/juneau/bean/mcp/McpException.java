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
 * Runtime exception carrying JSON-RPC error fields for mapping to {@link JsonRpcError}.
 */
@Bean
public class McpException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private int code;
	private Object data;

	/**
	 * Constructor.
	 *
	 * @param code JSON-RPC error code.
	 * @param message Error message.
	 */
	public McpException(int code, String message) {
		super(message);
		this.code = code;
	}

	/**
	 * Constructor.
	 *
	 * @param code JSON-RPC error code.
	 * @param message Error message.
	 * @param data Optional structured error data.
	 */
	public McpException(int code, String message, Object data) {
		super(message);
		this.code = code;
		this.data = data;
	}

	/**
	 * JSON-RPC error code.
	 *
	 * @return The code.
	 */
	public int getCode() {
		return code;
	}

	/**
	 * Sets the JSON-RPC error code.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public McpException setCode(int value) {
		code = value;
		return this;
	}

	/**
	 * Optional structured error data.
	 *
	 * @return The data, or {@code null} if not set.
	 */
	public Object getData() {
		return data;
	}

	/**
	 * Sets optional structured error data.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public McpException setData(Object value) {
		data = value;
		return this;
	}

	/**
	 * Converts this exception to a {@link JsonRpcError}.
	 *
	 * @return A new error object. Never {@code null}.
	 */
	public JsonRpcError toJsonRpcError() {
		return new JsonRpcError()
			.setCode(code)
			.setMessage(getMessage())
			.setData(data);
	}
}
