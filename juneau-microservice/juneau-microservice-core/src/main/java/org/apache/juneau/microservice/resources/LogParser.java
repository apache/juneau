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

import java.io.*;
import java.nio.charset.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;

import org.apache.juneau.internal.ASet;

/**
 * Utility class for reading log files.
 *
 * <p>
 * Provides the capability of returning splices of log files based on dates and filtering based on thread and logger
 * names.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-microservice-core}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public final class LogParser implements Iterable<LogParser.Entry>, Iterator<LogParser.Entry>, Closeable {
	private BufferedReader br;
	LogEntryFormatter formatter;
	Date start, end;
	Set<String> loggerFilter, severityFilter;
	String threadFilter;
	private Entry next;

	/**
	 * Constructor.
	 *
	 * @param formatter The log entry formatter.
	 * @param f The log file.
	 * @param start Don't return rows before this date.  If <jk>null</jk>, start from the beginning of the file.
	 * @param end Don't return rows after this date.  If <jk>null</jk>, go to the end of the file.
	 * @param thread Only return log entries with this thread name.
	 * @param loggers Only return log entries produced by these loggers (simple class names).
	 * @param severity Only return log entries with the specified severity.
	 * @throws IOException Thrown by underlying stream.
	 */
	public LogParser(LogEntryFormatter formatter, File f, Date start, Date end, String thread, String[] loggers, String[] severity) throws IOException {
		br = new BufferedReader(new InputStreamReader(new FileInputStream(f), Charset.defaultCharset()));
		this.formatter = formatter;
		this.start = start;
		this.end = end;
		this.threadFilter = thread;
		if (loggers != null)
			this.loggerFilter = ASet.of(loggers);
		if (severity != null)
			this.severityFilter = ASet.of(severity);

		// Find the first line.
		String line;
		while (next == null && (line = br.readLine()) != null) {
			Entry e = new Entry(line);
			if (e.matches())
				next = e;
		}
	}

	@Override /* Iterator */
	public boolean hasNext() {
		return next != null;
	}

	@Override /* Iterator */
	public Entry next() {
		Entry current = next;
		Entry prev = next;
		try {
			next = null;
			String line = null;
			while (next == null && (line = br.readLine()) != null) {
				Entry e = new Entry(line);
				if (e.isRecord) {
					if (e.matches())
						next = e;
					prev = null;
				} else {
					if (prev != null)
						prev.addText(e.line);
				}
			}
		} catch (IOException e) {
			throw runtimeException(e);
		}
		return current;
	}

	@Override /* Iterator */
	public void remove() {
		throw new NoSuchMethodError();
	}

	@Override /* Iterable */
	public Iterator<Entry> iterator() {
		return this;
	}

	@Override /* Closeable */
	public void close() throws IOException {
		br.close();
	}

	/**
	 * Serializes the contents of the parsed log file to the specified writer and then closes the underlying reader.
	 *
	 * @param w The writer to write the log file to.
	 * @throws IOException Thrown by underlying stream.
	 */
	public void writeTo(Writer w) throws IOException {
		try {
			if (! hasNext())
				w.append("[EMPTY]");
			else for (LogParser.Entry le : this)
				le.append(w);
		} finally {
			close();
		}
	}

	/**
	 * Represents a single line from the log file.
	 */
	@SuppressWarnings("javadoc")
	public final class Entry {
		public Date date;
		public String severity, logger;
		protected String line, text;
		protected String thread;
		protected List<String> additionalText;
		protected boolean isRecord;

		Entry(String line) throws IOException {
			try {
				this.line = line;
				Matcher m = formatter.getLogEntryPattern().matcher(line);
				if (m.matches()) {
					isRecord = true;
					String s = formatter.getField("date", m);
					if (s != null)
						date = formatter.getDateFormat().parse(s);
					thread = formatter.getField("thread", m);
					severity = formatter.getField("level", m);
					logger = formatter.getField("logger", m);
					text = formatter.getField("msg", m);
					if (logger != null && logger.indexOf('.') > -1)
						logger = logger.substring(logger.lastIndexOf('.')+1);
				}
			} catch (ParseException e) {
				throw ioException(e);
			}
		}

		void addText(String t) {
			if (additionalText == null)
				additionalText = new LinkedList<>();
			additionalText.add(t);
		}

		public String getText() {
			if (additionalText == null)
				return text;
			if (text == null)
				return "";
			int i = text.length();
			for (String s : additionalText)
				i += s.length() + 1;
			StringBuilder sb = new StringBuilder(i);
			sb.append(text);
			for (String s : additionalText)
				sb.append('\n').append(s);
			return sb.toString();
		}

		public String getThread() {
			return thread;
		}

		public Writer appendHtml(Writer w) throws IOException {
			w.append(toHtml(line)).append("<br>");
			if (additionalText != null)
				for (String t : additionalText)
					w.append(toHtml(t)).append("<br>");
			return w;
		}

		protected Writer append(Writer w) throws IOException {
			w.append(line).append('\n');
			if (additionalText != null)
				for (String t : additionalText)
					w.append(t).append('\n');
			return w;
		}

		boolean matches() {
			if (! isRecord)
				return false;
			if (start != null && date.before(start))
				return false;
			if (end != null && date.after(end))
				return false;
			if (threadFilter != null && ! threadFilter.equals(thread))
				return false;
			if (loggerFilter != null && ! loggerFilter.contains(logger))
				return false;
			if (severityFilter != null && ! severityFilter.contains(severity))
				return false;
			return true;
		}
	}

	static final String toHtml(String s) {
		if (s.indexOf('<') != -1)
			return s.replaceAll("<", "&lt;");//$NON-NLS-2$
		return s;
	}
}

