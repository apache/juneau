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

import static java.util.logging.Level.*;
import static javax.servlet.http.HttpServletResponse.*;
import static org.apache.juneau.html.HtmlDocSerializerContext.*;
import static org.apache.juneau.rest.RestServletContext.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

import javax.servlet.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.converters.*;
import org.apache.juneau.transforms.*;
import org.apache.juneau.utils.*;

/**
 * REST resource that allows access to a file system directory.
 * <p>
 * The root directory is specified in one of two ways:
 * </p>
 * <ul class='spaced-list'>
 * 	<li>Specifying the location via a <l>DirectoryResource.rootDir</l> property.
 * 	<li>Overriding the {@link #getRootDir()} method.
 * </ul>
 * <p>
 * Read/write access control is handled through the following properties:
 * </p>
 * <ul class='spaced-list'>
 * 	<li><l>DirectoryResource.allowViews</l> - If <jk>true</jk>, allows view and download access to files.
 * 	<li><l>DirectoryResource.allowPuts</l> - If <jk>true</jk>, allows files to be created or overwritten.
 * 	<li><l>DirectoryResource.allowDeletes</l> - If <jk>true</jk>, allows files to be deleted.
 * </ul>
 * <p>
 * Access can also be controlled by overriding the {@link #checkAccess(RestRequest)} method.
 */
@RestResource(
	title="File System Explorer",
	description="Contents of $R{attribute.path}",
	messages="nls/DirectoryResource",
	properties={
		@Property(name=HTML_uriAnchorText, value=PROPERTY_NAME),
		@Property(name=HTMLDOC_links, value="{up:'$R{requestParentURI}',options:'?method=OPTIONS',source:'$R{servletParentURI}/source?classes=(org.apache.juneau.rest.samples.DirectoryResource)'}"),
		@Property(name=REST_allowMethodParam, value="*"),
		@Property(name="DirectoryResource.rootDir", value=""),
		@Property(name="DirectoryResource.allowViews", value="false"),
		@Property(name="DirectoryResource.allowDeletes", value="false"),
		@Property(name="DirectoryResource.allowPuts", value="false")
	}
)
public class DirectoryResource extends Resource {
	private static final long serialVersionUID = 1L;

	private File rootDir;     // The root directory

	// Settings enabled through servlet init parameters
	private boolean allowDeletes, allowPuts, allowViews;

	private static Logger logger = Logger.getLogger(DirectoryResource.class.getName());

	@Override /* Servlet */
	public void init() throws ServletException {
		ObjectMap p = getProperties();
		rootDir = new File(p.getString("DirectoryResource.rootDir"));
		allowViews = p.getBoolean("DirectoryResource.allowViews", false);
		allowDeletes = p.getBoolean("DirectoryResource.allowDeletes", false);
		allowPuts = p.getBoolean("DirectoryResource.allowPuts", false);
	}

	/**
	 * Returns the root directory defined by the 'rootDir' init parameter.
	 * Subclasses can override this method to provide their own root directory.
	 * @return The root directory.
	 */
	protected File getRootDir() {
		if (rootDir == null) {
			rootDir = new File(getProperties().getString("rootDir"));
			if (! rootDir.exists())
				if (! rootDir.mkdirs())
					throw new RuntimeException("Could not create root dir");
		}
		return rootDir;
	}

	/**
	 * [GET /*]
	 * On directories, returns a directory listing.
	 * On files, returns information about the file.
	 *
	 * @param req The HTTP request.
	 * @return Either a FileResource or list of FileResources depending on whether it's a
	 * 	file or directory.
	 * @throws Exception If file could not be read or access was not granted.
	 */
	@RestMethod(name="GET", path="/*",
		description="On directories, returns a directory listing.\nOn files, returns information about the file.",
		converters={Queryable.class}
	)
	public Object doGet(RestRequest req) throws Exception {
		checkAccess(req);

		String pathInfo = req.getPathInfo();
		File f = pathInfo == null ? rootDir : new File(rootDir.getAbsolutePath() + pathInfo);

		if (!f.exists())
			throw new RestException(SC_NOT_FOUND, "File not found");

		req.setAttribute("path", f.getAbsolutePath());

		if (f.isDirectory()) {
			List<FileResource> l = new LinkedList<FileResource>();
			File[] files = f.listFiles();
			if (files != null) {
				for (File fc : files) {
					URL fUrl = new URL(req.getRequestURL().append("/").append(fc.getName()).toString());
					l.add(new FileResource(fc, fUrl));
				}
			}
			return l;
		}

		return new FileResource(f, new URL(req.getRequestURL().toString()));
	}

	/**
	 * [DELETE /*]
	 * Delete a file on the file system.
	 *
	 * @param req The HTTP request.
	 * @return The message <js>"File deleted"</js> if successful.
	 * @throws Exception If file could not be read or access was not granted.
	 */
	@RestMethod(name="DELETE", path="/*",
		description="Delete a file on the file system."
	)
	public Object doDelete(RestRequest req) throws Exception {
		checkAccess(req);

		File f = new File(rootDir.getAbsolutePath() + req.getPathInfo());
		deleteFile(f);

		if (req.getHeader("Accept").contains("text/html"))
			return new Redirect();
		return "File deleted";
	}

