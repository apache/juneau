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
package org.apache.juneau.http;

import static org.apache.juneau.internal.StringUtils.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.urlencoding.*;

/**
 * Specifies a dynamic supplier of {@link NameValuePair} objects.
 *
 * This class is thread safe.
 */
public class NameValuePairSupplier implements Iterable<NameValuePair> {

	/** Represents no header supplier */
	public final class Null extends NameValuePairSupplier {}

	private final List<Iterable<NameValuePair>> pairs = new CopyOnWriteArrayList<>();

	private volatile VarResolver varResolver;

	/**
	 * Convenience creator.
	 *
	 * @return A new {@link NameValuePairSupplier} object.
	 */
	public static NameValuePairSupplier create() {
		return new NameValuePairSupplier();
	}

	/**
	 * Creates an empty instance.
	 *
	 * @return A new empty instance.
	 */
	public static NameValuePairSupplier of() {
		return new NameValuePairSupplier();
	}

	/**
	 * Creates an instance initialized with the specified pairs.
	 *
	 * @param pairs The pairs to add to this list.
	 * @return A new instance.
	 */
	public static NameValuePairSupplier of(Collection<NameValuePair> pairs) {
		return new NameValuePairSupplier().addAll(pairs);
	}

	/**
	 * Creates an instance initialized with the specified pairs.
	 *
	 * @param parameters
	 * 	Initial list of parameters.
	 * 	<br>Must be an even number of parameters representing key/value pairs.
	 * @throws RuntimeException If odd number of parameters were specified.
	 * @return A new instance.
	 */
	public static NameValuePairSupplier ofPairs(Object...parameters) {
		NameValuePairSupplier s = NameValuePairSupplier.create();
		if (parameters.length % 2 != 0)
			throw new BasicRuntimeException("Odd number of parameters passed into NameValuePairSupplier.ofPairs()");
		for (int i = 0; i < parameters.length; i+=2)
			s.add(stringify(parameters[i]), parameters[i+1]);
		return s;
	}

	/**
	 * Convenience creator.
	 *
	 * @param values
	 * 	The values to populate this supplier with.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link NameValuePair}.
	 * 		<li>{@link NameValuePairSupplier}.
	 * 	</ul>
	 * @return A new {@link NameValuePairSupplier} object.
	 */
	public static NameValuePairSupplier of(Object...values) {
		NameValuePairSupplier s = NameValuePairSupplier.create();
		for (Object v : values) {
			if (v instanceof NameValuePair)
				s.add((NameValuePair)v);
			else if (v instanceof NameValuePairSupplier)
				s.add((NameValuePairSupplier)v);
			else if (v != null)
				throw new BasicRuntimeException("Invalid type passed to NameValuePairSupplier.of(): {0}", v.getClass().getName());
		}
		return s;
	}

	/**
	 * Allows values to contain SVL variables.
	 *
	 * <p>
	 * Resolves variables in values when using the following methods:
	 * <ul>
	 * 	<li class='jm'>{@link #ofPairs(Object...) ofPairs(Object...)}
	 * 	<li class='jm'>{@link #add(String, Object) add(String,Object)}
	 * 	<li class='jm'>{@link #add(String, Supplier) add(String,Supplier&lt;?&gt;)}
	 * 	<li class='jm'>{@link #add(String, Object, HttpPartType, HttpPartSerializerSession, HttpPartSchema, boolean) add(String,Object,HttpPartType,HttpPartSerializerSession,HttpPartSchema,boolean)}
	 * </ul>
	 *
	 * <p>
	 * Uses {@link VarResolver#DEFAULT} to resolve variables.
	 *
	 * @return This object (for method chaining).
	 */
	public NameValuePairSupplier resolving() {
		return resolving(VarResolver.DEFAULT);
	}

	/**
	 * Allows values to contain SVL variables.
	 *
	 * <p>
	 * Resolves variables in values when using the following methods:
	 * <ul>
	 * 	<li class='jm'>{@link #ofPairs(Object...) ofPairs(Object...)}
	 * 	<li class='jm'>{@link #add(String, Object) add(String,Object)}
	 * 	<li class='jm'>{@link #add(String, Supplier) add(String,Supplier&lt;?&gt;)}
	 * 	<li class='jm'>{@link #add(String, Object, HttpPartType, HttpPartSerializerSession, HttpPartSchema, boolean) add(String,Object,HttpPartType,HttpPartSerializerSession,HttpPartSchema,boolean)}
	 * </ul>
	 *
	 * @param varResolver The variable resolver to use for resolving variables.
	 * @return This object (for method chaining).
	 */
	public NameValuePairSupplier resolving(VarResolver varResolver) {
		this.varResolver = varResolver;
		return this;
	}

