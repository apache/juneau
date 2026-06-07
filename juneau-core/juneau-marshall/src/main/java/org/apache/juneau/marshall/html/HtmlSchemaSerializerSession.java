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
package org.apache.juneau.marshall.html;

import java.io.*;

import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.jsonschema.*;
import org.apache.juneau.marshall.serializer.*;

/**
 * Context object that lives for the duration of a single serialization of {@link HtmlSchemaSerializer} and its subclasses.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/HtmlBasics">HTML Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S110" // Inheritance depth acceptable for HtmlSchemaSerializerSession hierarchy
})
public class HtmlSchemaSerializerSession extends HtmlSerializerSession {

	/**
	 * Builder class.
	 */
	public static class Builder extends HtmlSerializerSession.Builder<Builder> {

		private HtmlSchemaSerializer ctx;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 */
		protected Builder(HtmlSchemaSerializer ctx) {
			super(ctx);
			this.ctx = ctx;
		}

		@Override
		public HtmlSchemaSerializerSession build() {
			return new HtmlSchemaSerializerSession(this);
		}

	}

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * @return A new builder.
	 */
	public static Builder create(HtmlSchemaSerializer ctx) {
		return new Builder(ctx);
	}

	private final JsonSchemaGeneratorSession genSession;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected HtmlSchemaSerializerSession(Builder builder) {
		super(builder);
		this.genSession = builder.ctx.getGenerator().getSession();
	}

	@Override /* Overridden from SerializerSession */
	protected void doSerialize(SerializerPipe out, Object o) throws IOException, SerializeException {
		try {
			super.doSerialize(out, genSession.getSchema(o));
		} catch (MarshallingRecursionException e) {
			throw new SerializeException(e);
		}
	}
}