/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2011, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.html;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.serializer.*;

/**
 * Serializes POJOs to HTTP responses as stripped HTML.
 *
 *
 * <h6 class='topic'>Media types</h6>
 * <p>
 * 	Handles <code>Accept</code> types: <code>text/html+stripped</code>
 * <p>
 * 	Produces <code>Content-Type</code> types: <code>text/html</code>
 *
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	Produces the same output as {@link HtmlDocSerializer}, but without the header and body tags and page title and description.
 * 	Used primarily for JUnit testing the {@link HtmlDocSerializer} class.
 *
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Produces(value="text/html+stripped",contentType="text/html")
public class HtmlStrippedDocSerializer extends HtmlSerializer {

	//---------------------------------------------------------------------------
	// Overridden methods
	//---------------------------------------------------------------------------

	@Override /* Serializer */
	protected void doSerialize(Object o, Writer out, SerializerContext ctx) throws IOException, SerializeException {
		HtmlSerializerContext hctx = (HtmlSerializerContext)ctx;
		HtmlSerializerWriter w = hctx.getWriter(out);
		if (o == null
			|| (o instanceof Collection && ((Collection<?>)o).size() == 0)
			|| (o.getClass().isArray() && Array.getLength(o) == 0))
			w.sTag(1, "p").append("No Results").eTag("p").nl();
		else
			super.doSerialize(o, w, hctx);
	}
}
