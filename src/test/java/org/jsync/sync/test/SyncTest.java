package org.jsync.sync.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.io.File;
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
	private static final String className = "org.jsync.sync.test.Syncee";

	@Test
	public void testSimpleLoadSync() throws Exception {

		String fileName = "res/.src/" + className.replace('.', '/');
		System.out.println(fileName);
		File file = new File(fileName);
		file.mkdirs();

		Files.setAttribute(Paths.get("res/.src"), "dos:hidden", true);

		PrintWriter writer = new PrintWriter(file + ".java");
		writer.print("package org.jsync.sync.test;\n" + "\n" + "public class Syncee{\n"
				+ "	public final static boolean result = false;\n" + "	\n" + "	public String getResult(){\n"
				+ "		return \"this was \" + result;\n" + "	}\n" + "}\n");
		writer.flush();
		writer.close();
		val loadClass = new Sync<Object>(className, "res/.src", "res/.src");
		System.out.println(loadClass.update());
		assertNotNull("The class has not been loaded", loadClass.getInstance());
		assertSame("this was false",
				loadClass.getInstance().getClass().getMethod("getResult").invoke(loadClass.getInstance()));
		assertSame(className, loadClass.getInstance().getClass().getName());
		PrintWriter writerFinal = new PrintWriter(file + ".java");
		writerFinal.print("package org.jsync.sync.test;\n" + "\n" + "public class Syncee{\n"
				+ "	public final static boolean result = true;\n" + "	\n" + "	public String getResult(){\n"
				+ "		return \"this was \" + result;\n" + "	}\n" + "}\n");
		writerFinal.flush();
		writerFinal.close();
		loadClass.update();

		assertSame("this was true",
				loadClass.getInstance().getClass().getMethod("getResult").invoke(loadClass.getInstance()));
	}
}
