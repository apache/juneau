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

import static javax.servlet.http.HttpServletResponse.*;
import static org.apache.juneau.html.HtmlDocSerializer.*;
import static org.apache.juneau.rest.annotation.HookEvent.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.http.HttpMethodName.*;

import java.io.*;
import java.net.URI;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.config.*;
import org.apache.juneau.dto.LinkString;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.converters.*;
import org.apache.juneau.transforms.*;

/**
 * REST resource for viewing and accessing log files.
 */
@RestResource(
	path="/logs",
	title="Log files",
	description="Log files from this service",
	properties={
		@Property(name=HTML_uriAnchorText, value="PROPERTY_NAME"),
	},
	allowedMethodParams="*",
	pojoSwaps={
		IteratorSwap.class,       // Allows Iterators and Iterables to be serialized.
		DateSwap.ISO8601DT.class  // Serialize Date objects as ISO8601 strings.
	}
)
public class LogsResource extends BasicRestServlet {
	private static final long serialVersionUID = 1L;

	private File logDir;
	private LogEntryFormatter leFormatter;

	private final FileFilter filter = new FileFilter() {
		@Override /* FileFilter */
		public boolean accept(File f) {
			return f.isDirectory() || f.getName().endsWith(".log");
		}
	};

	/**
	 * Initializes the log directory and formatter.
	 * 
	 * @param builder The resource config.
	 * @throws Exception
	 */
	@RestHook(INIT) 
	public void init(RestContextBuilder builder) throws Exception {
		Config c = builder.getConfig();

		logDir = new File(c.getString("Logging/logDir", "."));
		leFormatter = new LogEntryFormatter(
			c.getString("Logging/format", "[{date} {level}] {msg}%n"),
			c.getString("Logging/dateFormat", "yyyy.MM.dd hh:mm:ss"),
			c.getBoolean("Logging/useStackTraceHashes")
		);
	}

	/**
	 * [GET /*] - Get file details or directory listing.
	 * 
	 * @param req The HTTP request
	 * @param res The HTTP response
	 * @param properties The writable properties for setting the descriptions.
	 * @param path The log file path.
	 * @return The log file.
	 * @throws Exception
	 */
	@RestMethod(
		name=GET,
		path="/*",
		swagger= {
			"responses:{",
				"200: {description:'OK'},",
				"404: {description:'Not Found'}",
			"}"
		}
	)
	public Object getFileOrDirectory(RestRequest req, RestResponse res, RequestProperties properties, @PathRemainder String path) throws Exception {

		File f = getFile(path);

		if (f.isDirectory()) {
			Set<FileResource> l = new TreeSet<>(new FileResourceComparator());
			File[] files = f.listFiles(filter);
			if (files != null) {
				for (File fc : files) {
					URI fUrl = new URI("servlet:/" + fc.getName());
					l.add(new FileResource(fc, fUrl));
				}
			}
			return l;
		}

		return new FileResource(f, new URI("servlet:/"));
	}

