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
package org.apache.juneau.examples.rest.petstore.rest;

import static org.apache.juneau.dto.html5.HtmlBuilder.*;
import static org.apache.juneau.http.HttpMethodName.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.rest.annotation.HookEvent.*;

import java.sql.*;
import java.util.*;

import org.apache.juneau.jsonschema.annotation.ExternalDocs;
import org.apache.juneau.config.*;
import org.apache.juneau.dto.*;
import org.apache.juneau.dto.html5.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.annotation.Body;
import org.apache.juneau.http.annotation.Query;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.exception.*;
import org.apache.juneau.rest.widget.*;

/**
 * Sample resource that shows how Juneau can serialize ResultSets.
 */
@RestResource(
	path="/sql",
	title="SQL query service",
	description="Executes queries against the local derby '$C{SqlQueryResource/connectionUrl}' database",
	htmldoc=@HtmlDoc(
		widgets={
			ThemeMenuItem.class
		},
		navlinks={
			"up: request:/..",
			"options: servlet:/?method=OPTIONS",
			"$W{ThemeMenuItem}",
			"source: $C{Source/gitHub}/org/apache/juneau/examples/rest/$R{servletClassSimple}.java"
		},
		aside={
			"<div style='min-width:200px' class='text'>",
			"	<p>An example of a REST interface over a relational database that serializes ResultSet objects.</p>",
			"	<p>Specify one or more queries delimited by semicolons.</p>",
			"	<h5>Examples:</h5>",
			"	<ul>",
			"		<li><a class='link' href='?sql=select+*+from+sys.systables'>Tables</a>",
			"		<li><a class='link' href='?sql=select+*+from+PetstorePet'>Pets</a>",
			"		<li><a class='link' href='?sql=select+*+from+PetstoreOrder'>Orders</a>",
			"		<li><a class='link' href='?sql=select+*+from+PetstoreUser'>Users</a>",
			"	</ul>",
			"</div>"
		}
	),
	swagger=@ResourceSwagger(
		contact=@Contact(name="Juneau Developer",email="dev@juneau.apache.org"),
		license=@License(name="Apache 2.0",url="http://www.apache.org/licenses/LICENSE-2.0.html"),
		version="2.0",
		termsOfService="You are on your own.",
		externalDocs=@ExternalDocs(description="Apache Juneau",url="http://juneau.apache.org")
	)
)
public class SqlQueryResource extends BasicRestServlet {
	private static final long serialVersionUID = 1L;

	private String driver, connectionUrl;
	private boolean allowUpdates, allowTempUpdates, includeRowNums;

	/**
	 * Initializes the registry URL and rest client.
	 *
	 * @param builder The resource config.
	 * @throws Exception
	 */
	@RestHook(INIT)
	public void initConnection(RestContextBuilder builder) throws Exception {
		Config cf = builder.getConfig();

		driver = cf.getString("SqlQueryResource/driver");
		connectionUrl = cf.getString("SqlQueryResource/connectionUrl");
		allowUpdates = cf.getBoolean("SqlQueryResource/allowUpdates", false);
		allowTempUpdates = cf.getBoolean("SqlQueryResource/allowTempUpdates", false);
		includeRowNums = cf.getBoolean("SqlQueryResource/includeRowNums", false);

		try {
			Class.forName(driver).newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@RestMethod(
		summary="Display the query entry page"
	)
	public Div get(
			@Query(name="sql", description="Text to prepopulate the SQL query field with.", example="select * from sys.systables") String sql
		) {

		return div(
			script("text/javascript",
				new String[]{"\n	// Quick and dirty function to allow tabs in textarea."
				+"\n	function checkTab(e) {"
				+"\n		if (e.keyCode == 9) {"
				+"\n			var t = e.target;"
				+"\n			var ss = t.selectionStart, se = t.selectionEnd;"
				+"\n			t.value = t.value.slice(0,ss).concat('\\t').concat(t.value.slice(ss,t.value.length));"
				+"\n			e.preventDefault();"
				+"\n		}"
				+"\n	}"
				+"\n	// Load results from IFrame into this document."
				+"\n	function loadResults(b) {"
				+"\n		var doc = b.contentDocument || b.contentWindow.document;"
				+"\n		var data = doc.getElementById('data') || doc.getElementsByTagName('body')[0];"
				+"\n		document.getElementById('results').innerHTML = data.innerHTML;"
				+"\n	}"}
			),
			form("servlet:/").method(POST).target("buf").children(
				table(
					tr(
						th("Position (1-10000):").style("white-space:nowrap"),
						td(input().name("pos").type("number").value(1)),
						th("Limit (1-10000):").style("white-space:nowrap"),
						td(input().name("limit").type("number").value(100)),
						td(button("submit", "Submit"), button("reset", "Reset"))
					),
					tr(
						td().colspan(5).children(
							textarea().name("sql").text(sql == null ? " " : sql).style("width:100%;height:200px;font-family:Courier;font-size:9pt;").onkeydown("checkTab(event)")
						)
					)
				)
			),
			br(),
			div().id("results"),
			iframe().name("buf").style("display:none").onload("parent.loadResults(this)")
		);
	}

	@RestMethod(
		summary="Execute one or more queries",
		swagger=@MethodSwagger(
			responses={
				"200:{ description:'Query results.\nEach entry in the array is a result of one query.\nEach result can be a result set (for queries) or update count (for updates).', 'x-example':[[{col1:'val1'},{col2:'val2'},{col3:'val3'}],123]}"
			}
		)
	)
	public List<Object> post(
			@Body(description="Query input", example="{sql:'select * from sys.systables',pos:1,limit:100}") PostInput in
		) throws BadRequest {

		List<Object> results = new LinkedList<>();

		// Don't try to submit empty input.
		if (isEmpty(in.sql))
			return results;

		if (in.pos < 1 || in.pos > 10000)
			throw new BadRequest("Invalid value for position.  Must be between 1-10000");
		if (in.limit < 1 || in.limit > 10000)
			throw new BadRequest("Invalid value for limit.  Must be between 1-10000");

		String sql = null;

		// Create a connection and statement.
		// If these fais, let the exception filter up as a 500 error.
		try (Connection c = DriverManager.getConnection(connectionUrl)) {
			c.setAutoCommit(false);
			try (Statement st = c.createStatement()) {
				for (String s : in.sql.split(";")) {
					sql = s.trim();
					if (! sql.isEmpty()) {
						Object o = null;
						if (allowUpdates || (allowTempUpdates && ! sql.matches("(?:i)commit.*"))) {
							if (st.execute(sql)) {
								try (ResultSet rs = st.getResultSet()) {
									o = new ResultSetList(rs, in.pos, in.limit, includeRowNums);
								}
							} else {
								o = st.getUpdateCount();
							}
						} else {
							try (ResultSet rs = st.executeQuery(sql)) {
								o = new ResultSetList(rs, in.pos, in.limit, includeRowNums);
							}
						}
						results.add(o);
					}
				}
			}
			if (allowUpdates)
				c.commit();
			else if (allowTempUpdates)
				c.rollback();
		} catch (SQLException e) {
			throw new BadRequest(e, "Invalid query:  {0}", sql);
		}

		return results;
	}

	/** The parsed form post */
	public static class PostInput {
		public String sql = "";
		public int pos = 1, limit = 100;
	}
}
