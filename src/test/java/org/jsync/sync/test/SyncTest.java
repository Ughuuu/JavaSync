package org.jsync.sync.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.jsync.sync.ClassSync;
import org.jsync.sync.Commiter;
import org.jsync.sync.SourceSync;
import org.junit.Test;

import lombok.val;

/**
 * Tests the functionality of Sync class.
 * 
 * @author Dragos
 *
 */
public class SyncTest {
	private static final String classNameA = "org.jsync.sync.test.SynceeA";
	private static final String classNameB = "org.jsync.sync.test.SynceeB";

	private void makeSource(String classFullName, String className) throws IOException {
		String fileName = classFullName.replace('.', '/');
		System.out.println(fileName);
		File file = new File(fileName);
		file.mkdirs();
		new File("./res/src/").mkdirs();

		PrintWriter writer = new PrintWriter(file + ".java");
		writer.print("package org.jsync.sync.test;\n" + "\n" + "public class " + className + "{\n"
				+ "	public final static boolean result = false;\n" + "	\n" + "	public String getResult(){\n"
				+ "		return \"this was \" + result;\n" + "	}\n" + "}\n");
		writer.flush();
		writer.close();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testSimpleLoadSync() throws Exception {
		makeSource(classNameA, "SynceeA");
		makeSource(classNameB, "SynceeB");
		SourceSync.options = "-8";
		val sourceClassA = new SourceSync(classNameA, "./org", "./res/src/");
		val sourceClassB = new SourceSync(classNameB, "./org", "./res/src/");
		val classA = new ClassSync(this.getClass().getClassLoader(), classNameA, "./res/src/");
		val classB = new ClassSync(this.getClass().getClassLoader(), classNameB, "./res/src/");
		assertSame("This class needs to change", true, sourceClassA.isSourceDirty());
		assertSame("This class needs to change", true, sourceClassB.isSourceDirty());
		List<SourceSync> listSource = new ArrayList<SourceSync>();
		listSource.add(sourceClassA);
		listSource.add(sourceClassB);
		List<ClassSync> listClass = new ArrayList<ClassSync>();
		listClass.add(classA);
		listClass.add(classB);
		SourceSync.updateSource(listSource.toArray(new SourceSync[0]));
		ClassSync.updateClass(listClass.toArray(new ClassSync[0]));
		assertNotNull("The class has not been loaded", classA.newInstance());
		assertNotNull("The class has not been loaded", classB.newInstance());
		assertSame("This class doesn't need to change", false, sourceClassA.isSourceDirty());
		assertSame("This class doesn't need to change", false, sourceClassB.isSourceDirty());
		assertSame("this was false",
				classA.newInstance().getClass().getMethod("getResult").invoke(classA.newInstance()));
		assertSame("this was false",
				classB.newInstance().getClass().getMethod("getResult").invoke(classB.newInstance()));
		assertSame(classNameA, classA.newInstance().getClass().getName());
		assertSame(classNameB, classB.newInstance().getClass().getName());

		String fileName = classNameA.replace('.', '/');
		System.out.println(fileName);
		File file = new File(fileName);

		PrintWriter writerFinal = new PrintWriter(file + ".java");
		writerFinal.print("package org.jsync.sync.test;\n" + "\n" + "public class SynceeA{\n"
				+ "	public final static boolean result = true;\n" + "	\n" + "	public String getResult(){\n"
				+ "		return \"this was \" + result;\n" + "	}\n" + "}\n");
		writerFinal.flush();
		writerFinal.close();
		assertSame("This class needs to change", true, sourceClassA.isSourceDirty());
	}
}
