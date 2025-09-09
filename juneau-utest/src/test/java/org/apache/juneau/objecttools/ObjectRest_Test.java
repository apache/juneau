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
package org.apache.juneau.objecttools;

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({"rawtypes","serial"})
class ObjectRest_Test extends SimpleTestBase {

	//====================================================================================================
	// testBasic
	//====================================================================================================
	@Test void a01_basic() {

		var model = ObjectRest.create(new JsonMap()); // An empty model.

		// Do a PUT
		model.put("A", new JsonMap());
		model.put("A/B", new JsonMap());
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
		var model = ObjectRest.create(new JsonMap());

		// Java beans.
		var p = new Person("some name", 123,
			new Address("street A", "city A", "state A", 12345, true),
			new Address("street B", "city B", "state B", 12345, false)
		);
		model.put("/person1", p);

		// Make sure it got stored correctly.
		var serializer = JsonSerializer.create().json5().addRootType().build();
		assertEquals("{person1:{name:'some name',age:123,addresses:[{street:'street A',city:'city A',state:'state A',zip:12345,isCurrent:true},{street:'street B',city:'city B',state:'state B',zip:12345,isCurrent:false}]}}", serializer.serialize(model.getRootObject()));

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
		var s = serializer.serialize(p);
		var expectedValue = "{_type:'Person',name:'some name',age:123,addresses:[{street:'street A',city:'city A',state:'state A',zip:12345,isCurrent:true},{street:'street B',city:'city B',state:'state B',zip:12345,isCurrent:false}]}";
		assertEquals(expectedValue, s);

		// Parse it back to Java objects.
		p = (Person)JsonParser.create().beanDictionary(Person.class).build().parse(s, Object.class);
		expectedValue = "city B";
		s = p.addresses[1].city;
		assertEquals(expectedValue, s);

		// Parse it back into JSON again.
		s = serializer.serialize(p);
		expectedValue = "{_type:'Person',name:'some name',age:123,addresses:[{street:'street A',city:'city A',state:'state A',zip:12345,isCurrent:true},{street:'street B',city:'city B',state:'state B',zip:12345,isCurrent:false}]}";
		assertEquals(expectedValue, s);

		// Try adding an address
		model = ObjectRest.create(p);
		model.post("addresses", new Address("street C", "city C", "state C", 12345, true));
		s = ((Address)model.get("addresses/2")).toString();
		expectedValue = "Address(street=street C,city=city C,state=state C,zip=12345,isCurrent=true)";
		assertEquals(expectedValue, s);

		// Try replacing addresses
		model.put("addresses/0", new Address("street D", "city D", "state D", 12345, false));
		model.put("addresses/1", new Address("street E", "city E", "state E", 12345, false));
		model.put("addresses/2", new Address("street F", "city F", "state F", 12345, false));
		serializer = JsonSerializer.create().json5().build();
		s = serializer.serialize(p);
		expectedValue = "{name:'some name',age:123,addresses:[{street:'street D',city:'city D',state:'state D',zip:12345,isCurrent:false},{street:'street E',city:'city E',state:'state E',zip:12345,isCurrent:false},{street:'street F',city:'city F',state:'state F',zip:12345,isCurrent:false}]}";
		assertEquals(expectedValue, s);

		// Try removing an address
		model.delete("addresses/1");
		s = serializer.serialize(p);
		expectedValue = "{name:'some name',age:123,addresses:[{street:'street D',city:'city D',state:'state D',zip:12345,isCurrent:false},{street:'street F',city:'city F',state:'state F',zip:12345,isCurrent:false}]}";
		assertEquals(expectedValue, s);

		model.delete("addresses/0");
		model.delete("addresses/0");
		s = serializer.serialize(p);
		expectedValue = "{name:'some name',age:123,addresses:[]}";
		assertEquals(expectedValue, s);

		// Try adding an out-of-bounds address (should pad it with nulls)
		model.put("addresses/2", new Address("street A", "city A", "state A", 12345, true));
		s = serializer.serialize(p);
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
		model = ObjectRest.create(new JsonMap());
		var o = model.get("xxx");
		assertEquals("null", (""+o));

		// Make sure blanks and "/" returns the root object.
		s = model.get("").toString();
		assertEquals("{}", s);
		s = model.get("/").toString();
		assertEquals("{}", s);

		// Make sure doing a PUT against "" or "/" replaces the root object.
		var m2 = JsonMap.ofJson("{x:1}");
		model.put("", m2);
		s = model.get("").toString();
		assertEquals("{x:1}", s);
		m2 = JsonMap.ofJson("{x:2}");
		model.put("/", m2);
		s = model.get("").toString();
		assertEquals("{x:2}", s);

		// Make sure doing a POST against "" or "/" adds to the root object.
		model = ObjectRest.create(new JsonList());
		model.post("", Integer.valueOf(1));
		model.post("/", Integer.valueOf(2));
		s = model.get("").toString();
		assertEquals("[1,2]", s);
	}

