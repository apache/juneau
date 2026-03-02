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
package org.apache.juneau.jena;

/**
 * Subclass of {@link RdfSerializer} for serializing POJOs to TriX (XML) format.
 *
 * <p>
 * Produces RDF triples in TriX XML format. TriX represents RDF as XML elements, suitable for
 * XML-based toolchains and transformations.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Serialize a bean to TriX XML string.</jc>
 * 	Person <jv>person</jv> = <jk>new</jk> Person(<js>"Alice"</js>, 30);
 * 	String <jv>triX</jv> = TriXSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>person</jv>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Or use the TriX marshaller for convenience.</jc>
 * 	String <jv>triX</jv> = TriX.of(<jv>person</jv>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Custom serializer with swaps.</jc>
 * 	TriXSerializer <jv>s</jv> = TriXSerializer.create().swaps(DateSwap.<jk>class</jk>).build();
 * 	String <jv>triX</jv> = <jv>s</jv>.serialize(<jv>bean</jv>);
 * </p>
 *
 * <h5 class='figure'>Example output (bean with name/age):</h5>
 * <p class='bxml'>
 * 	<xt>&lt;TriX&gt;</xt>
 * 		<xt>&lt;graph&gt;</xt>
 * 			<xt>&lt;uri&gt;</xt>...<xt>&lt;/uri&gt;</xt>
 * 			<xt>&lt;triple&gt;</xt>
 * 				<xt>&lt;id&gt;</xt>...<xt>&lt;/id&gt;</xt>
 * 				<xt>&lt;name&gt;</xt>Alice<xt>&lt;/name&gt;</xt>
 * 				<xt>&lt;age&gt;</xt>30<xt>&lt;/age&gt;</xt>
 * 			<xt>&lt;/triple&gt;</xt>
 * 		<xt>&lt;/graph&gt;</xt>
 * 	<xt>&lt;/TriX&gt;</xt>
 * </p>
 *
 * <h5 class='figure'>Complex (nested address + array):</h5>
 * <p class='bxml'>
 * 	<xt>&lt;TriX&gt;</xt>
 * 		<xt>&lt;graph&gt;</xt>
 * 			<xt>&lt;triple&gt;</xt>...subject, predicate, object for name, age, address ref, tags...
 * 			<xt>&lt;triple&gt;</xt>...address subject, street, city, state...
 * 			<xt>&lt;triple&gt;</xt>...tags list members...
 * 		<xt>&lt;/graph&gt;</xt>
 * 	<xt>&lt;/TriX&gt;</xt>
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'>{doc jmr.RdfDetails}
 * </ul>
 */
public class TriXSerializer extends RdfSerializer {

	/** Default TriX serializer, all default settings.*/
	public static final TriXSerializer DEFAULT = new TriXSerializer(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static RdfSerializer.Builder create() {
		return RdfSerializer.create().triX();
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public TriXSerializer(RdfSerializer.Builder builder) {
		super(builder.triX());
	}
}
