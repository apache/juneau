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
 * JSON-RPC 2.0 error object.
 */
@Bean
public class JsonRpcError {

	private int code;
	private String message;
	private Object data;

	/**
	 * Error code.
	 *
	 * @return The code.
	 */
	public int getCode() {
		return code;
	}

	/**
	 * Sets the error code.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public JsonRpcError setCode(int value) {
		code = value;
		return this;
	}

	/**
	 * Error message.
	 *
	 * @return The message, or {@code null} if not set.
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Sets the error message.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public JsonRpcError setMessage(String value) {
		message = value;
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
	public JsonRpcError setData(Object value) {
		data = value;
		return this;
	}
}
