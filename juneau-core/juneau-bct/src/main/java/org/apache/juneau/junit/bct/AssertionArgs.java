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
package org.apache.juneau.junit.bct;

import static java.util.Optional.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.text.*;
import java.util.*;
import java.util.function.*;

/**
 * Configuration and context object for advanced assertion operations.
 *
 * <p>This class encapsulates additional arguments and configuration options for assertion methods
 * in the Bean-Centric Testing (BCT) framework. It provides a fluent API for customizing assertion
 * behavior including custom converters and enhanced error messaging.</p>
 *
 * <p>The primary purposes of this class are:</p>
 * <ul>
 *     <li><b>Custom Bean Conversion:</b> Override the default {@link BeanConverter} for specialized object introspection</li>
 *     <li><b>Enhanced Error Messages:</b> Add context-specific error messages with parameter substitution</li>
 *     <li><b>Fluent Configuration:</b> Chain configuration calls for readable test setup</li>
 *     <li><b>Assertion Context:</b> Provide additional context for complex assertion scenarios</li>
 * </ul>
 *
 * <h5 class='section'>Basic Usage:</h5>
 * <p class='bjava'>
 *     <jc>// Simple usage with default settings</jc>
 *     <jsm>assertBean</jsm>(<jsm>args</jsm>(), <jv>myBean</jv>, <js>"name,age"</js>, <js>"John,30"</js>);
 *
 *     <jc>// Custom error message</jc>
 *     <jsm>assertBean</jsm>(<jsm>args</jsm>().setMessage(<js>"User validation failed"</js>),
 *         <jv>user</jv>, <js>"email,active"</js>, <js>"john@example.com,true"</js>);
 * </p>
 *
 * <h5 class='section'>Custom Bean Converter:</h5>
 * <p class='bjava'>
 *     <jc>// Use custom converter for specialized object handling</jc>
 *     <jk>var</jk> <jv>customConverter</jv> = BasicBeanConverter.<jsm>builder</jsm>()
 *         .defaultSettings()
 *         .addStringifier(MyClass.<jk>class</jk>, <jp>obj</jp> -> <jp>obj</jp>.getDisplayName())
 *         .build();
 *
 *     <jsm>assertBean</jsm>(<jsm>args</jsm>().setBeanConverter(<jv>customConverter</jv>),
 *         <jv>myCustomObject</jv>, <js>"property"</js>, <js>"expectedValue"</js>);
 * </p>
 *
 * <h5 class='section'>Advanced Error Messages:</h5>
 * <p class='bjava'>
 *     <jc>// Parameterized error messages</jc>
 *     <jsm>assertBean</jsm>(<jsm>args</jsm>().setMessage(<js>"Validation failed for user {0}"</js>, <jv>userId</jv>),
 *         <jv>user</jv>, <js>"status"</js>, <js>"ACTIVE"</js>);
 *
 *     <jc>// Dynamic error message with supplier</jc>
 *     <jsm>assertBean</jsm>(<jsm>args</jsm>().setMessage(() -> <js>"Test failed at "</js> + Instant.<jsm>now</jsm>()),
 *         <jv>result</jv>, <js>"success"</js>, <js>"true"</js>);
 * </p>
 *
 * <h5 class='section'>Fluent Configuration:</h5>
 * <p class='bjava'>
 *     <jc>// Chain multiple configuration options</jc>
 *     <jk>var</jk> <jv>testArgs</jv> = args()
 *         .setBeanConverter(<jv>customConverter</jv>)
 *         .setMessage(<js>"Integration test failed for module {0}"</js>, <jv>moduleName</jv>);
 *
 *     <jsm>assertBean</jsm>(<jv>testArgs</jv>, <jv>moduleConfig</jv>, <js>"enabled,version"</js>, <js>"true,2.1.0"</js>);
 *     <jsm>assertBeans</jsm>(<jv>testArgs</jv>, <jv>moduleList</jv>, <js>"name,status"</js>,
 *         <js>"ModuleA,ACTIVE"</js>, <js>"ModuleB,ACTIVE"</js>);
 * </p>
 *
 * <h5 class='section'>Error Message Composition:</h5>
 * <p>When assertion failures occur, error messages are intelligently composed:</p>
 * <ul>
 *     <li><b>Base Message:</b> Custom message set via {@link #setMessage(String, Object...)} or {@link #setMessage(Supplier)}</li>
 *     <li><b>Assertion Context:</b> Specific context provided by individual assertion methods</li>
 *     <li><b>Composite Format:</b> <js>"{base message}, Caused by: {assertion context}"</js></li>
 * </ul>
 *
 * <p class='bjava'>
 *     <jc>// Example error message composition:</jc>
 *     <jc>// Base: "User validation failed for user 123"</jc>
 *     <jc>// Context: "Bean assertion failed."</jc>
 *     <jc>// Result: "User validation failed for user 123, Caused by: Bean assertion failed."</jc>
 * </p>
 *
 * <h5 class='section'>Thread Safety:</h5>
 * <p>This class is <b>not thread-safe</b> and is intended for single-threaded test execution.
 * Each test method should create its own instance using {@link BctAssertions#args()} or create
 * a new instance directly with {@code new AssertionArgs()}.</p>
 *
 * <h5 class='section'>Immutability Considerations:</h5>
 * <p>While this class uses fluent setters that return {@code this} for chaining, the instance
 * is mutable. For reusable configurations across multiple tests, consider creating a factory
 * method that returns pre-configured instances.</p>
 *
 * @see BctAssertions#args()
 * @see BeanConverter
 * @see BasicBeanConverter
 */
