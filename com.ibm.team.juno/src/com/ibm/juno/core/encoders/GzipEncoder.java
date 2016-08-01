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
import java.util.zip.*;

/**
 * Encoder for handling <js>"gzip"</js> encoding and decoding.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class GzipEncoder extends Encoder {

	@Override /* Encoder */
	public OutputStream getOutputStream(OutputStream os) throws IOException {
		return new GZIPOutputStream(os) {
			@Override /* OutputStream */
			public final void close() throws IOException {
				finish();
				super.close();
			}
		};
	}

	@Override /* Encoder */
	public InputStream getInputStream(InputStream is) throws IOException {
		return new GZIPInputStream(is);
	}

	/**
	 * Returns <code>[<js>"gzip"</js>]</code>.
	 */
	@Override /* Encoder */
	public String[] getCodings() {
		return new String[]{"gzip"};
	}
}
