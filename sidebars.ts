/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

import type { SidebarsConfig } from '@docusaurus/plugin-content-docs';

// This runs in Node.js - Don't use client-side code here (browser APIs, JSX...)

/**
 * Creating a sidebar enables you to:
 - create an ordered group of docs
 - render a sidebar for each doc of that group
 - provide next/previous navigation

 The sidebars can be generated from the filesystem, or explicitly defined here.

 Create as many sidebars as you want.
 */
const sidebars: SidebarsConfig = {
	mainSidebar: [
		{
			type: 'category',
			label: 'Documentation',
			collapsed: false,
			items: [
				{
					type: 'category',
					label: 'About Juneau',
					collapsed: false,
					items: [
						{
							type: 'doc',
							id: 'topics/01.JuneauEcosystemOverview',
							label: '1. Juneau Ecosystem',
						},
						{
							type: 'doc',
							id: 'topics/01.01.FrameworkComparisons',
							label: '1.1. Framework Comparisons',
						},
						{
							type: 'doc',
							id: 'topics/01.02.WhyJuneau',
							label: '1.2. Why Choose Juneau?',
						},
					],
				},
				{
					type: 'category',
					label: 'Core & Data',
					collapsed: false,
					items: [
				{
					type: 'category',
					label: '2. juneau-commons',
					collapsed: true,
					link: {
						type: 'doc',
						id: 'topics/02.juneau-commons',
					},
					items: [
						{
							type: 'doc',
							id: 'topics/02.01.JuneauCommonsUtils',
							label: '2.1. Utils Package',
						},
						{
							type: 'doc',
							id: 'topics/02.02.JuneauCommonsCollections',
							label: '2.2. Collections Package',
						},
						{
							type: 'doc',
							id: 'topics/02.03.JuneauCommonsLang',
							label: '2.3. Lang Package',
						},
						{
							type: 'doc',
							id: 'topics/02.04.JuneauCommonsReflection',
							label: '2.4. Reflection Package',
						},
						{
							type: 'doc',
							id: 'topics/02.05.JuneauCommonsBeans',
							label: '2.5. Beans Package',
						},
						{
							type: 'doc',
							id: 'topics/02.06.JuneauCommonsSettings',
							label: '2.6. Settings Package',
						},
						{
							type: 'doc',
							id: 'topics/02.07.JuneauCommonsFunction',
							label: '2.7. Function Package',
						},
						{
							type: 'doc',
							id: 'topics/02.08.JuneauCommonsConversion',
							label: '2.8. Conversion Package',
						},
						{
							type: 'doc',
							id: 'topics/02.09.JuneauCommonsIO',
							label: '2.9. I/O Package',
						},
						{
							type: 'category',
							label: '2.10. Inject Package',
							collapsed: true,
							link: { type: 'doc', id: 'topics/02.10.JuneauCommonsInject' },
							items: [
								{
									type: 'doc',
									id: 'topics/02.10.01.ValueAnnotation',
									label: '2.10.1. @Value Annotation Basics',
								},
								{
									type: 'doc',
									id: 'topics/02.10.02.ValueFrameworkInternal',
									label: '2.10.2. @Value Framework-Internal Adoption',
								},
							],
						},
						{
							type: 'doc',
							id: 'topics/02.11.SimpleVariableLanguage',
							label: '2.11. Simple Variable Language',
						},
						{
							type: 'doc',
							id: 'topics/02.12.JuneauCommonsHttp',
							label: '2.12. Http Package',
						},
						{
							type: 'doc',
							id: 'topics/02.13.JuneauCommonsHttpPart',
							label: '2.13. Http-Part Package',
						},
						{
							type: 'doc',
							id: 'topics/02.14.JuneauCommonsConcurrent',
							label: '2.14. Concurrent Package',
						},
						{
							type: 'doc',
							id: 'topics/02.15.JuneauCommonsLogging',
							label: '2.15. Logging Package',
						},
						{
							type: 'doc',
							id: 'topics/02.16.JuneauCommonsRuntime',
							label: '2.16. Runtime Package',
						},
						{
							type: 'doc',
							id: 'topics/02.17.JuneauCommonsTime',
							label: '2.17. Time Package',
						},
					],
				},
			{
				type: 'category',
				label: '3. juneau-marshall',
				collapsed: true,
				items: [
					{
						type: 'doc',
						id: 'topics/03.01.Marshallers',
						label: '3.1. Marshallers',
					},
					{
						type: 'doc',
						id: 'topics/03.02.SerializersAndParsers',
						label: '3.2. Serializers and Parsers',
					},
					{
						type: 'category',
						label: '3.3. Bean Contexts',
						collapsed: true,
						link: {
							type: 'doc',
							id: 'topics/03.03.BeanContexts',
						},
						items: [
							{
								type: 'doc',
								id: 'topics/03.03.01.JavaBeansSupport',
								label: '3.3.1. Java Beans Support',
							},
							{
								type: 'doc',
								id: 'topics/03.03.02.JavaRecordsSupport',
								label: '3.3.2. Java Records Support',
							},
							{
								type: 'doc',
								id: 'topics/03.03.03.BeanTypeAnnotation',
								label: '3.3.3. @BeanType Annotation',
							},
							{
								type: 'doc',
								id: 'topics/03.03.04.BeanPropAnnotation',
								label: '3.3.4. @BeanProp Annotation',
							},
							{
								type: 'doc',
								id: 'topics/03.03.05.BeanCtorAnnotation',
								label: '3.3.5. @BeanCtor Annotation',
							},
							{
								type: 'doc',
								id: 'topics/03.03.06.BeanIgnoreAnnotation',
								label: '3.3.6. @MarshalledIgnore Annotation',
							},
							{
								type: 'doc',
								id: 'topics/03.03.07.NamePropertyAnnotation',
								label: '3.3.7. @NameProperty Annotation',
							},
							{
								type: 'doc',
								id: 'topics/03.03.08.ParentPropertyAnnotation',
								label: '3.3.8. @ParentProperty Annotation',
							},
							{
								type: 'doc',
								id: 'topics/03.03.09.PojoBuilders',
								label: '3.3.9. POJO Builders',
							},
							{
								type: 'doc',
								id: 'topics/03.03.10.BypassSerialization',
								label: '3.3.10. Bypass Serialization using `Readers` and `InputStreams`',
							},
							{
								type: 'doc',
								id: 'topics/03.03.11.ViewProjection',
								label: '3.3.11. View-Based Projection',
							},
							{
								type: 'doc',
								id: 'topics/03.03.12.MarshalledAnnotation',
								label: '3.3.12. @Marshalled Annotation',
							},
							{
								type: 'doc',
								id: 'topics/03.03.13.MarshalledAs',
								label: '3.3.13. @MarshalledAs Enum',
							},
							{
								type: 'doc',
								id: 'topics/03.03.14.MarshalledFilter',
								label: '3.3.14. MarshalledFilter',
							},
							{
								type: 'doc',
								id: 'topics/03.03.15.ExampleAnnotation',
								label: '3.3.15. @Example Annotation',
							},
						],
					},
					{
						type: 'doc',
						id: 'topics/03.04.HttpPartSerializersParsers',
						label: '3.4. HTTP Part Serializers and Parsers',
					},
					{
						type: 'doc',
						id: 'topics/03.05.ContextSettings',
						label: '3.5. Context Settings',
					},
					{
						type: 'doc',
						id: 'topics/03.06.NullAndInclusionPolicies',
						label: '3.6. Null & Inclusion Policies',
					},
					{
						type: 'doc',
						id: 'topics/03.07.ContextAnnotations',
						label: '3.7. Context Annotations',
					},
					{
						type: 'doc',
						id: 'topics/03.08.JsonMap',
						label: '3.8. JsonMap and JsonList',
					},
					{
						type: 'doc',
						id: 'topics/03.09.MarshalledNodeAndJsonPointer',
						label: '3.9. Tree Model & RFC 6901 JSON-Pointer',
					},
					{
						type: 'doc',
						id: 'topics/03.10.ComplexDataTypes',
						label: '3.10. Complex Data Types',
					},
					{
						type: 'doc',
						id: 'topics/03.11.SupportedJdkDatatypes',
						label: '3.11. Supported JDK Datatypes',
					},
					{
						type: 'doc',
						id: 'topics/03.12.SerializerSetsParserSets',
						label: '3.12. SerializerSets and ParserSets',
					},
					{
						type: 'category',
						label: '3.13. Swaps',
						collapsed: true,
						link: { type: 'doc', id: 'topics/03.13.Swaps' },
						items: [
							{
								type: 'doc',
								id: 'topics/03.13.01.DefaultSwaps',
								label: '3.13.1. Default Swaps',
							},
							{
								type: 'doc',
								id: 'topics/03.13.02.AutoSwaps',
								label: '3.13.2. Auto-detected swaps',
							},
							{
								type: 'doc',
								id: 'topics/03.13.03.PerMediaTypeSwaps',
								label: '3.13.3. Per-media-type Swaps',
							},
							{
								type: 'doc',
								id: 'topics/03.13.04.OneWaySwaps',
								label: '3.13.4. One-way Swaps',
							},
							{
								type: 'doc',
								id: 'topics/03.13.05.SwapAnnotation',
								label: '3.13.5. @Swap Annotation',
							},
							{
								type: 'doc',
								id: 'topics/03.13.06.TemplatedSwaps',
								label: '3.13.6. Templated Swaps',
							},
							{
								type: 'doc',
								id: 'topics/03.13.07.SurrogateClasses',
								label: '3.13.7. Surrogate Classes',
							},
						],
					},
					{
						type: 'doc',
						id: 'topics/03.14.DynamicallyAppliedAnnotations',
						label: '3.14. Dynamically Applied Annotations',
					},
					{
						type: 'category',
						label: '3.15. Bean Dictionaries',
						collapsed: true,
						link: { type: 'doc', id: 'topics/03.15.BeanDictionaries' },
						items: [
							{
								type: 'doc',
								id: 'topics/03.15.01.BeanSubTypes',
								label: '3.15.1. Bean Subtypes',
							},
						],
					},
					{
						type: 'doc',
						id: 'topics/03.16.VirtualBeans',
						label: '3.16. Virtual Beans',
					},
					{
						type: 'doc',
						id: 'topics/03.17.Recursion',
						label: '3.17. Non-Tree Models and Recursion Detection',
					},
					{
						type: 'doc',
						id: 'topics/03.18.ParsingIntoGenericModels',
						label: '3.18. Parsing into Generic Models',
					},
					{
						type: 'doc',
						id: 'topics/03.19.MarshallingUris',
						label: '3.19. URIs',
					},
					{
						type: 'doc',
						id: 'topics/03.20.JacksonComparison',
						label: '3.20. Comparison with Jackson',
					},
					{
						type: 'doc',
						id: 'topics/03.21.PojoCategories',
						label: '3.21. POJO Categories',
					},
					{
						type: 'category',
						label: '3.22. Simple Variable Language',
						collapsed: true,
						link: { type: 'doc', id: 'topics/03.22.MarshallSimpleVariableLanguage' },
						items: [
							{
								type: 'doc',
								id: 'topics/03.22.01.SvlVariables',
								label: '3.22.1. SVL Variables',
							},
							{
								type: 'doc',
								id: 'topics/03.22.02.VarResolvers',
								label: '3.22.2. VarResolvers and VarResolverSessions',
							},
							{
								type: 'doc',
								id: 'topics/03.22.03.DefaultVarResolver',
								label: '3.22.3. VarResolver.DEFAULT',
							},
							{
								type: 'doc',
								id: 'topics/03.22.04.SvlOtherNotes',
								label: '3.22.4. Other Notes',
							},
						],
					},
					{
						type: 'doc',
						id: 'topics/03.23.MarshallEncoders',
						label: '3.23. Encoders',
					},
					{
						type: 'doc',
						id: 'topics/03.24.ObjectTools',
						label: '3.24. Object Tools',
					},
					{
						type: 'category',
						label: '3.25. JSON Support',
						collapsed: true,
						link: { type: 'doc', id: 'topics/03.25.JsonSupport' },
						items: [
							{
								type: 'doc',
								id: 'topics/03.25.01.JsonMethodology',
								label: '3.25.1. JSON Methodology',
							},
							{
								type: 'doc',
								id: 'topics/03.25.02.JsonSerializers',
								label: '3.25.2. JSON Serializers',
							},
							{
								type: 'doc',
								id: 'topics/03.25.03.Json5',
								label: '3.25.3. JSON 5',
							},
							{
								type: 'doc',
								id: 'topics/03.25.04.JsonParsers',
								label: '3.25.4. JSON Parsers',
							},
							{
								type: 'doc',
								id: 'topics/03.25.05.JsonAnnotation',
								label: '3.25.5. @Json Annotation',
							},
							{
								type: 'doc',
								id: 'topics/03.25.06.JsonSchemaDetails',
								label: '3.25.6. JSON-Schema Support',
							},
						],
					},
					{
						type: 'category',
						label: '3.26. XML Support',
						collapsed: true,
						link: { type: 'doc', id: 'topics/03.26.XmlSupport' },
						items: [
							{
								type: 'doc',
								id: 'topics/03.26.01.XmlMethodology',
								label: '3.26.1. XML Methodology',
							},
							{
								type: 'doc',
								id: 'topics/03.26.02.XmlSerializers',
								label: '3.26.2. XML Serializers',
							},
							{
								type: 'doc',
								id: 'topics/03.26.03.XmlParsers',
								label: '3.26.3. XML Parsers',
							},
							{
								type: 'doc',
								id: 'topics/03.26.04.XmlBeanTypeNameAnnotation',
								label: '3.26.4. @Marshalled(typeName) Annotation',
							},
							{
								type: 'doc',
								id: 'topics/03.26.05.XmlChildNameAnnotation',
								label: '3.26.5. @Xml(childName) Annotation',
							},
							{
								type: 'category',
								label: '3.26.6. @Xml(format) Annotation',
								collapsed: true,
								link: { type: 'doc', id: 'topics/03.26.06.XmlFormatAnnotation' },
								items: [
									{
										type: 'doc',
										id: 'topics/03.26.06.01.XmlFormatAttrs',
										label: '3.26.6.1. Attribute Formats',
									},
									{
										type: 'doc',
										id: 'topics/03.26.06.02.XmlFormatCollapsed',
										label: '3.26.6.2. Collapsed Format',
									},
									{
										type: 'doc',
										id: 'topics/03.26.06.03.XmlFormatElements',
										label: '3.26.6.3. Element & Mixed Formats',
									},
									{
										type: 'doc',
										id: 'topics/03.26.06.04.XmlFormatText',
										label: '3.26.6.4. Text Formats',
									},
									{
										type: 'doc',
										id: 'topics/03.26.06.05.XmlFormatWhitespace',
										label: '3.26.6.5. Whitespace Handling',
									},
								],
							},
							{
								type: 'doc',
								id: 'topics/03.26.07.XmlNamespaces',
								label: '3.26.7. Namespaces',
							},
							{
								type: 'doc',
								id: 'topics/03.26.08.SoapXml',
								label: '3.26.8. SOAP/XML Support',
							},
						],
					},
					{
						type: 'category',
						label: '3.27. HTML Support',
						collapsed: true,
						link: { type: 'doc', id: 'topics/03.27.HtmlSupport' },
						items: [
							{
								type: 'doc',
								id: 'topics/03.27.01.HtmlMethodology',
								label: '3.27.1. HTML Methodology',
							},
							{
								type: 'doc',
								id: 'topics/03.27.02.HtmlSerializers',
								label: '3.27.2. HTML Serializers',
							},
							{
								type: 'doc',
								id: 'topics/03.27.03.HtmlParsers',
								label: '3.27.3. HTML Parsers',
							},
							{
								type: 'doc',
								id: 'topics/03.27.04.HtmlAnnotation',
								label: '3.27.4. @Html Annotation',
							},
							{
								type: 'doc',
								id: 'topics/03.27.05.HtmlRenderAnnotation',
								label: '3.27.5. @Html(render) Annotation',
							},
							{
								type: 'doc',
								id: 'topics/03.27.06.HtmlDocSerializer',
								label: '3.27.6. HtmlDocSerializer',
							},
							{
								type: 'doc',
								id: 'topics/03.27.07.BasicHtmlDocTemplate',
								label: '3.27.7. BasicHtmlDocTemplate',
							},
							{
								type: 'doc',
								id: 'topics/03.27.08.HtmlCustomTemplates',
								label: '3.27.8. Custom Templates',
							},
							{
								type: 'doc',
								id: 'topics/03.27.09.HtmlSchemaSupport',
								label: '3.27.9. HTML-Schema Support',
							},
						],
					},
					{
						type: 'category',
						label: '3.28. UON Support',
						collapsed: true,
						link: { type: 'doc', id: 'topics/03.28.UonSupport' },
						items: [
							{
								type: 'doc',
								id: 'topics/03.28.01.UonMethodology',
								label: '3.28.1. UON Methodology',
							},
							{
								type: 'doc',
								id: 'topics/03.28.02.UonSerializers',
								label: '3.28.2. UON Serializers',
							},
							{
								type: 'doc',
								id: 'topics/03.28.03.UonParsers',
								label: '3.28.3. UON Parsers',
							},
						],
					},
					{
						type: 'category',
						label: '3.29. URL-Encoding Support',
						collapsed: true,
						link: { type: 'doc', id: 'topics/03.29.UrlEncodingSupport' },
						items: [
							{
								type: 'doc',
								id: 'topics/03.29.01.UrlEncMethodology',
								label: '3.29.1. URL-Encoding Methodology',
							},
							{
								type: 'doc',
								id: 'topics/03.29.02.UrlEncSerializers',
								label: '3.29.2. URL-Encoding Serializers',
							},
							{
								type: 'doc',
								id: 'topics/03.29.03.UrlEncParsers',
								label: '3.29.3. URL-Encoding Parsers',
							},
							{
								type: 'doc',
								id: 'topics/03.29.04.UrlEncodingAnnotation',
								label: '3.29.4. @UrlEncoding Annotation',
							},
						],
					},
					{
						type: 'category',
						label: '3.30. MessagePack Support',
						collapsed: true,
						link: { type: 'doc', id: 'topics/03.30.MessagePackSupport' },
						items: [
							{
								type: 'doc',
								id: 'topics/03.30.01.MsgPackSerializers',
								label: '3.30.1. MessagePack Serializers',
							},
							{
								type: 'doc',
								id: 'topics/03.30.02.MsgPackParsers',
								label: '3.30.2. MessagePack Parsers',
							},
						],
					},
					{
						type: 'category',
						label: '3.31. OpenApi Support',
						collapsed: true,
						link: { type: 'doc', id: 'topics/03.31.OpenApiSupport' },
						items: [
							{
								type: 'doc',
								id: 'topics/03.31.01.OpenApiMethodology',
								label: '3.31.1. OpenAPI Methodology',
							},
							{
								type: 'doc',
								id: 'topics/03.31.02.OpenApiSerializers',
								label: '3.31.2. OpenAPI Serializers',
							},
							{
								type: 'doc',
								id: 'topics/03.31.03.OpenApiParsers',
								label: '3.31.3. OpenAPI Parsers',
							},
						],
					},
					{
						type: 'doc',
						id: 'topics/03.32.TomlSupport',
						label: '3.32. TOML Support',
					},
					{
						type: 'doc',
						id: 'topics/03.33.Prototext',
						label: '3.33. Prototext (Protobuf Text Format)',
					},
					{
						type: 'doc',
						id: 'topics/03.34.Protobuf',
						label: '3.34. Protobuf (Binary Format)',
					},
					{
						type: 'doc',
						id: 'topics/03.35.Parquet',
						label: '3.35. Parquet Basics',
					},
					{
						type: 'category',
						label: '3.36. YAML Support',
						collapsed: true,
						link: { type: 'doc', id: 'topics/03.36.YamlSupport' },
						items: [
							{
								type: 'doc',
								id: 'topics/03.36.01.YamlMethodology',
								label: '3.36.1. YAML Methodology',
							},
							{
								type: 'doc',
								id: 'topics/03.36.02.YamlSerializers',
								label: '3.36.2. YAML Serializers',
							},
							{
								type: 'doc',
								id: 'topics/03.36.03.YamlParsers',
								label: '3.36.3. YAML Parsers',
							},
							{
								type: 'doc',
								id: 'topics/03.36.04.YamlAnnotation',
								label: '3.36.4. @YamlConfig Annotation',
							},
						],
					},
					{
						type: 'category',
						label: '3.37. CSV Support',
						collapsed: true,
						link: { type: 'doc', id: 'topics/03.37.CsvSupport' },
						items: [
							{
								type: 'doc',
								id: 'topics/03.37.01.CsvSerializers',
								label: '3.37.1. CSV Serializers',
							},
							{
								type: 'doc',
								id: 'topics/03.37.02.CsvParsers',
								label: '3.37.2. CSV Parsers',
							},
						],
					},
					{
						type: 'doc',
						id: 'topics/03.38.Markdown',
						label: '3.38. Markdown Support',
					},
					{
						type: 'category',
						label: '3.39. JSONL Support',
						collapsed: true,
						link: { type: 'doc', id: 'topics/03.39.JsonlSupport' },
						items: [
							{
								type: 'doc',
								id: 'topics/03.39.01.Json5l',
								label: '3.39.1. JSON5L Basics',
							},
						],
					},
					{
						type: 'doc',
						id: 'topics/03.40.Hjson',
						label: '3.40. Hjson Support',
					},
					{
						type: 'doc',
						id: 'topics/03.41.Jcs',
						label: '3.41. JCS Support',
					},
					{
						type: 'doc',
						id: 'topics/03.42.Bson',
						label: '3.42. BSON Support',
					},
					{
						type: 'doc',
						id: 'topics/03.43.Cbor',
						label: '3.43. CBOR Support',
					},
					{
						type: 'doc',
						id: 'topics/03.44.HoconSupport',
						label: '3.44. HOCON Support',
					},
					{
						type: 'doc',
						id: 'topics/03.45.Ini',
						label: '3.45. INI Basics',
					},
					{
						type: 'doc',
						id: 'topics/03.46.Sse',
						label: '3.46. SSE Support',
					},
					{
						type: 'category',
						label: '3.47. Token & Record Streaming',
						collapsed: true,
						link: { type: 'doc', id: 'topics/03.47.TokenRecordStreaming' },
						items: [
							{
								type: 'doc',
								id: 'topics/03.47.01.RecordStreaming',
								label: '3.47.1. Record Streaming',
							},
							{
								type: 'doc',
								id: 'topics/03.47.02.TokenStreaming',
								label: '3.47.2. Token Streaming',
							},
							{
								type: 'doc',
								id: 'topics/03.47.03.ArrayRecordStreaming',
								label: '3.47.3. Array-Record Streaming',
							},
							{
								type: 'doc',
								id: 'topics/03.47.04.RestStreamingIntegration',
								label: '3.47.4. REST Streaming Integration',
							},
							{
								type: 'doc',
								id: 'topics/03.47.05.ReadingContinuousStreams',
								label: '3.47.5. Reading Continuous Streams',
							},
							{
								type: 'doc',
								id: 'topics/03.47.06.LargeDatasetStreaming',
								label: '3.47.6. Large-Dataset Streaming',
							},
						],
					},
					{
						type: 'doc',
						id: 'topics/03.48.BestPractices',
						label: '3.48. Best Practices',
					},
					{
						type: 'doc',
						id: 'topics/03.49.PlainText',
						label: '3.49. PlainText Support',
					},
				],
				link: {
					type: 'doc',
					id: 'topics/03.juneau-marshall',
				},
			},
				{
					type: 'category',
					label: '4. juneau-marshall-rdf',
					collapsed: true,
					items: [
						{
							type: 'doc',
							id: 'topics/04.01.Rdf',
							label: '4.1. RDF Basics',
						},
						{
							type: 'doc',
							id: 'topics/04.02.RdfSerializers',
							label: '4.2. RDF Serializers',
						},
						{
							type: 'doc',
							id: 'topics/04.03.RdfParsers',
							label: '4.3. RDF Parsers',
						},
					],
					link: {
						type: 'doc',
						id: 'topics/04.juneau-marshall-rdf',
					},
				},
				{
					type: 'category',
					label: '5. juneau-bean',
					collapsed: true,
					items: [
						{
							type: 'doc',
							id: 'topics/05.01.JuneauBeanHtml5',
							label: '5.1. juneau-bean-html5',
						},
						{
							type: 'doc',
							id: 'topics/05.02.JuneauBeanAtom',
							label: '5.2. juneau-bean-atom',
						},
						{
							type: 'doc',
							id: 'topics/05.03.JuneauBeanJsonSchema',
							label: '5.3. juneau-bean-jsonschema',
						},
						{
							type: 'doc',
							id: 'topics/05.04.JuneauBeanOpenApi3',
							label: '5.4. juneau-bean-openapi-v3',
						},
						{
							type: 'doc',
							id: 'topics/05.05.JuneauBeanCommon',
							label: '5.5. juneau-bean-common',
						},
						{
							type: 'doc',
							id: 'topics/05.06.JuneauBeanSwagger2',
							label: '5.6. juneau-bean-swagger-v2',
						},
						{
							type: 'doc',
							id: 'topics/05.07.JuneauBeanMcp',
							label: '5.7. juneau-bean-mcp',
						},
						{
							type: 'doc',
							id: 'topics/05.08.JuneauBeanRfc7807',
							label: '5.8. juneau-bean-rfc7807',
						},
						{
							type: 'doc',
							id: 'topics/05.09.JuneauBeanHal',
							label: '5.9. juneau-bean-hal',
						},
						{
							type: 'doc',
							id: 'topics/05.10.JuneauBeanJsonApi',
							label: '5.10. juneau-bean-jsonapi',
						},
						{
							type: 'doc',
							id: 'topics/05.11.JuneauBeanJsonPatch',
							label: '5.11. juneau-bean-jsonpatch',
						},
					],
					link: {
						type: 'doc',
						id: 'topics/05.juneau-bean',
					},
				},
				{
					type: 'category',
					label: '6. juneau-config',
					collapsed: true,
					items: [
						{
							type: 'category',
							label: '6.1. Overview',
							collapsed: true,
							link: { type: 'doc', id: 'topics/06.01.Overview' },
							items: [
								{
									type: 'doc',
									id: 'topics/06.01.01.SyntaxRules',
									label: '6.1.1. Syntax Rules',
								},
							],
						},
						{
							type: 'category',
							label: '6.2. Reading Entries',
							collapsed: true,
							link: { type: 'doc', id: 'topics/06.02.ReadingEntries' },
							items: [
								{
									type: 'doc',
									id: 'topics/06.02.01.Pojos',
									label: '6.2.1. POJOs',
								},
								{
									type: 'doc',
									id: 'topics/06.02.02.Arrays',
									label: '6.2.2. Arrays',
								},
								{
									type: 'doc',
									id: 'topics/06.02.03.JCFObjects',
									label: '6.2.3. Java Collection Framework Objects',
								},
								{
									type: 'doc',
									id: 'topics/06.02.04.BinaryData',
									label: '6.2.4. Binary Data',
								},
							],
						},
						{
							type: 'category',
							label: '6.3. Variables',
							collapsed: true,
							link: { type: 'doc', id: 'topics/06.03.Variables' },
							items: [
								{
									type: 'doc',
									id: 'topics/06.03.01.LogicVariables',
									label: '6.3.1. Logic Variables',
								},
								{
									type: 'doc',
									id: 'topics/06.03.02.PropertySources',
									label: '6.3.2. Property Sources',
								},
							],
						},
						{
							type: 'doc',
							id: 'topics/06.04.ModdedEntries',
							label: '6.4. Modded/Encoded Entries',
						},
						{
							type: 'doc',
							id: 'topics/06.05.Sections',
							label: '6.5. Sections',
						},
						{
							type: 'category',
							label: '6.6. Setting Values',
							collapsed: true,
							link: { type: 'doc', id: 'topics/06.06.SettingValues' },
							items: [
								{
									type: 'doc',
									id: 'topics/06.06.01.FileSystemChanges',
									label: '6.6.1. File System Changes',
								},
								{
									type: 'doc',
									id: 'topics/06.06.02.CustomEntrySerialization',
									label: '6.6.2. Custom Entry Serialization',
								},
								{
									type: 'doc',
									id: 'topics/06.06.03.BulkSettingValues',
									label: '6.6.3. Setting Values in Bulk',
								},
							],
						},
						{
							type: 'doc',
							id: 'topics/06.07.ConfigListeners',
							label: '6.7. Listeners',
						},
						{
							type: 'doc',
							id: 'topics/06.08.SerializingConfigs',
							label: '6.8. Serializing',
						},
						{
							type: 'doc',
							id: 'topics/06.09.Imports',
							label: '6.9. Imports',
						},
						{
							type: 'category',
							label: '6.10. Config Stores',
							collapsed: true,
							link: { type: 'doc', id: 'topics/06.10.ConfigStores' },
							items: [
								{
									type: 'doc',
									id: 'topics/06.10.01.MemoryStore',
									label: '6.10.1. MemoryStore',
								},
								{
									type: 'doc',
									id: 'topics/06.10.02.FileStore',
									label: '6.10.2. FileStore',
								},
								{
									type: 'doc',
									id: 'topics/06.10.03.CustomStores',
									label: '6.10.3. Custom ConfigStores',
								},
								{
									type: 'doc',
									id: 'topics/06.10.04.StoreListeners',
									label: '6.10.4. ConfigStore Listeners',
								},
							],
						},
						{
							type: 'doc',
							id: 'topics/06.11.ReadOnlyConfigs',
							label: '6.11. Read-only Configs',
						},
						{
							type: 'doc',
							id: 'topics/06.12.ClosingConfigs',
							label: '6.12. Closing Configs',
						},
						{
							type: 'doc',
							id: 'topics/06.13.SystemDefaultConfig',
							label: '6.13. System Default Config',
						},
						{
							type: 'doc',
							id: 'topics/06.14.YamlConfigFiles',
							label: '6.14. YAML Config Files',
						},
						{
							type: 'doc',
							id: 'topics/06.15.ConfigProfilesAndRelaxedBinding',
							label: '6.15. Config Profiles & Relaxed Binding',
						},
					],
					link: {
						type: 'doc',
						id: 'topics/06.juneau-config',
					},
				},
				{
					type: 'category',
					label: '7. juneau-test',
					collapsed: true,
					items: [
						{
							type: 'doc',
							id: 'topics/07.01.AssertionsOverview',
							label: '7.1. Assertions Overview',
						},
						{
							type: 'category',
							label: '7.2. Bean-Centric Testing',
							collapsed: true,
							link: { type: 'doc', id: 'topics/07.02.BeanCentricTesting' },
							items: [
								{
									type: 'doc',
									id: 'topics/07.02.01.CustomErrorMessages',
									label: '7.2.1. Custom Error Messages',
								},
								{
									type: 'category',
									label: '7.2.2. Customization',
									collapsed: true,
									link: { type: 'doc', id: 'topics/07.02.02.Customization' },
									items: [
										{
											type: 'doc',
											id: 'topics/07.02.02.01.Stringifiers',
											label: '7.2.2.1. Stringifiers',
										},
										{
											type: 'doc',
											id: 'topics/07.02.02.02.Listifiers',
											label: '7.2.2.2. Listifiers',
										},
										{
											type: 'doc',
											id: 'topics/07.02.02.03.Swappers',
											label: '7.2.2.3. Swappers',
										},
									],
								},
								{
									type: 'doc',
									id: 'topics/07.02.03.PropertyExtractors',
									label: '7.2.3. Property Extractors',
								},
							],
						},
						{
							type: 'doc',
							id: 'topics/07.03.JuneauTestJUnitExtensions',
							label: '7.3. JUnit Extensions',
						},
					],
					link: {
						type: 'doc',
						id: 'topics/07.juneau-test',
					},
				},
					],
				},
				{
					type: 'category',
					label: 'REST',
					collapsed: false,
					items: [
				{
					type: 'doc',
					id: 'topics/08.juneau-rest',
					label: '8. juneau-rest',
				},
				{
					type: 'category',
					label: '9. juneau-rest-common',
					collapsed: true,
					items: [
						{
							type: 'doc',
							id: 'topics/09.01.HelperClasses',
							label: '9.1. Helper Classes',
						},
						{
							type: 'doc',
							id: 'topics/09.02.Annotations',
							label: '9.2. Annotations',
						},
						{
							type: 'doc',
							id: 'topics/09.03.HttpHeaders',
							label: '9.3. HTTP Headers',
						},
						{
							type: 'doc',
							id: 'topics/09.04.HttpParts',
							label: '9.4. HTTP Parts',
						},
						{
							type: 'doc',
							id: 'topics/09.05.HttpEntitiesAndResources',
							label: '9.5. HTTP Entities and Resources',
						},
						{
							type: 'doc',
							id: 'topics/09.06.HttpResponses',
							label: '9.6. HTTP Responses',
						},
						{
							type: 'doc',
							id: 'topics/09.07.RemoteProxyInterfaces',
							label: '9.7. Remote Proxy Interfaces',
						},
					],
					link: {
						type: 'doc',
						id: 'topics/09.juneau-rest-common',
					},
				},
				{
					type: 'category',
					label: '10. juneau-rest-server',
					collapsed: true,
					items: [
						{
							type: 'doc',
							id: 'topics/10.01.RestServerOverview',
							label: '10.1. REST Server Overview',
						},
						{
							type: 'category',
							label: '10.2. @Rest-Annotated Classes',
							collapsed: true,
							link: { type: 'doc', id: 'topics/10.02.RestAnnotatedClasses' },
							items: [
								{
									type: 'doc',
									id: 'topics/10.02.01.PredefinedClasses',
									label: '10.2.1. Predefined Classes',
								},
								{
									type: 'doc',
									id: 'topics/10.02.02.ChildResources',
									label: '10.2.2. Child Resources',
								},
								{
									type: 'doc',
									id: 'topics/10.02.03.PathVariables',
									label: '10.2.3. Path Variables',
								},
								{
									type: 'doc',
									id: 'topics/10.02.04.Deployment',
									label: '10.2.4. Deployment',
								},
								{
									type: 'doc',
									id: 'topics/10.02.05.LifecycleHooks',
									label: '10.2.5. Lifecycle Hooks',
								},
							],
						},
						{
							type: 'category',
							label: '10.3. @RestOp-Annotated Methods',
							collapsed: true,
							link: { type: 'doc', id: 'topics/10.03.RestOpAnnotatedMethods' },
							items: [
								{
									type: 'doc',
									id: 'topics/10.03.01.InferredHttpMethodsAndPaths',
									label: '10.3.1. Inferred HTTP Methods and Paths',
								},
								{
									type: 'doc',
									id: 'topics/10.03.02.JavaMethodParameters',
									label: '10.3.2. Java Method Parameters',
								},
								{
									type: 'doc',
									id: 'topics/10.03.03.JavaMethodReturnTypes',
									label: '10.3.3. Java Method Return Types',
								},
								{
									type: 'doc',
									id: 'topics/10.03.04.JavaMethodThrowableTypes',
									label: '10.3.4. Java Method Throwable Types',
								},
								{
									type: 'doc',
									id: 'topics/10.03.05.PathPatterns',
									label: '10.3.5. Path Patterns',
								},
								{
									type: 'doc',
									id: 'topics/10.03.06.Matchers',
									label: '10.3.6. Matchers',
								},
								{
									type: 'doc',
									id: 'topics/10.03.07.OverloadingHttpMethods',
									label: '10.3.7. Overloading HTTP Methods',
								},
								{
									type: 'doc',
									id: 'topics/10.03.08.AdditionalInformation',
									label: '10.3.8. Additional Information',
								},
							],
						},
						{
							type: 'category',
							label: '10.4. HTTP Parts',
							collapsed: true,
							link: { type: 'doc', id: 'topics/10.04.RestServerHttpParts' },
							items: [
								{
									type: 'doc',
									id: 'topics/10.04.01.PartMarshallers',
									label: '10.4.1. Part Marshallers',
								},
								{
									type: 'doc',
									id: 'topics/10.04.02.HttpPartAnnotations',
									label: '10.4.2. HTTP Part Annotations',
								},
								{
									type: 'doc',
									id: 'topics/10.04.03.DefaultParts',
									label: '10.4.3. Default Parts',
								},
								{
									type: 'doc',
									id: 'topics/10.04.04.RequestBeans',
									label: '10.4.4. Request Beans',
								},
								{
									type: 'doc',
									id: 'topics/10.04.05.ResponseBeans',
									label: '10.4.5. @Response Beans',
								},
								{
									type: 'doc',
									id: 'topics/10.04.06.HttpPartApis',
									label: '10.4.6. HTTP Part APIs',
								},
								{
									type: 'doc',
									id: 'topics/10.04.07.HttpPartValidation',
									label: '10.4.7. HTTP Part Validation',
								},
							],
						},
						{
							type: 'doc',
							id: 'topics/10.05.Marshalling',
							label: '10.5. Marshalling',
						},
						{
							type: 'doc',
							id: 'topics/10.06.HandlingFormPosts',
							label: '10.6. Form Posts',
						},
						{
							type: 'doc',
							id: 'topics/10.07.RestServerComposition',
							label: '10.7. Mixins and Multi-Mount Paths',
						},
						{
							type: 'doc',
							id: 'topics/10.08.RestServerMixinSubContexts',
							label: '10.8. Mixin Sub-Contexts',
						},
						{
							type: 'doc',
							id: 'topics/10.09.RestServerStandaloneVsMixin',
							label: '10.9. Standalone vs Mixin Resources',
						},
						{
							type: 'doc',
							id: 'topics/10.10.RestServerChildrenVsMixins',
							label: '10.10. Children vs Mixins',
						},
						{
							type: 'doc',
							id: 'topics/10.11.RestServerSelfRegistration',
							label: '10.11. Self-Registration',
						},
						{
							type: 'doc',
							id: 'topics/10.12.RestServerProgrammaticBuilder',
							label: '10.12. Programmatic Fluent Builder',
						},
						{
							type: 'doc',
							id: 'topics/10.13.RestServerSse',
							label: '10.13. Server-Sent Events',
						},
						{
							type: 'doc',
							id: 'topics/10.14.Guards',
							label: '10.14. Guards',
						},
						{
							type: 'doc',
							id: 'topics/10.15.Converters',
							label: '10.15. Converters',
						},
						{
							type: 'doc',
							id: 'topics/10.16.LocalizedMessages',
							label: '10.16. Localized Messages',
						},
						{
							type: 'doc',
							id: 'topics/10.17.RestServerEncoders',
							label: '10.17. Encoders',
						},
						{
							type: 'doc',
							id: 'topics/10.18.ConfigurationFiles',
							label: '10.18. Configuration Files',
						},
						{
							type: 'doc',
							id: 'topics/10.19.RestServerSvlVariables',
							label: '10.19. SVL Variables',
						},
						{
							type: 'doc',
							id: 'topics/10.20.StaticFiles',
							label: '10.20. Static files',
						},
						{
							type: 'doc',
							id: 'topics/10.21.StaticFilesMixin',
							label: '10.21. Static-Files Mixin',
						},
						{
							type: 'doc',
							id: 'topics/10.22.ConventionEndpointsMixins',
							label: '10.22. Convention-Endpoints Mixin Pack',
						},
						{
							type: 'doc',
							id: 'topics/10.23.OpsIntrospectionMixins',
							label: '10.23. Ops / Introspection Mixin Pack',
						},
						{
							type: 'doc',
							id: 'topics/10.24.JspViewSupport',
							label: '10.24. JSP View Support',
						},
						{
							type: 'doc',
							id: 'topics/10.25.ThymeleafViewSupport',
							label: '10.25. Thymeleaf View Support',
						},
						{
							type: 'doc',
							id: 'topics/10.26.MustacheViewSupport',
							label: '10.26. Mustache View Support',
						},
						{
							type: 'doc',
							id: 'topics/10.27.FreemarkerViewSupport',
							label: '10.27. FreeMarker View Support',
						},
						{
							type: 'doc',
							id: 'topics/10.28.ClientVersioning',
							label: '10.28. Client Versioning',
						},
						{
							type: 'category',
							label: '10.29. Swagger',
							collapsed: true,
							link: { type: 'doc', id: 'topics/10.29.Swagger' },
							items: [
								{
									type: 'doc',
									id: 'topics/10.29.01.BasicRestServletSwagger',
									label: '10.29.1. BasicRestServlet/BasicRestObject Swagger and OpenAPI 3.1',
								},
								{
									type: 'doc',
									id: 'topics/10.29.02.ApiDocsMixins',
									label: '10.29.2. API-Docs Mixin Pack',
								},
								{
									type: 'doc',
									id: 'topics/10.29.03.BasicSwaggerInfo',
									label: '10.29.3. Basic Swagger Info',
								},
								{
									type: 'doc',
									id: 'topics/10.29.04.SwaggerTags',
									label: '10.29.4. Swagger Tags',
								},
								{
									type: 'doc',
									id: 'topics/10.29.05.SwaggerOperations',
									label: '10.29.5. Swagger Operations',
								},
								{
									type: 'doc',
									id: 'topics/10.29.06.SwaggerParameters',
									label: '10.29.6. Swagger Parameters',
								},
								{
									type: 'doc',
									id: 'topics/10.29.07.SwaggerResponses',
									label: '10.29.7. Swagger Responses',
								},
								{
									type: 'doc',
									id: 'topics/10.29.08.SwaggerModels',
									label: '10.29.8. Swagger Models',
								},
								{
									type: 'doc',
									id: 'topics/10.29.09.SwaggerStylesheet',
									label: '10.29.9. SwaggerUI.css',
								},
							],
						},
						{
							type: 'doc',
							id: 'topics/10.30.ExecutionStatistics',
							label: '10.30. REST method execution statistics',
						},
						{
							type: 'category',
							label: '10.31. @HtmlDocConfig Annotation',
							collapsed: true,
							link: { type: 'doc', id: 'topics/10.31.HtmlDocConfigAnnotation' },
							items: [
								{
									type: 'doc',
									id: 'topics/10.31.01.HtmlUIvsDI',
									label: '10.31.1. User Interfaces (UI) vs. Developer Interfaces (DI)',
								},
								{
									type: 'doc',
									id: 'topics/10.31.02.HtmlWidgets',
									label: '10.31.2. Widgets',
								},
								{
									type: 'doc',
									id: 'topics/10.31.03.HtmlPredefinedWidgets',
									label: '10.31.3. Predefined Widgets',
								},
								{
									type: 'doc',
									id: 'topics/10.31.04.HtmlUiCustomization',
									label: '10.31.4. UI Customization',
								},
								{
									type: 'doc',
									id: 'topics/10.31.05.HtmlStylesheets',
									label: '10.31.5. Stylesheets',
								},
							],
						},
						{
							type: 'doc',
							id: 'topics/10.32.RestServerLoggingAndDebugging',
							label: '10.32. Logging / Debugging',
						},
						{
							type: 'doc',
							id: 'topics/10.33.HttpStatusCodes',
							label: '10.33. HTTP Status Codes',
						},
						{
							type: 'doc',
							id: 'topics/10.34.RestServerProblemDetails',
							label: '10.34. RFC 7807 / 9457 Problem Details',
						},
						{
							type: 'doc',
							id: 'topics/10.35.RestServerConditionalGet',
							label: '10.35. Conditional-GET / ETag Helpers',
						},
						{
							type: 'doc',
							id: 'topics/10.36.RestServerRateLimitAndRequestId',
							label: '10.36. Rate-Limiting and Request-Id Propagation',
						},
						{
							type: 'doc',
							id: 'topics/10.37.RestServerTestBeanInjection',
							label: '10.37. Test-time Bean Injection',
						},
						{
							type: 'doc',
							id: 'topics/10.38.RestServerAuthGuards',
							label: '10.38. AuthN Guards — Bearer / API-Key / JWT',
						},
						{
							type: 'doc',
							id: 'topics/10.39.RestServerValidation',
							label: '10.39. Jakarta Bean Validation',
						},
						{
							type: 'doc',
							id: 'topics/10.40.RestServerObservability',
							label: '10.40. Observability — Micrometer + OpenTelemetry',
						},
						{
							type: 'doc',
							id: 'topics/10.41.RestServerMicrometerMetrics',
							label: '10.41. Micrometer Metrics Bridge',
						},
						{
							type: 'doc',
							id: 'topics/10.42.RestServerOtelTracing',
							label: '10.42. OpenTelemetry Tracing Bridge',
						},
						{
							type: 'doc',
							id: 'topics/10.43.RestServerAsyncDispatch',
							label: '10.43. Async Returns + Virtual-Thread Dispatch',
						},
						{
							type: 'doc',
							id: 'topics/10.44.AuthFilterFramework',
							label: '10.44. AuthN Filter Framework — Servlet-Layer Authentication',
						},
						{
							type: 'doc',
							id: 'topics/10.45.RestServerAuthenticator',
							label: '10.45. REST Authenticator — Resource-Level Authentication',
						},
						{
							type: 'doc',
							id: 'topics/10.46.SamlAuthSupport',
							label: '10.46. SAML 2.0 AuthN Support (juneau-rest-server-auth-saml)',
						},
						{
							type: 'doc',
							id: 'topics/10.47.OAuthAuthSupport',
							label: '10.47. OAuth 2.0 / OIDC AuthN Support (juneau-rest-server-auth-oauth)',
						},
						{
							type: 'doc',
							id: 'topics/10.48.OidcRelyingParty',
							label: '10.48. OIDC Relying Party Login (juneau-rest-server-auth-oidc-rp)',
						},
						{
							type: 'doc',
							id: 'topics/10.49.RestServerReactive',
							label: '10.49. Reactive-Streams Returns',
						},
						{
							type: 'doc',
							id: 'topics/10.50.BuiltInParameters',
							label: '10.50. Built-in Parameters',
						},
						{
							type: 'doc',
							id: 'topics/10.51.SessionOptions',
							label: '10.51. Session Options via HTTP',
						},
						{
							type: 'doc',
							id: 'topics/10.52.UsingWithOsgi',
							label: '10.52. Using with OSGi',
						},
						{
							type: 'doc',
							id: 'topics/10.53.RestContext',
							label: '10.53. RestContext',
						},
						{
							type: 'doc',
							id: 'topics/10.54.RestOpContext',
							label: '10.54. RestOpContext',
						},
						{
							type: 'doc',
							id: 'topics/10.55.ResponseProcessors',
							label: '10.55. Response Processors',
						},
						{
							type: 'doc',
							id: 'topics/10.56.RestRpc',
							label: '10.56. REST/RPC',
						},
						{
							type: 'doc',
							id: 'topics/10.57.SerializingUris',
							label: '10.57. Serializing URIs',
						},
						{
							type: 'doc',
							id: 'topics/10.58.UtilityBeans',
							label: '10.58. Utility Beans',
						},
						{
							type: 'doc',
							id: 'topics/10.59.HtmlBeans',
							label: '10.59. Using with HTML Beans',
						},
						{
							type: 'doc',
							id: 'topics/10.60.OtherNotes',
							label: '10.60. Other Notes',
						},
						{
							type: 'doc',
							id: 'topics/10.61.Log4j',
							label: '10.61. Using LOG4J for logging',
						},
						{
							type: 'doc',
							id: 'topics/10.62.RestServerManagementLogging',
							label: '10.62. Management / Logging Surface (juneau-rest-server-management-logging)',
						},
					],
					link: {
						type: 'doc',
						id: 'topics/10.juneau-rest-server',
					},
				},
				{
					type: 'doc',
					id: 'topics/11.juneau-rest-server-mcp',
					label: '11. juneau-rest-server-mcp',
				},
				{
					type: 'category',
					label: '12. juneau-rest-server-springboot',
					collapsed: true,
					items: [
						{
							type: 'doc',
							id: 'topics/12.01.SpringBootOverview',
							label: '12.1. Spring Boot Overview',
						},
					],
					link: {
						type: 'doc',
						id: 'topics/12.juneau-rest-server-springboot',
					},
				},
				{
					type: 'category',
					label: '13. juneau-rest-client',
					collapsed: true,
					items: [
						{
							type: 'doc',
							id: 'topics/13.01.PojoMarshalling',
							label: '13.1. POJO Marshalling',
						},
						{
							type: 'doc',
							id: 'topics/13.02.RequestParts',
							label: '13.2. Request Parts',
						},
						{
							type: 'doc',
							id: 'topics/13.03.RequestContent',
							label: '13.3. Request Content',
						},
						{
							type: 'doc',
							id: 'topics/13.04.ResponseStatus',
							label: '13.4. Response Status',
						},
						{
							type: 'doc',
							id: 'topics/13.05.ResponseHeaders',
							label: '13.5. Response Headers',
						},
						{
							type: 'doc',
							id: 'topics/13.06.ResponseContent',
							label: '13.6. Response Content',
						},
						{
							type: 'doc',
							id: 'topics/13.07.CustomCallHandlers',
							label: '13.7. Custom Call Handlers',
						},
						{
							type: 'doc',
							id: 'topics/13.08.Interceptors',
							label: '13.8. Interceptors',
						},
						{
							type: 'category',
							label: '13.9. REST Proxies',
							collapsed: true,
							link: { type: 'doc', id: 'topics/13.09.RestProxies' },
							items: [
								{
									type: 'doc',
									id: 'topics/13.09.01.Remote',
									label: '13.9.1. @Remote',
								},
								{
									type: 'doc',
									id: 'topics/13.09.02.RemoteMethod',
									label: '13.9.2. @RemoteOp',
								},
								{
									type: 'doc',
									id: 'topics/13.09.03.Content',
									label: '13.9.3. @Content',
								},
								{
									type: 'doc',
									id: 'topics/13.09.04.FormData',
									label: '13.9.4. @FormData',
								},
								{
									type: 'doc',
									id: 'topics/13.09.05.Query',
									label: '13.9.5. @Query',
								},
								{
									type: 'doc',
									id: 'topics/13.09.06.Header',
									label: '13.9.6. @Header',
								},
								{
									type: 'doc',
									id: 'topics/13.09.07.Path',
									label: '13.9.7. @Path',
								},
								{
									type: 'doc',
									id: 'topics/13.09.08.Request',
									label: '13.9.8. @Request',
								},
								{
									type: 'doc',
									id: 'topics/13.09.09.Response',
									label: '13.9.9. @Response',
								},
								{
									type: 'doc',
									id: 'topics/13.09.10.DualPurposeInterfaces',
									label: '13.9.10. Dual-purpose (end-to-end) interfaces',
								},
							],
						},
						{
							type: 'doc',
							id: 'topics/13.10.RestClientLoggingAndDebugging',
							label: '13.10. Logging and Debugging',
						},
						{
							type: 'doc',
							id: 'topics/13.11.CustomizingHttpClient',
							label: '13.11. Customizing HttpClient',
						},
						{
							type: 'doc',
							id: 'topics/13.12.ExtendingRestClient',
							label: '13.12. Extending RestClient',
						},
						{
							type: 'category',
							label: '13.13. Authentication',
							collapsed: true,
							link: { type: 'doc', id: 'topics/13.13.Authentication' },
							items: [
								{
									type: 'doc',
									id: 'topics/13.13.01.AuthenticationBASIC',
									label: '13.13.1. BASIC Authentication',
								},
								{
									type: 'doc',
									id: 'topics/13.13.02.AuthenticationForm',
									label: '13.13.2. FORM-based Authentication',
								},
								{
									type: 'doc',
									id: 'topics/13.13.03.AuthenticationOIDC',
									label: '13.13.3. OIDC Authentication',
								},
							],
						},
						{
							type: 'doc',
							id: 'topics/13.14.NextGenRestClient',
							label: '13.14. Next-Generation REST Client (Beta)',
						},
						{
							type: 'category',
							label: '13.15. HTTP Transport Adapters',
							collapsed: true,
							link: { type: 'doc', id: 'topics/13.15.HttpTransportAdapters' },
							items: [
								{
									type: 'doc',
									id: 'topics/13.15.01.RestClientApacheHttpClient45',
									label: '13.15.1. Apache HttpClient 4.5 Transport',
								},
								{
									type: 'doc',
									id: 'topics/13.15.02.RestClientApacheHttpClient50',
									label: '13.15.2. Apache HttpClient 5.0 Transport',
								},
								{
									type: 'doc',
									id: 'topics/13.15.03.RestClientOkHttp',
									label: '13.15.3. OkHttp Transport',
								},
								{
									type: 'doc',
									id: 'topics/13.15.04.RestClientJetty',
									label: '13.15.4. Jetty HTTP Client Transport',
								},
							],
						},
						{
							type: 'doc',
							id: 'topics/13.16.StreamingCursors',
							label: '13.16. Streaming Cursors',
						},
						{
							type: 'doc',
							id: 'topics/13.17.ContentTypeNegotiation',
							label: '13.17. Content-Type Negotiation',
						},
					],
					link: {
						type: 'doc',
						id: 'topics/13.juneau-rest-client',
					},
				},
				{
					type: 'category',
					label: '14. juneau-rest-mock',
					collapsed: true,
					items: [
						{
							type: 'doc',
							id: 'topics/14.01.MockRestClientOverview',
							label: '14.1. Mock REST Client Overview',
						},
					],
					link: {
						type: 'doc',
						id: 'topics/14.juneau-rest-mock',
					},
				},
					],
				},
				{
					type: 'category',
					label: 'Microservices',
					collapsed: false,
					items: [
				{
					type: 'category',
					label: '15. juneau-microservice',
					collapsed: true,
					items: [
						{
							type: 'doc',
							id: 'topics/15.01.MicroserviceOverview',
							label: '15.1. Microservice Overview',
						},
						{
							type: 'doc',
							id: 'topics/15.02.Args',
							label: '15.2. Args',
						},
						{
							type: 'doc',
							id: 'topics/15.03.Manifest',
							label: '15.3. Manifest',
						},
						{
							type: 'doc',
							id: 'topics/15.04.SystemProperties',
							label: '15.4. System properties',
						},
						{
							type: 'doc',
							id: 'topics/15.05.VarResolver',
							label: '15.5. VarResolver',
						},
						{
							type: 'doc',
							id: 'topics/15.06.ConsoleCommands',
							label: '15.6. Console Commands',
						},
						{
							type: 'doc',
							id: 'topics/15.07.MicroserviceCoreListeners',
							label: '15.7. Listeners',
						},
						{
							type: 'doc',
							id: 'topics/15.08.InjectAwareMicroservice',
							label: '15.8. Inject-Aware Microservice',
						},
						{
							type: 'doc',
							id: 'topics/15.09.MicroserviceTesting',
							label: '15.9. Whole-Microservice Integration Tests (@MicroserviceTest)',
						},
					],
					link: {
						type: 'doc',
						id: 'topics/15.juneau-microservice',
					},
				},
				{
					type: 'category',
					label: '16. juneau-microservice-jetty',
					collapsed: true,
					items: [
						{
							type: 'doc',
							id: 'topics/16.01.MicroserviceJettyOverview',
							label: '16.1. Microservice Jetty Overview',
						},
						{
							type: 'doc',
							id: 'topics/16.02.LifecycleMethods',
							label: '16.2. Lifecycle Methods',
						},
						{
							type: 'doc',
							id: 'topics/16.03.ResourceClasses',
							label: '16.3. Resource Classes',
						},
						{
							type: 'doc',
							id: 'topics/16.04.PredefinedResourceClasses',
							label: '16.4. Predefined Resource Classes',
						},
						{
							type: 'doc',
							id: 'topics/16.05.Config',
							label: '16.5. Config',
						},
						{
							type: 'doc',
							id: 'topics/16.06.JettyXml',
							label: '16.6. Jetty.xml file',
						},
						{
							type: 'doc',
							id: 'topics/16.07.UiCustomization',
							label: '16.7. UI Customization',
						},
						{
							type: 'doc',
							id: 'topics/16.08.Extending',
							label: '16.8. Customizing via @Bean',
						},
						{
							type: 'doc',
							id: 'topics/16.09.HealthProbes',
							label: '16.9. Health / Readiness / Liveness Probes',
						},
						{
							type: 'doc',
							id: 'topics/16.10.GracefulShutdown',
							label: '16.10. Graceful Shutdown & Readiness Gating',
						},
						{
							type: 'doc',
							id: 'topics/16.11.ManagementSurface',
							label: '16.11. Management Surface (Actuator-style endpoints)',
						},
					],
					link: {
						type: 'doc',
						id: 'topics/16.juneau-microservice-jetty',
					},
				},
				{
					type: 'doc',
					id: 'topics/17.juneau-microservice-tomcat',
					label: '17. juneau-microservice-tomcat',
				},
				{
					type: 'category',
					label: '18. juneau-sc',
					collapsed: true,
					items: [
						{
							type: 'doc',
							id: 'topics/18.01.ScServerOverview',
							label: '18.1. juneau-sc-server',
						},
						{
							type: 'doc',
							id: 'topics/18.02.ScClientOverview',
							label: '18.2. juneau-sc-client',
						},
					],
					link: {
						type: 'doc',
						id: 'topics/18.juneau-sc',
					},
				},
					],
				},
				{
					type: 'category',
					label: 'Examples & Showcases',
					collapsed: false,
					items: [
						{
							type: 'doc',
							id: 'topics/19.juneau-petstore',
							label: '19. juneau-petstore',
						},
						{
							type: 'doc',
							id: 'topics/20.juneau-examples',
							label: '20. juneau-examples',
						},
					],
				},
				{
					type: 'category',
					label: 'Packaging & Deployment',
					collapsed: false,
					items: [
						{
							type: 'category',
							label: '21. Dependency Management',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/21.01.NativeImageAndLayeredJars',
									label: '21.1. GraalVM Native Image & Docker Layering',
								},
							],
							link: {
								type: 'doc',
								id: 'topics/21.DependencyManagement',
							},
						},
						{
							type: 'category',
							label: '23. juneau-shaded',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/23.01.JuneauShadedCore',
									label: '23.1. juneau-shaded-core',
								},
								{
									type: 'doc',
									id: 'topics/23.02.JuneauShadedRestClient',
									label: '23.2. juneau-shaded-rest-client',
								},
								{
									type: 'doc',
									id: 'topics/23.03.JuneauShadedRestServer',
									label: '23.3. juneau-shaded-rest-server',
								},
								{
									type: 'doc',
									id: 'topics/23.04.JuneauShadedRestServerSpringboot',
									label: '23.4. juneau-shaded-rest-server-springboot',
								},
								{
									type: 'doc',
									id: 'topics/23.05.JuneauShadedAll',
									label: '23.5. juneau-shaded-all',
								},
							],
							link: {
								type: 'doc',
								id: 'topics/23.juneau-shaded',
							},
						},
						{
							type: 'doc',
							id: 'topics/24.StarterProjects',
							label: '24. Starter Projects',
						},
					],
				},
				{
					type: 'category',
					label: 'Guides & Reference',
					collapsed: false,
					items: [
						{
							type: 'category',
							label: '25. Security',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/25.01.MarshallingSecurity',
									label: '25.1. Marshalling Security',
								},
								{
									type: 'doc',
									id: 'topics/25.02.SvlSecurity',
									label: '25.2. SVL Security',
								},
								{
									type: 'doc',
									id: 'topics/25.03.RestSecurity',
									label: '25.3. REST Security',
								},
								{
									type: 'doc',
									id: 'topics/25.04.ContentSecurityPolicy',
									label: '25.4. Content Security Policy',
								},
							],
							link: {
								type: 'doc',
								id: 'topics/25.Security',
							},
						},
						{
							type: 'doc',
							id: 'topics/26.V9MigrationGuide',
							label: '26. V9.0 Migration Guide',
						},
						{
							type: 'doc',
							id: 'topics/27.V10MigrationGuide',
							label: '27. V10.0 Migration Guide',
						},
					],
				},
			],
		},
		{
			type: 'category',
			label: 'Developer Info',
			collapsed: false,
			items: [
				{
					type: 'doc',
					id: 'developer-info/01.DeveloperLinks',
					label: '1. Developer Links',
				},
				{
					type: 'category',
					label: '2. How-to Articles',
					collapsed: false,
					items: [
						{
							type: 'doc',
							id: 'developer-info/02.01.BecomingAContributor',
							label: '2.1. Becoming a contributor',
						},
						{
							type: 'doc',
							id: 'developer-info/02.02.NonCommittersContribute',
							label: '2.2. How non-committers can contribute code',
						},
						{
							type: 'doc',
							id: 'developer-info/02.03.NewMemberGuidelines',
							label: '2.3. New member guidelines',
						},
						{
							type: 'doc',
							id: 'developer-info/02.04.NewReleaseGuidelines',
							label: '2.4. New release guidelines',
						},
						{
							type: 'doc',
							id: 'developer-info/02.05.VersioningGuidelines',
							label: '2.5. Versioning guidelines',
						},
						{
							type: 'doc',
							id: 'developer-info/02.06.CodeFormattingStylesheet',
							label: '2.6. Using the code formatting stylesheet',
						},
						{
							type: 'doc',
							id: 'developer-info/02.08.DevelopmentWishList',
							label: '2.7. Development wish list',
						},
						{
							type: 'doc',
							id: 'developer-info/02.09.AiKnowledgeAuthoring',
							label: '2.8. AI Knowledge Authoring Guide',
						},
					],
				},
				{
					type: 'doc',
					id: 'developer-info/03.CurrentMembers',
					label: '3. Current Members',
				},
				{
					type: 'category',
					label: '4. Scripts',
					collapsed: false,
					items: [
						{
							type: 'doc',
							id: 'developer-info/04.01.Scripts',
							label: '4.1. Scripts',
						},
						{
							type: 'category',
							label: '4.2. Code Scripts',
							collapsed: false,
							items: [
								{
									type: 'doc',
									id: 'developer-info/04.02.01.ScriptTest',
									label: '4.2.1. test.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.02.02.ScriptPush',
									label: '4.2.2. push.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.02.03.ScriptRelease',
									label: '4.2.3. release.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.02.04.ScriptPromptPgpPassphrase',
									label: '4.2.4. prompt-pgp-passphrase.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.02.05.ScriptCurrentRelease',
									label: '4.2.5. current-release.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.02.06.ScriptMavenVersion',
									label: '4.2.6. maven-version.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.02.07.ScriptApplyEclipsePrefs',
									label: '4.2.7. apply-eclipse-prefs.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.02.08.ScriptCleanupWhitespace',
									label: '4.2.8. cleanup-whitespace.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.02.09.ScriptCheckFluentSetterOverrides',
									label: '4.2.9. check-fluent-setter-overrides.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.02.10.ScriptRevertStaged',
									label: '4.2.10. revert-staged.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.02.11.ScriptRevertUnstaged',
									label: '4.2.11. revert-unstaged.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.02.12.ScriptStartPetstoreJetty',
									label: '4.2.12. start-petstore-jetty.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.02.13.ScriptStartPetstoreSpringboot',
									label: '4.2.13. start-petstore-springboot.py',
								},
							],
						},
						{
							type: 'category',
							label: '4.3. Doc Scripts',
							collapsed: false,
							items: [
								{
									type: 'doc',
									id: 'developer-info/04.03.01.ScriptReleaseDocsStage',
									label: '4.3.1. release-docs-stage.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.03.02.ScriptReleaseDocs',
									label: '4.3.2. release-docs.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.03.03.ScriptBuildDocs',
									label: '4.3.3. build-docs.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.03.04.ScriptCreateMvnSite',
									label: '4.3.4. create-mvn-site.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.03.05.ScriptStartDocusaurus',
									label: '4.3.5. start-docusaurus.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.03.06.ScriptCheckTopicLinks',
									label: '4.3.6. check-topic-links.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.03.07.ScriptAiArtifacts',
									label: '4.3.7. generate-ai-artifacts.py / check-ai-artifacts.py',
								},
							],
						},
					],
				},
				{
					type: 'doc',
					id: 'developer-info/05.ArticlesIndex',
					label: '5. Articles',
				},
			],
		},
		{
			type: 'category',
			label: 'Release Notes',
			collapsed: true,
			items: [
				{
					type: 'category',
					label: 'Version 10.x',
					items: [
						{
							type: 'doc',
							id: 'release-notes/10.0.0',
							label: '10.0.0',
						},
					],
				},
				{
					type: 'category',
					label: 'Version 9.x',
					items: [
						{
							type: 'doc',
							id: 'release-notes/9.5.0',
							label: '9.5.0',
						},
						{
							type: 'doc',
							id: 'release-notes/9.2.0',
							label: '9.2.0',
						},
						{
							type: 'doc',
							id: 'release-notes/9.1.0',
							label: '9.1.0',
						},
						{
							type: 'doc',
							id: 'release-notes/9.0.1',
							label: '9.0.1',
						},
						{
							type: 'doc',
							id: 'release-notes/9.0.0',
							label: '9.0.0',
						},
					],
				},
				{
					type: 'category',
					label: 'Version 8.x',
					items: [
						{
							type: 'doc',
							id: 'release-notes/8.2.0',
							label: '8.2.0',
						},
						{
							type: 'doc',
							id: 'release-notes/8.1.3',
							label: '8.1.3',
						},
						{
							type: 'doc',
							id: 'release-notes/8.1.2',
							label: '8.1.2',
						},
						{
							type: 'doc',
							id: 'release-notes/8.1.1',
							label: '8.1.1',
						},
						{
							type: 'doc',
							id: 'release-notes/8.1.0',
							label: '8.1.0',
						},
						{
							type: 'doc',
							id: 'release-notes/8.0.0',
							label: '8.0.0',
						},
					],
				},
				{
					type: 'category',
					label: 'Version 7.x',
					items: [
						{
							type: 'doc',
							id: 'release-notes/7.2.2',
							label: '7.2.2',
						},
						{
							type: 'doc',
							id: 'release-notes/7.2.1',
							label: '7.2.1',
						},
						{
							type: 'doc',
							id: 'release-notes/7.2.0',
							label: '7.2.0',
						},
						{
							type: 'doc',
							id: 'release-notes/7.1.0',
							label: '7.1.0',
						},
						{
							type: 'doc',
							id: 'release-notes/7.0.1',
							label: '7.0.1',
						},
						{
							type: 'doc',
							id: 'release-notes/7.0.0',
							label: '7.0.0',
						},
					],
				},
				{
					type: 'category',
					label: 'Version 6.x',
					items: [
						{
							type: 'doc',
							id: 'release-notes/6.4.0',
							label: '6.4.0',
						},
						{
							type: 'doc',
							id: 'release-notes/6.3.1',
							label: '6.3.1',
						},
						{
							type: 'doc',
							id: 'release-notes/6.3.0',
							label: '6.3.0',
						},
						{
							type: 'doc',
							id: 'release-notes/6.2.0',
							label: '6.2.0',
						},
						{
							type: 'doc',
							id: 'release-notes/6.1.0',
							label: '6.1.0',
						},
						{
							type: 'doc',
							id: 'release-notes/6.0.1',
							label: '6.0.1',
						},
						{
							type: 'doc',
							id: 'release-notes/6.0.0',
							label: '6.0.0',
						},
					],
				},
				{
					type: 'category',
					label: 'Version 5.x',
					items: [
						{
							type: 'doc',
							id: 'release-notes/5.2.0.1',
							label: '5.2.0.1',
						},
						{
							type: 'doc',
							id: 'release-notes/5.2.0.0',
							label: '5.2.0.0',
						},
						{
							type: 'doc',
							id: 'release-notes/5.1.0.20',
							label: '5.1.0.20',
						},
						{
							type: 'doc',
							id: 'release-notes/5.1.0.19',
							label: '5.1.0.19',
						},
						{
							type: 'doc',
							id: 'release-notes/5.1.0.18',
							label: '5.1.0.18',
						},
						{
							type: 'doc',
							id: 'release-notes/5.1.0.17',
							label: '5.1.0.17',
						},
						{
							type: 'doc',
							id: 'release-notes/5.1.0.16',
							label: '5.1.0.16',
						},
						{
							type: 'doc',
							id: 'release-notes/5.1.0.15',
							label: '5.1.0.15',
						},
						{
							type: 'doc',
							id: 'release-notes/5.1.0.14',
							label: '5.1.0.14',
						},
						{
							type: 'doc',
							id: 'release-notes/5.1.0.13',
							label: '5.1.0.13',
						},
						{
							type: 'doc',
							id: 'release-notes/5.1.0.12',
							label: '5.1.0.12',
						},
						{
							type: 'doc',
							id: 'release-notes/5.1.0.11',
							label: '5.1.0.11',
						},
						{
							type: 'doc',
							id: 'release-notes/5.1.0.10',
							label: '5.1.0.10',
						},
						{
							type: 'doc',
							id: 'release-notes/5.1.0.09',
							label: '5.1.0.09',
						},
						{
							type: 'doc',
							id: 'release-notes/5.1.0.08',
							label: '5.1.0.08',
						},
						{
							type: 'doc',
							id: 'release-notes/5.1.0.07',
							label: '5.1.0.07',
						},
						{
							type: 'doc',
							id: 'release-notes/5.1.0.06',
							label: '5.1.0.06',
						},
						{
							type: 'doc',
							id: 'release-notes/5.1.0.05',
							label: '5.1.0.05',
						},
						{
							type: 'doc',
							id: 'release-notes/5.1.0.04',
							label: '5.1.0.04',
						},
						{
							type: 'doc',
							id: 'release-notes/5.1.0.03',
							label: '5.1.0.03',
						},
						{
							type: 'doc',
							id: 'release-notes/5.1.0.02',
							label: '5.1.0.02',
						},
						{
							type: 'doc',
							id: 'release-notes/5.1.0.01',
							label: '5.1.0.01',
						},
						{
							type: 'doc',
							id: 'release-notes/5.1.0.00',
							label: '5.1.0.00',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.36',
							label: '5.0.0.36',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.35',
							label: '5.0.0.35',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.34',
							label: '5.0.0.34',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.33',
							label: '5.0.0.33',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.32',
							label: '5.0.0.32',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.31',
							label: '5.0.0.31',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.30',
							label: '5.0.0.30',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.29',
							label: '5.0.0.29',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.28',
							label: '5.0.0.28',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.27',
							label: '5.0.0.27',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.26',
							label: '5.0.0.26',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.25',
							label: '5.0.0.25',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.24',
							label: '5.0.0.24',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.23',
							label: '5.0.0.23',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.22',
							label: '5.0.0.22',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.21',
							label: '5.0.0.21',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.20',
							label: '5.0.0.20',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.19',
							label: '5.0.0.19',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.18',
							label: '5.0.0.18',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.17',
							label: '5.0.0.17',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.16',
							label: '5.0.0.16',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.15',
							label: '5.0.0.15',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.14',
							label: '5.0.0.14',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.13',
							label: '5.0.0.13',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.12',
							label: '5.0.0.12',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.11',
							label: '5.0.0.11',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.10',
							label: '5.0.0.10',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.09',
							label: '5.0.0.09',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.08',
							label: '5.0.0.08',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.07',
							label: '5.0.0.07',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.06',
							label: '5.0.0.06',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.05',
							label: '5.0.0.05',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.04',
							label: '5.0.0.04',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.03',
							label: '5.0.0.03',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.02',
							label: '5.0.0.02',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.01',
							label: '5.0.0.01',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.00',
							label: '5.0.0.00',
						},
					],
				},
			],
		},
	],
};

export default sidebars;
