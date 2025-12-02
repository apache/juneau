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
package org.apache.juneau.common.reflect;

/**
 * Defines how class names should be formatted when rendered as strings.
 *
 * <p>
 * Controls which parts of the fully qualified class name are included in the output.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Given class: java.util.Map.Entry</jc>
 *
 * 	ClassNameFormat.<jsf>FULL</jsf>      <jc>// "java.util.Map$Entry" or "java.util.Map.Entry"</jc>
 * 	ClassNameFormat.<jsf>SHORT</jsf>     <jc>// "Map$Entry" or "Map.Entry"</jc>
 * 	ClassNameFormat.<jsf>SIMPLE</jsf>    <jc>// "Entry"</jc>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-common">Common</a>
 * </ul>
 */
public enum ClassNameFormat {

	/**
	 * Full name including package and outer classes.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><js>"java.lang.String"</js>
	 * 	<li><js>"java.util.Map$Entry"</js> or <js>"java.util.Map.Entry"</js> (depending on separator)
	 * 	<li><js>"com.example.Outer$Inner$Deep"</js>
	 * </ul>
	 *
	 * <p>
	 * This format includes the complete package path and all enclosing class names.
	 * The separator character between outer and inner classes is configurable.
	 */
	FULL,

	/**
	 * Short name including outer classes but not the package.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><js>"String"</js>
	 * 	<li><js>"Map$Entry"</js> or <js>"Map.Entry"</js> (depending on separator)
	 * 	<li><js>"Outer$Inner$Deep"</js>
	 * </ul>
	 *
	 * <p>
	 * This format includes enclosing class names but omits the package.
	 * Useful when the package context is already known or not needed.
	 * The separator character between outer and inner classes is configurable.
	 */
	SHORT,

	/**
	 * Simple name without package or outer classes.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><js>"String"</js>
	 * 	<li><js>"Entry"</js>
	 * 	<li><js>"Deep"</js>
	 * </ul>
	 *
	 * <p>
	 * This format returns only the innermost class name, omitting both the package
	 * and any enclosing class names. This is equivalent to {@link Class#getSimpleName()}.
	 */
	SIMPLE
}
