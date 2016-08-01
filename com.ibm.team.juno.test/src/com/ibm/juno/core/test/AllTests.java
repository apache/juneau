/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2016. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.test;

import org.junit.runner.*;
import org.junit.runners.*;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
	com.ibm.juno.core.test.CT_Annotations.class,
	com.ibm.juno.core.test.CT_BeanContext.class,
	com.ibm.juno.core.test.CT_BeanFilter.class,
	com.ibm.juno.core.test.CT_BeanMap.class,
	com.ibm.juno.core.test.CT_ClassMeta.class,
	com.ibm.juno.core.test.CT_DataConversionTest.class,
	com.ibm.juno.core.test.CT_IgnoredClasses.class,
	com.ibm.juno.core.test.CT_JacocoDummy.class,
	com.ibm.juno.core.test.CT_ObjectList.class,
	com.ibm.juno.core.test.CT_ObjectMap.class,
	com.ibm.juno.core.test.CT_ParserGenerics.class,
	com.ibm.juno.core.test.CT_ParserReader.class,
	com.ibm.juno.core.test.CT_PojoFilter.class,
	com.ibm.juno.core.test.CT_PropertyNamerDashedLC.class,
	com.ibm.juno.core.test.CT_Visibility.class,

	com.ibm.juno.core.test.a.rttests.CT_RoundTripAddClassAttrs.class,
	com.ibm.juno.core.test.a.rttests.CT_RoundTripBeanInheritance.class,
	com.ibm.juno.core.test.a.rttests.CT_RoundTripBeanMaps.class,
	com.ibm.juno.core.test.a.rttests.CT_RoundTripObjectsAsStrings.class,
	com.ibm.juno.core.test.a.rttests.CT_RoundTripObjectsWithSpecialMethods.class,
	com.ibm.juno.core.test.a.rttests.CT_RoundTripDTOs.class,
	com.ibm.juno.core.test.a.rttests.CT_RoundTripEnum.class,
	com.ibm.juno.core.test.a.rttests.CT_RoundTripFilterBeans.class,
	com.ibm.juno.core.test.a.rttests.CT_RoundTripGenerics.class,
	com.ibm.juno.core.test.a.rttests.CT_RoundTripLargeObjects.class,
	com.ibm.juno.core.test.a.rttests.CT_RoundTripMaps.class,
	com.ibm.juno.core.test.a.rttests.CT_RoundTripPrimitiveObjectBeans.class,
	com.ibm.juno.core.test.a.rttests.CT_RoundTripPrimitivesBeans.class,
	com.ibm.juno.core.test.a.rttests.CT_RoundTripReadOnlyBeans.class,
	com.ibm.juno.core.test.a.rttests.CT_RoundTripSimpleObjects.class,
	com.ibm.juno.core.test.a.rttests.CT_RoundTripToObjectMaps.class,
	com.ibm.juno.core.test.a.rttests.CT_RoundTripTrimStrings.class,

	com.ibm.juno.core.test.csv.CT_Csv.class,

	com.ibm.juno.core.test.dto.atom.CT_Atom.class,

	com.ibm.juno.core.test.dto.cognos.CT_CognosXml.class,

	com.ibm.juno.core.test.dto.jsonschema.CT_JsonSchema.class,

	com.ibm.juno.core.test.filters.CT_BeanFilter.class,
	com.ibm.juno.core.test.filters.CT_BeanMap.class,
	com.ibm.juno.core.test.filters.CT_ByteArrayBase64Filter.class,
	com.ibm.juno.core.test.filters.CT_CalendarFilter.class,
	com.ibm.juno.core.test.filters.CT_DateFilter.class,
	com.ibm.juno.core.test.filters.CT_EnumerationFilter.class,
	com.ibm.juno.core.test.filters.CT_IteratorFilter.class,
	com.ibm.juno.core.test.filters.CT_ReaderFilter.class,

	com.ibm.juno.core.test.html.CT_Common.class,
	com.ibm.juno.core.test.html.CT_CommonParser.class,
	com.ibm.juno.core.test.html.CT_Html.class,

	com.ibm.juno.core.test.ini.CT_ConfigFile.class,
	com.ibm.juno.core.test.ini.CT_ConfigMgr.class,

	com.ibm.juno.core.test.jena.CT_Common.class,
	com.ibm.juno.core.test.jena.CT_CommonParser.class,
	com.ibm.juno.core.test.jena.CT_CommonXml.class,
	//com.ibm.juno.core.test.jena.CT_Rdf.class, -- Can't run in Foundation tests because of missing Jena libraries.
	com.ibm.juno.core.test.jena.CT_RdfParser.class,

	com.ibm.juno.core.test.json.CT_Common.class,
	com.ibm.juno.core.test.json.CT_CommonParser.class,
	com.ibm.juno.core.test.json.CT_Json.class,
	com.ibm.juno.core.test.json.CT_JsonParser.class,
	com.ibm.juno.core.test.json.CT_JsonSchema.class,

	com.ibm.juno.core.test.urlencoding.CT_Common_Uon.class,
	com.ibm.juno.core.test.urlencoding.CT_Common_UrlEncoding.class,
	com.ibm.juno.core.test.urlencoding.CT_CommonParser_Uon.class,
	com.ibm.juno.core.test.urlencoding.CT_CommonParser_UrlEncoding.class,
	com.ibm.juno.core.test.urlencoding.CT_UonParser.class,
	com.ibm.juno.core.test.urlencoding.CT_UonParserReader.class,
	com.ibm.juno.core.test.urlencoding.CT_UonSerializer.class,
	com.ibm.juno.core.test.urlencoding.CT_UrlEncodingParser.class,
	com.ibm.juno.core.test.urlencoding.CT_UrlEncodingSerializer.class,

	com.ibm.juno.core.test.utils.CT_Args.class,
	com.ibm.juno.core.test.utils.CT_ArrayUtils.class,
	com.ibm.juno.core.test.utils.CT_ByteArrayCache.class,
	com.ibm.juno.core.test.utils.CT_ByteArrayInOutStream.class,
	com.ibm.juno.core.test.utils.CT_CharSet.class,
	com.ibm.juno.core.test.utils.CT_ClassUtils.class,
	com.ibm.juno.core.test.utils.CT_CollectionUtils.class,
	com.ibm.juno.core.test.utils.CT_FilteredMap.class,
	com.ibm.juno.core.test.utils.CT_IdentityList.class,
	com.ibm.juno.core.test.utils.CT_IOPipe.class,
	com.ibm.juno.core.test.utils.CT_IOUtils.class,
	com.ibm.juno.core.test.utils.CT_KeywordStore.class,
	com.ibm.juno.core.test.utils.CT_MultiIterable.class,
	com.ibm.juno.core.test.utils.CT_MultiSet.class,
	com.ibm.juno.core.test.utils.CT_ParserReader.class,
	com.ibm.juno.core.test.utils.CT_PojoIntrospector.class,
	com.ibm.juno.core.test.utils.CT_PojoQuery.class,
	com.ibm.juno.core.test.utils.CT_PojoRest.class,
	com.ibm.juno.core.test.utils.CT_SimpleMap.class,
	com.ibm.juno.core.test.utils.CT_StringBuilderWriter.class,
	com.ibm.juno.core.test.utils.CT_StringUtils.class,
	com.ibm.juno.core.test.utils.CT_StringVarResolver.class,

	com.ibm.juno.core.test.xml.CT_Common.class,
	com.ibm.juno.core.test.xml.CT_CommonParser.class,
	com.ibm.juno.core.test.xml.CT_CommonXml.class,
	com.ibm.juno.core.test.xml.CT_Xml.class,
	com.ibm.juno.core.test.xml.CT_XmlCollapsed.class,
	com.ibm.juno.core.test.xml.CT_XmlContent.class,
	com.ibm.juno.core.test.xml.CT_XmlParser.class,
})
public class AllTests {}
