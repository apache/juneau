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

import org.apache.juneau.parser.*;

/**
 * Annotation used on subclasses of {@link Parser} to identify the media types that it consumes.
 *
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	Provides a way to define the contents of {@link Parser#getMediaTypes()} through an annotation.
 * <p>
 * 	The {@link Parser#getMediaTypes()} default implementation gathers the media types by looking
 * 		for this annotation.
 * 	It should be noted that this annotation is optional and that the {@link Parser#getMediaTypes()} method can
 * 		be overridden by subclasses to return the media types programmatically.
 *
 *
 * <h6 class='topic'>Examples</h6>
 * <p>
 * 	Standard example:
 * <p class='bcode'>
 * 	<ja>@Consumes</ja>({<js>"application/json"</js>,<js>"text/json"</js>})
 * 	<jk>public class</jk> JsonParser <jk>extends</jk> ReaderParser {...}
 * </p>
 * <p>
 * 	The media types can also be <code>media-range</code> values per
 * 		<a href='http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.1'>RFC2616/14.1</a>.
 * <p class='bcode'>
 * 	<jc>// Consumes any text</jc>
 * 	<ja>@Consumes</ja>({<js>"text\/*"</js>})
 * 	<jk>public class</jk> AnythingParser <jk>extends</jk> ReaderParser {...}
 *
 * 	<jc>// Consumes anything</jc>
 * 	<ja>@Consumes</ja>({<js>"*\/*"</js>})
 * 	<jk>public class</jk> AnythingParser <jk>extends</jk> ReaderParser {...}
 * </p>
 *
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
@Inherited
public @interface Consumes {

	/**
	 * The media types that the parser can handle.
	 * <p>
	 * 	Can contain meta-characters per the <code>media-type</code> specification of
	 * 	<a href='http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.1'>RFC2616/14.1</a>
	 * @return The media types that the parser can handle.
	 */
	String[] value() default {};
}
