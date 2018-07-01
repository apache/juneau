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
import static org.apache.juneau.rest.annotation.HookEvent.*;
import static org.apache.juneau.microservice.resources.DirectoryResource.*;

import java.io.*;

import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.servlet.*;
import org.apache.commons.io.*;
import org.apache.juneau.dto.html5.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.microservice.resources.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.helper.*;
import org.apache.juneau.rest.widget.*;
import org.apache.juneau.utils.*;

/**
 * Sample resource that extends {@link DirectoryResource} to open up the temp directory as a REST resource.
 */
@RestResource(
	path="/tempDir",
	title="Temp Directory View Service",
	description="View and download files in the '$R{rootDir}' directory.",
	htmldoc=@HtmlDoc(
		widgets={
			ContentTypeMenuItem.class,
			ThemeMenuItem.class
		},
		navlinks={
			"up: request:/..",
			"options: servlet:/?method=OPTIONS",
			"upload: servlet:/upload",
			"$W{ContentTypeMenuItem}",
			"$W{ThemeMenuItem}",
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
		@Property(name=DIRECTORY_RESOURCE_rootDir, value="$C{TempDirResource/dir,$S{java.io.tmpdir}}"),
		@Property(name=DIRECTORY_RESOURCE_allowViews, value="true"),
		@Property(name=DIRECTORY_RESOURCE_allowDeletes, value="true"),
		@Property(name=DIRECTORY_RESOURCE_allowUploads, value="false")
	},
	swagger=@ResourceSwagger(
		contact=@Contact(name="Juneau Developer",email="dev@juneau.apache.org"),
		license=@License(name="Apache 2.0",url="http://www.apache.org/licenses/LICENSE-2.0.html"),
		version="2.0",
		termsOfService="You are on your own.",
		externalDocs=@ExternalDocs(description="Apache Juneau",url="http://juneau.apache.org")
	)
)
public class TempDirResource extends DirectoryResource {
	private static final long serialVersionUID = 1L;

	@Override /* DirectoryResource */
	@RestHook(INIT)
	public void init(RestContextBuilder b) throws Exception { 
		super.init(b);
		File rootDir = getRootDir();
		if (! rootDir.exists()) {
			rootDir.mkdirs();
			
			// Make some dummy files.
			FileUtils.touch(new File(rootDir, "A.txt"));
			FileUtils.touch(new File(rootDir, "B.txt"));
			FileUtils.touch(new File(rootDir, "C.txt"));
		}
	}
	
	@RestMethod(
		name=GET, 
		path="/upload", 
		summary="Upload file form entry page", 
		description="Renders an example form page for uploading a file in multipart/form-data format to the temp directory."
	)
	public Form getUploadForm() {
		return
			form().id("form").action("servlet:/upload").method(POST).enctype("multipart/form-data")
			.children(
				input().name("contents").type("file"),
				button("submit", "Submit")
			)
		;
	}

	@RestMethod(
		name=POST, 
		path="/upload", 
		summary="Upload a file as a multipart form post",
		description= {
			"Shows how to use the Apache Commons ServletFileUpload class for handling multi-part form posts.\n",
			"Matcher ensures Java method is called only when Content-Type is multipart/form-data."
		},
		matchers=TempDirResource.MultipartFormDataMatcher.class
	)
	public RedirectToServletRoot uploadFile(RestRequest req) throws Exception {
		ServletFileUpload upload = new ServletFileUpload();
		FileItemIterator iter = upload.getItemIterator(req);
		while (iter.hasNext()) {
			FileItemStream item = iter.next();
			if (item.getFieldName().equals("contents")) {
				File f = new File(getRootDir(), item.getName());
				try (FileOutputStream fos = new FileOutputStream(f)) {
					IOPipe.create(item.openStream(), fos).run();
				}
			}
		}
		return RedirectToServletRoot.INSTANCE; 
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