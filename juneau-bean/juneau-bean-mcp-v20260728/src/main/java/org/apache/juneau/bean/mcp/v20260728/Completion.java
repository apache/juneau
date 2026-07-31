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
 * Completion values for a {@value McpMethods#COMPLETION_COMPLETE} request.
 *
 * <p>
 * This is a lossless wire carrier. It does not itself enforce the protocol's 100-value cap or any other
 * server policy; dispatch is responsible for capping, ordering, and validating values before construction.
 */
@Marshalled
public class Completion {

	private List<String> values;
	private Integer total;
	private Boolean hasMore;

	/**
	 * Candidate completion values.
	 *
	 * @return The values list, or {@code null} if not set.
	 */
	public List<String> getValues() {
		return u(values);
	}

	/**
	 * Sets the candidate completion values.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Completion setValues(List<String> value) {
		values = value;
		return this;
	}

	/**
	 * Sets the candidate completion values.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Completion setValues(String...value) {
		values = list(value);
		return this;
	}

	/**
	 * Appends to the candidate completion values.
	 *
	 * @param value The values to append.
	 * @return This object (for method chaining).
	 */
	public Completion addValues(String...value) {
		if (values == null)
			values = list();
		Collections.addAll(values, value);
		return this;
	}

	/**
	 * Appends to the candidate completion values.
	 *
	 * @param value The values to append.
	 * @return This object (for method chaining).
	 */
	public Completion addValues(Collection<String> value) {
		if (values == null)
			values = list();
		values.addAll(value);
		return this;
	}

	/**
	 * Optional total number of matches, which may exceed {@link #getValues()} when truncated.
	 *
	 * @return The total, or {@code null} if not set.
	 */
	public Integer getTotal() {
		return total;
	}

	/**
	 * Sets the total number of matches.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Completion setTotal(Integer value) {
		total = value;
		return this;
	}

	/**
	 * Optional flag indicating additional values exist beyond {@link #getValues()}.
	 *
	 * @return The flag, or {@code null} if not set.
	 */
	public Boolean getHasMore() {
		return hasMore;
	}

	/**
	 * Sets the has-more flag.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Completion setHasMore(Boolean value) {
		hasMore = value;
		return this;
	}
}
