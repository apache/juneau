/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.utils;

import java.net.*;
import java.text.*;

/**
 * Generic exception thrown from the {@link PojoRest} class.
 * <p>
 * 	Typically, this is a user-error, such as trying to address a non-existent node in the tree.
 * <p>
 * 	The status code is an HTTP-equivalent code.  It will be one of the following:
 * <ul>
 * 	<li>{@link HttpURLConnection#HTTP_BAD_REQUEST HTTP_BAD_REQUEST} - Attempting to do something impossible.
 * 	<li>{@link HttpURLConnection#HTTP_NOT_FOUND HTTP_NOT_FOUND} - Attempting to access a non-existent node in the tree.
 * 	<li>{@link HttpURLConnection#HTTP_FORBIDDEN HTTP_FORBIDDEN} - Attempting to overwrite the root object.
 * </ul>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class PojoRestException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private int status;

	/**
	 * Constructor.
	 *
	 * @param status The HTTP-equivalent status code.
	 * @param message The detailed message.
	 * @param args Optional message arguments.
	 */
	public PojoRestException(int status, String message, Object...args) {
		super(args.length == 0 ? message : MessageFormat.format(message, args));
		this.status = status;
	}

	/**
	 * The HTTP-equivalent status code.
	 * <p>
	 * 	See above for details.
	 *
	 * @return The HTTP-equivalent status code.
	 */
	public int getStatus() {
		return status;
	}
}
