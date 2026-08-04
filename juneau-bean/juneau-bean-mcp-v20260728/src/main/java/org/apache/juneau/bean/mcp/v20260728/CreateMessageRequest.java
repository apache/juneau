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
 * Parameters for {@value McpMethods#SAMPLING_CREATE_MESSAGE}, a server-to-client duplex request (MCP sampling).
 * Never dispatched through {@code McpRevision} — this is a plain POJO decoded/encoded by
 * hand (or via the client's typed duplex flow) inside an {@code McpServerRequestHandler} implementation.
 */
@Marshalled
public class CreateMessageRequest {

	private List<SamplingMessage> messages;
	private ModelPreferences modelPreferences;
	private String systemPrompt;
	private String includeContext;
	private Double temperature;
	private Integer maxTokens;
	private List<String> stopSequences;
	private Map<String,Object> metadata;

	/**
	 * The conversation history to sample from.
	 *
	 * @return The messages list, or {@code null} if not set.
	 */
	public List<SamplingMessage> getMessages() {
		return u(messages);
	}

	/**
	 * Sets the messages.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public CreateMessageRequest setMessages(List<SamplingMessage> value) {
		messages = value;
		return this;
	}

	/**
	 * Sets the messages.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public CreateMessageRequest setMessages(SamplingMessage...value) {
		messages = list(value);
		return this;
	}

	/**
	 * Appends to the messages.
	 *
	 * @param value The values to append.
	 * @return This object (for method chaining).
	 */
	public CreateMessageRequest addMessages(SamplingMessage...value) {
		if (messages == null)
			messages = list();
		Collections.addAll(messages, value);
		return this;
	}

	/**
	 * Appends to the messages.
	 *
	 * @param value The values to append.
	 * @return This object (for method chaining).
	 */
	public CreateMessageRequest addMessages(Collection<SamplingMessage> value) {
		if (messages == null)
			messages = list();
		messages.addAll(value);
		return this;
	}

	/**
	 * Model-selection preferences.
	 *
	 * @return The preferences, or {@code null} if not set.
	 */
	public ModelPreferences getModelPreferences() {
		return modelPreferences;
	}

	/**
	 * Sets the model preferences.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public CreateMessageRequest setModelPreferences(ModelPreferences value) {
		modelPreferences = value;
		return this;
	}

	/**
	 * An optional system prompt.
	 *
	 * @return The system prompt, or {@code null} if not set.
	 */
	public String getSystemPrompt() {
		return systemPrompt;
	}

	/**
	 * Sets the system prompt.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public CreateMessageRequest setSystemPrompt(String value) {
		systemPrompt = value;
		return this;
	}

	/**
	 * Which MCP context to include ({@code none}/{@code thisServer}/{@code allServers} per the pinned schema).
	 * Kept as a plain {@code String}, matching this module's existing precedent of not over-modeling
	 * schema string-enums that do not gate real branching logic in this codebase.
	 *
	 * @return The include-context value, or {@code null} if not set.
	 */
	public String getIncludeContext() {
		return includeContext;
	}

	/**
	 * Sets the include-context value.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public CreateMessageRequest setIncludeContext(String value) {
		includeContext = value;
		return this;
	}

	/**
	 * Sampling temperature.
	 *
	 * @return The temperature, or {@code null} if not set.
	 */
	public Double getTemperature() {
		return temperature;
	}

	/**
	 * Sets the temperature.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public CreateMessageRequest setTemperature(Double value) {
		temperature = value;
		return this;
	}

	/**
	 * Maximum tokens to sample.
	 *
	 * @return The max tokens, or {@code null} if not set.
	 */
	public Integer getMaxTokens() {
		return maxTokens;
	}

	/**
	 * Sets the max tokens.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public CreateMessageRequest setMaxTokens(Integer value) {
		maxTokens = value;
		return this;
	}

	/**
	 * Sequences that stop sampling when generated.
	 *
	 * @return The stop sequences, or {@code null} if not set.
	 */
	public List<String> getStopSequences() {
		return u(stopSequences);
	}

	/**
	 * Sets the stop sequences.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public CreateMessageRequest setStopSequences(List<String> value) {
		stopSequences = value;
		return this;
	}

	/**
	 * Sets the stop sequences.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public CreateMessageRequest setStopSequences(String...value) {
		stopSequences = list(value);
		return this;
	}

	/**
	 * Appends to the stop sequences.
	 *
	 * @param value The values to append.
	 * @return This object (for method chaining).
	 */
	public CreateMessageRequest addStopSequences(String...value) {
		if (stopSequences == null)
			stopSequences = list();
		Collections.addAll(stopSequences, value);
		return this;
	}

	/**
	 * Appends to the stop sequences.
	 *
	 * @param value The values to append.
	 * @return This object (for method chaining).
	 */
	public CreateMessageRequest addStopSequences(Collection<String> value) {
		if (stopSequences == null)
			stopSequences = list();
		stopSequences.addAll(value);
		return this;
	}

	/**
	 * Free-form server-defined metadata.
	 *
	 * @return The metadata map, or {@code null} if not set.
	 */
	public Map<String,Object> getMetadata() {
		return u(metadata);
	}

	/**
	 * Sets the metadata.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public CreateMessageRequest setMetadata(Map<String,Object> value) {
		metadata = value;
		return this;
	}

	/**
	 * Convenience method to add a single metadata entry.
	 *
	 * @param name The entry name.  Can be <jk>null</jk> ({@link LinkedHashMap} tolerates a <jk>null</jk> key).
	 * @param value The entry value.  Can be <jk>null</jk> (stored as <jk>null</jk>).
	 * @return This object (for method chaining).
	 */
	public CreateMessageRequest putMetadata(String name, Object value) {
		if (metadata == null)
			metadata = map();
		metadata.put(name, value);
		return this;
	}
}
