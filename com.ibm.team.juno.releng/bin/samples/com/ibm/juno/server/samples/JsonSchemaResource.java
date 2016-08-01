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

import com.ibm.juno.core.dto.jsonschema.*;
import com.ibm.juno.microservice.*;
import com.ibm.juno.server.annotation.*;

/**
 * Sample resource that shows how to serialize JSON-Schema documents.
 */
@RestResource(
	path="/jsonSchema",
	messages="nls/JsonSchemaResource",
	properties={
		@Property(name=HTMLDOC_title, value="Sample JSON-Schema document"),
		@Property(name=HTMLDOC_links, value="{up:'$R{requestParentURI}',options:'?method=OPTIONS',source:'$R{servletParentURI}/source?classes=(com.ibm.juno.server.samples.JsonSchemaResource)'}")
	}
)
public class JsonSchemaResource extends ResourceJena {
	private static final long serialVersionUID = 1L;

	private Schema schema;     // The schema document

	@Override /* Servlet */
	public void init() {

		try {
			schema = new Schema()
				.setId("http://example.com/sample-schema#")
				.setSchemaVersionUri("http://json-schema.org/draft-04/schema#")
				.setTitle("Example Schema")
				.setType(JsonType.OBJECT)
				.addProperties(
					new SchemaProperty("firstName", JsonType.STRING),
					new SchemaProperty("lastName", JsonType.STRING),
					new SchemaProperty("age", JsonType.INTEGER)
						.setDescription("Age in years")
						.setMinimum(0)
				)
				.addRequired("firstName", "lastName");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/** GET request handler */
	@RestMethod(name="GET", path="/")
	public Schema getSchema() throws Exception {
		return schema;
	}

	/**
	 * PUT request handler.
	 * Replaces the schema document with the specified content, and then mirrors it as the response.
	 */
	@RestMethod(name="PUT", path="/")
	public Schema setSchema(@Content Schema schema) throws Exception {
		this.schema = schema;
		return schema;
	}
}