	/**
	 * Add a name-value pair to this supplier.
	 *
	 * @param h The name-value pair to add. <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	public NameValuePairSupplier add(NameValuePair h) {
		if (h != null)
			pairs.add(Collections.singleton(h));
		return this;
	}

	/**
	 * Add a supplier to this supplier.
	 *
	 * @param h The supplier to add. <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	public NameValuePairSupplier add(NameValuePairSupplier h) {
		if (h != null)
			pairs.add(h);
		return this;
	}

	/**
	 * Adds all the specified name-value pairs to this supplier.
	 *
	 * @param pairs The pairs to add to this supplier.
	 * @return This object(for method chaining).
	 */
	private NameValuePairSupplier addAll(Collection<NameValuePair> pairs) {
		this.pairs.addAll(pairs.stream().map(x->Collections.singleton(x)).collect(Collectors.toList()));
		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Appenders
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Appends the specified name/value pair to the end of this list.
	 *
	 * <p>
	 * The pair is added as a {@link BasicNameValuePair}.
	 *
	 * @param name The pair name.
	 * @param value The pair value.
	 * @return This object (for method chaining).
	 */
	public NameValuePairSupplier add(String name, Object value) {
		return add(new BasicNameValuePair(name, resolver(value)));
	}

	/**
	 * Appends the specified name/value pair to the end of this list using a value supplier.
	 *
	 * <p>
	 * The pair is added as a {@link BasicNameValuePair}.
	 *
	 * <p>
	 * Value is re-evaluated on each call to {@link BasicNameValuePair#getValue()}.
	 *
	 * @param name The pair name.
	 * @param value The pair value supplier.
	 * @return This object (for method chaining).
	 */
	public NameValuePairSupplier add(String name, Supplier<?> value) {
		return add(new BasicNameValuePair(name, resolver(value)));
	}

	/**
	 * Appends the specified name/value pair to the end of this list.
	 *
	 * <p>
	 * The value is converted to UON notation using the {@link UrlEncodingSerializer} defined on the client.
	 *
	 * @param name The pair name.
	 * @param value The pair value.
	 * @param partType The HTTP part type.
	 * @param serializer
	 * 	The serializer to use for serializing the value to a string value.
	 * @param schema
	 * 	The schema object that defines the format of the output.
	 * 	<br>If <jk>null</jk>, defaults to the schema defined on the parser.
	 * 	<br>If that's also <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 	<br>Only used if serializer is schema-aware (e.g. {@link OpenApiSerializer}).
	 * @param skipIfEmpty If value is a blank string, the value should return as <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public NameValuePairSupplier add(String name, Object value, HttpPartType partType, HttpPartSerializerSession serializer, HttpPartSchema schema, boolean skipIfEmpty) {
		return add(new SerializedNameValuePair(name, resolver(value), partType, serializer, schema, skipIfEmpty));
	}

	/**
	 * Returns this list as a URL-encoded custom query.
	 */
	@Override /* Object */
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (NameValuePair p : this) {
			String v = p.getValue();
			if (v != null) {
				if (sb.length() > 0)
					sb.append("&");
				sb.append(urlEncode(p.getName())).append('=').append(urlEncode(p.getValue()));
			}
		}
		return sb.toString();
	}

	@Override
	public Iterator<NameValuePair> iterator() {
		return CollectionUtils.iterator(pairs);
	}

	/**
	 * Returns these pairs as an array.
	 *
	 * @return These pairs as an array.
	 */
	public NameValuePair[] toArray() {
		ArrayList<NameValuePair> l = new ArrayList<>();
		for (NameValuePair p : this)
			l.add(p);
		return l.toArray(new NameValuePair[l.size()]);
	}

	/**
	 * Returns these pairs as an array.
	 *
	 * @param array The array to copy in to.
	 * @return These pairs as an array.
	 */
	public <T extends NameValuePair> T[] toArray(T[] array) {
		ArrayList<NameValuePair> l = new ArrayList<>();
		for (NameValuePair p : this)
			l.add(p);
		return l.toArray(array);
	}

	private Supplier<Object> resolver(Object input) {
		return ()->(varResolver == null ? unwrap(input) : varResolver.resolve(stringify(unwrap(input))));
	}

	private Object unwrap(Object o) {
		while (o instanceof Supplier)
			o = ((Supplier<?>)o).get();
		return o;
	}
}
