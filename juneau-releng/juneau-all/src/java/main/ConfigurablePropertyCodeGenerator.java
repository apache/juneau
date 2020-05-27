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

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.config.*;
import org.apache.juneau.config.store.*;
import org.apache.juneau.csv.*;
import org.apache.juneau.html.*;
import org.apache.juneau.http.exception.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.jena.*;
import org.apache.juneau.jso.*;
import org.apache.juneau.json.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.plaintext.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.client2.*;
import org.apache.juneau.rest.mock2.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.soap.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;

public class ConfigurablePropertyCodeGenerator {

	static Class<?>[] classes = new Class<?>[]{
		BeanContextBuilder.class,
		BeanTraverseBuilder.class,
		ConfigBuilder.class,
		ConfigClasspathStoreBuilder.class,
		ConfigFileStoreBuilder.class,
		ConfigMemoryStoreBuilder.class,
		ConfigStoreBuilder.class,
		ContextBuilder.class,
		CsvParserBuilder.class,
		CsvSerializerBuilder.class,
		HtmlDocSerializerBuilder.class,
		HtmlParserBuilder.class,
		HtmlSchemaSerializerBuilder.class,
		HtmlSerializerBuilder.class,
		HtmlStrippedDocSerializerBuilder.class,
		InputStreamParserBuilder.class,
		JsonParserBuilder.class,
		JsonSchemaGeneratorBuilder.class,
		JsonSchemaSerializerBuilder.class,
		JsonSerializerBuilder.class,
		JsoParserBuilder.class,
		JsoSerializerBuilder.class,
		MockRestClientBuilder.class,
		MsgPackParserBuilder.class,
		MsgPackSerializerBuilder.class,
		OpenApiParserBuilder.class,
		OpenApiSerializerBuilder.class,
		OutputStreamSerializerBuilder.class,
		ParserBuilder.class,
		ParserGroupBuilder.class,
		PlainTextParserBuilder.class,
		PlainTextSerializerBuilder.class,
		RdfParserBuilder.class,
		RdfSerializerBuilder.class,
		ReaderParserBuilder.class,
		RestClientBuilder.class,
		RestContextBuilder.class,
		RestMethodContextBuilder.class,
		SerializerBuilder.class,
		SerializerGroupBuilder.class,
		SimpleJsonParserBuilder.class,
		SimpleJsonSerializerBuilder.class,
		SoapXmlSerializerBuilder.class,
		UonParserBuilder.class,
		UonSerializerBuilder.class,
		UrlEncodingParserBuilder.class,
		UrlEncodingSerializerBuilder.class,
		WriterSerializerBuilder.class,
		XmlParserBuilder.class,
		XmlSerializerBuilder.class,
		BadRequest.class,
		Conflict.class,
		ExpectationFailed.class,
		FailedDependency.class,
		Forbidden.class,
		Gone.class,
		HttpException.class,
		HttpVersionNotSupported.class,
		InsufficientStorage.class,
		InternalServerError.class,
		LengthRequired.class,
		Locked.class,
		LoopDetected.class,
		MethodNotAllowed.class,
		MisdirectedRequest.class,
		NetworkAuthenticationRequired.class,
		NotAcceptable.class,
		NotExtended.class,
		NotFound.class,
		NotImplemented.class,
		PayloadTooLarge.class,
		PreconditionFailed.class,
		PreconditionRequired.class,
		RangeNotSatisfiable.class,
		RequestHeaderFieldsTooLarge.class,
		ServiceUnavailable.class,
		TooManyRequests.class,
		Unauthorized.class,
		UnavailableForLegalReasons.class,
		UnprocessableEntity.class,
		UnsupportedMediaType.class,
		UpgradeRequired.class,
		UriTooLong.class,
		VariantAlsoNegotiates.class,
		Accepted.class,
		AlreadyReported.class,
		Continue.class,
		Created.class,
		EarlyHints.class,
		Found.class,
		HttpResponse.class,
		IMUsed.class,
		MovedPermanently.class,
		MultipleChoices.class,
		MultiStatus.class,
		NoContent.class,
		NonAuthoritiveInformation.class,
		NotModified.class,
		Ok.class,
		PartialContent.class,
		PermanentRedirect.class,
		Processing.class,
		ResetContent.class,
		SeeOther.class,
		SwitchingProtocols.class,
		TemporaryRedirect.class,
		UseProxy.class
	};

	private static String[] SOURCE_PATHS = {
		"juneau-core/juneau-config",
		"juneau-core/juneau-dto",
		"juneau-core/juneau-marshall",
		"juneau-core/juneau-marshall-rdf",
		"juneau-rest/juneau-rest-client",
		"juneau-rest/juneau-rest-server",
		"juneau-rest/juneau-rest-mock",
	};

