/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.microservice.resources;

import static javax.servlet.http.HttpServletResponse.*;
import static org.apache.juneau.html.HtmlDocSerializerContext.*;
import static org.apache.juneau.server.RestServletContext.*;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.dto.*;
import org.apache.juneau.ini.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.server.*;
import org.apache.juneau.server.annotation.*;
import org.apache.juneau.server.annotation.Properties;
import org.apache.juneau.server.converters.*;
import org.apache.juneau.transforms.*;

/**
 * REST resource for viewing and accessing log files.
 */
@RestResource(
	path="/logs",
	label="Log files",
	description="Log files from this service",
	properties={
		@Property(name=HTML_uriAnchorText, value=PROPERTY_NAME),
		@Property(name=REST_allowMethodParam, value="true")
	},
	transforms={
		IteratorSwap.class,       // Allows Iterators and Iterables to be serialized.
		DateSwap.ISO8601DT.class  // Serialize Date objects as ISO8601 strings.
	}
)
@SuppressWarnings("nls")
public class LogsResource extends Resource {
	private static final long serialVersionUID = 1L;

	private ConfigFile cf = getConfig();

	private File logDir = new File(cf.getString("Logging/logDir", "."));
	private LogEntryFormatter leFormatter = new LogEntryFormatter(
		cf.getString("Logging/format", "[{date} {level}] {msg}%n"),
		cf.getString("Logging/dateFormat", "yyyy.MM.dd hh:mm:ss"),
		cf.getBoolean("Logging/useStackTraceHashes")
	);

	private final FileFilter filter = new FileFilter() {
		@Override /* FileFilter */
		public boolean accept(File f) {
			return f.isDirectory() || f.getName().endsWith(".log");
		}
	};

	/**
	 * [GET /*] - Get file details or directory listing.
	 *
	 * @param req The HTTP request
	 * @param properties The writable properties for setting the descriptions.
	 * @param path The log file path.
	 * @return The log file.
	 * @throws Exception
	 */
	@RestMethod(name="GET", path="/*", rc={200,404})
	public Object getFileOrDirectory(RestRequest req, @Properties ObjectMap properties, @PathRemainder String path) throws Exception {

		File f = getFile(path);

		if (f.isDirectory()) {
			Set<FileResource> l = new TreeSet<FileResource>(new FileResourceComparator());
			for (File fc : f.listFiles(filter)) {
				URL fUrl = new URL(req.getTrimmedRequestURL().append('/').append(fc.getName()).toString());
				l.add(new FileResource(fc, fUrl));
			}
			properties.put(HTMLDOC_description, "Contents of " + f.getAbsolutePath());
			return l;
		}

		properties.put(HTMLDOC_description, "File details on " + f.getAbsolutePath());
		return new FileResource(f, new URL(req.getTrimmedRequestURL().toString()));
	}

	/**
	 * [VIEW /*] - Retrieve the contents of a log file.
	 *
	 * @param req The HTTP request.
	 * @param res The HTTP response.
	 * @param path The log file path.
	 * @param properties The writable properties for setting the descriptions.
	 * @param highlight If <code>true</code>, add color highlighting based on severity.
	 * @param start Optional start timestamp.  Don't print lines logged before the specified timestamp.  Example:  "&start=2014-01-23 11:25:47".
	 * @param end Optional end timestamp.  Don't print lines logged after the specified timestamp.  Example:  "&end=2014-01-23 11:25:47".
	 * @param thread Optional thread name filter.  Only show log entries with the specified thread name.  Example: "&thread=pool-33-thread-1".
	 * @param loggers Optional logger filter.  Only show log entries if they were produced by one of the specified loggers (simple class name).  Example: "&loggers=(LinkIndexService,LinkIndexRestService)".
	 * @param severity Optional severity filter.  Only show log entries with the specified severity.  Example: "&severity=(ERROR,WARN)".
	 * @throws Exception
	 */
	@RestMethod(name="VIEW", path="/*", rc={200,404})
	@SuppressWarnings("nls")
	public void viewFile(RestRequest req, RestResponse res, @PathRemainder String path, @Properties ObjectMap properties, @Param("highlight") boolean highlight, @Param("start") String start, @Param("end") String end, @Param("thread") String thread, @Param("loggers") String[] loggers, @Param("severity") String[] severity) throws Exception {

		File f = getFile(path);
		if (f.isDirectory())
			throw new RestException(SC_METHOD_NOT_ALLOWED, "View not available on directories");

		Date startDate = StringUtils.parseISO8601Date(start), endDate = StringUtils.parseISO8601Date(end);

		if (! highlight) {
			Object o = getReader(f, startDate, endDate, thread, loggers, severity);
			res.setContentType("text/plain");
			if (o instanceof Reader)
				res.setOutput(o);
			else {
				LogParser p = (LogParser)o;
				Writer w = res.getNegotiatedWriter();
				try {
					p.writeTo(w);
				} finally {
					w.flush();
					w.close();
				}
			}
			return;
		}

		res.setContentType("text/html");
		PrintWriter w = res.getNegotiatedWriter();
		try {
			w.println("<html><body style='font-family:monospace;font-size:8pt;white-space:pre;'>");
			LogParser lp = getLogParser(f, startDate, endDate, thread, loggers, severity);
			try {
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
			} finally {
				lp.close();
			}
		} finally {
			w.close();
		}
	}

