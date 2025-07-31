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
package org.apache.juneau;

import static org.apache.juneau.utest.utils.Utils.*;
import static java.util.Optional.*;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.common.utils.*;
import org.apache.juneau.marshaller.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.*;

/**
 * Base class for all JUnit 5 unit tests.
 */
@TestMethodOrder(MethodName.class)
public abstract class SimpleTestBase {

	/** Assert value when stringified matches the specified pattern. */
	protected void assertMatches(String pattern, Object value) {
		var m = StringUtils.getMatchPattern(pattern).matcher(s(value));
		if (! m.matches()) {
			var msg = "Pattern didn't match: \n\tExpected:\n"+pattern+"\n\tActual:\n"+value;
			System.err.println(msg);  // For easier debugging.
			fail(msg);
		}
	}

	protected void assertJson(Object value, String json) {
		assertEquals(json, Json5.DEFAULT.write(value));
	}

	/**
	 * Asserts an object matches the expected string after it's been stringified.
	 */
	protected void assertString(String expected, Object actual) {
		assertEquals(expected, s(actual));
	}

	protected void assertNotEmpty(Collection<?> c) {
		assertTrue(c != null && ! c.isEmpty());
	}

	protected void assertNotEmpty(Map<?,?> c) {
		assertTrue(c != null && ! c.isEmpty());
	}

	protected void assertEmpty(Collection<?> c) {
		assertTrue(c != null && c.isEmpty());
	}

	protected void assertSize(int expected, Collection<?> c) {
		assertEquals(expected, ofNullable(c).map(Collection::size).orElse(-1));
	}

	/**
	 * Asserts an exception is thrown and contains the specified message.
	 * Example:  assertThrown(()->doSomething(), "My exception message:  foo*");
	 */
	protected void assertThrown(Snippet snippet, String msg) {
		try {
			snippet.run();
			fail("Exception not thrown.");
		} catch (Throwable e) {
			if (! StringUtils.getMatchPattern(msg, Pattern.DOTALL).matcher(e.getMessage()).matches()) {
				System.err.println("Thrown message didn't match.  Message=" + e.getMessage());
				fail("Thrown message didn't match.  Message=" + e.getMessage());
			}
		}
	}

	protected void assertThrown(Snippet snippet, Class<? extends Throwable> thrownType) {
		try {
			snippet.run();
			fail("Exception not thrown.");
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
			assertTrue(thrownType.isInstance(e), ()->"Wrong type thrown: " + e.getClass().getName());
		}
	}

	/**
	 * Asserts an exception is not thrown
	 * Example:  assertThrown(()->doSomething());
	 */
	protected void assertNotThrown(Snippet snippet) {
		try {
			snippet.run();
		} catch (Throwable e) {
			fail("Exception thrown.");
		}
	}

	/**
	 * Asserts that the fields/properties on the specified bean are the specified values.
	 * Collections and calendars are serialized consistently for ease of testing.
	 * Example:
	 * 	assertBean(
	 * 		myBean,
	 * 		"prop1,prop2,prop3"
	 * 		"val1,val2,val3"
	 * 	);
	 */
	protected void assertBean(Object o, String fields, String value) {
		if (o == null) throw new NullPointerException("Bean was null");
		var sb = new StringBuilder();
		var f = StringUtils.split(fields);
		for (var i = 0; i < f.length; i++) {
			if (i > 0) sb.append(",");
			sb.append(s(getBeanProp(o, f[i])));
		}
		assertEquals(value, sb.toString());
	}

