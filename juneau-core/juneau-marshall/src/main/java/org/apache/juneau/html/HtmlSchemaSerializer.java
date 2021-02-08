// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.html;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.serializer.*;

/**
 * Serializes POJO metamodels to HTML.
 *
 * <h5 class='topic'>Media types</h5>
 *
 * Handles <c>Accept</c> types:  <bc>text/html+schema</bc>
 * <p>
 * Produces <c>Content-Type</c> types:  <bc>text/html</bc>
 *
 * <h5 class='topic'>Description</h5>
 *
 * Essentially the same as {@link HtmlSerializer}, except serializes the POJO metamodel instead of the model itself.
 *
 * <p>
 * Produces output that describes the POJO metamodel similar to an XML schema document.
 *
 * <p>
 * The easiest way to create instances of this class is through the {@link HtmlSerializer#getSchemaSerializer()},
 * which will create a schema serializer with the same settings as the originating serializer.
 */
@ConfigurableContext
public class HtmlSchemaSerializer extends HtmlSerializer {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	static final String PREFIX = "HtmlSchemaSerializer";

	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Default serializer, all default settings.*/
	public static final HtmlSchemaSerializer DEFAULT = new HtmlSchemaSerializer(ContextProperties.DEFAULT);

	/** Default serializer, all default settings.*/
	public static final HtmlSchemaSerializer DEFAULT_READABLE = new Readable(ContextProperties.DEFAULT);

	/** Default serializer, single quotes, simple mode. */
	public static final HtmlSchemaSerializer DEFAULT_SIMPLE = new Simple(ContextProperties.DEFAULT);

	/** Default serializer, single quotes, simple mode, with whitespace. */
	public static final HtmlSchemaSerializer DEFAULT_SIMPLE_READABLE = new SimpleReadable(ContextProperties.DEFAULT);

	//-------------------------------------------------------------------------------------------------------------------
	// Predefined subclasses
	//-------------------------------------------------------------------------------------------------------------------

	/** Default serializer, with whitespace. */
	public static class Readable extends HtmlSchemaSerializer {

		/**
		 * Constructor.
		 *
		 * @param cp The property store containing all the settings for this object.
		 */
		public Readable(ContextProperties cp) {
			super(
				cp.builder().setDefault(WSERIALIZER_useWhitespace, true).build()
			);
		}
	}

	/** Default serializer, single quotes, simple mode. */
	public static class Simple extends HtmlSchemaSerializer {

		/**
		 * Constructor.
		 *
		 * @param cp The property store containing all the settings for this object.
		 */
		public Simple(ContextProperties cp) {
			super(
				cp.builder()
					.setDefault(WSERIALIZER_quoteChar, '\'')
					.build()
				);
		}
	}

	/** Default serializer, single quotes, simple mode, with whitespace. */
	public static class SimpleReadable extends HtmlSchemaSerializer {

		/**
		 * Constructor.
		 *
		 * @param cp The property store containing all the settings for this object.
		 */
		public SimpleReadable(ContextProperties cp) {
			super(
				cp.builder()
					.setDefault(WSERIALIZER_quoteChar, '\'')
					.setDefault(WSERIALIZER_useWhitespace, true)
					.build()
			);
		}
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final JsonSchemaGenerator generator;

	/**
	 * Constructor.
	 *
	 * @param cp
	 * 	The property store to use for creating the context for this serializer.
	 */
	public HtmlSchemaSerializer(ContextProperties cp) {
		super(
			cp.builder()
				.setDefault(BEANTRAVERSE_detectRecursions, true)
				.setDefault(BEANTRAVERSE_ignoreRecursions, true)
				.build(),
			"text/html", "text/html+schema"
		);

		generator = JsonSchemaGenerator.create().apply(getContextProperties()).build();
	}

	@Override /* Context */
	public HtmlSchemaSerializerBuilder builder() {
		return new HtmlSchemaSerializerBuilder(getContextProperties());
	}

	@Override /* Context */
	public HtmlSchemaSerializerSession createSession() {
		return createSession(createDefaultSessionArgs());
	}

	@Override /* Serializer */
	public HtmlSchemaSerializerSession createSession(SerializerSessionArgs args) {
		return new HtmlSchemaSerializerSession(this, args);
	}

	JsonSchemaGenerator getGenerator() {
		return generator;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public OMap toMap() {
		return super.toMap()
			.a(
				"HtmlSchemaSerializer",
				OMap
					.create()
					.filtered()
					.a("generator", generator)
			);
	}
}
