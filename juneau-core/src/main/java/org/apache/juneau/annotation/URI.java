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
import java.net.*;

import org.apache.juneau.serializer.*;

/**
 * Used to identify a class or bean property as a URI.
 * <p>
 * 	By default, instances of {@link URL} and {@link URI} are considered URIs during serialization, and are
 * 		handled differently depending on the serializer (e.g. <code>HtmlSerializer</code> creates a hyperlink,
 * 		<code>RdfXmlSerializer</code> creates an <code>rdf:resource</code> object, etc...).
 * <p>
 * 	This annotation allows you to identify other classes that return URIs via <code>toString()</code> as URI objects.
 * <p>
 * 	Relative URIs are automatically prepended with {@link SerializerContext#SERIALIZER_absolutePathUriBase} and {@link SerializerContext#SERIALIZER_relativeUriBase}
 * 		during serialization just like relative <code>URIs</code>.
 * <p>
 * 	This annotation can be applied to classes, interfaces, or bean property methods for fields.
 *
 * <h6 class='topic'>Examples</h6>
 * <p class='bcode'>
 *
 * 	<jc>// Applied to a class whose toString() method returns a URI.</jc>
 * 	<ja>@URI</ja>
 * 	<jk>public class</jk> MyURI {
 * 		<ja>@Override</ja>
 * 		<jk>public</jk> String toString() {
 * 			<jk>return</jk> <js>"http://localhost:9080/foo/bar"</js>;
 * 		}
 * 	}
 *
 * 	<jc>// Applied to bean properties</jc>
 * 	<jk>public class</jk> MyBean {
 *
 * 		<ja>@URI</ja>
 * 		<jk>public</jk> String <jf>beanUri</jf>;
 *
 * 		<ja>@URI</ja>
 * 		<jk>public</jk> String getParentUri() {
 * 			...
 * 		}
 * 	}
 * </p>
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
@Documented
@Target({TYPE,FIELD,METHOD})
@Retention(RUNTIME)
@Inherited
public @interface URI {}