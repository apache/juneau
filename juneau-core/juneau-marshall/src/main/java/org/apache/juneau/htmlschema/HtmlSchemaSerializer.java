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
package org.apache.juneau.htmlschema;

import org.apache.juneau.*;
import org.apache.juneau.html.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.serializer.*;

/**
 * Serializes POJO metamodels to HTML.
 *
 * <h5 class='topic'>Media types</h5>
 *
 * Handles <code>Accept</code> types:  <code><b>text/html+schema</b></code>
 * <p>
 * Produces <code>Content-Type</code> types:  <code><b>text/html</b></code>
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
public class HtmlSchemaSerializer extends HtmlSerializer {

	/** Default serializer, all default settings.*/
	public static final HtmlSchemaSerializer DEFAULT = new HtmlSchemaSerializer(PropertyStore.DEFAULT);

	/** Default serializer, all default settings.*/
	public static final HtmlSchemaSerializer DEFAULT_READABLE = new Readable(PropertyStore.DEFAULT);

	/** Default serializer, single quotes, simple mode. */
	public static final HtmlSchemaSerializer DEFAULT_SIMPLE = new Simple(PropertyStore.DEFAULT);

	/** Default serializer, single quotes, simple mode, with whitespace. */
	public static final HtmlSchemaSerializer DEFAULT_SIMPLE_READABLE = new SimpleReadable(PropertyStore.DEFAULT);

	private final JsonSchemaSerializer jsctx;

	//-------------------------------------------------------------------------------------------------------------------
	// Predefined subclasses
	//-------------------------------------------------------------------------------------------------------------------

	/** Default serializer, with whitespace. */
	public static class Readable extends HtmlSchemaSerializer {

		/**
		 * Constructor.
		 *
		 * @param ps The property store containing all the settings for this object.
		 */
		public Readable(PropertyStore ps) {
			super(
				ps.builder().set(SERIALIZER_useWhitespace, true).build()
			);
		}
	}

	/** Default serializer, single quotes, simple mode. */
	public static class Simple extends HtmlSchemaSerializer {

		/**
		 * Constructor.
		 *
		 * @param ps The property store containing all the settings for this object.
		 */
		public Simple(PropertyStore ps) {
			super(
				ps.builder()
					.set(WSERIALIZER_quoteChar, '\'')
					.build()
				);
		}
	}

	/** Default serializer, single quotes, simple mode, with whitespace. */
	public static class SimpleReadable extends HtmlSchemaSerializer {

		/**
		 * Constructor.
		 *
		 * @param ps The property store containing all the settings for this object.
		 */
		public SimpleReadable(PropertyStore ps) {
			super(
				ps.builder()
					.set(WSERIALIZER_quoteChar, '\'')
					.set(SERIALIZER_useWhitespace, true)
					.build()
			);
		}
	}

	/**
	 * Constructor.
	 *
	 * @param ps
	 * 	The property store to use for creating the context for this serializer.
	 */
	public HtmlSchemaSerializer(PropertyStore ps) {
		this(ps, "text/html", "text/html+schema");
	}

	@Override /* Context */
	public HtmlSchemaSerializerBuilder builder() {
		return new HtmlSchemaSerializerBuilder(getPropertyStore());
	}

	/**
	 * Constructor.
	 *
	 * @param ps
	 * 	The property store containing all the settings for this object.
	 * @param produces
	 * 	The media type that this serializer produces.
	 * @param accept
	 * 	The accept media types that the serializer can handle.
	 * 	<p>
	 * 	Can contain meta-characters per the <code>media-type</code> specification of
	 * 	<a class="doclink" href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.1">RFC2616/14.1</a>
	 * 	<p>
	 * 	If empty, then assumes the only media type supported is <code>produces</code>.
	 * 	<p>
	 * 	For example, if this serializer produces <js>"application/json"</js> but should handle media types of
	 * 	<js>"application/json"</js> and <js>"text/json"</js>, then the arguments should be:
	 * 	<p class='bcode w800'>
	 * 	<jk>super</jk>(ps, <js>"application/json"</js>, <js>"application/json,text/json"</js>);
	 * 	</p>
	 * 	<br>...or...
	 * 	<p class='bcode w800'>
	 * 	<jk>super</jk>(ps, <js>"application/json"</js>, <js>"*&#8203;/json"</js>);
	 * 	</p>
	 * <p>
	 * The accept value can also contain q-values.
	 */
	public HtmlSchemaSerializer(PropertyStore ps, String produces, String accept) {
		super(
			ps.builder()
				.set(SERIALIZER_detectRecursions, true)
				.set(SERIALIZER_ignoreRecursions, true)
				.build(),
			produces,
			accept
		);
		this.jsctx = new JsonSchemaSerializer(ps);
	}

	@Override /* Serializer */
	public HtmlSchemaSerializerSession createSession(SerializerSessionArgs args) {
		return new HtmlSchemaSerializerSession(jsctx, this, args);
	}

	@Override /* Context */
	public HtmlSchemaSerializerSession createSession() {
		return createSession(createDefaultSessionArgs());
	}
}
