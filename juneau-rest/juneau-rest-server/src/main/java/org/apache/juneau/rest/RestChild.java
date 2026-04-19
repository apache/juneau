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
package org.apache.juneau.rest;

import org.apache.juneau.cp.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Represents a simple child REST resource / path mapping.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Parent resource declares its children declaratively via the @Rest annotation.</jc>
 * 	<ja>@Rest</ja>(children = { MyChildResource.<jk>class</jk>, OtherChildResource.<jk>class</jk> })
 * 	<jk>public class</jk> MyResource { ... }
 *
 * 	<jc>// Or programmatically via RestContextInit when constructing the context directly:</jc>
 * 	<jk>new</jk> RestContext(<jk>new</jk> RestContextInit(
 * 		MyResource.<jk>class</jk>, <jk>null</jk>, <jk>null</jk>, () -&gt; <jk>new</jk> MyResource(),
 * 		<js>""</js>, List.<jsm>of</jsm>(<jk>new</jk> RestChild(<js>"/child"</js>, <jk>new</jk> MyChildResource()))));
 * </p>
 *
 * <h5 class='section'>Note (9.5):</h5>
 * <p>
 * The legacy {@code public MyResource(RestContext.Builder builder)} constructor pattern (where the resource class
 * imperatively registered children inside its own ctor) is removed in 9.5 along with {@code RestContext.Builder}.
 * Resource classes now declare children either via the {@link Rest#children() @Rest(children=...)} annotation or
 * by passing them through {@link RestContextInit#children() RestContextInit.children}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestAnnotatedClassBasics">@Rest-Annotated Class Basics</a>
 * </ul>
 */
public class RestChild {

	final String path;
	final Object resource;

	/**
	 * Constructor.
	 *
	 * @param path The child resource path relative to the parent resource URI.
	 * @param resource
	 * 	The child resource.
	 * 	<br>Can either be a Class (which will be instantiated using the registered {@link BasicBeanStore})
	 * 	or an already-instantiated object.
	 */
	public RestChild(String path, Object resource) {
		this.path = path;
		this.resource = resource;
	}
}