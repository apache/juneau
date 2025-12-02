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
package org.apache.juneau.commons.function;

/**
 * A functional interface identical to {@link Runnable} but allows the {@link #run()} method to throw checked exceptions.
 *
 * <p>
 * This interface is useful when you need to pass arbitrary code snippets to methods that expect a {@link Runnable},
 * but your code may throw checked exceptions. Unlike {@link Runnable}, the {@link #run()} method can throw any
 * {@link Throwable}, making it suitable for exception testing and fluent API patterns.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>Functional interface - can be used with lambda expressions and method references
 * 	<li>Exception support - allows checked exceptions to be thrown
 * 	<li>Fluent API friendly - enables passing code snippets in method chains
 * 	<li>Testing support - useful for exception testing scenarios
 * </ul>
 *
 * <h5 class='section'>Use Cases:</h5>
 * <ul class='spaced-list'>
 * 	<li>Exception testing - verifying that code throws expected exceptions
 * 	<li>Fluent interfaces - passing code snippets to builder methods
 * 	<li>Conditional execution - wrapping code that may throw exceptions
 * 	<li>Error handling - passing error-prone code to handlers
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Exception testing</jc>
 * 	Snippet <jv>code</jv> = () -&gt; {
 * 		<jk>throw new</jk> IllegalArgumentException(<js>"Expected error"</js>);
 * 	};
 * 	<jsm>assertThrown</jsm>(IllegalArgumentException.<jk>class</jk>, <jv>code</jv>);
 *
 * 	<jc>// Fluent API usage</jc>
 * 	<jv>builder</jv>
 * 		.setValue(<js>"test"</js>)
 * 		.onError(() -&gt; {
 * 			<jk>throw new</jk> ValidationException(<js>"Invalid value"</js>);
 * 		})
 * 		.build();
 *
 * 	<jc>// Conditional execution with exceptions</jc>
 * 	<jk>if</jk> (<jv>shouldExecute</jv>) {
 * 		<jv>executeSafely</jv>(() -&gt; {
 * 			<jk>throw new</jk> IOException(<js>"File not found"</js>);
 * 		});
 * 	}
 * </p>
 *
 * <h5 class='section'>Comparison with Runnable:</h5>
 * <ul class='spaced-list'>
 * 	<li><b>Runnable:</b> Cannot throw checked exceptions (must catch and wrap)
 * 	<li><b>Snippet:</b> Can throw any {@link Throwable} (checked or unchecked)
 * 	<li><b>Runnable:</b> Used for standard Java concurrency patterns
 * 	<li><b>Snippet:</b> Used for exception testing and fluent APIs
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauEcosystemOverview">Juneau Ecosystem Overview</a>
 * </ul>
 */
public interface Snippet {

	/**
	 * Executes arbitrary code and optionally throws an exception.
	 *
	 * <p>
	 * This method is the functional method of this interface. It can throw any {@link Throwable},
	 * including checked exceptions, which distinguishes it from {@link Runnable#run()}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Snippet <jv>snippet</jv> = () -&gt; {
	 * 		<jc>// Code that may throw exceptions</jc>
	 * 		<jk>if</jk> (<jv>invalid</jv>) {
	 * 			<jk>throw new</jk> IllegalArgumentException(<js>"Invalid state"</js>);
	 * 		}
	 * 	};
	 *
	 * 	<jv>snippet</jv>.run();  <jc>// May throw IllegalArgumentException</jc>
	 * </p>
	 *
	 * @throws Throwable Any throwable (checked or unchecked).
	 */
	void run() throws Throwable;
}