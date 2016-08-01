/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2011, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.xml;

import java.io.*;

import com.ibm.juno.core.serializer.*;

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
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class XmlSchemaDocSerializer extends XmlSchemaSerializer {

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
		super.doSerialize(o, w, ctx);
	}
}
