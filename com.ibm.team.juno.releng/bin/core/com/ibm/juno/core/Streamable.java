/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core;

import java.io.*;

/**
 * Interface that identifies that an object can be serialized directly to an output stream.
 * <p>
 * 	Instances must identify the media type of the content by implementing the
 * 	{@link #getMediaType()} method.
 * </p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public interface Streamable {

	/**
	 * Serialize this object to the specified output stream.
	 *
	 * @param os The output stream to stream to.
	 * @throws IOException
	 */
	void streamTo(OutputStream os) throws IOException;

	/**
	 * Returns the serialized media type for this resource (e.g. <js>"text/html"</js>).
	 *
	 * @return The media type, or <jk>null</jk> if the media type is not known.
	 */
	String getMediaType();
}
