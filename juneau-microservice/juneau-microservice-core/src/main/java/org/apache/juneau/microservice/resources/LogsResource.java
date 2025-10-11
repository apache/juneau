/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.microservice.resources;

import static org.apache.juneau.common.utils.StringUtils.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.bean.*;
import org.apache.juneau.config.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.beans.*;
import org.apache.juneau.rest.converter.*;
import org.apache.juneau.rest.servlet.*;

/**
 * REST resource for viewing and accessing log files.
 */
@Rest(
	path="/logs",
	title="Log files",
	description="Log files from this service",
	allowedMethodParams="*"
)
@HtmlConfig(uriAnchorText="PROPERTY_NAME")
@SuppressWarnings("javadoc")
public class LogsResource extends BasicRestServlet {
	private static final long serialVersionUID = 1L;

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private File logDir;
	private LogEntryFormatter leFormatter;
	boolean allowDeletes;

	@RestInit
	public void init(Config config) throws Exception {
		logDir = new File(config.get("Logging/logDir").asString().orElse("logs"));
		allowDeletes = config.get("Logging/allowDeletes").asBoolean().orElse(true);
		leFormatter = new LogEntryFormatter(
			config.get("Logging/format").asString().orElse("[{date} {level}] {msg}%n"),
			config.get("Logging/dateFormat").asString().orElse("yyyy.MM.dd hh:mm:ss"),
			config.get("Logging/useStackTraceHashes").asBoolean().orElse(true)
		);
	}

	@RestGet(
		path="/*",
		summary="View information on file or directory",
		description="Returns information about the specified file or directory."
	)
	@HtmlDocConfig(
		nav={"<h5>Folder:  $RA{fullPath}</h5>"}
	)
	public FileResource getFile(RestRequest req, @Path("/*") String path) throws NotFound, Exception {

		File dir = getFile(path);
		req.setAttribute("fullPath", dir.getAbsolutePath());

		return new FileResource(dir, path, allowDeletes, true);
	}