	//====================================================================================================
	// testAddressBook
	//====================================================================================================
	@Test void b02_addressBook() {
		var model = ObjectRest.create(new AddressBook());


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

	@Bean(p="street,city,state,zip,isCurrent")
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
		@Override /* Object */
		public String toString() {
			return "Address(street="+street+",city="+city+",state="+state+",zip="+zip+",isCurrent="+isCurrent+")";
		}
	}

	@Bean(typeName="Person",p="name,age,addresses")
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

		@Override /* Object */
		public String toString() {
			return "Person(name="+name+",age="+age+")";
		}
	}

	//====================================================================================================
	// PojoRest(Object,ReaderParser)
	//====================================================================================================
	@Test void c01_constructors() {
		var model = ObjectRest.create(new AddressBook(), JsonParser.DEFAULT);

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
		var model = ObjectRest.create(new AddressBook()).setRootLocked();
		assertThrowsWithMessage(ObjectRestException.class, "Cannot overwrite root object", ()->model.put("", new AddressBook()));
		assertThrowsWithMessage(ObjectRestException.class, "Cannot overwrite root object", ()->model.put(null, new AddressBook()));
		assertThrowsWithMessage(ObjectRestException.class, "Cannot overwrite root object", ()->model.put("/", new AddressBook()));
	}

	//====================================================================================================
	// getRootObject()
	//====================================================================================================
	@Test void e01_getRootObject() {
		var model = ObjectRest.create(new AddressBook());
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
		var model = ObjectRest.create(new A());
		var l = JsonList.ofJson("[{a:'b'}]");
		var m = JsonMap.ofJson("{a:'b'}");

		assertMapped(model, ObjectRest::get,
			"f1,f2,f3,f4,f2a,f3a,f4a,f5,f6,f7,f8",
			"<null>,0,0,false,<null>,<null>,<null>,<null>,<null>,<null>,<null>");

		assertMapped(model, (r,p) -> r.getWithDefault(p, "foo"),
			"f1,f2,f3,f4,f2a,f3a,f4a,f5,f6,f7,f8",
			"foo,0,0,false,foo,foo,foo,foo,foo,foo,foo");

		assertMapped(model, ObjectRest::getString,
			"f1,f2,f3,f4,f2a,f3a,f4a,f5,f6,f7,f8",
			"<null>,0,0,false,<null>,<null>,<null>,<null>,<null>,<null>,<null>");

		assertMapped(model, (r,p) -> r.getString(p, "foo"),
			"f1,f2,f3,f4,f2a,f3a,f4a,f5,f6,f7,f8",
			"foo,0,0,false,foo,foo,foo,foo,foo,foo,foo");

		assertMapped(model, ObjectRest::getInt,
			"f1,f2,f3,f4,f2a,f3a,f4a,f5,f6,f7,f8",
			"<null>,0,0,0,<null>,<null>,<null>,<null>,<null>,<null>,<null>");

		assertMapped(model, (r,p) -> r.getInt(p, 1),
			"f1,f2,f3,f4,f2a,f3a,f4a,f5,f6,f7,f8",
			"1,0,0,0,1,1,1,1,1,1,1");

		assertMapped(model, ObjectRest::getLong,
			"f1,f2,f3,f4,f2a,f3a,f4a,f5,f6,f7,f8",
			"<null>,0,0,0,<null>,<null>,<null>,<null>,<null>,<null>,<null>");

		assertMapped(model, (r,p) -> r.getLong(p, 1L),
			"f1,f2,f3,f4,f2a,f3a,f4a,f5,f6,f7,f8",
			"1,0,0,0,1,1,1,1,1,1,1");

		assertMapped(model, ObjectRest::getBoolean,
			"f1,f2,f3,f4,f2a,f3a,f4a,f5,f6,f7,f8",
			"<null>,false,false,false,<null>,<null>,<null>,<null>,<null>,<null>,<null>");

		assertMapped(model, (r,p) -> r.getBoolean(p, true),
			"f1,f2,f3,f4,f2a,f3a,f4a,f5,f6,f7,f8",
			"true,false,false,false,true,true,true,true,true,true,true");

		assertMapped(model, ObjectRest::getMap,
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

		assertMapped(model, (r,p) -> r.getMap(p, m),
			"f1,f2,f2a,f3,f3a,f4,f4a,f5,f6,f7,f8",
			"{a=b},InvalidDataConversionException,{a=b},InvalidDataConversionException,{a=b},InvalidDataConversionException,{a=b},{a=b},{a=b},{a=b},{a=b}");

		assertMapped(model, ObjectRest::getJsonMap,
			"f1,f2,f3,f4,f2a,f3a,f4a,f5,f6,f7,f8",
			"<null>,InvalidDataConversionException,InvalidDataConversionException,InvalidDataConversionException,<null>,<null>,<null>,<null>,<null>,<null>,<null>");

		assertMapped(model, (r,p) -> r.getJsonMap(p, m),
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

		assertEquals("[{a:'b'}]", model.getJsonList("f1", l).toString());
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonList("f2", l));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonList("f3", l));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonList("f4", l));
		assertEquals("[{a:'b'}]", model.getJsonList("f2a", l).toString());
		assertEquals("[{a:'b'}]", model.getJsonList("f3a", l).toString());
		assertEquals("[{a:'b'}]", model.getJsonList("f4a", l).toString());
		assertEquals("[{a:'b'}]", model.getJsonList("f5", l).toString());
		assertEquals("[{a:'b'}]", model.getJsonList("f6", l).toString());
		assertEquals("[{a:'b'}]", model.getJsonList("f7", l).toString());
		assertEquals("[{a:'b'}]", model.getJsonList("f8", l).toString());

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
		assertEquals("{f5a:'a'}", model.getJsonMap("f5").toString());
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonMap("f6"));
		assertEquals("{f5a:'a'}", model.getJsonMap("f7").toString());
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonMap("f8"));

		assertThrows(InvalidDataConversionException.class, ()->model.getJsonMap("f1", m));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonMap("f2", m));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonMap("f3", m));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonMap("f4", m));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonMap("f2a", m));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonMap("f3a", m));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonMap("f4a", m));
		assertEquals("{f5a:'a'}", model.getJsonMap("f5", m).toString());
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonMap("f6", m));
		assertEquals("{f5a:'a'}", model.getJsonMap("f7", m).toString());
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonMap("f8", m));

		assertThrows(InvalidDataConversionException.class, ()->model.getList("f1"));
		assertThrows(InvalidDataConversionException.class, ()->model.getList("f2"));
		assertThrows(InvalidDataConversionException.class, ()->model.getList("f3"));
		assertThrows(InvalidDataConversionException.class, ()->model.getList("f4"));
		assertThrows(InvalidDataConversionException.class, ()->model.getList("f2a"));
		assertThrows(InvalidDataConversionException.class, ()->model.getList("f3a"));
		assertThrows(InvalidDataConversionException.class, ()->model.getList("f4a"));
		assertEquals("[{f5a:'a'}]", model.getList("f5").toString());
		assertEquals("[{f6a:'a'}]", model.getList("f6").toString());
		assertEquals("[{f5a:'a'}]", model.getList("f7").toString());
		assertEquals("[{f6a:'a'}]", model.getList("f8").toString());

		assertThrows(InvalidDataConversionException.class, ()->model.getList("f1", l));
		assertThrows(InvalidDataConversionException.class, ()->model.getList("f2", l));
		assertThrows(InvalidDataConversionException.class, ()->model.getList("f3", l));
		assertThrows(InvalidDataConversionException.class, ()->model.getList("f4", l));
		assertThrows(InvalidDataConversionException.class, ()->model.getList("f2a", l));
		assertThrows(InvalidDataConversionException.class, ()->model.getList("f3a", l));
		assertThrows(InvalidDataConversionException.class, ()->model.getList("f4a", l));
		assertEquals("[{f5a:'a'}]", model.getList("f5", l).toString());
		assertEquals("[{f6a:'a'}]", model.getList("f6", l).toString());
		assertEquals("[{f5a:'a'}]", model.getList("f7", l).toString());
		assertEquals("[{f6a:'a'}]", model.getList("f8", l).toString());

		assertThrows(InvalidDataConversionException.class, ()->model.getJsonList("f1"));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonList("f2"));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonList("f3"));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonList("f4"));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonList("f2a"));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonList("f3a"));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonList("f4a"));
		assertEquals("[{f5a:'a'}]", model.getJsonList("f5").toString());
		assertEquals("[{f6a:'a'}]", model.getJsonList("f6").toString());
		assertEquals("[{f5a:'a'}]", model.getJsonList("f7").toString());
		assertEquals("[{f6a:'a'}]", model.getJsonList("f8").toString());

		assertThrows(InvalidDataConversionException.class, ()->model.getJsonList("f1", l));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonList("f2", l));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonList("f3", l));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonList("f4", l));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonList("f2a", l));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonList("f3a", l));
		assertThrows(InvalidDataConversionException.class, ()->model.getJsonList("f4a", l));
		assertEquals("[{f5a:'a'}]", model.getJsonList("f5", l).toString());
		assertEquals("[{f6a:'a'}]", model.getJsonList("f6", l).toString());
		assertEquals("[{f5a:'a'}]", model.getJsonList("f7", l).toString());
		assertEquals("[{f6a:'a'}]", model.getJsonList("f8", l).toString());
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
		public JsonMap f7;
		public JsonList f8;

		public A init() {
			f1 = "1";
			f2 = 2;
			f3 = 3L;
			f4 = true;
			f2a = 2;
			f3a = 3L;
			f4a = true;
			try {
				f5 = JsonMap.ofJson("{f5a:'a'}");
				f6 = JsonList.ofJson("[{f6a:'a'}]");
				f7 = JsonMap.ofJson("{f5a:'a'}");
				f8 = JsonList.ofJson("[{f6a:'a'}]");
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
			return this;
		}
	}

	//====================================================================================================
	// invokeMethod(String url, String method, String args)
	//====================================================================================================
	@Test void f02_invokeMethod() throws Exception {

		var model = ObjectRest.create(new AddressBook().init());
		assertEquals("Person(name=Bill Clinton,age=65)", model.invokeMethod("0", "toString", ""));

		model = ObjectRest.create(new AddressBook().init(), JsonParser.DEFAULT);
		assertEquals("Person(name=Bill Clinton,age=65)", model.invokeMethod("0", "toString", ""));
		assertEquals("NY", model.invokeMethod("0/addresses/0/state", "toString", ""));
		assertNull(model.invokeMethod("1", "toString", ""));
	}

	//====================================================================================================
	// getPublicMethods(String url)
	//====================================================================================================
	@Test void f03_getPublicMethods() {
		var model = ObjectRest.create(new AddressBook().init());
		assertTrue(Json5Serializer.DEFAULT.toString(model.getPublicMethods("0")).contains("'toString'"));
		assertTrue(Json5Serializer.DEFAULT.toString(model.getPublicMethods("0/addresses/0/state")).contains("'toString'"));
		assertNull(model.getPublicMethods("1"));
	}

	//====================================================================================================
	// getClassMeta(String url)
	//====================================================================================================
	@Test void f04_getClassMeta() {
		var model = ObjectRest.create(new AddressBook().init());
		assertEquals("Person", model.getClassMeta("0").getInnerClass().getSimpleName());
		assertEquals("String", model.getClassMeta("0/addresses/0/state").getInnerClass().getSimpleName());
		assertNull(model.getClassMeta("1"));
		assertNull(model.getClassMeta("0/addresses/1/state"));
	}
}