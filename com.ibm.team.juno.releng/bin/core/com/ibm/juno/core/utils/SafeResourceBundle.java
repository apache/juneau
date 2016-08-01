/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.utils;

import java.text.*;
import java.util.*;

/**
 * Wraps a {@link ResourceBundle} to gracefully handle missing bundles and entries.
 * <p>
 * If the bundle isn't found, <code>getString(key)</code> returns <js>"{!!key}"</js>.
 * <p>
 * If the key isn't found, <code>getString(key)</code> returns <js>"{!key}"</js>.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class SafeResourceBundle extends ResourceBundle {

	private ResourceBundle rb;
	private String className = null;
	Map<String,String> classPrefixedKeys = new HashMap<String,String>();

	SafeResourceBundle() {}

	/**
	 * Constructor.
	 *
	 * @param rb The resource bundle to wrap.  Can be <jk>null</jk> to indicate a missing bundle.
	 * @param forClass The class using this resource bundle.
	 */
	public SafeResourceBundle(ResourceBundle rb, Class<?> forClass) {
		this.rb = rb;
		if (forClass != null)
			className = forClass.getSimpleName();
		if (rb != null) {
			String c = className + '.';
			for (Enumeration<String> e = getKeys(); e.hasMoreElements();) {
				String key = e.nextElement();
				if (key.startsWith(c))
					classPrefixedKeys.put(key.substring(className.length() + 1), key);
			}
		}
	}

	@Override /* ResourceBundle */
	public boolean containsKey(String key) {
		return rb != null && (rb.containsKey(key) || classPrefixedKeys.containsKey(key));
	}

	/**
	 * Similar to {@link ResourceBundle#getString(String)} except allows you to pass in {@link MessageFormat} objects.
	 *
	 * @param key The resource bundle key.
	 * @param args Optional variable replacement arguments.
	 * @return The resolved value.  Never <jk>null</jk>.  <js>"{!!key}"</j> if the bundle is missing.  <js>"{!key}"</j> if the key is missing.
	 */
	public String getString(String key, Object...args) {
		if (rb == null)
			return "{!!"+key+"}";
		if (! containsKey(key))
			return "{!" + key + "}";
		String val = getString(key);
		if (args.length > 0)
			return MessageFormat.format(val, args);
		return val;
	}

	/**
	 * Looks for all the specified keys in the resource bundle and returns the first value that exists.
	 *
	 * @param keys
	 * @return The resolved value, or <jk>null</jk> if no value is found or the resource bundle is missing.
	 */
	public String findFirstString(String...keys) {
		if (rb == null)
			return null;
		for (String k : keys) {
			if (containsKey(k))
				return getString(k);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override /* ResourceBundle */
	public Set<String> keySet() {
		if (rb == null)
			return Collections.emptySet();
		return new MultiSet<String>(rb.keySet(), classPrefixedKeys.keySet()) ;
	}

	/**
	 * Returns all keys in this resource bundle with the specified prefix.
	 *
	 * @param prefix The prefix.
	 * @return The set of all keys in the resource bundle with the prefix.
	 */
	public Set<String> keySet(String prefix) {
		Set<String> set = new HashSet<String>();
		for (String s : keySet()) {
			if (s.equals(prefix) || (s.startsWith(prefix) && s.charAt(prefix.length()) == '.'))
				set.add(s);
		}
		return set;
	}

	@Override /* ResourceBundle */
	public Enumeration<String> getKeys() {
		if (rb == null)
			return new Vector<String>(0).elements();
		return rb.getKeys();
	}

	@Override /* ResourceBundle */
	protected Object handleGetObject(String key) {
		if (rb == null)
			return "{!!"+key+"}";
		try {
			if (classPrefixedKeys.containsKey(key))
				key = classPrefixedKeys.get(key);
			return rb.getObject(key);
		} catch (Exception e) {
			return "{!"+key+"}";
		}
	}
}