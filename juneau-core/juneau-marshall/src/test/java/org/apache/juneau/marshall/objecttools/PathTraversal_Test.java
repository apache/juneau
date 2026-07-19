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
package org.apache.juneau.marshall.objecttools;

import static org.apache.juneau.BasicTestUtils.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.bean.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.json5.*;
import org.apache.juneau.marshall.parser.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"rawtypes",  // Raw types required for generic test utility.
	"serial",  // serialVersionUID not required for test classes.
	"java:S5961"  // Test comprehensiveness requires more than 25 assertions.
})
class PathTraversal_Test extends TestBase {

	//====================================================================================================
	// testBasic
	//====================================================================================================
	@Test void a01_basic() {

		var model = PathTraversal.create(new Json5Map()); // An empty model.

		// Do a PUT
		model.put("A", new Json5Map());
		model.put("A/B", new Json5Map());
		model.put("A/B/C", "A new string");
		assertEquals("{A:{B:{C:'A new string'}}}", model.toString());

		// Do a POST to a list.
		model.put("A/B/C", new LinkedList());
		model.post("A/B/C", "String #1");
		model.post("A/B/C", "String #2");
		assertEquals("{A:{B:{C:['String #1\','String #2']}}}", model.toString());

		// Do some GETs
		var s = (String) model.get("A/B/C/0");
		assertEquals("String #1", s);

		var m = (Map) model.get("A/B");
		assertEquals("{C:['String #1','String #2']}", m.toString());
	}

	//====================================================================================================
	// testBeans
	//====================================================================================================
	@Test void b01_beans() throws Exception {
		var model = PathTraversal.create(new Json5Map());

		// Java beans.
		var p = new Person("some name", 123,
			new Address("street A", "city A", "state A", 12345, true),
			new Address("street B", "city B", "state B", 12345, false)
		);
		model.put("/person1", p);

		// Make sure it got stored correctly.
		var serializer = Json5Serializer.create().addRootType().build();
		assertEquals("{person1:{name:'some name',age:123,addresses:[{street:'street A',city:'city A',state:'state A',zip:12345,isCurrent:true},{street:'street B',city:'city B',state:'state B',zip:12345,isCurrent:false}]}}", serializer.write(model.getRootObject()));

		// Get the original Person object back.
		p = (Person)model.get("/person1");
		assertEquals("city B", p.addresses[1].city);

		// Look for deep information inside beans.
		var a3 = (Address)model.get("/person1/addresses/1");
		assertEquals("city B", a3.city);

		serializer = Json5Serializer.DEFAULT.copy().addBeanTypes().addRootType().build();
		p = new Person("some name", 123,
			new Address("street A", "city A", "state A", 12345, true),
			new Address("street B", "city B", "state B", 12345, false)
		);

		// Serialize it to JSON.
		var s = serializer.write(p);
		var expectedValue = "{_type:'Person',name:'some name',age:123,addresses:[{street:'street A',city:'city A',state:'state A',zip:12345,isCurrent:true},{street:'street B',city:'city B',state:'state B',zip:12345,isCurrent:false}]}";
		assertEquals(expectedValue, s);

		// Parse it back to Java objects.
		p = (Person)Json5Parser.create().beanDictionary(Person.class).build().read(s, Object.class);
		expectedValue = "city B";
		s = p.addresses[1].city;
		assertEquals(expectedValue, s);

		// Parse it back into JSON again.
		s = serializer.write(p);
		expectedValue = "{_type:'Person',name:'some name',age:123,addresses:[{street:'street A',city:'city A',state:'state A',zip:12345,isCurrent:true},{street:'street B',city:'city B',state:'state B',zip:12345,isCurrent:false}]}";
		assertEquals(expectedValue, s);

		// Try adding an address
		model = PathTraversal.create(p);
		model.post("addresses", new Address("street C", "city C", "state C", 12345, true));
		s = ((Address)model.get("addresses/2")).toString();
		expectedValue = "Address(street=street C,city=city C,state=state C,zip=12345,isCurrent=true)";
		assertEquals(expectedValue, s);

		// Try replacing addresses
		model.put("addresses/0", new Address("street D", "city D", "state D", 12345, false));
		model.put("addresses/1", new Address("street E", "city E", "state E", 12345, false));
		model.put("addresses/2", new Address("street F", "city F", "state F", 12345, false));
		serializer = Json5Serializer.create().build();
		s = serializer.write(p);
		expectedValue = "{name:'some name',age:123,addresses:[{street:'street D',city:'city D',state:'state D',zip:12345,isCurrent:false},{street:'street E',city:'city E',state:'state E',zip:12345,isCurrent:false},{street:'street F',city:'city F',state:'state F',zip:12345,isCurrent:false}]}";
		assertEquals(expectedValue, s);

		// Try removing an address
		model.delete("addresses/1");
		s = serializer.write(p);
		expectedValue = "{name:'some name',age:123,addresses:[{street:'street D',city:'city D',state:'state D',zip:12345,isCurrent:false},{street:'street F',city:'city F',state:'state F',zip:12345,isCurrent:false}]}";
		assertEquals(expectedValue, s);

		model.delete("addresses/0");
		model.delete("addresses/0");
		s = serializer.write(p);
		expectedValue = "{name:'some name',age:123,addresses:[]}";
		assertEquals(expectedValue, s);

		// Try adding an out-of-bounds address (should pad it with nulls)
		model.put("addresses/2", new Address("street A", "city A", "state A", 12345, true));
		s = serializer.write(p);
		expectedValue = "{name:'some name',age:123,addresses:[null,null,{street:'street A',city:'city A',state:'state A',zip:12345,isCurrent:true}]}";
		assertEquals(expectedValue, s);

		// Try adding an address as a map (should be automatically converted to an Address)
		var m = new HashMap<String,Object>();
		m.put("street","street D");
		m.put("city","city D");
		m.put("state","state D");
		m.put("zip",Integer.valueOf(12345));

		// Try the same for an address in an array.
		model.put("addresses/1", m);
		s = ((Address)model.get("addresses/1")).toString();
		expectedValue = "Address(street=street D,city=city D,state=state D,zip=12345,isCurrent=false)";
		assertEquals(expectedValue, s);

		// Try setting some fields.
		model.put("addresses/1/zip", Integer.valueOf(99999));
		s = model.get("addresses/1/zip").toString();
		expectedValue = "99999";
		assertEquals(expectedValue, s);

		// Make sure we can get non-existent branches without throwing any exceptions.
		// get() method should just return null.
		model = PathTraversal.create(new Json5Map());
		var o = model.get("xxx");
		assertEquals("null", (""+o));

		// Make sure blanks and "/" returns the root object.
		s = model.get("").toString();
		assertEquals("{}", s);
		s = model.get("/").toString();
		assertEquals("{}", s);

		// Make sure doing a PUT against "" or "/" replaces the root object.
		var m2 = Json5Map.ofString("{x:1}");
		model.put("", m2);
		s = model.get("").toString();
		assertEquals("{x:1}", s);
		m2 = Json5Map.ofString("{x:2}");
		model.put("/", m2);
		s = model.get("").toString();
		assertEquals("{x:2}", s);

		// Make sure doing a POST against "" or "/" adds to the root object.
		model = PathTraversal.create(new Json5List());
		model.post("", Integer.valueOf(1));
		model.post("/", Integer.valueOf(2));
		s = model.get("").toString();
		assertEquals("[1,2]", s);
	}