public class AssertionArgs {

	private BeanConverter beanConverter;
	private Supplier<String> messageSupplier;

	/**
	 * Creates a new instance with default settings.
	 *
	 * <p>Instances start with no custom bean converter and no custom error message.
	 * All assertion methods will use default behavior until configured otherwise.</p>
	 */
	public AssertionArgs() { /* no-op */ }

	/**
	 * Sets a custom {@link BeanConverter} for object introspection and property access.
	 *
	 * <p>The custom converter allows fine-tuned control over how objects are converted to strings,
	 * how collections are listified, and how nested properties are accessed. This is particularly
	 * useful for:</p>
	 * <ul>
	 *     <li><b>Custom Object Types:</b> Objects that don't follow standard JavaBean patterns</li>
	 *     <li><b>Specialized Formatting:</b> Custom string representations for assertion comparisons</li>
	 *     <li><b>Performance Optimization:</b> Cached or optimized property access strategies</li>
	 *     <li><b>Domain-Specific Logic:</b> Business-specific property resolution rules</li>
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 *     <jc>// Create converter with custom stringifiers</jc>
	 *     <jk>var</jk> <jv>converter</jv> = BasicBeanConverter.<jsm>builder</jsm>()
	 *         .defaultSettings()
	 *         .addStringifier(LocalDate.<jk>class</jk>, <jp>date</jp> -> <jp>date</jp>.format(DateTimeFormatter.<jsf>ISO_LOCAL_DATE</jsf>))
	 *         .addStringifier(Money.<jk>class</jk>, <jp>money</jp> -> <jp>money</jp>.getAmount().toPlainString())
	 *         .build();
	 *
	 *     <jc>// Use in assertions</jc>
	 *     <jsm>assertBean</jsm>(<jsm>args</jsm>().setBeanConverter(<jv>converter</jv>),
	 *         <jv>order</jv>, <js>"date,total"</js>, <js>"2023-12-01,99.99"</js>);
	 * </p>
	 *
	 * @param value The custom bean converter to use. If null, assertions will fall back to the default converter.
	 * @return This instance for method chaining.
	 */
	public AssertionArgs setBeanConverter(BeanConverter value) {
		beanConverter = value;
		return this;
	}

