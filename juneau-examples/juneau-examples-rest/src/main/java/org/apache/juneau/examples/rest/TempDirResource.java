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

import static org.apache.juneau.dto.html5.HtmlBuilder.*;
import static org.apache.juneau.http.HttpMethodName.*;

import java.io.*;

import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.servlet.*;
import org.apache.juneau.dto.html5.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.widget.*;
import org.apache.juneau.utils.*;

/**
 * Sample resource that extends {@link DirectoryResource} to open up the temp directory as a REST resource.
 */
@RestResource(
	path="/tempDir",
	title="Temp Directory View Service",
	description="View and download files in the '$S{java.io.tmpdir}' directory.",
	htmldoc=@HtmlDoc(
		widgets={
			ContentTypeMenuItem.class,
			StyleMenuItem.class
		},
		links={
			"up: request:/..",
			"options: servlet:/?method=OPTIONS",
			"upload: servlet:/upload",
			"$W{ContentTypeMenuItem}",
			"$W{StyleMenuItem}",
			"source: $C{Source/gitHub}/org/apache/juneau/examples/rest/$R{servletClassSimple}.java"
		},
		aside={
			"<div style='max-width:400px' class='text'>",
			"	<p>Shows how to use the predefined DirectoryResource class.</p>",
			"	<p>Also shows how to use HTML5 beans to create a form entry page.</p>",
			"</div>"
		}
	),
	properties={
		@Property(name="rootDir", value="$S{java.io.tmpdir}"),
		@Property(name="allowViews", value="true"),
		@Property(name="allowDeletes", value="true"),
		@Property(name="allowPuts", value="false")
	}
)
public class TempDirResource extends DirectoryResource {
	private static final long serialVersionUID = 1L;

	/**
	 * [GET /upload] - Display the form entry page for uploading a file to the temp directory.
	 */
	@RestMethod(name=GET, path="/upload")
	public Form getUploadForm() {
		return
			form().id("form").action("servlet:/upload").method(POST).enctype("multipart/form-data")
			.children(
				input().name("contents").type("file"),
				button("submit", "Submit")
			)
		;
	}

	/**
	 * [POST /upload] - Upload a file as a multipart form post.
	 * Shows how to use the Apache Commons ServletFileUpload class for handling multi-part form posts.
	 */
	@RestMethod(name=POST, path="/upload", matchers=TempDirResource.MultipartFormDataMatcher.class)
	public Redirect uploadFile(RestRequest req) throws Exception {
		ServletFileUpload upload = new ServletFileUpload();
		FileItemIterator iter = upload.getItemIterator(req);
		while (iter.hasNext()) {
			FileItemStream item = iter.next();
			if (item.getFieldName().equals("contents")) {
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
			return contentType != null && contentType.startsWith("multipart/form-data");
		}
	}
}