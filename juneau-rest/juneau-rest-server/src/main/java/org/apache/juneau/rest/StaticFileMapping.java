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
package org.apache.juneau.rest;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.utils.*;

/**
 * Static file mapping.
 * 
 * <p>
 * Used to define paths and locations of statically-served files such as images or HTML documents.
 * 
 * <p>
 * An example where this class is used is in the {@link RestResource#staticFiles} annotation:
 * <p class='bcode'>
 * <jk>package</jk> com.foo.mypackage;
 * 
 * <ja>@RestResource</ja>(
 * 	path=<js>"/myresource"</js>,
 * 	staticFiles={<js>"htdocs:docs"</js>}
 * )
 * <jk>public class</jk> MyResource <jk>extends</jk> RestServletDefault {...}
 * </p>
 * 
 * <p>
 * Static files are found by using the {@link ClasspathResourceFinder} defined on the resource.
 * 
 * <p>
 * In the example above, given a GET request to <l>/myresource/htdocs/foobar.html</l>, the servlet will attempt to find 
 * the <l>foobar.html</l> file in the following ordered locations:
 * <ol>
 * 	<li><l>com.foo.mypackage.docs</l> package.
 * 	<li><l>org.apache.juneau.rest.docs</l> package (since <l>RestServletDefault</l> is in <l>org.apache.juneau.rest</l>).
 * 	<li><l>[working-dir]/docs</l> directory.
 * </ol>
 * 
 * 
 * <h5 class='topic'>Notes</h5>
 * <ul class='spaced-list'>
 * 	<li>
 * 		Mappings are cumulative from parent to child.  Child resources can override mappings made on parent resources.
 * 	<li>
 * 		The media type on the response is determined by the {@link org.apache.juneau.rest.RestContext#getMediaTypeForName(String)} method.
 * </ul>
 * 
 * 
 * <h5 class='section'>Documentation:</h5>
 * <ul>
 * 	<li><a class="doclink" href="../../../../overview-summary.html#juneau-rest-server.StaticFiles">Overview &gt; Static Files</a>
 * </ul>
 */
public class StaticFileMapping {
	
	final Class<?> resourceClass;
	final String path, location;
	final Map<String,Object> responseHeaders;
	
	/**
	 * Constructor.
	 * 
	 * @param resourceClass 
	 * 	The resource/servlet class which serves as the base location of the location below.
	 * @param path 
	 * 	The mapped URI path.
	 * 	<br>Leading and trailing slashes are trimmed.
	 * @param location 
	 * 	The location relative to the resource class.
	 * 	<br>Leading and trailing slashes are trimmed.
	 * @param responseHeaders 
	 * 	The response headers.
	 * 	Can be <jk>null</jk>. 
	 */
	public StaticFileMapping(Class<?> resourceClass, String path, String location, Map<String,Object> responseHeaders) {
		this.resourceClass = resourceClass;
		this.path = StringUtils.trimSlashes(path);
		this.location = StringUtils.trimSlashes(location);
		this.responseHeaders = responseHeaders == null ? null : Collections.unmodifiableMap(new LinkedHashMap<>(responseHeaders));
	}
	
	/**
	 * Constructor using a mapping string to represent a path/location pairing.
	 * 
	 * <p>
	 * Mapping string must be one of these formats:
	 * <ul>
	 * 	<li><js>"path:location"</js> (e.g. <js>"foodocs:docs/foo"</js>)
	 * 	<li><js>"path:location:headers-json"</js> (e.g. <js>"foodocs:docs/foo:{'Cache-Control':'max-age=86400, public'}"</js>)
	 * </ul>
	 * 
	 * @param resourceClass 
	 * 	The resource/servlet class which serves as the base location of the location below.
	 * @param mappingString 
	 * 	The mapping string that represents the path/location mapping.
	 * 	<br>Leading and trailing slashes and whitespace are trimmed from path and location.
	 */
	public StaticFileMapping(Class<?> resourceClass, String mappingString) {
		this.resourceClass = resourceClass;
		String[] parts = StringUtils.split(mappingString, ':', 3);
		if (parts == null || parts.length <= 1)
			throw new FormattedRuntimeException("Invalid mapping string format: ''{0}''", mappingString);
		this.path = StringUtils.trimSlashes(parts[0]); 
		this.location = StringUtils.trimSlashes(parts[1]); 
		if (parts.length == 3) {
			try {
				responseHeaders = Collections.unmodifiableMap(new ObjectMap(parts[2]));
			} catch (ParseException e) {
				throw new FormattedRuntimeException(e, "Invalid mapping string format: ''{0}''", mappingString);
			}
		} else {
			responseHeaders = null;
		}
	}
}
