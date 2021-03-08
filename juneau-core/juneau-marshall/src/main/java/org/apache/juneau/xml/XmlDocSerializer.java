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
import org.apache.juneau.collections.*;
import org.apache.juneau.serializer.*;

/**
 * Serializes POJOs to HTTP responses as XML.
 *
 * <h5 class='topic'>Media types</h5>
 *
 * Handles <c>Accept</c> types:  <bc>text/xml</bc>
 * <p>
 * Produces <c>Content-Type</c> types:  <bc>text/xml</bc>
 *
 * <h5 class='topic'>Description</h5>
 *
 * Same as {@link XmlSerializer}, except prepends <code><xt>&lt;?xml</xt> <xa>version</xa>=<xs>'1.0'</xs>
 * <xa>encoding</xa>=<xs>'UTF-8'</xs><xt>?&gt;</xt></code> to the response to make it a valid XML document.
 *
 */
public class XmlDocSerializer extends XmlSerializer {

	//-------------------------------------------------------------------------------------------------------------------
	// Predefined subclasses
	//-------------------------------------------------------------------------------------------------------------------

	/** Default serializer without namespaces. */
	public static class Ns extends XmlDocSerializer {

		/**
		 * Constructor.
		 *
		 * @param cp The property store containing all the settings for this object.
		 */
		public Ns(ContextProperties cp) {
			super(cp.copy().setDefault(XML_enableNamespaces, true).build());
		}
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 *
	 * @param cp The property store containing all the settings for this object.
	 */
	public XmlDocSerializer(ContextProperties cp) {
		super(cp);
	}

	@Override /* Serializer */
	public XmlDocSerializerSession createSession() {
		return createSession(createDefaultSessionArgs());
	}

	@Override /* Serializer */
	public XmlDocSerializerSession createSession(SerializerSessionArgs args) {
		return new XmlDocSerializerSession(this, args);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public OMap toMap() {
		return super.toMap()
			.a(
				"XmlDocSerializer",
				OMap
					.create()
					.filtered()
			);
	}
}
