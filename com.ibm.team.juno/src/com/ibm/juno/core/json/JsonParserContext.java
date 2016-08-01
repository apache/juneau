/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2016. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.json;

import static com.ibm.juno.core.json.JsonParserProperties.*;

import java.io.*;
import java.lang.reflect.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.parser.*;

/**
 * Context object that lives for the duration of a single parsing of {@link JsonParser}.
 * <p>
 * 	This class is NOT thread safe.  It is meant to be discarded after one-time use.
 * </p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class JsonParserContext extends ParserContext {

	private final boolean strictMode;

	/**
	 * Create a new parser context with the specified options.
	 *
	 * @param beanContext The bean context being used.
	 * @param jpp The JSON parser properties.
	 * @param pp The default parser properties.
	 * @param op The override properties.
	 * @param javaMethod The java method that called this parser, usually the method in a REST servlet.
	 * @param outer The outer object for instantiating top-level non-static inner classes.
	 */
	public JsonParserContext(BeanContext beanContext, JsonParserProperties jpp, ParserProperties pp, ObjectMap op, Method javaMethod, Object outer) {
		super(beanContext, pp, op, javaMethod, outer);
		if (op == null || op.isEmpty()) {
			strictMode = jpp.isStrictMode();
		} else {
			strictMode = op.getBoolean(JSON_strictMode, jpp.isStrictMode());
		}
	}

	final boolean isStrictMode() {
		return strictMode;
	}

	/**
	 * Returns the reader associated with this context wrapped in a {@link ParserReader}.
	 *
	 * @param in The reader being wrapped.
	 * @param estimatedSize The estimated size of the input.
	 * @return The reader wrapped in a specialized parser reader.
	 */
	public ParserReader getReader(Reader in, int estimatedSize) {
		if (in instanceof ParserReader)
			return (ParserReader)in;
		return new ParserReader(in, Math.min(8096, estimatedSize));
	}
}
