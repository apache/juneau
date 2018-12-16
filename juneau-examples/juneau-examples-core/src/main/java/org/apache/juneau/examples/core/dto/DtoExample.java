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

import org.apache.juneau.dto.atom.Feed;
import org.apache.juneau.dto.atom.Person;
import org.apache.juneau.dto.swagger.Swagger;
import org.apache.juneau.html.HtmlSerializer;
import org.apache.juneau.http.MediaType;
import org.apache.juneau.json.JsonSerializer;
import org.apache.juneau.xml.XmlSerializer;

import static org.apache.juneau.dto.atom.AtomBuilder.*;
import static org.apache.juneau.dto.html5.HtmlBuilder.*;
import static org.apache.juneau.dto.swagger.SwaggerBuilder.*;

/**
 * Sample class which shows the usage of DTO module which is a
 * Sub module of the core.
 */
public class DtoExample {

    /**
     * DTO Samples
     * @param args
     * @throws Exception
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
        html =  JsonSerializer.create().simple().sq().build().serialize(mainJsp);

        Feed feed =
                feed("tag:juneau.apache.org", "Juneau ATOM specification", "2018-12-15T08:52:05Z")
                        .title("Example apache Juneau feed")
                        .subtitle(text("html").text("Describes <em>stuff</em> about Juneau"))
                        .links(
                                link("alternate", "text/html", "http://juneau.apache.org/").hreflang("en"),
                                link("self", "application/atom+xml", "http://juneau.apache.org/feed.atom")
                        )
                        .rights("Copyright (c) 2016, Apache Foundation")
                        .authors(new Person("Juneau_Commiter"))
                        .updated("2018-12-15T08:52:05Z")
                        .entries(
                                entry("tag:juneau.sample.com,2013:1.2345", "Juneau ATOM specification snapshot", "2016-01-02T03:04:05Z")
                                        .published("2016-01-02T03:04:05Z")
                                        .content(
                                                content("xhtml")
                                                        .lang("en")
                                                        .base("http://www.apache.org/")
                                                        .text("<div><p><i>[Update: Juneau supports ATOM.]</i></p></div>")
                                        )
                        );

        // Serialize to ATOM/XML
        /**
         * <feed>
         *     <title>Example apache Juneau feed</title>
         *     <link href="http://juneau.apache.org/" hreflang="en" rel="alternate" type="text/html"/>
         *     <link href="http://juneau.apache.org/feed.atom" rel="self" type="application/atom+xml"/>
         *     <rights>Copyright (c) 2016, Apache Foundation</rights>
         *     <author><name>Juneau_Commiter</name></author>
         *     <updated>2018-12-15T08:52:05Z</updated>
         *     <id>tag:juneau.apache.org</id>
         *     <subtitle type="html">Describes <em>stuff</em> about Juneau</subtitle>
         *     <entry>
         *          <title>Juneau ATOM specification snapshot</title>
         *          <updated>2016-01-02T03:04:05Z</updated>
         *          <id>tag:juneau.sample.com,2013:1.2345</id>
         *          <published>2016-01-02T03:04:05Z</published>
         *          <content lang="en" base="http://www.apache.org/" type="xhtml">
         *              <div><p><i>[Update: Juneau supports ATOM.]</i></p></div>
         *          </content>
         *     </entry>
         * </feed>
         */
        String atomXml = XmlSerializer.DEFAULT.serialize(feed);
        System.out.print(atomXml);

        Swagger swagger = swagger()
                .swagger("2.0")
                .info(
                        info("Swagger Petstore", "1.0.0")
                                .description("This is a sample server Petstore server.")
                                .termsOfService("http://swagger.io/terms/")
                                .contact(
                                        contact().email("apiteam@swagger.io")
                                )
                                .license(
                                        license("Apache 2.0").url("http://www.apache.org/licenses/LICENSE-2.0.html")
                                )
                )
                .path("/pet", "post",
                        operation()
                                .tags("pet")
                                .summary("Add a new pet to the store")
                                .description("")
                                .operationId("addPet")
                                .consumes(MediaType.JSON, MediaType.XML)
                                .produces(MediaType.JSON, MediaType.XML)
                                .parameters(
                                        parameterInfo("body", "body")
                                                .description("Pet object that needs to be added to the store")
                                                .required(true)
                                )
                                .response("405", responseInfo("Invalid input"))
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