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
package org.apache.juneau.examples.core.dto;

import org.apache.juneau.*;
import org.apache.juneau.dto.atom.Feed;
import org.apache.juneau.dto.swagger.Swagger;
import org.apache.juneau.html.HtmlSerializer;
import org.apache.juneau.json.*;

import static org.apache.juneau.dto.atom.AtomBuilder.*;
import static org.apache.juneau.dto.atom.AtomBuilder.link;
import static org.apache.juneau.dto.html5.HtmlBuilder.*;
import static org.apache.juneau.dto.swagger.SwaggerBuilder.*;

import java.net.*;

/**
 * Sample class which shows the usage of DTO module which is a
 * Sub module of the core.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class DtoExample {

	/**
	 * DTO Samples
	 *
	 * @param args Unused.
	 * @throws Exception Unused.
	 */
	@SuppressWarnings("unused")
	public static void main(String[] args) throws Exception {

		//Produces
		/**
		 * <table>
		 * <tr>
		 * <th>c1</th>
		 * <th>c2</th>
		 * </tr>
		 * <tr>
		 * <td>v1</td>
		 * <td>v2</td>
		 * </tr>
		 * </table>
		 */
		Object mytable =
			table(
				tr(
					th("c1"),
					th("c2")
				),
				tr(
					td("v1"),
					td("v2")
				)
			);

		String html = HtmlSerializer.DEFAULT.serialize(mytable);

		Object mainJsp =
			form().action("main.jsp").method("GET")
			.children(
				input("text").name("first_name").value("apache"), br(),
				input("text").name("last_name").value("juneau"), br(),
				button("submit", "Submit"),
				button("reset", "Reset")
			);

		/**
		 * <form action='main.jsp' method='POST'>
		 * Position (1-10000): <input name='pos' type='number'
		 * value='1'/><br/>
		 * Limit (1-10000): <input name='pos' type='number'
		 * value='100'/><br/>
		 * <button type='submit'>Submit</button>
		 * <button type='reset'>Reset</button>
		 * </form>
		 */
		html = HtmlSerializer.DEFAULT.serialize(mainJsp);

		/**
		 * Produces
		 * {
		 *    a:{action:'main.jsp',method:'GET'},
		 *    c:[
		 *    {a:{type:'text',name:'first_name',value:'apache'}},{},
		 *    {a:{type:'text',name:'last_name',value:'juneau'}},{},
		 *    {a:{type:'submit'},c:['Submit']},
		 *    {a:{type:'reset'},c:['Reset']}
		 *    ]
		 * }
		 */
		html =  Json5Serializer.DEFAULT.serialize(mainJsp);

		Feed feed =
			feed("tag:foo.org", "Title", "2016-12-31T05:02:03Z")
			.setSubtitle(text("html").setText("Subtitle"))
			.setLinks(
				link("alternate", "text/html", "http://foo.org/").setHreflang("en"),
				link("self", "application/atom+xml", "http://foo.org/feed.atom")
			)
			.setGenerator(
				generator("Example Toolkit").setUri("http://www.foo.org/").setVersion("1.0")
			)
			.setEntries(
				entry("tag:foo.org", "Title", "2016-12-31T05:02:03Z")
				.setLinks(
					link("alternate", "text/html", "http://foo.org/2005/04/02/atom"),
					link("enclosure", "audio/mpeg", "http://foo.org/audio/foobar.mp3").setLength(1337)
				)
				.setPublished("2016-12-31T05:02:03Z")
				.setAuthors(
					person("John Smith").setUri(new URI("http://foo.org/")).setEmail("foo@foo.org")
				)
				.setContributors(
					person("John Smith"),
					person("Jane Smith")
				)
				.setContent(
					content("xhtml")
					.setLang("en")
					.setBase("http://foo.org/")
					.setText("<div><p><i>[Sample content]</i></p></div>")
				)
			);

		Swagger swagger = swagger()
			.setSwagger("2.0")
			.setInfo(
				info("Swagger Petstore", "1.0.0")
				.setDescription("This is a sample server Petstore server.")
				.setTermsOfService("http://swagger.io/terms/")
				.setContact(
					contact().setEmail("apiteam@swagger.io")
				)
				.setLicense(
					license("Apache 2.0").setUrl(URI.create("http://www.apache.org/licenses/LICENSE-2.0.html"))
				)
			)
			.addPath("/pet", "post",
				operation()
				.setTags("pet")
				.setSummary("Add a new pet to the store")
				.setDescription("")
				.setOperationId("addPet")
				.setConsumes(MediaType.JSON, MediaType.XML)
				.setProduces(MediaType.JSON, MediaType.XML)
				.setParameters(
					parameterInfo("body", "body")
					.setDescription("Pet object that needs to be added to the store")
					.setRequired(true)
				)
				.addResponse("405", responseInfo("Invalid input"))
			);

		// Serialize to Swagger/JSON
		/**
		 * Produces
		 * {
		 *  "swagger": "2.0",
		 *  "info": {
		 *      "title": "Swagger Petstore",
		 *      "description": "This is a sample server Petstore server.",
		 *      "version": "1.0.0",
		 *      "termsOfService": "http://swagger.io/terms/",
		 *      "contact": {
		 *          "email": "apiteam@swagger.io"
		 *      },
		 *      "license": {
		 *          "name": "Apache 2.0",
		 *          "url": "http://www.apache.org/licenses/LICENSE-2.0.html"
		 *      }
		 *  },
		 * "paths": {
		 *      "/pet": {
		 *          "post": {
		 *              "tags": [
		 *                  "pet"
		 *               ],
		 *              "summary": "Add a new pet to the store",
		 *              "description": "",
		 *              "operationId": "addPet",
		 *              "consumes": [
		 *                  "application/json",
		 *                  "text/xml"
		 *              ],
		 *              "produces": [
		 *                  "application/json",
		 *                  "text/xml"
		 *              ],
		 *              "parameters": [
		 *                  {
		 *                      "in": "body",
		 *                      "name": "body",
		 *                      "description": "Pet object that needs to be added to the store",
		 *                      "required": true
		 *                  }
		 *              ],
		 *              "responses": {
		 *                  "405": {
		 *                      "description": "Invalid input"
		 *                  }
		 *              }
		 *         }
		 *      }
		 *  },
		 *  }
		 */
		String swaggerJson = JsonSerializer.DEFAULT_READABLE.serialize(swagger);

	}
}