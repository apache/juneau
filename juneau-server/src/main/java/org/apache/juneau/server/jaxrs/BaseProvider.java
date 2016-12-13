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
package org.apache.juneau.server.jaxrs;

import static javax.servlet.http.HttpServletResponse.*;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.server.annotation.*;

/**
 * Base class for defining JAX-RS providers based on Juneau serializers and parsers.
 */
public class BaseProvider implements MessageBodyReader<Object>, MessageBodyWriter<Object> {

	private SerializerGroup serializers = new SerializerGroup();
	private ParserGroup parsers = new ParserGroup();
	private ObjectMap properties = new ObjectMap();

	/**
	 * Constructor.
	 */
	protected BaseProvider() {
		try {
			properties = new ObjectMap();
			JuneauProvider jp = getClass().getAnnotation(JuneauProvider.class);
			serializers.append(jp.serializers());
			parsers.append(jp.parsers());
			for (Property p : jp.properties())
				properties.put(p.name(), p.value());
			serializers.addBeanFilters(jp.beanFilters());
			parsers.addBeanFilters(jp.beanFilters());
			serializers.addPojoSwaps(jp.pojoSwaps());
			parsers.addPojoSwaps(jp.pojoSwaps());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns properties defined on the specified method through the {@link RestMethod#properties()}
	 * 	annotation specified on the method and the {@link JuneauProvider#properties()} annotation
	 * 	specified on the provider class.
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
		return serializers.findMatch(mediaType.toString()) != null;
	}

	@Override /* MessageBodyWriter */
	public void writeTo(Object o, Class<?> type, Type gType, Annotation[] a, MediaType mediaType,
			MultivaluedMap<String,Object> headers, OutputStream out) throws IOException, WebApplicationException {
		try {
			String mt = serializers.findMatch(mediaType.toString());
			if (mt == null)
				throw new WebApplicationException(SC_NOT_ACCEPTABLE);
			Serializer s = serializers.getSerializer(mt);
			ObjectMap mp = getMethodProperties(a);
			mp.append("mediaType", mediaType.toString());
			if (s.isWriterSerializer()) {
				WriterSerializer s2 = (WriterSerializer)s;
				OutputStreamWriter w = new OutputStreamWriter(out, IOUtils.UTF8);
				SerializerSession session = s.createSession(w, mp, null);
				s2.serialize(session, o);
				w.flush();
				w.close();
			} else {
				OutputStreamSerializer s2 = (OutputStreamSerializer)s;
				SerializerSession session = s.createSession(s2, mp, null);
				s2.serialize(session, o);
				out.flush();
				out.close();
			}
		} catch (SerializeException e) {
			throw new IOException(e);
		}
	}

	@Override /* MessageBodyReader */
	public boolean isReadable(Class<?> type, Type gType, Annotation[] a, MediaType mediaType) {
		return parsers.findMatch(mediaType.toString()) != null;
	}

	@Override /* MessageBodyReader */
	public Object readFrom(Class<Object> type, Type gType, Annotation[] a, MediaType mediaType,
			MultivaluedMap<String,String> headers, InputStream in) throws IOException, WebApplicationException {
		try {
			String mt = parsers.findMatch(mediaType.toString());
			if (mt == null)
				throw new WebApplicationException(SC_UNSUPPORTED_MEDIA_TYPE);
			Parser p = parsers.getParser(mt);
			BeanContext bc = p.getBeanContext();
			ClassMeta<?> cm = bc.getClassMeta(gType);
			ObjectMap mp = getMethodProperties(a);
			mp.put("mediaType", mediaType.toString());
			if (p.isReaderParser()) {
				ReaderParser p2 = (ReaderParser)p;
				InputStreamReader r = new InputStreamReader(in, IOUtils.UTF8);
				ParserSession session = p2.createSession(r, mp, null, null);
				return p2.parse(session, cm);
			}
			InputStreamParser p2 = (InputStreamParser)p;
			ParserSession session = p2.createSession(in, mp, null, null);
			return p2.parse(session, cm);
		} catch (ParseException e) {
			throw new IOException(e);
		}
	}
}
