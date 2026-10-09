/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.petstore.console.ops;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.juneau.commons.beanquery.*;
import org.apache.juneau.petstore.console.*;
import org.apache.juneau.http.*;
import org.apache.juneau.petstore.service.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.datatables.*;
import org.apache.juneau.rest.server.datatables.adapter.*;
import org.apache.juneau.rest.server.view.*;
import org.apache.juneau.rest.server.view.freemarker.*;

/**
 * Audit log: a read-only, server-mode table over the store's audit trail.
 *
 * <p>
 * The table's {@code at} column takes a timestamp range as a column search value; the query layer parses the ISO-8601
 * instants.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// What the table sends as the "at" column search to show one day:</jc>
 * 	<js>"$between(2026-09-30T00:00:00Z,2026-10-01T00:00:00Z)"</js>
 *
 * 	<ja>@RestPost</ja>(path=<js>"/query"</js>)
 * 	<jk>public</jk> DataTablesResults&lt;AuditEntry&gt; query(<ja>@Content</ja> DataTablesRequest <jv>req</jv>) {
 * 		<jk>try</jk> (<jk>var</jk> <jv>s</jv> = store().queryAudit()) {
 * 			<jk>return</jk> DataTablesQuery.<jsm>run</jsm>(<jv>req</jv>, <jv>s</jv>);
 * 		}
 * 	}
 * </p>
 */
@Rest(path="/audit", title="Audit")
@SuppressWarnings({
	"java:S110" // Inheritance depth comes from the BasicRestServlet hierarchy, not this page.
})
public class AuditRest extends PetstoreConsolePage {

	private static final long serialVersionUID = 1L;

	private static final int ROWS_CAP = 200;

	/**
	 * Renders the Audit page.
	 *
	 * @return The page view.
	 */
	@RestGet(path="/")
	public View page() { return FreemarkerView.of("audit.ftlh"); }

	/**
	 * Server-mode DataTables query over the audit trail.
	 *
	 * @param req The DataTables request.
	 * @return One page of audit entries, or a 200 {@code error} envelope for a malformed search expression.
	 */
	@RestPost(path="/query")
	public DataTablesResults<AuditEntry> query(@Content DataTablesRequest req) {
		try (var s = store().queryAudit()) {
			return DataTablesQuery.run(req, s);
		} catch (BeanQuerySyntaxException e) {
			return DataTablesResults.error(req.getDraw(), e.getMessage());
		}
	}

	/**
	 * @return The newest audit entries first, capped at {@value #ROWS_CAP}.
	 */
	@RestGet(path="/rows")
	public List<AuditEntry> rows() {
		var all = tl(store().getAudit());
		Collections.reverse(all);
		return all.size() > ROWS_CAP ? all.subList(0, ROWS_CAP) : all;
	}
}
