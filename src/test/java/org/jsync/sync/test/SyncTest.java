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
		String fileName = "res/.src/" + classFullName.replace('.', '/');
		System.out.println(fileName);
		File file = new File(fileName);
		file.mkdirs();

		Files.setAttribute(Paths.get("res/.src"), "dos:hidden", true);

		PrintWriter writer = new PrintWriter(file + ".java");
		writer.print("package org.jsync.sync.test;\n" + "\n" + "public class "+className+"{\n"
				+ "	public final static boolean result = false;\n" + "	\n" + "	public String getResult(){\n"
				+ "		return \"this was \" + result;\n" + "	}\n" + "}\n");
		writer.flush();
		writer.close();
	}
	
	@Test
	public void testSimpleLoadSync() throws Exception {
		makeSource(classNameA, "SynceeA");
		makeSource(classNameB, "SynceeB");
		val loadClassA = new Sync<Object>(classNameA, "res/.src", "res/.src");
		val loadClassB = new Sync<Object>(classNameB, "res/.src", "res/.src");
		assertSame("This class needs to change", true, loadClassA.needsChange());
		assertSame("This class needs to change", true, loadClassB.needsChange());
		List<Sync> list = new ArrayList<Sync>();
		list.add(loadClassB);
		loadClassA.update(list);
		assertNotNull("The class has not been loaded", loadClassA.getInstance());
		assertNotNull("The class has not been loaded", loadClassB.getInstance());
		assertSame("This class doesn't need to change", false, loadClassA.needsChange());
		assertSame("This class doesn't need to change", false, loadClassB.needsChange());
		assertSame("this was false",
				loadClassA.getInstance().getClass().getMethod("getResult").invoke(loadClassA.getInstance()));
		assertSame("this was false",
				loadClassB.getInstance().getClass().getMethod("getResult").invoke(loadClassB.getInstance()));
		assertSame(classNameA, loadClassA.getInstance().getClass().getName());
		assertSame(classNameB, loadClassB.getInstance().getClass().getName());
		
		
		String fileName = "res/.src/" + classNameA.replace('.', '/');
		System.out.println(fileName);
		File file = new File(fileName);
		
		PrintWriter writerFinal = new PrintWriter(file + ".java");
		writerFinal.print("package org.jsync.sync.test;\n" + "\n" + "public class SynceeA{\n"
				+ "	public final static boolean result = true;\n" + "	\n" + "	public String getResult(){\n"
				+ "		return \"this was \" + result;\n" + "	}\n" + "}\n");
		writerFinal.flush();
		writerFinal.close();
		assertSame("This class needs to change", true, loadClassA.needsChange());
		System.out.println(loadClassA.update());

		assertSame("this was true",
				loadClassA.getInstance().getClass().getMethod("getResult").invoke(loadClassA.getInstance()));
	}
}
