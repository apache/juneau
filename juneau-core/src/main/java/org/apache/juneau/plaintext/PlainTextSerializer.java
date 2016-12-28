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
 * <h6 class='topic'>Media types</h6>
 * <p>
 * 	Handles <code>Accept</code> types: <code>text/plain</code>
 * <p>
 * 	Produces <code>Content-Type</code> types: <code>text/plain</code>
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	Essentially converts POJOs to plain text using the <code>toString()</code> method.
 * <p>
 * 	Also serializes objects using a transform if the object class has an {@link PojoSwap PojoSwap&lt;?,String&gt;} transform defined on it.
 *
 * <h6 class='topic'>Configurable properties</h6>
 * <p>
 * 	This class has the following properties associated with it:
 * <ul>
 * 	<li>{@link SerializerContext}
 * 	<li>{@link BeanContext}
 * </ul>
 */
@Produces("text/plain")
public final class PlainTextSerializer extends WriterSerializer {

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Serializer */
	protected void doSerialize(SerializerSession session, Object o) throws Exception {
		session.getWriter().write(o == null ? "null" : session.convertToType(o, String.class));
	}

	@Override /* Serializer */
	public PlainTextSerializer clone() {
		try {
			return (PlainTextSerializer)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // Shouldn't happen.
		}
	}
}
