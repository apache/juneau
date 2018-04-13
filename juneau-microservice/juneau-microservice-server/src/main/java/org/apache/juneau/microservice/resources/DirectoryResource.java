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
import static org.apache.juneau.html.HtmlDocSerializer.*;
import static org.apache.juneau.http.HttpMethodName.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.rest.annotation.HookEvent.*;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.logging.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.dto.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.converters.*;
import org.apache.juneau.rest.exception.*;
import org.apache.juneau.rest.helper.*;
import org.apache.juneau.transforms.*;
import org.apache.juneau.utils.*;

/**
 * REST resource that allows access to a file system directory.
 * 
 * <p>
 * The root directory is specified in one of two ways:
 * <ul class='spaced-list'>
 * 	<li>
 * 		Specifying the location via a <l>DirectoryResource.rootDir</l> property.
 * 	<li>
 * 		Overriding the {@link #getRootDir()} method.
 * </ul>
 * 
 * <p>
 * Read/write access control is handled through the following properties:
 * <ul class='spaced-list'>
 * 	<li>
 * 		<l>DirectoryResource.allowViews</l> - If <jk>true</jk>, allows view and download access to files.
 * 	<li>
 * 		<l>DirectoryResource.allowPuts</l> - If <jk>true</jk>, allows files to be created or overwritten.
 * 	<li>
 * 		<l>DirectoryResource.allowDeletes</l> - If <jk>true</jk>, allows files to be deleted.
 * </ul>
 * 
 * <p>
 * Access can also be controlled by overriding the {@link #checkAccess(RestRequest)} method.
 */
@RestResource(
	title="File System Explorer",
	messages="nls/DirectoryResource",
	htmldoc=@HtmlDoc(
		navlinks={
			"up: request:/..",
			"options: servlet:/?method=OPTIONS"
		}
	),
	allowedMethodParams="*",
	properties={
		@Property(name=HTML_uriAnchorText, value="PROPERTY_NAME"),
		@Property(name="DirectoryResource.rootDir", value="")
	}
)
@SuppressWarnings("javadoc")
public class DirectoryResource extends BasicRestServlet {
	private static final long serialVersionUID = 1L;

	private File rootDir;     // The root directory

	// Settings enabled through servlet init parameters
	boolean allowDeletes, allowPuts, allowViews;

	private static Logger logger = Logger.getLogger(DirectoryResource.class.getName());

	@RestHook(INIT)
	public void init(RestContextBuilder b) throws Exception { 
		RestContextProperties p = b.getProperties();
		rootDir = new File(p.getString("DirectoryResource.rootDir"));
		allowViews = p.getBoolean("DirectoryResource.allowViews", false);
		allowDeletes = p.getBoolean("DirectoryResource.allowDeletes", false);
		allowPuts = p.getBoolean("DirectoryResource.allowPuts", false);
	}

