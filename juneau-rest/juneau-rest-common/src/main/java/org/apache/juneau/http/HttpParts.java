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
package org.apache.juneau.http;

import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.httppart.*;
import org.apache.juneau.commons.lang.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.http.part.*;

/**
 * Static factory methods for creating HTTP parts (query parameters, form fields, path variables).
 *
 * <p>
 * Import statically for clean DSL-style usage:
 * <p class='bjava'>
 * 	import static org.apache.juneau.http.HttpParts.*;
 *
 * 	PartList <jv>form</jv> = PartList.<jsm>of</jsm>(
 * 		part(<js>"username"</js>, <js>"alice"</js>),
 * 		part(<js>"password"</js>, <jv>passwordSupplier</jv>)
 * 	);
 * </p>
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.ng.*}).
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/juneau-ng-rest-client">juneau-ng REST client</a>
 * </ul>
 *
 * @since 9.2.1
 */
public final class HttpParts {

	private HttpParts() {}

	/**
	 * Creates an {@link HttpPart} with an eager string value.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param value The part value. May be <jk>null</jk>.
	 * @return A new part. Never <jk>null</jk>.
	 */
	public static HttpPart part(String name, String value) {
		return HttpPartBean.of(name, value);
	}

	/**
	 * Creates an {@link HttpPart} with a lazy value supplier.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param valueSupplier Supplier for the value. Must not be <jk>null</jk>.
	 * @return A new part. Never <jk>null</jk>.
	 */
	public static HttpPart part(String name, Supplier<String> valueSupplier) {
		return HttpPartBean.of(name, valueSupplier);
	}

	/**
	 * Creates a {@link PartList} from the given parts.
	 *
	 * @param parts The parts. Must not be <jk>null</jk>.
	 * @return A new list. Never <jk>null</jk>.
	 */
	public static PartList partList(HttpPart... parts) {
		return PartList.of(parts);
	}

	/**
	 * Creates a {@link PartList} from alternating name/value string pairs.
	 *
	 * @param pairs Alternating name/value strings. Must not be <jk>null</jk>. Length must be even.
	 * @return A new list. Never <jk>null</jk>.
	 */
	public static PartList partListOfPairs(String... pairs) {
		return PartList.ofPairs(pairs);
	}

	private static final Function<ClassMeta<?>,ConstructorInfo> CONSTRUCTOR_FUNCTION =
		x -> x.getPublicConstructor(y -> y.hasParameterTypes(String.class))
			.orElseGet(() -> x.getPublicConstructor(y -> y.hasParameterTypes(String.class, String.class)).orElse(null));

	/**
	 * Returns the {@code (String)} or {@code (String,String)} public constructor for the specified type.
	 *
	 * <p>
	 * Used by part-parsing logic to instantiate typed parts from a wire string.
	 *
	 * @param type The type to find the constructor on. Must not be <jk>null</jk>.
	 * @return The constructor wrapped in an {@link Optional}; empty if none is found.
	 */
	public static Optional<ConstructorInfo> getConstructor(ClassMeta<?> type) {
		return type.getProperty("HttpPart.Constructor", CONSTRUCTOR_FUNCTION);
	}

	/**
	 * Returns <jk>true</jk> if the specified type is a transport-neutral part type for the given
	 * {@link HttpPartType}.
	 *
	 * <p>
	 * The check is whether the type is assignable to {@link HttpHeader} (for {@code HEADER}) or
	 * {@link HttpPart} (for {@code PATH}, {@code QUERY}, {@code FORMDATA}).
	 *
	 * @param partType The part type. Must not be <jk>null</jk>.
	 * @param type The type to check. Must not be <jk>null</jk>.
	 * @return <jk>true</jk> if the type is a part type for the given category.
	 */
	public static boolean isHttpPart(HttpPartType partType, ClassMeta<?> type) {
		return switch (partType) {
			case PATH, QUERY, FORMDATA -> type.getProperty("HttpPart.isHttpPart", x -> x.isAssignableTo(HttpPart.class)).orElse(false);
			case HEADER -> type.getProperty("HttpPart.isHttpHeader", x -> x.isAssignableTo(HttpHeader.class)).orElse(false);
			default -> false;
		};
	}

