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

import static org.apache.juneau.soap.SoapXmlSerializer.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.utils.*;
import org.apache.juneau.xml.*;

/**
 * Session object that lives for the duration of a single use of {@link SoapXmlSerializer}.
 *
 * <p>
 * This class is NOT thread safe.
 * It is typically discarded after one-time use although it can be reused within the same thread.
 */
public class SoapXmlSerializerSession extends XmlSerializerSession {

	private final String soapAction;

	/**
	 * Create a new session using properties specified in the context.
	 *
	 * @param ctx
	 * 	The context creating this session object.
	 * 	The context contains all the configuration settings for this object.
	 * @param args
	 * 	Runtime arguments.
	 * 	These specify session-level information such as locale and URI context.
	 * 	It also include session-level properties that override the properties defined on the bean and
	 * 	serializer contexts.
	 */
	public SoapXmlSerializerSession(SoapXmlSerializer ctx, SerializerSessionArgs args) {
		super(ctx, args);

		soapAction = getProperty(SOAPXML_SOAPAction, String.class, ctx.soapAction);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Overridden methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* SerializerSession */
	protected void doSerialize(SerializerPipe out, Object o) throws Exception {
		try (XmlWriter w = getXmlWriter(out)) {
			w.append("<?xml")
				.attr("version", "1.0")
				.attr("encoding", "UTF-8")
				.appendln("?>");
			w.oTag("soap", "Envelope")
				.attr("xmlns", "soap", soapAction)
				.appendln(">");
			w.sTag(1, "soap", "Body").nl(1);
			indent += 2;
			w.flush();
			super.doSerialize(out, o);
			w.ie(1).eTag("soap", "Body").nl(1);
			w.eTag("soap", "Envelope").nl(0);
		}
	}

	@Override /* Serializer */
	public Map<String,String> getResponseHeaders() {
		return new AMap<String,String>().append("SOAPAction", soapAction);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  The SOAPAction HTTP header value to set on responses.
	 *
	 * @see SoapXmlSerializer#SOAPXML_SOAPAction
	 * @return
	 * 	The SOAPAction HTTP header value to set on responses.
	 */
	public String getSoapAction() {
		return soapAction;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Session */
	public ObjectMap toMap() {
		return super.toMap()
			.append("SoapXmlSerializerSession", new DefaultFilteringObjectMap()
				.append("soapAction", soapAction)
			);
	}
}
