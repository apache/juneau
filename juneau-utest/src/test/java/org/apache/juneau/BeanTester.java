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

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.marshaller.*;

/**
 * Utility class for testing bean functionality in a standardized way.
 * 
 * <p>
 * This class provides a fluent API for setting up and executing common bean tests including:
 * <ul>
 *    <li>Getter/setter validation using {@link TestUtils#assertBean(Object, String, String)}
 *    <li>Copy constructor/method testing
 *    <li>JSON serialization/deserialization testing
 *    <li>Round-trip serialization testing
 *    <li>toString() method validation
 * </ul>
 * 
 * <h5 class='section'>Example Usage:</h5>
 * <p class='bjava'>
 *    BeanTester&lt;MyBean&gt; tester = testBean(myBean)
 *       .json("{prop1:'value1',prop2:'value2'}")
 *       .props("prop1,prop2")
 *       .vals("'value1','value2'")
 *       .string("{prop1:'value1',prop2:'value2'}");
 *    
 *    tester.assertGettersAndSetters();
 *    tester.assertToJson();
 *    tester.assertFromJson();
 *    tester.assertRoundTrip();
 *    tester.assertToString();
 * </p>
 * 
 * <p>
 * This class is typically used in nested test classes to provide consistent testing patterns
 * across different bean types in the Apache Juneau project.
 * 
 * @param <T> The type of bean being tested
 */
public class BeanTester<T> {

	private T bean;
	private Class<T> c;
	
	/**
	 * Sets the bean instance to be tested.
	 * 
	 * @param value The bean instance to test.
	 * @return This object for method chaining.
	 */
	public BeanTester<T> bean(T value) { bean = value; c = (Class<T>) bean.getClass(); return this; }
	
	/**
	 * Returns the bean instance being tested.
	 * 
	 * @return The bean instance being tested.
	 */
	public T bean() { return bean; }

	private String json;
	
	/**
	 * Sets the expected JSON representation of the bean.
	 * 
	 * <p>
	 * This JSON string is used by {@link #assertToJson()} to verify that the bean
	 * serializes to the expected JSON format, and by {@link #assertFromJson()} to
	 * verify that the JSON deserializes back to an equivalent bean.
	 * 
	 * @param value The expected JSON representation.
	 * @return This object for method chaining.
	 */
	public BeanTester<T> json(String value) { json = value; return this; }
	
	/**
	 * Returns the expected JSON representation of the bean.
	 * 
	 * @return The expected JSON representation.
	 */
	public String json() { return json; }

	private String props;
	
	/**
	 * Sets the comma-delimited list of property names for bean validation.
	 * 
	 * <p>
	 * This string is used by {@link TestUtils#assertBean(Object, String, String)} to
	 * validate that the bean has the expected properties with the expected values.
	 * Property names should be listed in the same order as their corresponding values
	 * in the {@link #vals(String)} parameter.
	 * 
	 * @param value Comma-delimited list of property names (e.g., "prop1,prop2,prop3").
	 * @return This object for method chaining.
	 */
	public BeanTester<T> props(String value) { props = value; return this; }
	
	/**
	 * Returns the comma-delimited list of property names.
	 * 
	 * @return The comma-delimited list of property names.
	 */
	public String props() { return props; }

	private String vals;
	
	/**
	 * Sets the comma-delimited list of expected property values for bean validation.
	 * 
	 * <p>
	 * This string is used by {@link TestUtils#assertBean(Object, String, String)} to
	 * validate that the bean properties have the expected values. Values should be
	 * listed in the same order as their corresponding property names in the
	 * {@link #props(String)} parameter. String values should be quoted (e.g., "'value'").
	 * 
	 * @param value Comma-delimited list of expected values (e.g., "'value1','value2',123").
	 * @return This object for method chaining.
	 */
	public BeanTester<T> vals(String value) { vals = value; return this; }
	
	/**
	 * Returns the comma-delimited list of expected property values.
	 * 
	 * @return The comma-delimited list of expected property values.
	 */
	public String vals() { return vals; }

	private String string;
	
	/**
	 * Sets the expected string representation of the bean.
	 * 
	 * <p>
	 * This string is used by {@link #assertToString()} to verify that the bean's
	 * {@code toString()} method returns the expected value.
	 * 
	 * @param value The expected string representation.
	 * @return This object for method chaining.
	 */
	public BeanTester<T> string(String value) { string = value; return this; }
	
