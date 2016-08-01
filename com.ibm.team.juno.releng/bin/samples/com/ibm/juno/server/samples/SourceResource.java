/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.server.samples;

import static com.ibm.juno.core.html.HtmlDocSerializerProperties.*;

import java.io.*;
import java.util.*;

import com.ibm.juno.core.html.annotation.*;
import com.ibm.juno.core.utils.*;
import com.ibm.juno.microservice.*;
import com.ibm.juno.server.annotation.*;

/**
 * Servlet for viewing source code on classes whose Java files are present on the classpath.
 * <p>
 * This class is by no means perfect but is pretty much the best you can get using only regular expression matching.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@SuppressWarnings("serial")
@RestResource(
	path="/source",
	messages="nls/SourceResource",
	properties={
		@Property(name=HTMLDOC_title, value="Source code viewer"),
		@Property(name=HTMLDOC_cssImports, value="$R{servletURI}/htdocs/code-highlighting.css"),
		@Property(name=HTMLDOC_links, value="{up:'$R{requestParentURI}',options:'$R{servletURI}?method=OPTIONS',source:'$R{servletParentURI}/source?classes=(com.ibm.juno.server.samples.SourceResource)'}"),
	}
)
public class SourceResource extends Resource {

	/** View source on the specified classes. */
	@RestMethod(name="GET", path="/")
	public Object getSource(@Param("classes") String[] classes) throws Exception {
		if (classes == null)
			return "Specify classes using ?classes=(class1,class2,....) attribute";
		List<Object> l = new LinkedList<Object>();
		for (String c : classes) {
			try {
				l.add(new Source(Class.forName(c)));
			} catch (ClassNotFoundException e) {
				l.add("Class " + c + " not found");
			} catch (Exception e) {
				l.add(e.getLocalizedMessage());
			}
		}
		return l;
	}

	/**
	 * POJO that allows us to serialize HTML directly to the output.
	 */
	@Html(asPlainText=true)
	public static class Source {
		private Class<?> c;
		private Source(Class<?> c) {
			this.c = c;
		}
		@Override /* Object */
		public String toString() {
			String filename = c.getSimpleName() + ".java";
			InputStream is = c.getResourceAsStream('/' + c.getPackage().getName().replace('.','/') + '/' + filename);
			if (is == null)
				return "Source for class " + c.getName() + " not found";
			StringBuilder sb = new StringBuilder();
			try {
					sb.append("<h3>").append(c.getSimpleName()).append(".java").append("</h3>");
					sb.append("<p class='bcode'>");
					sb.append(highlight(IOUtils.read(is), "java"));
					sb.append("</p>");
			} catch (Exception e) {
				return e.getLocalizedMessage();
			}
			return sb.toString();
		}
	}

	public static String highlight(String code, String lang) throws Exception {
		if (lang.equalsIgnoreCase("xml")) {
			code = code.replaceAll("&", "&amp;");
			code = code.replaceAll("<", "&lt;");
			code = code.replaceAll(">", "&gt;");
			code = code.replaceAll("(&lt;[^\\s&]+&gt;)", "<xt>$1</xt>");
			code = code.replaceAll("(&lt;[^\\s&]+)(\\s)", "<xt>$1</xt>$2");
			code = code.replaceAll("(['\"])(/?&gt;)", "$1<xt>$2</xt>");
			code = code.replaceAll("([\\S]+)=", "<xa>$1</xa>=");
			code = code.replaceAll("=(['\"][^'\"]+['\"])", "=<xs>$1</xs>");
		} else if (lang.equalsIgnoreCase("java")) {
			code = code.replaceAll("&", "&amp;");
			code = code.replaceAll("<", "&lt;");
			code = code.replaceAll(">", "&gt;");
			code = code.replaceAll("(?s)(\\/\\*\\*.*?\\*\\/)", "<jd>$1</jd>"); // javadoc comments
			code = code.replaceAll("(@\\w+)", "<ja>$1</ja>"); // annotations
			code = code.replaceAll("(?s)(?!\\/)(\\/\\*.*?\\*\\/)", "<jc>$1</jc>"); // C style comments
			code = code.replaceAll("(?m)(\\/\\/.*)", "<jc>$1</jc>"); // C++ style comments
			code = code.replaceAll("(?m)('[^'\n]*'|\"[^\"\n]*\")", "<js>$1</js>"); // quotes
			code = code.replaceAll("(?<!@)(import|package|boolean|byte|char|double|float|final|static|transient|synchronized|private|protected|public|int|long|short|abstract|class|interface|extends|implements|null|true|false|void|break|case|catch|continue|default|do|else|finally|for|goto|if|instanceof|native|new|return|super|switch|this|threadsafe|throws|throw|try|while)(?=\\W)", "<jk>$1</jk>"); // quotes
			code = code.replaceAll("<\\/jk>(\\s+)<jk>", "$1"); // quotes
		}
		return code;
	}
}
