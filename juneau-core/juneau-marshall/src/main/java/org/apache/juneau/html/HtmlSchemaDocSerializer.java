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

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Instantiates a new clean-slate {@link HtmlSchemaDocSerializer} object.
	 *
	 * <p>
	 * This is equivalent to simply calling <code><jk>new</jk> HtmlSchemaDocSerializerBuilder()</code>.
	 *
	 * <p>
	 * Note that this method creates a builder initialized to all default settings, whereas {@link #copy()} copies
	 * the settings of the object called on.
	 *
	 * @return A new {@link HtmlSchemaDocSerializerBuilder} object.
	 */
	public static HtmlSchemaDocSerializerBuilder create() {
		return new HtmlSchemaDocSerializerBuilder();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	final JsonSchemaGenerator generator;

	/**
	 * Constructor.
	 *
	 * @param builder
	 * 	The builder for this object.
	 */
	protected HtmlSchemaDocSerializer(HtmlDocSerializerBuilder builder) {
		super(builder.detectRecursions().ignoreRecursions());

		generator = JsonSchemaGenerator.create().beanContext(getBeanContext()).build();
	}

	@Override /* Serializer */
	public HtmlSchemaDocSerializerSession createSession() {
		return createSession(defaultArgs());
	}

	@Override /* Serializer */
	public HtmlSchemaDocSerializerSession createSession(SerializerSessionArgs args) {
		return new HtmlSchemaDocSerializerSession(this, args);
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
				"HtmlSchemaDocSerializer",
				OMap
					.create()
					.filtered()
			);
	}
}
