/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.urlencoding;

import static com.ibm.juno.core.urlencoding.UonSerializerProperties.*;
import static com.ibm.juno.core.urlencoding.UrlEncodingProperties.*;

import java.io.*;
import java.lang.reflect.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.serializer.*;

/**
 * Context object that lives for the duration of a single serialization of {@link UonSerializer} and {@link UrlEncodingSerializer}.
 * <p>
 * 	See {@link SerializerContext} for details.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class UonSerializerContext extends SerializerContext {

	boolean simpleMode, useWhitespace, encodeChars, expandedParams;

	/**
	 * Constructor.
	 *
	 * @param beanContext The bean context being used by the serializer.
	 * @param sp Default general serializer properties.
	 * @param usp Default UON serializer properties.
	 * @param uep Default URL-Encoding properties.
	 * @param op Override properties.
	 * @param javaMethod Java method that invoked this serializer.
	 * 	When using the REST API, this is the Java method invoked by the REST call.
	 * 	Can be used to access annotations defined on the method or class.
	 */
	protected UonSerializerContext(BeanContext beanContext, SerializerProperties sp, UonSerializerProperties usp, UrlEncodingProperties uep, ObjectMap op, Method javaMethod) {
		super(beanContext, sp, op, javaMethod);
		if (op == null || op.isEmpty()) {
			simpleMode = usp.simpleMode;
			useWhitespace = usp.useWhitespace;
			encodeChars = usp.encodeChars;
			expandedParams = uep.expandedParams;
		} else {
			simpleMode = op.getBoolean(UON_simpleMode, usp.simpleMode);
			useWhitespace = op.getBoolean(UON_useWhitespace, usp.useWhitespace);
			encodeChars = op.getBoolean(UON_encodeChars, usp.encodeChars);
			expandedParams = op.getBoolean(URLENC_expandedParams, uep.expandedParams);

		}
	}

	/**
	 * Returns the {@link UonSerializerProperties#UON_simpleMode} setting value in this context.
	 *
	 * @return The {@link UonSerializerProperties#UON_simpleMode} setting value in this context.
	 */
	public final boolean isSimpleMode() {
		return simpleMode;
	}

	/**
	 * Returns the {@link UonSerializerProperties#UON_encodeChars} setting value in this context.
	 *
	 * @return The {@link UonSerializerProperties#UON_encodeChars} setting value in this context.
	 */
	public final boolean isEncodeChars() {
		return encodeChars;
	}

	/**
	 * Returns the {@link UrlEncodingProperties#URLENC_expandedParams} setting value in this context.
	 *
	 * @return The {@link UrlEncodingProperties#URLENC_expandedParams} setting value in this context.
	 */
	public final boolean isExpandedParams() {
		return expandedParams;
	}

	/**
	 * Wraps the specified writer in a {@link UonSerializerWriter}.
	 *
	 * @param out The writer to wrap.
	 * @return The wrapped writer.
	 */
	protected UonSerializerWriter getWriter(Writer out) {
		if (out instanceof UonSerializerWriter)
			return (UonSerializerWriter)out;
		return new UonSerializerWriter(out, useWhitespace, isSimpleMode(), isEncodeChars(), getRelativeUriBase(), getAbsolutePathUriBase());
	}
}
