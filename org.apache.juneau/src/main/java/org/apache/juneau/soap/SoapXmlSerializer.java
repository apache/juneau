/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.soap;

import static org.apache.juneau.soap.SoapXmlSerializerContext.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.xml.*;

/**
 * Serializes POJOs to HTTP responses as XML+SOAP.
 *
 *
 * <h6 class='topic'>Media types</h6>
 * <p>
 * 	Handles <code>Accept</code> types: <code>text/xml+soap</code>
 * <p>
 * 	Produces <code>Content-Type</code> types: <code>text/xml+soap</code>
 *
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	Essentially the same output as {@link XmlDocSerializer}, except wrapped in a standard SOAP envelope.
 *
 *
 * <h6 class='topic'>Configurable properties</h6>
 * <p>
 * 	This class has the following properties associated with it:
 * <ul>
 * 	<li>{@link SoapXmlSerializerContext}
 * 	<li>{@link BeanContext}
 * </ul>
 *
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
@Produces(value="text/xml+soap",contentType="text/xml")
public final class SoapXmlSerializer extends XmlSerializer {

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
			.attr("xmlns", "soap", s.getProperties().getString(SOAPXML_SOAPAction, "http://www.w3.org/2003/05/soap-envelope"))
			.appendln(">");
		w.sTag(1, "soap", "Body").nl();
		super.serialize(s, o);
		w.eTag(1, "soap", "Body").nl();
		w.eTag("soap", "Envelope").nl();
	}

	@Override /* Serializer */
	public ObjectMap getResponseHeaders(ObjectMap properties) {
		return super.getResponseHeaders(properties)
			.append("SOAPAction", properties.getString(SOAPXML_SOAPAction, "http://www.w3.org/2003/05/soap-envelope"));
	}
}
