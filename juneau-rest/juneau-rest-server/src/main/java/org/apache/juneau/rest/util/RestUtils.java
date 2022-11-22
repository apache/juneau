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
package org.apache.juneau.rest.util;

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.common.internal.ThrowableUtils.*;
import static org.apache.juneau.internal.ArrayUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.uon.*;

/**
 * Various reusable utility methods.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
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

	private static Map<Integer,String> httpMsgs = mapBuilder(Integer.class, String.class)
		.add(100, "Continue")
		.add(101, "Switching Protocols")
		.add(102, "Processing")
		.add(103, "Early Hints")
		.add(200, "OK")
		.add(201, "Created")
		.add(202, "Accepted")
		.add(203, "Non-Authoritative Information")
		.add(204, "No Content")
		.add(205, "Reset Content")
		.add(206, "Partial Content")
		.add(300, "Multiple Choices")
		.add(301, "Moved Permanently")
		.add(302, "Temporary Redirect")
		.add(303, "See Other")
		.add(304, "Not Modified")
		.add(305, "Use Proxy")
		.add(307, "Temporary Redirect")
		.add(400, "Bad Request")
		.add(401, "Unauthorized")
		.add(402, "Payment Required")
		.add(403, "Forbidden")
		.add(404, "Not Found")
		.add(405, "Method Not Allowed")
		.add(406, "Not Acceptable")
		.add(407, "Proxy Authentication Required")
		.add(408, "Request Time-Out")
		.add(409, "Conflict")
		.add(410, "Gone")
		.add(411, "Length Required")
		.add(412, "Precondition Failed")
		.add(413, "Request Entity Too Large")
		.add(414, "Request-URI Too Large")
		.add(415, "Unsupported Media Type")
		.add(500, "Internal Server Error")
		.add(501, "Not Implemented")
		.add(502, "Bad Gateway")
		.add(503, "Service Unavailable")
		.add(504, "Gateway Timeout")
		.add(505, "HTTP Version Not Supported")
		.build()
	;

	/**
	 * Identical to {@link HttpServletRequest#getPathInfo()} but doesn't decode encoded characters.
	 *
	 * @param req The HTTP request
	 * @return The un-decoded path info.
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
	 *
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
			throw new BasicRuntimeException(e, "Could not find servlet path in request URI.  URI=''{0}'', servletPath=''{1}''", requestURI, servletPath);
		}
	}

	/**
	 * Parses HTTP header.
	 *
	 * @param s The string to parse.
	 * @return The parsed string.
	 */
	public static String[] parseHeader(String s) {
		int i = s.indexOf(':');
		if (i == -1)
			i = s.indexOf('=');
		if (i == -1)
			return null;
		String name = s.substring(0, i).trim().toLowerCase(Locale.ENGLISH);
		String val = s.substring(i+1).trim();
		return new String[]{name,val};
	}

	/**
	 * Parses key/value pairs separated by either : or =
	 *
	 * @param s The string to parse.
	 * @return The parsed string.
	 */
	public static String[] parseKeyValuePair(String s) {
		int i = -1;
		for (int j = 0; j < s.length() && i < 0; j++) {
			char c = s.charAt(j);
			if (c == '=' || c == ':')
				i = j;
		}
		if (i == -1)
			return null;
		String name = s.substring(0, i).trim();
		String val = s.substring(i+1).trim();
		return new String[]{name,val};
	}

	static String resolveNewlineSeparatedAnnotation(String[] value, String fromParent) {
		if (value.length == 0)
			return fromParent;

		List<String> l = list();
		for (String v : value) {
			if (! "INHERIT".equals(v))
				l.add(v);
			else if (fromParent != null)
				l.add(fromParent);
		}
		return join(l, '\n');
	}

	private static final Pattern INDEXED_LINK_PATTERN = Pattern.compile("(?s)(\\S*)\\[(\\d+)\\]\\:(.*)");

	static String[] resolveLinks(String[] links, String[] parentLinks) {
		if (links.length == 0)
			return parentLinks;

		List<String> list = list();
		for (String l : links) {
			if ("INHERIT".equals(l))
				addAll(list, parentLinks);
			else if (l.indexOf('[') != -1 && INDEXED_LINK_PATTERN.matcher(l).matches()) {
				Matcher lm = INDEXED_LINK_PATTERN.matcher(l);
				lm.matches();
				String key = lm.group(1);
				int index = Math.min(list.size(), Integer.parseInt(lm.group(2)));
				String remainder = lm.group(3);
				list.add(index, key.isEmpty() ? remainder : key + ":" + remainder);
			} else {
				list.add(l);
			}
		}
		return array(list, String.class);
	}

	static String[] resolveContent(String[] content, String[] parentContent) {
		if (content.length == 0)
			return parentContent;

		List<String> list = list();
		for (String l : content) {
			if ("INHERIT".equals(l)) {
				addAll(list, parentContent);
			} else if ("NONE".equals(l)) {
				return new String[0];
			} else {
				list.add(l);
			}
		}
		return array(list, String.class);
	}

	/**
	 * Parses a URL query string or form-data content.
	 *
	 * @param qs A reader or string containing the query string to parse.
	 * @return A new map containing the parsed query.
	 */
	public static Map<String,String[]> parseQuery(Object qs) {
		return parseQuery(qs, null);
	}

	/**
	 * Same as {@link #parseQuery(Object)} but allows you to specify the map to insert values into.
	 *
	 * @param qs A reader containing the query string to parse.
	 * @param map The map to pass the values into.
	 * @return The same map passed in, or a new map if it was <jk>null</jk>.
	 */
	public static Map<String,String[]> parseQuery(Object qs, Map<String,String[]> map) {

		try {
			Map<String,String[]> m = map;
			if (m == null)
				m = map();

			if (qs == null || ((qs instanceof CharSequence) && isEmpty(stringify(qs))))
				return m;

			try (ParserPipe p = new ParserPipe(qs)) {

				final int S1=1; // Looking for attrName start.
				final int S2=2; // Found attrName start, looking for = or & or end.
				final int S3=3; // Found =, looking for valStart or &.
				final int S4=4; // Found valStart, looking for & or end.

				try (UonReader r = new UonReader(p, true)) {
					int c = r.peekSkipWs();
					if (c == '?')
						r.read();

					int state = S1;
					String currAttr = null;
					while (c != -1) {
						c = r.read();
						if (state == S1) {
							if (c != -1) {
								r.unread();
								r.mark();
								state = S2;
							}
						} else if (state == S2) {
							if (c == -1) {
								add(m, r.getMarked(), null);
							} else if (c == '\u0001') {
								m.put(r.getMarked(0,-1), null);
								state = S1;
							} else if (c == '\u0002') {
								currAttr = r.getMarked(0,-1);
								state = S3;
							}
						} else if (state == S3) {
							if (c == -1 || c == '\u0001') {
								add(m, currAttr, "");
								state = S1;
							} else {
								if (c == '\u0002')
									r.replace('=');
								r.unread();
								r.mark();
								state = S4;
							}
						} else if (state == S4) {
							if (c == -1) {
								add(m, currAttr, r.getMarked());
							} else if (c == '\u0001') {
								add(m, currAttr, r.getMarked(0,-1));
								state = S1;
							} else if (c == '\u0002') {
								r.replace('=');
							}
						}
					}
				}

				return m;
			}
		} catch (IOException e) {
			throw asRuntimeException(e); // Should never happen.
		}
	}

	private static void add(Map<String,String[]> m, String key, String val) {
		boolean b = m.containsKey(key);
		if (val == null) {
			if (! b)
				m.put(key, null);
		} else if (b && m.get(key) != null) {
			m.put(key, append(m.get(key), val));
		} else {
			m.put(key, new String[]{val});
		}
	}

	/**
	 * Parses a string that can consist of a simple string or JSON object/array.
	 *
	 * @param s The string to parse.
	 * @return The parsed value, or <jk>null</jk> if the input is null.
	 * @throws ParseException Invalid JSON in string.
	 */
	public static Object parseAnything(String s) throws ParseException {
		if (isJson(s))
			return JsonParser.DEFAULT.parse(s, Object.class);
		return s;
	}

	/**
	 * If the specified path-info starts with the specified context path, trims the context path from the path info.
	 *
	 * @param contextPath The context path.
	 * @param path The URL path.
	 * @return The path following the context path, or the original path.
	 */
	public static String trimContextPath(String contextPath, String path) {
		if (path == null)
			return null;
		if (path.length() == 0 || path.equals("/") || contextPath.length() == 0 || contextPath.equals("/"))
			return path;
		String op = path;
		if (path.charAt(0) == '/')
			path = path.substring(1);
		if (contextPath.charAt(0) == '/')
			contextPath = contextPath.substring(1);
		if (path.startsWith(contextPath)) {
			if (path.length() == contextPath.length())
				return "/";
			path = path.substring(contextPath.length());
			if (path.isEmpty() || path.charAt(0) == '/')
				return path;
		}
		return op;
	}

	/**
	 * Normalizes the {@link RestOp#path()} value.
	 *
	 * @param path The path to normalize.
	 * @return The normalized path.
	 */
	public static String fixMethodPath(String path) {
		if (path == null)
			return null;
		if (path.equals("/"))
			return path;
		return trimTrailingSlashes(path);
	}

	/**
	 * Returns <jk>true</jk> if the specified value is a valid context path.
	 *
	 * The path must start with a "/" character but not end with a "/" character.
	 * For servlets in the default (root) context, the value should be "".
	 *
	 * @param value The value to test.
	 * @return <jk>true</jk> if the specified value is a valid context path.
	 */
	public static boolean isValidContextPath(String value) {
		if (value == null)
			return false;
		if (value.isEmpty())
			return true;
		if (value.charAt(value.length()-1) == '/' || value.charAt(0) != '/')
			return false;
		return true;
	}

	/**
	 * Converts the specified path segment to a valid context path.
	 *
	 * <ul>
	 * 	<li><jk>nulls</jk> and <js>"/"</js> are converted to empty strings.
	 * 	<li>Trailing slashes are trimmed.
	 * 	<li>Leading slash is added if needed.
	 * </ul>
	 *
	 * @param s The value to convert.
	 * @return The converted path.
	 */
	public static String toValidContextPath(String s) {
		if (s == null || s.isEmpty())
			return "";
		s = trimTrailingSlashes(s);
		if (s.isEmpty())
			return s;
		if (s.charAt(0) != '/')
			s = '/' + s;
		return s;
	}

	/**
	 * Throws a {@link RuntimeException} if the method {@link #isValidContextPath(String)} returns <jk>false</jk> for the specified value.
	 *
	 * @param value The value to test.
	 */
	public static void validateContextPath(String value) {
		if (! isValidContextPath(value))
			throw new BasicRuntimeException("Value is not a valid context path: [{0}]", value);
	}

	/**
	 * Returns <jk>true</jk> if the specified value is a valid servlet path.
	 *
	 * This path must with a "/" character and includes either the servlet name or a path to the servlet,
	 * but does not include any extra path information or a query string.
	 * Should be an empty string ("") if the servlet used to process this request was matched using the "/*" pattern.
	 *
	 * @param value The value to test.
	 * @return <jk>true</jk> if the specified value is a valid servlet path.
	 */
	public static boolean isValidServletPath(String value) {
		if (value == null)
			return false;
		if (value.isEmpty())
			return true;
		if (value.equals("/") || value.charAt(value.length()-1) == '/' || value.charAt(0) != '/')
			return false;
		return true;
	}

	/**
	 * Throws a {@link RuntimeException} if the method {@link #isValidServletPath(String)} returns <jk>false</jk> for the specified value.
	 *
	 * @param value The value to test.
	 */
	public static void validateServletPath(String value) {
		if (! isValidServletPath(value))
			throw new BasicRuntimeException("Value is not a valid servlet path: [{0}]", value);
	}

	/**
	 * Returns <jk>true</jk> if the specified value is a valid path-info path.
	 *
	 * The extra path information follows the servlet path but precedes the query string and will start with a "/" character.
	 * The value should be null if there was no extra path information.
	 *
	 * @param value The value to test.
	 * @return <jk>true</jk> if the specified value is a valid path-info path.
	 */
	public static boolean isValidPathInfo(String value) {
		if (value == null)
			return true;
		if (value.isEmpty() || value.charAt(0) != '/')
			return false;
		return true;
	}

	/**
	 * Throws a {@link RuntimeException} if the method {@link #isValidPathInfo(String)} returns <jk>false</jk> for the specified value.
	 *
	 * @param value The value to test.
	 */
	public static void validatePathInfo(String value) {
		if (! isValidPathInfo(value))
			throw new BasicRuntimeException("Value is not a valid path-info path: [{0}]", value);
	}
}
