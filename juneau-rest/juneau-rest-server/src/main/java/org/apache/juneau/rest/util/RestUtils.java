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

import static org.apache.juneau.internal.ArrayUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.util.*;
import java.util.regex.*;

import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.uon.*;
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
		.append(100, "Continue")
		.append(101, "Switching Protocols")
		.append(102, "Processing")
		.append(103, "Early Hints")
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
			throw new FormattedRuntimeException(e, "Could not find servlet path in request URI.  URI=''{0}'', servletPath=''{1}''", requestURI, servletPath);
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

		List<String> l = new ArrayList<>();
		for (String v : value) {
			if (! "INHERIT".equals(v))
				l.add(v);
			else if (fromParent != null)
				l.addAll(Arrays.asList(fromParent));
		}
		return join(l, '\n');
	}

	private static final Pattern INDEXED_LINK_PATTERN = Pattern.compile("(?s)(\\S*)\\[(\\d+)\\]\\:(.*)");

	static String[] resolveLinks(String[] links, String[] parentLinks) {
		if (links.length == 0)
			return parentLinks;

		List<String> list = new ArrayList<>();
		for (String l : links) {
			if ("INHERIT".equals(l))
				list.addAll(Arrays.asList(parentLinks));
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
		return list.toArray(new String[list.size()]);
	}

	static String[] resolveContent(String[] content, String[] parentContent) {
		if (content.length == 0)
			return parentContent;

		List<String> list = new ArrayList<>();
		for (String l : content) {
			if ("INHERIT".equals(l)) {
				list.addAll(Arrays.asList(parentContent));
			} else if ("NONE".equals(l)) {
				return new String[0];
			} else {
				list.add(l);
			}
		}
		return list.toArray(new String[list.size()]);
	}

	/**
	 * Parses a URL query string or form-data body.
	 *
	 * @param qs A reader or string containing the query string to parse.
	 * @return A new map containing the parsed query.
	 * @throws Exception
	 */
	public static Map<String,String[]> parseQuery(Object qs) throws Exception {
		return parseQuery(qs, null);
	}

	/**
	 * Same as {@link #parseQuery(Object)} but allows you to specify the map to insert values into.
	 *
	 * @param qs A reader containing the query string to parse.
	 * @param map The map to pass the values into.
	 * @return The same map passed in, or a new map if it was <jk>null</jk>.
	 * @throws Exception
	 */
	public static Map<String,String[]> parseQuery(Object qs, Map<String,String[]> map) throws Exception {

		Map<String,String[]> m = map;
		if (m == null)
			m = new TreeMap<>();

		if (qs == null || ((qs instanceof CharSequence) && isEmpty(qs)))
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
	 * @throws ParseException
	 */
	public static Object parseAnything(String s) throws ParseException {
		if (isJson(s))
			return JsonParser.DEFAULT.parse(s, Object.class);
		return s;
	}

	/**
	 * Merges the specified parent and child arrays.
	 *
	 * <p>
	 * The general concept is to allow child values to override parent values.
	 *
	 * <p>
	 * The rules are:
	 * <ul>
	 * 	<li>If the child array is not empty, then the child array is returned.
	 * 	<li>If the child array is empty, then the parent array is returned.
	 * 	<li>If the child array contains {@link None}, then an empty array is always returned.
	 * 	<li>If the child array contains {@link Inherit}, then the contents of the parent array are inserted into the position of the {@link Inherit} entry.
	 * </ul>
	 *
	 * @param fromParent The parent array.
	 * @param fromChild The child array.
	 * @return A new merged array.
	 */
	public static Object[] merge(Object[] fromParent, Object[] fromChild) {

		if (ArrayUtils.contains(None.class, fromChild))
			return new Object[0];

		if (fromChild.length == 0)
			return fromParent;

		if (! ArrayUtils.contains(Inherit.class, fromChild))
			return fromChild;

		List<Object> l = new ArrayList<>(fromParent.length + fromChild.length);
		for (Object o : fromChild) {
			if (o == Inherit.class)
				l.addAll(Arrays.asList(fromParent));
			else
				l.add(o);
		}
		return l.toArray(new Object[l.size()]);
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
}