	/**
	 * Asserts that the values of the specified fields match the list of beans.
	 * @param l The list of beans to check.
	 * @param fields A comma-delimited list of bean property names.
	 * @param values The comma-delimited list of values for each bean.
	 */
	@SuppressWarnings("rawtypes")
	protected void assertBeans(Collection l, String fields, Object...values) {
		assertEquals(values.length, l.size(), ()->"Expected "+values.length+" rows but had actual " + l.size());
		var r = 0;
		var f = StringUtils.split(fields);
		for (var o : l) {
			var sb = new StringBuilder();
			var first = true;
			for (var ff : f) {
				if (! first) sb.append(",");
				first = false;
				String[] subfields = null;
				if (contains(ff, '{')) {
					subfields = ff.substring(ff.indexOf('{')+1, ff.indexOf("}")).split(":");
					ff = ff.substring(0, ff.indexOf('{'));
				}
				var bp = getBeanProp(o, ff);
				if (subfields != null) {
					if (isListOrArray(bp)) {
						sb.append("[");
						for (var i = 0; i < length(bp); i++) {
							if (i > 0) sb.append(",");
							sb.append(beanProps(get(bp, i), subfields));
						}
						sb.append("]");
					} else {
						sb.append(beanProps(bp, subfields));
					}
				} else {
					sb.append(s(getBeanProp(o, ff)));
				}
			}
			assertEquals(s(values[r]), sb.toString(), "Object at row " + (r+1) + " didn't match.");
			r++;
		}
	}

	/**
	 * Returns the specified bean properties on the specified bean as a comma-delimited list.
	 */
	private String beanProps(Object o, String[] properties) {
		if (o == null) return "null";
		var sb = new StringBuilder();
		sb.append("{");
		for (var i = 0; i < properties.length; i++) {
			if (i > 0) sb.append(",");
			sb.append(s(getBeanProp(o, properties[i])));
		}
		sb.append("}");
		return sb.toString();
	}

	/**
	 * Returns the specified element from a List or array.
	 */
	private Object get(Object o, int i) {
		if (o instanceof List) return ((List<?>)o).get(i);
		if (o.getClass().isArray()) return Array.get(o, i);
		throw new RuntimeException("Invalid data type for length(Object): " + o.getClass().getSimpleName());
	}

	/**
	 * Creates a map wrapper around a bean so that get/set operations on properties
	 * can be done via generic get()/put() methods.
	 */
	public static <T> BeanMap<T> beanMap(T bean) {
		return BeanContext.DEFAULT_SESSION.toBeanMap(bean);
	}

	/**
	 * Returns the value of the specified field/property on the specified object.
	 * First looks for getter, then looks for field.
	 * Methods and fields can be any visibility.
	 */
	public static Object getBeanProp(Object o, String name) {
		return Utils.safe(() -> {
			Field f = null;
			Class<?> c = o.getClass();
			var n = Character.toUpperCase(name.charAt(0)) + name.substring(1);
			var m = Arrays.stream(c.getMethods()).filter(x -> isGetter(x, n)).filter(x -> x.getAnnotation(BeanIgnore.class) == null).findFirst().orElse(null);
			if (m != null) {
				m.setAccessible(true);
				return m.invoke(o);
			}
			Class<?> c2 = c;
			while (f == null && c2 != null) {
				f = Arrays.stream(c2.getDeclaredFields()).filter(x -> x.getName().equals(name)).findFirst().orElse(null);
				c2 = c2.getSuperclass();
			}
			if (f != null) {
				f.setAccessible(true);
				return f.get(o);
			}
			throw new RuntimeException("No field called " + name + " found on class "+c.getName()+"");
		});
	}

	private static boolean isGetter(Method m, String n) {
		var mn = m.getName();
		return ((("get"+n).equals(mn) || ("is"+n).equals(mn)) && m.getParameterCount() == 0);
	}

	/**
	 * Returns true if this is a List or array.
	 */
	private boolean isListOrArray(Object o) {
		return o instanceof List || o.getClass().isArray();
	}

	/**
	 * Returns the length of a List or array.
	 */
	private int length(Object o) {
		if (o == null) return 0;
		if (o instanceof List) return ((List<?>)o).size();
		if (o.getClass().isArray()) return Array.getLength(o);
		throw new RuntimeException("Invalid data type for length(Object): " + o.getClass().getSimpleName());
	}

	/**
	 * Creates an array of objects.
	 */
	@SafeVarargs
	protected static <T> T[] a(T...x) {
		return x;
	}
}