	/**
	 * Returns the root directory defined by the 'rootDir' init parameter.
	 * 
	 * <p>
	 * Subclasses can override this method to provide their own root directory.
	 * 
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

	@RestMethod(
		name=GET, 
		path="/*",
		summary="View files on directory",
		description="Returns a listing of all files in the specified directory.",
		converters={Queryable.class}
	)
	public FileListing listFiles(@PathRemainder String path) throws NotFound, Exception {
		FileListing l = new FileListing();
		for (File fc : getDir(path).listFiles()) 
			l.add(new FileResource(fc, (path != null ? (path + '/') : "") + urlEncode(fc.getName())));
		return l;
	}
	
	@RestMethod(
		name=GET, 
		path="/file/*",
		summary="View information about file",
		description="Returns detailed information about the specified file."
	)
	public FileResource getFileInfo(@PathRemainder String path) throws NotFound, Exception {
		return new FileResource(getFile(path), path);
	}

	@RestMethod(
		name=DELETE, 
		path="/file/*",
		summary="Delete file",
		description="Delete a file on the file system."
	)
	public RedirectToRoot deleteFile(@PathRemainder String path) throws MethodNotAllowed {
		if (! allowDeletes)
			throw new MethodNotAllowed("DELETE not enabled");
		deleteFile(getFile(path));
		return new RedirectToRoot();
	}

	@RestMethod(
		name=PUT, 
		path="/file/*",
		summary="Add or replace file",
		description="Add or overwrite a file on the file system."
	)
	public RedirectToRoot updateFile(
		@Body(schema="{type:'string',format:'binary'}") InputStream is, 
		@PathRemainder String path
	) throws InternalServerError {
		
		if (! allowPuts)
			throw new MethodNotAllowed("PUT not enabled");

		File f = getFile(path);
		
		try (OutputStream os = new BufferedOutputStream(new FileOutputStream(f))) {
			IOPipe.create(is, os).run();
		} catch (IOException e) {
			throw new InternalServerError(e);
		}
		
		return new RedirectToRoot();
	}

	@RestMethod(
		name="VIEW", 
		path="/file/*",
		summary="View contents of file",
		description="View the contents of a file."
	)
	public FileContents doView(RestResponse res, @PathRemainder String path) throws NotFound, MethodNotAllowed {
		if (! allowViews)
			throw new MethodNotAllowed("VIEW not enabled");

		res.setContentType("text/plain");
		try {
			return new FileContents(getFile(path));
		} catch (FileNotFoundException e) {
			throw new NotFound("File not found");
		}
	}
	
	@RestMethod(
		name="DOWNLOAD", 
		path="/file/*",
		summary="Download file",
		description="Download the contents of a file"
	)
	public FileContents doDownload(RestResponse res, @PathRemainder String path) throws NotFound, MethodNotAllowed {
		if (! allowViews)
			throw new MethodNotAllowed("DOWNLOAD not enabled");

		res.setContentType("application/octet-stream");
		try {
			return new FileContents(getFile(path));
		} catch (FileNotFoundException e) {
			throw new NotFound("File not found");
		}
	}

	private File getFile(String path) throws NotFound {
		File f = new File(rootDir.getAbsolutePath() + '/' + path);
		if (f.exists() && f.isFile())
			return f;
		throw new NotFound("File not found.");
	}
	
	private File getDir(String path) throws NotFound {
		if (path == null)
			return rootDir;
		File f = new File(rootDir.getAbsolutePath() + '/' + path);
		if (f.exists() && f.isDirectory())
			return f;
		throw new NotFound("Directory not found.");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helper beans
	//-----------------------------------------------------------------------------------------------------------------

	@SuppressWarnings("serial")
	@ResponseInfo(description="Directory listing")
	static class FileListing extends ArrayList<FileResource> {}
	
	@ResponseInfo(schema="{schema:{type:'string',format:'binary'}}", description="Contents of file")
	static class FileContents extends FileReader {
		public FileContents(File file) throws FileNotFoundException {
			super(file);
		}
	}
	
	@ResponseInfo(description="Redirect to root page on success")
	static class RedirectToRoot extends RedirectToServletRoot {}

	@ResponseInfo(description="File action")
	public static class Action extends LinkString {
		public Action(String name, String uri, Object...uriArgs) {
			super(name, uri, uriArgs);
		}
	}

	@ResponseInfo(description="File or directory details")
	public class FileResource {
		private File f;
		private String path;

		public FileResource(File f, String path) {
			this.f = f;
			this.path = path;
		}

		// Bean property getters

		public URI getUri() {
			if (f.isDirectory())
				return URI.create("servlet:/"+path);
			return URI.create("servlet:/file/"+path);
		}

		public String getType() {
			return (f.isDirectory() ? "dir" : "file");
		}

		public String getName() {
			return f.getName();
		}

		public long getSize() {
			return f.isDirectory() ? f.listFiles().length : f.length();
		}

		@Swap(DateSwap.ISO8601DTP.class)
		public Date getLastModified() {
			return new Date(f.lastModified());
		}

		public List<Action> getActions() throws Exception {
			List<Action> l = new ArrayList<>();
			if (allowViews && f.canRead() && ! f.isDirectory()) {
				l.add(new Action("view", getUri().toString() + "?method=VIEW"));
				l.add(new Action("download", getUri().toString() + "?method=DOWNLOAD"));
			}
			if (allowDeletes && f.canWrite() && ! f.isDirectory())
				l.add(new Action("delete", getUri().toString() + "?method=DELETE"));
			return l;
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
