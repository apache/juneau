/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core;

import java.text.*;

/**
 * General bean runtime operation exception.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class BeanRuntimeException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param message The error message.
	 */
	public BeanRuntimeException(String message) {
		super(message);
	}

	/**
	 * Shortcut for calling <code><jk>new</jk> BeanRuntimeException(String.format(c.getName() + <js>": "</js> + message, args));</code>
	 *
	 * @param c The class name of the bean that caused the exception.
	 * @param message The error message.
	 * @param args Arguments passed in to the {@code String.format()} method.
	 */
	public BeanRuntimeException(Class<?> c, String message, Object... args) {
		this(c.getName() + ": " + (args.length == 0 ? message : MessageFormat.format(message, args)));
	}

	/**
	 * Constructor.
	 *
	 * @param cause The initial cause of the exception.
	 */
	public BeanRuntimeException(Throwable cause) {
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
	public synchronized BeanRuntimeException initCause(Throwable cause) {
		super.initCause(cause);
		return this;
	}
}
