/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.jso;

import java.io.*;

import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.serializer.*;

/**
 * Serializes POJOs to HTTP responses as Java {@link ObjectOutputStream ObjectOutputStreams}.
 *
 *
 * <h6 class='topic'>Media types</h6>
 * <p>
 * 	Handles <code>Accept</code> types: <code>application/x-java-serialized-object</code>
 * <p>
 * 	Produces <code>Content-Type</code> types: <code>application/x-java-serialized-object</code>
 *
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Produces("application/x-java-serialized-object")
public final class JavaSerializedObjectSerializer extends OutputStreamSerializer {

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* OutputStreamSerializer */
	protected void doSerialize(Object o, OutputStream out, SerializerContext ctx) throws IOException, SerializeException {
		ObjectOutputStream oos = new ObjectOutputStream(out);
		oos.writeObject(o);
		oos.flush();
		oos.close();
	}

	@Override /* Serializer */
	public JavaSerializedObjectSerializer clone() {
		try {
			return (JavaSerializedObjectSerializer)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // Shouldn't happen
		}
	}
}
