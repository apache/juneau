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

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.*;
import java.time.*;
import java.util.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.beanquery.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"java:S1172", // Fixtures: unused parameters are signature-only.
	"java:S1186" // Fixtures: empty constructors/setters are signature-only.
})
class SqlRowMapper_Test {

	private static final String URL = "jdbc:h2:mem:SqlRowMapper_Test;DB_CLOSE_DELAY=-1";

	record TaskRecord(String name, int age, boolean active) {}

	record WhenRecord(Instant opened, java.util.Date closed, Status status) {}

	enum Status { OPEN, CLOSED }

	enum Bodied { A { @Override int v() { return 1; } }; abstract int v(); }

	record Wide(byte b, short s, long l, float f, double d, char c, Object o) {}

	public static class TimeBean {
		private Instant opened;
		private java.util.Date closed;
		public void setOpened(Instant opened) { this.opened = opened; }
		public void setClosed(java.util.Date closed) { this.closed = closed; }
		public Instant getOpened() { return opened; }
		public java.util.Date getClosed() { return closed; }
	}

	static class HiddenBean {  // Non-public class with a public constructor and setters.
		private String name;
		public HiddenBean() {}
		public void setName(String name) { this.name = name; }
		public String getName() { return name; }
	}

	static class PkgBase {
		private String name;
		private int age;
		public void setName(String name) { this.name = name; }
		public void setAge(int age) { this.age = age; }
		public String getName() { return name; }
		public int getAge() { return age; }
	}

	public static class PublicChild extends PkgBase {}  // javac adds public visibility bridges for the inherited setters.

	public static class GenericBase<T> {
		Object value;
		public void setValue(T v) { value = v; }
	}

	public static class GenericChild extends GenericBase<String> {
		@Override public void setValue(String v) { value = "child:" + v; }  // Also generates setValue(Object) bridge.
		public Object getValue() { return value; }
	}

	public static class FluentBase {
		String name;
		public FluentBase setName(String n) { name = n; return this; }
	}

	public static class FluentChild extends FluentBase {
		@Override public FluentChild setName(String n) { name = "child:" + n; return this; }  // Covariant; bridge returns FluentBase.
		public String getName() { return name; }
	}

	public static class NotSetters {
		String v;
		public void settle(int x) { v = "settle"; }
		public void setup(String x) { v = "setup"; }
		public void setx(String x) { v = "setx"; }
		public void set(String x) { v = "set"; }
		public String getV() { return v; }
	}

	public static class TaskBean {
		private String name;
		private int age;
		private Status status;
		public TaskBean() {}
		public void setName(String name) { this.name = name; }
		public void setAge(int age) { this.age = age; }
		public TaskBean setStatus(Status status) { this.status = status; return this; }  // Fluent, non-void.
		public String getName() { return name; }
		public int getAge() { return age; }
		public Status getStatus() { return status; }
	}

	public static class FieldBean {
		public String name;
		public int age;
		@BeanIgnore public String secret;
	}

	public static class ReadOnlyBean {
		private String name;
		public String getName() { return name; }
		@BeanProp(ro="true") public void setName(String v) { name = v; }  // ro only blocks parsers, not row loading.
	}

	public static class UrlBean {
		private String url;
		public void setURL(String v) { url = v; }
		public String getURL() { return url; }
	}

	public static class NamedBean {
		private String firstName;
		public void setFirstName(String v) { firstName = v; }
		public String getFirstName() { return firstName; }
	}

	public static class Ambiguous {
		public Ambiguous() {}
		public void setX(String s) {}
		public void setX(int i) {}
	}

	public static class Throwing {
		public Throwing() {}
		public void setAge(int age) { throw new IllegalStateException("nope"); }
	}

	public static class ThrowingCtor {
		public ThrowingCtor() { throw new IllegalStateException("nope"); }
	}

	interface NotConcrete {}

	abstract static class Abstract {
		public Abstract() {}
	}

	static class NoNoArgCtor {
		NoNoArgCtor(int x) {}
	}

	static class NonPublicCtor {
		NonPublicCtor() {}
	}

	@BeforeAll
	static void createTables() throws SQLException {
		try (var c = DriverManager.getConnection(URL); var s = c.createStatement()) {
			s.execute("CREATE TABLE \"t\" (\"name\" VARCHAR(20), \"age\" INT)");
			s.execute("INSERT INTO \"t\" VALUES ('Alice', 30), ('Bob', NULL)");
			s.execute("CREATE TABLE \"b\" (\"name\" VARCHAR(20), \"age\" INT, \"active\" BOOLEAN, \"status\" VARCHAR(10), \"opened\" TIMESTAMP WITH TIME ZONE, \"closed\" TIMESTAMP)");
			s.execute("INSERT INTO \"b\" VALUES ('Alice', 30, TRUE, 'OPEN', TIMESTAMP WITH TIME ZONE '2026-01-01 00:00:00+00', TIMESTAMP '2026-02-03 04:05:06'), "
				+ "('Bob', NULL, NULL, 'NOPE', NULL, NULL)");
		}
	}

