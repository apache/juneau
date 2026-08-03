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
 * MCP client capability advertisement carried by RequestMeta.
 */
@Marshalled
public class ClientCapabilities {

	private RootsCapability roots;
	private ElicitationCapability elicitation;
	private Map<String,Object> sampling;
	private Map<String,Object> experimental;
	private Map<String,Object> extensions;

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
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ClientCapabilities setRoots(RootsCapability value) {
		roots = value;
		return this;
	}

	/**
	 * Elicitation capability.
	 *
	 * @return The capability, or {@code null} if not set.
	 */
	public ElicitationCapability getElicitation() {
		return elicitation;
	}

	/**
	 * Sets elicitation capability.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ClientCapabilities setElicitation(ElicitationCapability value) {
		elicitation = value;
		return this;
	}

	/**
	 * Sampling capability (free-form map for forward compatibility).
	 *
	 * @return The sampling map, or {@code null} if not set.
	 */
	public Map<String,Object> getSampling() {
		return u(sampling);
	}

	/**
	 * Sets sampling capability.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ClientCapabilities setSampling(Map<String,Object> value) {
		sampling = value;
		return this;
	}

	/**
	 * Convenience method to add a single sampling capability entry.
	 *
	 * @param name The entry name.  Can be <jk>null</jk> ({@link LinkedHashMap} tolerates a <jk>null</jk> key).
	 * @param value The entry value.  Can be <jk>null</jk> (stored as <jk>null</jk>).
	 * @return This object (for method chaining).
	 */
	public ClientCapabilities putSampling(String name, Object value) {
		if (sampling == null)
			sampling = map();
		sampling.put(name, value);
		return this;
	}

	/**
	 * Experimental capability extensions.
	 *
	 * @return The experimental map, or {@code null} if not set.
	 */
	public Map<String,Object> getExperimental() {
		return u(experimental);
	}

	/**
	 * Sets experimental extensions.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ClientCapabilities setExperimental(Map<String,Object> value) {
		experimental = value;
		return this;
	}

	/**
	 * Convenience method to add a single experimental extension entry.
	 *
	 * @param name The extension name.  Can be <jk>null</jk> ({@link LinkedHashMap} tolerates a <jk>null</jk> key).
	 * @param value The extension value.  Can be <jk>null</jk> (stored as <jk>null</jk>).
	 * @return This object (for method chaining).
	 */
	public ClientCapabilities putExperimental(String name, Object value) {
		if (experimental == null)
			experimental = map();
		experimental.put(name, value);
		return this;
	}

	/**
	 * Schema-defined capability extensions (distinct from the free-form {@code experimental} bag).
	 *
	 * @return The extensions map, or {@code null} if not set.
	 */
	public Map<String,Object> getExtensions() {
		return u(extensions);
	}

	/**
	 * Sets capability extensions.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ClientCapabilities setExtensions(Map<String,Object> value) {
		extensions = value;
		return this;
	}

	/**
	 * Convenience method to add a single capability extension entry.
	 *
	 * <p>
	 * The extension map is lazily initialized on first use and preserves insertion order across repeated calls.
	 *
	 * @param name The extension name.  Can be <jk>null</jk> ({@link LinkedHashMap} tolerates a <jk>null</jk> key).
	 * @param value The extension value.  Can be <jk>null</jk> (stored as <jk>null</jk>).
	 * @return This object (for method chaining).
	 */
	public ClientCapabilities putExtensions(String name, Object value) {
		if (extensions == null)
			extensions = map();
		extensions.put(name, value);
		return this;
	}
}
