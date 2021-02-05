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
package org.apache.juneau.utils;

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.junit.*;

@SuppressWarnings({"unchecked","rawtypes","serial"})
@FixMethodOrder(NAME_ASCENDING)
public class PojoRestTest {

	//====================================================================================================
	// testBasic
	//====================================================================================================
	@Test
	public void testBasic() {

		// TODO: Need to write some exhaustive tests here. Will open work item
		// to do that later.
		PojoRest model = new PojoRest(new OMap()); // An empty model.

		// Do a PUT
		model.put("A", new OMap());
		model.put("A/B", new OMap());
		model.put("A/B/C", "A new string");
		assertEquals("{A:{B:{C:'A new string'}}}", model.toString());

		// Do a POST to a list.
		model.put("A/B/C", new LinkedList());
		model.post("A/B/C", "String #1");
		model.post("A/B/C", "String #2");
		assertEquals("{A:{B:{C:['String #1\','String #2']}}}", model.toString());

		// Do some GETs
		String s = (String) model.get("A/B/C/0");
		assertEquals("String #1", s);

		Map m = (Map) model.get("A/B");
		assertEquals("{C:['String #1','String #2']}", m.toString());
	}

	//====================================================================================================
	// testBeans
	//====================================================================================================
	@Test
	public void testBeans() throws Exception {
		PojoRest model;

		// Java beans.
		model = new PojoRest(new OMap());
		Person p = new Person("some name", 123,
			new Address("street A", "city A", "state A", 12345, true),
			new Address("street B", "city B", "state B", 12345, false)
		);
		model.put("/person1", p);

		// Make sure it got stored correctly.
		JsonSerializer serializer = JsonSerializer.create().ssq().addRootType().build();
		assertEquals("{person1:{name:'some name',age:123,addresses:[{street:'street A',city:'city A',state:'state A',zip:12345,isCurrent:true},{street:'street B',city:'city B',state:'state B',zip:12345,isCurrent:false}]}}", serializer.serialize(model.getRootObject()));

		// Get the original Person object back.
		p = (Person)model.get("/person1");
		assertEquals("city B", p.addresses[1].city);

		// Look for deep information inside beans.
		Address a3 = (Address)model.get("/person1/addresses/1");
		assertEquals("city B", a3.city);

		serializer = SimpleJsonSerializer.DEFAULT.builder().addBeanTypes().addRootType().build();
		p = new Person("some name", 123,
			new Address("street A", "city A", "state A", 12345, true),
			new Address("street B", "city B", "state B", 12345, false)
		);

		// Serialize it to JSON.
		String s = serializer.serialize(p);
		String expectedValue = "{_type:'Person',name:'some name',age:123,addresses:[{street:'street A',city:'city A',state:'state A',zip:12345,isCurrent:true},{street:'street B',city:'city B',state:'state B',zip:12345,isCurrent:false}]}";
		assertEquals(expectedValue, s);

		// Parse it back to Java objects.
		p = (Person)JsonParser.create().dictionary(Person.class).build().parse(s, Object.class);
		expectedValue = "city B";
		s = p.addresses[1].city;
		assertEquals(expectedValue, s);

		// Parse it back into JSON again.
		s = serializer.serialize(p);
		expectedValue = "{_type:'Person',name:'some name',age:123,addresses:[{street:'street A',city:'city A',state:'state A',zip:12345,isCurrent:true},{street:'street B',city:'city B',state:'state B',zip:12345,isCurrent:false}]}";
		assertEquals(expectedValue, s);

		// Try adding an address
		model = new PojoRest(p);
		model.post("addresses", new Address("street C", "city C", "state C", 12345, true));
		s = ((Address)model.get("addresses/2")).toString();
		expectedValue = "Address(street=street C,city=city C,state=state C,zip=12345,isCurrent=true)";
		assertEquals(expectedValue, s);

		// Try replacing addresses
		model.put("addresses/0", new Address("street D", "city D", "state D", 12345, false));
		model.put("addresses/1", new Address("street E", "city E", "state E", 12345, false));
		model.put("addresses/2", new Address("street F", "city F", "state F", 12345, false));
		serializer = JsonSerializer.create().ssq().build();
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
		Map m = new HashMap();
		m.put("street","street D");
		m.put("city","city D");
		m.put("state","state D");
		m.put("zip",new Integer(12345));

		// Try the same for an address in an array.
		model.put("addresses/1", m);
		s = ((Address)model.get("addresses/1")).toString();
		expectedValue = "Address(street=street D,city=city D,state=state D,zip=12345,isCurrent=false)";
		assertEquals(expectedValue, s);

		// Try setting some fields.
		model.put("addresses/1/zip", new Integer(99999));
		s = model.get("addresses/1/zip").toString();
		expectedValue = "99999";
		assertEquals(expectedValue, s);

		// Make sure we can get non-existent branches without throwing any exceptions.
		// get() method should just return null.
		model = new PojoRest(new OMap());
		Object o = model.get("xxx");
		assertEquals("null", (""+o));

		// Make sure blanks and "/" returns the root object.
		s = model.get("").toString();
		assertEquals("{}", s);
		s = model.get("/").toString();
		assertEquals("{}", s);

		// Make sure doing a PUT against "" or "/" replaces the root object.
		OMap m2 = OMap.ofJson("{x:1}");
		model.put("", m2);
		s = model.get("").toString();
		assertEquals("{x:1}", s);
		m2 = OMap.ofJson("{x:2}");
		model.put("/", m2);
		s = model.get("").toString();
		assertEquals("{x:2}", s);

		// Make sure doing a POST against "" or "/" adds to the root object.
		model = new PojoRest(new OList());
		model.post("", new Integer(1));
		model.post("/", new Integer(2));
		s = model.get("").toString();
		assertEquals("[1,2]", s);
	}

