/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2016. All Rights Reserved.
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
 * 	This class is NOT thread safe.  It is meant to be discarded after one-time use.
 * </p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class UonParserContext extends ParserContext {

	private final boolean decodeChars, whitespaceAware, expandedParams;

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
	 * Create a specialized parser context for parsing URL parameters.
	 * <p>
	 * 	The main difference is that characters are never decoded, and the {@link UonParserProperties#UON_decodeChars} property is always ignored.
	 * </p>
	 * @param beanContext The bean context being used.
	 * @param pp The default parser properties.
	 * @param upp The default UON-Encoding properties.
	 * @param uep The default URL-Encoding properties.
	 */
	public UonParserContext(BeanContext beanContext, ParserProperties pp, UonParserProperties upp, UrlEncodingProperties uep) {
		super(beanContext, pp, null, null, null);
		decodeChars = false;
		whitespaceAware = upp.whitespaceAware;
		expandedParams = uep.expandedParams;
	}

	final boolean isExpandedParams() {
		return expandedParams;
	}

	final boolean isDecodeChars() {
		return decodeChars;
	}

	final boolean isWhitespaceAware() {
		return whitespaceAware;
	}

	/**
	 * Wraps the specified reader in a {@link UonParserReader}.
	 *
	 * @param r The reader to wrap.
	 * @param estimatedSize The estimated size of the input.
	 * @return The wrapped reader.
	 */
	final UonParserReader getUrlEncodingParserReader(Reader r, int estimatedSize) {
		if (r instanceof UonParserReader)
			return (UonParserReader)r;
		return new UonParserReader(r, Math.min(8096, estimatedSize), decodeChars);
	}

	/**
	 * Returns true if the specified bean property should be expanded as multiple key-value pairs.
	 */
	final boolean shouldUseExpandedParams(BeanPropertyMeta<?> pMeta) {
		ClassMeta<?> cm = pMeta.getClassMeta();
		if (cm.isArray() || cm.isCollection()) {
			if (expandedParams)
				return true;
			if (pMeta.getBeanMeta().getClassMeta().getUrlEncodingMeta().isExpandedParams())
				return true;
		}
		return false;
	}
}