	/**
	 * [VIEW /*] - Retrieve the contents of a log file as parsed entries.
	 *
	 * @param req The HTTP request.
	 * @param path The log file path.
	 * @param start Optional start timestamp.  Don't print lines logged before the specified timestamp.  Example:  "&start=2014-01-23 11:25:47".
	 * @param end Optional end timestamp.  Don't print lines logged after the specified timestamp.  Example:  "&end=2014-01-23 11:25:47".
	 * @param thread Optional thread name filter.  Only show log entries with the specified thread name.  Example: "&thread=pool-33-thread-1".
	 * @param loggers Optional logger filter.  Only show log entries if they were produced by one of the specified loggers (simple class name).  Example: "&loggers=(LinkIndexService,LinkIndexRestService)".
	 * @param severity Optional severity filter.  Only show log entries with the specified severity.  Example: "&severity=(ERROR,WARN)".
	 * @return The parsed contents of the log file.
	 * @throws Exception
	 */
	@RestMethod(name="PARSE", path="/*", converters=Queryable.class, rc={200,404})
	public LogParser viewParsedEntries(RestRequest req, @PathRemainder String path, @Param("start") String start, @Param("end") String end, @Param("thread") String thread, @Param("loggers") String[] loggers, @Param("severity") String[] severity) throws Exception {

		File f = getFile(path);
		Date startDate = StringUtils.parseISO8601Date(start), endDate = StringUtils.parseISO8601Date(end);

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
	@RestMethod(name="DOWNLOAD", path="/*", rc={200,404})
	public Object downloadFile(RestResponse res, @PathRemainder String path) throws Exception {

		File f = getFile(path);

		if (f.isDirectory())
			throw new RestException(SC_METHOD_NOT_ALLOWED, "Download not available on directories");

		res.setContentType("application/octet-stream"); //$NON-NLS-1$
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
	@RestMethod(name="DELETE", path="/*", rc={200,404})
	public Object deleteFile(@PathRemainder String path) throws Exception {

		File f = getFile(path);

		if (f.isDirectory())
			throw new RestException(SC_BAD_REQUEST, "Delete not available on directories.");

		if (f.canWrite())
			if (! f.delete())
				throw new RestException(SC_FORBIDDEN, "Could not delete file.");

		return new Redirect(path + "/.."); //$NON-NLS-1$
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
		private File f;
		public String type;
		public Object name;
		public Long size;
		@BeanProperty(swap=DateSwap.Medium.class) public Date lastModified;
		public URL view, highlighted, parsed, download, delete;

		public FileResource(File f, URL url) throws IOException {
			this.f = f;
			this.type = (f.isDirectory() ? "dir" : "file");
			this.name = f.isDirectory() ? new Link(f.getName(), url.toString()) : f.getName();
			this.size = f.isDirectory() ? null : f.length();
			this.lastModified = new Date(f.lastModified());
			if (f.canRead() && ! f.isDirectory()) {
				this.view = new URL(url + "?method=VIEW");
				this.highlighted = new URL(url + "?method=VIEW&highlight=true");
				this.parsed = new URL(url + "?method=PARSE");
				this.download = new URL(url + "?method=DOWNLOAD");
				this.delete = new URL(url + "?method=DELETE");
			}
		}
	}

	private static class FileResourceComparator implements Comparator<FileResource>, Serializable {
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
