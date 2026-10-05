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
package org.apache.juneau.beanquery.sql;

import static org.apache.juneau.commons.beanquery.SearchType.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;
import java.sql.*;
import java.time.*;
import java.util.*;

import javax.sql.*;

import org.apache.juneau.commons.beanquery.*;
import org.junit.jupiter.api.*;

class SqlBeanQueryContext_Builder_Test {

	private static final TestDialect D = TestDialect.INSTANCE;

	private static SqlBeanQueryContext.Builder<Object> builder() {
		return SqlBeanQueryContext.create(Object.class)
			.dialect(D)
			.table("person")
			.column("name", TEXT)
			.column("age", NUMERIC)
			.connectionSupplier(() -> { throw new UnsupportedOperationException("render-only test"); });
	}

	private static void assertIse(String message, SqlBeanQueryContext.Builder<Object> b) {
		assertEquals(message, assertThrows(IllegalStateException.class, b::build).getMessage());
	}

	@Test
	void a01_buildRequiresDialectTableConnectionAndColumns() {
		assertIse("SqlBeanQueryContext requires a dialect.", SqlBeanQueryContext.create(Object.class).table("t").column("a", TEXT).connectionSupplier(() -> null));
		assertIse("SqlBeanQueryContext requires a table.", SqlBeanQueryContext.create(Object.class).dialect(D).column("a", TEXT).connectionSupplier(() -> null));
		assertIse("SqlBeanQueryContext requires a connection source.", SqlBeanQueryContext.create(Object.class).dialect(D).table("t").column("a", TEXT));
		assertIse("At least one column must be declared.", SqlBeanQueryContext.create(Object.class).dialect(D).table("t").connectionSupplier(() -> null));
		assertThrows(IllegalArgumentException.class, () -> SqlBeanQueryContext.create(null));
	}

	@Test
	void a02_setterArgumentChecks() {
		var hoistedTarget1 = builder();
		assertEquals("Column 'nope' is not declared.", assertThrows(IllegalArgumentException.class, () -> hoistedTarget1.columnSql("nope", "x")).getMessage());
		var hoistedTarget2 = builder();
		assertThrows(IllegalArgumentException.class, () -> hoistedTarget2.columnSql("name", " "));
		var hoistedTarget3 = builder();
		assertThrows(IllegalArgumentException.class, () -> hoistedTarget3.dialect(null));
		var hoistedTarget4 = builder();
		assertThrows(IllegalArgumentException.class, () -> hoistedTarget4.table(""));
		var hoistedTarget5 = builder();
		assertThrows(IllegalArgumentException.class, () -> hoistedTarget5.fetchSize(0));
		var hoistedTarget6 = builder();
		assertThrows(IllegalArgumentException.class, () -> hoistedTarget6.queryTimeout(Duration.ZERO));
		var hoistedTarget7 = builder();
		assertThrows(IllegalArgumentException.class, () -> hoistedTarget7.connectionReleaser(null));
	}

	@Test
	void a03_renderRowsUsesResolvedQuery() {
		var c = builder().columnSql("name", "full_name").build();
		var s = c.renderRows(c.resolve(new BeanQuery().setSearch("age=$gt(21)").setSort("name-").setView("name").setPosition(10).setLimit(5)));
		assertBean(s, "sql,binds,columns", "SELECT \"full_name\" FROM \"person\" WHERE \"age\" > ? ORDER BY \"full_name\" DESC LIMIT 5 OFFSET 10,[21],[name]");
		assertEquals("SELECT \"full_name\", \"age\" FROM \"person\" LIMIT 100", c.renderRows(c.resolve(new BeanQuery())).sql());
	}

	@Test
	void a04_guardsAreParenthesizedAndAnded() {
		var c = builder().guard(SqlFragment.of("\"tenant\" = ? OR \"tenant\" = ?", "t1", "t2")).build();
		var q = c.resolve(new BeanQuery().setSearch("age=$gt(21)"));
		var total = c.renderCount(q, false);
		assertEquals("SELECT count(*) FROM \"person\" WHERE (\"tenant\" = ? OR \"tenant\" = ?)", total.sql());
		var matched = c.renderCount(q, true, SqlFragment.of("\"age\" < ?", 65));
		assertBean(matched, "sql,binds", "SELECT count(*) FROM \"person\" WHERE (\"tenant\" = ? OR \"tenant\" = ?) AND (\"age\" < ?) AND \"age\" > ?,[t1,t2,65,21]");
	}

	@Test
	void a05_copyRoundTrips() {
		var c = builder().columnSql("name", "full_name").guard(SqlFragment.of("\"x\" = 1")).build();
		var d = c.copy().table("people").build();
		var q = new BeanQuery().setView("name");
		assertEquals("SELECT \"full_name\" FROM \"people\" WHERE (\"x\" = 1) LIMIT 100", d.renderRows(d.resolve(q)).sql());
		assertEquals("SELECT \"full_name\" FROM \"person\" WHERE (\"x\" = 1) LIMIT 100", c.renderRows(c.resolve(q)).sql());  // Unchanged.
		assertSame(Object.class, c.getType());
		assertSame(Object.class, d.getType());  // Carried through copy().
	}

