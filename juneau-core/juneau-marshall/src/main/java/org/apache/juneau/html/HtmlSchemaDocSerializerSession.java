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
package org.apache.juneau.html;

import org.apache.juneau.commons.http.MediaType;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.commons.svl.*;

/**
 * Context object that lives for the duration of a single serialization of {@link HtmlSchemaDocSerializer} and its subclasses.
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
	"java:S110" // Inheritance depth acceptable for HtmlSchemaDocSerializerSession hierarchy
})
public class HtmlSchemaDocSerializerSession extends HtmlDocSerializerSession {

	/**
	 * Builder class.
	 */
	public static class Builder extends HtmlDocSerializerSession.Builder<Builder> {

		private HtmlSchemaDocSerializer ctx;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 */
		protected Builder(HtmlSchemaDocSerializer ctx) {
			super(ctx);
			this.ctx = ctx;
		}

		@Override
		public HtmlSchemaDocSerializerSession build() {
			return new HtmlSchemaDocSerializerSession(this);
		}

	}

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * @return A new builder.
	 */
	public static Builder create(HtmlSchemaDocSerializer ctx) {
		return new Builder(ctx);
	}

	private final JsonSchemaGeneratorSession genSession;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected HtmlSchemaDocSerializerSession(Builder builder) {
		super(builder);
		genSession = builder.ctx.getGenerator().getSession();
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