	/**
	 * [VIEW /*] - Retrieve the contents of a log file.
	 * 
	 * @param req The HTTP request.
	 * @param res The HTTP response.
	 * @param path The log file path.
	 * @param properties The writable properties for setting the descriptions.
	 * @param highlight If <code>true</code>, add color highlighting based on severity.
	 * @param start Optional start timestamp.  Don't print lines logged before the specified timestamp.  Example:  "&amp;start=2014-01-23 11:25:47".
	 * @param end Optional end timestamp.  Don't print lines logged after the specified timestamp.  Example:  "&amp;end=2014-01-23 11:25:47".
	 * @param thread Optional thread name filter.  Only show log entries with the specified thread name.  Example: "&amp;thread=pool-33-thread-1".
	 * @param loggers Optional logger filter.  Only show log entries if they were produced by one of the specified loggers (simple class name).  Example: "&amp;loggers=(LinkIndexService,LinkIndexRestService)".
	 * @param severity Optional severity filter.  Only show log entries with the specified severity.  Example: "&amp;severity=(ERROR,WARN)".
	 * @throws Exception
	 */
	@RestMethod(
		name="VIEW",
		path="/*",
		swagger= {
			"responses:{",
				"200: {description:'OK'},",
				"404: {description:'Not Found'}",
			"}"
		}
	)
	public void viewFile(RestRequest req, RestResponse res, @PathRemainder String path, RequestProperties properties, @Query("highlight") boolean highlight, @Query("start") String start, @Query("end") String end, @Query("thread") String thread, @Query("loggers") String[] loggers, @Query("severity") String[] severity) throws Exception {

		File f = getFile(path);
		if (f.isDirectory())
			throw new RestException(SC_METHOD_NOT_ALLOWED, "View not available on directories");

		Date startDate = parseISO8601Date(start), endDate = parseISO8601Date(end);

		if (! highlight) {
			Object o = getReader(f, startDate, endDate, thread, loggers, severity);
			res.setContentType("text/plain");
			if (o instanceof Reader)
				res.setOutput(o);
			else {
				try (LogParser p = (LogParser)o; Writer w = res.getNegotiatedWriter()) {
					p.writeTo(w);
				}
			}
			return;
		}

		res.setContentType("text/html");
		try (PrintWriter w = res.getNegotiatedWriter()) {
			w.println("<html><body style='font-family:monospace;font-size:8pt;white-space:pre;'>");
			try (LogParser lp = getLogParser(f, startDate, endDate, thread, loggers, severity)) {
				if (! lp.hasNext())
					w.append("<span style='color:gray'>[EMPTY]</span>");
				else for (LogParser.Entry le : lp) {
					char s = le.severity.charAt(0);
					String color = "black";
					//SEVERE|WARNING|INFO|CONFIG|FINE|FINER|FINEST
					if (s == 'I')
						color = "#006400";
					else if (s == 'W')
						color = "#CC8400";
					else if (s == 'E' || s == 'S')
						color = "#DD0000";
					else if (s == 'D' || s == 'F' || s == 'T')
						color = "#000064";
					w.append("<span style='color:").append(color).append("'>");
					le.appendHtml(w).append("</span>");
				}
				w.append("</body></html>");
			}
		}
	}

	/**
	 * [VIEW /*] - Retrieve the contents of a log file as parsed entries.
	 * 
	 * @param req The HTTP request.
	 * @param path The log file path.
	 * @param start Optional start timestamp.  Don't print lines logged before the specified timestamp.  Example:  "&amp;start=2014-01-23 11:25:47".
	 * @param end Optional end timestamp.  Don't print lines logged after the specified timestamp.  Example:  "&amp;end=2014-01-23 11:25:47".
	 * @param thread Optional thread name filter.  Only show log entries with the specified thread name.  Example: "&amp;thread=pool-33-thread-1".
	 * @param loggers Optional logger filter.  Only show log entries if they were produced by one of the specified loggers (simple class name).  Example: "&amp;loggers=(LinkIndexService,LinkIndexRestService)".
	 * @param severity Optional severity filter.  Only show log entries with the specified severity.  Example: "&amp;severity=(ERROR,WARN)".
	 * @return The parsed contents of the log file.
	 * @throws Exception
	 */
	@RestMethod(
		name="PARSE",
		path="/*",
		converters=Queryable.class,
		swagger= {
			"responses:{",
				"200: {description:'OK'},",
				"404: {description:'Not Found'}",
			"}"
		}
	)
	public LogParser viewParsedEntries(RestRequest req, @PathRemainder String path, @Query("start") String start, @Query("end") String end, @Query("thread") String thread, @Query("loggers") String[] loggers, @Query("severity") String[] severity) throws Exception {

		File f = getFile(path);
		Date startDate = parseISO8601Date(start), endDate = parseISO8601Date(end);

		if (f.isDirectory())
			throw new RestException(SC_METHOD_NOT_ALLOWED, "View not available on directories");

		return getLogParser(f, startDate, endDate, thread, loggers, severity);
	}

