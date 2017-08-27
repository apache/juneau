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
package org.apache.juneau.soap;

import org.apache.juneau.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.xml.*;

/**
 * Serializes POJOs to HTTP responses as XML+SOAP.
 *
 * <h5 class='section'>Media types:</h5>
 *
 * Handles <code>Accept</code> types: <code>text/xml+soap</code>
 *
 * <p>
 * Produces <code>Content-Type</code> types: <code>text/xml+soap</code>
 *
 * <h5 class='section'>Description:</h5>
 *
 * Essentially the same output as {@link XmlDocSerializer}, except wrapped in a standard SOAP envelope.
 *
 * <h5 class='section'>Configurable properties:</h5>
 *
 * This class has the following properties associated with it:
 * <ul>
 * 	<li>{@link SoapXmlSerializerContext}
 * 	<li>{@link BeanContext}
 * </ul>
 */
public final class SoapXmlSerializer extends XmlSerializer {

	private final SoapXmlSerializerContext ctx;

	/**
	 * Constructor.
	 *
	 * @param propertyStore The property store containing all the settings for this object.
	 */
	public SoapXmlSerializer(PropertyStore propertyStore) {
		super(propertyStore, "text/xml", "text/xml+soap");
		this.ctx = createContext(SoapXmlSerializerContext.class);
	}

	@Override /* Serializer */
	public WriterSerializerSession createSession(SerializerSessionArgs args) {
		return new SoapXmlSerializerSession(ctx, args);
	}
}
