package proje;
import com.github.javaparser.*;

import java.util.HashMap;
import java.nio.file.Files;
import java.nio.file.FileVisitResult;
import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.Path;
import java.nio.file.Paths;
import com.github.javaparser.ast.CompilationUnit;

import java.util.ArrayList;
public class ProjectParser {

	HashMap<String, CompilationUnit> classDict = new HashMap<>();
	private class V extends SimpleFileVisitor<Path>{
		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
			var jp = new JavaParser();
			if (attr.isRegularFile() && file.toString().endsWith(".java")) {
				try {
					var parsation = jp.parse(file).getResult().get();
					var fileName = srcPath.relativize(parsation.getStorage().get().getPath()).toString();
					classDict.put(fileName, parsation);
				} catch (IOException e) {}
				
			}	
			return FileVisitResult.CONTINUE;
		}
		
	}
	
	Path srcPath;
	public static HashMap<String, CompilationUnit>  parse(String arg) {
		var thing = new ProjectParser();
		thing.srcPath = Paths.get(arg);
		try {
			Files.walkFileTree(thing.srcPath, thing.new V());
		} catch (IOException e) {}
		return thing.classDict;

	}
	
}
