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
import org.apache.juneau.annotation.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.xml.*;

/**
 * Serializes POJOs to HTTP responses as XML+SOAP.
 *
 * <h5 class='topic'>Media types</h5>
 *
 * Handles <code>Accept</code> types:  <code><b>text/xml+soap</b></code>
 * <p>
 * Produces <code>Content-Type</code> types:  <code><b>text/xml+soap</b></code>
 *
 * <h5 class='topic'>Description</h5>
 *
 * Essentially the same output as {@link XmlDocSerializer}, except wrapped in a standard SOAP envelope.
 */
@ConfigurableContext
public final class SoapXmlSerializer extends XmlSerializer {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	static final String PREFIX = "SoapXmlSerializer";

	/**
	 * Configuration property:  The <code>SOAPAction</code> HTTP header value to set on responses.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"SoapXmlSerializer.SOAPAction.s"</js>
	 * 	<li><b>Data type:</b>  <code>String</code>
	 * 	<li><b>Default:</b>  <js>"http://www.w3.org/2003/05/soap-envelope"</js>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link SoapXmlSerializerBuilder#soapAction(String)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String SOAPXML_SOAPAction = PREFIX + ".SOAPAction.s";


	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	final String soapAction;

	/**
	 * Constructor.
	 *
	 * @param ps The property store containing all the settings for this object.
	 */
	public SoapXmlSerializer(PropertyStore ps) {
		super(ps, "text/xml", "text/xml+soap");
		soapAction = getStringProperty(SOAPXML_SOAPAction, "http://www.w3.org/2003/05/soap-envelope");
	}

	@Override /* Context */
	public SoapXmlSerializerBuilder builder() {
		return new SoapXmlSerializerBuilder(getPropertyStore());
	}

	/**
	 * Instantiates a new clean-slate {@link SoapXmlSerializerBuilder} object.
	 *
	 * <p>
	 * This is equivalent to simply calling <code><jk>new</jk> SoapXmlSerializerBuilder()</code>.
	 *
	 * <p>
	 * Note that this method creates a builder initialized to all default settings, whereas {@link #builder()} copies
	 * the settings of the object called on.
	 *
	 * @return A new {@link SoapXmlSerializerBuilder} object.
	 */
	public static SoapXmlSerializerBuilder create() {
		return new SoapXmlSerializerBuilder();
	}

	@Override /* Serializer */
	public SoapXmlSerializerSession createSession() {
		return createSession(createDefaultSessionArgs());
	}

	@Override /* Serializer */
	public SoapXmlSerializerSession createSession(SerializerSessionArgs args) {
		return new SoapXmlSerializerSession(this, args);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  The SOAPAction HTTP header value to set on responses.
	 *
	 * @see #SOAPXML_SOAPAction
	 * @return
	 * 	The SOAPAction HTTP header value to set on responses.
	 */
	public String getSoapAction() {
		return soapAction;
	}
}
