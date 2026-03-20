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
package org.apache.juneau.json5;

import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;

/**
 * Session object that lives for the duration of a single use of {@link Json5Serializer}.
 *
 * <p>
 * Extends {@link JsonSerializerSession} to support JSON5 serialization with unquoted attribute names
 * (when safe to do so) and single-quoted strings.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JsonBasics">JSON Basics</a>
 * </ul>
 */
public class Json5SerializerSession extends JsonSerializerSession {

	/**
	 * Builder class.
	 */
	public static class Builder extends JsonSerializerSession.Builder {

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 */
		protected Builder(Json5Serializer ctx) {
			super(ctx);
		}

		@Override
		public Json5SerializerSession build() {
			return new Json5SerializerSession(this);
		}
	}

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * @return A new builder.
	 */
	public static Builder create(Json5Serializer ctx) {
		return new Builder(ctx);
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected Json5SerializerSession(Builder builder) {
		super(builder);
	}

	@Override
	protected JsonWriter getJsonWriter(SerializerPipe out) {
		var output = out.getRawOutput();
		if (output instanceof JsonWriter output2)
			return output2;
		var w = JsonWriter.create(out.getWriter(), isUseWhitespace(), getMaxIndent(), isEscapeSolidus(), getQuoteChar(), true, isTrimStrings(), getUriResolver());
		out.setWriter(w);
		return w;
	}
}
