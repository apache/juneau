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
package org.apache.juneau.rest;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.servlet.http.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.utils.*;

/**
 * Various reusable utility methods.
 */
public final class RestUtils {

	/**
	 * Returns readable text for an HTTP response code.
	 *
	 * @param rc The HTTP response code.
	 * @return Readable text for an HTTP response code, or <jk>null</jk> if it's an invalid code.
	 */
	public static String getHttpResponseText(int rc) {
		return httpMsgs.get(rc);
	}

	private static Map<Integer,String> httpMsgs = new AMap<Integer,String>()
		.append(200, "OK")
		.append(201, "Created")
		.append(202, "Accepted")
		.append(203, "Non-Authoritative Information")
		.append(204, "No Content")
		.append(205, "Reset Content")
		.append(206, "Partial Content")
		.append(300, "Multiple Choices")
		.append(301, "Moved Permanently")
		.append(302, "Temporary Redirect")
		.append(303, "See Other")
		.append(304, "Not Modified")
		.append(305, "Use Proxy")
		.append(307, "Temporary Redirect")
		.append(400, "Bad Request")
		.append(401, "Unauthorized")
		.append(402, "Payment Required")
		.append(403, "Forbidden")
		.append(404, "Not Found")
		.append(405, "Method Not Allowed")
		.append(406, "Not Acceptable")
		.append(407, "Proxy Authentication Required")
		.append(408, "Request Time-Out")
		.append(409, "Conflict")
		.append(410, "Gone")
		.append(411, "Length Required")
		.append(412, "Precondition Failed")
		.append(413, "Request Entity Too Large")
		.append(414, "Request-URI Too Large")
		.append(415, "Unsupported Media Type")
		.append(500, "Internal Server Error")
		.append(501, "Not Implemented")
		.append(502, "Bad Gateway")
		.append(503, "Service Unavailable")
		.append(504, "Gateway Timeout")
		.append(505, "HTTP Version Not Supported")
	;

	/**
	 * Trims <js>'/'</js> characters from both the start and end of the specified string.
	 *
	 * @param s The string to trim.
	 * @return A new trimmed string, or the same string if no trimming was necessary.
	 */
	public static String trimSlashes(String s) {
		if (s == null)
			return null;
		while (StringUtils.endsWith(s, '/'))
			s = s.substring(0, s.length()-1);
		while (s.length() > 0 && s.charAt(0) == '/')
			s = s.substring(1);
		return s;
	}

	/**
	 * Trims <js>'/'</js> characters from the end of the specified string.
	 *
	 * @param s The string to trim.
	 * @return A new trimmed string, or the same string if no trimming was necessary.
	 */
	public static String trimTrailingSlashes(String s) {
		if (s == null)
			return null;
		while (StringUtils.endsWith(s, '/'))
			s = s.substring(0, s.length()-1);
		return s;
	}

	/**
	 * Trims <js>'/'</js> characters from the end of the specified string.
	 *
	 * @param s The string to trim.
	 * @return The same string buffer.
	 */
	public static StringBuffer trimTrailingSlashes(StringBuffer s) {
		if (s == null)
			return null;
		while (s.length() > 0 && s.charAt(s.length()-1) == '/')
			s.setLength(s.length()-1);
		return s;
	}

	/**
	 * Decodes a <code>application/x-www-form-urlencoded</code> string using <code>UTF-8</code> encoding scheme.
	 *
	 * @param s The string to decode.
	 * @return The decoded string, or <jk>null</jk> if input is <jk>null</jk>.
	 */
	public static String decode(String s) {
		if (s == null)
			return s;
		boolean needsDecode = false;
		for (int i = 0; i < s.length() && ! needsDecode; i++) {
			char c = s.charAt(i);
			if (c == '+' || c == '%')
				needsDecode = true;
		}
		if (needsDecode)
		try {
				return URLDecoder.decode(s, "UTF-8");
			} catch (UnsupportedEncodingException e) {/* Won't happen */}
		return s;
	}

