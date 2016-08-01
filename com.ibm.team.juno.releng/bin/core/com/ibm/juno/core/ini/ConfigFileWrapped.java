/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.ini;
import static com.ibm.juno.core.utils.ThrowableUtils.*;

import java.io.*;
import java.util.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.core.utils.*;

/**
 * Wraps an instance of {@link ConfigFileImpl} in an interface that will
 * 	automatically replace {@link StringVarResolver} variables.
 * <p>
 * The {@link ConfigFile#getResolving(StringVarResolver)} returns an instance of this class.
 * <p>
 * This class overrides the {@link #getString(String, String)} to resolve string variables.
 * All other method calls are passed through to the inner config file.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class ConfigFileWrapped extends ConfigFile {

	private final ConfigFileImpl cf;
	private final StringVarResolver vr;

	ConfigFileWrapped(ConfigFileImpl cf, StringVarResolver vr) {
		this.cf = cf;
		this.vr = vr;
	}

	@Override /* ConfigFile */
	public void clear() {
		cf.clear();
	}

	@Override /* ConfigFile */
	public boolean containsKey(Object key) {
		return cf.containsKey(key);
	}

	@Override /* ConfigFile */
	public boolean containsValue(Object value) {
		return cf.containsValue(value);
	}

	@Override /* ConfigFile */
	public Set<java.util.Map.Entry<String,Section>> entrySet() {
		return cf.entrySet();
	}

	@Override /* ConfigFile */
	public Section get(Object key) {
		return cf.get(key);
	}

	@Override /* ConfigFile */
	public boolean isEmpty() {
		return cf.isEmpty();
	}

	@Override /* ConfigFile */
	public Set<String> keySet() {
		return cf.keySet();
	}

	@Override /* ConfigFile */
	public Section put(String key, Section value) {
		return cf.put(key, value);
	}

	@Override /* ConfigFile */
	public void putAll(Map<? extends String,? extends Section> map) {
		cf.putAll(map);
	}

	@Override /* ConfigFile */
	public Section remove(Object key) {
		return cf.remove(key);
	}

	@Override /* ConfigFile */
	public int size() {
		return cf.size();
	}

	@Override /* ConfigFile */
	public Collection<Section> values() {
		return cf.values();
	}

	@Override /* ConfigFile */
	public ConfigFile loadIfModified() throws IOException {
		cf.loadIfModified();
		return this;
	}

	@Override /* ConfigFile */
	public ConfigFile load() throws IOException {
		cf.load();
		return this;
	}

	@Override /* ConfigFile */
	public ConfigFile load(Reader r) throws IOException {
		cf.load(r);
		return this;
	}


	@Override /* ConfigFile */
	public boolean isEncoded(String key) {
		return cf.isEncoded(key);
	}

	@Override /* ConfigFile */
	public ConfigFile addLines(String section, String... lines) {
		cf.addLines(section, lines);
		return this;
	}

	@Override /* ConfigFile */
	public ConfigFile addHeaderComments(String section, String... headerComments) {
		cf.addHeaderComments(section, headerComments);
		return this;
	}

	@Override /* ConfigFile */
	public ConfigFile clearHeaderComments(String section) {
		cf.clearHeaderComments(section);
		return this;
	}

	@Override /* ConfigFile */
	public Section getSection(String name) {
		return cf.getSection(name);
	}

	@Override /* ConfigFile */
	public Section getSection(String name, boolean create) {
		return cf.getSection(name, create);
	}

	@Override /* ConfigFile */
	public ConfigFile addSection(String name) {
		cf.addSection(name);
		return this;
	}

	@Override /* ConfigFile */
	public ConfigFile setSection(String name, Map<String,String> contents) {
		cf.setSection(name, contents);
		return this;
	}

	@Override /* ConfigFile */
	public ConfigFile removeSection(String name) {
		cf.removeSection(name);
		return this;
	}

	@Override /* ConfigFile */
	public ConfigFile save() throws IOException {
		cf.save();
		return this;
	}

	@Override /* ConfigFile */
	public ConfigFile serializeTo(Writer out, ConfigFileFormat format) throws IOException {
		cf.serializeTo(out, format);
		return this;
	}

	@Override /* ConfigFile */
	public String toString() {
		return cf.toString();
	}

	@Override /* ConfigFile */
	@SuppressWarnings("hiding")
	public ConfigFile getResolving(StringVarResolver vr) {
		assertFieldNotNull(vr, "vr");
		return new ConfigFileWrapped(cf, vr);
	}

	@Override /* ConfigFile */
	public ConfigFile getResolving() {
		return new ConfigFileWrapped(cf, StringVarResolver.DEFAULT);
	}

	@Override /* ConfigFile */
	public ConfigFile addListener(ConfigFileListener listener) {
		cf.addListener(listener);
		return this;
	}

	@Override /* ConfigFile */
	public Writable toWritable() {
		return cf.toWritable();
	}

	@Override /* ConfigFile */
	public ConfigFile merge(ConfigFile newCf) {
		cf.merge(newCf);
		return this;
	}

	@Override /* ConfigFile */
	protected WriterSerializer getSerializer() throws SerializeException {
		return cf.getSerializer();
	}

	@Override /* ConfigFile */
	protected ReaderParser getParser() throws ParseException {
		return cf.getParser();
	}

	@Override /* ConfigFile */
	public String get(String sectionName, String sectionKey) {
		String s = cf.get(sectionName, sectionKey);
		if (s == null)
			return null;
		return vr.resolve(s);
	}

	@Override /* ConfigFile */
	public String put(String sectionName, String sectionKey, Object value, boolean encoded) {
		return cf.put(sectionName, sectionKey, value, encoded);
	}

	@Override /* ConfigFile */
	public String remove(String sectionName, String sectionKey) {
		return cf.remove(sectionName, sectionKey);
	}

	@Override /* ConfigFile */
	public Set<String> getSectionKeys(String sectionName) {
		return cf.getSectionKeys(sectionName);
	}

	@Override /* ConfigFile */
	protected void readLock() {
		cf.readLock();
	}

	@Override /* ConfigFile */
	protected void readUnlock() {
		cf.readUnlock();
	}
}
