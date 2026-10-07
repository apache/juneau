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
package org.apache.juneau.commons.beanquery;

import static org.apache.juneau.commons.TestAssertions.*;
import static org.apache.juneau.commons.beanquery.SearchType.*;
import static org.junit.jupiter.api.Assertions.*;

import java.math.*;
import java.time.*;
import java.util.*;

import org.apache.juneau.commons.*;
import org.apache.juneau.commons.bean.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"java:S1186" // Fixture: Empty.doIt() is intentionally empty (a non-property method).
})
class QueryModel_Test extends TestBase {

	public enum Color { RED }

	public static class Person {
		public String nickName;                                   // Public field: readable and writable.
		public final String code = "c";                           // Public final field: readable only.
		private String name;
		private int age;
		public String getName() { return name; }
		public void setName(String v) { name = v; }
		public int getAge() { return age; }                       // Getter only: readable, not writable.
		public void setSecretAge(int v) { age = v; }              // Setter only: writable, not readable.
		public boolean isActive() { return true; }
		public Color getColor() { return Color.RED; }
		@BeanProp("born") public Instant getBirthday() { return Instant.EPOCH; }
		@BeanIgnore public String getPassword() { return "x"; }
		public String getBoom() { throw new IllegalStateException("boom"); }
		@BeanProp(wo="true") public String getSecret() { return "s"; }
		public void setSecret(@SuppressWarnings("unused") String v) { /* stored nowhere */ }
		@BeanProp(ro="true") public String getLocked() { return "l"; }
		public void setLocked(@SuppressWarnings("unused") String v) { /* ignored */ }
		public void setExplode(@SuppressWarnings("unused") String v) { throw new IllegalStateException("bang"); }
		public String getExplode() { return "e"; }
	}

	public static class Empty {
		String hidden;                                            // Package-private: not a bean field.
		public void doIt() {}
	}

	public interface Marker {}

	public record Point(int x, String label) {}

	private static List<String> describe(Map<String,QueryProperty> m) {
		return m.values().stream().map(p -> p.getName() + ":" + p.getType().getSimpleName() + ":" + p.getSearchType()).toList();
	}

	//====================================================================================================
	// Read side
	//====================================================================================================

	@Test
	void a01_readableProperties() {
		var m = QueryModel.of(Person.class);
		assertList(describe(m.getReadableProperties()),
			"active:boolean:BOOLEAN", "age:int:NUMERIC", "boom:String:TEXT", "born:Instant:TIMESTAMP",
			"code:String:TEXT", "color:Color:ENUM", "explode:String:TEXT", "locked:String:TEXT", "name:String:TEXT", "nickName:String:TEXT");
		assertNull(m.getReadableProperty("secret"));      // wo=true: not a column.
		assertNull(m.getReadableProperty("password"));    // @BeanIgnore.
		assertNull(m.getReadableProperty("secretAge"));   // Write-only: not a column.
		assertNull(m.getReadableProperty("class"));       // getClass() is declared on Object.
		assertSame(Person.class, m.getBeanClass());
	}

	@Test
	void a02_read() {
		var p = new Person();
		p.setName("Alice");
		p.nickName = "Al";
		var m = QueryModel.of(Person.class);
		assertList(List.of(m.getReadableProperty("name").read(p), m.getReadableProperty("nickName").read(p), m.getReadableProperty("born").read(p)),
			"Alice", "Al", "1970-01-01T00:00:00Z");
	}

	@Test
	void a03_readFailureNamesColumn() {
		var p = new Person();
		var boom = QueryModel.of(Person.class).getReadableProperty("boom");
		var e = assertThrows(BeanQueryExecutionException.class, () -> boom.read(p));
		assertEquals("Failed to read column 'boom'.", e.getMessage());
		assertEquals("boom", e.getCause().getMessage());
	}

	@Test
	void a04_record() {
		var m = QueryModel.of(Point.class);
		assertList(describe(m.getReadableProperties()), "label:String:TEXT", "x:int:NUMERIC");
		assertEquals(3, m.getReadableProperty("x").read(new Point(3, "a")));
	}

