/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.samples;

import static com.ibm.juno.core.html.HtmlDocSerializerProperties.*;

import java.io.*;

import com.ibm.juno.microservice.*;
import com.ibm.juno.server.*;
import com.ibm.juno.server.annotation.*;

/**
 * Service at <code>/jazz/rawSql</code>.
 * Used for executing SQL queries against the repository database.
 */
@RestResource(
	path="/codeFormatter",
	messages="nls/CodeFormatterResource",
	properties={
		@Property(name=HTMLDOC_title, value="Code Formatter"),
		@Property(name=HTMLDOC_description, value="Add syntax highlighting tags to source code"),
		@Property(name=HTMLDOC_links, value="{options:'?method=OPTIONS',source:'$R{servletParentURI}/source?classes=(com.ibm.juno.server.samples.CodeFormatterResource)'}"),
	}
)
@SuppressWarnings("serial")
public class CodeFormatterResource extends Resource {

	/** [GET /] - Display query entry page. */
	@RestMethod(name="GET", path="/")
	public ReaderResource getQueryEntryPage(RestRequest req) throws IOException {
		return req.getReaderResource("CodeFormatterResource.html", true);
	}

	/** [POST /] - Execute SQL query. */
	@RestMethod(name="POST", path="/")
	public String executeQuery(@Param("code") String code, @Param("lang") String lang) throws Exception {
		return SourceResource.highlight(code, lang);
	}
}
