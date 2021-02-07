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
package org.apache.juneau.parser;

import java.nio.charset.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;

/**
 * Subclass of {@link Parser} for characters-based parsers.
 *
 * <h5 class='topic'>Description</h5>
 *
 * This class is typically the parent class of all character-based parsers.
 * It has 1 abstract method to implement...
 * <ul>
 * 	<li><c>parse(ParserSession, ClassMeta)</c>
 * </ul>
 */
@ConfigurableContext
public abstract class ReaderParser extends Parser {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	static final String PREFIX = "ReaderParser";

	/**
	 * Configuration property:  File charset.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.parser.ReaderParser#RPARSER_fileCharset RPARSER_fileCharset}
	 * 	<li><b>Name:</b>  <js>"ReaderParser.fileCharset.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>System property:</b>  <c>ReaderParser.fileCharset</c>
	 * 	<li><b>Environment variable:</b>  <c>READERPARSER_FILECHARSET</c>
	 * 	<li><b>Default:</b>  <js>"DEFAULT"</js>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.parser.annotation.ParserConfig#fileCharset()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.parser.ReaderParserBuilder#fileCharset(Charset)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 *
	 * <p>
	 * The character set to use for reading <c>Files</c> from the file system.
	 *
	 * <p>
	 * Used when passing in files to {@link Parser#parse(Object, Class)}.
	 *
	 * <p>
	 * <js>"DEFAULT"</js> can be used to indicate the JVM default file system charset.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a parser that reads UTF-8 files.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.fileCharset(<js>"UTF-8"</js>)
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>RPARSER_fileCharset</jsf>, <js>"UTF-8"</js>)
	 * 		.build();
	 *
	 * 	<jc>// Use it to read a UTF-8 encoded file.</jc>
	 * 	MyBean myBean = p.parse(<jk>new</jk> File(<js>"MyBean.txt"</js>), MyBean.<jk>class</jk>);
	 * </p>
	 */
	public static final String RPARSER_fileCharset = PREFIX + ".fileCharset.s";

	/**
	 * Configuration property:  Input stream charset.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.parser.ReaderParser#RPARSER_streamCharset RPARSER_streamCharset}
	 * 	<li><b>Name:</b>  <js>"ReaderParser.streamCharset.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>System property:</b>  <c>ReaderParser.streamCharset</c>
	 * 	<li><b>Environment variable:</b>  <c>READERPARSER_STREAMCHARSET</c>
	 * 	<li><b>Default:</b>  <js>"UTF-8"</js>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.parser.annotation.ParserConfig#streamCharset()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.parser.ReaderParserBuilder#streamCharset(Charset)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 *
	 * <p>
	 * The character set to use for converting <c>InputStreams</c> and byte arrays to readers.
	 *
	 * <p>
	 * Used when passing in input streams and byte arrays to {@link Parser#parse(Object, Class)}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a parser that reads UTF-8 files.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.streamCharset(Charset.<jsm>forName</jsm>(<js>"UTF-8"</js>))
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>RPARSER_streamCharset</jsf>, <js>"UTF-8"</js>)
	 * 		.build();
	 *
	 * 	<jc>// Use it to read a UTF-8 encoded input stream.</jc>
	 * 	MyBean myBean = p.parse(<jk>new</jk> FileInputStream(<js>"MyBean.txt"</js>), MyBean.<jk>class</jk>);
	 * </p>
	 */
	public static final String RPARSER_streamCharset = PREFIX + ".streamCharset.s";

	static final ReaderParser DEFAULT = new ReaderParser(PropertyStore.create().build(), "") {
		@Override
		public ReaderParserSession createSession(ParserSessionArgs args) {
			throw new NoSuchMethodError();
		}
	};

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final Charset streamCharset, fileCharset;

	/**
	 * Constructor.
	 *
	 * @param ps The property store containing all the settings for this object.
	 * @param consumes The list of media types that this parser consumes (e.g. <js>"application/json"</js>, <js>"*&#8203;/json"</js>).
	 */
	protected ReaderParser(PropertyStore ps, String...consumes) {
		super(ps, consumes);

		streamCharset = ps.get(RPARSER_streamCharset, Charset.class).orElse(IOUtils.UTF8);
		fileCharset = ps.get(RPARSER_fileCharset, Charset.class).orElse(Charset.defaultCharset());
	}

	@Override /* Parser */
	public final boolean isReaderParser() {
		return true;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * File charset.
	 *
	 * @see #RPARSER_fileCharset
	 * @return
	 * 	The character set to use for reading <c>Files</c> from the file system.
	 */
	protected final Charset getFileCharset() {
		return fileCharset;
	}

	/**
	 * Input stream charset.
	 *
	 * @see #RPARSER_streamCharset
	 * @return
	 * 	The character set to use for converting <c>InputStreams</c> and byte arrays to readers.
	 */
	protected final Charset getStreamCharset() {
		return streamCharset;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public OMap toMap() {
		return super.toMap()
			.a(
				"ReaderParser",
				OMap
					.create()
					.filtered()
					.a("fileCharset", fileCharset)
					.a("streamCharset", streamCharset)
			);
	}
}