	private static final Function<ClassMeta<?>,String> HEADER_NAME_FUNCTION = x -> {
		var n = Holder.<String>empty();
		x.forEachAnnotation(org.apache.juneau.http.Header.class, y -> ne(y.value()), y -> n.set(y.value()));
		x.forEachAnnotation(org.apache.juneau.http.Header.class, y -> ne(y.name()), y -> n.set(y.name()));
		if (n.isEmpty())
			n.set(readPublicStaticStringField(x.inner(), "NAME"));
		return n.orElse(null);
	};

	private static final Function<ClassMeta<?>,String> QUERY_NAME_FUNCTION = x -> {
		var n = Holder.<String>empty();
		x.forEachAnnotation(org.apache.juneau.http.Query.class, y -> ne(y.value()), y -> n.set(y.value()));
		x.forEachAnnotation(org.apache.juneau.http.Query.class, y -> ne(y.name()), y -> n.set(y.name()));
		if (n.isEmpty())
			n.set(readPublicStaticStringField(x.inner(), "NAME"));
		return n.orElse(null);
	};

	private static final Function<ClassMeta<?>,String> FORMDATA_NAME_FUNCTION = x -> {
		var n = Holder.<String>empty();
		x.forEachAnnotation(org.apache.juneau.http.FormData.class, y -> ne(y.value()), y -> n.set(y.value()));
		x.forEachAnnotation(org.apache.juneau.http.FormData.class, y -> ne(y.name()), y -> n.set(y.name()));
		if (n.isEmpty())
			n.set(readPublicStaticStringField(x.inner(), "NAME"));
		return n.orElse(null);
	};

	private static final Function<ClassMeta<?>,String> PATH_NAME_FUNCTION = x -> {
		var n = Holder.<String>empty();
		x.forEachAnnotation(org.apache.juneau.http.Path.class, y -> ne(y.value()), y -> n.set(y.value()));
		x.forEachAnnotation(org.apache.juneau.http.Path.class, y -> ne(y.name()), y -> n.set(y.name()));
		if (n.isEmpty())
			n.set(readPublicStaticStringField(x.inner(), "NAME"));
		return n.orElse(null);
	};

	private static String readPublicStaticStringField(Class<?> c, String fieldName) {
		try {
			var f = c.getField(fieldName);
			if (java.lang.reflect.Modifier.isStatic(f.getModifiers()) && f.getType() == String.class) {
				var v = f.get(null);
				return v == null ? null : v.toString();
			}
		} catch (@SuppressWarnings("unused") ReflectiveOperationException ignored) {
			// Field does not exist or is not accessible.
		}
		return null;
	}

	/**
	 * Returns the name of the specified part type by reading the {@code @Header}, {@code @Query},
	 * {@code @FormData}, or {@code @Path} annotation on the class.
	 *
	 * @param partType The part type. Must not be <jk>null</jk>.
	 * @param type The type to check. Must not be <jk>null</jk>.
	 * @return The part name, wrapped in an {@link Optional}.
	 */
	public static Optional<String> getName(HttpPartType partType, ClassMeta<?> type) {
		return switch (partType) {
			case FORMDATA -> type.getProperty("HttpPart.formData.name", FORMDATA_NAME_FUNCTION);
			case HEADER -> type.getProperty("HttpPart.header.name", HEADER_NAME_FUNCTION);
			case PATH -> type.getProperty("HttpPart.path.name", PATH_NAME_FUNCTION);
			case QUERY -> type.getProperty("HttpPart.query.name", QUERY_NAME_FUNCTION);
			default -> opte();
		};
	}
}
