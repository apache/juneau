/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.client;

import org.apache.http.*;

/**
 * Used to intercept http connection responses to allow modification of that response before processing
 * and for listening for call lifecycle events.
 * <p>
 * Useful if you want to prevent {@link RestCallException RestCallExceptions} from being thrown on error conditions.
 */
public abstract class RestCallInterceptor {

	/**
	 * Called when {@link RestCall} object is created.
	 *
	 * @param restCall The restCall object invoking this method.
	 */
	public void onInit(RestCall restCall) {}

	/**
	 * Called immediately after an HTTP response has been received.
	 *
	 * @param statusCode The HTTP status code received.
	 * @param restCall The restCall object invoking this method.
	 * @param req The HTTP request object.
	 * @param res The HTTP response object.
	 */
	public void onConnect(RestCall restCall, int statusCode, HttpRequest req, HttpResponse res) {}

	/**
	 * Called if retry is going to be attempted.
	 *
	 * @param statusCode The HTTP status code received.
	 * @param restCall The restCall object invoking this method.
	 * @param req The HTTP request object.
	 * @param res The HTTP response object.
	 * @param ex The exception thrown from the client.
	 */
	public void onRetry(RestCall restCall, int statusCode, HttpRequest req, HttpResponse res, Exception ex) {}

	/**
	 * Called when {@link RestCall#close()} is called.
	 *
	 * @param restCall The restCall object invoking this method.
	 * @throws RestCallException
	 */
	public void onClose(RestCall restCall) throws RestCallException {}
}