	@Test
	void a05_configPropertyNamer() {
		var config = BeanConfigContext.create().propertyNamer(PropertyNamerDLC.INSTANCE).build();
		assertList(QueryModel.of(Person.class, config).getReadableProperties().keySet(),
			"active", "age", "boom", "born", "code", "color", "explode", "locked", "name", "nick-name");
	}

	@Test
	void a06_ofBeanMeta() {
		var m = QueryModel.of(BeanMeta.of(Point.class));
		assertList(m.getReadableProperties().keySet(), "label", "x");
	}

	@Test
	void a07_nonBeanHasNoProperties() {
		assertList(QueryModel.of(Empty.class).getReadableProperties().keySet());
		assertList(QueryModel.of(Marker.class).getReadableProperties().keySet());
	}

	@Test
	void a08_nullArgs() {
		assertThrows(IllegalArgumentException.class, () -> QueryModel.of((Class<?>)null));
		assertThrows(IllegalArgumentException.class, () -> QueryModel.of(Person.class, null));
		assertThrows(IllegalArgumentException.class, () -> QueryModel.of((BeanMeta<?>)null));
	}

	//====================================================================================================
	// Write side
	//====================================================================================================

	@Test
	void c01_writableProperties() {
		assertList(QueryModel.of(Person.class).getWritableProperties().keySet(),
			"explode", "locked", "name", "nickName", "secret", "secretAge");   // code: final; age: no setter; locked: ro=true still writable.
		assertList(QueryModel.of(Point.class).getWritableProperties().keySet());
	}

	@Test
	void c02_write() {
		var p = new Person();
		var m = QueryModel.of(Person.class);
		m.getReadableProperty("name").write(p, "Bob");
		m.getWritableProperties().get("secretAge").write(p, 7);
		m.getReadableProperty("nickName").write(p, "B");
		assertList(List.of(p.getName(), p.getAge(), p.nickName), "Bob", "7", "B");
		assertEquals(int.class, m.getWritableProperties().get("secretAge").getWriteType());
	}

	@Test
	void c03_writeNullToPrimitiveIsNoOp() {
		var p = new Person();
		p.setSecretAge(5);
		var age = QueryModel.of(Person.class).getWritableProperties().get("secretAge");
		age.write(p, null);
		assertEquals(5, p.getAge());
	}

	@Test
	void c04_notReadableNotWritable() {
		var m = QueryModel.of(Person.class);
		var p = new Person();
		var secretAge = m.getWritableProperties().get("secretAge");
		assertThrows(IllegalStateException.class, () -> secretAge.read(p));
		var age = m.getReadableProperty("age");
		assertThrows(IllegalStateException.class, () -> age.write(p, 1));
		assertTrue(m.getWritableProperties().get("locked").canWrite());  // ro=true only blocks parsers.
	}

	@Test
	void c05_writeFailureNamesColumn() {
		var p = new Person();
		var explode = QueryModel.of(Person.class).getWritableProperties().get("explode");
		var e = assertThrows(BeanQueryExecutionException.class, () -> explode.write(p, "x"));
		assertEquals("Failed to write column 'explode'.", e.getMessage());
		assertEquals("bang", e.getCause().getMessage());
	}

	//====================================================================================================
	// Instance creation
	//====================================================================================================

	public static class Throwing {
		public Throwing() { throw new IllegalStateException("nope"); }
		public void setX(@SuppressWarnings("unused") String v) { /* unused */ }
	}

	public static class Setters {
		private int n = 7;
		private String s;
		public int getN() { return n; }
		public void setN(int v) { n = v; }
		public String getS() { return s; }
		public void setS(String v) { s = v; }
	}

	public class Inner {
		public String x;
	}

	@Test
	void d01_newInstanceNoArg() {
		var m = QueryModel.of(Setters.class);
		assertList(m.getConstructorArgs());
		assertList(m.getConstructorArgTypes());
		var b = m.newInstance();
		m.getWritableProperties().get("s").write(b, "x");
		m.getWritableProperties().get("n").write(b, null);  // Null for a primitive: keeps the default.
		assertBean(b, "n,s", "7,x");
	}

