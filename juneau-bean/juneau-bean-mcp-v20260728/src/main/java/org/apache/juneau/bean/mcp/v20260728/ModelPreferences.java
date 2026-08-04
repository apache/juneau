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
 * Model-selection preferences for {@link CreateMessageRequest} (MCP sampling).
 */
@Marshalled
public class ModelPreferences {

	private List<ModelHint> hints;
	private Double costPriority;
	private Double speedPriority;
	private Double intelligencePriority;

	/**
	 * Ordered model-family/name hints, most-preferred first.
	 *
	 * @return The hints list, or {@code null} if not set.
	 */
	public List<ModelHint> getHints() {
		return u(hints);
	}

	/**
	 * Sets the hints.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ModelPreferences setHints(List<ModelHint> value) {
		hints = value;
		return this;
	}

	/**
	 * Sets the hints.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ModelPreferences setHints(ModelHint...value) {
		hints = list(value);
		return this;
	}

	/**
	 * Appends to the hints.
	 *
	 * @param value The values to append.
	 * @return This object (for method chaining).
	 */
	public ModelPreferences addHints(ModelHint...value) {
		if (hints == null)
			hints = list();
		Collections.addAll(hints, value);
		return this;
	}

	/**
	 * Appends to the hints.
	 *
	 * @param value The values to append.
	 * @return This object (for method chaining).
	 */
	public ModelPreferences addHints(Collection<ModelHint> value) {
		if (hints == null)
			hints = list();
		hints.addAll(value);
		return this;
	}

	/**
	 * Relative priority (0.0-1.0) placed on minimizing cost.
	 *
	 * @return The priority, or {@code null} if not set.
	 */
	public Double getCostPriority() {
		return costPriority;
	}

	/**
	 * Sets the cost priority.
	 *
	 * @param value The new value, {@code 0.0}-{@code 1.0}.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ModelPreferences setCostPriority(Double value) {
		costPriority = value;
		return this;
	}

	/**
	 * Relative priority (0.0-1.0) placed on minimizing latency.
	 *
	 * @return The priority, or {@code null} if not set.
	 */
	public Double getSpeedPriority() {
		return speedPriority;
	}

	/**
	 * Sets the speed priority.
	 *
	 * @param value The new value, {@code 0.0}-{@code 1.0}.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ModelPreferences setSpeedPriority(Double value) {
		speedPriority = value;
		return this;
	}

	/**
	 * Relative priority (0.0-1.0) placed on maximizing capability.
	 *
	 * @return The priority, or {@code null} if not set.
	 */
	public Double getIntelligencePriority() {
		return intelligencePriority;
	}

	/**
	 * Sets the intelligence priority.
	 *
	 * @param value The new value, {@code 0.0}-{@code 1.0}.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ModelPreferences setIntelligencePriority(Double value) {
		intelligencePriority = value;
		return this;
	}
}