	/**
	 * Returns the expected string representation of the bean.
	 * 
	 * @return The expected string representation.
	 */
	public String string() { return string; }

	/**
	 * Validates that the bean's getters and setters work correctly.
	 * 
	 * <p>
	 * This method uses {@link TestUtils#assertBean(Object, String, String)} to verify
	 * that the bean's properties match the expected values specified by {@link #props(String)}
	 * and {@link #vals(String)}.
	 * 
	 * @throws AssertionError if any property values don't match expectations.
	 */
	public void assertGettersAndSetters() {
		assertBean(bean, props, vals);
	}

	/**
	 * Validates that the bean's copy constructor or copy() method works correctly.
	 * 
	 * <p>
	 * This method uses reflection to invoke the bean's {@code copy()} method, then verifies:
	 * <ul>
	 *    <li>The returned object is not the same instance as the original
	 *    <li>The returned object has the same property values as the original
	 * </ul>
	 * 
	 * @throws AssertionError if the copy is the same instance or has different property values.
	 * @throws RuntimeException if the bean doesn't have a {@code copy()} method or it fails to invoke.
	 */
	public void assertCopy() {
		var t = safe(()->bean.getClass().getMethod("copy").invoke(bean));
		assertNotSame(bean, t);
		assertBean(t, props, vals);
	}

	/**
	 * Validates that the bean serializes to the expected JSON format.
	 * 
	 * <p>
	 * This method uses {@link TestUtils#assertJson(String, Object)} to verify that
	 * the bean serializes to the JSON string specified by {@link #json(String)}.
	 * 
	 * @throws AssertionError if the serialized JSON doesn't match the expected format.
	 */
	public void assertToJson() {
		assertJson(json, bean);
	}

	/**
	 * Validates that the expected JSON deserializes to a bean with correct property values.
	 * 
	 * <p>
	 * This method deserializes the JSON string specified by {@link #json(String)} and
	 * verifies that the resulting bean has the expected property values specified by
	 * {@link #props(String)} and {@link #vals(String)}.
	 * 
	 * @throws AssertionError if the deserialized bean doesn't have the expected property values.
	 * @throws RuntimeException if JSON deserialization fails.
	 */
	public void assertFromJson() {
		assertBean(json(json, c), props, vals);
	}

	/**
	 * Validates that the bean can be serialized to JSON and deserialized back correctly.
	 * 
	 * <p>
	 * This method performs a complete round-trip test by:
	 * <ol>
	 *    <li>Serializing the bean to JSON
	 *    <li>Deserializing the JSON back to a bean
	 *    <li>Verifying the resulting bean has the expected property values
	 * </ol>
	 * 
	 * @throws AssertionError if the round-trip bean doesn't have the expected property values.
	 * @throws RuntimeException if serialization or deserialization fails.
	 */
	public void assertRoundTrip() {
		assertBean(jsonRoundTrip(bean, c), props, vals);
	}

	/**
	 * Validates that the bean's toString() method returns the expected string.
	 * 
	 * <p>
	 * This method compares the bean's {@code toString()} output with the string
	 * specified by {@link #string(String)}.
	 * 
	 * @throws AssertionError if the toString() output doesn't match the expected string.
	 */
	public void assertToString() {
		assertEquals(string, bean.toString());
	}

	/**
	 * Serializes an object to JSON using the default sorted JSON serializer.
	 * 
	 * @param o The object to serialize.
	 * @return The JSON representation of the object.
	 */
	private static String json(Object o) {
		return Json5.DEFAULT_SORTED.write(o);
	}

	/**
	 * Deserializes a JSON string to an object of the specified type.
	 * 
	 * @param <T> The type to deserialize to.
	 * @param o The JSON string to deserialize.
	 * @param c The class to deserialize to.
	 * @return The deserialized object.
	 */
	private static <T> T json(String o, Class<T> c) {
		return safe(()->Json5.DEFAULT_SORTED.read(o, c));
	}

	/**
	 * Performs a round-trip serialization/deserialization test.
	 * 
	 * @param <T> The type of object to round-trip.
	 * @param o The object to serialize and deserialize.
	 * @param c The class to deserialize to.
	 * @return The object after round-trip serialization/deserialization.
	 */
	private static <T> T jsonRoundTrip(T o, Class<T> c) {
		return json(json(o), c);
	}
}
