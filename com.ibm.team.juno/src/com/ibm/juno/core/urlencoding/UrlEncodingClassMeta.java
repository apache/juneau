/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.urlencoding;

import com.ibm.juno.core.urlencoding.annotation.*;
import com.ibm.juno.core.utils.*;

/**
 * Metadata on classes specific to the URL-Encoding serializers and parsers pulled from the {@link UrlEncoding @UrlEncoding} annotation on the class.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class UrlEncodingClassMeta {

	private final UrlEncoding urlEncoding;
	private final boolean expandedParams;

	/**
	 * Constructor.
	 *
	 * @param c The class that this annotation is defined on.
	 */
	public UrlEncodingClassMeta(Class<?> c) {
		this.urlEncoding = ReflectionUtils.getAnnotation(UrlEncoding.class, c);
		if (urlEncoding != null) {
			expandedParams = urlEncoding.expandedParams();
		} else {
			expandedParams = false;
		}
	}

	/**
	 * Returns the {@link UrlEncoding} annotation defined on the class.
	 *
	 * @return The value of the {@link UrlEncoding} annotation, or <jk>null</jk> if annotation is not specified.
	 */
	protected UrlEncoding getAnnotation() {
		return urlEncoding;
	}

	/**
	 * Returns the {@link UrlEncoding#expandedParams()} annotation defined on the class.
	 *
	 * @return The value of the {@link UrlEncoding#expandedParams()} annotation.
	 */
	protected boolean isExpandedParams() {
		return expandedParams;
	}
}