	/**
	 * [PUT /*]
	 * Add or overwrite a file on the file system.
	 *
	 * @param req The HTTP request.
	 * @return The message <js>"File added"</js> if successful.
	 * @throws Exception If file could not be read or access was not granted.
	 */
	@RestMethod(name="PUT", path="/*",
		description="Add or overwrite a file on the file system."
	)
	public Object doPut(RestRequest req) throws Exception {
		checkAccess(req);

		File f = new File(rootDir.getAbsolutePath() + req.getPathInfo());
		String parentSubPath = f.getParentFile().getAbsolutePath().substring(rootDir.getAbsolutePath().length());
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(f));
		IOPipe.create(req.getInputStream(), bos).closeOut().run();
		if (req.getContentType().contains("html"))
			return new Redirect(parentSubPath);
		return "File added";
	}

	/**
	 * [VIEW /*]
	 * View the contents of a file.
	 * Applies to files only.
	 *
	 * @param req The HTTP request.
	 * @param res The HTTP response.
	 * @return A Reader containing the contents of the file.
	 * @throws Exception If file could not be read or access was not granted.
	 */
	@RestMethod(name="VIEW", path="/*",
		description="View the contents of a file.\nApplies to files only."
	)
	public Reader doView(RestRequest req, RestResponse res) throws Exception {
		checkAccess(req);

		File f = new File(rootDir.getAbsolutePath() + req.getPathInfo());

		if (!f.exists())
			throw new RestException(SC_NOT_FOUND, "File not found");

		if (f.isDirectory())
			throw new RestException(SC_METHOD_NOT_ALLOWED, "VIEW not available on directories");

		res.setContentType("text/plain");
		return new FileReader(f);
	}

	/**
	 * [DOWNLOAD /*]
	 * Download the contents of a file.
	 * Applies to files only.
	 *
	 * @param req The HTTP request.
	 * @param res The HTTP response.
	 * @return A Reader containing the contents of the file.
	 * @throws Exception If file could not be read or access was not granted.
	 */
	@RestMethod(name="DOWNLOAD", path="/*",
		description="Download the contents of a file.\nApplies to files only."
	)
	public Reader doDownload(RestRequest req, RestResponse res) throws Exception {
		checkAccess(req);

		File f = new File(rootDir.getAbsolutePath() + req.getPathInfo());

		if (!f.exists())
			throw new RestException(SC_NOT_FOUND, "File not found");

		if (f.isDirectory())
			throw new RestException(SC_METHOD_NOT_ALLOWED, "DOWNLOAD not available on directories");

		res.setContentType("application");
		return new FileReader(f);
	}

	/**
	 * Verify that the specified request is allowed.
	 * Subclasses can override this method to provide customized behavior.
	 * Method should throw a {@link RestException} if the request should be disallowed.
	 *
	 * @param req The HTTP request.
	 */
	protected void checkAccess(RestRequest req) {
		String method = req.getMethod();
		if (method.equals("VIEW") && ! allowViews)
			throw new RestException(SC_METHOD_NOT_ALLOWED, "VIEW not enabled");
		if (method.equals("PUT") && ! allowPuts)
			throw new RestException(SC_METHOD_NOT_ALLOWED, "PUT not enabled");
		if (method.equals("DELETE") && ! allowDeletes)
			throw new RestException(SC_METHOD_NOT_ALLOWED, "DELETE not enabled");
		if (method.equals("DOWNLOAD") && ! allowViews)
			throw new RestException(SC_METHOD_NOT_ALLOWED, "DOWNLOAD not enabled");
	}

	/** File POJO */
	public class FileResource {
		private File f;
		private URL url;

		/**
		 * Constructor.
		 * @param f The file.
		 * @param url The URL of the file resource.
		 */
		public FileResource(File f, URL url) {
			this.f = f;
			this.url = url;
		}

		// Bean property getters

		/**
		 * @return The URL of the file resource.
		 */
		public URL getUrl() {
			return url;
		}

		/**
		 * @return The file type.
		 */
		public String getType() {
			return (f.isDirectory() ? "dir" : "file");
		}

		/**
		 * @return The file name.
		 */
		public String getName() {
			return f.getName();
		}

		/**
		 * @return The file size.
		 */
		public long getSize() {
			return f.length();
		}

		/**
		 * @return The file last modified timestamp.
		 */
		@BeanProperty(swap=DateSwap.ISO8601DTP.class)
		public Date getLastModified() {
			return new Date(f.lastModified());
		}

		/**
		 * @return A hyperlink to view the contents of the file.
		 * @throws Exception If access is not allowed.
		 */
		public URL getView() throws Exception {
			if (allowViews && f.canRead() && ! f.isDirectory())
				return new URL(url + "?method=VIEW");
			return null;
		}

		/**
		 * @return A hyperlink to download the contents of the file.
		 * @throws Exception If access is not allowed.
		 */
		public URL getDownload() throws Exception {
			if (allowViews && f.canRead() && ! f.isDirectory())
				return new URL(url + "?method=DOWNLOAD");
			return null;
		}

		/**
		 * @return A hyperlink to delete the file.
		 * @throws Exception If access is not allowed.
		 */
		public URL getDelete() throws Exception {
			if (allowDeletes && f.canWrite())
				return new URL(url + "?method=DELETE");
			return null;
		}
	}

	/** Utility method */
	private void deleteFile(File f) {
		try {
			if (f.isDirectory()) {
				File[] files = f.listFiles();
				if (files != null) {
					for (File fc : files)
						deleteFile(fc);
				}
			}
			f.delete();
		} catch (Exception e) {
			logger.log(WARNING, "Cannot delete file '" + f.getAbsolutePath() + "'", e);
		}
	}
}