	// Characters that do not need to be URL-encoded
	private static final AsciiSet unencodedChars = new AsciiSet("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_.!~*'()\\");

	/**
	 * Encodes a <code>application/x-www-form-urlencoded</code> string using <code>UTF-8</code> encoding scheme.
	 *
	 * @param s The string to encode.
	 * @return The encoded string, or <jk>null</jk> if input is <jk>null</jk>.
	 */
	public static String encode(String s) {
		if (s == null)
			return null;
		boolean needsEncode = false;
		for (int i = 0; i < s.length() && ! needsEncode; i++)
			needsEncode |= (! unencodedChars.contains(s.charAt(i)));
		if (needsEncode)
		try {
				return URLEncoder.encode(s, "UTF-8");
			} catch (UnsupportedEncodingException e) {/* Won't happen */}
		return s;
	}

	/**
	 * Identical to {@link HttpServletRequest#getPathInfo()} but doesn't decode encoded characters.
	 *
	 * @param req The HTTP request
	 * @return The undecoded path info.
	 */
	public static String getPathInfoUndecoded(HttpServletRequest req) {
		String requestURI = req.getRequestURI();
		String contextPath = req.getContextPath();
		String servletPath = req.getServletPath();
		int l = contextPath.length() + servletPath.length();
		if (requestURI.length() == l)
			return null;
		return requestURI.substring(l);
	}

	/**
	 * Efficiently trims the path info part from a request URI.
	 * <p>
	 * The result is the URI of the servlet itself.
	 *
	 * @param requestURI The value returned by {@link HttpServletRequest#getRequestURL()}
	 * @param contextPath The value returned by {@link HttpServletRequest#getContextPath()}
	 * @param servletPath The value returned by {@link HttpServletRequest#getServletPath()}
	 * @return The same StringBuilder with remainder trimmed.
	 */
	public static StringBuffer trimPathInfo(StringBuffer requestURI, String contextPath, String servletPath) {
		if (servletPath.equals("/"))
			servletPath = "";
		if (contextPath.equals("/"))
			contextPath = "";

		try {
			// Given URL:  http://hostname:port/servletPath/extra
			// We want:    http://hostname:port/servletPath
			int sc = 0;
			for (int i = 0; i < requestURI.length(); i++) {
				char c = requestURI.charAt(i);
				if (c == '/') {
					sc++;
					if (sc == 3) {
						if (servletPath.isEmpty()) {
							requestURI.setLength(i);
							return requestURI;
						}

						// Make sure context path follows the authority.
						for (int j = 0; j < contextPath.length(); i++, j++)
							if (requestURI.charAt(i) != contextPath.charAt(j))
								throw new Exception("case=1");

						// Make sure servlet path follows the authority.
						for (int j = 0; j < servletPath.length(); i++, j++)
							if (requestURI.charAt(i) != servletPath.charAt(j))
								throw new Exception("case=2");

						// Make sure servlet path isn't a false match (e.g. /foo2 should not match /foo)
						c = (requestURI.length() == i ? '/' : requestURI.charAt(i));
						if (c == '/' || c == '?') {
							requestURI.setLength(i);
							return requestURI;
						}

						throw new Exception("case=3");
					}
				} else if (c == '?') {
					if (sc != 2)
						throw new Exception("case=4");
					if (servletPath.isEmpty()) {
						requestURI.setLength(i);
						return requestURI;
					}
					throw new Exception("case=5");
				}
			}
			if (servletPath.isEmpty())
				return requestURI;
			throw new Exception("case=6");
		} catch (Exception e) {
			throw new RuntimeException("Could not find servlet path in request URI.  URI=["+requestURI+"], servletPath=["+servletPath+"]", e);
		}
	}

	static String[] parseHeader(String s) {
		int i = s.indexOf(':');
		if (i == -1)
			return null;
		String name = s.substring(0, i).trim().toLowerCase(Locale.ENGLISH);
		String val = s.substring(i+1).trim();
		return new String[]{name,val};
	}
}
