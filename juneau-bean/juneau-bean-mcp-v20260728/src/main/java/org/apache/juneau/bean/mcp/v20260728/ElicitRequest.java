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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.juneau.marshall.*;

/**
 * One entry of an {@code input_required} pause's {@code inputRequests} map for an elicitation sub-request (MCP
 * {@code 2026-07-28} SEP-2322), built by a {@code tools/call}/{@code prompts/get}/{@code resources/read} handler
 * and placed via {@code org.apache.juneau.rest.server.mcp.v20260728.ElicitationRequests}.
 *
 * <p>
 * {@link #getRequestedSchema()} is a restricted JSON Schema (see
 * {@code org.apache.juneau.bean.mcp.v20260728.ElicitSchema}) describing the expected shape of the end user's
 * answer; it permits only primitive-typed, non-nested top-level properties.
 */
@Marshalled
public class ElicitRequest {

	private String message;
	private Map<String,Object> requestedSchema;

	/**
	 * The prompt shown to the end user.
	 *
	 * @return The message, or {@code null} if not set.
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Sets the prompt shown to the end user.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ElicitRequest setMessage(String value) {
		message = value;
		return this;
	}

	/**
	 * The restricted JSON Schema describing the expected answer shape.
	 *
	 * @return An unmodifiable view of the schema, or {@code null} if not set.
	 */
	public Map<String,Object> getRequestedSchema() {
		return u(requestedSchema);
	}

	/**
	 * Sets the restricted JSON Schema describing the expected answer shape.
	 *
	 * @param value The new value, typically built via {@code ElicitSchema.create()....build()}.  Can be
	 * 	<jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ElicitRequest setRequestedSchema(Map<String,Object> value) {
		requestedSchema = value;
		return this;
	}
}