	@RestOp(
		method="VIEW",
		path="/*",
		summary="View contents of log file",
		description="View the contents of a log file."
	)
	public void viewFile(
			RestResponse res,
			@Path("/*") String path,
			@Query(name="highlight", schema=@Schema(d="Add severity color highlighting.")) boolean highlight,
			@Query(name="start", schema=@Schema(d="Start timestamp (ISO8601, full or partial).\nDon't print lines logged before the specified timestamp.\nUse any of the following formats: yyyy, yyyy-MM, yyyy-MM-dd, yyyy-MM-ddThh, yyyy-MM-ddThh:mm, yyyy-MM-ddThh:mm:ss, yyyy-MM-ddThh:mm:ss.SSS")) String start,
			@Query(name="end", schema=@Schema(d="End timestamp (ISO8601, full or partial).\nDon't print lines logged after the specified timestamp.\nUse any of the following formats: yyyy, yyyy-MM, yyyy-MM-dd, yyyy-MM-ddThh, yyyy-MM-ddThh:mm, yyyy-MM-ddThh:mm:ss, yyyy-MM-ddThh:mm:ss.SSS")) String end,
			@Query(name="thread", schema=@Schema(d="Thread name filter.\nOnly show log entries with the specified thread name.")) String thread,
			@Query(name="loggers", schema=@Schema(d="Logger filter (simple class name).\nOnly show log entries if they were produced by one of the specified loggers.")) String[] loggers,
			@Query(name="severity",schema=@Schema( d="Severity filter.\nOnly show log entries with the specified severity.")) String[] severity
		) throws NotFound, MethodNotAllowed, IOException {

		File f = getFile(path);

		Date startDate = parseIsoDate(start), endDate = parseIsoDate(end);

		if (! highlight) {
			Object o = getReader(f, startDate, endDate, thread, loggers, severity);
			res.setContentType("text/plain");
			if (o instanceof Reader)
				res.setContent(o);
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

	@RestOp(
		method="PARSE",
		path="/*",
		converters=Queryable.class,
		summary="View parsed contents of file",
		description="View the parsed contents of a file.",
		swagger=@OpSwagger(
			parameters={
				 Queryable.SWAGGER_PARAMS
			}
		)
	)
	@HtmlDocConfig(
		nav={"<h5>Folder:  $RA{fullPath}</h5>"}
	)
	public LogParser viewParsedEntries(
			RestRequest req,
			@Path("/*") String path,
			@Query(name="start", schema=@Schema(d="Start timestamp (ISO8601, full or partial).\nDon't print lines logged before the specified timestamp.\nUse any of the following formats: yyyy, yyyy-MM, yyyy-MM-dd, yyyy-MM-ddThh, yyyy-MM-ddThh:mm, yyyy-MM-ddThh:mm:ss, yyyy-MM-ddThh:mm:ss.SSS")) String start,
			@Query(name="end", schema=@Schema(d="End timestamp (ISO8601, full or partial).\nDon't print lines logged after the specified timestamp.\nUse any of the following formats: yyyy, yyyy-MM, yyyy-MM-dd, yyyy-MM-ddThh, yyyy-MM-ddThh:mm, yyyy-MM-ddThh:mm:ss, yyyy-MM-ddThh:mm:ss.SSS")) String end,
			@Query(name="thread", schema=@Schema(d="Thread name filter.\nOnly show log entries with the specified thread name.")) String thread,
			@Query(name="loggers", schema=@Schema(d="Logger filter (simple class name).\nOnly show log entries if they were produced by one of the specified loggers.")) String[] loggers,
			@Query(name="severity", schema=@Schema(d="Severity filter.\nOnly show log entries with the specified severity.")) String[] severity
		) throws NotFound, IOException {

		File f = getFile(path);
		req.setAttribute("fullPath", f.getAbsolutePath());

		Date startDate = parseIsoDate(start), endDate = parseIsoDate(end);

		return getLogParser(f, startDate, endDate, thread, loggers, severity);
	}

	@RestOp(
		method="DOWNLOAD",
		path="/*",
		summary="Download file",
		description="Download the contents of a file.\nContent-Type is set to 'application/octet-stream'."
	)
	public FileContents downloadFile(RestResponse res, @Path("/*") String path) throws NotFound, MethodNotAllowed {
		res.setContentType("application/octet-stream");
		try {
			return new FileContents(getFile(path));
		} catch (FileNotFoundException e) {
			throw new NotFound("File not found");
		}
	}

	@RestDelete(
		path="/*",
		summary="Delete log file",
		description="Delete a log file on the file system."
	)
	public RedirectToRoot deleteFile(@Path("/*") String path) throws MethodNotAllowed {
		deleteFile(getFile(path));
		return new RedirectToRoot();
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Helper beans
	//-----------------------------------------------------------------------------------------------------------------

	@Response(schema=@Schema(type="string",format="binary",description="Contents of file"))
	static class FileContents extends FileInputStream {
		public FileContents(File file) throws FileNotFoundException {
			super(file);
		}
	}

	@Response(schema=@Schema(description="Redirect to root page on success"))
	static class RedirectToRoot extends SeeOtherRoot {}

	@Response(schema=@Schema(description="File action"))
	public static class Action extends LinkString {
		public Action(String name, String uri, Object...uriArgs) {
			super(name, uri, uriArgs);
		}
	}

	@Response(schema=@Schema(description="File or directory details"))
	@Bean(properties="type,name,size,lastModified,actions,files")
	public static class FileResource {
		private final File f;
		private final String path;
		private final String uri;
		private final boolean includeChildren, allowDeletes;

		public FileResource(File f, String path, boolean allowDeletes, boolean includeChildren) {
			this.f = f;
			this.path = path;
			this.uri = "servlet:/"+(path == null ? "" : path);
			this.includeChildren = includeChildren;
			this.allowDeletes = allowDeletes;
		}

		public String getType() {
			return (f.isDirectory() ? "dir" : "file");
		}

		public LinkString getName() {
			return new LinkString(f.getName(), uri);
		}

		public long getSize() {
			return f.isDirectory() ? f.listFiles().length : f.length();
		}

		public Date getLastModified() {
			return new Date(f.lastModified());
		}

		@Html(format=HtmlFormat.HTML_CDC)
		public List<Action> getActions() throws Exception {
			List<Action> l = new ArrayList<>();
			if (f.canRead() && ! f.isDirectory()) {
				l.add(new Action("view", uri + "?method=VIEW"));
				l.add(new Action("highlighted", uri + "?method=VIEW&highlight=true"));
				l.add(new Action("parsed", uri + "?method=PARSE"));
				l.add(new Action("download", uri + "?method=DOWNLOAD"));
				if (allowDeletes)
					l.add(new Action("delete", uri + "?method=DELETE"));
			}
			return l;
		}

		public Set<FileResource> getFiles() {
			if (f.isFile() || ! includeChildren)
				return null;
			Set<FileResource> s = new TreeSet<>(FILE_COMPARATOR);
			for (File fc : f.listFiles(FILE_FILTER))
				s.add(new FileResource(fc, (path != null ? (path + '/') : "") + urlEncode(fc.getName()), allowDeletes, false));
			return s;
		}

		static final FileFilter FILE_FILTER = f -> f.isDirectory() || f.getName().endsWith(".log");

		static final Comparator<FileResource> FILE_COMPARATOR = (o1, o2) -> {
        	int c = o1.getType().compareTo(o2.getType());
        	return c != 0 ? c : o1.getName().compareTo(o2.getName());
        };
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Helper methods
	//-----------------------------------------------------------------------------------------------------------------

	private File getFile(String path) throws NotFound {
		if (path == null)
			return logDir;
		File f = new File(logDir.getAbsolutePath() + '/' + path);
		if (f.exists())
			return f;
		throw new NotFound("File not found.");
	}

	private void deleteFile(File f) {
		if (! allowDeletes)
			throw new MethodNotAllowed("DELETE not enabled");
		if (f.isDirectory()) {
			File[] files = f.listFiles();
			if (files != null) {
				for (File fc : files)
					deleteFile(fc);
			}
		}
		if (! f.delete())
			throw new Forbidden("Could not delete file {0}", f.getAbsolutePath()) ;
	}

	private static BufferedReader getReader(File f) throws IOException {
		return new BufferedReader(new InputStreamReader(new FileInputStream(f), Charset.defaultCharset()));
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
