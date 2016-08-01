/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.parser;

import java.lang.reflect.*;
import java.util.*;
import java.util.logging.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.core.utils.*;

/**
 * Context object that lives for the duration of a single parsing of {@link Parser} and its subclasses.
 * <p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class ParserContext {

	private static Logger logger = Logger.getLogger(ParserContext.class.getName());

	private boolean debug, closed;
	private final BeanContext beanContext;
	private final List<String> warnings = new LinkedList<String>();

	private ObjectMap properties;
	private Method javaMethod;
	private Object outer;

	/**
	 * Create a new parser context with the specified options.
	 *
	 * @param beanContext The bean context being used.
	 * @param pp The default parser properties.
	 * @param properties The override properties.
	 * @param javaMethod The java method that called this parser, usually the method in a REST servlet.
	 * @param outer The outer object for instantiating top-level non-static inner classes.
	 */
	public ParserContext(BeanContext beanContext, ParserProperties pp, ObjectMap properties, Method javaMethod, Object outer) {
		this.debug = pp.debug;
		this.beanContext = beanContext;
		this.properties = properties;
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
	 * Returns the {@link SerializerProperties#SERIALIZER_debug} setting value in this context.
	 *
	 * @return The {@link SerializerProperties#SERIALIZER_debug} setting value in this context.
	 */
	public final boolean isDebug() {
		return debug;
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
		msg = args.length == 0 ? msg : String.format(msg, args);
		logger.warning(msg);
		warnings.add(warnings.size() + 1 + ": " + msg);
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
