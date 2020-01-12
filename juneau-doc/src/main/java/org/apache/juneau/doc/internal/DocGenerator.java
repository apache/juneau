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
package org.apache.juneau.doc.internal;

import static org.apache.juneau.doc.internal.Console.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Utility for generating the overview.html page.
 */
public class DocGenerator {

	static List<String> WARNINGS = new ArrayList<>();

	static final String COPYRIGHT = ""
		+ "\n/***************************************************************************************************************************"
		+ "\n * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file"
		+ "\n * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file"
		+ "\n * to you under the Apache License, Version 2.0 (the \"License\"); you may not use this file except in compliance"
		+ "\n * with the License.  You may obtain a copy of the License at"
		+ "\n *  "
		+ "\n *  http://www.apache.org/licenses/LICENSE-2.0"
		+ "\n *  "
		+ "\n * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an"
		+ "\n * \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the"
		+ "\n * specific language governing permissions and limitations under the License."
		+ "\n ***************************************************************************************************************************/"
	;

	static String juneauVersion = System.getProperty("juneauVersion");
	static String juneauTagPattern = juneauVersion.replace(".", "\\.") + "\\-\\w+";

	/**
	 * Entry point.
	 *
	 * @param args Not used
	 */
	public static void main(String[] args) {
		if (args.length == 0 || args.length == 1 && args[0].equals("build"))
			build();
		printWarnings();
	}


