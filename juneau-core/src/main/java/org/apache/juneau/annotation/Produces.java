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
package org.apache.juneau.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.serializer.*;

/**
 * Annotation used on subclasses of {@link Serializer} to identify the media types that it produces.
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	Provides a way to define the contents of {@link Serializer#getMediaTypes()} through an annotation.
 * <p>
 * 	The {@link Serializer#getMediaTypes()} default implementation gathers the media types by looking
 * 		for this annotation.
 * 	It should be noted that this annotation is optional and that the {@link Serializer#getMediaTypes()} method can
 * 		be overridden by subclasses to return the media types programmatically.
 *
 * <h6 class='topic'>Example:</h6>
 * <p>
 * 	Standard example:
 * <p class='bcode'>
 * 	<ja>@Produces</ja>(<js>"application/json,text/json"</js>)
 * 	<jk>public class</jk> JsonSerializer <jk>extends</jk> WriterSerializer {...}
 * </p>
 * <p>
 * 	The media types can also be <code>media-range</code> values per
 * 		<a href='http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.1'>RFC2616/14.1</a>.
 * 	When meta-characters are used, you should specify the {@link #contentType()} value to
 * 		indicate the real media type value that can be set on the <code>Content-Type</code> response header.
 *
 * <p class='bcode'>
 * 	<jc>// Produces any text</jc>
 * 	<ja>@Produces</ja>(value=<js>"text\/*"</js>, contentType=<js>"text/plain"</js>)
 * 	<jk>public class</jk> AnythingSerializer <jk>extends</jk> WriterSerializer {...}
 *
 * 	<jc>// Produces anything</jc>
 * 	<ja>@Produces</ja>(value=<js>"*\/*"</js>, contentType=<js>"text/plain"</js>)
 * 	<jk>public class</jk> AnythingSerializer <jk>extends</jk> WriterSerializer {...}
 * </p>
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
@Inherited
public @interface Produces {

	/**
	 * A comma-delimited list of the media types that the serializer can handle.
	 * <p>
	 * 	Can contain meta-characters per the <code>media-type</code> specification of
	 * 	<a href='http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.1'>RFC2616/14.1</a>
	 * @return The media types that the parser can handle.
	 */
	String value() default "";

	/**
	 * The content type that this serializer produces.
	 * <p>
	 * 	Can be used to override the <code>Content-Type</code> response type if the media types
	 * 		are <code>media-ranges</code> with meta-characters, or the <code>Content-Type</code>
	 * 		differs from the media type for some reason.
	 * @return The content type that this serializer produces, or blank if no overriding value exists.
	 */
	String contentType() default "";
}
