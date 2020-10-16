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
import org.apache.juneau.annotation.*;
import org.apache.juneau.assertions.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.config.*;
import org.apache.juneau.config.store.*;
import org.apache.juneau.csv.*;
import org.apache.juneau.csv.annotation.*;
import org.apache.juneau.html.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.exception.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.jena.*;
import org.apache.juneau.jena.annotation.*;
import org.apache.juneau.jso.*;
import org.apache.juneau.jso.annotation.*;
import org.apache.juneau.json.*;
import org.apache.juneau.json.annotation.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.jsonschema.annotation.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.msgpack.annotation.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.oapi.annotation.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.plaintext.*;
import org.apache.juneau.plaintext.annotation.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.soap.*;
import org.apache.juneau.soap.annotation.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.uon.annotation.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.urlencoding.annotation.*;
import org.apache.juneau.xml.*;
import org.apache.juneau.xml.annotation.*;

public class ConfigurablePropertyCodeGenerator {

	static Class<?>[] classes = new Class<?>[]{
		BasicRuntimeException.class,
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
		MockRestClientBuilder.class,
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
		UseProxy.class,
		SessionArgs.class,
		BeanSessionArgs.class,
		SerializerSessionArgs.class,
		ParserSessionArgs.class,

		Assertion.class,
		FluentAssertion.class,
		FluentDateAssertion.class,
		FluentIntegerAssertion.class,
		FluentLongAssertion.class,
		FluentStringAssertion.class,
		FluentThrowableAssertion.class,
		FluentObjectAssertion.class,
		FluentComparableAssertion.class,
		FluentArrayAssertion.class,
		FluentCollectionAssertion.class,
		FluentListAssertion.class,
		FluentZonedDateTimeAssertion.class,
		FluentBooleanAssertion.class,

		DateAssertion.class,
		IntegerAssertion.class,
		LongAssertion.class,
		StringAssertion.class,
		ThrowableAssertion.class,
		ObjectAssertion.class,
		ComparableAssertion.class,
		ArrayAssertion.class,
		CollectionAssertion.class,
		ListAssertion.class,
		ZonedDateTimeAssertion.class,
		BooleanAssertion.class,

		BasicHeader.class,
		BasicNameValuePair.class,
		BasicHttpEntity.class,
		BasicHttpResource.class,
		SerializedNameValuePair.class,
		SerializedHeader.class,
		SerializedHttpEntity.class,

		ExecutableInfo.class,
		ConstructorInfo.class,
		MethodInfo.class,

		TargetedAnnotation.class,
		TargetedAnnotation.OnClass.class,
		TargetedAnnotation.OnClassMethodField.class,
		TargetedAnnotation.OnConstructor.class,
		TargetedAnnotation.OnMethodField.class,
		TargetedAnnotation.OnClassMethodFieldConstructor.class,
		BeanAnnotation.class,
		BeancAnnotation.class,
		BeanIgnoreAnnotation.class,
		BeanpAnnotation.class,
		ExampleAnnotation.class,
		NamePropertyAnnotation.class,
		ParentPropertyAnnotation.class,
		SwapAnnotation.class,
		UriAnnotation.class,
		CsvAnnotation.class,
		HtmlAnnotation.class,
		HtmlLinkAnnotation.class,
		JsoAnnotation.class,
		JsonAnnotation.class,
		SchemaAnnotation.class,
		MsgPackAnnotation.class,
		OpenApiAnnotation.class,
		PlainTextAnnotation.class,
		SoapXmlAnnotation.class,
		UonAnnotation.class,
		UrlEncodingAnnotation.class,
		XmlAnnotation.class,
		RdfAnnotation.class

	};

	static Set<Class<?>> ignoreClasses = ASet.of(
		org.apache.http.entity.AbstractHttpEntity.class,
		org.apache.http.entity.BasicHttpEntity.class
	);

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
				if (m.getAnnotation(FluentSetter.class) != null) {
					s.add(m);
				}
			}
			configMethods.put(c, s);
		}

		for (Class<?> c : classes) {
			File f = findClassFile(c);
			if (f == null)
				continue;
			System.out.println("Processing " + f.getName());
			String s = IOUtils.read(f);

			int i1 = s.indexOf("<FluentSetters>"), i2 = s.indexOf("</FluentSetters>");
			String cpSection = null;
			if (i1 != -1 && i2 != -1) {
				cpSection = s.substring(i1+15, i2);
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
			ClassInfo ci = ClassInfo.of(c);
			String cName = ci.getSimpleName();
			Set<String> ignore = ASet.of();
			for (FluentSetters fs : ci.getAnnotations(FluentSetters.class)) {
				if (! fs.returns().isEmpty())
					cName = fs.returns();
				for (String i : fs.ignore())
					ignore.add(i);
			}

			for (ClassInfo pc : ClassInfo.of(c).getParentsParentFirst()) {
				Class<?> pcc = pc.inner();
				if (pcc != c) {
					Set<Method> ms = configMethods.get(pcc);
					if (ms != null) {
						for (Method m : ms) {

							// Don't render deprecated methods.
							if (m.getAnnotation(Deprecated.class) != null)
								continue;

							String mSig = new StringBuilder(m.getName()).append("(").append(getArgs(m)).append(")").toString();

							// Don't render ignored methods.
							if (ignore.contains(m.getName()) || ignore.contains(mSig))
								continue;

							StringBuilder sigLine = new StringBuilder();
							sigLine.append("\n\tpublic ");
							if (m.getTypeParameters().length > 0)
								sigLine.append("<").append(Arrays.asList(m.getTypeParameters()).stream().map(x -> x.getName()).collect(Collectors.joining(", "))).append("> ");
							sigLine.append(cName).append(" ").append(mSig).append(" ");
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
						if (! ignoreClasses.contains(pc.inner()))
							System.err.println(pc.inner().getSimpleName() + " not found in " + c.getName());
					}
				}
			}

			s = s.substring(0, i1+15) + sb.toString() + "\n\n\t// " + s.substring(i2);
			IOUtils.write(f, new StringReader(s));
		}

		System.out.println("DONE");
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
		return null;
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
