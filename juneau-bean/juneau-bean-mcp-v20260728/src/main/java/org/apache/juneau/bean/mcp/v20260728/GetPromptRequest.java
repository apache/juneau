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

import org.apache.juneau.marshall.*;

/**
 * Parameters for {@value McpMethods#PROMPTS_GET}.
 */
@Marshalled
public class GetPromptRequest extends RequestParams<GetPromptRequest> {

	private String name;
	private Map<String,Object> arguments;
	private Map<String,Object> inputResponses;
	private String requestState;

	/**
	 * Prompt name.
	 *
	 * @return The name, or {@code null} if not set.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the prompt name.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public GetPromptRequest setName(String value) {
		name = value;
		return this;
	}

	/**
	 * Argument values keyed by argument name.
	 *
	 * @return The arguments map, or {@code null} if not set.
	 */
	public Map<String,Object> getArguments() {
		return u(arguments);
	}

	/**
	 * Sets argument values.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public GetPromptRequest setArguments(Map<String,Object> value) {
		arguments = value;
		return this;
	}

	/**
	 * Convenience method to add a single argument value.
	 *
	 * @param name The argument name.  Can be <jk>null</jk> ({@link LinkedHashMap} tolerates a <jk>null</jk> key).
	 * @param value The argument value.  Can be <jk>null</jk> (stored as <jk>null</jk>).
	 * @return This object (for method chaining).
	 */
	public GetPromptRequest putArgument(String name, Object value) {
		if (arguments == null)
			arguments = map();
		arguments.put(name, value);
		return this;
	}

	/**
	 * Server-assigned-id-keyed map of collected answers to a prior {@code input_required} pause, echoed back on a
	 * resume call.
	 *
	 * @return The map, or {@code null} if not set.
	 */
	public Map<String,Object> getInputResponses() {
		return u(inputResponses);
	}

	/**
	 * Sets the collected-answers map.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public GetPromptRequest setInputResponses(Map<String,Object> value) {
		inputResponses = value;
		return this;
	}

	/**
	 * Convenience method to add a single collected answer.
	 *
	 * @param id The server-assigned id.  Can be <jk>null</jk> ({@link LinkedHashMap} tolerates a <jk>null</jk> key).
	 * @param value The answer value.  Can be <jk>null</jk> (stored as <jk>null</jk>).
	 * @return This object (for method chaining).
	 */
	public GetPromptRequest putInputResponse(String id, Object value) {
		if (inputResponses == null)
			inputResponses = map();
		inputResponses.put(id, value);
		return this;
	}

	/**
	 * The opaque continuation token echoed back from a prior {@code input_required} result.
	 *
	 * @return The token, or {@code null} if not set.
	 */
	public String getRequestState() {
		return requestState;
	}

	/**
	 * Sets the echoed continuation token.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public GetPromptRequest setRequestState(String value) {
		requestState = value;
		return this;
	}
}