	/**
	 * Sets a parameterized error message for assertion failures.
	 *
	 * <p>This method uses {@link MessageFormat} to substitute parameters into the message template.
	 * The formatting occurs immediately when this method is called, not when the assertion fails.</p>
	 *
	 * <h5 class='section'>Parameter Substitution:</h5>
	 * <p>Uses standard MessageFormat patterns:</p>
	 * <ul>
	 *     <li><code>{0}</code> - First parameter</li>
	 *     <li><code>{1}</code> - Second parameter</li>
	 *     <li><code>{0,number,#}</code> - Formatted number</li>
	 *     <li><code>{0,date,short}</code> - Formatted date</li>
	 * </ul>
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 *     <jc>// Simple parameter substitution</jc>
	 *     <jsm>assertBean</jsm>(<jsm>args</jsm>().setMessage(<js>"User {0} validation failed"</js>, <jv>userId</jv>),
	 *         <jv>user</jv>, <js>"active"</js>, <js>"true"</js>);
	 *
	 *     <jc>// Multiple parameters</jc>
	 *     <jsm>assertBean</jsm>(<jsm>args</jsm>().setMessage(<js>"Test {0} failed on iteration {1}"</js>, <jv>testName</jv>, <jv>iteration</jv>),
	 *         <jv>result</jv>, <js>"success"</js>, <js>"true"</js>);
	 *
	 *     <jc>// Number formatting</jc>
	 *     <jsm>assertBean</jsm>(<jsm>args</jsm>().setMessage(<js>"Expected {0,number,#.##} but got different value"</js>, <jv>expectedValue</jv>),
	 *         <jv>actual</jv>, <js>"value"</js>, <js>"123.45"</js>);
	 * </p>
	 *
	 * @param message The message template with MessageFormat placeholders.
	 * @param args The parameters to substitute into the message template.
	 * @return This instance for method chaining.
	 */
	public AssertionArgs setMessage(String message, Object...args) {
		messageSupplier = fs(message, args);
		return this;
	}

	/**
	 * Sets a custom error message supplier for assertion failures.
	 *
	 * <p>The supplier allows for dynamic message generation, including context that may only
	 * be available at the time of assertion failure. This is useful for:</p>
	 * <ul>
	 *     <li><b>Timestamps:</b> Including the exact time of failure</li>
	 *     <li><b>Test State:</b> Including runtime state information</li>
	 *     <li><b>Expensive Operations:</b> Deferring costly string operations until needed</li>
	 *     <li><b>Conditional Messages:</b> Different messages based on runtime conditions</li>
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 *     <jc>// Dynamic message with timestamp</jc>
	 *     <jsm>assertBean</jsm>(<jsm>args</jsm>().setMessage(() -> <js>"Test failed at "</js> + Instant.<jsm>now</jsm>()),
	 *         <jv>result</jv>, <js>"status"</js>, <js>"SUCCESS"</js>);
	 *
	 *     <jc>// Message with expensive computation</jc>
	 *     <jsm>assertBean</jsm>(<jsm>args</jsm>().setMessage(() -> <js>"Failed after "</js> + computeTestDuration() + <js>" ms"</js>),
	 *         <jv>response</jv>, <js>"error"</js>, <js>"null"</js>);
	 * </p>
	 *
	 * @param value The message supplier. Called only when an assertion fails.
	 * @return This instance for method chaining.
	 */
	public AssertionArgs setMessage(Supplier<String> value) {
		messageSupplier = value;
		return this;
	}

	/**
	 * Gets the configured bean converter, if any.
	 *
	 * @return An Optional containing the custom converter, or empty if using default behavior.
	 */
	protected Optional<BeanConverter> getBeanConverter() { return ofNullable(beanConverter); }

	/**
	 * Gets the base message supplier for composition with assertion-specific messages.
	 *
	 * @return The configured message supplier, or null if no custom message was set.
	 */
	protected Supplier<String> getMessage() { return messageSupplier; }

	/**
	 * Composes the final error message by combining custom and assertion-specific messages.
	 *
	 * <p>This method implements the message composition strategy used throughout the assertion framework:</p>
	 * <ul>
	 *     <li><b>No Custom Message:</b> Returns the assertion-specific message as-is</li>
	 *     <li><b>With Custom Message:</b> Returns <code>"{custom}, Caused by: {assertion}"</code></li>
	 * </ul>
	 *
	 * <p>This allows tests to provide high-level context while preserving the specific
	 * technical details about what assertion failed.</p>
	 *
	 * @param msg The assertion-specific message template.
	 * @param args Parameters for the assertion-specific message.
	 * @return A supplier that produces the composed error message.
	 */
	protected Supplier<String> getMessage(String msg, Object...args) {
		return messageSupplier == null ? fs(msg, args) : fs("{0}, Caused by: {1}", messageSupplier.get(), f(msg, args));
	}
}