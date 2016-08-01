/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.server.matchers;

import com.ibm.juno.server.*;

/**
 * Predefined matcher for matching requests with content type <js>"multipart/form-data"</js>.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class MultipartFormDataMatcher extends RestMatcher {
	@Override /* RestMatcher */
	public boolean matches(RestRequest req) {
		String contentType = req.getContentType();
		return contentType != null && contentType.startsWith("multipart/form-data"); //$NON-NLS-1$
	}
}
