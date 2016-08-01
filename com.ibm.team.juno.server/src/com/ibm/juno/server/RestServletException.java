/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.server;

import java.text.*;

import javax.servlet.*;

/**
 * General exception thrown from {@link RestServlet} during construction or initialization.
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class RestServletException extends ServletException {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param message The detailed message.
	 * @param args Optional message arguments.
	 */
	public RestServletException(String message, Object...args) {
		super(args.length == 0 ? message : MessageFormat.format(message, args));
	}

	/**
	 * Sets the inner cause for this exception.
	 *
	 * @param cause The inner cause.
	 * @return This object (for method chaining).
	 */
	@Override /* Throwable */
	public synchronized RestServletException initCause(Throwable cause) {
		super.initCause(cause);
		return this;
	}
}
