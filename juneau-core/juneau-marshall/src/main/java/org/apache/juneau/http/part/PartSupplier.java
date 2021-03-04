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
package org.apache.juneau.http.part;

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
 * Specifies a dynamic supplier of {@link Part} objects.
 *
 * This class is thread safe.
 */
public class PartSupplier implements Iterable<Part> {

	/** Represents no header supplier */
	public static final class Null extends PartSupplier {}

	private final List<Iterable<Part>> parts = new CopyOnWriteArrayList<>();

	private volatile VarResolver varResolver;

	/**
	 * Convenience creator.
	 *
	 * @return A new {@link PartSupplier} object.
	 */
	public static PartSupplier create() {
		return new PartSupplier();
	}

	/**
	 * Creates an empty instance.
	 *
	 * @return A new empty instance.
	 */
	public static PartSupplier of() {
		return new PartSupplier();
	}

	/**
	 * Creates an instance initialized with the specified pairs.
	 *
	 * @param pairs The pairs to add to this list.
	 * @return A new instance.
	 */
	public static PartSupplier of(Collection<Part> pairs) {
		return new PartSupplier().addAll(pairs);
	}

	/**
	 * Creates an instance initialized with the specified pairs.
	 *
	 * @param pairs
	 * 	Initial list of parts.
	 * 	<br>Must be an even number of parts representing key/value pairs.
	 * @throws RuntimeException If odd number of parts were specified.
	 * @return A new instance.
	 */
	public static PartSupplier ofPairs(Object...pairs) {
		PartSupplier s = PartSupplier.create();
		if (pairs.length % 2 != 0)
			throw new BasicRuntimeException("Odd number of pairs passed into PartSupplier.ofPairs()");
		for (int i = 0; i < pairs.length; i+=2)
			s.add(stringify(pairs[i]), pairs[i+1]);
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
	 * 		<li>{@link PartSupplier}.
	 * 	</ul>
	 * @return A new {@link PartSupplier} object.
	 */
	public static PartSupplier of(Object...values) {
		PartSupplier s = PartSupplier.create();
		for (Object v : values) {
			if (v instanceof Part)
				s.add((Part)v);
			else if (v instanceof PartSupplier)
				s.add((PartSupplier)v);
			else if (v != null)
				throw new BasicRuntimeException("Invalid type passed to PartSupplier.of(): {0}", v.getClass().getName());
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
	public PartSupplier resolving() {
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
	public PartSupplier resolving(VarResolver varResolver) {
		this.varResolver = varResolver;
		return this;
	}

	/**
	 * Add a name-value pair to this supplier.
	 *
	 * @param h The name-value pair to add. <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	public PartSupplier add(Part h) {
		if (h != null)
			parts.add(Collections.singleton(h));
		return this;
	}

	/**
	 * Add a supplier to this supplier.
	 *
	 * @param h The supplier to add. <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	public PartSupplier add(PartSupplier h) {
		if (h != null)
			parts.add(h);
		return this;
	}

	/**
	 * Adds all the specified name-value pairs to this supplier.
	 *
	 * @param pairs The pairs to add to this supplier.
	 * @return This object(for method chaining).
	 */
	private PartSupplier addAll(Collection<Part> pairs) {
		this.parts.addAll(pairs.stream().filter(x->x != null).map(x->Collections.singleton(x)).collect(Collectors.toList()));
		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Appenders
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Appends the specified name/value pair to the end of this list.
	 *
	 * <p>
	 * The pair is added as a {@link BasicPart}.
	 *
	 * @param name The pair name.
	 * @param value The pair value.
	 * @return This object (for method chaining).
	 */
	public PartSupplier add(String name, Object value) {
		return add(new BasicPart(name, resolver(value)));
	}

	/**
	 * Appends the specified name/value pair to the end of this list using a value supplier.
	 *
	 * <p>
	 * The pair is added as a {@link BasicPart}.
	 *
	 * <p>
	 * Value is re-evaluated on each call to {@link BasicPart#getValue()}.
	 *
	 * @param name The pair name.
	 * @param value The pair value supplier.
	 * @return This object (for method chaining).
	 */
	public PartSupplier add(String name, Supplier<?> value) {
		return add(new BasicPart(name, resolver(value)));
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
	public PartSupplier add(String name, Object value, HttpPartType partType, HttpPartSerializerSession serializer, HttpPartSchema schema, boolean skipIfEmpty) {
		return add(new SerializedPart(name, resolver(value), partType, serializer, schema, skipIfEmpty));
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
	public Iterator<Part> iterator() {
		return CollectionUtils.iterator(parts);
	}

	/**
	 * Returns these pairs as an array.
	 *
	 * @return These pairs as an array.
	 */
	public Part[] toArray() {
		ArrayList<Part> l = new ArrayList<>();
		for (Part p : this)
			l.add(p);
		return l.toArray(new Part[l.size()]);
	}

	/**
	 * Returns these pairs as an array.
	 *
	 * @param array The array to copy in to.
	 * @return These pairs as an array.
	 */
	public <T extends Part> T[] toArray(T[] array) {
		ArrayList<Part> l = new ArrayList<>();
		for (Part p : this)
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