	//====================================================================================================
	// testAddressBook
	//====================================================================================================
	@Test
	public void testAddressBook() {
		PojoRest model;

		model = new PojoRest(new AddressBook());

		// Try adding a person to the address book.
		Person billClinton = new Person("Bill Clinton", 65,
			new Address("55W. 125th Street", "New York", "NY", 10027, true)
		);

		model.post("/", billClinton);

		// Make sure we get the original person back.
		billClinton = (Person)model.get("/0");
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
	@Test
	public void testConstructors() throws Exception {
		PojoRest model = new PojoRest(new AddressBook(), JsonParser.DEFAULT);

		// Try adding a person to the address book.
		Person billClinton = new Person("Bill Clinton", 65,
			new Address("55W. 125th Street", "New York", "NY", 10027, true)
		);

		model.post("/", billClinton);

		// Make sure we get the original person back.
		billClinton = (Person)model.get("/0");
	}

	//====================================================================================================
	// setRootLocked()
	//====================================================================================================
	@Test
	public void testRootLocked() throws Exception {
		PojoRest model = new PojoRest(new AddressBook()).setRootLocked();
		assertThrown(()->model.put("", new AddressBook())).is("Cannot overwrite root object");
		assertThrown(()->model.put(null, new AddressBook())).is("Cannot overwrite root object");
		assertThrown(()->model.put("/", new AddressBook())).is("Cannot overwrite root object");
	}

	//====================================================================================================
	// getRootObject()
	//====================================================================================================
	@Test
	public void testGetRootObject() throws Exception {
		PojoRest model = new PojoRest(new AddressBook());
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
	// getOMap(String url)
	// getOMap(String url, OMap defVal)
	// getOList(String url)
	// getOList(String url, OList defVal)
	//====================================================================================================
	@Test
	public void testGetMethods() throws Exception {
		PojoRest model = new PojoRest(new A());
		OList l = OList.ofJson("[{a:'b'}]");
		OMap m = OMap.ofJson("{a:'b'}");

		assertNull(model.get("f1"));
		assertEquals(0, model.get("f2"));
		assertEquals(0l, model.get("f3"));
		assertFalse((Boolean)model.get("f4"));
		assertNull(model.get("f2a"));
		assertNull(model.get("f3a"));
		assertNull(model.get("f4a"));
		assertNull(model.get("f5"));
		assertNull(model.get("f6"));
		assertNull(model.get("f7"));
		assertNull(model.get("f8"));

		assertEquals("foo", model.getWithDefault("f1", "foo"));
		assertEquals(0, model.getWithDefault("f2", "foo"));
		assertEquals(0l, model.getWithDefault("f3", "foo"));
		assertEquals(false, model.getWithDefault("f4", "foo"));
		assertEquals("foo", model.getWithDefault("f2a", "foo"));
		assertEquals("foo", model.getWithDefault("f3a", "foo"));
		assertEquals("foo", model.getWithDefault("f4a", "foo"));
		assertEquals("foo", model.getWithDefault("f5", "foo"));
		assertEquals("foo", model.getWithDefault("f6", "foo"));
		assertEquals("foo", model.getWithDefault("f7", "foo"));
		assertEquals("foo", model.getWithDefault("f8", "foo"));

		assertNull(model.getString("f1"));
		assertEquals("0", model.getString("f2"));
		assertEquals("0", model.getString("f3"));
		assertEquals("false", model.getString("f4"));
		assertNull(model.getString("f2a"));
		assertNull(model.getString("f3a"));
		assertNull(model.getString("f4a"));
		assertNull(model.getString("f5"));
		assertNull(model.getString("f6"));
		assertNull(model.getString("f7"));
		assertNull(model.getString("f8"));

		assertEquals("foo", model.getString("f1", "foo"));
		assertEquals("0", model.getString("f2", "foo"));
		assertEquals("0", model.getString("f3", "foo"));
		assertEquals("false", model.getString("f4", "foo"));
		assertEquals("foo", model.getString("f2a", "foo"));
		assertEquals("foo", model.getString("f3a", "foo"));
		assertEquals("foo", model.getString("f4a", "foo"));
		assertEquals("foo", model.getString("f5", "foo"));
		assertEquals("foo", model.getString("f6", "foo"));
		assertEquals("foo", model.getString("f7", "foo"));
		assertEquals("foo", model.getString("f8", "foo"));

		assertNull(model.getInt("f1"));
		assertEquals(0, (int)model.getInt("f2"));
		assertEquals(0, (int)model.getInt("f3"));
		assertEquals(0, (int)model.getInt("f4"));
		assertNull(model.getInt("f2a"));
		assertNull(model.getInt("f3a"));
		assertNull(model.getInt("f4a"));
		assertNull(model.getInt("f5"));
		assertNull(model.getInt("f6"));
		assertNull(model.getInt("f7"));
		assertNull(model.getInt("f8"));

		assertEquals(1, (int)model.getInt("f1", 1));
		assertEquals(0, (int)model.getInt("f2", 1));
		assertEquals(0, (int)model.getInt("f3", 1));
		assertEquals(0, (int)model.getInt("f4", 1));
		assertEquals(1, (int)model.getInt("f2a", 1));
		assertEquals(1, (int)model.getInt("f3a", 1));
		assertEquals(1, (int)model.getInt("f4a", 1));
		assertEquals(1, (int)model.getInt("f5", 1));
		assertEquals(1, (int)model.getInt("f6", 1));
		assertEquals(1, (int)model.getInt("f7", 1));
		assertEquals(1, (int)model.getInt("f8", 1));

		assertNull(model.getLong("f1"));
		assertEquals(0, (long)model.getLong("f2"));
		assertEquals(0, (long)model.getLong("f3"));
		assertEquals(0, (long)model.getLong("f4"));
		assertNull(model.getLong("f2a"));
		assertNull(model.getLong("f3a"));
		assertNull(model.getLong("f4a"));
		assertNull(model.getLong("f5"));
		assertNull(model.getLong("f6"));
		assertNull(model.getLong("f7"));
		assertNull(model.getLong("f8"));

		assertEquals(1, (long)model.getLong("f1", 1l));
		assertEquals(0, (long)model.getLong("f2", 1l));
		assertEquals(0, (long)model.getLong("f3", 1l));
		assertEquals(0, (long)model.getLong("f4", 1l));
		assertEquals(1, (long)model.getLong("f2a", 1l));
		assertEquals(1, (long)model.getLong("f3a", 1l));
		assertEquals(1, (long)model.getLong("f4a", 1l));
		assertEquals(1, (long)model.getLong("f5", 1l));
		assertEquals(1, (long)model.getLong("f6", 1l));
		assertEquals(1, (long)model.getLong("f7", 1l));
		assertEquals(1, (long)model.getLong("f8", 1l));

		assertNull(model.getBoolean("f1"));
		assertEquals(false, model.getBoolean("f2"));
		assertEquals(false, model.getBoolean("f3"));
		assertEquals(false, model.getBoolean("f4"));
		assertNull(model.getBoolean("f2a"));
		assertNull(model.getBoolean("f3a"));
		assertNull(model.getBoolean("f4a"));
		assertNull(model.getBoolean("f5"));
		assertNull(model.getBoolean("f6"));
		assertNull(model.getBoolean("f7"));
		assertNull(model.getBoolean("f8"));

		assertEquals(true, model.getBoolean("f1", true));
		assertEquals(false, model.getBoolean("f2", true));
		assertEquals(false, model.getBoolean("f3", true));
		assertEquals(false, model.getBoolean("f4", true));
		assertEquals(true, model.getBoolean("f2a", true));
		assertEquals(true, model.getBoolean("f3a", true));
		assertEquals(true, model.getBoolean("f4a", true));
		assertEquals(true, model.getBoolean("f5", true));
		assertEquals(true, model.getBoolean("f6", true));
		assertEquals(true, model.getBoolean("f7", true));
		assertEquals(true, model.getBoolean("f8", true));

		assertNull(model.getMap("f1"));
		assertThrown(()->model.getMap("f2")).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getMap("f3")).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getMap("f4")).isType(InvalidDataConversionException.class);
		assertNull(model.getMap("f2a"));
		assertNull(model.getMap("f3a"));
		assertNull(model.getMap("f4a"));
		assertNull(model.getMap("f5"));
		assertNull(model.getMap("f6"));
		assertNull(model.getMap("f7"));
		assertNull(model.getMap("f8"));

		assertEquals("{a:'b'}", model.getMap("f1", m).toString());
		assertThrown(()->model.getMap("f2", m)).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getMap("f3", m)).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getMap("f4", m)).isType(InvalidDataConversionException.class);
		assertEquals("{a:'b'}", model.getMap("f2a", m).toString());
		assertEquals("{a:'b'}", model.getMap("f3a", m).toString());
		assertEquals("{a:'b'}", model.getMap("f4a", m).toString());
		assertEquals("{a:'b'}", model.getMap("f5", m).toString());
		assertEquals("{a:'b'}", model.getMap("f6", m).toString());
		assertEquals("{a:'b'}", model.getMap("f7", m).toString());
		assertEquals("{a:'b'}", model.getMap("f8", m).toString());

		assertNull(model.getMap("f1"));
		assertThrown(()->model.getOMap("f2")).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getOMap("f3")).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getOMap("f4")).isType(InvalidDataConversionException.class);
		assertNull(model.getOMap("f2a"));
		assertNull(model.getOMap("f3a"));
		assertNull(model.getOMap("f4a"));
		assertNull(model.getOMap("f5"));
		assertNull(model.getOMap("f6"));
		assertNull(model.getOMap("f7"));
		assertNull(model.getOMap("f8"));

		assertEquals("{a:'b'}", model.getOMap("f1", m).toString());
		assertThrown(()->model.getOMap("f2", m)).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getOMap("f3", m)).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getOMap("f4", m)).isType(InvalidDataConversionException.class);
		assertEquals("{a:'b'}", model.getOMap("f2a", m).toString());
		assertEquals("{a:'b'}", model.getOMap("f3a", m).toString());
		assertEquals("{a:'b'}", model.getOMap("f4a", m).toString());
		assertEquals("{a:'b'}", model.getOMap("f5", m).toString());
		assertEquals("{a:'b'}", model.getOMap("f6", m).toString());
		assertEquals("{a:'b'}", model.getOMap("f7", m).toString());
		assertEquals("{a:'b'}", model.getOMap("f8", m).toString());

		assertNull(model.getList("f1"));
		assertThrown(()->model.getList("f2")).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getList("f3")).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getList("f4")).isType(InvalidDataConversionException.class);
		assertNull(model.getList("f2a"));
		assertNull(model.getList("f3a"));
		assertNull(model.getList("f4a"));
		assertNull(model.getList("f5"));
		assertNull(model.getList("f6"));
		assertNull(model.getList("f7"));
		assertNull(model.getList("f8"));

		assertEquals("[{a:'b'}]", model.getList("f1", l).toString());
		assertThrown(()->model.getList("f2", l)).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getList("f3", l)).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getList("f4", l)).isType(InvalidDataConversionException.class);
		assertEquals("[{a:'b'}]", model.getList("f2a", l).toString());
		assertEquals("[{a:'b'}]", model.getList("f3a", l).toString());
		assertEquals("[{a:'b'}]", model.getList("f4a", l).toString());
		assertEquals("[{a:'b'}]", model.getList("f5", l).toString());
		assertEquals("[{a:'b'}]", model.getList("f6", l).toString());
		assertEquals("[{a:'b'}]", model.getList("f7", l).toString());
		assertEquals("[{a:'b'}]", model.getList("f8", l).toString());

		assertNull(model.getOList("f1"));
		assertThrown(()->model.getOList("f2")).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getOList("f3")).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getOList("f4")).isType(InvalidDataConversionException.class);
		assertNull(model.getOList("f2a"));
		assertNull(model.getOList("f3a"));
		assertNull(model.getOList("f4a"));
		assertNull(model.getOList("f5"));
		assertNull(model.getOList("f6"));
		assertNull(model.getOList("f7"));
		assertNull(model.getOList("f8"));

		assertEquals("[{a:'b'}]", model.getOList("f1", l).toString());
		assertThrown(()->model.getOList("f2", l)).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getOList("f3", l)).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getOList("f4", l)).isType(InvalidDataConversionException.class);
		assertEquals("[{a:'b'}]", model.getOList("f2a", l).toString());
		assertEquals("[{a:'b'}]", model.getOList("f3a", l).toString());
		assertEquals("[{a:'b'}]", model.getOList("f4a", l).toString());
		assertEquals("[{a:'b'}]", model.getOList("f5", l).toString());
		assertEquals("[{a:'b'}]", model.getOList("f6", l).toString());
		assertEquals("[{a:'b'}]", model.getOList("f7", l).toString());
		assertEquals("[{a:'b'}]", model.getOList("f8", l).toString());

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
		assertThrown(()->model.getInt("f5")).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getInt("f6")).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getInt("f7")).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getInt("f8")).isType(InvalidDataConversionException.class);

		assertEquals(1, (int)model.getInt("f1", 9));
		assertEquals(2, (int)model.getInt("f2", 9));
		assertEquals(3, (int)model.getInt("f3", 9));
		assertEquals(1, (int)model.getInt("f4", 9));
		assertEquals(2, (int)model.getInt("f2a", 9));
		assertEquals(3, (int)model.getInt("f3a", 9));
		assertEquals(1, (int)model.getInt("f4a", 9));
		assertThrown(()->model.getInt("f5", 9)).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getInt("f6", 9)).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getInt("f7", 9)).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getInt("f8", 9)).isType(InvalidDataConversionException.class);

		assertEquals(1, (long)model.getLong("f1"));
		assertEquals(2, (long)model.getLong("f2"));
		assertEquals(3, (long)model.getLong("f3"));
		assertEquals(1, (long)model.getLong("f4"));
		assertEquals(2, (long)model.getLong("f2a"));
		assertEquals(3, (long)model.getLong("f3a"));
		assertEquals(1, (long)model.getLong("f4a"));
		assertThrown(()->model.getLong("f5")).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getLong("f6")).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getLong("f7")).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getInt("f8")).isType(InvalidDataConversionException.class);

		assertEquals(1, (long)model.getLong("f1", 9l));
		assertEquals(2, (long)model.getLong("f2", 9l));
		assertEquals(3, (long)model.getLong("f3", 9l));
		assertEquals(1, (long)model.getLong("f4", 9l));
		assertEquals(2, (long)model.getLong("f2a", 9l));
		assertEquals(3, (long)model.getLong("f3a", 9l));
		assertEquals(1, (long)model.getLong("f4a", 9l));
		assertThrown(()->model.getLong("f5", 9l)).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getLong("f6", 9l)).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getLong("f7", 9l)).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getLong("f8", 9l)).isType(InvalidDataConversionException.class);

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

		assertThrown(()->model.getMap("f1")).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getMap("f2")).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getMap("f3")).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getMap("f4")).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getMap("f2a")).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getMap("f3a")).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getMap("f4a")).isType(InvalidDataConversionException.class);
		assertEquals("{f5a:'a'}", model.getMap("f5").toString());
		assertThrown(()->model.getMap("f6")).isType(InvalidDataConversionException.class);
		assertEquals("{f5a:'a'}", model.getMap("f7").toString());
		assertThrown(()->model.getMap("f8")).isType(InvalidDataConversionException.class);

		assertThrown(()->model.getMap("f1", m)).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getMap("f2", m)).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getMap("f3", m)).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getMap("f4", m)).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getMap("f2a", m)).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getMap("f3a", m)).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getMap("f4a", m)).isType(InvalidDataConversionException.class);
		assertEquals("{f5a:'a'}", model.getMap("f5", m).toString());
		assertThrown(()->model.getMap("f6", m)).isType(InvalidDataConversionException.class);
		assertEquals("{f5a:'a'}", model.getMap("f7", m).toString());
		assertThrown(()->model.getMap("f8", m)).isType(InvalidDataConversionException.class);

		assertThrown(()->model.getOMap("f1")).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getOMap("f2")).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getOMap("f3")).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getOMap("f4")).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getOMap("f2a")).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getOMap("f3a")).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getOMap("f4a")).isType(InvalidDataConversionException.class);
		assertEquals("{f5a:'a'}", model.getOMap("f5").toString());
		assertThrown(()->model.getOMap("f6")).isType(InvalidDataConversionException.class);
		assertEquals("{f5a:'a'}", model.getOMap("f7").toString());
		assertThrown(()->model.getOMap("f8")).isType(InvalidDataConversionException.class);

		assertThrown(()->model.getOMap("f1", m)).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getOMap("f2", m)).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getOMap("f3", m)).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getOMap("f4", m)).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getOMap("f2a", m)).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getOMap("f3a", m)).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getOMap("f4a", m)).isType(InvalidDataConversionException.class);
		assertEquals("{f5a:'a'}", model.getOMap("f5", m).toString());
		assertThrown(()->model.getOMap("f6", m)).isType(InvalidDataConversionException.class);
		assertEquals("{f5a:'a'}", model.getOMap("f7", m).toString());
		assertThrown(()->model.getOMap("f8", m)).isType(InvalidDataConversionException.class);

		assertThrown(()->model.getList("f1")).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getList("f2")).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getList("f3")).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getList("f4")).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getList("f2a")).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getList("f3a")).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getList("f4a")).isType(InvalidDataConversionException.class);
		assertEquals("[{f5a:'a'}]", model.getList("f5").toString());
		assertEquals("[{f6a:'a'}]", model.getList("f6").toString());
		assertEquals("[{f5a:'a'}]", model.getList("f7").toString());
		assertEquals("[{f6a:'a'}]", model.getList("f8").toString());

		assertThrown(()->model.getList("f1", l)).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getList("f2", l)).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getList("f3", l)).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getList("f4", l)).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getList("f2a", l)).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getList("f3a", l)).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getList("f4a", l)).isType(InvalidDataConversionException.class);
		assertEquals("[{f5a:'a'}]", model.getList("f5", l).toString());
		assertEquals("[{f6a:'a'}]", model.getList("f6", l).toString());
		assertEquals("[{f5a:'a'}]", model.getList("f7", l).toString());
		assertEquals("[{f6a:'a'}]", model.getList("f8", l).toString());

		assertThrown(()->model.getOList("f1")).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getOList("f2")).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getOList("f3")).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getOList("f4")).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getOList("f2a")).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getOList("f3a")).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getOList("f4a")).isType(InvalidDataConversionException.class);
		assertEquals("[{f5a:'a'}]", model.getOList("f5").toString());
		assertEquals("[{f6a:'a'}]", model.getOList("f6").toString());
		assertEquals("[{f5a:'a'}]", model.getOList("f7").toString());
		assertEquals("[{f6a:'a'}]", model.getOList("f8").toString());

		assertThrown(()->model.getOList("f1", l)).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getOList("f2", l)).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getOList("f3", l)).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getOList("f4", l)).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getOList("f2a", l)).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getOList("f3a", l)).isType(InvalidDataConversionException.class);
		assertThrown(()->model.getOList("f4a", l)).isType(InvalidDataConversionException.class);
		assertEquals("[{f5a:'a'}]", model.getOList("f5", l).toString());
		assertEquals("[{f6a:'a'}]", model.getOList("f6", l).toString());
		assertEquals("[{f5a:'a'}]", model.getOList("f7", l).toString());
		assertEquals("[{f6a:'a'}]", model.getOList("f8", l).toString());
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
		public OMap f7;
		public OList f8;

		public A init() {
			f1 = "1";
			f2 = 2;
			f3 = 3l;
			f4 = true;
			f2a = 2;
			f3a = 3l;
			f4a = true;
			try {
				f5 = OMap.ofJson("{f5a:'a'}");
				f6 = OList.ofJson("[{f6a:'a'}]");
				f7 = OMap.ofJson("{f5a:'a'}");
				f8 = OList.ofJson("[{f6a:'a'}]");
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
			return this;
		}
	}

	//====================================================================================================
	// invokeMethod(String url, String method, String args)
	//====================================================================================================
	@Test
	public void testInvokeMethod() throws Exception {

		PojoRest model = new PojoRest(new AddressBook().init());
		assertEquals("Person(name=Bill Clinton,age=65)", model.invokeMethod("0", "toString", ""));

		model = new PojoRest(new AddressBook().init(), JsonParser.DEFAULT);
		assertEquals("Person(name=Bill Clinton,age=65)", model.invokeMethod("0", "toString", ""));
		assertEquals("NY", model.invokeMethod("0/addresses/0/state", "toString", ""));
		assertNull(model.invokeMethod("1", "toString", ""));
	}

	//====================================================================================================
	// getPublicMethods(String url)
	//====================================================================================================
	@Test
	public void testGetPublicMethods() throws Exception {
		PojoRest model = new PojoRest(new AddressBook().init());
		assertTrue(SimpleJsonSerializer.DEFAULT.toString(model.getPublicMethods("0")).contains("'toString'"));
		assertTrue(SimpleJsonSerializer.DEFAULT.toString(model.getPublicMethods("0/addresses/0/state")).contains("'toString'"));
		assertNull(model.getPublicMethods("1"));
	}

	//====================================================================================================
	// getClassMeta(String url)
	//====================================================================================================
	@Test
	public void testGetClassMeta() throws Exception {
		PojoRest model = new PojoRest(new AddressBook().init());
		assertEquals("Person", model.getClassMeta("0").getInnerClass().getSimpleName());
		assertEquals("String", model.getClassMeta("0/addresses/0/state").getInnerClass().getSimpleName());
		assertNull(model.getClassMeta("1"));
		assertNull(model.getClassMeta("0/addresses/1/state"));
	}
}
