/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.client;

/**
 * Enumeration of HTTP methods.
 * <p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public enum HttpMethod {

	/** HTTP GET */
	GET(false),

	/** HTTP PUT */
	PUT(true),

	/** HTTP POST */
	POST(true),

	/** HTTP DELETE */
	DELETE(false),

	/** HTTP OPTIONS */
	OPTIONS(false),

	/** HTTP HEAD */
	HEAD(false),

	/** HTTP TRACE */
	TRACE(false),

	/** HTTP CONNECT */
	CONNECT(false),

	/** HTTP MOVE */
	MOVE(false);

	private boolean hasContent;

	HttpMethod(boolean hasContent) {
		this.hasContent = hasContent;
	}

	/**
	 * Returns whether this HTTP method normally has content.
	 *
	 * @return <jk>true</jk> if this HTTP method normally has content.
	 */
	public boolean hasContent() {
		return hasContent;
	}
}
