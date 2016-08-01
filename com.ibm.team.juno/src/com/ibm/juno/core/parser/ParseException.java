/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2016. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.parser;

import java.text.*;

import com.ibm.juno.core.*;

/**
 * Exception that indicates invalid syntax encountered during parsing.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class ParseException extends FormattedException {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param msg The error message.
	 * @param args Optional printf arguments to replace in the error message.
	 */
	public ParseException(String msg, Object...args) {
		super(msg, args);
	}

	/**
	 * Constructor.
	 *
	 * @param lineNumber The line number where the parse error was detected.
	 * @param colNumber The column number where the parse error was detected.
	 * @param msg The error message.
	 * @param args Optional printf arguments to replace in the error message.
	 */
	public ParseException(int lineNumber, int colNumber, String msg, Object... args) {
		super(getMessage(lineNumber, colNumber, msg, args));
	}

	private static String getMessage(int lineNumber, int colNumber, String msg, Object... args) {
		if (args.length != 0)
			msg = MessageFormat.format(msg, args);
		if (lineNumber != -1)
			msg = "Parse exception occurred at line='"+lineNumber+"', column='"+colNumber+"'.  " + msg;
		return msg;
	}

	/**
	 * Constructor.
	 *
	 * @param cause The cause of the parse exception.
	 */
	public ParseException(Exception cause) {
		super(cause == null ? null : cause.getLocalizedMessage());
		initCause(cause);
	}

	/**
	 * Returns the highest-level <code>ParseException</code> in the stack trace.
	 * Useful for JUnit testing of error conditions.
	 *
	 * @return The root parse exception, or this exception if there isn't one.
	 */
	public ParseException getRootCause() {
		ParseException t = this;
		while (! (t.getCause() == null || ! (t.getCause() instanceof ParseException)))
			t = (ParseException)t.getCause();
		return t;
	}

	/**
	 * Sets the inner cause for this exception.
	 *
	 * @param cause The inner cause.
	 * @return This object (for method chaining).
	 */
	@Override /* Throwable */
	public synchronized ParseException initCause(Throwable cause) {
		super.initCause(cause);
		return this;
	}
}
