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
package org.apache.juneau.microservice.resources;

import static org.apache.juneau.internal.ThrowableUtils.*;

import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.logging.*;
import java.util.logging.Formatter;
import java.util.regex.*;

import org.apache.juneau.collections.*;

/**
 * Log entry formatter.
 *
 * <p>
 * Uses three simple parameter for configuring log entry formats:
 * <ul class='spaced-list'>
 * 	<li>
 * 		<c>dateFormat</c> - A {@link SimpleDateFormat} string describing the format for dates.
 * 	<li>
 * 		<c>format</c> - A string with <c>{...}</c> replacement variables representing predefined fields.
 * 	<li>
 * 		<c>useStackTraceHashes</c> - A setting that causes duplicate stack traces to be replaced with 8-character
 * 		 hash strings.
 * </ul>
 *
 * <p>
 * This class converts the format strings into a regular expression that can be used to parse the resulting log file.
 */
public class LogEntryFormatter extends Formatter {

	private ConcurrentHashMap<String,AtomicInteger> hashes;
	private DateFormat df;
	private String format;
	private Pattern rePattern;
	private Map<String,Integer> fieldIndexes;

	/**
	 * Create a new formatter.
	 *
	 * @param format
	 * 	The log entry format.  e.g. <js>"[{date} {level}] {msg}%n"</js>
	 * 	The string can contain any of the following variables:
	 * 	<ol>
	 * 		<li><js>"{date}"</js> - The date, formatted per <js>"Logging/dateFormat"</js>.
	 * 		<li><js>"{class}"</js> - The class name.
	 * 		<li><js>"{method}"</js> - The method name.
	 * 		<li><js>"{logger}"</js> - The logger name.
	 * 		<li><js>"{level}"</js> - The log level name.
	 * 		<li><js>"{msg}"</js> - The log message.
	 * 		<li><js>"{threadid}"</js> - The thread ID.
	 * 		<li><js>"{exception}"</js> - The localized exception message.
	 * 	</ol>
	 * @param dateFormat
	 * 	The {@link SimpleDateFormat} format to use for dates.  e.g. <js>"yyyy.MM.dd hh:mm:ss"</js>.
	 * @param useStackTraceHashes
	 * 	If <jk>true</jk>, only print unique stack traces once and then refer to them by a simple 8 character hash
	 * 	identifier.
	 */
	public LogEntryFormatter(String format, String dateFormat, boolean useStackTraceHashes) {
		this.df = new SimpleDateFormat(dateFormat);
		if (useStackTraceHashes)
			hashes = new ConcurrentHashMap<>();

		fieldIndexes = new HashMap<>();

		format = format
			.replaceAll("\\{date\\}", "%1\\$s")
			.replaceAll("\\{class\\}", "%2\\$s")
			.replaceAll("\\{method\\}", "%3\\$s")
			.replaceAll("\\{logger\\}", "%4\\$s")
			.replaceAll("\\{level\\}", "%5\\$s")
			.replaceAll("\\{msg\\}", "%6\\$s")
			.replaceAll("\\{threadid\\}", "%7\\$s")
			.replaceAll("\\{exception\\}", "%8\\$s");

		this.format = format;

		// Construct a regular expression to match this log entry.
		int index = 1;
		StringBuilder re = new StringBuilder();
		int S1 = 1; // Looking for %
		int S2 = 2; // Found %, looking for number.
		int S3 = 3; // Found number, looking for $.
		int S4 = 4; // Found $, looking for s.
		int state = 1;
		int i1 = 0;
		for (int i = 0; i < format.length(); i++) {
			char c = format.charAt(i);
			if (state == S1) {
				if (c == '%')
					state = S2;
				else {
					if (! (Character.isLetterOrDigit(c) || Character.isWhitespace(c)))
						re.append('\\');
					re.append(c);
				}
			} else if (state == S2) {
				if (Character.isDigit(c)) {
					i1 = i;
					state = S3;
				} else {
					re.append("\\%").append(c);
					state = S1;
				}
			} else if (state == S3) {
				if (c == '$') {
					state = S4;
				} else {
					re.append("\\%").append(format.substring(i1, i));
					state = S1;
				}
			} else if (state == S4) {
				if (c == 's') {
					int group = Integer.parseInt(format.substring(i1, i-1));
					switch (group) {
						case 1:
							fieldIndexes.put("date", index++);
							re.append("(" + dateFormat.replaceAll("[mHhsSdMy]", "\\\\d").replaceAll("\\.", "\\\\.") + ")");
							break;
						case 2:
							fieldIndexes.put("class", index++);
							re.append("([\\p{javaJavaIdentifierPart}\\.]+)");
							break;
						case 3:
							fieldIndexes.put("method", index++);
							re.append("([\\p{javaJavaIdentifierPart}\\.]+)");
							break;
						case 4:
							fieldIndexes.put("logger", index++);
							re.append("([\\w\\d\\.\\_]+)");
							break;
						case 5:
							fieldIndexes.put("level", index++);
							re.append("(SEVERE|WARNING|INFO|CONFIG|FINE|FINER|FINEST)");
							break;
						case 6:
							fieldIndexes.put("msg", index++);
							re.append("(.*)");
							break;
						case 7:
							fieldIndexes.put("threadid", index++);
							re.append("(\\\\d+)");
							break;
						case 8:
							fieldIndexes.put("exception", index++);
							re.append("(.*)");
							break;
						default: // Fall through.
					}
				} else {
					re.append("\\%").append(format.substring(i1, i));
				}
				state = S1;
			}
		}

		// The log parser
		String sre = re.toString();
		if (sre.endsWith("\\%n"))
			sre = sre.substring(0, sre.length()-3);

		// Replace instances of %n.
		sre = sre.replaceAll("\\\\%n", "\\\\n");

		rePattern = Pattern.compile(sre);
		fieldIndexes = AMap.unmodifiable(fieldIndexes);
	}

