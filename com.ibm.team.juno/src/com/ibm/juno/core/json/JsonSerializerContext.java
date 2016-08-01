/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2016. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.json;

import static com.ibm.juno.core.json.JsonSerializerProperties.*;

import java.io.*;
import java.lang.reflect.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.serializer.*;

/**
 * Context object that lives for the duration of a single serialization of {@link JsonSerializer} and its subclasses.
 * <p>
 * 	See {@link SerializerContext} for details.
 * </p>
 * <p>
 * 	This class is NOT thread safe.  It is meant to be discarded after one-time use.
 * </p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class JsonSerializerContext extends SerializerContext {

	private final boolean simpleMode, useWhitespace, escapeSolidus;

	/**
	 * Constructor.
	 * @param beanContext The bean context being used by the serializer.
	 * @param sp Default general serializer properties.
	 * @param jsp Default JSON serializer properties.
	 * @param op Override properties.
	 * @param javaMethod Java method that invoked this serializer.
	 * 	When using the REST API, this is the Java method invoked by the REST call.
	 * 	Can be used to access annotations defined on the method or class.
	 */
	protected JsonSerializerContext(BeanContext beanContext, SerializerProperties sp, JsonSerializerProperties jsp, ObjectMap op, Method javaMethod) {
		super(beanContext, sp, op, javaMethod);
		if (op == null || op.isEmpty()) {
			simpleMode = jsp.simpleMode;
			useWhitespace = jsp.useWhitespace;
			escapeSolidus = jsp.escapeSolidus;
		} else {
			simpleMode = op.getBoolean(JSON_simpleMode, jsp.simpleMode);
			useWhitespace = op.getBoolean(JSON_useWhitespace, jsp.useWhitespace);
			escapeSolidus = op.getBoolean(JSON_escapeSolidus, jsp.escapeSolidus);
		}
	}

	final boolean isSimpleMode() {
		return simpleMode;
	}

	final boolean isUseWhitespace() {
		return useWhitespace;
	}

	final boolean isEscapeSolidus() {
		return escapeSolidus;
	}

	/**
	 * Wraps the specified writer inside a {@link JsonSerializerWriter}.
	 *
	 * @param out The writer being wrapped.
	 * @return The wrapped writer.
	 */
	public JsonSerializerWriter getWriter(Writer out) {
		if (out instanceof JsonSerializerWriter)
			return (JsonSerializerWriter)out;
		return new JsonSerializerWriter(out, isUseIndentation(), isUseWhitespace(), isEscapeSolidus(), getQuoteChar(), isSimpleMode(), isTrimStrings(), getRelativeUriBase(), getAbsolutePathUriBase());
	}
}
