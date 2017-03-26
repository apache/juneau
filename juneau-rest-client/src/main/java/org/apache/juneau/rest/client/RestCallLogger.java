// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.rest.client;

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
 * Use the {@link RestClientBuilder#logTo(Level, Logger)} and {@link RestCall#logTo(Level, Logger)}
 * <p>
 * methods to create instances of this class.
 */
public class RestCallLogger extends RestCallInterceptor {

	/**
	 * Default HTTP request logger.
	 * Logs outgoing HTTP requests to the <code>org.apache.juneau.rest.client</code> logger at <jsf>WARNING</jsf> level.
	 */
	public static final RestCallLogger DEFAULT = new RestCallLogger(Level.WARNING, Logger.getLogger("org.apache.juneau.rest.client"));

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
					sb.append("\n=== HTTP Call (outgoing) =======================================================");

					sb.append("\n=== REQUEST ===\n").append(req);
					sb.append("\n---request headers---");
					for (Header h : req.getAllHeaders())
						sb.append("\n\t").append(h);
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
						sb.append("\n\t").append(h);
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
