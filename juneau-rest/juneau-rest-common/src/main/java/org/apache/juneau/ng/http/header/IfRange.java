/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.ng.http.header;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.apache.juneau.commons.utils.ThrowableUtils.rex;
import static org.apache.juneau.commons.utils.Utils.*;

import java.time.*;
import java.util.Optional;
import java.util.function.*;

import org.apache.juneau.http.header.*;

/**
 * Represents an HTTP <c>If-Range</c> header.
 *
 * <p>
 * If the entity is unchanged, send me the part(s) that I am missing; otherwise, send me the entire new entity.
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
@SuppressWarnings({
	"java:S2160" // equals() on HttpHeaderBean uses name + getValue()
})
public class IfRange extends HttpHeaderBean {

	/** The header name */
	public static final String NAME = "If-Range";

	private static boolean isEtag(String s) {
		return s != null && (s.startsWith("\"") || s.startsWith("W/"));
	}

	private final EntityTag etagValue;
	private final ZonedDateTime zonedValue;
	private final Supplier<?> supplier;

	/**
	 * Constructor with an entity-tag value.
	 *
	 * @param value The entity tag. May be <jk>null</jk>.
	 */
	public IfRange(EntityTag value) {
		super(NAME, (String)null);
		this.etagValue = value;
		this.zonedValue = null;
		this.supplier = null;
	}

	/**
	 * Constructor with a wire string (HTTP-date or entity-tag).
	 *
	 * @param value The header value. May be <jk>null</jk>.
	 */
	public IfRange(String value) {
		super(NAME, (String)null);
		if (value != null && isEtag(value)) {
			this.etagValue = EntityTag.of(value);
			this.zonedValue = null;
		} else {
			this.etagValue = null;
			this.zonedValue = e(value) ? null : parseHttpDate(value);
		}
		this.supplier = null;
	}

	/**
	 * Constructor with a lazy supplier of {@link EntityTag} or {@link ZonedDateTime}.
	 *
	 * @param valueSupplier Supplier for the value. Must not be <jk>null</jk>.
	 */
	public IfRange(Supplier<?> valueSupplier) {
		super(NAME, (String)null);
		this.etagValue = null;
		this.zonedValue = null;
		this.supplier = valueSupplier;
	}

	/**
	 * Constructor with an HTTP-date.
	 *
	 * @param value The date/time. May be <jk>null</jk>.
	 */
	public IfRange(ZonedDateTime value) {
		super(NAME, (String)null);
		this.etagValue = null;
		this.zonedValue = value;
		this.supplier = null;
	}

	/**
	 * @return This header as an {@link EntityTag}.
	 */
	public Optional<EntityTag> asEntityTag() {
		if (nn(supplier)) {
			var o = supplier.get();
			return opt(o instanceof EntityTag o2 ? o2 : null);
		}
		return opt(etagValue);
	}

	/**
	 * @return This header as a {@link ZonedDateTime} when the value is an HTTP-date.
	 */
	public Optional<ZonedDateTime> asZonedDateTime() {
		if (nn(supplier)) {
			var o = supplier.get();
			return opt(o instanceof ZonedDateTime o2 ? o2 : null);
		}
		return opt(zonedValue);
	}

	@Override /* HttpHeader */
	public String getValue() {
		if (nn(supplier)) {
			var o = supplier.get();
			if (o == null)
				return null;
			if (o instanceof EntityTag o2)
				return o2.toString();
			if (o instanceof ZonedDateTime o2)
				return RFC_1123_DATE_TIME.format(o2);
			throw rex("Invalid object type returned by supplier: {0}", cn(o));
		}
		if (nn(etagValue))
			return s(etagValue);
		if (nn(zonedValue))
			return RFC_1123_DATE_TIME.format(zonedValue);
		return null;
	}

	/**
	 * Static factory with an entity-tag.
	 *
	 * @param value The tag. May be <jk>null</jk>.
	 * @return A new instance or <jk>null</jk>.
	 */
	public static IfRange of(EntityTag value) {
		return value == null ? null : new IfRange(value);
	}

	/**
	 * Static factory with a wire string.
	 *
	 * @param value The value. May be <jk>null</jk>.
	 * @return A new instance or <jk>null</jk>.
	 */
	public static IfRange of(String value) {
		return value == null ? null : new IfRange(value);
	}

	/**
	 * Static factory with a lazy supplier.
	 *
	 * @param valueSupplier Supplier of {@link EntityTag} or {@link ZonedDateTime}. Must not be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static IfRange of(Supplier<?> valueSupplier) {
		return new IfRange(valueSupplier);
	}

	/**
	 * Static factory with an HTTP-date.
	 *
	 * @param value The date/time. May be <jk>null</jk>.
	 * @return A new instance or <jk>null</jk>.
	 */
	public static IfRange of(ZonedDateTime value) {
		return value == null ? null : new IfRange(value);
	}

	private static ZonedDateTime parseHttpDate(String value) {
		return ZonedDateTime.from(RFC_1123_DATE_TIME.parse(value)).truncatedTo(SECONDS);
	}
}
