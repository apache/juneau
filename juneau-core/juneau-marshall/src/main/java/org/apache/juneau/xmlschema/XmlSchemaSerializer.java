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
package org.apache.juneau.xmlschema;

import org.apache.juneau.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.xml.*;

/**
 * Serializes POJO metadata to HTTP responses as XML.
 *
 * <h5 class='topic'>Media types</h5>
 *
 * Handles <code>Accept</code> types:  <code><b>text/xml+schema</b></code>
 * <p>
 * Produces <code>Content-Type</code> types:  <code><b>text/xml</b></code>
 *
 * <h5 class='topic'>Description</h5>
 *
 * Produces the XML-schema representation of the XML produced by the {@link XmlSerializer} class with the same properties.
 */
public class XmlSchemaSerializer extends XmlSerializer {

	/**
	 * Constructor.
	 *
	 * @param ps Initialize with the specified config property store.
	 */
	public XmlSchemaSerializer(PropertyStore ps) {
		super(
			ps.builder()
				.set(XML_enableNamespaces, true)
				.build(),
			"text/xml",
			"text/xml+schema"
		);
	}

	@Override /* Serializer */
	public XmlSchemaSerializerSession createSession() {
		return createSession(createDefaultSessionArgs());
	}

	@Override /* Serializer */
	public XmlSchemaSerializerSession createSession(SerializerSessionArgs args) {
		return new XmlSchemaSerializerSession(this, args);
	}
}
