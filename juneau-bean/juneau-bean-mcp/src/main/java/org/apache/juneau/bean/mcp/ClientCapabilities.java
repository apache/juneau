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

import java.util.*;

import org.apache.juneau.annotation.*;

/**
 * MCP client capability advertisement in {@link InitializeRequest}.
 */
@Bean
public class ClientCapabilities {

	private RootsCapability roots;
	private Map<String, Object> sampling;
	private Map<String, Object> experimental;

	/**
	 * Roots capability.
	 *
	 * @return The capability, or {@code null} if not set.
	 */
	public RootsCapability getRoots() {
		return roots;
	}

	/**
	 * Sets roots capability.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public ClientCapabilities setRoots(RootsCapability value) {
		roots = value;
		return this;
	}

	/**
	 * Sampling capability (free-form map for forward compatibility).
	 *
	 * @return The sampling map, or {@code null} if not set.
	 */
	public Map<String, Object> getSampling() {
		return sampling;
	}

	/**
	 * Sets sampling capability.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public ClientCapabilities setSampling(Map<String, Object> value) {
		sampling = value;
		return this;
	}

	/**
	 * Experimental capability extensions.
	 *
	 * @return The experimental map, or {@code null} if not set.
	 */
	public Map<String, Object> getExperimental() {
		return experimental;
	}

	/**
	 * Sets experimental extensions.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public ClientCapabilities setExperimental(Map<String, Object> value) {
		experimental = value;
		return this;
	}
}
