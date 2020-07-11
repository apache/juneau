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
import org.apache.juneau.marshall.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.svl.*;

/**
 * Specifies a dynamic supplier of {@link Header} objects.
 *
 * This class is thread safe.
 */
public class HeaderSupplier implements Iterable<Header> {

	/** Represents no header supplier */
	public static final class Null extends HeaderSupplier {}

	private final List<Iterable<Header>> headers = new CopyOnWriteArrayList<>();

	private volatile VarResolver varResolver;

	/**
	 * Convenience creator.
	 *
	 * @return A new {@link HeaderSupplier} object.
	 */
	public static HeaderSupplier create() {
		return new HeaderSupplier();
	}

	/**
	 * Creates an empty instance.
	 *
	 * @return A new empty instance.
	 */
	public static HeaderSupplier of() {
		return new HeaderSupplier();
	}

	/**
	 * Creates an instance initialized with the specified headers.
	 *
	 * @param headers The headers to add to this list.
	 * @return A new instance.
	 */
	public static HeaderSupplier of(Collection<Header> headers) {
		return new HeaderSupplier().addAll(headers);
	}

	/**
	 * Creates an instance initialized with the specified name/value pairs.
	 *
	 * @param parameters
	 * 	Initial list of parameters.
	 * 	<br>Must be an even number of parameters representing key/value pairs.
	 * @throws RuntimeException If odd number of parameters were specified.
	 * @return A new instance.
	 */
	public static HeaderSupplier ofPairs(Object...parameters) {
		HeaderSupplier s = HeaderSupplier.create();
		if (parameters.length % 2 != 0)
			throw new BasicRuntimeException("Odd number of parameters passed into HeaderSupplier.ofPairs()");
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
	 * 		<li>{@link Header}.
	 * 		<li>{@link HeaderSupplier}.
	 * 	</ul>
	 * @return A new {@link HeaderSupplier} object.
	 */
	public static HeaderSupplier of(Object...values) {
		HeaderSupplier s = HeaderSupplier.create();
		for (Object v : values) {
			if (v instanceof Header)
				s.add((Header)v);
			else if (v instanceof HeaderSupplier)
				s.add((HeaderSupplier)v);
			else if (v != null)
				throw new BasicRuntimeException("Invalid type passed to HeaderSupplier.of(): {0}", v.getClass().getName());
		}
		return s;
	}

	/**
	 * Allows header values to contain SVL variables.
	 *
	 * <p>
	 * Resolves variables in header values when using the following methods:
	 * <ul>
	 * 	<li class='jm'>{@link #ofPairs(Object...) ofPairs(Object...)}
	 * 	<li class='jm'>{@link #add(String, Object) add(String,Object)}
	 * 	<li class='jm'>{@link #add(String, Supplier) add(String,Supplier&lt;?&gt;)}
	 * 	<li class='jm'>{@link #add(String, Object, HttpPartSerializerSession, HttpPartSchema, boolean) add(String,Object,HttpPartSerializerSession,HttpPartSchema,boolean)}
	 * </ul>
	 *
	 * <p>
	 * Uses {@link VarResolver#DEFAULT} to resolve variables.
	 *
	 * @return This object (for method chaining).
	 */
	public HeaderSupplier resolving() {
		return resolving(VarResolver.DEFAULT);
	}

	/**
	 * Allows header values to contain SVL variables.
	 *
	 * <p>
	 * Resolves variables in header values when using the following methods:
	 * <ul>
	 * 	<li class='jm'>{@link #ofPairs(Object...) ofPairs(Object...)}
	 * 	<li class='jm'>{@link #add(String, Object) add(String,Object)}
	 * 	<li class='jm'>{@link #add(String, Supplier) add(String,Supplier&lt;?&gt;)}
	 * 	<li class='jm'>{@link #add(String, Object, HttpPartSerializerSession, HttpPartSchema, boolean) add(String,Object,HttpPartSerializerSession,HttpPartSchema,boolean)}
	 * </ul>
	 *
	 * @param varResolver The variable resolver to use for resolving variables.
	 * @return This object (for method chaining).
	 */
	public HeaderSupplier resolving(VarResolver varResolver) {
		this.varResolver = varResolver;
		return this;
	}

	/**
	 * Add a header to this supplier.
	 *
	 * @param h The header to add. <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	public HeaderSupplier add(Header h) {
		if (h != null)
			headers.add(Collections.singleton(h));
		return this;
	}

	/**
	 * Add a supplier to this supplier.
	 *
	 * @param h The supplier to add. <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	public HeaderSupplier add(HeaderSupplier h) {
		if (h != null)
			headers.add(h);
		return this;
	}

	/**
	 * Adds all the specified headers to this supplier.
	 *
	 * @param headers The headers to add to this supplier.
	 * @return This object(for method chaining).
	 */
	private HeaderSupplier addAll(Collection<Header> headers) {
		this.headers.addAll(headers.stream().filter(x->x != null).map(x->Collections.singleton(x)).collect(Collectors.toList()));
		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Appenders
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Appends the specified header to the end of this list.
	 *
	 * <p>
	 * The header is added as a {@link BasicHeader}.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return This object (for method chaining).
	 */
	public HeaderSupplier add(String name, Object value) {
		return add(new BasicHeader(name, resolver(value)));
	}

	/**
	 * Appends the specifiedheader to the end of this list using a value supplier.
	 *
	 * <p>
	 * The header is added as a {@link BasicHeader}.
	 *
	 * <p>
	 * Value is re-evaluated on each call to {@link BasicHeader#getValue()}.
	 *
	 * @param name The header name.
	 * @param value The header value supplier.
	 * @return This object (for method chaining).
	 */
	public HeaderSupplier add(String name, Supplier<?> value) {
		return add(new BasicHeader(name, resolver(value)));
	}

	/**
	 * Appends the specified header to the end of this list.
	 *
	 * @param name The header name.
	 * @param value The header value.
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
	public HeaderSupplier add(String name, Object value, HttpPartSerializerSession serializer, HttpPartSchema schema, boolean skipIfEmpty) {
		return add(new SerializedHeader(name, resolver(value), serializer, schema, skipIfEmpty));
	}

	/**
	 * Returns this list as a JSON list of strings.
	 */
	@Override /* Object */
	public String toString() {
		return SimpleJson.DEFAULT.toString(toArray());
	}

	@Override
	public Iterator<Header> iterator() {
		return CollectionUtils.iterator(headers);
	}

	/**
	 * Returns these headers as an array.
	 *
	 * @return These headers as an array.
	 */
	public Header[] toArray() {
		ArrayList<Header> l = new ArrayList<>();
		for (Header p : this)
			l.add(p);
		return l.toArray(new Header[l.size()]);
	}

	/**
	 * Returns these headers as an array.
	 *
	 * @param array The array to copy in to.
	 * @return These headers as an array.
	 */
	public <T extends Header> T[] toArray(T[] array) {
		ArrayList<Header> l = new ArrayList<>();
		for (Header p : this)
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
