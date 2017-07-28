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

import static org.apache.juneau.serializer.SerializerContext.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.serializer.*;

/**
 * Serializes POJO metamodels to HTML.
 *
 * <h5 class='section'>Media types:</h5>
 *
 * Handles <code>Accept</code> types: <code>text/html+schema</code>
 *
 * <p>
 * Produces <code>Content-Type</code> types: <code>text/html</code>
 *
 * <h5 class='section'>Description:</h5>
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
@Produces(value="text/html+schema", contentType="text/html")
public final class HtmlSchemaDocSerializer extends HtmlDocSerializer {

	@SuppressWarnings("hiding")
	final HtmlDocSerializerContext ctx;

	/**
	 * Constructor.
	 *
	 * @param propertyStore The property store to use for creating the context for this serializer.
	 */
	public HtmlSchemaDocSerializer(PropertyStore propertyStore) {
		super(propertyStore.copy().append(SERIALIZER_detectRecursions, true).append(SERIALIZER_ignoreRecursions, true));
		this.ctx = createContext(HtmlDocSerializerContext.class);
	}

	@Override /* Serializer */
	public HtmlDocSerializerSession createSession(SerializerSessionArgs args) {
		return new HtmlDocSerializerSession(ctx, args);
	}
}
