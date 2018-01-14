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
package org.apache.juneau.rest.jaxrs;

import static javax.servlet.http.HttpServletResponse.*;
import static org.apache.juneau.internal.IOUtils.*;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.serializer.*;

/**
 * Base class for defining JAX-RS providers based on Juneau serializers and parsers.
 */
public class BaseProvider implements MessageBodyReader<Object>, MessageBodyWriter<Object> {

	private SerializerGroup serializers;
	private ParserGroup parsers;
	private ObjectMap properties = new ObjectMap();

	/**
	 * Constructor.
	 */
	protected BaseProvider() {
		try {
			properties = new ObjectMap();
			JuneauProvider jp = getClass().getAnnotation(JuneauProvider.class);

			for (Property p : jp.properties())
				properties.put(p.name(), p.value());
			for (String p : jp.flags())
				properties.put(p, true);

			serializers = SerializerGroup.create()
				.append(jp.serializers())
				.beanFilters(jp.beanFilters())
				.pojoSwaps(jp.pojoSwaps())
				.set(properties)
				.build();

			parsers = ParserGroup.create()
				.append(jp.parsers())
				.beanFilters(jp.beanFilters())
				.pojoSwaps(jp.pojoSwaps())
				.set(properties)
				.build();

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns properties defined on the specified method through the {@link RestMethod#properties() @RestMethod.properties()}
	 * annotation specified on the method and the {@link JuneauProvider#properties()} annotation specified on the 
	 * provider class.
	 * 
	 * @param a All annotations defined on the method.
	 * @return A map of all properties define on the method.
	 */
	protected ObjectMap getMethodProperties(Annotation[] a) {
		ObjectMap m = new ObjectMap().setInner(properties);
		for (Annotation aa : a) {
			if (aa instanceof RestMethod) {
				for (Property p : ((RestMethod)aa).properties())
					m.put(p.name(), p.value());
				for (String p : ((RestMethod)aa).flags())
					m.put(p, true);
			}
		}
		return m;
	}

	@Override /* MessageBodyWriter */
	public long getSize(Object o, Class<?> type, Type gType, Annotation[] a, MediaType mediaType) {
		return -1;
	}

	@Override /* MessageBodyWriter */
	public boolean isWriteable(Class<?> type, Type gType, Annotation[] a, MediaType mediaType) {
		return serializers.getSerializerMatch(mediaType.toString()) != null;
	}

	@Override /* MessageBodyWriter */
	public void writeTo(Object o, Class<?> type, Type gType, Annotation[] a, MediaType mediaType,
			MultivaluedMap<String,Object> headers, OutputStream os) throws IOException, WebApplicationException {
		try {
			SerializerMatch sm = serializers.getSerializerMatch(mediaType.toString());
			if (sm == null)
				throw new WebApplicationException(SC_NOT_ACCEPTABLE);
			Serializer s = sm.getSerializer();
			ObjectMap mp = getMethodProperties(a);
			mp.append("mediaType", mediaType.toString());
			Locale locale = getLocale(headers);
			TimeZone timeZone = getTimeZone(headers);
			
			SerializerSession session = s.createSession(new SerializerSessionArgs(mp, null, locale, timeZone, sm.getMediaType(), null));
			
			// Leave this open in case an error occurs.
			Closeable c = s.isWriterSerializer() ? new OutputStreamWriter(os, UTF8) : os;
			session.serialize(o, c);

		} catch (SerializeException e) {
			throw new IOException(e);
		}
	}

	@Override /* MessageBodyReader */
	public boolean isReadable(Class<?> type, Type gType, Annotation[] a, MediaType mediaType) {
		return parsers.getParserMatch(mediaType.toString()) != null;
	}

	@Override /* MessageBodyReader */
	public Object readFrom(Class<Object> type, Type gType, Annotation[] a, MediaType mediaType,
			MultivaluedMap<String,String> headers, InputStream in) throws IOException, WebApplicationException {
		try {
			ParserMatch pm = parsers.getParserMatch(mediaType.toString());
			if (pm == null)
				throw new WebApplicationException(SC_UNSUPPORTED_MEDIA_TYPE);
			Parser p = pm.getParser();
			ObjectMap mp = getMethodProperties(a);
			mp.put("mediaType", mediaType.toString());
			Locale locale = getLocale(headers);
			TimeZone timeZone = getTimeZone(headers);
			ParserSession session = p.createSession(new ParserSessionArgs(mp, null, locale, timeZone, pm.getMediaType(), null));
			Object in2 = session.isReaderParser() ? new InputStreamReader(in, UTF8) : in;
			return session.parse(in2, p.getClassMeta(gType));
		} catch (ParseException e) {
			throw new IOException(e);
		}
	}

	@SuppressWarnings("rawtypes")
	private static Locale getLocale(MultivaluedMap headers) {
		if (headers.containsKey("Accept-Language") && headers.get("Accept-Language") != null) {
			String h = String.valueOf(headers.get("Accept-Language"));
			if (h != null) {
				MediaTypeRange[] mr = MediaTypeRange.parse(h);
				if (mr.length > 0)
					return toLocale(mr[0].getMediaType().getType());
			}
		}
		return null;
	}

	/*
	 * Converts an Accept-Language value entry to a Locale.
	 */
	private static Locale toLocale(String lang) {
		String country = "";
		int i = lang.indexOf('-');
		if (i > -1) {
			country = lang.substring(i+1).trim();
			lang = lang.substring(0,i).trim();
		}
		return new Locale(lang, country);
	}

	@SuppressWarnings("rawtypes")
	private static TimeZone getTimeZone(MultivaluedMap headers) {
		if (headers.containsKey("Time-Zone") && headers.get("Time-Zone") != null) {
			String h = String.valueOf(headers.get("Time-Zone"));
			return TimeZone.getTimeZone(h);
		}
		return null;
	}

}
