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
package org.apache.juneau.rest.server.mcp;

import java.util.*;

/**
 * Revision-neutral invocation of an {@link McpCompleter}.
 *
 * <p>
 * Supersedes the wire-level {@code CompleteRequest} bean. Built by a dated adapter after it has
 * validated the incoming {@code completion/complete} request and resolved the target
 * {@link McpCompleter}; by the time an adapter constructs one of these, {@link #getValue()} is
 * guaranteed non-<jk>null</jk>.
 */
public class McpCompletionRequest {

	private McpCompletionRef ref;
	private String argumentName;
	private String value;
	private Map<String,String> contextArguments = Map.of();

	/**
	 * The completion target.
	 *
	 * @return The reference, or <jk>null</jk> if not set.
	 */
	public McpCompletionRef getRef() {
		return ref;
	}

	/**
	 * Sets the completion target.
	 *
	 * @param value The new value. Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public McpCompletionRequest setRef(McpCompletionRef value) {
		ref = value;
		return this;
	}

	/**
	 * The name of the prompt argument or template variable being completed.
	 *
	 * @return The argument name, or <jk>null</jk> if not set.
	 */
	public String getArgumentName() {
		return argumentName;
	}

	/**
	 * Sets the name of the prompt argument or template variable being completed.
	 *
	 * @param value The new value. Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public McpCompletionRequest setArgumentName(String value) {
		argumentName = value;
		return this;
	}

	/**
	 * The current (partial) value being completed.
	 *
	 * <p>
	 * Adapters must validate this as a non-<jk>null</jk> string before dispatching to a completer; this
	 * type does not itself enforce that constraint.
	 *
	 * @return The current value. Never <jk>null</jk> once an adapter has validated the request.
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Sets the current (partial) value being completed.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public McpCompletionRequest setValue(String value) {
		this.value = value;
		return this;
	}

	/**
	 * Additional already-resolved argument values supplied by the client for context.
	 *
	 * @return An immutable, insertion-ordered map. Never <jk>null</jk>; empty when the request omitted
	 * 	context.
	 */
	public Map<String,String> getContextArguments() {
		return contextArguments;
	}

	/**
	 * Sets the additional already-resolved argument values supplied by the client for context.
	 *
	 * <p>
	 * The supplied map is defensively copied into an immutable, insertion-ordered map. A <jk>null</jk>
	 * value normalizes to an immutable empty map, matching the contract for omitted context.
	 *
	 * @param value The new value. Can be <jk>null</jk>, which normalizes to an empty map.
	 * @return This object.
	 */
	public McpCompletionRequest setContextArguments(Map<String,String> value) {
		contextArguments = value == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(value));
		return this;
	}
}