	private static <T> T mapRow(SqlRowMapper<T> mapper, String sql, String...columns) throws SQLException {
		try (var c = DriverManager.getConnection(URL); var st = c.prepareStatement(sql); var rs = st.executeQuery()) {
			assertTrue(rs.next());
			return mapper.map(rs, List.of(columns));
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// map()
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void a01_map_rowsInViewOrderWithNulls() throws SQLException {
		try (var c = DriverManager.getConnection(URL);
				var st = c.prepareStatement("SELECT \"age\", \"name\" FROM \"t\" ORDER BY \"name\"");
				var rs = st.executeQuery()) {
			var columns = List.of("age", "name");
			var mapper = SqlRowMapper.map();
			assertTrue(rs.next());
			var alice = mapper.map(rs, columns);
			assertList(alice.keySet(), "age", "name");
			assertBean(alice, "age,name", "30,Alice");
			assertTrue(rs.next());
			var bob = mapper.map(rs, columns);
			assertBean(bob, "age,name", "<null>,Bob");
			assertNotSame(alice, bob);
		}
	}

	@Test
	void a02_map_sameSharedInstanceEveryCall() {
		assertSame(SqlRowMapper.map(), SqlRowMapper.map());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// bean(): records
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_record_mapsByComponentNameInAnyColumnOrder() throws SQLException {
		var row = mapRow(SqlRowMapper.bean(TaskRecord.class), "SELECT \"age\", \"name\", \"active\" FROM \"b\" WHERE \"name\" = 'Alice'", "age", "name", "active");
		assertBean(row, "name,age,active", "Alice,30,true");
	}

	@Test
	void b02_record_missingColumnAndNullPrimitiveGetDefaults() throws SQLException {
		var row = mapRow(SqlRowMapper.bean(TaskRecord.class), "SELECT \"age\", \"active\" FROM \"b\" WHERE \"name\" = 'Bob'", "age", "active");
		assertBean(row, "name,age,active", "<null>,0,false");
	}

	@Test
	void b03_record_instantDateAndEnum() throws SQLException {
		var row = mapRow(SqlRowMapper.bean(WhenRecord.class), "SELECT \"opened\", \"closed\", \"status\" FROM \"b\" WHERE \"name\" = 'Alice'", "opened", "closed", "status");
		assertBean(row, "opened,status", "2026-01-01T00:00:00Z,OPEN");
		assertInstanceOf(java.sql.Timestamp.class, row.closed());
	}

	@Test
	void b04_record_nullInstantDateAndEnum() throws SQLException {
		var row = mapRow(SqlRowMapper.bean(WhenRecord.class), "SELECT \"opened\", \"closed\" FROM \"b\" WHERE \"name\" = 'Bob'", "opened", "closed");
		assertBean(row, "opened,closed,status", "<null>,<null>,<null>");
	}

	@Test
	void b05_record_allPrimitivesAndObjectComponent() throws SQLException {
		var mapper = SqlRowMapper.bean(Wide.class);
		var row = mapRow(mapper, "SELECT CAST(1 AS TINYINT) AS \"b\", CAST(2 AS SMALLINT) AS \"s\", CAST(3 AS BIGINT) AS \"l\", CAST(1.5 AS REAL) AS \"f\", CAST(2.5 AS DOUBLE PRECISION) AS \"d\", 'x' AS \"c\", 'obj' AS \"o\"", "b", "s", "l", "f", "d", "c", "o");
		assertBean(row, "b,s,l,f,d,c,o", "1,2,3,1.5,2.5,x,obj");
		var empty = mapRow(mapper, "SELECT 1 AS \"z\"", "z");
		assertBean(empty, "b,s,l,f,d,c,o", "0,0,0,0.0,0.0,\0,<null>");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// bean(): setter beans
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void c01_setterBean_fluentSetterEnumAndExtraColumnIgnored() throws SQLException {
		var row = mapRow(SqlRowMapper.bean(TaskBean.class), "SELECT \"name\", \"age\", \"status\", \"closed\" FROM \"b\" WHERE \"name\" = 'Alice'", "name", "age", "status", "closed");
		assertBean(row, "name,age,status", "Alice,30,OPEN");
	}

	@Test
	void c02_setterBean_nullPrimitiveKeepsDefaultAndNullReferenceIsSet() throws SQLException {
		var row = mapRow(SqlRowMapper.bean(TaskBean.class), "SELECT \"name\", \"age\" FROM \"b\" WHERE \"name\" = 'Bob'", "name", "age");
		assertBean(row, "name,age,status", "Bob,0,<null>");
	}

	@Test
	void c03_setterBean_newInstancePerRow() throws SQLException {
		var mapper = SqlRowMapper.bean(TaskBean.class);
		assertNotSame(mapRow(mapper, "SELECT \"name\" FROM \"b\"", "name"), mapRow(mapper, "SELECT \"name\" FROM \"b\"", "name"));
	}

	@Test
	void c04_setterBean_instantAndDateProperties() throws SQLException {
		var row = mapRow(SqlRowMapper.bean(TimeBean.class), "SELECT \"opened\", \"closed\" FROM \"b\" WHERE \"name\" = 'Alice'", "opened", "closed");
		assertBean(row, "opened", "2026-01-01T00:00:00Z");
		assertInstanceOf(java.sql.Timestamp.class, row.getClosed());
		var none = mapRow(SqlRowMapper.bean(TimeBean.class), "SELECT \"opened\", \"closed\" FROM \"b\" WHERE \"name\" = 'Bob'", "opened", "closed");
		assertBean(none, "opened,closed", "<null>,<null>");
	}

	@Test
	void c05_setterBean_nonPublicClassWithPublicSetters() throws SQLException {
		var row = mapRow(SqlRowMapper.bean(HiddenBean.class), "SELECT \"name\" FROM \"b\" WHERE \"name\" = 'Alice'", "name");
		assertBean(row, "name", "Alice");
	}

	@Test
	void c06_setterBean_inheritedFromPackagePrivateBaseViaBridges() throws SQLException {
		var row = mapRow(SqlRowMapper.bean(PublicChild.class), "SELECT \"name\", \"age\" FROM \"b\" WHERE \"name\" = 'Alice'", "name", "age");
		assertBean(row, "name,age", "Alice,30");
	}

	@Test
	void c07_setterBean_genericOverrideIsNotAmbiguous() throws SQLException {
		var row = mapRow(SqlRowMapper.bean(GenericChild.class), "SELECT \"name\" AS \"value\" FROM \"b\" WHERE \"name\" = 'Alice'", "value");
		assertBean(row, "value", "child:Alice");
	}

	@Test
	void c08_setterBean_covariantFluentOverrideIsNotAmbiguous() throws SQLException {
		var row = mapRow(SqlRowMapper.bean(FluentChild.class), "SELECT \"name\" FROM \"b\" WHERE \"name\" = 'Alice'", "name");
		assertBean(row, "name", "child:Alice");
	}

	@Test
	void c09_setterBean_followsJuneauSetterNaming() throws SQLException {
		// Juneau bean rules (as for parsers): setup(String) is the setter for 'up', setx(String) for 'x'.
		var mapper = SqlRowMapper.bean(NotSetters.class);
		assertBean(mapRow(mapper, "SELECT 'a' AS \"up\"", "up"), "v", "setup");
		assertBean(mapRow(mapper, "SELECT 'a' AS \"x\"", "x"), "v", "setx");
	}

	@Test
	void c10_publicFieldsAreSetAndBeanIgnoreIsSkipped() throws SQLException {
		var row = mapRow(SqlRowMapper.bean(FieldBean.class), "SELECT \"name\", \"age\", 'x' AS \"secret\" FROM \"b\" WHERE \"name\" = 'Alice'", "name", "age", "secret");
		assertBean(row, "name,age,secret", "Alice,30,<null>");
	}

	@Test
	void c10b_readOnlyPropertyIsPopulatedFromRow() throws SQLException {
		var row = mapRow(SqlRowMapper.bean(ReadOnlyBean.class), "SELECT \"name\" FROM \"b\" WHERE \"name\" = 'Alice'", "name");
		assertBean(row, "name", "Alice");
	}

	@Test
	void c11_propertyNamesFollowPropertyNamer() throws SQLException {
		var row = mapRow(SqlRowMapper.bean(UrlBean.class), "SELECT 'u' AS \"URL\"", "URL");
		assertBean(row, "URL", "u");
	}

	@Test
	void c12_beanFromBeanMeta_customPropertyNamer() throws SQLException {
		var config = BeanConfigContext.create().propertyNamer(PropertyNamerULC.INSTANCE).build();
		var mapper = SqlRowMapper.bean(BeanMeta.of(NamedBean.class, config));
		var row = mapRow(mapper, "SELECT 'Ann' AS \"first_name\"", "first_name");
		assertBean(row, "firstName", "Ann");
		assertBean(mapRow(mapper, "SELECT 'Bob' AS \"firstName\"", "firstName"), "firstName", "<null>");
	}

	@Test
	void c13_beanFromBeanMeta() throws SQLException {
		var row = mapRow(SqlRowMapper.bean(BeanMeta.of(TaskRecord.class)), "SELECT \"name\", \"age\", \"active\" FROM \"b\" WHERE \"name\" = 'Alice'", "name", "age", "active");
		assertBean(row, "name,age,active", "Alice,30,true");
		assertThrows(IllegalArgumentException.class, () -> SqlRowMapper.bean((BeanMeta<?>)null));
		var bm = BeanMeta.of(Ambiguous.class);
		assertEquals("Cannot map rows to 'Ambiguous': ambiguous setter for 'x'.", assertThrows(IllegalArgumentException.class, () -> SqlRowMapper.bean(bm)).getMessage());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// bean(): failures
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void d01_unknownEnumConstant_namesColumnAndTypeButNoSql() {
		var mapper = SqlRowMapper.bean(TaskBean.class);
		var e = assertThrows(BeanQueryExecutionException.class, () -> mapRow(mapper, "SELECT \"status\" FROM \"b\" WHERE \"name\" = 'Bob'", "status"));
		assertEquals("Failed to map column 'status' to TaskBean.", e.getMessage());
		assertInstanceOf(IllegalArgumentException.class, e.getCause());
	}

	@Test
	void d02_unconvertibleColumnValue_namesColumnAndKeepsDriverCause() {
		var mapper = SqlRowMapper.bean(TaskRecord.class);
		var e = assertThrows(BeanQueryExecutionException.class, () -> mapRow(mapper, "SELECT \"name\" FROM \"b\" WHERE \"name\" = 'Alice'", "age"));  // 'Alice' is not an int.
		assertEquals("Failed to map column 'age' to TaskRecord.", e.getMessage());
		assertInstanceOf(SQLException.class, e.getCause());
	}

	@Test
	void d03_setterThrows_wrappedWithColumn() {
		var mapper = SqlRowMapper.bean(Throwing.class);
		var e = assertThrows(BeanQueryExecutionException.class, () -> mapRow(mapper, "SELECT \"age\" FROM \"b\" WHERE \"name\" = 'Alice'", "age"));
		assertEquals("Failed to map column 'age' to Throwing.", e.getMessage());
		assertEquals("nope", e.getCause().getMessage());
	}

	@Test
	void d04_constructorThrows_wrappedWithoutColumn() {
		var mapper = SqlRowMapper.bean(ThrowingCtor.class);
		var e = assertThrows(BeanQueryExecutionException.class, () -> mapRow(mapper, "SELECT \"age\" FROM \"b\"", "age"));
		assertEquals("Failed to construct a ThrowingCtor.", e.getMessage());
		assertEquals("nope", e.getCause().getMessage());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// bean(): planning
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void e01_planningRejections() {
		assertPlanError("Cannot map rows to 'NotConcrete': an interface.", NotConcrete.class);
		assertPlanError("Cannot map rows to 'Abstract': an abstract class.", Abstract.class);
		assertPlanError("Cannot map rows to 'Status': an enum.", Status.class);
		assertPlanError("Cannot map rows to 'Bodied': an enum.", Bodied.class);
		assertPlanError("Cannot map rows to 'int[]': an array type.", int[].class);
		assertPlanError("Cannot map rows to 'int': a primitive type.", int.class);
		assertPlanError("Cannot map rows to 'NoNoArgCtor': no public no-arg constructor.", NoNoArgCtor.class);
		assertPlanError("Cannot map rows to 'NonPublicCtor': no public no-arg constructor.", NonPublicCtor.class);
		assertPlanError("Cannot map rows to 'Ambiguous': ambiguous setter for 'x'.", Ambiguous.class);
	}

	private static void assertPlanError(String message, Class<?> type) {
		assertEquals(message, assertThrows(IllegalArgumentException.class, () -> SqlRowMapper.bean(type)).getMessage());
	}

	@Test
	void e02_nullType() {
		assertThrows(IllegalArgumentException.class, () -> SqlRowMapper.bean((Class<?>)null));
	}

	@Test
	void e03_tryBean_returnsMapperOrReasonWithoutThrowing() {
		var ok = SqlRowMappers.tryBean(TaskRecord.class);
		assertNull(ok.reason());
		assertNotNull(ok.mapper());
		var bad = SqlRowMappers.tryBean(Ambiguous.class);
		assertNull(bad.mapper());
		assertEquals("ambiguous setter for 'x'", bad.reason());
	}
}
