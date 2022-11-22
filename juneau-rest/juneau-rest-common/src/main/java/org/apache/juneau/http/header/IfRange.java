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
package org.apache.juneau.http.header;

import static java.time.format.DateTimeFormatter.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import java.time.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;

/**
 * Represents a parsed <l>If-Range</l> HTTP request header.
 *
 * <p>
 * If the entity is unchanged, send me the part(s) that I am missing; otherwise, send me the entire new entity.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	If-Range: "737060cd8c284d8af7ad3082f209582d"
 * </p>
 *
 * <h5 class='topic'>RFC2616 Specification</h5>
 *
 * If a client has a partial copy of an entity in its cache, and wishes to have an up-to-date copy of the entire entity
 * in its cache, it could use the Range request-header with a conditional GET (using either or both of
 * If-Unmodified-Since and If-Match.)
 * However, if the condition fails because the entity has been modified, the client would then have to make a second
 * request to obtain the entire current entity-body.
 *
 * <p>
 * The If-Range header allows a client to "short-circuit" the second request.
 * Informally, its meaning is `if the entity is unchanged, send me the part(s) that I am missing; otherwise, send me
 * the entire new entity'.
 * <p class='bcode'>
 * 	If-Range = "If-Range" ":" ( entity-tag | HTTP-date )
 * </p>
 *
 * <p>
 * If the client has no entity tag for an entity, but does have a Last- Modified date, it MAY use that date in an
 * If-Range header.
 * (The server can distinguish between a valid HTTP-date and any form of entity-tag by examining no more than two
 * characters.)
 * The If-Range header SHOULD only be used together with a Range header, and MUST be ignored if the request does not
 * include a Range header, or if the server does not support the sub-range operation.
 *
 * <p>
 * If the entity tag given in the If-Range header matches the current entity tag for the entity, then the server SHOULD
 * provide the specified sub-range of the entity using a 206 (Partial content) response.
 * If the entity tag does not match, then the server SHOULD return the entire entity using a 200 (OK) response.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616.html">Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 *
 * @serial exclude
 */
@Header("If-Range")
public class IfRange extends BasicDateHeader {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;
	private static final String NAME = "If-Range";

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be an RFC-1123 formated string (e.g. <js>"Sat, 29 Oct 1994 19:43:31 GMT"</js>).
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static IfRange of(String value) {
		return value == null ? null : new IfRange(value);
	}

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static IfRange of(ZonedDateTime value) {
		return value == null ? null : new IfRange(value);
	}

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static IfRange of(EntityTag value) {
		return value == null ? null : new IfRange(value);
	}

	/**
	 * Static creator with delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Supplier must supply either {@link EntityTag} or {@link ZonedDateTime} objects.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static IfRange of(Supplier<?> value) {
		return value == null ? null : new IfRange(value);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final EntityTag value;
	private final Supplier<?> supplier;

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be an RFC-1123 formated string (e.g. <js>"Sat, 29 Oct 1994 19:43:31 GMT"</js>).
	 * 	<br>Can be <jk>null</jk>.
	 */
	public IfRange(String value) {
		super(NAME, isEtag(value) ? null : value);
		this.value = isEtag(value) ? EntityTag.of(value) : null;
		this.supplier = null;
	}

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public IfRange(ZonedDateTime value) {
		super(NAME, value);
		this.value = null;
		this.supplier = null;
	}

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public IfRange(EntityTag value) {
		super(NAME, (String)null);
		this.value = value;
		this.supplier = null;
	}

	/**
	 * Constructor with delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Supplier must supply either {@link EntityTag} or {@link ZonedDateTime} objects.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public IfRange(Supplier<?> value) {
		super(NAME, (String)null);
		this.value = null;
		this.supplier = value;
	}

	@Override /* Header */
	public String getValue() {
		if (supplier != null) {
			Object o = supplier.get();
			if (o == null)
				return null;
			if (o instanceof EntityTag) {
				return o.toString();
			} else if (o instanceof ZonedDateTime) {
				return RFC_1123_DATE_TIME.format((ZonedDateTime)o);
			}
			throw new BasicRuntimeException("Invalid object type returned by supplier: {0}", className(o));
		}
		if (value != null)
			return stringify(value);
		return super.getValue();
	}

	/**
	 * Returns this header as an {@link EntityTag}.
	 *
	 * @return This header as an {@link EntityTag}.
	 */
	public Optional<EntityTag> asEntityTag() {
		if (supplier != null) {
			Object o = supplier.get();
			return optional(o instanceof EntityTag ? (EntityTag)o : null);
		}
		return optional(value);
	}

	private static boolean isEtag(String s) {
		return s.startsWith("\"") || s.startsWith("W/");
	}
}
