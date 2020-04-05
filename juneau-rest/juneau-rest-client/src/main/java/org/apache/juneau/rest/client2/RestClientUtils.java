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
package org.apache.juneau.rest.client2;

import static org.apache.juneau.httppart.HttpPartType.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.rest.client2.AddFlag.*;

import java.util.*;

import org.apache.http.*;
import org.apache.http.message.BasicHeader;
import org.apache.juneau.http.*;
import org.apache.juneau.httppart.*;

/**
 * Static utility methods shared by mutiple classes.
 */
class RestClientUtils {

	static Header toHeader(Object o) {
		if (o instanceof Header)
			return (Header)o;
		if (o instanceof NameValuePair) {
			NameValuePair p = (NameValuePair)o;
			return new BasicHeader(p.getName(), p.getValue());
		}
		return null;
	}

	static NameValuePair toQuery(Object o) {
		if (o instanceof NameValuePair)
			return (NameValuePair)o;
		return null;
	}

	static NameValuePair toFormData(Object o) {
		if (o instanceof NameValuePair)
			return (NameValuePair)o;
		return null;
	}

	static List<Header> toHeaders(NameValuePair...pairs) {
		List<Header> l = new ArrayList<>();
		for (NameValuePair p : pairs)
			l.add(toHeader(p));
		return l;
	}

	static List<Header> toHeaders(NameValuePairs pairs) {
		List<Header> l = new ArrayList<>();
		for (NameValuePair p : pairs)
			l.add(toHeader(p));
		return l;
	}

	static Header toHeader(EnumSet<AddFlag> flags, String name, Object value, HttpPartSerializerSession serializer, HttpPartSchema schema) {
		return new SerializedHeader(name, value, serializer, schema, flags.contains(SKIP_IF_EMPTY));
	}

	static NameValuePair toQuery(EnumSet<AddFlag> flags, String name, Object value, HttpPartSerializerSession serializer, HttpPartSchema schema) {
		return new SerializedNameValuePair(name, value, QUERY,  serializer, schema, flags.contains(SKIP_IF_EMPTY));
	}

	static NameValuePair toFormData(EnumSet<AddFlag> flags, String name, Object value, HttpPartSerializerSession serializer, HttpPartSchema schema) {
		return new SerializedNameValuePair(name, value, FORMDATA,  serializer, schema, flags.contains(SKIP_IF_EMPTY));
	}

	@SuppressWarnings("rawtypes")
	static List<Header> toHeaders(EnumSet<AddFlag> flags, Map headers, HttpPartSerializerSession serializer, HttpPartSchema schema) {
		List<Header> l = new ArrayList<>();
		for (Map.Entry e : (Set<Map.Entry>)headers.entrySet())
			l.add(new SerializedHeader(stringify(e.getKey()), e.getValue(), serializer, null, flags.contains(SKIP_IF_EMPTY)));
		return l;
	}

	@SuppressWarnings("rawtypes")
	static List<NameValuePair> toQuery(EnumSet<AddFlag> flags, Map params, HttpPartSerializerSession serializer, HttpPartSchema schema) {
		List<NameValuePair> l = new ArrayList<>();
		for (Map.Entry e : (Set<Map.Entry>)params.entrySet())
			l.add(new SerializedNameValuePair(stringify(e.getKey()), e.getValue(), QUERY, serializer, null, flags.contains(SKIP_IF_EMPTY)));
		return l;
	}

	@SuppressWarnings("rawtypes")
	static List<NameValuePair> toFormData(EnumSet<AddFlag> flags, Map params, HttpPartSerializerSession serializer, HttpPartSchema schema) {
		List<NameValuePair> l = new ArrayList<>();
		for (Map.Entry e : (Set<Map.Entry>)params.entrySet())
			l.add(new SerializedNameValuePair(stringify(e.getKey()), e.getValue(), FORMDATA, serializer, null, flags.contains(SKIP_IF_EMPTY)));
		return l;
	}
}