	/**
	 * Returns the regular expression pattern used for matching log entries.
	 *
	 * @return The regular expression pattern used for matching log entries.
	 */
	public Pattern getLogEntryPattern() {
		return rePattern;
	}

	/**
	 * Returns the {@link DateFormat} used for matching dates.
	 *
	 * @return The {@link DateFormat} used for matching dates.
	 */
	public DateFormat getDateFormat() {
		return df;
	}

	/**
	 * Given a matcher that has matched the pattern specified by {@link #getLogEntryPattern()}, returns the field value
	 * from the match.
	 *
	 * @param fieldName
	 * 	The field name.
	 * 	Possible values are:
	 * 	<ul>
	 * 		<li><js>"date"</js>
	 * 		<li><js>"class"</js>
	 * 		<li><js>"method"</js>
	 * 		<li><js>"logger"</js>
	 * 		<li><js>"level"</js>
	 * 		<li><js>"msg"</js>
	 * 		<li><js>"threadid"</js>
	 * 		<li><js>"exception"</js>
	 * 	</ul>
	 * @param m The matcher.
	 * @return The field value, or <jk>null</jk> if the specified field does not exist.
	 */
	public String getField(String fieldName, Matcher m) {
		Integer i = fieldIndexes.get(fieldName);
		return (i == null ? null : m.group(i));
	}

	@Override /* Formatter */
	public String format(LogRecord r) {
		String msg = formatMessage(r);
		Throwable t = r.getThrown();
		String hash = null;
		int c = 0;
		if (hashes != null && t != null) {
			hash = hashCode(t);
			hashes.putIfAbsent(hash, new AtomicInteger(0));
			c = hashes.get(hash).incrementAndGet();
			if (c == 1) {
				msg = '[' + hash + '.' + c + "] " + msg;
			} else {
				msg = '[' + hash + '.' + c + "] " + msg + ", " + t.getLocalizedMessage();
				t = null;
			}
		}
		String s = String.format(format,
			df.format(new Date(r.getMillis())),
			r.getSourceClassName(),
			r.getSourceMethodName(),
			r.getLoggerName(),
			r.getLevel(),
			msg,
			r.getThreadID(),
			r.getThrown() == null ? "" : r.getThrown().getMessage());
		if (t != null)
			s += String.format("%n%s", getStackTrace(r.getThrown()));
		return s;
	}

	private static String hashCode(Throwable t) {
		int i = 0;
		while (t != null) {
			for (StackTraceElement e : t.getStackTrace())
				i ^= e.hashCode();
			t = t.getCause();
		}
		return Integer.toHexString(i);
	}
}