	@Test
	void d02_recordConstructorWithPrimitiveDefaults() {
		var m = QueryModel.of(Point.class);
		assertList(m.getConstructorArgs(), "x", "label");
		assertList(m.getConstructorArgTypes(), int.class, String.class);
		var p = m.newInstance(null, "a");
		assertList(List.of(p.x(), p.label()), "0", "a");
		assertEquals(new Point(4, "b"), m.newInstance(4, "b"));
	}

	@Test
	void d03_constructorFailure() {
		var m = QueryModel.of(Throwing.class);
		var e = assertThrows(BeanQueryExecutionException.class, m::newInstance);
		assertEquals("Failed to construct a Throwing.", e.getMessage());
		assertEquals("nope", e.getCause().getMessage());
	}

	@Test
	void d04_wrongArgCount() {
		var m = QueryModel.of(Point.class);
		var e = assertThrows(IllegalArgumentException.class, () -> m.newInstance(1));
		assertEquals("Expected 2 constructor arguments for Point but got 1.", e.getMessage());
	}

	@Test
	void d05_wrongArgType() {
		var m = QueryModel.of(Point.class);
		var e = assertThrows(BeanQueryExecutionException.class, () -> m.newInstance("x", "a"));
		assertEquals("Failed to construct a Point.", e.getMessage());
		assertInstanceOf(IllegalArgumentException.class, e.getCause());
	}

	public static class Ctor {
		private final String b;
		private final int a;
		@BeanCtor(properties="b,a") public Ctor(String b, int a) { this.b = b; this.a = a; }
		public String getB() { return b; }
		public int getA() { return a; }
	}

	@Test
	void d06_beanCtorAlignment() {
		var m = QueryModel.of(Ctor.class);
		assertList(m.getConstructorArgs(), "b", "a");
		assertList(m.getConstructorArgTypes(), String.class, int.class);
		assertBean(m.newInstance("x", null), "b,a", "x,0");
	}

	@Test
	void d07_nullArgsArray() {
		var m = QueryModel.of(Setters.class);
		assertThrows(IllegalArgumentException.class, () -> m.newInstance((Object[])null));
	}

	@Test
	void d08_noConstructor() {
		var m = QueryModel.of(Marker.class);
		var e = assertThrows(IllegalStateException.class, m::newInstance);
		assertEquals("Marker has no usable constructor.", e.getMessage());
	}

	@Test
	void d09_nonStaticInnerClassHasNoConstructor() {
		var m = QueryModel.of(Inner.class);
		assertList(m.getConstructorArgs());
		var e = assertThrows(IllegalStateException.class, m::newInstance);
		assertEquals("Inner has no usable constructor.", e.getMessage());
	}

	record PackagePrivateRecord(String name, int age) {}

	@Test
	void d10_packagePrivateRecord_newInstance() {
		var model = QueryModel.of(PackagePrivateRecord.class);
		assertList(model.getConstructorArgs(), "name", "age");
		var r = model.newInstance("Alice", 30);
		assertEquals("Alice", r.name());
		assertEquals(30, r.age());
	}

	//====================================================================================================
	// searchTypeOf (migrated from BeanProperties_Test.a02_typeOf)
	//====================================================================================================

	@Test
	void b01_searchTypeOf() {
		assertList(
			List.of(boolean.class, Boolean.class, char.class, Character.class, int.class, Long.class, BigDecimal.class,
				Date.class, Calendar.class, Instant.class, LocalDate.class, Color.class, DayOfWeek.class, String.class, Object.class)
				.stream().map(QueryProperty::searchTypeOf).toList(),
			BOOLEAN, BOOLEAN, TEXT, TEXT, NUMERIC, NUMERIC, NUMERIC,
			TIMESTAMP, TIMESTAMP, TIMESTAMP, TIMESTAMP, ENUM, ENUM, TEXT, TEXT);  // DayOfWeek: enum implementing TemporalAccessor, ENUM wins.
	}
}
