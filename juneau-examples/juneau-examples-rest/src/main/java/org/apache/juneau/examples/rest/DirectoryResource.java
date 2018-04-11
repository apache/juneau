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
package org.apache.juneau.examples.rest;

import static java.util.logging.Level.*;
import static org.apache.juneau.html.HtmlSerializer.*;
import static org.apache.juneau.http.HttpMethodName.*;
import static org.apache.juneau.rest.annotation.HookEvent.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.converters.*;
import org.apache.juneau.rest.exception.*;
import org.apache.juneau.rest.helper.*;
import org.apache.juneau.rest.widget.*;
import org.apache.juneau.utils.*;

/**
 * Sample REST resource for exploring local file systems.
 */
@RestResource(
	messages="nls/DirectoryResource",
	htmldoc=@HtmlDoc(
		widgets={
			ContentTypeMenuItem.class,
			StyleMenuItem.class
		},
		navlinks={
			"up: request:/..",
			"options: servlet:/?method=OPTIONS",
			"$W{ContentTypeMenuItem}",
			"$W{StyleMenuItem}",
			"source: $C{Source/gitHub}/org/apache/juneau/examples/rest/$R{servletClassSimple}.java"
		}
	),
	allowedMethodParams="*",
	properties={
		@Property(name=HTML_uriAnchorText, value="PROPERTY_NAME"),
		@Property(name="rootDir", value="$S{java.io.tmpdir}"),
		@Property(name="allowViews", value="false"),
		@Property(name="allowDeletes", value="false"),
		@Property(name="allowPuts", value="false")
	},
	swagger={
		"info: {",
			"contact:{name:'Juneau Developer',email:'dev@juneau.apache.org'},",
			"license:{name:'Apache 2.0',url:'http://www.apache.org/licenses/LICENSE-2.0.html'},",
			"version:'2.0',",
			"termsOfService:'You are on your own.'",
		"},",
		"externalDocs:{description:'Apache Juneau',url:'http://juneau.apache.org'}"
	}
)
public class DirectoryResource extends BasicRestServlet {
	private static final long serialVersionUID = 1L;

	private File rootDir;     // The root directory

	// Settings enabled through servlet init parameters
	boolean allowDeletes, allowPuts, allowViews;

	private static Logger logger = Logger.getLogger(DirectoryResource.class.getName());

	@RestHook(INIT)
	public void init(RestContextBuilder builder) throws Exception {
		RestContextProperties p = builder.getProperties();
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

	@RestMethod(
		name=GET, 
		path="/*",
		summary="Get file or directory information",
		description="Returns information about a file or directory.",
		converters={Queryable.class}
	)
	public Object doGet(RestRequest req, RequestProperties properties) throws NotFound, InternalServerError {

		String pathInfo = req.getPathInfo();
		File f = pathInfo == null ? rootDir : new File(rootDir.getAbsolutePath() + pathInfo);

		if (!f.exists())
			throw new NotFound("File not found");

		properties.put("path", f.getAbsolutePath());

		try {
			if (f.isDirectory()) {
				List<FileResource> l = new LinkedList<>();
				File[] lfc = f.listFiles();
				if (lfc != null) {
					for (File fc : lfc) {
						URL fUrl = new URL(req.getRequestURL().append("/").append(fc.getName()).toString());
						l.add(new FileResource(fc, fUrl));
					}
				}
				return l;
			}

			return new FileResource(f, new URL(req.getRequestURL().toString()));
			
		} catch (MalformedURLException e) {
			throw new InternalServerError(e);
		}
	}

	@RestMethod(
		name=DELETE, 
		path="/*", 
		summary="Delete file",
		description="Delete a file on the file system.",
		guards=AdminGuard.class
	)
	public Object doDelete(RestRequest req) throws MethodNotAllowed {

		if (! allowDeletes)
			throw new MethodNotAllowed("DELETE not enabled");

		File f = new File(rootDir.getAbsolutePath() + req.getPathInfo());
		deleteFile(f);

		if (req.getHeader("Accept").contains("text/html"))
			return new Redirect();
		return "File deleted";
	}

	@RestMethod(
		name=PUT, 
		path="/*", 
		summary="Upload file",
		description="Uploads a file to the file system.",
		guards=AdminGuard.class
	)
	public Object doPut(RestRequest req) throws MethodNotAllowed, InternalServerError, Forbidden {

		if (! allowPuts)
			throw new MethodNotAllowed("PUT not enabled");

		File f = new File(rootDir.getAbsolutePath() + req.getPathInfo());
		String parentSubPath = f.getParentFile().getAbsolutePath().substring(rootDir.getAbsolutePath().length());
		
		try (InputStream is = req.getInputStream(); OutputStream os = new BufferedOutputStream(new FileOutputStream(f))) {
			IOPipe.create(is, os).run();
		} catch (IOException e) {
			throw new InternalServerError(e);
		}
		
		if (req.getContentType().contains("html"))
			return new Redirect(parentSubPath);
		return "File added";
	}

	/** VIEW request handler (overloaded GET for viewing file contents) */
	@SuppressWarnings("resource")
	@RestMethod(
		name="VIEW", 
		path="/*",
		summary="View file",
		description="Views the contents of a file as plain text."
	)
	public void doView(RestRequest req, RestResponse res) throws MethodNotAllowed, NotFound {

		if (! allowViews)
			throw new MethodNotAllowed("VIEW not enabled");

		File f = new File(rootDir.getAbsolutePath() + req.getPathInfo());

		if (f.isDirectory())
			throw new MethodNotAllowed("VIEW not available on directories");

		try {
			res.setOutput(new FileReader(f)).setContentType("text/plain");
		} catch (FileNotFoundException e) {
			throw new NotFound("File not found");
		}
	}

	/** DOWNLOAD request handler (overloaded GET for downloading file contents) */
	@SuppressWarnings("resource")
	@RestMethod(
		name="DOWNLOAD",
		path="/*",
		summary="Download file",
		description="Download the contents of a file as an octet stream."
	)
	public void doDownload(RestRequest req, RestResponse res) throws MethodNotAllowed, NotFound {

		if (! allowViews)
			throw new MethodNotAllowed("DOWNLOAD not enabled");

		File f = new File(rootDir.getAbsolutePath() + req.getPathInfo());

		if (f.isDirectory())
			throw new MethodNotAllowed("DOWNLOAD not available on directories");

		try {
			res.setOutput(new FileReader(f)).setContentType("application");
		} catch (FileNotFoundException e) {
			throw new NotFound("File not found");
		}
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
			if (f.isDirectory()) {
				File[] lfc = f.listFiles();
				if (lfc != null)
					for (File fc : lfc)
						deleteFile(fc);
			}
			f.delete();
		} catch (Exception e) {
			logger.log(WARNING, "Cannot delete file '" + f.getAbsolutePath() + "'", e);
		}
	}
}
