/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2016. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core;

import java.text.*;

/**
 * Subclass of runtime exceptions that take in a message and zero or more arguments.
 * <p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class FormattedRuntimeException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param message The {@link MessageFormat}-style message.
	 * @param args The arguments in the message.
	 */
	public FormattedRuntimeException(String message, Object...args) {
		super(args.length == 0 ? message : MessageFormat.format(message, args));
	}

	/**
	 * Constructor.
	 *
	 * @param causedBy The cause of this exception.
	 * @param message The {@link MessageFormat}-style message.
	 * @param args The arguments in the message.
	 */
	public FormattedRuntimeException(Throwable causedBy, String message, Object...args) {
		this(message, args);
		initCause(causedBy);
	}
}