	@Test
	void a06_dataSourceFailureIsExecutionError() {
		var cause = new SQLException("no route to host");
		var ds = (DataSource)Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {DataSource.class}, (p, m, a) -> {
			throw cause;
		});
		var c = builder().dataSource(ds).build();
		var e = assertThrows(BeanQueryExecutionException.class, c::getSession);
		assertEquals("Failed to open a database connection.", e.getMessage());
		assertSame(cause, e.getCause());
	}

	@Test
	void a07_unknownColumnsNeverReachSql() {
		var c = builder().build();
		var hoistedArg1 = new BeanQuery().setSearch("password=x");
		assertThrows(BeanQuerySyntaxException.class, () -> c.resolve(hoistedArg1));
		var hoistedArg2 = new BeanQuery().setSort("password");
		assertThrows(BeanQuerySyntaxException.class, () -> c.resolve(hoistedArg2));
	}

	@Test
	void a08_columnTypeAndCaps() {
		var c = builder().maxSearchClauses(5).maxSortKeys(3).build();
		assertMapped(c, (o, p) -> o.getColumnType(p), "name,age", "TEXT,NUMERIC");
		assertEquals("Unknown column 'nope'.", assertThrows(IllegalArgumentException.class, () -> c.getColumnType("nope")).getMessage());
		assertBean(c, "maxSearchClauses,maxSortKeys", "5,3");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Default row-mapper selection
	//-----------------------------------------------------------------------------------------------------------------

	record Person(String name, int age) {}

	static class NotPlannable {
		NotPlannable(int x) {}
	}

	private static <T> SqlBeanQueryContext.Builder<T> builderOf(Class<T> type) {
		return SqlBeanQueryContext.create(type)
			.dialect(D)
			.table("person")
			.column("name", TEXT)
			.connectionSupplier(() -> { throw new UnsupportedOperationException("render-only test"); });
	}

	private static String rowMapperMessage(SqlBeanQueryContext<?> c) {
		return assertThrows(IllegalStateException.class, c::rowMapper).getMessage();
	}

	@Test
	void d01_mapTypesDefaultToMapMapper() {
		assertSame(SqlRowMapper.map(), builderOf(Map.class).build().rowMapper());
		assertSame(SqlRowMapper.map(), builderOf(HashMap.class).build().rowMapper());
		assertSame(SqlRowMapper.map(), builderOf(LinkedHashMap.class).build().rowMapper());
	}

	@Test
	void d02_otherMapSubtypeHasNoDefaultAndKeepsReason() {
		assertEquals("SqlBeanQueryContext requires a rowMapper for find and stream: rows are LinkedHashMap.", rowMapperMessage(builderOf(TreeMap.class).build()));
	}

	@Test
	void d03_objectHasNoDefaultAndNoReason() {
		assertEquals("SqlBeanQueryContext requires a rowMapper for find and stream.", rowMapperMessage(builderOf(Object.class).build()));
	}

	@Test
	void d04_plannableBeanTypeDefaultsToBeanMapper() {
		var mapper = builderOf(Person.class).build().rowMapper();
		assertNotNull(mapper);
		assertNotSame(SqlRowMappers.MAP, mapper);
		assertEquals("BeanMapper", mapper.getClass().getSimpleName());
	}

	@Test
	void d05_unplannableTypeHasNoDefaultAndKeepsPlanningReason() {
		assertEquals("SqlBeanQueryContext requires a rowMapper for find and stream: cannot map rows to 'NotPlannable': no public no-arg constructor.",
			rowMapperMessage(builderOf(NotPlannable.class).build()));
	}

	@Test
	void d06_explicitRowMapperWinsOverDefault() {
		SqlRowMapper<Person> explicit = (rs, cols) -> null;
		assertSame(explicit, builderOf(Person.class).rowMapper(explicit).build().rowMapper());
		@SuppressWarnings({
			"unchecked" // Class literals are raw; TreeMap.class is safe to view as the parameterized type.
		})
		var treeType = (Class<TreeMap<String,Object>>)(Class<?>)TreeMap.class;
		SqlRowMapper<TreeMap<String,Object>> explicitTree = (rs, cols) -> new TreeMap<>();
		assertSame(explicitTree, builderOf(treeType).rowMapper(explicitTree).build().rowMapper());
	}

	@Test
	void d07_copyKeepsTheSelection() {
		var ctx = builderOf(Person.class).build();
		assertSame(ctx.rowMapper(), ctx.copy().build().rowMapper());
		assertEquals("SqlBeanQueryContext requires a rowMapper for find and stream: rows are LinkedHashMap.",
			rowMapperMessage(builderOf(TreeMap.class).build().copy().build()));
	}
}
