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

/**
 * Revision-neutral identification of the declaration a {@code completion/complete} request targets.
 *
 * <p>
 * Supersedes the wire-level closed {@code CompletionReference} family ({@code PromptReference} /
 * {@code ResourceTemplateReference}). {@link #getTarget()} is always an <em>exact declaration</em>: a
 * prompt name or a registered {@code uriTemplate} string, never a concrete/expanded URI.
 *
 * <p>
 * Use {@link #prompt(String)} or {@link #resource(String)} to construct an instance; this keeps neutral
 * dispatch code free of free-form kind strings.
 */
public class McpCompletionRef {

	/**
	 * The kind of declaration a {@link McpCompletionRef} targets.
	 */
	public enum Kind {

		/** Targets a declared prompt argument. */
		PROMPT,

		/** Targets a declared resource-template variable. */
		RESOURCE
	}

	private Kind kind;
	private String target;

	/**
	 * Creates a reference to a prompt argument completion target.
	 *
	 * @param name The exact prompt name. Can be <jk>null</jk>.
	 * @return A new reference. Never <jk>null</jk>.
	 */
	public static McpCompletionRef prompt(String name) {
		return new McpCompletionRef().setKind(Kind.PROMPT).setTarget(name);
	}

	/**
	 * Creates a reference to a resource-template variable completion target.
	 *
	 * @param uriTemplate The exact registered URI template string. Can be <jk>null</jk>.
	 * @return A new reference. Never <jk>null</jk>.
	 */
	public static McpCompletionRef resource(String uriTemplate) {
		return new McpCompletionRef().setKind(Kind.RESOURCE).setTarget(uriTemplate);
	}

	/**
	 * The kind of declaration this reference targets.
	 *
	 * @return The kind, or <jk>null</jk> if not set.
	 */
	public Kind getKind() {
		return kind;
	}

	/**
	 * Sets the kind of declaration this reference targets.
	 *
	 * @param value The new value. Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public McpCompletionRef setKind(Kind value) {
		kind = value;
		return this;
	}

	/**
	 * The exact target declaration: a prompt name for {@link Kind#PROMPT}, or a registered URI template
	 * string for {@link Kind#RESOURCE}.
	 *
	 * @return The target, or <jk>null</jk> if not set.
	 */
	public String getTarget() {
		return target;
	}

	/**
	 * Sets the exact target declaration.
	 *
	 * @param value The new value. Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public McpCompletionRef setTarget(String value) {
		target = value;
		return this;
	}
}
