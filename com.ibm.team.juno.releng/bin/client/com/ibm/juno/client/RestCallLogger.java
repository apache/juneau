/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.client;

import java.io.*;
import java.text.*;
import java.util.logging.*;

import org.apache.http.*;
import org.apache.http.client.methods.*;
import org.apache.http.util.*;

/**
 * Specialized interceptor for logging calls to a log file.
 * <p>
 * Causes a log entry to be created that shows all the request and response headers and content
 * 	at the end of the request.
 * <p>
 * Use the {@link RestClient#logTo(Level, Logger)} and {@link RestCall#logTo(Level, Logger)}
 * <p>
 * methods to create instances of this class.
 */
public class RestCallLogger extends RestCallInterceptor {

	private Level level;
	private Logger log;

	/**
	 * Constructor.
	 *
	 * @param level The log level to log messages at.
	 * @param log The logger to log to.
	 */
	protected RestCallLogger(Level level, Logger log) {
		this.level = level;
		this.log = log;
	}

	@Override /* RestCallInterceptor */
	public void onInit(RestCall restCall) {
		if (log.isLoggable(level))
			restCall.captureResponse();
	}

	@Override /* RestCallInterceptor */
	public void onConnect(RestCall restCall, int statusCode, HttpRequest req, HttpResponse res) {
		// Do nothing.
	}

	@Override /* RestCallInterceptor */
	public void onRetry(RestCall restCall, int statusCode, HttpRequest req, HttpResponse res, Exception ex) {
		if (log.isLoggable(level)) {
			if (ex == null)
			log.log(level, MessageFormat.format("Call to {0} returned {1}.  Will retry.", req.getRequestLine().getUri(), statusCode)); //$NON-NLS-1$
			else
				log.log(level, MessageFormat.format("Call to {0} caused exception {1}.  Will retry.", req.getRequestLine().getUri(), ex.getLocalizedMessage()), ex); //$NON-NLS-1$
		}
	}

	@Override /* RestCallInterceptor */
	public void onClose(RestCall restCall) throws RestCallException {
		try {
			if (log.isLoggable(level)) {
				String output = restCall.getCapturedResponse();
				StringBuilder sb = new StringBuilder();
				HttpUriRequest req = restCall.getRequest();
				HttpResponse res = restCall.getResponse();
				if (req != null) {
					sb.append("\n=== HTTP Call ==================================================================");

					sb.append("\n=== REQUEST ===\n").append(req);
					sb.append("\n---request headers---");
					for (Header h : req.getAllHeaders())
						sb.append("\n").append(h);
					if (req instanceof HttpEntityEnclosingRequestBase) {
						sb.append("\n---request entity---");
						HttpEntityEnclosingRequestBase req2 = (HttpEntityEnclosingRequestBase)req;
						HttpEntity e = req2.getEntity();
						if (e == null)
							sb.append("\nEntity is null");
						else {
							if (e.getContentType() != null)
								sb.append("\n").append(e.getContentType());
							if (e.getContentEncoding() != null)
								sb.append("\n").append(e.getContentEncoding());
							if (e.isRepeatable()) {
								try {
									sb.append("\n---request content---\n").append(EntityUtils.toString(e));
								} catch (Exception ex) {
									throw new RuntimeException(ex);
								}
							}
						}
					}
				}
				if (res != null) {
					sb.append("\n=== RESPONSE ===\n").append(res.getStatusLine());
					sb.append("\n---response headers---");
					for (Header h : res.getAllHeaders())
						sb.append("\n").append(h);
					sb.append("\n---response content---\n").append(output);
					sb.append("\n=== END ========================================================================");
				}
				log.log(level, sb.toString());
			}
		} catch (IOException e) {
			log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
	}
}
