/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server;

import static com.ibm.juno.server.RestUtils.*;

import java.util.*;
import java.util.regex.*;

import com.ibm.juno.core.utils.*;
import com.ibm.juno.server.annotation.*;

/**
 * A parsed path pattern constructed from a {@link RestMethod#path()} value.
 * <p>
 * Handles aspects of matching and precedence ordering.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class UrlPathPattern implements Comparable<UrlPathPattern> {
	private Pattern pattern;
	String patternString;
	private boolean isOnlyDotAll, isDotAll;
	String[] vars = new String[0];

	/**
	 * Constructor.
	 *
	 * @param patternString The raw pattern string from the {@link RestMethod#path()} annotation.
	 */
	public UrlPathPattern(String patternString) {
		this.patternString = patternString;
		if (! StringUtils.startsWith(patternString, '/'))
			patternString = '/' + patternString;
		if (patternString.equals("/*")) {
			isOnlyDotAll = true;
			return;
		}
		if (patternString.endsWith("/*"))
			isDotAll = true;

		// Find all {xxx} variables.
		Pattern p = Pattern.compile("\\{([^\\}]+)\\}");
		List<String> vl = new LinkedList<String>();
		Matcher m = p.matcher(patternString);
		while (m.find())
			vl.add(m.group(1));
		this.vars = vl.toArray(new String[vl.size()]);

		patternString = patternString.replaceAll("\\{[^\\}]+\\}", "([^\\/]+)");
		patternString = patternString.replaceAll("\\/\\*$", "((?:)|(?:\\/.*))");
		pattern = Pattern.compile(patternString);
	}

	/**
	 * Returns a non-<jk>null</jk> value if the specified path matches this pattern.
	 *
	 * @param path The path to match against.
	 * @return An array of values matched against <js>"{var}"</js> variable in the pattern,
	 * 	or an empty array if the pattern matched but no vars were present, or <jk>null</jk>
	 * 	if the specified path didn't match the pattern.
	 */
	protected String[] match(String path) {

		if (isOnlyDotAll) {
			// Remainder always gets leading slash trimmed.
			if (path != null)
				path = path.substring(1);
			return new String[]{path};
		}

		if (path == null)
			return (patternString.equals("/") ? new String[]{} : null);

		Matcher m = pattern.matcher(path);
		if (! m.matches())
			return null;

		int len = m.groupCount();
		String[] v = new String[len];

		for (int i = 0; i < len; i++) {
			if (isDotAll && i == len-1)
				v[i] = m.group(i+1).isEmpty() ? null : m.group(i+1).substring(1);
			else
			v[i] = decode(m.group(i+1));
		}

		return v;
	}

	/**
	 * Comparator for this object.
	 * The comparator is designed to order URL pattern from most-specific to least-specific.
	 * For example, the following patterns would be ordered as follows:
	 * <ol>
	 * 	<li><code>/foo/bar</code>
	 * 	<li><code>/foo/bar/*</code>
	 * 	<li><code>/foo/{id}/bar</code>
	 * 	<li><code>/foo/{id}/bar/*</code>
	 * 	<li><code>/foo/{id}</code>
	 * 	<li><code>/foo/{id}/*</code>
	 * 	<li><code>/foo</code>
	 * 	<li><code>/foo/*</code>
	 * </ol>
	 */
	@Override /* Comparable */
	public int compareTo(UrlPathPattern o) {
		String s1 = patternString.replaceAll("\\{[^\\}]+\\}", ".").replaceAll("\\w+", "X").replaceAll("\\.", "W");
		String s2 = o.patternString.replaceAll("\\{[^\\}]+\\}", ".").replaceAll("\\w+", "X").replaceAll("\\.", "W");
		if (s1.isEmpty())
			s1 = "+";
		if (s2.isEmpty())
			s2 = "+";
		if (! s1.endsWith("/*"))
			s1 = s1 + "/W";
		if (! s2.endsWith("/*"))
			s2 = s2 + "/W";
		int c = s2.compareTo(s1);
		if (c == 0)
			return o.toRegEx().compareTo(toRegEx());
		return c;
	}

	@Override /* Object */
	public boolean equals(Object o) {
		if (! (o instanceof UrlPathPattern))
			return false;
		return (compareTo((UrlPathPattern)o) == 0);
	}

	@Override /* Object */
	public int hashCode() {
		return super.hashCode();
	}

	@Override /* Object */
	public String toString() {
		return patternString;
	}

	/**
	 * Returns this path pattern as the compiled regular expression.
	 * Useful for debugging.
	 *
	 * @return The path pattern.
	 */
	public String toRegEx() {
		return isOnlyDotAll ? "*" : pattern.pattern();
	}
}