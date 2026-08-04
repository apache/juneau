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
 * The end user's answer to an {@link ElicitRequest} (MCP {@code 2026-07-28} SEP-2322), echoed back keyed by the
 * same server-assigned id in a follow-up request's {@code inputResponses} and decoded via
 * {@code org.apache.juneau.rest.server.mcp.v20260728.ElicitationResponses}.
 *
 * <p>
 * {@link #getContent()} is present only when {@link #getAction()} is {@link ElicitAction#ACCEPT}; its shape is
 * whatever {@link ElicitRequest#getRequestedSchema()} described, which is per-elicitation and unknowable at the
 * bean-definition level, so it is deliberately left an untyped, dynamic map.
 */
@Marshalled
public class ElicitResult {

	private ElicitAction action;
	private Map<String,Object> content;

	/**
	 * The end user's choice.
	 *
	 * @return The action, or {@code null} if not set.
	 */
	public ElicitAction getAction() {
		return action;
	}

	/**
	 * Sets the end user's choice.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ElicitResult setAction(ElicitAction value) {
		action = value;
		return this;
	}

	/**
	 * The schema-shaped answer content.  Present only when {@link #getAction()} is {@link ElicitAction#ACCEPT}.
	 *
	 * @return The content map, or {@code null} if not set.
	 */
	public Map<String,Object> getContent() {
		return u(content);
	}

	/**
	 * Sets the schema-shaped answer content.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ElicitResult setContent(Map<String,Object> value) {
		content = value;
		return this;
	}

	/**
	 * Convenience method to add a single content entry.
	 *
	 * @param name The entry name.  Can be <jk>null</jk> ({@link LinkedHashMap} tolerates a <jk>null</jk> key).
	 * @param value The entry value.  Can be <jk>null</jk> (stored as <jk>null</jk>).
	 * @return This object (for method chaining).
	 */
	public ElicitResult putContent(String name, Object value) {
		if (content == null)
			content = map();
		content.put(name, value);
		return this;
	}
}