	/**
	 * [DOWNLOAD /*] - Download file.
	 * 
	 * @param res The HTTP response.
	 * @param path The log file path.
	 * @return The contents of the log file.
	 * @throws Exception
	 */
	@RestMethod(
		name="DOWNLOAD",
		path="/*",
		swagger= {
			"responses:{",
				"200: {description:'OK'},",
				"404: {description:'Not Found'}",
			"}"
		}
	)
	public Object downloadFile(RestResponse res, @PathRemainder String path) throws Exception {

		File f = getFile(path);

		if (f.isDirectory())
			throw new RestException(SC_METHOD_NOT_ALLOWED, "Download not available on directories");

		res.setContentType("application/octet-stream");
		res.setContentLength((int)f.length());
		return new FileInputStream(f);
	}

	/**
	 * [DELETE /*] - Delete a file.
	 * 
	 * @param path The log file path.
	 * @return A redirect object to the root.
	 * @throws Exception
	 */
	@RestMethod(
		name=DELETE,
		path="/*",
		swagger= {
			"responses:{",
				"200: {description:'OK'},",
				"404: {description:'Not Found'}",
			"}"
		}
	)
	public Object deleteFile(@PathRemainder String path) throws Exception {

		File f = getFile(path);

		if (f.isDirectory())
			throw new RestException(SC_BAD_REQUEST, "Delete not available on directories.");

		if (f.canWrite())
			if (! f.delete())
				throw new RestException(SC_FORBIDDEN, "Could not delete file.");

		return new Redirect(path + "/..");
	}

	private static BufferedReader getReader(File f) throws IOException {
		return new BufferedReader(new InputStreamReader(new FileInputStream(f), Charset.defaultCharset()));
	}

	private File getFile(String path) {
		if (path != null && path.indexOf("..") != -1)
			throw new RestException(SC_NOT_FOUND, "File not found.");
		File f = (path == null ? logDir : new File(logDir.getAbsolutePath() + '/' + path));
		if (filter.accept(f))
			return f;
		throw new RestException(SC_NOT_FOUND, "File not found.");
	}

	/**
	 * File bean.
	 */
	@SuppressWarnings("javadoc")
	public static class FileResource {
		final File f;
		public final String type;
		public final Object name;
		public final Long size;
		@Swap(DateSwap.DateTimeMedium.class) public Date lastModified;
		public URI view, highlighted, parsed, download, delete;

		public FileResource(File f, URI uri) throws Exception {
			this.f = f;
			this.type = (f.isDirectory() ? "dir" : "file");
			this.name = f.isDirectory() ? new LinkString(f.getName(), uri.toString()) : f.getName();
			this.size = f.isDirectory() ? null : f.length();
			this.lastModified = new Date(f.lastModified());
			if (f.canRead() && ! f.isDirectory()) {
				this.view = new URI(uri + "?method=VIEW");
				this.highlighted = new URI(uri + "?method=VIEW&highlight=true");
				this.parsed = new URI(uri + "?method=PARSE");
				this.download = new URI(uri + "?method=DOWNLOAD");
				this.delete = new URI(uri + "?method=DELETE");
			}
		}
	}

	static final class FileResourceComparator implements Comparator<FileResource>, Serializable {
		private static final long serialVersionUID = 1L;
		@Override /* Comparator */
		public int compare(FileResource o1, FileResource o2) {
			int c = o1.type.compareTo(o2.type);
			return c != 0 ? c : o1.f.getName().compareTo(o2.f.getName());
		}
	}

	private Object getReader(File f, final Date start, final Date end, final String thread, final String[] loggers, final String[] severity) throws IOException {
		if (start == null && end == null && thread == null && loggers == null)
			return getReader(f);
		return getLogParser(f, start, end, thread, loggers, severity);
	}

	private LogParser getLogParser(File f, final Date start, final Date end, final String thread, final String[] loggers, final String[] severity) throws IOException {
		return new LogParser(leFormatter, f, start, end, thread, loggers, severity);
	}
}
