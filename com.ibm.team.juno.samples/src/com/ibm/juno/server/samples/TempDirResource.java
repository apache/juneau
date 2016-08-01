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

import java.io.*;

import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.servlet.*;

import com.ibm.juno.core.utils.*;
import com.ibm.juno.server.*;
import com.ibm.juno.server.annotation.*;

/**
 * Sample resource that extends {@link DirectoryResource} to open up the temp directory as a REST resource.
 */
@RestResource(
	path="/tempDir",
	messages="nls/TempDirResource",
	properties={
		@Property(name="rootDir", value="$S{java.io.tmpdir}"),
		@Property(name="allowViews", value="true"),
		@Property(name="allowDeletes", value="true"),
		@Property(name="allowPuts", value="false"),
		@Property(name=HTMLDOC_links, value="{up:'$R{requestParentURI}',options:'$R{servletURI}?method=OPTIONS',upload:'upload',source:'$R{servletParentURI}/source?classes=(com.ibm.juno.server.samples.TempDirResource,com.ibm.juno.server.samples.DirectoryResource)'}"),
	},
	stylesheet="styles/devops.css"
)
public class TempDirResource extends DirectoryResource {
	private static final long serialVersionUID = 1L;

	/**
	 * [GET /upload] - Display the form entry page for uploading a file to the temp directory.
	 */
	@RestMethod(name="GET", path="/upload")
	public ReaderResource getUploadPage(RestRequest req) throws IOException {
		return req.getReaderResource("TempDirUploadPage.html", true);
	}

	/**
	 * [POST /upload] - Upload a file as a multipart form post.
	 * Shows how to use the Apache Commons ServletFileUpload class for handling multi-part form posts.
	 */
	@RestMethod(name="POST", path="/upload", matchers=TempDirResource.MultipartFormDataMatcher.class)
	public Redirect uploadFile(RestRequest req) throws Exception {
		ServletFileUpload upload = new ServletFileUpload();
		FileItemIterator iter = upload.getItemIterator(req);
		while (iter.hasNext()) {
			FileItemStream item = iter.next();
			if (item.getFieldName().equals("contents")) { //$NON-NLS-1$
				File f = new File(getRootDir(), item.getName());
				IOPipe.create(item.openStream(), new FileOutputStream(f)).closeOut().run();
			}
		}
		return new Redirect(); // Redirect to the servlet root.
	}

	/** Causes a 404 if POST isn't multipart/form-data */
	public static class MultipartFormDataMatcher extends RestMatcher {
		@Override /* RestMatcher */
		public boolean matches(RestRequest req) {
			String contentType = req.getContentType();
			return contentType != null && contentType.startsWith("multipart/form-data"); //$NON-NLS-1$
		}
	}
}