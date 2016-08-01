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
 * Encoder for handling <js>"identity"</js> encoding and decoding (e.g. no encoding at all).
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class IdentityEncoder extends Encoder {

	/** Singleton */
	public static final IdentityEncoder INSTANCE = new IdentityEncoder();

	/** Constructor. */
	protected IdentityEncoder() {}

	@Override /* Encoder */
	public InputStream getInputStream(InputStream is) throws IOException {
		return is;
	}

	@Override /* Encoder */
	public OutputStream getOutputStream(OutputStream os) throws IOException {
		return os;
	}

	/**
	 * Returns <code>[<js>"identity"</js>]</code>.
	 */
	@Override /* Encoder */
	public String[] getCodings() {
		return new String[]{"identity"};
	}
}
