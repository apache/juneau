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
package org.apache.juneau.plaintext;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;

/**
 * Serializes POJOs to plain text using just the <code>toString()</code> method on the serialized object.
 *
 * <h5 class='section'>Media types:</h5>
 * <p>
 * Handles <code>Accept</code> types: <code>text/plain</code>
 * <p>
 * Produces <code>Content-Type</code> types: <code>text/plain</code>
 *
 * <h5 class='section'>Description:</h5>
 * <p>
 * Essentially converts POJOs to plain text using the <code>toString()</code> method.
 * <p>
 * Also serializes objects using a transform if the object class has an {@link PojoSwap PojoSwap&lt;?,String&gt;} transform defined on it.
 *
 * <h5 class='section'>Configurable properties:</h5>
 * <p>
 * This class has the following properties associated with it:
 * <ul>
 * 	<li>{@link SerializerContext}
 * 	<li>{@link BeanContext}
 * </ul>
 */
@Produces("text/plain")
public class PlainTextSerializer extends WriterSerializer {

	/** Default serializer, all default settings.*/
	public static final PlainTextSerializer DEFAULT = new PlainTextSerializer(PropertyStore.create());


	/**
	 * Constructor.
	 * @param propertyStore The property store containing all the settings for this object.
	 */
	public PlainTextSerializer(PropertyStore propertyStore) {
		super(propertyStore);
	}

	@Override /* CoreObject */
	public PlainTextSerializerBuilder builder() {
		return new PlainTextSerializerBuilder(propertyStore);
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Serializer */
	protected void doSerialize(SerializerSession session, Object o) throws Exception {
		session.getWriter().write(o == null ? "null" : session.convertToType(o, String.class));
	}
}
