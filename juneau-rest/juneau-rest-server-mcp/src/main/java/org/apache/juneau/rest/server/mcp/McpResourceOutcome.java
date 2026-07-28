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
 * Revision-neutral result of a {@code resources/read} invocation.
 *
 * <p>
 * Supersedes the wire-level {@code ReadResourceResult} bean.
 */
public class McpResourceOutcome {

	private List<McpResourceContents> contents;

	/**
	 * The resource's payload contents.
	 *
	 * @return The contents, or <jk>null</jk> if not set. A <jk>null</jk> value is distinct from an
	 * 	empty list: it means the wire property is omitted entirely.
	 */
	public List<McpResourceContents> getContents() {
		return contents;
	}

	/**
	 * Sets the resource's payload contents.
	 *
	 * @param value The new value. Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public McpResourceOutcome setContents(List<McpResourceContents> value) {
		contents = value;
		return this;
	}
}
