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

import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.internal.StateMachineState.*;
import static java.lang.Character.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
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
 * An example where this class is used is in the {@link Rest#staticFiles} annotation:
 * <p class='bcode w800'>
 * <jk>package</jk> com.foo.mypackage;
 *
 * <ja>@Rest</ja>(
 * 	path=<js>"/myresource"</js>,
 * 	staticFiles={<js>"htdocs:docs"</js>}
 * )
 * <jk>public class</jk> MyResource <jk>extends</jk> BasicRestServlet {...}
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
 * 	<li><l>org.apache.juneau.rest.docs</l> package (since <l>BasicRestServlet</l> is in <l>org.apache.juneau.rest</l>).
 * 	<li><l>[working-dir]/docs</l> directory.
 * </ol>
 *
 * <ul class='notes'>
 * 	<li>
 * 		Mappings are cumulative from parent to child.  Child resources can override mappings made on parent resources.
 * 	<li>
 * 		The media type on the response is determined by the {@link org.apache.juneau.rest.RestContext#getMediaTypeForName(String)} method.
 * </ul>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-rest-server.StaticFiles}
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
		this.path = trimSlashes(path);
		this.location = trimTrailingSlashes(location);
		this.responseHeaders = AMap.createUnmodifiable(responseHeaders);
	}

	/**
	 * Create one or more <c>StaticFileMappings</c> from the specified comma-delimited list of mappings.
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
	 * @param mapping
	 * 	The mapping string that represents the path/location mapping.
	 * 	<br>Leading and trailing slashes and whitespace are trimmed from path and location.
	 * @return A list of parsed mappings.  Never <jk>null</jk>.
	 * @throws ParseException If mapping was malformed.
	 */
	public static List<StaticFileMapping> parse(Class<?> resourceClass, String mapping) throws ParseException {

		mapping = trim(mapping);
		if (isEmpty(mapping))
			return Collections.emptyList();

		// States:
		// S01 = In path, looking for :
		// S02 = Found path:, looking for : or , or end
		// S03 = Found path:location:, looking for {
		// S04 = Found path:location:{, looking for }
		// S05 = Found path:location:{headers}, looking for , or end
		// S06 = Found path:location:{headers} , looking for start of path

		StateMachineState state = S01;
		int mark = 0;

		String path = null, location = null;
		List<StaticFileMapping> l = new ArrayList<>();
		String s = mapping;
		int jsonDepth = 0;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (state == S01) {
				if (c == ':') {
					path = trim(s.substring(mark, i));
					mark = i+1;
					state = S02;
				}
			} else if (state == S02) {
				if (c == ':') {
					location = trim(s.substring(mark, i));
					mark = i+1;
					state = S03;
				} else if (c == ',') {
					location = trim(s.substring(mark, i));
					l.add(new StaticFileMapping(resourceClass, path, location, null));
					mark = i+1;
					state = S01;
					path = null;
					location = null;
				}
			} else if (state == S03) {
				if (c == '{') {
					mark = i;
					state = S04;
				} else if (! isWhitespace(c)) {
					throw new ParseException("Invalid staticFiles mapping.  Expected '{' at beginning of headers.  mapping=[{0}]", mapping);
				}
			} else if (state == S04) {
				if (c == '{') {
					jsonDepth++;
				} else if (c == '}') {
					if (jsonDepth > 0) {
						jsonDepth--;
					} else {
						String json = s.substring(mark, i+1);
						l.add(new StaticFileMapping(resourceClass, path, location, new ObjectMap(json)));
						state = S05;
						path = null;
						location = null;
					}
				}
			} else if (state == S05) {
				if (c == ',') {
					state = S06;
				} else if (! isWhitespace(c)) {
					throw new ParseException("Invalid staticFiles mapping.  Invalid text following headers.  mapping=[{0}]", mapping);
				}
			} else /* state == S06 */ {
				if (! isWhitespace(c)) {
					mark = i;
					state = S01;
				}
			}
		}

		if (state == S01) {
			throw new ParseException("Invalid staticFiles mapping.  Couldn''t find '':'' following path.  mapping=[{0}]", mapping);
		} else if (state == S02) {
			location = trim(s.substring(mark, s.length()));
			l.add(new StaticFileMapping(resourceClass, path, location, null));
		} else if (state == S03) {
			throw new ParseException("Invalid staticFiles mapping.  Found extra '':'' following location.  mapping=[{0}]", mapping);
		} else if (state == S04) {
			throw new ParseException("Invalid staticFiles mapping.  Malformed headers.  mapping=[{0}]", mapping);
		}

		return l;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Object */
	public String toString() {
		return SimpleJsonSerializer.DEFAULT_READABLE.toString(toMap());
	}

	/**
	 * Returns the properties defined on this bean as a simple map for debugging purposes.
	 *
	 * @return A new map containing the properties defined on this bean.
	 */
	public ObjectMap toMap() {
		return new DefaultFilteringObjectMap()
			.append("resourceClass", resourceClass)
			.append("path", path)
			.append("location", location)
			.append("responseHeaders", responseHeaders)
		;
	}
}
