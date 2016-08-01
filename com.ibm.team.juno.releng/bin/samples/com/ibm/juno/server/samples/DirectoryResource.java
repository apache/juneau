/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.server.samples;

import static com.ibm.juno.core.html.HtmlDocSerializerProperties.*;
import static com.ibm.juno.core.html.HtmlSerializerProperties.*;
import static com.ibm.juno.server.RestServletProperties.*;
import static java.util.logging.Level.*;
import static javax.servlet.http.HttpServletResponse.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

import javax.servlet.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.utils.*;
import com.ibm.juno.microservice.*;
import com.ibm.juno.server.*;
import com.ibm.juno.server.annotation.*;
import com.ibm.juno.server.annotation.Properties;
import com.ibm.juno.server.converters.*;

/**
 * Sample REST resource for exploring local file systems.
 */
@RestResource(
	messages="nls/DirectoryResource",
	properties={
		@Property(name=HTML_uriAnchorText, value=PROPERTY_NAME),
		@Property(name=HTMLDOC_links, value="{up:'$R{requestParentURI}',options:'?method=OPTIONS',source:'$R{servletParentURI}/source?classes=(com.ibm.juno.server.samples.DirectoryResource)'}"),
		@Property(name=REST_allowMethodParam, value="*"),
		@Property(name="rootDir", value="$S{java.io.tmpdir}"),
		@Property(name="allowViews", value="false"),
		@Property(name="allowDeletes", value="false"),
		@Property(name="allowPuts", value="false")
	}
)
public class DirectoryResource extends Resource {
	private static final long serialVersionUID = 1L;

	protected File rootDir;     // The root directory

	// Settings enabled through servlet init parameters
	protected boolean allowDeletes, allowPuts, allowViews;

	private static Logger logger = Logger.getLogger(DirectoryResource.class.getName());

	@Override /* Servlet */
	public void init() throws ServletException {
		ObjectMap p = getProperties();
		rootDir = new File(p.getString("rootDir"));
		allowViews = p.getBoolean("allowViews", false);
		allowDeletes = p.getBoolean("allowDeletes", false);
		allowPuts = p.getBoolean("allowPuts", false);
	}

	/** Returns the root directory defined by the 'rootDir' init parameter */
	protected File getRootDir() {
		if (rootDir == null) {
			rootDir = new File(getProperties().getString("rootDir"));
			if (! rootDir.exists())
				if (! rootDir.mkdirs())
					throw new RuntimeException("Could not create root dir");
		}
		return rootDir;
	}

	/** GET request handler */
	@RestMethod(name="GET", path="/*", converters={Queryable.class})
	public Object doGet(RestRequest req, @Properties ObjectMap properties) throws Exception {

		String pathInfo = req.getPathInfo();
		File f = pathInfo == null ? rootDir : new File(rootDir.getAbsolutePath() + pathInfo);

		if (!f.exists())
			throw new RestException(SC_NOT_FOUND, "File not found");

		properties.put("path", f.getAbsolutePath());

		if (f.isDirectory()) {
			List<FileResource> l = new LinkedList<FileResource>();
			for (File fc : f.listFiles()) {
				URL fUrl = new URL(req.getRequestURL().append("/").append(fc.getName()).toString());
				l.add(new FileResource(fc, fUrl));
			}
			return l;
		}

		return new FileResource(f, new URL(req.getRequestURL().toString()));
	}

	/** DELETE request handler */
	@RestMethod(name="DELETE", path="/*", guards=AdminGuard.class)
	public Object doDelete(RestRequest req) throws Exception {

		if (! allowDeletes)
			throw new RestException(SC_METHOD_NOT_ALLOWED, "DELETE not enabled");

		File f = new File(rootDir.getAbsolutePath() + req.getPathInfo());
		deleteFile(f);

		if (req.getHeader("Accept").contains("text/html"))
			return new Redirect();
		return "File deleted";
	}

	/** PUT request handler */
	@RestMethod(name="PUT", path="/*", guards=AdminGuard.class)
	public Object doPut(RestRequest req) throws Exception {

		if (! allowPuts)
			throw new RestException(SC_METHOD_NOT_ALLOWED, "PUT not enabled");

		File f = new File(rootDir.getAbsolutePath() + req.getPathInfo());
		String parentSubPath = f.getParentFile().getAbsolutePath().substring(rootDir.getAbsolutePath().length());
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(f));
		IOPipe.create(req.getInputStream(), bos).closeOut().run();
		if (req.getContentType().contains("html"))
			return new Redirect(parentSubPath);
		return "File added";
	}

	/** VIEW request handler (overloaded GET for viewing file contents) */
	@RestMethod(name="VIEW", path="/*")
	public void doView(RestRequest req, RestResponse res) throws Exception {

		if (! allowViews)
			throw new RestException(SC_METHOD_NOT_ALLOWED, "VIEW not enabled");

		File f = new File(rootDir.getAbsolutePath() + req.getPathInfo());

		if (!f.exists())
			throw new RestException(SC_NOT_FOUND, "File not found");

		if (f.isDirectory())
			throw new RestException(SC_METHOD_NOT_ALLOWED, "VIEW not available on directories");

		res.setOutput(new FileReader(f)).setContentType("text/plain");
	}

	/** DOWNLOAD request handler (overloaded GET for downloading file contents) */
	@RestMethod(name="DOWNLOAD")
	public void doDownload(RestRequest req, RestResponse res) throws Exception {

		if (! allowViews)
			throw new RestException(SC_METHOD_NOT_ALLOWED, "DOWNLOAD not enabled");

		File f = new File(rootDir.getAbsolutePath() + req.getPathInfo());

		if (!f.exists())
			throw new RestException(SC_NOT_FOUND, "File not found");

		if (f.isDirectory())
			throw new RestException(SC_METHOD_NOT_ALLOWED, "DOWNLOAD not available on directories");

		res.setOutput(new FileReader(f)).setContentType("application");
	}

	/** File POJO */
	public class FileResource {
		private File f;
		private URL url;

		/** Constructor */
		public FileResource(File f, URL url) {
			this.f = f;
			this.url = url;
		}

		// Bean property getters

		public URL getUrl() {
			return url;
		}

		public String getType() {
			return (f.isDirectory() ? "dir" : "file");
		}

		public String getName() {
			return f.getName();
		}

		public long getSize() {
			return f.length();
		}

		public Date getLastModified() {
			return new Date(f.lastModified());
		}

		public URL getView() throws Exception {
			if (allowViews && f.canRead() && ! f.isDirectory())
				return new URL(url + "?method=VIEW");
			return null;
		}

		public URL getDownload() throws Exception {
			if (allowViews && f.canRead() && ! f.isDirectory())
				return new URL(url + "?method=DOWNLOAD");
			return null;
		}

		public URL getDelete() throws Exception {
			if (allowDeletes && f.canWrite())
				return new URL(url + "?method=DELETE");
			return null;
		}
	}

	/** Utility method */
	private void deleteFile(File f) {
		try {
			if (f.isDirectory())
				for (File fc : f.listFiles())
					deleteFile(fc);
			f.delete();
		} catch (Exception e) {
			logger.log(WARNING, "Cannot delete file '" + f.getAbsolutePath() + "'", e);
		}
	}
}
