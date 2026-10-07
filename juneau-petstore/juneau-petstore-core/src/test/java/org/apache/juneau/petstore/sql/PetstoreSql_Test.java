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
package org.apache.juneau.petstore.sql;

import static org.apache.juneau.test.bct.BctAssertions.*;

import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.beanquery.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.petstore.console.data.*;
import org.apache.juneau.petstore.dto.*;
import org.apache.juneau.petstore.service.*;
import org.apache.juneau.rest.server.datatables.*;
import org.apache.juneau.rest.server.datatables.adapter.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * SQL variant: the same DataTables requests return the same page in memory and over H2.
 */
@SuppressWarnings({
	"resource" // Each query session is closed by page()'s try-with-resources; Eclipse JDT @Owning warning is by design.
})
class PetstoreSql_Test extends TestBase {

	private static final String PET_COLUMNS = "[{\"data\":\"id\"},{\"data\":\"name\"},{\"data\":\"species\"},{\"data\":\"price\"},{\"data\":\"status\"}]";
	private static final String AUDIT_COLUMNS = "[{\"data\":\"at\"},{\"data\":\"actor\"},{\"data\":\"action\"},{\"data\":\"entity\"},{\"data\":\"entityId\"},{\"data\":\"detail\"}]";

	private static PetStore store;
	private static PetstoreSqlSchema sql;

	@BeforeAll
	static void load() throws Exception {
		store = PetstoreSeed.create().populate(new PetStore(PetstoreSeed.DEFAULT_CLOCK));
		sql = PetstoreSqlSchema.load("jdbc:h2:mem:PetstoreSql_Test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", store);
	}

	private static DataTablesRequest request(String columns, String body) throws Exception {
		return Json.to("{\"draw\":1,\"length\":10,\"columns\":" + columns + "," + body + "}", DataTablesRequest.class);
	}

	private static <T> List<Object> page(DataTablesRequest r, BeanQuerySession<T> s, Function<T,Object> id) {
		try (s) {
			var res = DataTablesQuery.run(r, s);
			assertString("", Objects.toString(res.getError(), ""));  // the query error, if any
			var out = new ArrayList<Object>();
			out.add(res.getRecordsTotal());
			out.add(res.getRecordsFiltered());
			res.getData().forEach(x -> out.add(id.apply(x)));
			return out;
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a — pets
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest(name="{0}")
	@CsvSource(delimiter='|', textBlock="""
		a01 default order       | "start":0,"order":[{"column":0,"dir":"asc"}]
		a02 third page by price | "start":20,"order":[{"column":3,"dir":"desc"},{"column":0,"dir":"asc"}]
		a03 global text search  | "start":0,"search":{"value":"frisky"},"order":[{"column":0,"dir":"asc"}]
		a04 status ribbon $eq   | "start":0,"order":[{"column":0,"dir":"asc"}],"columns":[{"data":"id"},{"data":"name"},{"data":"species"},{"data":"price"},{"data":"status","search":{"value":"$eq(AVAILABLE)"}}]
		a05 species ribbon $in  | "start":0,"order":[{"column":1,"dir":"asc"},{"column":0,"dir":"asc"}],"columns":[{"data":"id"},{"data":"name"},{"data":"species","search":{"value":"$in(DOG,CAT)"}},{"data":"price"},{"data":"status"}]
		a06 merged search       | "start":0,"search":{"value":"a"},"order":[{"column":0,"dir":"asc"}],"columns":[{"data":"id"},{"data":"name"},{"data":"species"},{"data":"price"},{"data":"status","search":{"value":"$eq(SOLD)"}}]
		a07 no match            | "start":0,"search":{"value":"zzzz-no-such-pet"},"order":[{"column":0,"dir":"asc"}]
		""")
	void a_petsMatch(String name, String body) throws Exception {
		var r = request(PET_COLUMNS, body);
		assertList(() -> "Case '" + name + "'", page(r, sql.pets().getSession(), Pet::getId), page(r, store.queryPets(), Pet::getId).toArray());
	}

	@Test void a08_defaultPageIsNotTrivial() throws Exception {
		var p = page(request(PET_COLUMNS, "\"start\":0,\"order\":[{\"column\":0,\"dir\":\"asc\"}]"), sql.pets().getSession(), Pet::getId);
		assertSize(12, p); // total, filtered, ten ids
	}

	@Test void a09_columnOverrideFilters() throws Exception {
		// The ribbon cases rely on the body's later "columns" key replacing the default one; if it didn't, both
		// sides would match unfiltered and the parity check would prove nothing.
		var p = page(request(PET_COLUMNS, "\"start\":0,\"order\":[{\"column\":0,\"dir\":\"asc\"}],\"columns\":[{\"data\":\"id\"},{\"data\":\"name\"},{\"data\":\"species\"},{\"data\":\"price\"},{\"data\":\"status\",\"search\":{\"value\":\"$eq(SOLD)\"}}]"), sql.pets().getSession(), Pet::getId);
		assertString("true", String.valueOf(((Number) p.get(1)).longValue() < ((Number) p.get(0)).longValue()));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b — audit
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest(name="{0}")
	@CsvSource(delimiter='|', textBlock="""
		b01 newest first      | "start":0,"order":[{"column":0,"dir":"desc"}]
		b02 second page       | "start":10,"order":[{"column":0,"dir":"desc"}]
		b03 global search     | "start":0,"search":{"value":"pet"},"order":[{"column":0,"dir":"desc"}]
		b04 actor column      | "start":0,"order":[{"column":0,"dir":"desc"}],"columns":[{"data":"at"},{"data":"actor","search":{"value":"console:"}},{"data":"action"},{"data":"entity"},{"data":"entityId"},{"data":"detail"}]
		""")
	void b_auditMatches(String name, String body) throws Exception {
		var r = request(AUDIT_COLUMNS, body);
		assertList(() -> "Case '" + name + "'", page(r, sql.audit().getSession(), AuditEntry::getId), page(r, store.queryAudit(), AuditEntry::getId).toArray());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c — schema parity
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_sqlColumnTypesMatchInMemory() {
		for (var c : PetStore.PET_QUERY.columns())
			assertString(() -> "pet column '" + c + "'", String.valueOf(PetStore.PET_QUERY.getColumnType(c)), sql.pets().getColumnType(c));
		for (var c : PetStore.AUDIT_QUERY.columns())
			assertString(() -> "audit column '" + c + "'", String.valueOf(PetStore.AUDIT_QUERY.getColumnType(c)), sql.audit().getColumnType(c));
	}
}
