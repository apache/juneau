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

import org.apache.juneau.marshall.hocon.*;

/**
 * A pairing of a {@link HoconSerializer} and {@link HoconParser} into a single class with convenience to/of methods.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Instance usage</jc>
 * 	Hocon <jv>hocon</jv> = <jk>new</jk> Hocon();
 * 	MyBean <jv>bean</jv> = <jv>hocon</jv>.to(<jv>hoconString</jv>, MyBean.<jk>class</jk>);
 * 	String <jv>hoconOut</jv> = <jv>hocon</jv>.of(<jv>bean</jv>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Static DEFAULT instance</jc>
 * 	MyBean <jv>bean</jv> = Hocon.<jsf>DEFAULT</jsf>.to(<jv>hoconString</jv>, MyBean.<jk>class</jk>);
 * 	String <jv>hoconOut</jv> = Hocon.<jsf>DEFAULT</jsf>.of(<jv>bean</jv>);
 * </p>
 *
 * <h5 class='figure'>Example output (bean):</h5>
 * <p class='bjson'>
 * name = myapp
 * port = 8080
 * debug = true
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Marshallers">marshallers</a>
 * </ul>
 */
public class Hocon extends CharMarshaller {

	/** Default reusable instance. */
	public static final Hocon DEFAULT = new Hocon();

	/**
	 * Constructor.
	 *
	 * <p>
	 * Uses {@link HoconSerializer#DEFAULT} and {@link HoconParser#DEFAULT}.
	 */
	public Hocon() {
		this(HoconSerializer.DEFAULT, HoconParser.DEFAULT);
	}

	/**
	 * Constructor.
	 *
	 * @param s The serializer to use.
	 * @param p The parser to use.
	 */
	public Hocon(HoconSerializer s, HoconParser p) {
		super(s, p);
	}
}
