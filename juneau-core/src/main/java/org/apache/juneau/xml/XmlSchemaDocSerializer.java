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
package org.apache.juneau.xml;

import org.apache.juneau.*;
import org.apache.juneau.serializer.*;

/**
 * Serializes POJO metadata to HTTP responses as XML.
 *
 *
 * <h6 class='topic'>Media types</h6>
 * <p>
 * 	Handles <code>Accept</code> types: <code>text/xml+schema</code>
 * <p>
 * 	Produces <code>Content-Type</code> types: <code>text/xml</code>
 *
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	Same as {@link XmlSchemaSerializer}, except prepends <code><xt>&lt;?xml</xt> <xa>version</xa>=<xs>'1.0'</xs> <xa>encoding</xa>=<xs>'UTF-8'</xs><xt>?&gt;</xt></code> to the response
 * 	to make it a valid XML document.
 *
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public class XmlSchemaDocSerializer extends XmlSchemaSerializer {

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	/**
	 * Constructor.
	 */
	public XmlSchemaDocSerializer() {}

	/**
	 * Constructor.
	 *
	 * @param cf The context factory to use for creating the context for this serializer.
	 */
	protected XmlSchemaDocSerializer(ContextFactory cf) {
		super(cf);
	}

	@Override /* Serializer */
	protected void doSerialize(SerializerSession session, Object o) throws Exception {
		XmlSerializerSession s = (XmlSerializerSession)session;
		XmlWriter w = s.getWriter();
		w.append("<?xml")
			.attr("version", "1.0")
			.attr("encoding", "UTF-8")
			.appendln("?>");
		super.doSerialize(s, o);
	}
}
