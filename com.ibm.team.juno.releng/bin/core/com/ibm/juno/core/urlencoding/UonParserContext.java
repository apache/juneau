/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.urlencoding;

import static com.ibm.juno.core.urlencoding.UonParserProperties.*;
import static com.ibm.juno.core.urlencoding.UrlEncodingProperties.*;

import java.io.*;
import java.lang.reflect.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.parser.*;

/**
 * Context object that lives for the duration of a single parsing in {@link UonParser} and {@link UrlEncodingParser}.
 * <p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class UonParserContext extends ParserContext {

	boolean decodeChars, whitespaceAware, expandedParams;

	/**
	 * Create a new parser context with the specified options.
	 *
	 * @param beanContext The bean context being used.
	 * @param pp The default parser properties.
	 * @param upp The default UON-Encoding properties.
	 * @param uep The default URL-Encoding properties.
	 * @param op The override properties.
	 * @param javaMethod The java method that called this parser, usually the method in a REST servlet.
	 * @param outer The outer object for instantiating top-level non-static inner classes.
	 */
	public UonParserContext(BeanContext beanContext, ParserProperties pp, UonParserProperties upp, UrlEncodingProperties uep, ObjectMap op, Method javaMethod, Object outer) {
		super(beanContext, pp, op, javaMethod, outer);
		if (op == null || op.isEmpty()) {
			decodeChars = upp.decodeChars;
			whitespaceAware = upp.whitespaceAware;
			expandedParams = uep.expandedParams;
		} else {
			decodeChars = op.getBoolean(UON_decodeChars, upp.decodeChars);
			whitespaceAware = op.getBoolean(UON_whitespaceAware, upp.whitespaceAware);
			expandedParams = op.getBoolean(URLENC_expandedParams, uep.expandedParams);
		}
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
	 * Wraps the specified reader in a {@link UonParserReader}.
	 *
	 * @param r The reader to wrap.
	 * @param estimatedSize The estimated size of the input.
	 * @return The wrapped reader.
	 */
	public final UonParserReader getUrlEncodingParserReader(Reader r, int estimatedSize) {
		if (r instanceof UonParserReader)
			return (UonParserReader)r;
		return new UonParserReader(r, Math.min(8096, estimatedSize), decodeChars);
	}
}
