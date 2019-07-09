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
package org.apache.juneau.rest.annotation;

import org.apache.juneau.rest.*;

/**
 * Represents a single logging rule for how to handle logging of HTTP requests/responses.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jf'>{@link RestContext#REST_logRules}
 * 	<li class='jf'>{@link RestMethodContext#RESTMETHOD_logRules}
 * </ul>
 */
@SuppressWarnings("javadoc")
public @interface LogRule {

	/**
	 * Sets the bean filters for the serializers and parsers defined on this method.
	 *
	 * <p>
	 * If no value is specified, the bean filters are inherited from the class.
	 * <br>Otherwise, this value overrides the bean filters defined on the class.
	 *
	 * <p>
	 * Use {@link Inherit} to inherit bean filters defined on the class.
	 *
	 * <p>
	 * Use {@link None} to suppress inheriting bean filters defined on the class.
	 */
	public String codes() default "";
	public String exceptions() default "";
	public String debugOnly() default "false";
	public String logLevel() default "INFO";

	public String req() default "SHORT";
	public String res() default "SHORT";
	public String verbose() default "false";
//
//
//	sb.append("\n=== HTTP Request (incoming) ====================================================");
//	sb.append("\n").append(method).append(" ").append(req.getRequestURI()).append((qs == null ? "" : "?" + qs));
//	sb.append("\n\tResponse code: ").append(res.getStatus());
//	if (execTime != null)
//		sb.append("\n\tExec time: ").append(res.getStatus()).append("ms");
//	if (reqBody != null)
//		sb.append("\n\tReq body: ").append(reqBody.length).append(" bytes");
//	if (resBody != null)
//		sb.append("\n\tRes body: ").append(resBody.length).append(" bytes");
//	sb.append("\n---Request Headers---");
//	for (Enumeration<String> hh = req.getHeaderNames(); hh.hasMoreElements();) {
//		String h = hh.nextElement();
//		sb.append("\n\t").append(h).append(": ").append(req.getHeader(h));
//	}
//	if (context != null && ! context.getDefaultRequestHeaders().isEmpty()) {
//		sb.append("\n---Default Servlet Headers---");
//		for (Map.Entry<String,Object> h : context.getDefaultRequestHeaders().entrySet()) {
//			sb.append("\n\t").append(h.getKey()).append(": ").append(h.getValue());
//		}
//	}
//	if (reqBody != null && reqBody.length > 0) {
//		try {
//			sb.append("\n---Request Body UTF-8---");
//			sb.append("\n").append(new String(reqBody, IOUtils.UTF8));
//			sb.append("\n---Request Body Hex---");
//			sb.append("\n").append(toSpacedHex(reqBody));
//		} catch (Exception e1) {
//			sb.append("\n").append(e1.getLocalizedMessage());
//		}
//	}
//	sb.append("\n---Response Headers---");
//	for (String h : res.getHeaderNames()) {
//		sb.append("\n\t").append(h).append(": ").append(res.getHeader(h));
//	}
//	if (resBody != null && resBody.length > 0) {
//		try {
//			sb.append("\n---Response Body UTF-8---");
//			sb.append("\n").append(new String(resBody, IOUtils.UTF8));
//			sb.append("\n---Response Body Hex---");
//			sb.append("\n").append(toSpacedHex(resBody));
//		} catch (Exception e1) {
//			sb.append(e1.getLocalizedMessage());
//		}
//	}
//	if (e != null) {
//		sb.append("\n---Exception---");
//		sb.append("\n").append(getStackTrace(e));
//	}
//	sb.append("\n=== END ========================================================================");

}
