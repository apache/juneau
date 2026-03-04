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
package org.apache.juneau.jsonl;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;

/**
 * Session object that lives for the duration of a single use of {@link JsonlSerializer}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JsonlBasics">JSONL Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"resource", // Resource management handled externally
	"java:S110", // Inheritance depth acceptable
	"java:S115" // Constants use UPPER_snakeCase convention
})
public class JsonlSerializerSession extends JsonSerializerSession {

	private static final String ARG_ctx = "ctx";

	/**
	 * Builder class.
	 */
	@SuppressWarnings({
		"unused" // ctx passed to super(); parent uses it; Builder stores for constructor chain
	})
	public static class Builder extends JsonSerializerSession.Builder {

		private JsonlSerializer ctx;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(JsonlSerializer ctx) {
			super(assertArgNotNull(ARG_ctx, ctx));
			this.ctx = ctx;
		}

		@Override
		public JsonlSerializerSession build() {
			return new JsonlSerializerSession(this);
		}
	}

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * 	<br>Cannot be <jk>null</jk>.
	 * @return A new builder.
	 */
	public static Builder create(JsonlSerializer ctx) {
		return new Builder(assertArgNotNull(ARG_ctx, ctx));
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected JsonlSerializerSession(Builder builder) {
		super(builder);
	}

	@Override /* Overridden from JsonSerializerSession */
	protected void doSerialize(SerializerPipe out, Object o) throws IOException, SerializeException {
		var w = getJsonWriter(out);
		var cm = getClassMetaForObject(o);
		var depth = getInitialDepth();

		if (cm != null && (cm.isCollection() || cm.isArray())) {
			var c = cm.isArray() ? toList(cm.inner(), o) : (Collection<?>) o;
			var elementType = cm.getElementType();
			for (Object item : c) {
				serializeAnything(w.i(depth), item, elementType, "root", null);
				w.w('\n');
			}
		} else if (cm != null && cm.isStreamable()) {
			var elementType = cm.getElementType();
			forEachStreamableEntry(o, cm, item -> {
				serializeAnything(w.i(depth), item, elementType, "root", null);
				w.w('\n');
			});
		} else {
			serializeAnything(w.i(depth), o, getExpectedRootType(o), "root", null);
			w.w('\n');
		}
	}
}
