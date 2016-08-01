/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core;

/**
 * Converts property names to dashed-lower-case format.
 * <p>
 * 	Examples:
 * <ul>
 * 	<li><js>"fooBar"</js> -&gt; <js>"foo-bar"</js>
 * 	<li><js>"fooBarURL"</js> -&gt; <js>"foo-bar-url"</js>
 * 	<li><js>"FooBarURL"</js> -&gt; <js>"foo-bar-url"</js>
 * </ul>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class PropertyNamerDashedLC implements PropertyNamer {

	@Override /* PropertyNamer */
	public String getPropertyName(String name) {
		if (name == null || name.isEmpty())
			return name;

		int numUCs = 0;
		boolean isPrevUC = Character.isUpperCase(name.charAt(0));
		for (int i = 1; i < name.length(); i++) {
			char c = name.charAt(i);
			if (Character.isUpperCase(c)) {
				if (! isPrevUC)
					numUCs++;
				isPrevUC = true;
			} else {
				isPrevUC = false;
			}
		}

		char[] name2 = new char[name.length() + numUCs];
		isPrevUC = Character.isUpperCase(name.charAt(0));
		name2[0] = Character.toLowerCase(name.charAt(0));
		int ni = 0;
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if (Character.isUpperCase(c)) {
				if (! isPrevUC)
					name2[ni++] = '-';
				isPrevUC = true;
				name2[ni++] = Character.toLowerCase(c);
			} else {
				isPrevUC = false;
				name2[ni++] = c;
			}
		}

		return new String(name2);
	}
}
