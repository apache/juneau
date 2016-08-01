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
 * Used to determine whether a request should be retried based on the HTTP response code.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public interface RetryOn {

	/**
	 * Default RetryOn that returns <jk>true</jk> of any HTTP response &gt;= 400 is received.
	 */
	public static final RetryOn DEFAULT = new RetryOn() {
		@Override /* RetryOn */
		public boolean onCode(int httpResponseCode) {
			return httpResponseCode <= 0 || httpResponseCode >= 400;
		}
	};

	/**
	 * Subclasses should override this method to determine whether the HTTP response is retryable.
	 *
	 * @param httpResponseCode The HTTP response code.
	 * @return <jk>true</jk> if the specified response code is retryable.
	 */
	boolean onCode(int httpResponseCode);
}
