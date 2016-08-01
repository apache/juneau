/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.utils;

import java.io.*;

/**
 * Subclass of a ByteArrayOutputStream that avoids a byte array copy when reading from an input stream.
 * <p>
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class ByteArrayInOutStream extends ByteArrayOutputStream {

	/**
	 * Creates a new input stream from this object.
	 *
	 * @return A new input stream from this object.
	 */
	public ByteArrayInputStream getInputStream() {
		return new ByteArrayInputStream(this.buf, 0, this.count);
	}
}