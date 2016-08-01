/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2011, 2016. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.serializer;

import com.ibm.juno.core.*;

/**
 * General exception thrown whenever an error occurs during serialization.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class SerializeException extends FormattedException {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param msg The error message.
	 * @param args Optional printf arguments to replace in the error message.
	 */
	public SerializeException(String msg, Object... args) {
		super(msg, args);
	}

	/**
	 * Constructor.
	 *
	 * @param cause The cause.
	 */
	public SerializeException(Throwable cause) {
		super(cause == null ? null : cause.getLocalizedMessage());
		initCause(cause);
	}

	/**
	 * Sets the inner cause for this exception.
	 *
	 * @param cause The inner cause.
	 * @return This object (for method chaining).
	 */
	@Override /* Throwable */
	public synchronized SerializeException initCause(Throwable cause) {
		super.initCause(cause);
		return this;
	}
}
