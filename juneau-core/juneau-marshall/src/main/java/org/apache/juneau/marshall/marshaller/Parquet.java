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

import org.apache.juneau.marshall.parquet.*;

/**
 * A pairing of {@link ParquetSerializer} and {@link ParquetParser} for Apache Parquet binary format.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Serialize a list of beans to Parquet bytes</jc>
 * 	List&lt;MyBean&gt; <jv>beans</jv> = List.<jsm>of</jsm>(new MyBean(...), new MyBean(...));
 * 	byte[] <jv>bytes</jv> = Parquet.<jsm>of</jsm>(<jv>beans</jv>);
 *
 * 	<jc>// Parse Parquet bytes back into a list of beans</jc>
 * 	List&lt;MyBean&gt; <jv>parsed</jv> = Parquet.<jsm>to</jsm>(<jv>bytes</jv>, MyBean.<jk>class</jk>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Using instance methods</jc>
 * 	Parquet <jv>m</jv> = Parquet.<jsf>DEFAULT</jsf>;
 * 	<jv>bytes</jv> = <jv>m</jv>.of(<jv>beans</jv>);
 * 	<jv>parsed</jv> = <jv>m</jv>.to(<jv>bytes</jv>, MyBean.<jk>class</jk>);
 * </p>
 *
 * <p>Output is binary (<jk>byte</jk>[]), Apache Parquet columnar format.</p>
 *
 * <p class='warnbox'>
 * 	<b>Note:</b> Parquet is <b>collection-oriented</b>. The serializer accepts a
 * 	{@link java.util.Collection} or array of beans (each bean is one row). A single
 * 	bean is automatically wrapped in a one-element list. The parser <em>always</em>
 * 	returns a {@code List<T>}, never a bare {@code T}.
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Parquet">Parquet Basics</a>
 * 	<li class='link'>{doc jm.Marshallers}
 * </ul>
 */
public class Parquet extends StreamMarshaller {

	/** Default reusable instance. */
	public static final Parquet DEFAULT = new Parquet();

	/**
	 * Constructor using default serializer and parser.
	 */
	public Parquet() {
		this(ParquetSerializer.DEFAULT, ParquetParser.DEFAULT);
	}

	/**
	 * Constructor.
	 *
	 * @param s The serializer.
	 * @param p The parser.
	 */
	public Parquet(ParquetSerializer s, ParquetParser p) {
		super(s, p);
	}
}
