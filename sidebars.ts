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

// Loose shape: the helpers below only touch these fields.
type Node = {
	type: string;
	label?: string;
	items?: Node[];
	customProps?: Record<string, unknown>;
	[key: string]: unknown;
};

// Marks an unnumbered divider heading; its children continue the parent's count.
function group(category: Node): Node {
	return {...category, customProps: {...category.customProps, unnumbered: true}};
}

// Prefixes position-based numbers ("2.2.1. Label"); throws on any labelless doc, ref or category.
function numbered(items: Node[], prefix = ''): Node[] {
	let n = 0;
	const visit = (list: Node[]): Node[] => list.map((item) => {
		if (item.type === 'category' && item.customProps?.unnumbered === true) {
			return {...item, items: visit(item.items ?? [])};
		}
		if (item.type !== 'doc' && item.type !== 'ref' && item.type !== 'category') {
			throw new Error(`numbered(): unsupported item type '${item.type}'`);
		}
		if (!item.label) {
			throw new Error(`numbered(): missing label on ${JSON.stringify(item).slice(0, 120)}`);
		}
		const num = prefix ? `${prefix}.${++n}` : `${++n}`;
		const label = `${num}. ${item.label}`;
		return item.type === 'category' ? {...item, label, items: numbered(item.items ?? [], num)} : {...item, label};
	});
	return visit(items);
}

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
			label: 'Getting Started',
			collapsed: false,
			link: {
				type: 'doc',
				id: 'topics/00.GettingStarted',
			},
			items: numbered([
				{
					type: 'doc',
					id: 'topics/00.01.GettingStartedInstall',
					label: 'Install Juneau',
				},
				{
					type: 'doc',
					id: 'topics/00.02.GettingStartedSerializer',
					label: 'Your First Serializer',
				},
				{
					type: 'doc',
					id: 'topics/00.03.GettingStartedRestApp',
					label: 'Your First REST App',
				},
				{
					type: 'doc',
					id: 'topics/00.04.GettingStartedClient',
					label: 'Your First Client',
				},
			]),
		},
		{
			type: 'category',
			label: 'Guides',
			collapsed: true,
			link: {
				type: 'doc',
				id: 'guides/00.Guides',
			},
			items: numbered([
				{
					type: 'doc',
					id: 'guides/01.SpringBootRestApi',
					label: 'Build a REST API on Spring Boot',
				},
				{
					type: 'doc',
					id: 'guides/02.SecureAnEndpoint',
					label: 'Secure an endpoint with JWT and OAuth',
				},
				{
					type: 'doc',
					id: 'guides/03.TestAnEndpoint',
					label: 'Test an endpoint with MockRestClient',
				},
				{
					type: 'doc',
					id: 'guides/04.TypedProxyClient',
					label: 'Call an existing REST API with a typed proxy',
				},
				{
					type: 'doc',
					id: 'guides/05.CustomSwaps',
					label: 'Serialize a type Juneau doesn\'t know',
				},
				{
					type: 'doc',
					id: 'guides/06.ExternalizeConfig',
					label: 'Externalize config with INI/YAML and variables',
				},
				{
					type: 'doc',
					id: 'guides/07.OpenApiDocs',
					label: 'Generate OpenAPI docs for your API',
				},
				{
					type: 'ref',
					id: 'topics/10.62.01.BuildingAConsolePage',
					label: 'Building a Console Page',
				},
				{
					type: 'ref',
					id: 'topics/11.01.JuneauMcpQuickstart',
					label: 'MCP Quickstart',
				},
			]),
		},
		{
			type: 'category',
			label: 'Documentation',
			collapsed: true,
			items: numbered([
				{
					type: 'category',
					label: 'About Juneau',
					collapsed: true,
					link: {
						type: 'doc',
						id: 'topics/01.JuneauEcosystemOverview',
					},
					items: [
						{
							type: 'doc',
							id: 'topics/01.01.FrameworkComparisons',
							label: 'Framework Comparisons',
						},
						{
							type: 'doc',
							id: 'topics/01.02.WhyJuneau',
							label: 'Why Choose Juneau?',
						},
					],
				},
				{
					type: 'category',
					label: 'Core & Data',
					collapsed: true,
					items: [
						{
							type: 'category',
							label: 'juneau-commons',
							collapsed: true,
							link: {
								type: 'doc',
								id: 'topics/02.juneau-commons',
							},
							items: [
								{
									type: 'doc',
									id: 'topics/02.01.JuneauCommonsUtils',
									label: 'Utils Package',
								},
								{
									type: 'doc',
									id: 'topics/02.02.JuneauCommonsCollections',
									label: 'Collections Package',
								},
								{
									type: 'doc',
									id: 'topics/02.03.JuneauCommonsLang',
									label: 'Lang Package',
								},
								{
									type: 'doc',
									id: 'topics/02.04.JuneauCommonsReflection',
									label: 'Reflection Package',
								},
								{
									type: 'doc',
									id: 'topics/02.05.JuneauCommonsBeans',
									label: 'Beans Package',
								},
								{
									type: 'doc',
									id: 'topics/02.06.JuneauCommonsSettings',
									label: 'Settings Package',
								},
								{
									type: 'doc',
									id: 'topics/02.07.JuneauCommonsFunction',
									label: 'Function Package',
								},
								{
									type: 'doc',
									id: 'topics/02.08.JuneauCommonsConversion',
									label: 'Conversion Package',
								},
								{
									type: 'doc',
									id: 'topics/02.09.JuneauCommonsIO',
									label: 'I/O Package',
								},
								{
									type: 'category',
									label: 'Inject Package',
									collapsed: true,
									link: {
										type: 'doc',
										id: 'topics/02.10.JuneauCommonsInject',
									},
									items: [
										{
											type: 'doc',
											id: 'topics/02.10.01.ValueAnnotation',
											label: '@Value Annotation Basics',
										},
										{
											type: 'doc',
											id: 'topics/02.10.02.ValueFrameworkInternal',
											label: '@Value Framework-Internal Adoption',
										},
										{
											type: 'doc',
											id: 'topics/02.10.03.ConfigProperties',
											label: '@ConfigProperties Annotation Basics',
										},
									],
								},
								{
									type: 'doc',
									id: 'topics/02.11.SimpleVariableLanguage',
									label: 'Simple Variable Language',
								},
								{
									type: 'doc',
									id: 'topics/02.12.JuneauCommonsHttp',
									label: 'Http Package',
								},
								{
									type: 'doc',
									id: 'topics/02.13.JuneauCommonsHttpPart',
									label: 'Http-Part Package',
								},
								{
									type: 'doc',
									id: 'topics/02.14.JuneauCommonsConcurrent',
									label: 'Concurrent Package',
								},
								{
									type: 'doc',
									id: 'topics/02.15.JuneauCommonsLogging',
									label: 'Logging Package',
								},
								{
									type: 'doc',
									id: 'topics/02.16.JuneauCommonsRuntime',
									label: 'Runtime Package',
								},
								{
									type: 'doc',
									id: 'topics/02.17.JuneauCommonsTime',
									label: 'Time Package',
								},
								{
									type: 'doc',
									id: 'topics/02.18.JuneauCommonsBeanQuery',
									label: 'BeanQuery Package',
								},
							],
						},
						{
							type: 'category',
							label: 'juneau-marshall',
							collapsed: true,
							items: [
								group({
									type: 'category',
									label: 'Core Concepts',
									collapsed: true,
									link: {
										type: 'generated-index',
										description: 'Marshallers, serializers and parsers, bean contexts and context settings.',
									},
									items: [
										{
											type: 'doc',
											id: 'topics/03.01.Marshallers',
											label: 'Marshallers',
										},
										{
											type: 'doc',
											id: 'topics/03.02.SerializersAndParsers',
											label: 'Serializers and Parsers',
										},
										{
											type: 'category',
											label: 'Bean Contexts',
											collapsed: true,
											link: {
												type: 'doc',
												id: 'topics/03.03.BeanContexts',
											},
											items: [
												{
													type: 'doc',
													id: 'topics/03.03.01.JavaBeansSupport',
													label: 'Java Beans Support',
												},
												{
													type: 'doc',
													id: 'topics/03.03.02.JavaRecordsSupport',
													label: 'Java Records Support',
												},
												{
													type: 'doc',
													id: 'topics/03.03.03.BeanTypeAnnotation',
													label: '@BeanType Annotation',
												},
												{
													type: 'doc',
													id: 'topics/03.03.04.BeanPropAnnotation',
													label: '@BeanProp Annotation',
												},
												{
													type: 'doc',
													id: 'topics/03.03.05.BeanCtorAnnotation',
													label: '@BeanCtor Annotation',
												},
												{
													type: 'doc',
													id: 'topics/03.03.06.BeanIgnoreAnnotation',
													label: '@BeanIgnore Annotation',
												},
												{
													type: 'doc',
													id: 'topics/03.03.07.NamePropertyAnnotation',
													label: '@NameProperty Annotation',
												},
												{
													type: 'doc',
													id: 'topics/03.03.08.ParentPropertyAnnotation',
													label: '@ParentProperty Annotation',
												},
												{
													type: 'doc',
													id: 'topics/03.03.09.PojoBuilders',
													label: 'POJO Builders',
												},
												{
													type: 'doc',
													id: 'topics/03.03.10.BypassSerialization',
													label: 'Bypass Serialization using Readers and InputStreams',
												},
												{
													type: 'doc',
													id: 'topics/03.03.11.ViewProjection',
													label: 'View-Based Projection',
												},
												{
													type: 'doc',
													id: 'topics/03.03.12.MarshalledAnnotation',
													label: '@Marshalled Annotation',
												},
												{
													type: 'doc',
													id: 'topics/03.03.13.MarshalledAs',
													label: '@MarshalledAs Enum',
												},
												{
													type: 'doc',
													id: 'topics/03.03.14.MarshalledFilter',
													label: 'MarshalledFilter',
												},
												{
													type: 'doc',
													id: 'topics/03.03.15.ExampleAnnotation',
													label: '@Example Annotation',
												},
											],
										},
										{
											type: 'doc',
											id: 'topics/03.04.HttpPartSerializersParsers',
											label: 'HTTP Part Serializers and Parsers',
										},
										{
											type: 'doc',
											id: 'topics/03.05.ContextSettings',
											label: 'Context Settings',
										},
										{
											type: 'doc',
											id: 'topics/03.06.NullAndInclusionPolicies',
											label: 'Null & Inclusion Policies',
										},
										{
											type: 'doc',
											id: 'topics/03.07.ContextAnnotations',
											label: 'Context Annotations',
										},
										{
											type: 'doc',
											id: 'topics/03.08.JsonMap',
											label: 'JsonMap and JsonList',
										},
										{
											type: 'doc',
											id: 'topics/03.09.MarshalledNodeAndJsonPointer',
											label: 'Tree Model & RFC 6901 JSON-Pointer',
										},
										{
											type: 'doc',
											id: 'topics/03.10.ComplexDataTypes',
											label: 'Complex Data Types',
										},
										{
											type: 'doc',
											id: 'topics/03.11.SupportedJdkDatatypes',
											label: 'Supported JDK Datatypes',
										},
										{
											type: 'doc',
											id: 'topics/03.12.SerializerSetsParserSets',
											label: 'SerializerSets and ParserSets',
										},
									],
								}),
								group({
									type: 'category',
									label: 'Customization and Advanced Types',
									collapsed: true,
									link: {
										type: 'generated-index',
										description: 'Swaps, dictionaries, virtual beans, URIs, SVL and other customization topics.',
									},
									items: [
										{
											type: 'category',
											label: 'Swaps',
											collapsed: true,
											link: {
												type: 'doc',
												id: 'topics/03.13.Swaps',
											},
											items: [
												{
													type: 'doc',
													id: 'topics/03.13.01.DefaultSwaps',
													label: 'Default Swaps',
												},
												{
													type: 'doc',
													id: 'topics/03.13.02.AutoSwaps',
													label: 'Auto-detected swaps',
												},
												{
													type: 'doc',
													id: 'topics/03.13.03.OneWaySwaps',
													label: 'One-way Swaps',
												},
												{
													type: 'doc',
													id: 'topics/03.13.04.SwapAnnotation',
													label: '@Swap Annotation',
												},
												{
													type: 'doc',
													id: 'topics/03.13.05.TemplatedSwaps',
													label: 'Templated Swaps',
												},
												{
													type: 'doc',
													id: 'topics/03.13.06.SurrogateClasses',
													label: 'Surrogate Classes',
												},
											],
										},
										{
											type: 'doc',
											id: 'topics/03.14.DynamicallyAppliedAnnotations',
											label: 'Dynamically Applied Annotations',
										},
										{
											type: 'category',
											label: 'Bean Dictionaries',
											collapsed: true,
											link: {
												type: 'doc',
												id: 'topics/03.15.BeanDictionaries',
											},
											items: [
												{
													type: 'doc',
													id: 'topics/03.15.01.BeanSubTypes',
													label: 'Bean Subtypes',
												},
											],
										},
										{
											type: 'doc',
											id: 'topics/03.16.VirtualBeans',
											label: 'Virtual Beans',
										},
										{
											type: 'doc',
											id: 'topics/03.17.Recursion',
											label: 'Non-Tree Models and Recursion Detection',
										},
										{
											type: 'doc',
											id: 'topics/03.18.ParsingIntoGenericModels',
											label: 'Parsing into Generic Models',
										},
										{
											type: 'doc',
											id: 'topics/03.19.MarshallingUris',
											label: 'URIs',
										},
										{
											type: 'category',
											label: 'Comparison with Jackson',
											collapsed: true,
											link: {
												type: 'doc',
												id: 'topics/03.20.JacksonComparison',
											},
											items: [
												{
													type: 'doc',
													id: 'topics/03.20.01.JpaAndHibernate',
													label: 'JPA and Hibernate',
												},
											],
										},
										{
											type: 'doc',
											id: 'topics/03.21.PojoCategories',
											label: 'POJO Categories',
										},
										{
											type: 'category',
											label: 'Simple Variable Language',
											collapsed: true,
											link: {
												type: 'doc',
												id: 'topics/03.22.MarshallSimpleVariableLanguage',
											},
											items: [
												{
													type: 'doc',
													id: 'topics/03.22.01.SvlVariables',
													label: 'SVL Variables',
												},
												{
													type: 'doc',
													id: 'topics/03.22.02.VarResolvers',
													label: 'VarResolvers and VarResolverSessions',
												},
												{
													type: 'doc',
													id: 'topics/03.22.03.DefaultVarResolver',
													label: 'VarResolver.DEFAULT',
												},
												{
													type: 'doc',
													id: 'topics/03.22.04.SvlOtherNotes',
													label: 'Other Notes',
												},
											],
										},
										{
											type: 'doc',
											id: 'topics/03.23.MarshallEncoders',
											label: 'Encoders',
										},
										{
											type: 'doc',
											id: 'topics/03.24.ObjectTools',
											label: 'Object Tools',
										},
									],
								}),
								group({
									type: 'category',
									label: 'Data Formats',
									collapsed: true,
									link: {
										type: 'generated-index',
										description: 'JSON, XML, HTML, UON, YAML, CSV and the other supported data formats.',
									},
									items: [
										{
											type: 'category',
											label: 'JSON Support',
											collapsed: true,
											link: {
												type: 'doc',
												id: 'topics/03.25.JsonSupport',
											},
											items: [
												{
													type: 'doc',
													id: 'topics/03.25.01.JsonMethodology',
													label: 'JSON Methodology',
												},
												{
													type: 'doc',
													id: 'topics/03.25.02.JsonSerializers',
													label: 'JSON Serializers',
												},
												{
													type: 'doc',
													id: 'topics/03.25.03.Json5',
													label: 'JSON 5',
												},
												{
													type: 'doc',
													id: 'topics/03.25.04.JsonParsers',
													label: 'JSON Parsers',
												},
												{
													type: 'doc',
													id: 'topics/03.25.05.JsonAnnotation',
													label: '@Json Annotation',
												},
												{
													type: 'doc',
													id: 'topics/03.25.06.JsonSchemaDetails',
													label: 'JSON-Schema Support',
												},
											],
										},
										{
											type: 'category',
											label: 'XML Support',
											collapsed: true,
											link: {
												type: 'doc',
												id: 'topics/03.26.XmlSupport',
											},
											items: [
												{
													type: 'doc',
													id: 'topics/03.26.01.XmlMethodology',
													label: 'XML Methodology',
												},
												{
													type: 'doc',
													id: 'topics/03.26.02.XmlSerializers',
													label: 'XML Serializers',
												},
												{
													type: 'doc',
													id: 'topics/03.26.03.XmlParsers',
													label: 'XML Parsers',
												},
												{
													type: 'doc',
													id: 'topics/03.26.04.XmlBeanTypeNameAnnotation',
													label: '@Marshalled(typeName) Annotation',
												},
												{
													type: 'category',
													label: '@Xml(format) Annotation',
													collapsed: true,
													link: {
														type: 'doc',
														id: 'topics/03.26.05.XmlFormatAnnotation',
													},
													items: [
														{
															type: 'doc',
															id: 'topics/03.26.05.01.XmlFormatAttrs',
															label: 'Attribute Formats',
														},
														{
															type: 'doc',
															id: 'topics/03.26.05.02.XmlFormatCollapsed',
															label: 'Collapsed Format',
														},
														{
															type: 'doc',
															id: 'topics/03.26.05.03.XmlFormatElements',
															label: 'Element & Mixed Formats',
														},
														{
															type: 'doc',
															id: 'topics/03.26.05.04.XmlFormatText',
															label: 'Text Formats',
														},
														{
															type: 'doc',
															id: 'topics/03.26.05.05.XmlFormatWhitespace',
															label: 'Whitespace Handling',
														},
													],
												},
												{
													type: 'doc',
													id: 'topics/03.26.06.XmlNamespaces',
													label: 'Namespaces',
												},
												{
													type: 'doc',
													id: 'topics/03.26.07.SoapXml',
													label: 'SOAP/XML Support',
												},
											],
										},
										{
											type: 'category',
											label: 'HTML Support',
											collapsed: true,
											link: {
												type: 'doc',
												id: 'topics/03.27.HtmlSupport',
											},
											items: [
												{
													type: 'doc',
													id: 'topics/03.27.01.HtmlMethodology',
													label: 'HTML Methodology',
												},
												{
													type: 'doc',
													id: 'topics/03.27.02.HtmlSerializers',
													label: 'HTML Serializers',
												},
												{
													type: 'doc',
													id: 'topics/03.27.03.HtmlParsers',
													label: 'HTML Parsers',
												},
												{
													type: 'doc',
													id: 'topics/03.27.04.HtmlAnnotation',
													label: '@Html Annotation',
												},
												{
													type: 'doc',
													id: 'topics/03.27.05.HtmlDocSerializer',
													label: 'HtmlDocSerializer',
												},
												{
													type: 'doc',
													id: 'topics/03.27.06.HtmlCustomTemplates',
													label: 'Custom Templates',
												},
												{
													type: 'doc',
													id: 'topics/03.27.07.HtmlSchemaSupport',
													label: 'HTML-Schema Support',
												},
											],
										},
										{
											type: 'category',
											label: 'UON Support',
											collapsed: true,
											link: {
												type: 'doc',
												id: 'topics/03.28.UonSupport',
											},
											items: [
												{
													type: 'doc',
													id: 'topics/03.28.01.UonSerializers',
													label: 'UON Serializers',
												},
												{
													type: 'doc',
													id: 'topics/03.28.02.UonParsers',
													label: 'UON Parsers',
												},
											],
										},
										{
											type: 'category',
											label: 'URL-Encoding Support',
											collapsed: true,
											link: {
												type: 'doc',
												id: 'topics/03.29.UrlEncodingSupport',
											},
											items: [
												{
													type: 'doc',
													id: 'topics/03.29.01.UrlEncSerializers',
													label: 'URL-Encoding Serializers',
												},
												{
													type: 'doc',
													id: 'topics/03.29.02.UrlEncParsers',
													label: 'URL-Encoding Parsers',
												},
												{
													type: 'doc',
													id: 'topics/03.29.03.UrlEncodingAnnotation',
													label: '@UrlEncoding Annotation',
												},
											],
										},
										{
											type: 'category',
											label: 'MessagePack Support',
											collapsed: true,
											link: {
												type: 'doc',
												id: 'topics/03.30.MessagePackSupport',
											},
											items: [
												{
													type: 'doc',
													id: 'topics/03.30.01.MsgPackSerializers',
													label: 'MessagePack Serializers',
												},
												{
													type: 'doc',
													id: 'topics/03.30.02.MsgPackParsers',
													label: 'MessagePack Parsers',
												},
											],
										},
										{
											type: 'category',
											label: 'OpenApi Support',
											collapsed: true,
											link: {
												type: 'doc',
												id: 'topics/03.31.OpenApiSupport',
											},
											items: [
												{
													type: 'doc',
													id: 'topics/03.31.01.OpenApiMethodology',
													label: 'OpenAPI Methodology',
												},
												{
													type: 'doc',
													id: 'topics/03.31.02.OpenApiSerializers',
													label: 'OpenAPI Serializers',
												},
												{
													type: 'doc',
													id: 'topics/03.31.03.OpenApiParsers',
													label: 'OpenAPI Parsers',
												},
											],
										},
										{
											type: 'doc',
											id: 'topics/03.32.TomlSupport',
											label: 'TOML Support',
										},
										{
											type: 'doc',
											id: 'topics/03.33.Prototext',
											label: 'Prototext (Protobuf Text Format)',
										},
										{
											type: 'doc',
											id: 'topics/03.34.Protobuf',
											label: 'Protobuf (Binary Format)',
										},
										{
											type: 'doc',
											id: 'topics/03.35.Parquet',
											label: 'Parquet Basics',
										},
										{
											type: 'category',
											label: 'YAML Support',
											collapsed: true,
											link: {
												type: 'doc',
												id: 'topics/03.36.YamlSupport',
											},
											items: [
												{
													type: 'doc',
													id: 'topics/03.36.01.YamlMethodology',
													label: 'YAML Methodology',
												},
												{
													type: 'doc',
													id: 'topics/03.36.02.YamlSerializers',
													label: 'YAML Serializers',
												},
												{
													type: 'doc',
													id: 'topics/03.36.03.YamlParsers',
													label: 'YAML Parsers',
												},
												{
													type: 'doc',
													id: 'topics/03.36.04.YamlAnnotation',
													label: '@YamlConfig Annotation',
												},
											],
										},
										{
											type: 'category',
											label: 'CSV Support',
											collapsed: true,
											link: {
												type: 'doc',
												id: 'topics/03.37.CsvSupport',
											},
											items: [
												{
													type: 'doc',
													id: 'topics/03.37.01.CsvSerializers',
													label: 'CSV Serializers',
												},
												{
													type: 'doc',
													id: 'topics/03.37.02.CsvParsers',
													label: 'CSV Parsers',
												},
											],
										},
										{
											type: 'doc',
											id: 'topics/03.38.Markdown',
											label: 'Markdown Support',
										},
										{
											type: 'category',
											label: 'JSONL Support',
											collapsed: true,
											link: {
												type: 'doc',
												id: 'topics/03.39.JsonlSupport',
											},
											items: [
												{
													type: 'doc',
													id: 'topics/03.39.01.Json5l',
													label: 'JSON5L Basics',
												},
											],
										},
										{
											type: 'doc',
											id: 'topics/03.40.Hjson',
											label: 'Hjson Support',
										},
										{
											type: 'doc',
											id: 'topics/03.41.Jcs',
											label: 'JCS Support',
										},
										{
											type: 'doc',
											id: 'topics/03.42.Bson',
											label: 'BSON Support',
										},
										{
											type: 'doc',
											id: 'topics/03.43.Cbor',
											label: 'CBOR Support',
										},
										{
											type: 'doc',
											id: 'topics/03.44.HoconSupport',
											label: 'HOCON Support',
										},
										{
											type: 'doc',
											id: 'topics/03.45.Ini',
											label: 'INI Basics',
										},
										{
											type: 'doc',
											id: 'topics/03.46.PlainText',
											label: 'PlainText Support',
										},
									],
								}),
								group({
									type: 'category',
									label: 'Streaming',
									collapsed: true,
									link: {
										type: 'generated-index',
										description: 'Server-Sent Events and token and record streaming.',
									},
									items: [
										{
											type: 'doc',
											id: 'topics/03.47.Sse',
											label: 'SSE Support',
										},
										{
											type: 'category',
											label: 'Token & Record Streaming',
											collapsed: true,
											link: {
												type: 'doc',
												id: 'topics/03.48.TokenRecordStreaming',
											},
											items: [
												{
													type: 'doc',
													id: 'topics/03.48.01.RecordStreaming',
													label: 'Record Streaming',
												},
												{
													type: 'doc',
													id: 'topics/03.48.02.TokenStreaming',
													label: 'Token Streaming',
												},
												{
													type: 'doc',
													id: 'topics/03.48.03.ArrayRecordStreaming',
													label: 'Array-Record Streaming',
												},
												{
													type: 'doc',
													id: 'topics/03.48.04.RestStreamingIntegration',
													label: 'REST Streaming Integration',
												},
												{
													type: 'doc',
													id: 'topics/03.48.05.ReadingContinuousStreams',
													label: 'Reading Continuous Streams',
												},
												{
													type: 'doc',
													id: 'topics/03.48.06.LargeDatasetStreaming',
													label: 'Large-Dataset Streaming',
												},
											],
										},
									],
								}),
								{
									type: 'doc',
									id: 'topics/03.49.BestPractices',
									label: 'Best Practices',
								},
							],
							link: {
								type: 'doc',
								id: 'topics/03.juneau-marshall',
							},
						},
						{
							type: 'category',
							label: 'juneau-marshall-rdf',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/04.01.Rdf',
									label: 'RDF Basics',
								},
								{
									type: 'doc',
									id: 'topics/04.02.RdfSerializers',
									label: 'RDF Serializers',
								},
								{
									type: 'doc',
									id: 'topics/04.03.RdfParsers',
									label: 'RDF Parsers',
								},
							],
							link: {
								type: 'doc',
								id: 'topics/04.juneau-marshall-rdf',
							},
						},
						{
							type: 'category',
							label: 'juneau-bean',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/05.01.JuneauBeanHtml5',
									label: 'juneau-bean-html5',
								},
								{
									type: 'doc',
									id: 'topics/05.02.JuneauBeanAtom',
									label: 'juneau-bean-atom',
								},
								{
									type: 'doc',
									id: 'topics/05.03.JuneauBeanJsonSchema',
									label: 'juneau-bean-jsonschema',
								},
								{
									type: 'doc',
									id: 'topics/05.04.JuneauBeanOpenApi3',
									label: 'juneau-bean-openapi-v3',
								},
								{
									type: 'doc',
									id: 'topics/05.05.JuneauBeanCommon',
									label: 'juneau-bean-common',
								},
								{
									type: 'doc',
									id: 'topics/05.06.JuneauBeanSwagger2',
									label: 'juneau-bean-swagger-v2',
								},
								{
									type: 'doc',
									id: 'topics/05.07.JuneauBeanMcp',
									label: 'juneau-bean-mcp-v20250618',
								},
								{
									type: 'doc',
									id: 'topics/05.08.JuneauBeanRfc7807',
									label: 'juneau-bean-rfc7807',
								},
								{
									type: 'doc',
									id: 'topics/05.09.JuneauBeanHal',
									label: 'juneau-bean-hal',
								},
								{
									type: 'doc',
									id: 'topics/05.10.JuneauBeanJsonApi',
									label: 'juneau-bean-jsonapi',
								},
								{
									type: 'doc',
									id: 'topics/05.11.JuneauBeanJsonPatch',
									label: 'juneau-bean-jsonpatch',
								},
								{
									type: 'doc',
									id: 'topics/05.12.JuneauBeanJsonRpc',
									label: 'juneau-bean-jsonrpc',
								},
							],
							link: {
								type: 'doc',
								id: 'topics/05.juneau-bean',
							},
						},
						{
							type: 'category',
							label: 'juneau-config',
							collapsed: true,
							items: [
								{
									type: 'category',
									label: 'Overview',
									collapsed: true,
									link: {
										type: 'doc',
										id: 'topics/06.01.Overview',
									},
									items: [
										{
											type: 'doc',
											id: 'topics/06.01.01.SyntaxRules',
											label: 'Syntax Rules',
										},
									],
								},
								{
									type: 'category',
									label: 'Reading Entries',
									collapsed: true,
									link: {
										type: 'doc',
										id: 'topics/06.02.ReadingEntries',
									},
									items: [
										{
											type: 'doc',
											id: 'topics/06.02.01.JCFObjects',
											label: 'Java Collection Framework Objects',
										},
									],
								},
								{
									type: 'category',
									label: 'Variables',
									collapsed: true,
									link: {
										type: 'doc',
										id: 'topics/06.03.Variables',
									},
									items: [
										{
											type: 'doc',
											id: 'topics/06.03.01.LogicVariables',
											label: 'Logic Variables',
										},
										{
											type: 'doc',
											id: 'topics/06.03.02.PropertySources',
											label: 'Property Sources',
										},
									],
								},
								{
									type: 'doc',
									id: 'topics/06.04.ModdedEntries',
									label: 'Modded/Encoded Entries',
								},
								{
									type: 'doc',
									id: 'topics/06.05.Sections',
									label: 'Sections',
								},
								{
									type: 'category',
									label: 'Setting Values',
									collapsed: true,
									link: {
										type: 'doc',
										id: 'topics/06.06.SettingValues',
									},
									items: [
										{
											type: 'doc',
											id: 'topics/06.06.01.FileSystemChanges',
											label: 'File System Changes',
										},
									],
								},
								{
									type: 'doc',
									id: 'topics/06.07.ConfigListeners',
									label: 'Listeners',
								},
								{
									type: 'doc',
									id: 'topics/06.08.Imports',
									label: 'Imports',
								},
								{
									type: 'category',
									label: 'Config Stores',
									collapsed: true,
									link: {
										type: 'doc',
										id: 'topics/06.09.ConfigStores',
									},
									items: [
										{
											type: 'doc',
											id: 'topics/06.09.01.ClasspathStore',
											label: 'ClasspathStore',
										},
									],
								},
								{
									type: 'doc',
									id: 'topics/06.10.ReadOnlyConfigs',
									label: 'Read-only Configs',
								},
								{
									type: 'doc',
									id: 'topics/06.11.ClosingConfigs',
									label: 'Closing Configs',
								},
								{
									type: 'doc',
									id: 'topics/06.12.SystemDefaultConfig',
									label: 'System Default Config',
								},
								{
									type: 'doc',
									id: 'topics/06.13.YamlConfigFiles',
									label: 'YAML Config Files',
								},
								{
									type: 'doc',
									id: 'topics/06.14.ConfigProfilesAndRelaxedBinding',
									label: 'Config Profiles & Relaxed Binding',
								},
								{
									type: 'doc',
									id: 'topics/06.15.IniConfigFormat',
									label: 'INI Config Format',
								},
							],
							link: {
								type: 'doc',
								id: 'topics/06.juneau-config',
							},
						},
						{
							type: 'category',
							label: 'juneau-bct',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/07.01.AssertionsOverview',
									label: 'Assertions Overview',
								},
								{
									type: 'category',
									label: 'Bean-Centric Testing',
									collapsed: true,
									link: {
										type: 'doc',
										id: 'topics/07.02.BeanCentricTesting',
									},
									items: [
										{
											type: 'doc',
											id: 'topics/07.02.01.CustomErrorMessages',
											label: 'Custom Error Messages',
										},
										{
											type: 'category',
											label: 'Customization',
											collapsed: true,
											link: {
												type: 'doc',
												id: 'topics/07.02.02.Customization',
											},
											items: [
												{
													type: 'doc',
													id: 'topics/07.02.02.01.Stringifiers',
													label: 'Stringifiers',
												},
												{
													type: 'doc',
													id: 'topics/07.02.02.02.Listifiers',
													label: 'Listifiers',
												},
												{
													type: 'doc',
													id: 'topics/07.02.02.03.Swappers',
													label: 'Swappers',
												},
											],
										},
										{
											type: 'doc',
											id: 'topics/07.02.03.PropertyExtractors',
											label: 'Property Extractors',
										},
									],
								},
								{
									type: 'doc',
									id: 'topics/07.03.JuneauBctJUnitExtensions',
									label: 'JUnit Extensions',
								},
							],
							link: {
								type: 'doc',
								id: 'topics/07.juneau-bct',
							},
						},
					],
				},
				{
					type: 'category',
					label: 'REST',
					collapsed: true,
					items: [
						{
							type: 'doc',
							id: 'topics/08.juneau-rest',
							label: 'juneau-rest',
						},
						{
							type: 'category',
							label: 'juneau-rest-common',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/09.01.HelperClasses',
									label: 'Helper Classes',
								},
								{
									type: 'doc',
									id: 'topics/09.02.Annotations',
									label: 'Annotations',
								},
								{
									type: 'doc',
									id: 'topics/09.03.HttpHeaders',
									label: 'HTTP Headers',
								},
								{
									type: 'doc',
									id: 'topics/09.04.HttpParts',
									label: 'HTTP Part Beans',
								},
								{
									type: 'doc',
									id: 'topics/09.05.HttpEntitiesAndResources',
									label: 'HTTP Entities and Resources',
								},
								{
									type: 'doc',
									id: 'topics/09.06.HttpResponses',
									label: 'HTTP Responses',
								},
								{
									type: 'doc',
									id: 'topics/09.07.RemoteProxyInterfaces',
									label: 'Remote Proxy Interfaces',
								},
							],
							link: {
								type: 'doc',
								id: 'topics/09.juneau-rest-common',
							},
						},
						{
							type: 'category',
							label: 'juneau-rest-server',
							collapsed: true,
							items: [
								group({
									type: 'category',
									label: 'Basics and Structure',
									collapsed: true,
									link: {
										type: 'generated-index',
										description: 'Annotated classes, operation methods, HTTP parts, marshalling and resource composition.',
									},
									items: [
										{
											type: 'doc',
											id: 'topics/10.01.RestServerOverview',
											label: 'REST Server Overview',
										},
										{
											type: 'category',
											label: '@Rest-Annotated Classes',
											collapsed: true,
											link: {
												type: 'doc',
												id: 'topics/10.02.RestAnnotatedClasses',
											},
											items: [
												{
													type: 'doc',
													id: 'topics/10.02.01.PredefinedClasses',
													label: 'Predefined Classes',
												},
												{
													type: 'doc',
													id: 'topics/10.02.02.ChildResources',
													label: 'Child Resources',
												},
												{
													type: 'doc',
													id: 'topics/10.02.03.PathVariables',
													label: 'Path Variables',
												},
												{
													type: 'doc',
													id: 'topics/10.02.04.Deployment',
													label: 'Deployment',
												},
												{
													type: 'doc',
													id: 'topics/10.02.05.LifecycleHooks',
													label: 'Lifecycle Hooks',
												},
												{
													type: 'doc',
													id: 'topics/10.02.06.RestAnnotationAttributes',
													label: '@Rest and @RestOp',
												},
											],
										},
										{
											type: 'category',
											label: '@RestOp-Annotated Methods',
											collapsed: true,
											link: {
												type: 'doc',
												id: 'topics/10.03.RestOpAnnotatedMethods',
											},
											items: [
												{
													type: 'doc',
													id: 'topics/10.03.01.InferredHttpMethodsAndPaths',
													label: 'Inferred HTTP Methods and Paths',
												},
												{
													type: 'doc',
													id: 'topics/10.03.02.JavaMethodParameters',
													label: 'Java Method Parameters',
												},
												{
													type: 'doc',
													id: 'topics/10.03.03.JavaMethodReturnTypes',
													label: 'Java Method Return Types',
												},
												{
													type: 'doc',
													id: 'topics/10.03.04.JavaMethodThrowableTypes',
													label: 'Java Method Throwable Types',
												},
												{
													type: 'doc',
													id: 'topics/10.03.05.PathPatterns',
													label: 'Path Patterns',
												},
												{
													type: 'doc',
													id: 'topics/10.03.06.Matchers',
													label: 'Matchers',
												},
												{
													type: 'doc',
													id: 'topics/10.03.07.OverloadingHttpMethods',
													label: 'Overloading HTTP Methods',
												},
											],
										},
										{
											type: 'category',
											label: 'HTTP parts in REST servers',
											collapsed: true,
											link: {
												type: 'doc',
												id: 'topics/10.04.RestServerHttpParts',
											},
											items: [
												{
													type: 'doc',
													id: 'topics/10.04.01.PartMarshallers',
													label: 'Part Marshallers',
												},
												{
													type: 'doc',
													id: 'topics/10.04.02.HttpPartAnnotations',
													label: 'HTTP Part Annotations',
												},
												{
													type: 'doc',
													id: 'topics/10.04.03.DefaultParts',
													label: 'Default Parts',
												},
												{
													type: 'doc',
													id: 'topics/10.04.04.RequestBeans',
													label: 'Request Beans',
												},
												{
													type: 'doc',
													id: 'topics/10.04.05.ResponseBeans',
													label: '@Response Beans',
												},
												{
													type: 'doc',
													id: 'topics/10.04.06.HttpPartValidation',
													label: 'HTTP Part Validation',
												},
											],
										},
										{
											type: 'doc',
											id: 'topics/10.05.Marshalling',
											label: 'Marshalling',
										},
										{
											type: 'doc',
											id: 'topics/10.06.HandlingFormPosts',
											label: 'Form Posts',
										},
										{
											type: 'doc',
											id: 'topics/10.07.RestServerComposition',
											label: 'Mixins and Multi-Mount Paths',
										},
										{
											type: 'doc',
											id: 'topics/10.08.RestServerMixinSubContexts',
											label: 'Mixin Sub-Contexts',
										},
										{
											type: 'doc',
											id: 'topics/10.09.RestServerStandaloneVsMixin',
											label: 'Standalone vs Mixin Resources',
										},
										{
											type: 'doc',
											id: 'topics/10.10.RestServerChildrenVsMixins',
											label: 'Children vs Mixins',
										},
										{
											type: 'doc',
											id: 'topics/10.11.RestServerSelfRegistration',
											label: 'Self-Registration',
										},
										{
											type: 'doc',
											id: 'topics/10.12.RestServerProgrammaticBuilder',
											label: 'Programmatic Fluent Builder',
										},
										{
											type: 'doc',
											id: 'topics/10.13.RestServerSse',
											label: 'Server-Sent Events',
										},
									],
								}),
								group({
									type: 'category',
									label: 'Built-in Features',
									collapsed: true,
									link: {
										type: 'generated-index',
										description: 'Guards, converters, messages, encoders, configuration files, static files and convention endpoints.',
									},
									items: [
										{
											type: 'doc',
											id: 'topics/10.14.Guards',
											label: 'Guards',
										},
										{
											type: 'doc',
											id: 'topics/10.15.Converters',
											label: 'Converters',
										},
										{
											type: 'doc',
											id: 'topics/10.16.LocalizedMessages',
											label: 'Localized Messages',
										},
										{
											type: 'doc',
											id: 'topics/10.17.RestServerEncoders',
											label: 'Encoders in REST servers',
										},
										{
											type: 'doc',
											id: 'topics/10.18.ConfigurationFiles',
											label: 'Configuration Files',
										},
										{
											type: 'doc',
											id: 'topics/10.19.RestServerSvlVariables',
											label: 'SVL Variables',
										},
										{
											type: 'doc',
											id: 'topics/10.20.StaticFiles',
											label: 'Static files',
										},
										{
											type: 'doc',
											id: 'topics/10.21.StaticFilesMixin',
											label: 'Static-Files Mixin',
										},
										{
											type: 'doc',
											id: 'topics/10.22.ConventionEndpointsMixins',
											label: 'Convention-Endpoints Mixin Pack',
										},
										{
											type: 'doc',
											id: 'topics/10.23.OpsIntrospectionMixins',
											label: 'Ops / Introspection Mixin Pack',
										},
									],
								}),
								group({
									type: 'category',
									label: 'Views and API Docs',
									collapsed: true,
									link: {
										type: 'generated-index',
										description: 'View engines (JSP, Thymeleaf, Mustache, FreeMarker), Swagger and HTML documentation support.',
									},
									items: [
										{
											type: 'doc',
											id: 'topics/10.24.JspViewSupport',
											label: 'JSP View Support',
										},
										{
											type: 'doc',
											id: 'topics/10.25.ThymeleafViewSupport',
											label: 'Thymeleaf View Support',
										},
										{
											type: 'doc',
											id: 'topics/10.26.MustacheViewSupport',
											label: 'Mustache View Support',
										},
										{
											type: 'doc',
											id: 'topics/10.27.FreemarkerViewSupport',
											label: 'FreeMarker View Support',
										},
										{
											type: 'category',
											label: 'Swagger',
											collapsed: true,
											link: {
												type: 'doc',
												id: 'topics/10.28.Swagger',
											},
											items: [
												{
													type: 'doc',
													id: 'topics/10.28.01.BasicRestServletSwagger',
													label: 'BasicRestServlet/BasicRestResource Swagger and OpenAPI 3.1',
												},
												{
													type: 'doc',
													id: 'topics/10.28.02.ApiDocsMixins',
													label: 'API-Docs Mixin Pack',
												},
												{
													type: 'doc',
													id: 'topics/10.28.03.BasicSwaggerInfo',
													label: 'Basic Swagger Info',
												},
												{
													type: 'doc',
													id: 'topics/10.28.04.SwaggerTags',
													label: 'Swagger Tags',
												},
												{
													type: 'doc',
													id: 'topics/10.28.05.SwaggerParameters',
													label: 'Swagger Parameters',
												},
												{
													type: 'doc',
													id: 'topics/10.28.06.SwaggerModels',
													label: 'Swagger Models',
												},
											],
										},
										{
											type: 'category',
											label: '@HtmlDocConfig Annotation',
											collapsed: true,
											link: {
												type: 'doc',
												id: 'topics/10.29.HtmlDocConfigAnnotation',
											},
											items: [
												{
													type: 'doc',
													id: 'topics/10.29.01.HtmlUIvsDI',
													label: 'User Interfaces (UI) vs. Developer Interfaces (DI)',
												},
												{
													type: 'doc',
													id: 'topics/10.29.02.HtmlWidgets',
													label: 'Widgets',
												},
												{
													type: 'doc',
													id: 'topics/10.29.03.HtmlPredefinedWidgets',
													label: 'Predefined Widgets',
												},
												{
													type: 'doc',
													id: 'topics/10.29.04.HtmlUiCustomization',
													label: 'UI Customization',
												},
												{
													type: 'doc',
													id: 'topics/10.29.05.HtmlStylesheets',
													label: 'Stylesheets',
												},
											],
										},
									],
								}),
								group({
									type: 'category',
									label: 'HTTP Behavior',
									collapsed: true,
									link: {
										type: 'generated-index',
										description: 'Logging, status codes, problem details, conditional GET, rate limiting, client versioning, validation and test support.',
									},
									items: [
										{
											type: 'doc',
											id: 'topics/10.30.RestServerLoggingAndDebugging',
											label: 'Logging / Debugging',
										},
										{
											type: 'doc',
											id: 'topics/10.31.HttpStatusCodes',
											label: 'HTTP Status Codes',
										},
										{
											type: 'doc',
											id: 'topics/10.32.RestServerProblemDetails',
											label: 'RFC 7807 / 9457 Problem Details',
										},
										{
											type: 'doc',
											id: 'topics/10.33.RestServerConditionalGet',
											label: 'Conditional-GET / ETag Helpers',
										},
										{
											type: 'doc',
											id: 'topics/10.34.RestServerRateLimitAndRequestId',
											label: 'Rate-Limiting and Request-Id Propagation',
										},
										{
											type: 'doc',
											id: 'topics/10.35.ClientVersioning',
											label: 'Client Versioning',
										},
										{
											type: 'doc',
											id: 'topics/10.36.RestServerValidation',
											label: 'Jakarta Bean Validation',
										},
										{
											type: 'doc',
											id: 'topics/10.37.RestServerTestBeanInjection',
											label: 'Test-time Bean Injection',
										},
									],
								}),
								group({
									type: 'category',
									label: 'Security',
									collapsed: true,
									link: {
										type: 'generated-index',
										description: 'Authentication guards and the Bearer, API-key, JWT, SAML, OAuth 2.0 and OIDC authentication frameworks.',
									},
									items: [
										{
											type: 'doc',
											id: 'topics/10.38.RestServerAuthGuards',
											label: 'AuthN Guards — Bearer / API-Key / JWT',
										},
										{
											type: 'doc',
											id: 'topics/10.39.AuthFilterFramework',
											label: 'AuthN Filter Framework — Servlet-Layer Authentication',
										},
										{
											type: 'doc',
											id: 'topics/10.40.RestServerAuthenticator',
											label: 'REST Authenticator — Resource-Level Authentication',
										},
										{
											type: 'doc',
											id: 'topics/10.41.SamlAuthSupport',
											label: 'SAML 2.0 AuthN Support (juneau-rest-server-auth-saml)',
										},
										{
											type: 'doc',
											id: 'topics/10.42.OAuthAuthSupport',
											label: 'OAuth 2.0 / OIDC AuthN Support (juneau-rest-server-auth-oauth)',
										},
										{
											type: 'doc',
											id: 'topics/10.43.OidcRelyingParty',
											label: 'OIDC Relying Party Login (juneau-rest-server-auth-oidc-rp)',
										},
									],
								}),
								group({
									type: 'category',
									label: 'Operations and Observability',
									collapsed: true,
									link: {
										type: 'generated-index',
										description: 'Observability, metrics and tracing bridges, execution statistics, and logging and management surfaces.',
									},
									items: [
										{
											type: 'doc',
											id: 'topics/10.44.RestServerObservability',
											label: 'Observability — Micrometer + OpenTelemetry',
										},
										{
											type: 'doc',
											id: 'topics/10.45.RestServerMicrometerMetrics',
											label: 'Micrometer Metrics Bridge',
										},
										{
											type: 'doc',
											id: 'topics/10.46.RestServerOtelTracing',
											label: 'OpenTelemetry Tracing Bridge',
										},
										{
											type: 'doc',
											id: 'topics/10.47.ExecutionStatistics',
											label: 'REST method execution statistics',
										},
										{
											type: 'doc',
											id: 'topics/10.48.RestServerManagementLogging',
											label: 'Management Logging: Logback & Log4j2 Backends',
										},
									],
								}),
								group({
									type: 'category',
									label: 'Advanced and Internals',
									collapsed: true,
									link: {
										type: 'generated-index',
										description: 'Reactive support, contexts, response processors, RPC and other advanced topics.',
									},
									items: [
										{
											type: 'doc',
											id: 'topics/10.49.RestServerAsyncDispatch',
											label: 'Async Returns + Virtual-Thread Dispatch',
										},
										{
											type: 'doc',
											id: 'topics/10.50.RestServerReactive',
											label: 'Reactive-Streams Returns',
										},
										{
											type: 'doc',
											id: 'topics/10.51.BuiltInParameters',
											label: 'Built-in Parameters',
										},
										{
											type: 'doc',
											id: 'topics/10.52.SessionOptions',
											label: 'Session Options via HTTP',
										},
										{
											type: 'doc',
											id: 'topics/10.53.UsingWithOsgi',
											label: 'Using with OSGi',
										},
										{
											type: 'doc',
											id: 'topics/10.54.RestContext',
											label: 'RestContext',
										},
										{
											type: 'doc',
											id: 'topics/10.55.RestOpContext',
											label: 'RestOpContext',
										},
										{
											type: 'doc',
											id: 'topics/10.56.ResponseProcessors',
											label: 'Response Processors',
										},
										{
											type: 'doc',
											id: 'topics/10.57.RestRpc',
											label: 'REST/RPC',
										},
										{
											type: 'doc',
											id: 'topics/10.58.SerializingUris',
											label: 'Serializing URIs',
										},
										{
											type: 'doc',
											id: 'topics/10.59.UtilityBeans',
											label: 'Utility Beans',
										},
										{
											type: 'doc',
											id: 'topics/10.60.HtmlBeans',
											label: 'Using with HTML Beans',
										},
										{
											type: 'doc',
											id: 'topics/10.61.RestServerRdf',
											label: 'RDF Support (juneau-rest-server-rdf)',
										},
									],
								}),
								group({
									type: 'category',
									label: 'Admin Console and Rich UI',
									collapsed: true,
									link: {
										type: 'generated-index',
										description: 'Admin console chrome, data tables, declarative widgets and rich table views.',
									},
									items: [
										{
											type: 'category',
											label: 'Admin-Console Chrome (juneau-console)',
											collapsed: true,
											link: {
												type: 'doc',
												id: 'topics/10.62.00.AdminConsoleChrome',
											},
											items: [
												{
													type: 'doc',
													id: 'topics/10.62.01.BuildingAConsolePage',
													label: 'Building a Console Page',
												},
												{
													type: 'doc',
													id: 'topics/10.62.02.CardAssetDependencies',
													label: 'Card Asset Dependencies',
												},
												{
													type: 'doc',
													id: 'topics/10.62.03.ChartCards',
													label: 'Chart Cards',
												},
												{
													type: 'doc',
													id: 'topics/10.62.04.JobLogCard',
													label: 'Job-Log Card',
												},
											],
										},
										{
											type: 'doc',
											id: 'topics/10.63.MarkdownViewRendering',
											label: 'Markdown View Rendering (juneau-console-views-markdown)',
										},
										{
											type: 'doc',
											id: 'topics/10.64.DataTables',
											label: 'DataTables Server-Side Processing (juneau-console-datatables)',
										},
										{
											type: 'doc',
											id: 'topics/10.65.DeclarativeWidgets',
											label: 'Declarative Widgets (juneau-console-widgets)',
										},
										{
											type: 'category',
											label: 'Rich Table Views (juneau-console-views)',
											collapsed: true,
											link: {
												type: 'doc',
												id: 'topics/10.66.00.RichTableViews',
											},
											items: [
												{
													type: 'doc',
													id: 'topics/10.66.01.RichTableViewsCatalog',
													label: 'Catalog and Columns',
												},
												{
													type: 'doc',
													id: 'topics/10.66.02.RichTableViewsActions',
													label: 'Row Actions and Bulk Mutation',
												},
												{
													type: 'doc',
													id: 'topics/10.66.03.RichTableViewsRenderers',
													label: 'Renderers and Cell Popovers',
												},
												{
													type: 'doc',
													id: 'topics/10.66.04.RichTableViewsSecurity',
													label: 'Security Model',
												},
												{
													type: 'doc',
													id: 'topics/10.66.05.RichTableViewsJavaScript',
													label: 'JavaScript API',
												},
												{
													type: 'doc',
													id: 'topics/10.66.06.RichTableViewsSettings',
													label: 'View Settings and Shareable Links',
												},
												{
													type: 'doc',
													id: 'topics/10.66.07.RichTableViewsRegions',
													label: 'Regions',
												},
												{
													type: 'doc',
													id: 'topics/10.66.08.RichTableViewsAsyncJobs',
													label: 'Async Jobs',
												},
												{
													type: 'doc',
													id: 'topics/10.66.09.RichTableViewsIcons',
													label: 'Icons and Sprites',
												},
												{
													type: 'doc',
													id: 'topics/10.66.10.TerminalViewRegion',
													label: 'Terminal View Region',
												},
												{
													type: 'doc',
													id: 'topics/10.66.11.TerminalRegion',
													label: 'Terminal Region',
												},
												{
													type: 'doc',
													id: 'topics/10.66.12.RowJobs',
													label: 'Row Jobs',
												},
												{
													type: 'doc',
													id: 'topics/10.66.13.ConsoleMessageBus',
													label: 'Console Message Bus',
												},
											],
										},
										{
											type: 'doc',
											id: 'topics/10.67.DatatableViewSettings',
											label: 'Datatable View Settings (juneau-console-views)',
										},
									],
								}),
							],
							link: {
								type: 'doc',
								id: 'topics/10.juneau-rest-server',
							},
						},
						{
							type: 'category',
							label: 'MCP (Model Context Protocol)',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/11.01.JuneauMcpQuickstart',
									label: 'Quickstart',
								},
								{
									type: 'doc',
									id: 'topics/11.02.JuneauMcpChoosingSetup',
									label: 'Choosing your setup',
								},
								{
									type: 'doc',
									id: 'topics/11.03.JuneauMcpRecipes',
									label: 'Recipes',
								},
								{
									type: 'doc',
									id: 'topics/11.04.juneau-rest-server-mcp',
									label: 'juneau-rest-server-mcp (reference)',
								},
								{
									type: 'doc',
									id: 'topics/11.05.juneau-rest-client-mcp',
									label: 'juneau-rest-client-mcp (reference)',
								},
							],
							link: {
								type: 'doc',
								id: 'topics/11.juneau-mcp',
							},
						},
						{
							type: 'category',
							label: 'juneau-rest-server-springboot',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/12.01.SpringBootOverview',
									label: 'Spring Boot Overview',
								},
							],
							link: {
								type: 'doc',
								id: 'topics/12.juneau-rest-server-springboot',
							},
						},
						{
							type: 'category',
							label: 'juneau-rest-client',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/13.01.ChoosingARestClient',
									label: 'Choosing a REST Client',
								},
								{
									type: 'doc',
									id: 'topics/13.02.PojoMarshalling',
									label: 'POJO Marshalling',
								},
								{
									type: 'doc',
									id: 'topics/13.03.RequestParts',
									label: 'Request Parts',
								},
								{
									type: 'doc',
									id: 'topics/13.04.RequestContent',
									label: 'Request Content',
								},
								{
									type: 'doc',
									id: 'topics/13.05.ResponseStatus',
									label: 'Response Status',
								},
								{
									type: 'doc',
									id: 'topics/13.06.ResponseHeaders',
									label: 'Response Headers',
								},
								{
									type: 'doc',
									id: 'topics/13.07.ResponseContent',
									label: 'Response Content',
								},
								{
									type: 'doc',
									id: 'topics/13.08.Interceptors',
									label: 'Interceptors',
								},
								{
									type: 'category',
									label: 'REST Proxies',
									collapsed: true,
									link: {
										type: 'doc',
										id: 'topics/13.09.RestProxies',
									},
									items: [
										{
											type: 'doc',
											id: 'topics/13.09.01.Remote',
											label: '@Remote',
										},
										{
											type: 'doc',
											id: 'topics/13.09.02.RemoteMethod',
											label: '@RemoteOp',
										},
										{
											type: 'doc',
											id: 'topics/13.09.03.Content',
											label: '@Content',
										},
										{
											type: 'doc',
											id: 'topics/13.09.04.FormData',
											label: '@FormData',
										},
										{
											type: 'doc',
											id: 'topics/13.09.05.Query',
											label: '@Query',
										},
										{
											type: 'doc',
											id: 'topics/13.09.06.Header',
											label: '@Header',
										},
										{
											type: 'doc',
											id: 'topics/13.09.07.Path',
											label: '@Path',
										},
										{
											type: 'doc',
											id: 'topics/13.09.08.Request',
											label: '@Request',
										},
										{
											type: 'doc',
											id: 'topics/13.09.09.Response',
											label: '@Response',
										},
										{
											type: 'doc',
											id: 'topics/13.09.10.DualPurposeInterfaces',
											label: 'Dual-purpose (end-to-end) interfaces',
										},
									],
								},
								{
									type: 'doc',
									id: 'topics/13.10.RestClientLoggingAndDebugging',
									label: 'Logging and Debugging',
								},
								{
									type: 'doc',
									id: 'topics/13.11.CustomizingHttpClient',
									label: 'Customizing HttpClient',
								},
								{
									type: 'doc',
									id: 'topics/13.12.ExtendingRestClient',
									label: 'Extending RestClient',
								},
								{
									type: 'doc',
									id: 'topics/13.13.Authentication',
									label: 'Authentication',
								},
								{
									type: 'category',
									label: 'Next-Generation REST Client',
									collapsed: true,
									link: {
										type: 'doc',
										id: 'topics/13.14.00.NextGenRestClient',
									},
									items: [
										{
											type: 'doc',
											id: 'topics/13.14.01.NgRequests',
											label: 'Requests',
										},
										{
											type: 'doc',
											id: 'topics/13.14.02.NgResponses',
											label: 'Responses',
										},
										{
											type: 'doc',
											id: 'topics/13.14.03.NgBodyConverters',
											label: 'Body Converters',
										},
										{
											type: 'doc',
											id: 'topics/13.14.04.NgAssertions',
											label: 'Assertions',
										},
										{
											type: 'doc',
											id: 'topics/13.14.05.NgInterceptors',
											label: 'Interceptors',
										},
										{
											type: 'doc',
											id: 'topics/13.14.06.NgRemoteProxies',
											label: 'Remote Proxies',
										},
									],
								},
								{
									type: 'category',
									label: 'HTTP Transport Adapters',
									collapsed: true,
									link: {
										type: 'doc',
										id: 'topics/13.15.HttpTransportAdapters',
									},
									items: [
										{
											type: 'doc',
											id: 'topics/13.15.01.RestClientApacheHttpClient45',
											label: 'Apache HttpClient 4.5 Transport',
										},
										{
											type: 'doc',
											id: 'topics/13.15.02.RestClientApacheHttpClient50',
											label: 'Apache HttpClient 5.x Transport',
										},
										{
											type: 'doc',
											id: 'topics/13.15.03.RestClientOkHttp',
											label: 'OkHttp Transport',
										},
										{
											type: 'doc',
											id: 'topics/13.15.04.RestClientJetty',
											label: 'Jetty HTTP Client Transport',
										},
									],
								},
								{
									type: 'doc',
									id: 'topics/13.16.StreamingCursors',
									label: 'Streaming Cursors',
								},
								{
									type: 'doc',
									id: 'topics/13.17.ContentTypeNegotiation',
									label: 'Content-Type Negotiation',
								},
								{
									type: 'doc',
									id: 'topics/13.18.NgClientSecurity',
									label: 'NG Client Security',
								},
							],
							link: {
								type: 'doc',
								id: 'topics/13.juneau-rest-client',
							},
						},
						{
							type: 'category',
							label: 'juneau-rest-mock',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/14.01.MockRestClientOverview',
									label: 'Mock REST Client Overview',
								},
								{
									type: 'doc',
									id: 'topics/14.02.NgMockRestClient',
									label: 'NG Mock REST Client',
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
					collapsed: true,
					items: [
						{
							type: 'category',
							label: 'juneau-microservice',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/15.01.MicroserviceOverview',
									label: 'Microservice Overview',
								},
								{
									type: 'doc',
									id: 'topics/15.02.Args',
									label: 'Args',
								},
								{
									type: 'doc',
									id: 'topics/15.03.Manifest',
									label: 'Manifest',
								},
								{
									type: 'doc',
									id: 'topics/15.04.Config',
									label: 'Config',
								},
								{
									type: 'doc',
									id: 'topics/15.05.VarResolver',
									label: 'VarResolver',
								},
								{
									type: 'doc',
									id: 'topics/15.06.ConsoleCommands',
									label: 'Console Commands',
								},
								{
									type: 'doc',
									id: 'topics/15.07.MicroserviceCoreListeners',
									label: 'Listeners',
								},
								{
									type: 'doc',
									id: 'topics/15.08.InjectAwareMicroservice',
									label: 'Inject-Aware Microservice',
								},
								{
									type: 'doc',
									id: 'topics/15.09.MicroserviceTesting',
									label: 'Whole-Microservice Integration Tests (@MicroserviceTest)',
								},
							],
							link: {
								type: 'doc',
								id: 'topics/15.juneau-microservice',
							},
						},
						{
							type: 'category',
							label: 'juneau-microservice-jetty',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/16.01.MicroserviceJettyOverview',
									label: 'Microservice Jetty Overview',
								},
								{
									type: 'doc',
									id: 'topics/16.02.LifecycleMethods',
									label: 'Lifecycle Methods',
								},
								{
									type: 'doc',
									id: 'topics/16.03.ResourceClasses',
									label: 'Resource Classes',
								},
								{
									type: 'doc',
									id: 'topics/16.04.PredefinedResourceClasses',
									label: 'Predefined Resource Classes',
								},
								{
									type: 'doc',
									id: 'topics/16.05.JettyXml',
									label: 'Jetty.xml file',
								},
								{
									type: 'doc',
									id: 'topics/16.06.UiCustomization',
									label: 'UI Customization',
								},
								{
									type: 'doc',
									id: 'topics/16.07.Extending',
									label: 'Customizing via @Bean',
								},
								{
									type: 'doc',
									id: 'topics/16.08.HealthProbes',
									label: 'Health / Readiness / Liveness Probes',
								},
								{
									type: 'doc',
									id: 'topics/16.09.GracefulShutdown',
									label: 'Graceful Shutdown & Readiness Gating',
								},
								{
									type: 'doc',
									id: 'topics/16.10.ManagementSurface',
									label: 'Management Surface (Actuator-style endpoints)',
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
							label: 'juneau-microservice-tomcat',
						},
						{
							type: 'category',
							label: 'juneau-sc',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/18.01.ScServerOverview',
									label: 'juneau-sc-server',
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
					collapsed: true,
					items: [
						{
							type: 'category',
							label: 'juneau-petstore',
							collapsed: true,
							link: {
								type: 'doc',
								id: 'topics/19.00.JuneauPetstore',
							},
							items: [
								{
									type: 'doc',
									id: 'topics/19.01.PetstoreConsoleTour',
									label: 'Console Tour',
								},
								{
									type: 'doc',
									id: 'topics/19.02.PetstoreStore',
									label: 'Store',
								},
								{
									type: 'doc',
									id: 'topics/19.03.PetstorePets',
									label: 'Pets',
								},
								{
									type: 'doc',
									id: 'topics/19.04.PetstoreSoldPets',
									label: 'Sold Pets',
								},
								{
									type: 'doc',
									id: 'topics/19.05.PetstorePetDetail',
									label: 'Pet Detail',
								},
								{
									type: 'doc',
									id: 'topics/19.06.PetstoreOrders',
									label: 'Orders',
								},
								{
									type: 'doc',
									id: 'topics/19.07.PetstoreJobs',
									label: 'Jobs',
								},
								{
									type: 'doc',
									id: 'topics/19.08.PetstoreAudit',
									label: 'Audit',
								},
								{
									type: 'doc',
									id: 'topics/19.09.PetstoreCustomCards',
									label: 'Custom Cards',
								},
								{
									type: 'doc',
									id: 'topics/19.10.PetstoreThemes',
									label: 'Themes',
								},
								{
									type: 'doc',
									id: 'topics/19.11.PetstorePendingChanges',
									label: 'Pending Changes',
								},
								{
									type: 'doc',
									id: 'topics/19.12.PetstoreUsers',
									label: 'Users',
								},
								{
									type: 'doc',
									id: 'topics/19.13.PetstoreRenderingFlavors',
									label: 'Rendering Flavors',
								},
								{
									type: 'doc',
									id: 'topics/19.14.PetstoreSecure',
									label: 'Secure',
								},
								{
									type: 'doc',
									id: 'topics/19.15.PetstoreAbout',
									label: 'About',
								},
								{
									type: 'doc',
									id: 'topics/19.16.PetstoreDeveloperPages',
									label: 'Developer Pages',
								},
								{
									type: 'doc',
									id: 'topics/19.17.PetstoreTwins',
									label: 'Template and Builder Twins',
								},
								{
									type: 'doc',
									id: 'topics/19.18.PetstoreFailLoud',
									label: 'FailLoud Catalogue',
								},
								{
									type: 'doc',
									id: 'topics/19.19.PortingToTheConsole',
									label: 'Porting to the Console',
								},
							],
						},
						{
							type: 'doc',
							id: 'topics/20.juneau-examples',
							label: 'juneau-examples',
						},
					],
				},
				{
					type: 'category',
					label: 'Packaging & Deployment',
					collapsed: true,
					items: [
						{
							type: 'category',
							label: 'Dependency Management',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/21.01.NativeImageAndLayeredJars',
									label: 'GraalVM Native Image & Docker Layering',
								},
							],
							link: {
								type: 'doc',
								id: 'topics/21.DependencyManagement',
							},
						},
						{
							type: 'category',
							label: 'juneau-terminal',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/22.01.AgentSidePane',
									label: 'Watching a Run from an AI Agent',
								},
								{
									type: 'doc',
									id: 'topics/22.02.DrivingTheTerminal',
									label: 'Driving a Run from Another Tool',
								},
							],
							link: {
								type: 'doc',
								id: 'topics/22.JuneauTerminal',
							},
						},
						{
							type: 'category',
							label: 'juneau-shaded',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/23.01.JuneauShadedCore',
									label: 'juneau-shaded-core',
								},
								{
									type: 'doc',
									id: 'topics/23.02.JuneauShadedRestClient',
									label: 'juneau-shaded-rest-client',
								},
								{
									type: 'doc',
									id: 'topics/23.03.JuneauShadedRestServer',
									label: 'juneau-shaded-rest-server',
								},
								{
									type: 'doc',
									id: 'topics/23.04.JuneauShadedRestServerSpringboot',
									label: 'juneau-shaded-rest-server-springboot',
								},
								{
									type: 'doc',
									id: 'topics/23.05.JuneauShadedAll',
									label: 'juneau-shaded-all',
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
							label: 'Starter Projects',
						},
					],
				},
				{
					type: 'category',
					label: 'Security & Migration',
					collapsed: true,
					items: [
						{
							type: 'category',
							label: 'Security',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/25.01.MarshallingSecurity',
									label: 'Marshalling Security',
								},
								{
									type: 'doc',
									id: 'topics/25.02.SvlSecurity',
									label: 'SVL Security',
								},
								{
									type: 'doc',
									id: 'topics/25.03.RestSecurity',
									label: 'REST Security',
								},
								{
									type: 'doc',
									id: 'topics/25.04.ContentSecurityPolicy',
									label: 'Content Security Policy',
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
							label: 'V9.0 Migration Guide',
						},
						{
							type: 'doc',
							id: 'topics/27.V10MigrationGuide',
							label: 'V10.0 Migration Guide',
						},
					],
				},
			]),
		},
		{
			type: 'category',
			label: 'Developer Info',
			collapsed: true,
			items: numbered([
				{
					type: 'doc',
					id: 'developer-info/01.DeveloperLinks',
					label: 'Developer Links',
				},
				{
					type: 'category',
					label: 'How-to Articles',
					collapsed: false,
					items: [
						{
							type: 'doc',
							id: 'developer-info/02.01.BecomingAContributor',
							label: 'Becoming a contributor',
						},
						{
							type: 'doc',
							id: 'developer-info/02.02.NonCommittersContribute',
							label: 'How non-committers can contribute code',
						},
						{
							type: 'doc',
							id: 'developer-info/02.03.NewMemberGuidelines',
							label: 'New member guidelines',
						},
						{
							type: 'doc',
							id: 'developer-info/02.04.NewReleaseGuidelines',
							label: 'New release guidelines',
						},
						{
							type: 'doc',
							id: 'developer-info/02.05.VersioningGuidelines',
							label: 'Versioning guidelines',
						},
						{
							type: 'doc',
							id: 'developer-info/02.06.CodeFormattingStylesheet',
							label: 'Using the code formatting stylesheet',
						},
						{
							type: 'doc',
							id: 'developer-info/02.08.DevelopmentWishList',
							label: 'Development wish list',
						},
						{
							type: 'doc',
							id: 'developer-info/02.09.AiKnowledgeAuthoring',
							label: 'AI Knowledge Authoring Guide',
						},
						{
							type: 'doc',
							id: 'developer-info/02.10.TopicPageTemplate',
							label: 'Topic Page Template',
						},
					],
				},
				{
					type: 'doc',
					id: 'developer-info/03.CurrentMembers',
					label: 'Current Members',
				},
				{
					type: 'category',
					label: 'Scripts',
					collapsed: false,
					items: [
						{
							type: 'doc',
							id: 'developer-info/04.01.Scripts',
							label: 'Scripts',
						},
						{
							type: 'category',
							label: 'Code Scripts',
							collapsed: false,
							items: [
								{
									type: 'doc',
									id: 'developer-info/04.02.01.ScriptTest',
									label: 'test.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.02.02.ScriptPush',
									label: 'push.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.02.03.ScriptRelease',
									label: 'release.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.02.04.ScriptPromptPgpPassphrase',
									label: 'prompt-pgp-passphrase.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.02.05.ScriptCurrentRelease',
									label: 'current-release.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.02.06.ScriptMavenVersion',
									label: 'maven-version.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.02.07.ScriptApplyEclipsePrefs',
									label: 'apply-eclipse-prefs.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.02.08.ScriptCleanupWhitespace',
									label: 'cleanup-whitespace.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.02.09.ScriptCheckFluentSetterOverrides',
									label: 'check-fluent-setter-overrides.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.02.10.ScriptRevertStaged',
									label: 'revert-staged.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.02.11.ScriptRevertUnstaged',
									label: 'revert-unstaged.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.02.12.ScriptStartPetstoreJetty',
									label: 'start-petstore-jetty.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.02.13.ScriptStartPetstoreSpringboot',
									label: 'start-petstore-springboot.py',
								},
							],
						},
						{
							type: 'category',
							label: 'Doc Scripts',
							collapsed: false,
							items: [
								{
									type: 'doc',
									id: 'developer-info/04.03.01.ScriptReleaseDocsStage',
									label: 'release-docs-stage.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.03.02.ScriptReleaseDocs',
									label: 'release-docs.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.03.03.ScriptBuildDocs',
									label: 'build-docs.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.03.04.ScriptCreateMvnSite',
									label: 'create-mvn-site.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.03.05.ScriptStartDocusaurus',
									label: 'start-docusaurus.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.03.06.ScriptCheckTopicLinks',
									label: 'check-topic-links.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.03.07.ScriptAiArtifacts',
									label: 'generate-ai-artifacts.py / check-ai-artifacts.py',
								},
							],
						},
					],
				},
			]),
		},
		{
			type: 'category',
			label: 'Articles',
			collapsed: true,
			link: {
				type: 'doc',
				id: 'developer-info/05.ArticlesIndex',
			},
			items: [
				{
					type: 'link',
					label: '2018-11-12 · Serializing and deserializing POJOs into many formats using Apache Juneau',
					href: 'https://medium.com/@shalithasuranga/serializing-and-deserializing-pojos-in-to-many-formats-using-apache-juneau-6b68d97aee6c',
				},
				{
					type: 'link',
					label: '2018-12-15 · Construct HTML fragments, Atom feeds, Swagger documents using Apache Juneau',
					href: 'https://medium.com/@ayeshmanthaperera/construct-html-fragments-as-java-objects-using-apache-juneau-aeb9a9c5c2ca',
				},
				{
					type: 'link',
					label: '2019-10-21 · Pet Store with Apache Juneau Part 1',
					href: 'https://dev.to/robledokari/pet-store-with-apache-juneau-part-1-5cgm',
				},
				{
					type: 'doc',
					id: 'developer-info/05.01.AgentBuildBoard',
					label: '2026-10-26 · Watching builds from an AI agent: the juneau-terminal board',
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
					label: '10.0.0',
					link: {
						type: 'doc',
						id: 'release-notes/10.0.0',
					},
					items: [
						{
							type: 'doc',
							id: 'release-notes/10.0.0-marshall',
							label: 'Marshall, commons and config',
						},
						{
							type: 'doc',
							id: 'release-notes/10.0.0-rest-server',
							label: 'REST server',
						},
						{
							type: 'doc',
							id: 'release-notes/10.0.0-rest-client',
							label: 'REST client',
						},
						{
							type: 'doc',
							id: 'release-notes/10.0.0-microservice',
							label: 'Microservice',
						},
						{
							type: 'doc',
							id: 'release-notes/10.0.0-other',
							label: 'Other modules',
						},
					],
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
				{
					type: 'doc',
					id: 'release-notes/8.x',
					label: '8.x',
				},
				{
					type: 'doc',
					id: 'release-notes/7.x',
					label: '7.x',
				},
				{
					type: 'doc',
					id: 'release-notes/6.x',
					label: '6.x',
				},
				{
					type: 'doc',
					id: 'release-notes/5.x',
					label: '5.x',
				},
			],
		},
	] as SidebarsConfig[string],
};

export default sidebars;
