/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2016. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.parser;

import static com.ibm.juno.core.parser.ParserProperties.*;

import java.lang.reflect.*;
import java.util.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.utils.*;
import com.ibm.juno.core.utils.log.*;

/**
 * Context object that lives for the duration of a single parsing of {@link Parser} and its subclasses.
 * <p>
 * 	This class is NOT thread safe.  It is meant to be discarded after one-time use.
 * </p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class ParserContext {

	private static JunoLogger logger = JunoLogger.getLogger(ParserContext.class);

	private final boolean debug, trimStrings;
	private boolean closed;
	private final BeanContext beanContext;
	private final List<String> warnings = new LinkedList<String>();

	private final ObjectMap properties;
	private final Method javaMethod;
	private final Object outer;

	/**
	 * Create a new parser context with the specified options.
	 *
	 * @param beanContext The bean context being used.
	 * @param pp The default parser properties.
	 * @param op The override properties.
	 * @param javaMethod The java method that called this parser, usually the method in a REST servlet.
	 * @param outer The outer object for instantiating top-level non-static inner classes.
	 */
	public ParserContext(BeanContext beanContext, ParserProperties pp, ObjectMap op, Method javaMethod, Object outer) {
		if (op == null || op.isEmpty()) {
			debug = pp.debug;
			trimStrings = pp.trimStrings;
		} else {
			debug = op.getBoolean(PARSER_debug, pp.debug);
			trimStrings = op.getBoolean(PARSER_trimStrings, pp.trimStrings);
		}
		this.beanContext = beanContext;
		this.properties = op;
		this.javaMethod = javaMethod;
		this.outer = outer;
	}

	/**
	 * Returns the bean context associated with this context.
	 *
	 * @return The bean context associated with this context.
	 */
	public final BeanContext getBeanContext() {
		return beanContext;
	}

	/**
	 * Returns the Java method that invoked this parser.
	 * <p>
	 * When using the REST API, this is the Java method invoked by the REST call.
	 * Can be used to access annotations defined on the method or class.
	 *
	 * @return The Java method that invoked this parser.
	*/
	public final Method getJavaMethod() {
		return javaMethod;
	}

	/**
	 * Returns the outer object used for instantiating top-level non-static member classes.
	 * When using the REST API, this is the servlet object.
	 *
	 * @return The outer object.
	*/
	public final Object getOuter() {
		return outer;
	}

	/**
	 * Returns the {@link ParserProperties#PARSER_debug} setting value in this context.
	 *
	 * @return The {@link ParserProperties#PARSER_debug} setting value in this context.
	 */
	public final boolean isDebug() {
		return debug;
	}

	/**
	 * Returns the {@link ParserProperties#PARSER_trimStrings} setting value in this context.
	 *
	 * @return The {@link ParserProperties#PARSER_trimStrings} setting value in this context.
	 */
	public final boolean isTrimStrings() {
		return trimStrings;
	}

	/**
	 * Returns the runtime properties associated with this context.
	 *
	 * @return The runtime properties associated with this context.
	 */
	public final ObjectMap getProperties() {
		return properties;
	}

	/**
	 * Logs a warning message.
	 *
	 * @param msg The warning message.
	 * @param args Optional printf arguments to replace in the error message.
	 */
	public void addWarning(String msg, Object... args) {
		logger.warning(msg, args);
		msg = args.length == 0 ? msg : String.format(msg, args);
		warnings.add((warnings.size() + 1) + ": " + msg);
	}

	/**
	 * Trims the specified object if it's a <code>String</code> and {@link #isTrimStrings()} returns <jk>true</jk>.
	 *
	 * @param o The object to trim.
	 * @return The trimmmed string if it's a string.
	 */
	@SuppressWarnings("unchecked")
	public final <K> K trim(K o) {
		if (trimStrings && o instanceof String)
			return (K)o.toString().trim();
		return o;

	}

	/**
	 * Trims the specified string if {@link ParserContext#isTrimStrings()} returns <jk>true</jk>.
	 *
	 * @param s The input string to trim.
	 * @return The trimmed string, or <jk>null</jk> if the input was <jk>null</jk>.
	 */
	public final String trim(String s) {
		if (trimStrings && s != null)
			return s.trim();
		return s;
	}

	/**
	 * Perform cleanup on this context object if necessary.
	 *
	 * @throws ParseException
	 */
	public void close() throws ParseException {
		if (closed)
			throw new ParseException("Attempt to close ParserContext more than once.");
		if (debug && warnings.size() > 0)
			throw new ParseException("Warnings occurred during parsing: \n" + StringUtils.join(warnings, "\n"));
	}

	@Override /* Object */
	protected void finalize() throws Throwable {
		if (! closed)
			throw new RuntimeException("ParserContext was not closed.");
	}
}
