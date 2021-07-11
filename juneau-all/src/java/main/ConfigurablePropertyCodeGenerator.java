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

import static org.apache.juneau.internal.IOUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.reflect.*;

public class ConfigurablePropertyCodeGenerator {

	static Set<Class<?>> ignoreClasses = ASet.of(
		org.apache.http.entity.AbstractHttpEntity.class,
		org.apache.http.entity.BasicHttpEntity.class,
		org.apache.http.message.AbstractHttpMessage.class,
		org.apache.http.message.BasicHttpResponse.class
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

		Map<Class<?>,File> classMap = new LinkedHashMap<>();
		for (String sp : SOURCE_PATHS) {
			File f = new File("../"+sp+"/src/main/java");
			Path p = f.toPath();
			try (Stream<Path> walkStream = Files.walk(p)) {
				walkStream
					.filter(x -> x.toFile().isFile())
					.map(x -> x.toFile())
					.filter(x -> x.getName().endsWith(".java"))
					.filter(x -> hasFluentSetters(x))
					.forEach(x -> classMap.put(toClass(x), x));
			}
		}

		System.out.println("Found " + classMap.size() + " classes with fluent setters.");

		for (Class<?> c : classMap.keySet()) {
			System.out.println("Seaching " + c.getName());
			Set<Method> s = new TreeSet<>(new MethodComparator());
			for (Method m : c.getDeclaredMethods()) {
				if (m.getAnnotation(FluentSetter.class) != null) {
					s.add(m);
					System.out.println("\tFound: " + m.getName());
				}
			}
			configMethods.put(c, s);
		}

		for (Map.Entry<Class<?>,File> e : classMap.entrySet()) {
			Class<?> c = e.getKey();
			File f = e.getValue();

			if (! c.isAnnotationPresent(FluentSetters.class))
				System.err.println("@FluentSetters not present on class " + c.getName());

			System.out.println("Processing " + f.getName());
			String s = read(f);

			int i1 = s.indexOf("<FluentSetters>"), i2 = s.indexOf("</FluentSetters>");
			String cpSection = null;
			if (i1 != -1 && i2 != -1) {
				cpSection = s.substring(i1+15, i2);
			} else {
				System.err.println("...<FluentSetters> not found: " + f.getName());
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
			pipe(new StringReader(s), f);
		}

		System.out.println("DONE");
	}

	private static boolean hasFluentSetters(File f) {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)))) {
			String line;
			while ((line = br.readLine()) != null)
				if (line.contains("@FluentSetters"))
					return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	private static Class<?> toClass(File f) {
		try {
			String n = f.getAbsolutePath();
			return Class.forName(n.substring(n.indexOf("/src/main/java/")+15).replace(".java","").replace('/','.'));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
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
