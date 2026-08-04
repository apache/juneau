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
 * MCP {@code audio} content block (base64 data).
 *
 * <p>
 * Joins the general-purpose {@link Content} dictionary alongside {@link TextContent}/{@link ImageContent}/
 * {@link EmbeddedResourceContent} — usable anywhere a {@link Content} is accepted, not only in sampling contexts.
 * Added specifically to give {@code SamplingMessage.content}/{@code CreateMessageResult.content} genuine
 * {@code text}/{@code image}/{@code audio}/{@code resource} fidelity to the real MCP sampling schema; usable
 * anywhere a {@link Content} is accepted, not only sampling contexts.
 */
@Marshalled(typeName = "audio")
public class AudioContent implements Content {

	private String data;
	private String mimeType;

	/**
	 * Base64-encoded audio bytes.
	 *
	 * @return The data, or {@code null} if not set.
	 */
	public String getData() {
		return data;
	}

	/**
	 * Sets the base64 payload.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public AudioContent setData(String value) {
		data = value;
		return this;
	}

	/**
	 * Audio MIME type (for example {@code audio/wav}, {@code audio/mpeg}).
	 *
	 * @return The MIME type, or {@code null} if not set.
	 */
	public String getMimeType() {
		return mimeType;
	}

	/**
	 * Sets the MIME type.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public AudioContent setMimeType(String value) {
		mimeType = value;
		return this;
	}
}
