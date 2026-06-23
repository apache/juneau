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
package org.apache.juneau.marshall.marshaller;

import org.apache.juneau.marshall.markdown.*;

/**
 * A pairing of a {@link MarkdownSerializer} and {@link MarkdownParser} into a single class with convenience to/of methods.
 *
 * <p>
 * The general idea is to combine a single serializer and parser inside a simplified API for serializing and parsing POJOs.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Using instance.</jc>
 * 	Markdown <jv>md</jv> = <jk>new</jk> Markdown();
 * 	MyPojo <jv>myPojo</jv> = <jv>md</jv>.to(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = <jv>md</jv>.of(<jv>myPojo</jv>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Using DEFAULT instance.</jc>
 * 	MyPojo <jv>myPojo</jv> = Markdown.<jsf>DEFAULT</jsf>.to(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = Markdown.<jsf>DEFAULT</jsf>.of(<jv>myPojo</jv>);
 * </p>
 *
 * <h5 class='figure'>Example output (bean with name/age):</h5>
 * <p class='bcode'>
 * 	| Property | Value |
 * 	|---|---|
 * 	| name | Alice |
 * 	| age | 30 |
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Marshallers">Marshallers</a>
 * </ul>
 */
public class Markdown extends CharMarshaller {

	/**
	 * Default reusable instance using fragment-mode serializer and parser.
	 */
	public static final Markdown DEFAULT = new Markdown();

	/**
	 * Constructor.
	 *
	 * <p>
	 * Uses {@link MarkdownSerializer#DEFAULT} and {@link MarkdownParser#DEFAULT}.
	 */
	public Markdown() {
		this(MarkdownSerializer.DEFAULT, MarkdownParser.DEFAULT);
	}

	/**
	 * Constructor.
	 *
	 * @param s
	 * 	The serializer to use for serializing output.
	 * 	<br>Must not be <jk>null</jk>.
	 * @param p
	 * 	The parser to use for parsing input.
	 * 	<br>Must not be <jk>null</jk>.
	 */
	public Markdown(MarkdownSerializer s, MarkdownParser p) {
		super(s, p);
	}
}
