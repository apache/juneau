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
 * {@link CompletionReference} targeting a registered resource template by its exact URI-template string.
 *
 * <p>
 * The {@link #getUri() uri} is the registered template string itself (for example {@code file:///{name}}),
 * not a concrete expanded URI.
 */
@Marshalled(typeName = "ref/resource")
public class ResourceTemplateReference implements CompletionReference {

	private String uri;

	/**
	 * Registered resource-template URI string.
	 *
	 * @return The URI template, or {@code null} if not set.
	 */
	public String getUri() {
		return uri;
	}

	/**
	 * Sets the resource-template URI string.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ResourceTemplateReference setUri(String value) {
		uri = value;
		return this;
	}
}
