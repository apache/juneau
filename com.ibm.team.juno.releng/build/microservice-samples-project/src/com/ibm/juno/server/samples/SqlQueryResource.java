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
import static javax.servlet.http.HttpServletResponse.*;

import java.io.*;
import java.sql.*;
import java.util.*;

import com.ibm.juno.core.dto.*;
import com.ibm.juno.core.ini.ConfigFile;
import com.ibm.juno.core.utils.StringUtils;
import com.ibm.juno.microservice.*;
import com.ibm.juno.server.*;
import com.ibm.juno.server.annotation.*;

/**
 * Sample resource that shows how Juno can serialize ResultSets.
 */
@RestResource(
	path="/sqlQuery",
	messages="nls/SqlQueryResource",
	properties={
		@Property(name=HTMLDOC_title, value="SQL query service"),
		@Property(name=HTMLDOC_description, value="Executes queries against the local derby '$C{SqlQueryResource/connectionUrl}' database"),
		@Property(name=HTMLDOC_links, value="{up:'$R{requestParentURI}',options:'$R{servletURI}?method=OPTIONS',source:'$R{servletParentURI}/source?classes=(com.ibm.juno.server.samples.SqlQueryResource)'}"),
	}
)
public class SqlQueryResource extends Resource {
	private static final long serialVersionUID = 1L;

	private ConfigFile cf = getConfig();
	
	private String driver = cf.getString("SqlQueryResource/driver");
	private String connectionUrl = cf.getString("SqlQueryResource/connectionUrl");
	private boolean 
		allowUpdates = cf.getBoolean("SqlQueryResource/allowUpdates", false), 
		allowTempUpdates = cf.getBoolean("SqlQueryResource/allowTempUpdates", false), 
		includeRowNums = cf.getBoolean("SqlQueryResource/includeRowNums", false);

	@Override /* Servlet */
	public void init() {
		try {
			Class.forName(driver).newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/** GET request handler - Display the query entry page. */
	@RestMethod(name="GET", path="/")
	public ReaderResource doGet(RestRequest req) throws IOException {
		return req.getReaderResource("SqlQueryResource.html", true);
	}

	/** POST request handler - Execute the query. */
	@RestMethod(name="POST", path="/")
	public List<Object> doPost(@Content PostInput in) throws Exception {

		List<Object> results = new LinkedList<Object>();

		// Don't try to submit empty input.
		if (StringUtils.isEmpty(in.sql))
			return results;

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
