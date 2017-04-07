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

import static javax.servlet.http.HttpServletResponse.*;
import static org.apache.juneau.dto.html5.HtmlBuilder.*;

import java.sql.*;
import java.util.*;

import org.apache.juneau.dto.*;
import org.apache.juneau.dto.html5.*;
import org.apache.juneau.ini.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.annotation.Body;

/**
 * Sample resource that shows how Juneau can serialize ResultSets.
 */
@RestResource(
	path="/sqlQuery",
	messages="nls/SqlQueryResource",
	title="SQL query service",
	description="Executes queries against the local derby '$C{SqlQueryResource/connectionUrl}' database",
	pageLinks="{up:'$R{requestParentURI}',options:'?method=OPTIONS',source:'$C{Source/gitHub}/org/apache/juneau/examples/rest/SqlQueryResource.java'}"
)
public class SqlQueryResource extends Resource {
	private static final long serialVersionUID = 1L;

	private String driver, connectionUrl;
	private boolean allowUpdates, allowTempUpdates, includeRowNums;

	@Override /* RestServlet */
	public synchronized void init(RestConfig servletConfig) throws Exception {
		super.init(servletConfig);
		ConfigFile cf = servletConfig.getConfigFile();

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

	/** GET request handler - Display the query entry page. */
	@RestMethod(name="GET", path="/", summary="Display the query entry page")
	public Div doGet(RestRequest req) {
		return div(
			script("text/javascript",
				"\n	// Quick and dirty function to allow tabs in textarea."
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
				+"\n	}"
			),
			form("sqlQuery").method("POST").target("buf").children(
				table(
					tr(
						th("Position (1-10000):"),
						td(input().name("pos").type("number").value(1)),
						th("Limit (1-10000):"),
						td(input().name("limit").type("number").value(100)),
						td(button("submit", "Submit"), button("reset", "Reset"))
					),
					tr(
						td().colspan(5).children(
							textarea().name("sql").style("width:100%;height:200px;font-family:Courier;font-size:9pt;").onkeydown("checkTab(event)")
						)
					)
				)
			),
			br(),
			div().id("results"),
			iframe().name("buf").style("display:none").onload("parent.loadResults(this)")
		);
	}

	/** POST request handler - Execute the query. */
	@RestMethod(name="POST", path="/", summary="Execute one or more queries")
	public List<Object> doPost(@Body PostInput in) throws Exception {

		List<Object> results = new LinkedList<Object>();

		// Don't try to submit empty input.
		if (StringUtils.isEmpty(in.sql))
			return results;
		
		System.err.println("SQL=["+in.sql+"]");

		if (in.pos < 1 || in.pos > 10000)
			throw new RestException(SC_BAD_REQUEST, "Invalid value for position.  Must be between 1-10000");
		if (in.limit < 1 || in.limit > 10000)
			throw new RestException(SC_BAD_REQUEST, "Invalid value for limit.  Must be between 1-10000");

		// Create a connection and statement.
		// If these fais, let the exception filter up as a 500 error.
		Connection c = DriverManager.getConnection(connectionUrl);
		c.setAutoCommit(false);
		Statement st = c.createStatement();
		String sql = null;

		try {
			for (String s : in.sql.split(";")) {
				sql = s.trim();
				if (! sql.isEmpty()) {
					Object o = null;
					if (allowUpdates || (allowTempUpdates && ! sql.matches("(?:i)commit.*"))) {
						if (st.execute(sql)) {
							ResultSet rs = st.getResultSet();
							o = new ResultSetList(rs, in.pos, in.limit, includeRowNums);
						} else {
							o = st.getUpdateCount();
						}
					} else {
						ResultSet rs = st.executeQuery(sql);
						o = new ResultSetList(rs, in.pos, in.limit, includeRowNums);
					}
					results.add(o);
				}
			}
			if (allowUpdates)
				c.commit();
			else if (allowTempUpdates)
				c.rollback();
		} catch (SQLException e) {
			c.rollback();
			throw new RestException(SC_BAD_REQUEST, "Invalid query:  {0}", sql).initCause(e);
		} finally {
			c.close();
		}

		return results;
	}

	/** The parsed form post */
	public static class PostInput {
		public String sql;
		public int pos = 1, limit = 100;
	}
}
