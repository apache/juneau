/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.encoders;

import java.io.*;

/**
 * Used for enabling decompression on requests and compression on responses, such as support for GZIP compression.
 *
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	Used to wrap input and output streams withing compression/decompression streams.
 * <p>
 * 	Encoders are registered with <code>RestServlets</code> through the <ja>@RestResource.encoders()</ja> annotation.
 *
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public abstract class Encoder {

	/**
	 * Converts the specified compressed input stream into an uncompressed stream.
	 *
	 * @param is The compressed stream.
	 * @return The uncompressed stream.
	 * @throws IOException If any errors occur, such as on a stream that's not a valid GZIP input stream.
	 */
	public abstract InputStream getInputStream(InputStream is) throws IOException;

	/**
	 * Converts the specified uncompressed output stream into an uncompressed stream.
	 *
	 * @param os The uncompressed stream.
	 * @return The compressed stream stream.
	 * @throws IOException If any errors occur.
	 */
	public abstract OutputStream getOutputStream(OutputStream os) throws IOException;

	/**
	 * Returns the codings in <code>Content-Encoding</code> and <code>Accept-Encoding</code> headers
	 * 	that this encoder handles (e.g. <js>"gzip"</js>).
	 *
	 * @return The codings that this encoder handles.
	 */
	public abstract String[] getCodings();
}
