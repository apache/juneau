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
package org.apache.juneau.rest.client2.logging;

import static org.apache.juneau.internal.StringUtils.*;

import java.text.*;

import org.apache.http.*;
import org.apache.http.util.*;
import org.apache.juneau.rest.client2.*;

/**
 * Specialized intercepter for logging calls to a log file.
 */
public abstract class RestCallLogger implements RestCallInterceptor {

	/**
	 * Returns <jk>true</jk> if the specified request/response should be logged.
	 *
	 * @param req The request.  Can be <jk>null</jk>.
	 * @param res The response.  Can be <jk>null</jk>.
	 * @return <jk>true</jk> if the specified request/response should be logged.
	 */
	public abstract boolean shouldLog(RestRequest req, RestResponse res);

	/**
	 * Logs a message.
	 *
	 * @param t Thrown exception.  Can be <jk>null</jk>.
	 * @param msg The message.
	 */
	public abstract void log(Throwable t, String msg);

	/**
	 * Logs a message.
	 *
	 * @param msg The message with {@link MessageFormat}-style arguments.
	 * @param args The arguments.
	 */
	protected final void log(String msg, Object...args) {
		log(null, format(msg, args));
	}

	/**
	 * Logs a message.
	 *
	 * @param t Thrown exception.  Can be <jk>null</jk>.
	 * @param msg The message with {@link MessageFormat}-style arguments.
	 * @param args The arguments.
	 */
	protected final void log(Throwable t, String msg, Object...args) {
		log(t, format(msg, args));
	}

	@Override /* RestCallInterceptor */
	public void onInit(RestRequest req) {
	}

	@Override /* RestCallInterceptor */
	public void onConnect(RestRequest req, RestResponse res) {
		if (shouldLog(req, null))
			res.getBody().cache();
	}

	@Override /* RestCallInterceptor */
	public void onClose(RestRequest req, RestResponse res) throws Exception {
		if (shouldLog(req, res)) {
			String output = res == null ? null : res.getBody().asString();
			StringBuilder sb = new StringBuilder();
			if (req != null) {
				sb.append("\n=== HTTP Call (outgoing) ======================================================");

				sb.append("\n=== REQUEST ===\n");
				sb.append(req.getMethod()).append(" ").append(req.getURI());
				sb.append("\n---request headers---");
				for (Header h : req.getAllHeaders())
					sb.append("\n\t").append(h);
				if (req.hasHttpEntity()) {
					sb.append("\n---request entity---");
					HttpEntity e = req.getHttpEntity();
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
				sb.append("\n=== END =======================================================================");
			}
			log(sb.toString());
		}
	}
}
