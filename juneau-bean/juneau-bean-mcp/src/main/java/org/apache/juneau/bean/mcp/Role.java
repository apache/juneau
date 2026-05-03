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

/**
 * Role for {@link PromptMessage} entries (MCP wire uses lowercase strings).
 */
public enum Role {

	/** User role. */
	USER("user"),

	/** Assistant role. */
	ASSISTANT("assistant"),

	/** System role. */
	SYSTEM("system"),

	/** Tool role. */
	TOOL("tool");

	private final String wire;

	Role(String wire) {
		this.wire = wire;
	}

	/**
	 * Wire token for JSON payloads.
	 *
	 * @return Lowercase MCP role string.
	 */
	public String toWire() {
		return wire;
	}

	@Override /* Object */
	public String toString() {
		return wire;
	}
}
