/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.plaintext;

import java.io.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.filter.*;
import com.ibm.juno.core.serializer.*;

/**
 * Serializes POJOs to plain text using just the <code>toString()</code> method on the serialized object.
 *
 *
 * <h6 class='topic'>Media types</h6>
 * <p>
 * 	Handles <code>Accept</code> types: <code>text/plain</code>
 * <p>
 * 	Produces <code>Content-Type</code> types: <code>text/plain</code>
 *
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	Essentially converts POJOs to plain text using the <code>toString()</code> method.
 * <p>
 * 	Also serializes objects using a filter if the object class has an {@link PojoFilter PojoFilter&lt;?,String&gt;} filter defined on it.
 *
 *
 * <h6 class='topic'>Configurable properties</h6>
 * <p>
 * 	This class has the following properties associated with it:
 * <ul>
 * 	<li>{@link BeanContextProperties}
 * </ul>
 *
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Produces("text/plain")
public final class PlainTextSerializer extends WriterSerializer {

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Serializer */
	protected void doSerialize(Object o, Writer out, SerializerContext ctx) throws IOException, SerializeException {
		out.write(o == null ? "null" : ctx.getBeanContext().convertToType(o, String.class));
	}

	@Override /* Serializer */
	public PlainTextSerializer clone() {
		try {
			return (PlainTextSerializer)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // Shouldn't happen.
		}
	}
}