	public static void main(String[] args) throws Exception {
		Map<Class<?>, Set<Method>> configMethods = new HashMap<>();

		for (Class<?> c : classes) {
			Set<Method> s = new TreeSet<>(new MethodComparator());
			for (Method m : c.getDeclaredMethods()) {
				if (m.getAnnotation(ConfigurationProperty.class) != null) {
					s.add(m);
				}
			}
			configMethods.put(c, s);
		}

		for (Class<?> c : classes) {
			File f = findClassFile(c);
			System.err.println("Processing " + f.getName());
			String s = IOUtils.read(f);

			int i1 = s.indexOf("<CONFIGURATION-PROPERTIES>"), i2 = s.indexOf("</CONFIGURATION-PROPERTIES>");
			String cpSection = null;
			if (i1 != -1 && i2 != -1) {
				cpSection = s.substring(i1+26, i2);
			} else {
				System.err.println("...skipped " + f.getName());
				continue;
			}

			Map<String,String> docs = Stream.of(cpSection.split("[\n]{2,}"))
				.map(x -> x.trim())
				.filter(x -> x.startsWith("/*"))
				.map(x -> x.split("(?s)\\*\\/.*public"))
				.collect(Collectors.toMap(x -> ("\n\tpublic"+x[1].substring(0, x[1].indexOf("\n"))), x -> ("\t"+x[0]+"*/\n")));

			StringBuilder sb = new StringBuilder();
			for (ClassInfo pc : ClassInfo.of(c).getParentsParentFirst()) {
				Class<?> pcc = pc.inner();
				if (pcc != c) {
					Set<Method> ms = configMethods.get(pcc);
					if (ms != null) {
						for (Method m : ms) {

							// Don't render deprecated methods.
							if (m.getAnnotation(Deprecated.class) != null)
								continue;

							StringBuilder sigLine = new StringBuilder();
							sigLine.append("\n\tpublic ");
							if (m.getTypeParameters().length > 0)
								sigLine.append("<").append(Arrays.asList(m.getTypeParameters()).stream().map(x -> x.getName()).collect(Collectors.joining(", "))).append("> ");
							sigLine.append(c.getSimpleName()).append(" ").append(m.getName()).append("(").append(getArgs(m)).append(") ");
							if ( m.getExceptionTypes().length > 0)
								sigLine.append("throws ").append(Arrays.asList(m.getExceptionTypes()).stream().map(x -> x.getSimpleName()).collect(Collectors.joining(", ")));
							sigLine.append("{");
							String sigLine2 = sigLine.toString();

							sb.append("\n\n");

							// Overridden javadocs
							String javadoc = docs.get(sigLine2);
							if (javadoc != null) {
								sb.append(javadoc);
							}

							// Line 1
							sb.append("\t")
								.append(m.getAnnotation(Deprecated.class) == null ? "" : "@Deprecated ")
								.append("@Override /* GENERATED - ").append(pcc.getSimpleName()).append(" */")
							;

							if (m.isVarArgs()) {
								Type t = m.getParameters()[m.getParameterCount()-1].getParameterizedType();
								if (t.toString().contains(" extends ")) {
									sb.append("\n\t")
										.append("@SuppressWarnings(\"unchecked\")");
									;
								}
							}

							// Line 2
							sb.append(sigLine2);

							// Body
							sb.append("\n\t\tsuper.").append(m.getName()).append("(").append(getArgVals(m)).append(");");
							sb.append("\n\t\treturn this;");
							sb.append("\n\t}");

						}
					} else if (pc.isAny(Throwable.class, RuntimeException.class, Exception.class)) {
						// Ignore
					} else {
						System.err.println(pc.inner().getSimpleName() + " not found.");
					}
				}
			}

			s = s.substring(0, i1+26) + sb.toString() + "\n\n\t// " + s.substring(i2);
			IOUtils.write(f, new StringReader(s));
		}

		System.err.println("DONE");
	}

	private static String getArgs(Method m) {
		StringBuilder sb = new StringBuilder();
		for (Parameter p : m.getParameters()) {
			if (sb.length() > 0)
				sb.append(", ");
			ClassInfo pi = ClassInfo.of(p.getParameterizedType());
			if (p.isVarArgs())
				sb.append(pi.getShortName().replace("[]","...") + p.getName());
			else
				sb.append(pi.getShortName() + " " + p.getName());
		}
		return sb.toString();
	}

	private static String getArgVals(Method m) {
		StringBuilder sb = new StringBuilder();
		for (Parameter p : m.getParameters()) {
			if (sb.length() > 0)
				sb.append(", ");
			sb.append(p.getName());
		}
		return sb.toString();
	}




	private static File findClassFile(Class<?> c) throws IOException {
		String path = c.getName().replace('.', '/') + ".java";
		for (String sp : SOURCE_PATHS) {
			File f = new File("../../"+sp+"/src/main/java/" + path);
			if (f.exists())
				return f;
		}
		throw new RuntimeException("Could not find source for class " + c.getName());
	}

	private static class MethodComparator implements Comparator<Method> {

		@Override
		public int compare(Method m1, Method m2) {
			int i = m1.getName().compareTo(m2.getName());
			if (i == 0)
				i = Integer.compare(m1.getParameterCount(), m2.getParameterCount());
			if (i == 0 && m1.getParameterCount() > 0)
				i = m1.getParameterTypes()[0].getName().compareTo(m2.getParameterTypes()[0].getName());
			if (i == 0 && m1.getParameterCount() > 1)
				i = m1.getParameterTypes()[1].getName().compareTo(m2.getParameterTypes()[1].getName());
			return i;
		}

	}

}
