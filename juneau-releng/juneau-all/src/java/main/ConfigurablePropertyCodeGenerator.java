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
		ContextBuilder.class,
		BeanContextBuilder.class,
		BeanTraverseBuilder.class,
		JsonSchemaGeneratorBuilder.class,
		SerializerBuilder.class,
		OutputStreamSerializerBuilder.class,
		JsoSerializerBuilder.class,
		MsgPackSerializerBuilder.class,
		WriterSerializerBuilder.class,
		CsvSerializerBuilder.class,
		JsonSerializerBuilder.class,
		JsonSchemaSerializerBuilder.class,
		PlainTextSerializerBuilder.class,
		RdfSerializerBuilder.class,
		UrlEncodingSerializerBuilder.class,
		UonSerializerBuilder.class,
		OpenApiSerializerBuilder.class,
		XmlSerializerBuilder.class,
		HtmlSerializerBuilder.class,
		SoapXmlSerializerBuilder.class,
		HtmlSchemaSerializerBuilder.class,
		HtmlDocSerializerBuilder.class,
		HtmlStrippedDocSerializerBuilder.class,
		SerializerGroupBuilder.class,
		ParserBuilder.class,
		InputStreamParserBuilder.class,
		JsoParserBuilder.class,
		UonParserBuilder.class,
		MsgPackParserBuilder.class,
		ReaderParserBuilder.class,
		CsvParserBuilder.class,
		JsonParserBuilder.class,
		PlainTextParserBuilder.class,
		RdfParserBuilder.class,
		OpenApiParserBuilder.class,
		UrlEncodingParserBuilder.class,
		XmlParserBuilder.class,
		HtmlParserBuilder.class,
		ParserGroupBuilder.class,
		RestClientBuilder.class,
		MockRestClient.class,
		RestContextBuilder.class,
		RestMethodContextBuilder.class,
		ConfigBuilder.class,
		ConfigStoreBuilder.class,
		ConfigClasspathStoreBuilder.class,
		ConfigFileStoreBuilder.class,
		ConfigMemoryStoreBuilder.class
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

			StringBuilder sb = new StringBuilder();
			for (ClassInfo pc : ClassInfo.of(c).getParentsParentFirst()) {
				Class<?> pcc = pc.inner();
				if (pcc != c) {
					Set<Method> ms = configMethods.get(pcc);
					if (ms != null) {
						for (Method m : ms) {

							// Line 1
							sb.append("\n\n\t")
								.append(m.getAnnotation(Deprecated.class) == null ? "" : "@Deprecated ")
								.append("@Override /* GENERATED - ").append(pcc.getSimpleName()).append(" */")
							;

							// Line 2
							sb.append("\n\tpublic ");
							if (m.getTypeParameters().length > 0)
								sb.append("<").append(Arrays.asList(m.getTypeParameters()).stream().map(x -> x.getName()).collect(Collectors.joining(", "))).append("> ");
							sb.append(c.getSimpleName()).append(" ").append(m.getName()).append("(").append(getArgs(m)).append(") ");
							if ( m.getExceptionTypes().length > 0)
								sb.append("throws ").append(Arrays.asList(m.getExceptionTypes()).stream().map(x -> x.getSimpleName()).collect(Collectors.joining(", ")));
							sb.append("{");

							// Body
							sb.append("\n\t\tsuper.").append(m.getName()).append("(").append(getArgVals(m)).append(");");
							sb.append("\n\t\treturn this;");
							sb.append("\n\t}");
						}
					} else {
						System.err.println(pc.inner().getSimpleName() + " not found.");
					}
				}
			}

			int i1 = s.indexOf("<CONFIGURATION-PROPERTIES>"), i2 = s.indexOf("</CONFIGURATION-PROPERTIES>");
			if (i1 != -1 && i2 != -1) {
				s = s.substring(0, i1+26) + sb.toString() + "\n\n\t// " + s.substring(i2);
				//System.err.println(s);
				//throw new RuntimeException();
				IOUtils.write(f, new StringReader(s));
			} else {
				System.err.println("...skipped " + f.getName());
			}
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
