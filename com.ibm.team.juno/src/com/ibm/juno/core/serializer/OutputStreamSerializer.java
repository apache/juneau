/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.serializer;

import java.io.*;

import com.ibm.juno.core.annotation.*;

/**
 * Subclass of {@link Serializer} for byte-based serializers.
 *
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	This class is typically the parent class of all byte-based serializers.
 * 	It has 1 abstract method to implement...
 * <ul>
 * 	<li>{@link #doSerialize(Object, OutputStream, SerializerContext)}
 * </ul>
 *
 *
 * <h6 class='topic'>@Produces annotation</h6>
 * <p>
 * 	The media types that this serializer can produce is specified through the {@link Produces @Produces} annotation.
 * <p>
 * 	However, the media types can also be specified programmatically by overriding the {@link #getMediaTypes()}
 * 		and {@link #getResponseContentType()} methods.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public abstract class OutputStreamSerializer extends Serializer<OutputStream> {

	@Override /* Serializer */
	public boolean isWriterSerializer() {
		return false;
	}

	//--------------------------------------------------------------------------------
	// Abstract methods
	//--------------------------------------------------------------------------------

	@Override /* Serializer */
	protected abstract void doSerialize(Object o, OutputStream out, SerializerContext ctx) throws IOException, SerializeException;

}
