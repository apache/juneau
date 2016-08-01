/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.server.jaxrs;

import static javax.servlet.http.HttpServletResponse.*;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.core.utils.*;
import com.ibm.juno.server.annotation.*;

/**
 * Base class for defining JAX-RS providers based on Juno serializers and parsers.
 *
 * @author James Bognar (jbognar@us.ibm.com)
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
			JunoProvider jp = getClass().getAnnotation(JunoProvider.class);
			serializers.append(jp.serializers());
			parsers.append(jp.parsers());
			for (Property p : jp.properties())
				properties.put(p.name(), p.value());
			serializers.addFilters(jp.filters());
			parsers.addFilters(jp.filters());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns properties defined on the specified method through the {@link RestMethod#properties()}
	 * 	annotation specified on the method and the {@link JunoProvider#properties()} annotation
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
			Serializer<?> s = serializers.getSerializer(mt);
			ObjectMap mp = getMethodProperties(a);
			mp.append("mediaType", mediaType.toString());
			SerializerContext ctx = s.createContext(mp, null);
			if (s.isWriterSerializer()) {
				WriterSerializer s2 = (WriterSerializer)s;
				OutputStreamWriter w = new OutputStreamWriter(out, IOUtils.UTF8);
				s2.serialize(o, w, ctx);
				w.flush();
				w.close();
			} else {
				OutputStreamSerializer s2 = (OutputStreamSerializer)s;
				s2.serialize(o, out, ctx);
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
			Parser<?> p = parsers.getParser(mt);
			BeanContext bc = p.getBeanContext();
			ClassMeta<?> cm = bc.getClassMeta(gType);
			ObjectMap mp = getMethodProperties(a);
			mp.put("mediaType", mediaType.toString());
			int length = IOUtils.getBufferSize(headers.getFirst("Content-Length"));
			if (p.isReaderParser()) {
				ReaderParser p2 = (ReaderParser)p;
				InputStreamReader r = new InputStreamReader(in, IOUtils.UTF8);
				ParserContext ctx = p2.createContext(mp, null, null);
				return p2.parse(r, length, cm, ctx);
			}
			InputStreamParser p2 = (InputStreamParser)p;
			ParserContext ctx = p2.createContext(mp, null, null);
			return p2.parse(in, length, cm, ctx);
		} catch (ParseException e) {
			throw new IOException(e);
		}
	}
}