	private static void build() {
		try {
			long startTime = System.currentTimeMillis();

			String template = IOUtils.readFile("docs/overview_template.html");

			String configProps = ConfigPropsGenerator.run();

			DocStore ds = new DocStore(new File("docs/docs.txt"));

			File top = new File("docs/Topics");

			reorder(top);

			Topics topics = new Topics(top);

			ReleaseNotes releaseNotes = new ReleaseNotes(new File("docs/ReleaseNotes"));

			StringBuilder toc = new StringBuilder("<ol class='toc'>\n"), contents = new StringBuilder();

			for (PageFile pf1 : topics.pageFiles) {
				toc
					.append("\t<li><p class='toc2'><a class='doclink' href='{OVERVIEW_URL}#").append(pf1.fullId).append("'>").append(pf1.title).append("</a>").append(pf1.tags.isEmpty() ? "" : ("<span class='update'>"+pf1.tags+"</span>")).append("</p>\n");
				ds
					.addLink(pf1.fullId, "#" + pf1.fullId, "Overview > " + pf1.title);
				contents
					.append("\n")
					.append("<!-- ==================================================================================================== -->\n\n")
					.append("<h2 class='topic' onclick='toggle(this)'><a href='#").append(pf1.fullId).append("' id='").append(pf1.fullId).append("'>").append(pf1.fullNumber).append(" - ").append(pf1.title).append("</a>").append(pf1.tags.isEmpty() ? "" : ("<span class='update'>"+pf1.tags+"</span>")).append("</h2>\n")
					.append("<div class='topic'>").append("<!-- START: ").append(pf1.fullNumber).append(" - " ).append(pf1.fullId).append(" -->\n")
					.append(pf1.contents).append("\n");

				if (! pf1.pageFiles.isEmpty()) {

					toc.append("\t<ol>\n");

					for (PageFile pf2 : pf1.pageFiles) {

						toc
							.append("\t\t<li><p><a class='doclink' href='{OVERVIEW_URL}#").append(pf2.fullId).append("'>").append(pf2.title).append("</a>").append(pf2.tags.isEmpty() ? "" : ("<span class='update'>"+pf2.tags+"</span>")).append("</p>\n");
						ds
							.addLink(pf2.fullId, "#" + pf2.fullId, "Overview > " + pf1.title + " > " + pf2.title);
						contents
							.append("\n")
							.append("<!-- ==================================================================================================== -->\n\n")
							.append("<h3 class='topic' onclick='toggle(this)'><a href='#").append(pf2.fullId).append("' id='").append(pf2.fullId).append("'>").append(pf2.fullNumber).append(" - ").append(pf2.title).append("</a>").append(pf2.tags.isEmpty() ? "" : ("<span class='update'>"+pf2.tags+"</span>")).append("</h3>\n")
							.append("<div class='topic'>").append("<!-- START: ").append(pf2.fullNumber).append(" - " ).append(pf2.fullId).append(" -->\n")
							.append(pf2.contents).append("\n");

						if (! pf2.pageFiles.isEmpty()) {
							toc.append("\t\t<ol>\n");

							for (PageFile pf3 : pf2.pageFiles) {

								toc
									.append("\t\t\t<li><p><a class='doclink' href='{OVERVIEW_URL}#").append(pf3.fullId).append("'>").append(pf3.title).append("</a>").append(pf3.tags.isEmpty() ? "" : ("<span class='update'>"+pf3.tags+"</span>")).append("</p>\n");
								ds
									.addLink(pf3.fullId, "#" + pf3.fullId, "Overview > " + pf1.title + " > " + pf2.title + " > " + pf3.title);
								contents
									.append("\n")
									.append("<!-- ==================================================================================================== -->\n\n")
									.append("<h4 class='topic' onclick='toggle(this)'><a href='#").append(pf3.fullId).append("' id='").append(pf3.fullId).append("'>").append(pf3.fullNumber).append(" - ").append(pf3.title).append("</a>").append(pf3.tags.isEmpty() ? "" : ("<span class='update'>"+pf3.tags+"</span>")).append("</h4>\n")
									.append("<div class='topic'>").append("<!-- START: ").append(pf3.fullNumber).append(" - " ).append(pf3.fullId).append(" -->\n")
									.append(pf3.contents).append("\n")
									.append("</div>").append("<!-- END: ").append(pf3.fullNumber).append(" - ").append(pf3.fullId).append(" -->\n");
							}

							toc.append("\t\t</ol>\n");
						}

						contents
							.append("</div>").append("<!-- END: ").append(pf2.fullNumber).append(" - ").append(pf2.fullId).append(" -->\n");
					}

					toc.append("\t</ol>\n");
				}

				contents
					.append("</div>").append("<!-- END: ").append(pf1.fullNumber).append(" - ").append(pf1.fullId).append(" -->\n");
			}

			StringBuilder tocRn = new StringBuilder("<ul class='toc'>\n"), rn = new StringBuilder();

			for (ReleaseFile rf : releaseNotes.releaseFiles) {
				tocRn
					.append("<li><p><a class='doclink' href='{OVERVIEW_URL}#").append(rf.version).append("'>").append(rf.title).append("</a></p>\n");
				rn
					.append("\n")
					.append("<!-- ==================================================================================================== -->\n\n")
					.append("<h3 class='topic' onclick='toggle(this)'><a href='#").append(rf.version).append("' id='").append(rf.version).append("'>").append(rf.title).append("</a></h3>\n")
					.append("<div class='topic'>").append("<!-- START: ").append(rf.version).append(" -->\n")
					.append(rf.contents).append("\n")
					.append("</div>").append("<!-- END: ").append(rf.version).append(" -->\n");
			}

			toc.append("</ol>\n");
			tocRn.append("</ul>\n");

			template = template
				.replace("{TOC-CONTENTS}", toc.toString())
				.replace("{CONTENTS}", contents.toString())
				.replace("{TOC-RELEASE-NOTES}", tocRn)
				.replace("{RELEASE-NOTES}", rn)
				.replace("{CONFIG-PROPS}", configProps)
				.replace("{OVERVIEW_URL}", "");

			IOUtils.writeFile("src/main/javadoc/overview.html", template);

			ds.save(new File("src/main/javadoc/resources/docs.txt"));

			info("Generated target/overview.html in {0}ms", System.currentTimeMillis()-startTime);

			startTime = System.currentTimeMillis();
			for (File f : new File("src/main/javadoc/doc-files").listFiles())
				Files.delete(f.toPath());
			for (File f : new File("src/main/javadoc/resources/fragments").listFiles())
				Files.delete(f.toPath());

			for (File f : topics.docFiles)
				Files.copy(f.toPath(), Paths.get("src/main/javadoc/doc-files", f.getName()), StandardCopyOption.REPLACE_EXISTING);
			for (File f : releaseNotes.docFiles)
				Files.copy(f.toPath(), Paths.get("src/main/javadoc/doc-files", f.getName()), StandardCopyOption.REPLACE_EXISTING);

			String toc2 = new StringBuilder().append("<!--").append(COPYRIGHT).append("\n-->\n").append(toc).toString();
			IOUtils.writeFile("src/main/javadoc/resources/fragments/toc.html", toc2);

			String tocRn2 = new StringBuilder().append("<!--").append(COPYRIGHT).append("\n-->\n").append(tocRn).toString();
			IOUtils.writeFile("src/main/javadoc/resources/fragments/rntoc.html", tocRn2);

			info("Copied doc-files in {0}ms", System.currentTimeMillis()-startTime);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void reorder(File dir) throws IOException {
		Topics t = new Topics(dir);

		int i = 1;
		for (PageFile f : t.pageFiles) {
			f.reorder(i++);
		}

		t = new Topics(dir);
		for (PageFile f : t.pageFiles)
			if (f.dir != null)
				reorder(f.dir);
	}

	static class Topics {
		Set<PageFile> pageFiles = new TreeSet<>();
		List<File> docFiles = new ArrayList<>();

		public Topics(File topicsDir) throws IOException {
			for (File f : topicsDir.listFiles()) {
				if (! f.getName().startsWith(".")) {
					if (f.isFile())
						pageFiles.add(new PageFile(null, f, docFiles));
					else if (f.isDirectory() && f.getName().contains("doc-files"))
						docFiles.addAll(Arrays.asList(f.listFiles()));
				}
			}
		}
	}

	static class ReleaseNotes {
		Set<ReleaseFile> releaseFiles = new TreeSet<>();
		List<File> docFiles = new ArrayList<>();

		public ReleaseNotes(File releaseNotesDir) throws IOException {
			for (File f : releaseNotesDir.listFiles()) {
				if (f.isFile())
					releaseFiles.add(new ReleaseFile(f));
				else if (f.isDirectory() && f.getName().contains("doc-files"))
					docFiles.addAll(Arrays.asList(f.listFiles()));
			}
		}
	}

	static class PageFile implements Comparable<PageFile> {
		String idWithNum, id, num, fullId, title, contents;
		String fullNumber;
		int pageNumber, dirNumber;
		String tags = "";
		File file, dir;

		Set<PageFile> pageFiles = new TreeSet<>();

		PageFile(PageFile parent, File f, List<File> docFiles) throws IOException {
			this.file = f;
			try {
				String n = f.getName();
				idWithNum = n.substring(0, n.lastIndexOf('.'));
				num = n.substring(0, n.indexOf('.'));
				id = idWithNum.substring(n.indexOf('.') + 1);
				fullId = (parent == null ? "" : parent.fullId + ".") + id;
				pageNumber = Integer.parseInt(n.substring(0, n.indexOf('.')));
				fullNumber = (parent == null ? "" : parent.fullNumber + ".") + pageNumber;
				String s = IOUtils.read(f);
				int i = s.indexOf("-->");
				s = s.substring(i+4).trim();
				i = s.indexOf("\n");
				title = s.substring(0, i);
				if (title.startsWith("{")) {
					tags = title.substring(1, title.indexOf('}'));
					if (juneauTagPattern != null)
						tags = tags.replaceAll(juneauTagPattern, "<b>$0</b>");
					s = s.substring(i+1);
					i = s.indexOf("\n");
					title = s.substring(0, i);
				}
				if (s.contains("{@link org.apache.juneau."))
					WARNINGS.add("Found {@link org.apache.juneau...} in file " + f.getAbsolutePath());
				contents = s.substring(i).trim()
					.replaceAll("oaj\\.", "org.apache.juneau.")
					.replaceAll("oajr\\.", "org.apache.juneau.rest.")
					.replaceAll("oajrc\\.", "org.apache.juneau.rest.client.")
				;
			} catch (Exception e) {
				WARNINGS.add("Problem with file " + f.getAbsolutePath() +", " + e.getMessage());
				return;
			}

			for (File d : f.getParentFile().listFiles()) {
				if (d.isDirectory()) {
					String n = d.getName();
					if (n.matches("\\d+\\..*")) {
						int dirNumber = Integer.parseInt(n.substring(0, n.indexOf('.')));
						String dirName = n.substring(n.indexOf('.') + 1);
						if (dirName.equals(id)) {
							this.dirNumber = dirNumber;
							dir = d;
							for (File f2 : d.listFiles()) {
								if (f2.isFile())
									pageFiles.add(new PageFile(this, f2, docFiles));
								else if (f2.isDirectory() && f2.getName().contains("doc-files"))
									docFiles.addAll(Arrays.asList(f2.listFiles()));
							}
						}
					}
				}
			}
		}

		public String getPageDirName(File f) {
			String n = f.getName();
			n = n.substring(n.indexOf('.') + 1);
			return n;
		}

		void reorder(int i) {
			if (pageNumber != i) {
				File f2 = new File(file.getParentFile(), String.format("%0"+num.length()+"d", i) + '.' + id + ".html");
				info("Renaming {0} to {1}", file.getName(), f2.getName());
				file.renameTo(f2);
			}
			if (dir != null && dirNumber != i) {
				File f2 = new File(file.getParentFile(), String.format("%0"+num.length()+"d", i) + '.' + id);
				info("Renaming {0} to {1}", dir.getName(), f2.getName());
				dir.renameTo(f2);
			}
		}

		@Override
		public int compareTo(PageFile o) {
			return this.idWithNum.compareTo(o.idWithNum);
		}
	}

	static void printWarnings() {
		if (WARNINGS.isEmpty())
			info("No DocGenerator warnings.");
		else {
			warning(WARNINGS.size()+" DocGenerator warnings:");
			for (int i = 0; i < WARNINGS.size(); i++)
				warning("["+(i+1)+"] " + WARNINGS.get(i));
		}
	}

	static class ReleaseFile implements Comparable<ReleaseFile> {
		String name, version, title, contents;

		ReleaseFile(File f) throws IOException {
			name = f.getName();
			name = name.substring(0, name.lastIndexOf('.'));
			version = name.replaceAll("[0](\\d+)", "$1");
			String s = IOUtils.read(f);
			int i = s.indexOf("-->");
			s = s.substring(i+4).trim();
			i = s.indexOf("\n");
			title = s.substring(0, i);
			contents = s.substring(i).trim()
				.replaceAll("oaj\\.", "org.apache.juneau.")
				.replaceAll("oajr\\.", "org.apache.juneau.rest.")
				.replaceAll("oajrc\\.", "org.apache.juneau.rest.client.")
			;
		}

		@Override
		public int compareTo(ReleaseFile o) {
			return this.name.compareTo(o.name);
		}
	}
}
