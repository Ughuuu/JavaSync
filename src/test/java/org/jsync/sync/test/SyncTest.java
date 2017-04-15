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

import org.jsync.sync.Commiter;
import org.jsync.sync.Sync;
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

	private void makeSource(String classFullName, String className) throws IOException{
		String fileName = classFullName.replace('.', '/');
		System.out.println(fileName);
		File file = new File(fileName);
		file.mkdirs();

		PrintWriter writer = new PrintWriter(file + ".java");
		writer.print("package org.jsync.sync.test;\n" + "\n" + "public class "+className+"{\n"
				+ "	public final static boolean result = false;\n" + "	\n" + "	public String getResult(){\n"
				+ "		return \"this was \" + result;\n" + "	}\n" + "}\n");
		writer.flush();
		writer.close();
	}
	
	@SuppressWarnings("rawtypes")
	@Test
	public void testSimpleLoadSync() throws Exception {
		makeSource(classNameA, "SynceeA");
		makeSource(classNameB, "SynceeB");
		Sync.options = "-8";
		val loadClassA = new Sync<Object>(this.getClass().getClassLoader(), classNameA, "org/", "res/src");
		val loadClassB = new Sync<Object>(this.getClass().getClassLoader(), classNameB, "org/", "res/src");
		assertSame("This class needs to change", true, loadClassA.isDirty());
		assertSame("This class needs to change", true, loadClassB.isDirty());
		List<Sync> list = new ArrayList<Sync>();
		list.add(loadClassB);
		list.add(loadClassA);
		Sync.updateAll(list);
		System.out.println(list.get(1).getCompileError());
		assertNotNull("The class has not been loaded", loadClassA.newInstance());
		assertNotNull("The class has not been loaded", loadClassB.newInstance());
		assertSame("This class doesn't need to change", false, loadClassA.isDirty());
		assertSame("This class doesn't need to change", false, loadClassB.isDirty());
		assertSame("this was false",
				loadClassA.newInstance().getClass().getMethod("getResult").invoke(loadClassA.newInstance()));
		assertSame("this was false",
				loadClassB.newInstance().getClass().getMethod("getResult").invoke(loadClassB.newInstance()));
		assertSame(classNameA, loadClassA.newInstance().getClass().getName());
		assertSame(classNameB, loadClassB.newInstance().getClass().getName());
		
		String fileName = classNameA.replace('.', '/');
		System.out.println(fileName);
		File file = new File(fileName);
		
		PrintWriter writerFinal = new PrintWriter(file + ".java");
		writerFinal.print("package org.jsync.sync.test;\n" + "\n" + "public class SynceeA{\n"
				+ "	public final static boolean result = true;\n" + "	\n" + "	public String getResult(){\n"
				+ "		return \"this was \" + result;\n" + "	}\n" + "}\n");
		writerFinal.flush();
		writerFinal.close();
		assertSame("This class needs to change", true, loadClassA.isDirty());
		System.out.println(Sync.update(loadClassA));

		assertSame("this was true",
				loadClassA.newInstance().getClass().getMethod("getResult").invoke(loadClassA.newInstance()));
	}
}