	//====================================================================================================
	// testAddressBook
	//====================================================================================================
	@Test void b02_addressBook() {
		var model = PathTraversal.create(new AddressBook());

		// Try adding a person to the address book.
		var billClinton = new Person("Bill Clinton", 65,
			new Address("55W. 125th Street", "New York", "NY", 10027, true)
		);

		model.post("/", billClinton);

		// Make sure we get the original person back.
		assertSame(billClinton, model.get("/0"));
	}

	public static class AddressBook extends LinkedList<Person> {

		public AddressBook init() {
			add(
				new Person("Bill Clinton", 65,
					new Address("55W. 125th Street", "New York", "NY", 10027, true)
				)
			);
			return this;
		}
	}

	@BeanType(p="street,city,state,zip,isCurrent")
	public static class Address {
		public String street;
		public String city;
		public String state;
		public int zip;
		public boolean isCurrent;

		public Address() {}

		public Address(String street, String city, String state, int zip, boolean isCurrent) {
			this.street = street;
			this.city = city;
			this.state = state;
			this.zip = zip;
			this.isCurrent = isCurrent;
		}
		@Override /* Overridden from Object */
		public String toString() {
			return "Address(street="+street+",city="+city+",state="+state+",zip="+zip+",isCurrent="+isCurrent+")";
		}
	}

	@Marshalled(typeName="Person")
	@BeanType(p="name,age,addresses")
	public static class Person {
		public String name;
		public int age;
		public Address[] addresses;

		public Person() {}

		public Person(String name, int age, Address...addresses) {
			this.name = name;
			this.age = age;
			this.addresses = addresses;
		}

		@Override /* Overridden from Object */
		public String toString() {
			return "Person(name="+name+",age="+age+")";
		}
	}

	//====================================================================================================
	// PathTraversal(Object,ReaderParser)
	//====================================================================================================
	@Test void c01_constructors() {
		var model = PathTraversal.create(new AddressBook(), Json5Parser.DEFAULT);

		// Try adding a person to the address book.
		var billClinton = new Person("Bill Clinton", 65,
			new Address("55W. 125th Street", "New York", "NY", 10027, true)
		);

		model.post("/", billClinton);

		// Make sure we get the original person back.
		assertSame(billClinton, model.get("/0"));
	}

	//====================================================================================================
	// setRootLocked()
	//====================================================================================================
	@Test void d01_rootLocked() {
		var model = PathTraversal.create(new AddressBook()).setRootLocked();
		assertThrowsWithMessage(PathTraversalException.class, "Cannot overwrite root object", ()->model.put("", new AddressBook()));
		assertThrowsWithMessage(PathTraversalException.class, "Cannot overwrite root object", ()->model.put(null, new AddressBook()));
		assertThrowsWithMessage(PathTraversalException.class, "Cannot overwrite root object", ()->model.put("/", new AddressBook()));
	}

	//====================================================================================================
	// getRootObject()
	//====================================================================================================
	@Test void e01_getRootObject() {
		var model = PathTraversal.create(new AddressBook());
		assertTrue(model.getRootObject() instanceof AddressBook);
		model.put("", "foobar");
		assertTrue(model.getRootObject() instanceof String);
		model.put("", null);
		assertNull(model.getRootObject());
	}

