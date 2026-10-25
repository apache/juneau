// Parse-only check of Java snippets, used by check-java-fences.py.  No symbol resolution, no compile.
// Usage: java JavaFenceParse.java <dir> <count>   (reads <dir>/0.java.txt .. <dir>/<count-1>.java.txt)
// Prints "FAIL <n>" for each snippet that parses neither as a compilation unit, nor in a class body, nor in a method body,
// nor as a sequence of independent pieces (e.g. a class declaration followed by loose statements).  The sequence split
// is done by JShell's completeness analysis; every piece is then parsed strictly by javac, because JShell alone accepts
// incomplete constructs such as "class Pet { ... }".
import com.sun.source.util.JavacTask;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import javax.tools.*;
import jdk.jshell.JShell;
import jdk.jshell.SourceCodeAnalysis;
import jdk.jshell.SourceCodeAnalysis.CompletionInfo;
import jdk.jshell.SourceCodeAnalysis.Completeness;

public class JavaFenceParse {

	public static void main(String[] args) throws Exception {
		Path dir = Path.of(args[0]);
		int count = Integer.parseInt(args[1]);
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		var quiet = new java.io.PrintStream(java.io.OutputStream.nullOutputStream());
		try (JShell shell = JShell.builder().out(quiet).err(quiet).build()) {
			SourceCodeAnalysis analysis = shell.sourceCodeAnalysis();
			for (int i = 0; i < count; i++) {
				String src = Files.readString(dir.resolve(i + ".java.txt"));
				if (! (parsesAny(compiler, src) || splitParses(compiler, analysis, src)))
					System.out.println("FAIL " + i);
			}
		}
	}

	private static boolean parsesAny(JavaCompiler compiler, String src) throws Exception {
		return parses(compiler, src)
			|| parses(compiler, "class W__ {\n" + src + "\n}")
			|| parses(compiler, "class W__ { void m__() {\n" + src + "\n}}");
	}

	private static boolean splitParses(JavaCompiler compiler, SourceCodeAnalysis analysis, String src) throws Exception {
		String rest = src;
		for (int guard = 0; guard < 500; guard++) {
			CompletionInfo info = analysis.analyzeCompletion(rest);
			if (info.completeness() == Completeness.EMPTY)
				return true;
			if (! info.completeness().isComplete() || info.remaining().equals(rest) || ! parsesAny(compiler, info.source()))
				return false;
			rest = info.remaining();
		}
		return false;
	}

	private static boolean parses(JavaCompiler compiler, String src) throws Exception {
		JavaFileObject file = new SimpleJavaFileObject(URI.create("string:///W__.java"), JavaFileObject.Kind.SOURCE) {
			@Override public CharSequence getCharContent(boolean ignore) { return src; }
		};
		DiagnosticCollector<JavaFileObject> diags = new DiagnosticCollector<>();
		JavacTask task = (JavacTask)compiler.getTask(null, null, diags, List.of("-proc:none"), null, List.of(file));
		task.parse();
		return diags.getDiagnostics().stream().noneMatch(d -> d.getKind() == Diagnostic.Kind.ERROR);
	}
}
