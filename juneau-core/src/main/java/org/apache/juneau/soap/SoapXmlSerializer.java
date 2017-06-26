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

import static org.apache.juneau.soap.SoapXmlSerializerContext.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.xml.*;

/**
 * Serializes POJOs to HTTP responses as XML+SOAP.
 *
 * <h5 class='section'>Media types:</h5>
 * <p>
 * Handles <code>Accept</code> types: <code>text/xml+soap</code>
 * <p>
 * Produces <code>Content-Type</code> types: <code>text/xml+soap</code>
 *
 * <h5 class='section'>Description:</h5>
 * <p>
 * Essentially the same output as {@link XmlDocSerializer}, except wrapped in a standard SOAP envelope.
 *
 * <h5 class='section'>Configurable properties:</h5>
 * <p>
 * This class has the following properties associated with it:
 * <ul>
 * 	<li>{@link SoapXmlSerializerContext}
 * 	<li>{@link BeanContext}
 * </ul>
 */
@Produces(value="text/xml+soap",contentType="text/xml")
public final class SoapXmlSerializer extends XmlSerializer {

	/**
	 * Constructor.
	 *
	 * @param propertyStore The property store containing all the settings for this object.
	 */
	public SoapXmlSerializer(PropertyStore propertyStore) {
		super(propertyStore);
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Serializer */
	protected void doSerialize(SerializerSession session, Object o) throws Exception {
		XmlSerializerSession s = (XmlSerializerSession)session;
		XmlWriter w = s.getWriter();
		w.append("<?xml")
			.attr("version", "1.0")
			.attr("encoding", "UTF-8")
			.appendln("?>");
		w.oTag("soap", "Envelope")
			.attr("xmlns", "soap", s.getProperty(SOAPXML_SOAPAction, "http://www.w3.org/2003/05/soap-envelope"))
			.appendln(">");
		w.sTag(1, "soap", "Body").nl(1);
		super.doSerialize(s, o);
		w.ie(1).eTag("soap", "Body").nl(1);
		w.eTag("soap", "Envelope").nl(0);
	}

	@Override /* Serializer */
	public ObjectMap getResponseHeaders(ObjectMap properties) {
		return new ObjectMap(super.getResponseHeaders(properties))
			.append("SOAPAction", properties.getString(SOAPXML_SOAPAction, "http://www.w3.org/2003/05/soap-envelope"));
	}
}