	//====================================================================================================
	// get(Class<T> type, String url)
	// get(Class<T> type, String url, T def)
	// getString(String url)
	// getString(String url, String defVal)
	// getInt(String url)
	// getInt(String url, Integer defVal)
	// getLong(String url)
	// getLong(String url, Long defVal)
	// getBoolean(String url)
	// getBoolean(String url, Boolean defVal)
	// getMap(String url)
	// getMap(String url, Map<?,?> defVal)
	// getList(String url)
	// getList(String url, List<?> defVal)
	// getJsonMap(String url)
	// getJsonMap(String url, JsonMap defVal)
	// getJsonList(String url)
	// getJsonList(String url, JsonList defVal)
	//====================================================================================================
	@Test void f01_getMethods() throws Exception {
		var model = PathTraversal.create(new A());
		var l = Json5List.ofString("[{a:'b'}]");
		var m = Json5Map.ofString("{a:'b'}");
		var jm = new JsonMap(m);
		var jl = new JsonList(l);

		assertMapped(model, PathTraversal::get,
			"f1,f2,f3,f4,f2a,f3a,f4a,f5,f6,f7,f8",
			"<null>,0,0,false,<null>,<null>,<null>,<null>,<null>,<null>,<null>");

		assertMapped(model, (r,p) -> r.getWithDefault(p, "foo"),
			"f1,f2,f3,f4,f2a,f3a,f4a,f5,f6,f7,f8",
			"foo,0,0,false,foo,foo,foo,foo,foo,foo,foo");

		assertMapped(model, PathTraversal::getString,
			"f1,f2,f3,f4,f2a,f3a,f4a,f5,f6,f7,f8",
			"<null>,0,0,false,<null>,<null>,<null>,<null>,<null>,<null>,<null>");

		assertMapped(model, (r,p) -> r.getString(p, "foo"),
			"f1,f2,f3,f4,f2a,f3a,f4a,f5,f6,f7,f8",
			"foo,0,0,false,foo,foo,foo,foo,foo,foo,foo");

		assertMapped(model, PathTraversal::getInt,
			"f1,f2,f3,f4,f2a,f3a,f4a,f5,f6,f7,f8",
			"<null>,0,0,0,<null>,<null>,<null>,<null>,<null>,<null>,<null>");

		assertMapped(model, (r,p) -> r.getInt(p, 1),
			"f1,f2,f3,f4,f2a,f3a,f4a,f5,f6,f7,f8",
			"1,0,0,0,1,1,1,1,1,1,1");

		assertMapped(model, PathTraversal::getLong,
			"f1,f2,f3,f4,f2a,f3a,f4a,f5,f6,f7,f8",
			"<null>,0,0,0,<null>,<null>,<null>,<null>,<null>,<null>,<null>");

		assertMapped(model, (r,p) -> r.getLong(p, 1L),
			"f1,f2,f3,f4,f2a,f3a,f4a,f5,f6,f7,f8",
			"1,0,0,0,1,1,1,1,1,1,1");

		assertMapped(model, PathTraversal::getBoolean,
			"f1,f2,f3,f4,f2a,f3a,f4a,f5,f6,f7,f8",
			"<null>,false,false,false,<null>,<null>,<null>,<null>,<null>,<null>,<null>");

		assertMapped(model, (r,p) -> r.getBoolean(p, true),
			"f1,f2,f3,f4,f2a,f3a,f4a,f5,f6,f7,f8",
			"true,false,false,false,true,true,true,true,true,true,true");

		BiFunction<PathTraversal,String,Object> f1 = (r,p) -> {
			try {
				return r.getMap(p);
			} catch (Exception e) {
				return cns(e);
			}
		};

		assertMapped(model, f1,
			"f1,f2,f3,f4,f2a,f3a,f4a,f5,f6,f7,f8",
			"<null>,InvalidDataConversionException,InvalidDataConversionException,InvalidDataConversionException,<null>,<null>,<null>,<null>,<null>,<null>,<null>");

		assertEquals("{a:'b'}", model.getMap("f1", m).toString());
		assertThrows(InvalidDataConversionException.class, ()->model.getMap("f2", m));
		assertThrows(InvalidDataConversionException.class, ()->model.getMap("f3", m));
		assertThrows(InvalidDataConversionException.class, ()->model.getMap("f4", m));
		assertEquals("{a:'b'}", model.getMap("f2a", m).toString());
		assertEquals("{a:'b'}", model.getMap("f3a", m).toString());
		assertEquals("{a:'b'}", model.getMap("f4a", m).toString());
		assertEquals("{a:'b'}", model.getMap("f5", m).toString());
		assertEquals("{a:'b'}", model.getMap("f6", m).toString());
		assertEquals("{a:'b'}", model.getMap("f7", m).toString());
		assertEquals("{a:'b'}", model.getMap("f8", m).toString());

		BiFunction<PathTraversal,String,Object> f2 = (r,p) -> {
			try {
				return r.getMap(p, m);
			} catch (Exception e) {
				return cns(e);
			}
		};

		assertMapped(model, f2,
			"f1,f2,f2a,f3,f3a,f4,f4a,f5,f6,f7,f8",
			"{a=b},InvalidDataConversionException,{a=b},InvalidDataConversionException,{a=b},InvalidDataConversionException,{a=b},{a=b},{a=b},{a=b},{a=b}");

		BiFunction<PathTraversal,String,Object> f3 = (r,p) -> {
			try {
				return r.getJsonMap(p);
			} catch (Exception e) {
				return cns(e);
			}
		};

		assertMapped(model, f3,
			"f1,f2,f3,f4,f2a,f3a,f4a,f5,f6,f7,f8",
			"<null>,InvalidDataConversionException,InvalidDataConversionException,InvalidDataConversionException,<null>,<null>,<null>,<null>,<null>,<null>,<null>");

		BiFunction<PathTraversal,String,Object> f4 = (r,p) -> {
			try {
				return r.getJsonMap(p, jm);
			} catch (Exception e) {
				return cns(e);
			}
		};

		assertMapped(model, f4,
			"f1,f2,f3,f4,f2a,f3a,f4a,f5,f6,f7,f8",
			"{a=b},InvalidDataConversionException,InvalidDataConversionException,InvalidDataConversionException,{a=b},{a=b},{a=b},{a=b},{a=b},{a=b},{a=b}");

		assertNull(model.getList("f1"));
		assertThrows(InvalidDataConversionException.class, ()->model.getList("f2"));
		assertThrows(InvalidDataConversionException.class, ()->model.getList("f3"));
		assertThrows(InvalidDataConversionException.class, ()->model.getList("f4"));
		assertNull(model.getList("f2a"));
		assertNull(model.getList("f3a"));
		assertNull(model.getList("f4a"));
		assertNull(model.getList("f5"));
		assertNull(model.getList("f6"));
		assertNull(model.getList("f7"));
		assertNull(model.getList("f8"));

		assertEquals("[{a:'b'}]", model.getList("f1", l).toString());
		assertThrows(InvalidDataConversionException.class, ()->model.getList("f2", l));
		assertThrows(InvalidDataConversionException.class, ()->model.getList("f3", l));
		assertThrows(InvalidDataConversionException.class, ()->model.getList("f4", l));
		assertEquals("[{a:'b'}]", model.getList("f2a", l).toString());
		assertEquals("[{a:'b'}]", model.getList("f3a", l).toString());
		assertEquals("[{a:'b'}]", model.getList("f4a", l).toString());
		assertEquals("[{a:'b'}]", model.getList("f5", l).toString());
		assertEquals("[{a:'b'}]", model.getList("f6", l).toString());
		assertEquals("[{a:'b'}]", model.getList("f7", l).toString());
		assertEquals("[{a:'b'}]", model.getList("f8", l).toString());

		assertNull(model.getJsonList("f1"));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonList("f2"));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonList("f3"));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonList("f4"));
		assertNull(model.getJsonList("f2a"));
		assertNull(model.getJsonList("f3a"));
		assertNull(model.getJsonList("f4a"));
		assertNull(model.getJsonList("f5"));
		assertNull(model.getJsonList("f6"));
		assertNull(model.getJsonList("f7"));
		assertNull(model.getJsonList("f8"));

		assertEquals("[{\"a\":\"b\"}]", model.getJsonList("f1", jl).toString());
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonList("f2", jl));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonList("f3", jl));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonList("f4", jl));
		assertEquals("[{\"a\":\"b\"}]", model.getJsonList("f2a", jl).toString());
		assertEquals("[{\"a\":\"b\"}]", model.getJsonList("f3a", jl).toString());
		assertEquals("[{\"a\":\"b\"}]", model.getJsonList("f4a", jl).toString());
		assertEquals("[{\"a\":\"b\"}]", model.getJsonList("f5", jl).toString());
		assertEquals("[{\"a\":\"b\"}]", model.getJsonList("f6", jl).toString());
		assertEquals("[{\"a\":\"b\"}]", model.getJsonList("f7", jl).toString());
		assertEquals("[{\"a\":\"b\"}]", model.getJsonList("f8", jl).toString());

		((A)model.getRootObject()).init();

		assertEquals("1", model.get("f1"));
		assertEquals("2", model.get("f2").toString());
		assertEquals("3", model.get("f3").toString());
		assertEquals("true", model.get("f4").toString());
		assertEquals("2", model.get("f2a").toString());
		assertEquals("3", model.get("f3a").toString());
		assertEquals("true", model.get("f4a").toString());
		assertEquals("{f5a:'a'}", model.get("f5").toString());
		assertEquals("[{f6a:'a'}]", model.get("f6").toString());
		assertEquals("{f5a:'a'}", model.get("f7").toString());
		assertEquals("[{f6a:'a'}]", model.get("f8").toString());

		assertEquals("1", model.getWithDefault("f1", "foo"));
		assertEquals("2", model.getWithDefault("f2", "foo").toString());
		assertEquals("3", model.getWithDefault("f3", "foo").toString());
		assertEquals("true", model.getWithDefault("f4", "foo").toString());
		assertEquals("2", model.getWithDefault("f2a", "foo").toString());
		assertEquals("3", model.getWithDefault("f3a", "foo").toString());
		assertEquals("true", model.getWithDefault("f4a", "foo").toString());
		assertEquals("{f5a:'a'}", model.getWithDefault("f5", "foo").toString());
		assertEquals("[{f6a:'a'}]", model.getWithDefault("f6", "foo").toString());
		assertEquals("{f5a:'a'}", model.getWithDefault("f7", "foo").toString());
		assertEquals("[{f6a:'a'}]", model.getWithDefault("f8", "foo").toString());

		assertEquals("1", model.getString("f1"));
		assertEquals("2", model.getString("f2"));
		assertEquals("3", model.getString("f3"));
		assertEquals("true", model.getString("f4"));
		assertEquals("2", model.getString("f2a"));
		assertEquals("3", model.getString("f3a"));
		assertEquals("true", model.getString("f4a"));
		assertEquals("{f5a:'a'}", model.getString("f5"));
		assertEquals("[{f6a:'a'}]", model.getString("f6"));
		assertEquals("{f5a:'a'}", model.getString("f7"));
		assertEquals("[{f6a:'a'}]", model.getString("f8"));

		assertEquals("1", model.getString("f1", "foo"));
		assertEquals("2", model.getString("f2", "foo"));
		assertEquals("3", model.getString("f3", "foo"));
		assertEquals("true", model.getString("f4", "foo"));
		assertEquals("2", model.getString("f2a", "foo"));
		assertEquals("3", model.getString("f3a", "foo"));
		assertEquals("true", model.getString("f4a", "foo"));
		assertEquals("{f5a:'a'}", model.getString("f5", "foo"));
		assertEquals("[{f6a:'a'}]", model.getString("f6", "foo"));
		assertEquals("{f5a:'a'}", model.getString("f7", "foo"));
		assertEquals("[{f6a:'a'}]", model.getString("f8", "foo"));

		assertEquals(1, (int)model.getInt("f1"));
		assertEquals(2, (int)model.getInt("f2"));
		assertEquals(3, (int)model.getInt("f3"));
		assertEquals(1, (int)model.getInt("f4"));
		assertEquals(2, (int)model.getInt("f2a"));
		assertEquals(3, (int)model.getInt("f3a"));
		assertEquals(1, (int)model.getInt("f4a"));
		assertThrows(InvalidDataConversionException.class, ()->model.getInt("f5"));
		assertThrows(InvalidDataConversionException.class, ()->model.getInt("f6"));
		assertThrows(InvalidDataConversionException.class, ()->model.getInt("f7"));
		assertThrows(InvalidDataConversionException.class, ()->model.getInt("f8"));

		assertEquals(1, (int)model.getInt("f1", 9));
		assertEquals(2, (int)model.getInt("f2", 9));
		assertEquals(3, (int)model.getInt("f3", 9));
		assertEquals(1, (int)model.getInt("f4", 9));
		assertEquals(2, (int)model.getInt("f2a", 9));
		assertEquals(3, (int)model.getInt("f3a", 9));
		assertEquals(1, (int)model.getInt("f4a", 9));
		assertThrows(InvalidDataConversionException.class, ()->model.getInt("f5", 9));
		assertThrows(InvalidDataConversionException.class, ()->model.getInt("f6", 9));
		assertThrows(InvalidDataConversionException.class, ()->model.getInt("f7", 9));
		assertThrows(InvalidDataConversionException.class, ()->model.getInt("f8", 9));

		assertEquals(1, (long)model.getLong("f1"));
		assertEquals(2, (long)model.getLong("f2"));
		assertEquals(3, (long)model.getLong("f3"));
		assertEquals(1, (long)model.getLong("f4"));
		assertEquals(2, (long)model.getLong("f2a"));
		assertEquals(3, (long)model.getLong("f3a"));
		assertEquals(1, (long)model.getLong("f4a"));
		assertThrows(InvalidDataConversionException.class, ()->model.getLong("f5"));
		assertThrows(InvalidDataConversionException.class, ()->model.getLong("f6"));
		assertThrows(InvalidDataConversionException.class, ()->model.getLong("f7"));
		assertThrows(InvalidDataConversionException.class, ()->model.getInt("f8"));

		assertEquals(1, (long)model.getLong("f1", 9L));
		assertEquals(2, (long)model.getLong("f2", 9L));
		assertEquals(3, (long)model.getLong("f3", 9L));
		assertEquals(1, (long)model.getLong("f4", 9L));
		assertEquals(2, (long)model.getLong("f2a", 9L));
		assertEquals(3, (long)model.getLong("f3a", 9L));
		assertEquals(1, (long)model.getLong("f4a", 9L));
		assertThrows(InvalidDataConversionException.class, ()->model.getLong("f5", 9L));
		assertThrows(InvalidDataConversionException.class, ()->model.getLong("f6", 9L));
		assertThrows(InvalidDataConversionException.class, ()->model.getLong("f7", 9L));
		assertThrows(InvalidDataConversionException.class, ()->model.getLong("f8", 9L));

		assertEquals(false, model.getBoolean("f1"));  // String "1" equates to false.
		assertEquals(true, model.getBoolean("f2"));
		assertEquals(true, model.getBoolean("f3"));
		assertEquals(true, model.getBoolean("f4"));
		assertEquals(true, model.getBoolean("f2a"));
		assertEquals(true, model.getBoolean("f3a"));
		assertEquals(true, model.getBoolean("f4a"));
		assertEquals(false, model.getBoolean("f5"));  // "{a:'b'}" equates to false.
		assertEquals(false, model.getBoolean("f6"));
		assertEquals(false, model.getBoolean("f7"));
		assertEquals(false, model.getBoolean("f8"));

		assertEquals(false, model.getBoolean("f1", true));  // String "1" equates to false.
		assertEquals(true, model.getBoolean("f2", true));
		assertEquals(true, model.getBoolean("f3", true));
		assertEquals(true, model.getBoolean("f4", true));
		assertEquals(true, model.getBoolean("f2a", true));
		assertEquals(true, model.getBoolean("f3a", true));
		assertEquals(true, model.getBoolean("f4a", true));
		assertEquals(false, model.getBoolean("f5", true));  // "{a:'b'}" equates to false.
		assertEquals(false, model.getBoolean("f6", true));
		assertEquals(false, model.getBoolean("f7", true));
		assertEquals(false, model.getBoolean("f8", true));

		assertThrows(InvalidDataConversionException.class, ()->model.getMap("f1"));
		assertThrows(InvalidDataConversionException.class, ()->model.getMap("f2"));
		assertThrows(InvalidDataConversionException.class, ()->model.getMap("f3"));
		assertThrows(InvalidDataConversionException.class, ()->model.getMap("f4"));
		assertThrows(InvalidDataConversionException.class, ()->model.getMap("f2a"));
		assertThrows(InvalidDataConversionException.class, ()->model.getMap("f3a"));
		assertThrows(InvalidDataConversionException.class, ()->model.getMap("f4a"));
		assertEquals("{f5a:'a'}", model.getMap("f5").toString());
		assertThrows(InvalidDataConversionException.class, ()->model.getMap("f6"));
		assertEquals("{f5a:'a'}", model.getMap("f7").toString());
		assertThrows(InvalidDataConversionException.class, ()->model.getMap("f8"));

		assertThrows(InvalidDataConversionException.class, ()->model.getMap("f1", m));
		assertThrows(InvalidDataConversionException.class, ()->model.getMap("f2", m));
		assertThrows(InvalidDataConversionException.class, ()->model.getMap("f3", m));
		assertThrows(InvalidDataConversionException.class, ()->model.getMap("f4", m));
		assertThrows(InvalidDataConversionException.class, ()->model.getMap("f2a", m));
		assertThrows(InvalidDataConversionException.class, ()->model.getMap("f3a", m));
		assertThrows(InvalidDataConversionException.class, ()->model.getMap("f4a", m));
		assertEquals("{f5a:'a'}", model.getMap("f5", m).toString());
		assertThrows(InvalidDataConversionException.class, ()->model.getMap("f6", m));
		assertEquals("{f5a:'a'}", model.getMap("f7", m).toString());
		assertThrows(InvalidDataConversionException.class, ()->model.getMap("f8", m));

		assertThrows(InvalidDataConversionException.class, ()->model.getJsonMap("f1"));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonMap("f2"));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonMap("f3"));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonMap("f4"));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonMap("f2a"));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonMap("f3a"));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonMap("f4a"));
		assertEquals("{\"f5a\":\"a\"}", model.getJsonMap("f5").toString());
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonMap("f6"));
		assertEquals("{\"f5a\":\"a\"}", model.getJsonMap("f7").toString());
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonMap("f8"));

		assertThrows(InvalidDataConversionException.class, ()->model.getJsonMap("f1", jm));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonMap("f2", jm));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonMap("f3", jm));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonMap("f4", jm));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonMap("f2a", jm));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonMap("f3a", jm));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonMap("f4a", jm));
		assertEquals("{\"f5a\":\"a\"}", model.getJsonMap("f5", jm).toString());
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonMap("f6", jm));
		assertEquals("{\"f5a\":\"a\"}", model.getJsonMap("f7", jm).toString());
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonMap("f8", jm));

		assertThrows(InvalidDataConversionException.class, ()->model.getList("f1"));
		assertThrows(InvalidDataConversionException.class, ()->model.getList("f2"));
		assertThrows(InvalidDataConversionException.class, ()->model.getList("f3"));
		assertThrows(InvalidDataConversionException.class, ()->model.getList("f4"));
		assertThrows(InvalidDataConversionException.class, ()->model.getList("f2a"));
		assertThrows(InvalidDataConversionException.class, ()->model.getList("f3a"));
		assertThrows(InvalidDataConversionException.class, ()->model.getList("f4a"));
		assertEquals("[{\"f5a\":\"a\"}]", model.getList("f5").toString());
		assertEquals("[{f6a:'a'}]", model.getList("f6").toString());
		assertEquals("[{\"f5a\":\"a\"}]", model.getList("f7").toString());
		assertEquals("[{f6a:'a'}]", model.getList("f8").toString());

		assertThrows(InvalidDataConversionException.class, ()->model.getList("f1", l));
		assertThrows(InvalidDataConversionException.class, ()->model.getList("f2", l));
		assertThrows(InvalidDataConversionException.class, ()->model.getList("f3", l));
		assertThrows(InvalidDataConversionException.class, ()->model.getList("f4", l));
		assertThrows(InvalidDataConversionException.class, ()->model.getList("f2a", l));
		assertThrows(InvalidDataConversionException.class, ()->model.getList("f3a", l));
		assertThrows(InvalidDataConversionException.class, ()->model.getList("f4a", l));
		assertEquals("[{\"f5a\":\"a\"}]", model.getList("f5", l).toString());
		assertEquals("[{f6a:'a'}]", model.getList("f6", l).toString());
		assertEquals("[{\"f5a\":\"a\"}]", model.getList("f7", l).toString());
		assertEquals("[{f6a:'a'}]", model.getList("f8", l).toString());

		assertThrows(InvalidDataConversionException.class, ()->model.getJsonList("f1"));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonList("f2"));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonList("f3"));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonList("f4"));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonList("f2a"));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonList("f3a"));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonList("f4a"));
		assertEquals("[{\"f5a\":\"a\"}]", model.getJsonList("f5").toString());
		assertEquals("[{\"f6a\":\"a\"}]", model.getJsonList("f6").toString());
		assertEquals("[{\"f5a\":\"a\"}]", model.getJsonList("f7").toString());
		assertEquals("[{\"f6a\":\"a\"}]", model.getJsonList("f8").toString());

		assertThrows(InvalidDataConversionException.class, ()->model.getJsonList("f1", jl));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonList("f2", jl));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonList("f3", jl));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonList("f4", jl));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonList("f2a", jl));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonList("f3a", jl));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonList("f4a", jl));
		assertEquals("[{\"f5a\":\"a\"}]", model.getJsonList("f5", jl).toString());
		assertEquals("[{\"f6a\":\"a\"}]", model.getJsonList("f6", jl).toString());
		assertEquals("[{\"f5a\":\"a\"}]", model.getJsonList("f7", jl).toString());
		assertEquals("[{\"f6a\":\"a\"}]", model.getJsonList("f8", jl).toString());
	}

	public static class A {
		public String f1;
		public int f2;
		public long f3;
		public boolean f4;
		public Integer f2a;
		public Long f3a;
		public Boolean f4a;
		public Map f5;
		public List f6;
		public Json5Map f7;
		public Json5List f8;

		public A init() {
			f1 = "1";
			f2 = 2;
			f3 = 3L;
			f4 = true;
			f2a = 2;
			f3a = 3L;
			f4a = true;
			try {
				f5 = Json5Map.ofString("{f5a:'a'}");
				f6 = Json5List.ofString("[{f6a:'a'}]");
				f7 = Json5Map.ofString("{f5a:'a'}");
				f8 = Json5List.ofString("[{f6a:'a'}]");
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
			return this;
		}
	}

	//====================================================================================================
	// getClassMeta(String url)
	//====================================================================================================
	@Test void f04_getClassMeta() {
		var model = PathTraversal.create(new AddressBook().init());
		assertEquals("Person", model.getClassMeta("0").inner().getSimpleName());
		assertEquals("String", model.getClassMeta("0/addresses/0/state").inner().getSimpleName());
		assertNull(model.getClassMeta("1"));
		assertNull(model.getClassMeta("0/addresses/1/state"));
	}

	//====================================================================================================
	// g - get(url, Type, Type[]) and getWithDefault(url, def, Type, Type[]) - parameterized typed getters
	//====================================================================================================
	@Test void g01_getParameterized_listOfStrings() {
		var model = PathTraversal.create(Json5Map.ofString("{a:['x','y','z']}"));
		List<String> v = model.get("a", LinkedList.class, String.class);
		assertEquals(3, v.size());
		assertEquals("x", v.get(0));
		assertEquals("z", v.get(2));
	}

	@Test void g02_getParameterized_mapOfStrings() {
		var model = PathTraversal.create(Json5Map.ofString("{a:{x:'1',y:'2'}}"));
		Map<String,Integer> v = model.get("a", TreeMap.class, String.class, Integer.class);
		assertEquals(2, v.size());
		assertEquals(Integer.valueOf(1), v.get("x"));
	}

	@Test void g03_getParameterized_nullReturnsNull() {
		var model = PathTraversal.create(new Json5Map());
		List<String> v = model.get("missing", LinkedList.class, String.class);
		assertNull(v);
	}

	@Test void g04_getWithDefaultParameterized_returnsDefault() {
		var model = PathTraversal.create(new Json5Map());
		var def = new LinkedList<String>();
		def.add("default");
		List<String> v = model.getWithDefault("missing", def, LinkedList.class, String.class);
		assertSame(def, v);
	}

	@Test void g05_getWithDefaultParameterized_returnsConverted() {
		var model = PathTraversal.create(Json5Map.ofString("{a:['x','y']}"));
		var def = new LinkedList<String>();
		List<String> v = model.getWithDefault("a", def, LinkedList.class, String.class);
		assertEquals(2, v.size());
		assertEquals("x", v.get(0));
	}

	//====================================================================================================
	// h - PUT operations exercising deep paths, lists, arrays, beans, errors
	//====================================================================================================
	@Test void h01_put_deepPathInMap() {
		// MarshalledMap.put now returns the prior value from the underlying LinkedHashMap,
		// matching the previous-value contract documented on PathTraversal.put.
		var inner = new Json5Map();
		inner.put("c", "oldVal");
		var middle = new Json5Map();
		middle.put("b", inner);
		var outer = new Json5Map();
		outer.put("a", middle);
		var model = PathTraversal.create(outer);
		var prev = model.put("a/b/c", "newVal");
		assertEquals("oldVal", prev);
		assertEquals("newVal", model.get("a/b/c"));
	}

	@Test void h02_put_intoList() {
		var model = PathTraversal.create(Json5Map.ofString("{l:['a','b','c']}"));
		var prev = model.put("l/1", "B");
		assertEquals("b", prev);
		assertEquals("B", model.get("l/1"));
	}

	@Test void h03a_put_parentNotFound_404() {
		// parentUrl resolves through a null intermediate -> getNode returns null -> 404
		var m = new HashMap<String,Object>();
		m.put("a", null);
		var model = PathTraversal.create(m);
		assertThrowsWithMessage(PathTraversalException.class, "Node at URL 'a/b' not found.", ()->model.put("a/b/c", "v"));
	}

	@Test void h03_put_parentMissing_404() {
		// When a parent URL resolves to a missing key in a Map, the resolved JsonNode has o==null and cm
		// defaults to Object; service() detects this and returns 404.
		var model = PathTraversal.create(new Json5Map());
		assertThrowsWithMessage(PathTraversalException.class, "Node at URL 'missing' not found.", ()->model.put("missing/x", "v"));
	}

	@Test void h04_put_intoBean() {
		// BeanMap.put may not return prior value depending on impl; we verify the put took effect.
		var model = PathTraversal.create(new AddressBook().init());
		model.put("0/name", "Hillary Clinton");
		assertEquals("Hillary Clinton", model.get("0/name"));
	}

	@Test void h05_put_intoArray_parentMap() {
		// Map containing array property -> PUT into array slot
		var arr = new String[]{"a","b","c"};
		var map = new Json5Map();
		map.put("arr", arr);
		var model = PathTraversal.create(map);
		var result = model.put("arr/1", "B");
		assertEquals("arr/1", result);
		assertEquals("B", ((String[])map.get("arr"))[1]);
	}

	@Test void h06_put_intoArray_parentBean() {
		var p = new Person("a", 1, new Address("s","c","st",1,true), new Address("s2","c2","st2",2,false));
		var model = PathTraversal.create(p);
		var newAddr = new Address("xs","xc","xst",99,true);
		var result = model.put("addresses/0", newAddr);
		assertEquals("addresses/0", result);
		assertEquals("xc", p.addresses[0].city);
	}

	@Test void h07_put_intoArray_listIndexOutOfBounds_expands() {
		// setArrayEntry expansion path (a.length <= index)
		var p = new Person("a", 1, new Address("s","c","st",1,true));
		var model = PathTraversal.create(p);
		var newAddr = new Address("xs","xc","xst",99,true);
		var result = model.put("addresses/3", newAddr);
		assertEquals("addresses/3", result);
		assertEquals(4, p.addresses.length);
		assertNull(p.addresses[1]);
	}

	@Test void h08_put_nonIntegerArrayIndexThrows() {
		var p = new Person("a", 1, new Address("s","c","st",1,true));
		var model = PathTraversal.create(p);
		assertThrowsWithMessage(PathTraversalException.class, "Cannot address an item in an array with a non-integer key", ()->model.put("addresses/foo", new Address()));
	}

	@Test void h09_put_intoLeafTypeThrows() {
		// PUT into a node whose parent is a bean property of type "Object" -- when leaf is a String, can't traverse further
		var model = PathTraversal.create(Json5Map.ofString("{name:'foo'}"));
		// "name" resolves to a String; trying to PUT name/x means the parent ("name") is leaf of unsupported type
		assertThrowsWithMessage(PathTraversalException.class, "Cannot perform PUT on", ()->model.put("name/x", "v"));
	}

	@Test void h11_put_setNotListThrows() {
		// Set is a Collection but not a List — service() emits a clear "non-List collection" error.
		var s = new LinkedHashSet<String>();
		s.add("a");
		var m = new HashMap<String,Object>();
		m.put("s", s);
		var model = PathTraversal.create(m);
		assertThrowsWithMessage(PathTraversalException.class, "Cannot PUT to indexed position in non-List collection 's/0'", ()->model.put("s/0", "x"));
	}

	@Test void h10_put_mapWithBeanValueType_convertsMap() {
		// Cover convert() bean+Map path: put a HashMap into a typed map slot whose value type is Address
		var p = new Person("a", 1, new Address("s","c","st",1,true));
		var model = PathTraversal.create(p);
		var asMap = new HashMap<String,Object>();
		asMap.put("street","S2"); asMap.put("city","C2"); asMap.put("state","ST2"); asMap.put("zip",2); asMap.put("isCurrent",false);
		model.put("addresses/0", asMap);
		assertEquals("C2", p.addresses[0].city);
	}

	@Test void h12_put_intoArray_parentNotMapOrBean_throws() {
		// Array nested directly inside a List: the array's parent is a Collection (not Map/Bean) -> PUT error.
		var root = new Json5List();
		root.add(new String[]{"a","b"});
		var model = PathTraversal.create(root);
		assertThrowsWithMessage(PathTraversalException.class, "Cannot perform PUT on '0/1' with parent node type", ()->model.put("0/1", "B"));
	}

	//====================================================================================================
	// i - POST operations
	//====================================================================================================
	@Test void i01_post_toCollection_returnsIndexedUrl() {
		var model = PathTraversal.create(Json5Map.ofString("{l:['a']}"));
		var ret = model.post("l", "b");
		assertEquals("l/1", ret);
	}

	@Test void i02_post_toRoot_listAddsAndReturnsUrl() {
		var model = PathTraversal.create(new Json5List());
		var ret = model.post("", "x");
		assertEquals("/0", ret);
	}

	@Test void i03_post_toRoot_array() {
		var model = PathTraversal.create(new String[]{"a","b"});
		var ret = model.post("", "c");
		assertEquals("/2", ret);
		var arr = (String[])model.getRootObject();
		assertEquals(3, arr.length);
		assertEquals("c", arr[2]);
	}

	@Test void i04_post_toRoot_invalidTypeThrows() {
		var model = PathTraversal.create(new Json5Map());
		assertThrowsWithMessage(PathTraversalException.class, "Cannot perform POST on", ()->model.post("", "v"));
	}

	@Test void i05_post_missingNode_404() {
		// Same scenario as h03 — a missing key resolves to an Object-typed node; service() detects this and returns 404.
		var model = PathTraversal.create(new Json5Map());
		assertThrowsWithMessage(PathTraversalException.class, "Node at URL 'missing' not found.", ()->model.post("missing", "v"));
	}

	@Test void i06_post_toArray_parentMap() {
		var map = new Json5Map();
		map.put("arr", new String[]{"a","b"});
		var model = PathTraversal.create(map);
		var ret = model.post("arr", "c");
		assertEquals("arr/2", ret);
		assertEquals(3, ((String[])map.get("arr")).length);
	}

	@Test void i07_post_toArray_parentBean() {
		var p = new Person("a", 1, new Address("s","c","st",1,true));
		var model = PathTraversal.create(p);
		var newAddr = new Address("xs","xc","xst",99,true);
		var ret = model.post("addresses", newAddr);
		assertEquals("addresses/1", ret);
		assertEquals(2, p.addresses.length);
	}

	@Test void i07a_post_toRootSet_returnsNullUrl() {
		// POST to root Set: c.add returns true, but `c instanceof List` false -> service returns null.
		var s = new LinkedHashSet<String>();
		s.add("a");
		var model = PathTraversal.create(s);
		var ret = model.post("", "b");
		assertNull(ret);
		assertTrue(s.contains("b"));
	}

	@Test void i07b_post_toSet_returnsNullUrl() {
		// POST to non-root Set inside a map.
		var s = new LinkedHashSet<String>();
		s.add("a");
		var m = new HashMap<String,Object>();
		m.put("s", s);
		var model = PathTraversal.create(m);
		var ret = model.post("s", "b");
		assertNull(ret);
		assertTrue(s.contains("b"));
	}

	@Test void i08_post_toLeafTypeThrows() {
		var model = PathTraversal.create(Json5Map.ofString("{name:'foo'}"));
		assertThrowsWithMessage(PathTraversalException.class, "Cannot perform POST on", ()->model.post("name", "v"));
	}

	@Test void i09_post_toArray_parentNotMapOrBean_throws() {
		// Array nested directly inside a List: the array's parent is a Collection (not Map/Bean) -> POST error.
		var root = new Json5List();
		root.add(new String[]{"a","b"});
		var model = PathTraversal.create(root);
		assertThrowsWithMessage(PathTraversalException.class, "Cannot perform POST on '0' with parent node type", ()->model.post("0", "c"));
	}

	@Test void i10_post_nodeNotFound_404() {
		// POST where the target URL resolves through a null intermediate -> getNode returns null -> 404.
		var m = new HashMap<String,Object>();
		m.put("a", null);
		var model = PathTraversal.create(m);
		assertThrowsWithMessage(PathTraversalException.class, "Node at URL 'a/b' not found.", ()->model.post("a/b", "v"));
	}

	//====================================================================================================
	// j - DELETE operations
	//====================================================================================================
	@Test void j01_delete_fromMap() {
		var model = PathTraversal.create(Json5Map.ofString("{a:'x',b:'y'}"));
		var prev = model.delete("a");
		assertEquals("x", prev);
		assertNull(model.get("a"));
	}

	@Test void j02_delete_fromList() {
		var model = PathTraversal.create(Json5Map.ofString("{l:['a','b','c']}"));
		var prev = model.delete("l/1");
		assertEquals("b", prev);
		var l = (List<?>)model.get("l");
		assertEquals(2, l.size());
		assertEquals("a", l.get(0));
		assertEquals("c", l.get(1));
	}

	@Test void j03_delete_fromArray_parentMap() {
		var map = new Json5Map();
		map.put("arr", new String[]{"a","b","c"});
		var model = PathTraversal.create(map);
		var prev = model.delete("arr/1");
		assertEquals("b", prev);
		assertEquals(2, ((String[])map.get("arr")).length);
	}

	@Test void j04_delete_fromArray_parentBean() {
		var p = new Person("a", 1, new Address("s","c","st",1,true), new Address("s2","c2","st2",2,false));
		var model = PathTraversal.create(p);
		var prev = (Address)model.delete("addresses/0");
		assertEquals("c", prev.city);
		assertEquals(1, p.addresses.length);
	}

	@Test void j05_delete_fromBean_setsNull() {
		var model = PathTraversal.create(new AddressBook().init());
		// DELETE on bean property invokes BeanMap.put(key, null)
		model.delete("0/name");
		var p = (Person)model.get("0");
		assertNull(p.name);
	}

	@Test void j06_delete_root() {
		var model = PathTraversal.create(Json5Map.ofString("{a:'x'}"));
		var prev = model.delete("");
		assertEquals("{a:'x'}", prev.toString());
		assertNull(model.getRootObject());
	}

	@Test void j07_delete_root_locked() {
		var model = PathTraversal.create(new Json5Map()).setRootLocked();
		assertThrowsWithMessage(PathTraversalException.class, "Cannot overwrite root object", ()->model.delete(""));
	}

	@Test void j08_delete_nonIntegerArrayIndex() {
		var p = new Person("a", 1, new Address("s","c","st",1,true));
		var model = PathTraversal.create(p);
		assertThrowsWithMessage(PathTraversalException.class, "Cannot address an item in an array with a non-integer key", ()->model.delete("addresses/foo"));
	}

	@Test void j09a_delete_setNotListThrows() {
		// Set is a Collection but not a List — service() emits a clear "non-List collection" error.
		var s = new LinkedHashSet<String>();
		s.add("a"); s.add("b");
		var m = new HashMap<String,Object>();
		m.put("s", s);
		var model = PathTraversal.create(m);
		assertThrowsWithMessage(PathTraversalException.class, "Cannot DELETE indexed position in non-List collection 's/0'", ()->model.delete("s/0"));
	}

	@Test void j09_delete_unsupportedLeafTypeThrows() {
		// DELETE on a leaf node whose parent type isn't map/list/array/bean
		var model = PathTraversal.create(Json5Map.ofString("{name:'foo'}"));
		assertThrowsWithMessage(PathTraversalException.class, "Cannot perform PUT on", ()->model.delete("name/x"));
	}

	@Test void j10_delete_fromArray_parentNotMapOrBean_throws() {
		// Array nested directly inside a List: the array's parent is a Collection (not Map/Bean) -> DELETE error.
		var root = new Json5List();
		root.add(new String[]{"a","b"});
		var model = PathTraversal.create(root);
		assertThrowsWithMessage(PathTraversalException.class, "Cannot perform POST on '0/1' with parent node type", ()->model.delete("0/1"));
	}

	@Test void j11_delete_parentMissing_404() {
		// DELETE where the parent URL resolves to a missing key (o==null, Object meta) -> 404.
		var model = PathTraversal.create(new Json5Map());
		assertThrowsWithMessage(PathTraversalException.class, "Node at URL 'missing' not found.", ()->model.delete("missing/x"));
	}

	@Test void j12_delete_parentNotFound_404() {
		// DELETE where the parent URL resolves through a null intermediate -> getNode returns null -> 404.
		var m = new HashMap<String,Object>();
		m.put("a", null);
		var model = PathTraversal.create(m);
		assertThrowsWithMessage(PathTraversalException.class, "Node at URL 'a/b' not found.", ()->model.delete("a/b/c"));
	}

	//====================================================================================================
	// k - getNode navigation: null o, list/array bounds, bean unknown property
	//====================================================================================================
	@Test void k01_get_nullValueInMap() {
		// get path that traverses through a null value in a map
		var model = PathTraversal.create(Json5Map.ofString("{a:null}"));
		assertNull(model.get("a/b"));
	}

	@Test void k02_get_listIndexOutOfBounds_returnsNull() {
		var model = PathTraversal.create(Json5Map.ofString("{l:['a','b']}"));
		assertNull(model.get("l/5"));
	}

	@Test void k03_get_arrayIndexOutOfBounds_returnsNull() {
		var model = PathTraversal.create(new String[]{"a","b"});
		assertNull(model.get("5"));
	}

	@Test void k04_get_arrayElement() {
		var model = PathTraversal.create(new String[]{"a","b","c"});
		assertEquals("b", model.get("1"));
	}

	@Test void k05_get_listNonIntegerKeyThrows() {
		var model = PathTraversal.create(Json5Map.ofString("{l:['a','b']}"));
		assertThrowsWithMessage(PathTraversalException.class, "Cannot address an item in an array with a non-integer key", ()->model.get("l/foo"));
	}

	@Test void k06_get_beanUnknownPropertyThrows() {
		var model = PathTraversal.create(new AddressBook().init());
		assertThrowsWithMessage(PathTraversalException.class, "Unknown property", ()->model.get("0/notARealField"));
	}

	@Test void k07_get_blankAndSlashReturnRoot() {
		var m = new Json5Map();
		var model = PathTraversal.create(m);
		assertSame(m, model.get(""));
		assertSame(m, model.get("/"));
		assertSame(m, model.get(null));
	}

	@Test void k08_get_deepPathThroughMixedTypes() {
		var p = new Person("a", 1, new Address("s","city1","st",1,true), new Address("s","city2","st",2,false));
		var model = PathTraversal.create(p);
		assertEquals("city2", model.get("addresses/1/city"));
	}

	@Test void k09a_getNode_collectionNotList() {
		// cm.isCollection() && o instanceof List branch: if collection is a Set (not List), the
		// second condition fails, so ct2 remains null. Confirms current behavior.
		var s = new HashSet<String>();
		s.add("x"); s.add("y");
		var m = new HashMap<String,Object>();
		m.put("s", s);
		var model = PathTraversal.create(m);
		// Indexing into a Set traverses through the (cm.isCollection() && !instanceof List) branch
		// and returns a JsonNode with o=null cm=Object since ct2 stays null. The result is null.
		assertNull(model.get("s/0"));
	}

	@Test void k09_getNode_nullObjectInBean() {
		// Bean property whose value is null - traversal returns null
		var p = new Person();
		p.name = null;
		var model = PathTraversal.create(p);
		assertNull(model.get("name/sub"));
	}

	//====================================================================================================
	// m - JsonNode constructor branch coverage (cm == null path, cm == object path)
	//====================================================================================================
	@Test void m01_jsonNode_nullValueGetsObjectMeta() {
		// Construct PathTraversal over null - JsonNode constructor takes the o==null branch in cm assignment
		var model = PathTraversal.create(null);
		assertNull(model.getRootObject());
		// PUT "" replaces root even when starting from null
		model.put("", "x");
		assertEquals("x", model.getRootObject());
	}

	@Test void m02_create_withParserUsesParserSession() {
		var model = PathTraversal.create(new Json5Map(), Json5Parser.DEFAULT);
		model.put("a", "v");
		assertEquals("v", model.get("a"));
	}

	//====================================================================================================
	// n - URI/path syntax: leading slashes, empty segments
	//====================================================================================================
	@Test void n01_path_leadingSlashIgnored() {
		var model = PathTraversal.create(Json5Map.ofString("{a:'v'}"));
		assertEquals("v", model.get("a"));
		assertEquals("v", model.get("/a"));
	}

	@Test void n02_path_listIndexNavigation() {
		var model = PathTraversal.create(Json5Map.ofString("{l:[{x:1},{x:2},{x:3}]}"));
		assertEquals(2, model.getInt("l/1/x"));
	}

	@Test void n03_post_emptyRootIsOk() {
		var model = PathTraversal.create(new Json5List());
		model.post(null, "x");
		model.post("/", "y");
		var l = (List<?>)model.getRootObject();
		assertEquals(2, l.size());
		assertEquals("x", l.get(0));
		assertEquals("y", l.get(1));
	}

	//====================================================================================================
	// o - Patch-like behavior: mix of put/delete on the same model
	//====================================================================================================
	@Test void o01_patch_likeWorkflow() {
		var model = PathTraversal.create(Json5Map.ofString("{user:{name:'Alice',roles:['admin','editor']}}"));
		model.put("user/name", "Bob");
		model.delete("user/roles/0");
		model.post("user/roles", "viewer");
		assertEquals("Bob", model.get("user/name"));
		var roles = (List<?>)model.get("user/roles");
		assertEquals(2, roles.size());
		assertEquals("editor", roles.get(0));
		assertEquals("viewer", roles.get(1));
	}

	//====================================================================================================
	// p - PathTraversalException
	//====================================================================================================
	@Test void p01_getStatus() {
		assertEquals(404, new PathTraversalException(404, "msg").getStatus());
	}

	@Test void p02_causeConstructor() {
		var cause = new RuntimeException("boom");
		var x = new PathTraversalException(cause, 500, "Failed %s", "x");
		assertSame(cause, x.getCause());
		assertEquals(500, x.getStatus());
		assertEquals("Failed x", x.getMessage());
	}

	@Test void p03_covariantSetMessage() {
		var x = new PathTraversalException(400, "orig");
		// setMessage is covariantly typed to return PathTraversalException and returns the same instance.
		PathTraversalException y = x.setMessage("new %s", "msg");
		assertSame(x, y);
		assertEquals("new msg", x.getMessage());
	}
}
