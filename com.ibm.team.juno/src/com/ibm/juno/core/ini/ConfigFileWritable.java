/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.ini;

import java.io.*;

import com.ibm.juno.core.*;

/**
 * Wraps a {@link ConfigFile} in a {@link Writable} to be rendered as plain text.
 */
class ConfigFileWritable implements Writable {

	private ConfigFileImpl cf;

	protected ConfigFileWritable(ConfigFileImpl cf) {
		this.cf = cf;
	}

	@Override /* Writable */
	public void writeTo(Writer out) throws IOException {
		cf.readLock();
		try {
			cf.serializeTo(out);
		} finally {
			cf.readUnlock();
		}
	}

	@Override /* Writable */
	public String getMediaType() {
		return "text/plain";
	}
}
