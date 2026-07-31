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

import org.apache.juneau.marshall.*;

/**
 * Parameters for {@value McpMethods#COMPLETION_COMPLETE}.
 */
@Marshalled
public class CompleteRequest {

	private CompletionReference ref;
	private CompletionArgument argument;
	private CompletionContext context;

	/**
	 * Completion target (prompt or resource template).
	 *
	 * @return The reference, or {@code null} if not set.
	 */
	public CompletionReference getRef() {
		return ref;
	}

	/**
	 * Sets the completion target.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public CompleteRequest setRef(CompletionReference value) {
		ref = value;
		return this;
	}

	/**
	 * Argument being completed.
	 *
	 * @return The argument, or {@code null} if not set.
	 */
	public CompletionArgument getArgument() {
		return argument;
	}

	/**
	 * Sets the argument being completed.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public CompleteRequest setArgument(CompletionArgument value) {
		argument = value;
		return this;
	}

	/**
	 * Optional already-resolved argument values.
	 *
	 * @return The context, or {@code null} if not set.
	 */
	public CompletionContext getContext() {
		return context;
	}

	/**
	 * Sets the optional context.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public CompleteRequest setContext(CompletionContext value) {
		context = value;
		return this;
	}
}
