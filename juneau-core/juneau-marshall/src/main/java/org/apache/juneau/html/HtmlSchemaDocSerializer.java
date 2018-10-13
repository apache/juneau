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
import org.apache.juneau.internal.*;
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
 * Essentially the same as {@link HtmlDocSerializer}, except serializes the POJO metamodel instead of the model itself.
 *
 * <p>
 * Produces output that describes the POJO metamodel similar to an XML schema document.
 *
 * <p>
 * The easiest way to create instances of this class is through the {@link HtmlSerializer#getSchemaSerializer()},
 * which will create a schema serializer with the same settings as the originating serializer.
 */
public final class HtmlSchemaDocSerializer extends HtmlDocSerializer {

	private final JsonSchemaGenerator generator;

	/**
	 * Constructor.
	 *
	 * @param ps
	 * 	The property store to use for creating the context for this serializer.
	 */
	public HtmlSchemaDocSerializer(PropertyStore ps) {
		this(ps, "text/html", "text/html+schema");
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
	 * 	{@doc RFC2616.section14.1}
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
	public HtmlSchemaDocSerializer(PropertyStore ps, String produces, String accept) {
		super(
			ps.builder()
				.set(BEANTRAVERSE_detectRecursions, true)
				.set(BEANTRAVERSE_ignoreRecursions, true)
				.build(),
			produces,
			accept
		);

		generator = JsonSchemaGenerator.create().apply(getPropertyStore()).build();
	}

	@Override /* Serializer */
	public HtmlSchemaDocSerializerSession createSession(SerializerSessionArgs args) {
		return new HtmlSchemaDocSerializerSession(this, args);
	}

	JsonSchemaGenerator getGenerator() {
		return generator;
	}

	/**
	 * @deprecated Use {@link #HtmlSchemaDocSerializer(PropertyStore, String, String...)}
	 */
	@SuppressWarnings("javadoc")
	@Deprecated
	public HtmlSchemaDocSerializer(PropertyStore ps, String produces, String...accept) {
		this(ps, produces, StringUtils.join(accept, ','));
	}
}
