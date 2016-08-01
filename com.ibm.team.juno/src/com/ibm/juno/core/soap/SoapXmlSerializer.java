/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2011, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.soap;

import static com.ibm.juno.core.soap.SoapXmlSerializerProperties.*;

import java.io.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.core.xml.*;

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
 * 	<li>{@link SoapXmlSerializerProperties}
 * 	<li>{@link XmlSerializerProperties}
 * 	<li>{@link SerializerProperties}
 * 	<li>{@link BeanContextProperties}
 * </ul>
 *
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Produces(value="text/xml+soap",contentType="text/xml")
public final class SoapXmlSerializer extends XmlSerializer {

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Serializer */
	protected void doSerialize(Object o, Writer out, SerializerContext ctx) throws IOException, SerializeException {
		XmlSerializerContext xctx = (XmlSerializerContext)ctx;
		XmlSerializerWriter w = xctx.getWriter(out);
		w.append("<?xml")
			.attr("version", "1.0")
			.attr("encoding", "UTF-8")
			.appendln("?>");
		w.oTag("soap", "Envelope")
			.attr("xmlns", "soap", xctx.getProperties().getString(SOAPXML_SOAPAction, "http://www.w3.org/2003/05/soap-envelope"))
			.appendln(">");
		w.sTag(1, "soap", "Body").nl();
		super.serialize(o, w, ctx);
		w.eTag(1, "soap", "Body").nl();
		w.eTag("soap", "Envelope").nl();
	}

	@Override /* Serializer */
	public ObjectMap getResponseHeaders(ObjectMap properties) {
		return super.getResponseHeaders(properties)
			.append("SOAPAction", properties.getString(SOAPXML_SOAPAction, "http://www.w3.org/2003/05/soap-envelope"));
	}
}
