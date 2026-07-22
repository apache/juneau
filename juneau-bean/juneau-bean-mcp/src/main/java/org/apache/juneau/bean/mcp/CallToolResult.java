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

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.juneau.marshall.*;

/**
 * Result payload for {@value McpMethods#TOOLS_CALL}.
 */
@Marshalled
public class CallToolResult {

	private List<Content> content;
	private Boolean isError;

	/**
	 * Content blocks returned by the tool.
	 *
	 * @return The content list, or {@code null} if not set.
	 */
	public List<Content> getContent() {
		return u(content);
	}

	/**
	 * Sets the content blocks.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public CallToolResult setContent(List<Content> value) {
		content = value;
		return this;
	}

	/**
	 * Sets the content blocks.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public CallToolResult setContent(Content...value) {
		content = list(value);
		return this;
	}

	/**
	 * Appends to the content blocks.
	 *
	 * @param value The values to append.
	 * @return This object (for method chaining).
	 */
	public CallToolResult addContent(Content...value) {
		if (content == null)
			content = list();
		Collections.addAll(content, value);
		return this;
	}

	/**
	 * Appends to the content blocks.
	 *
	 * @param value The values to append.
	 * @return This object (for method chaining).
	 */
	public CallToolResult addContent(Collection<Content> value) {
		if (content == null)
			content = list();
		content.addAll(value);
		return this;
	}

	/**
	 * When {@code true}, the tool reported an application-level error.
	 *
	 * @return The flag, or {@code null} if not set.
	 */
	public Boolean getIsError() {
		return isError;
	}

	/**
	 * Sets the error flag.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public CallToolResult setIsError(Boolean value) {
		isError = value;
		return this;
	}
}
