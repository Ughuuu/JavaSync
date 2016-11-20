package org.jsync.sync.test;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.jsync.sync.Commiter;
import org.jsync.sync.Sync;

import lombok.val;

/**
 * Tests the functionality of Sync class.
 * 
 * @author Dragos
 *
 */
public class ManualSyncTest {
	private static final String className = "org.jsync.sync.test.ManualTest";

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String args[]) throws Exception {

		String fileName = "res/.src/" + className.replace('.', '/');
		System.out.println(fileName);
		File file = new File(fileName);
		file.mkdirs();

		Files.setAttribute(Paths.get("res/.src"), "dos:hidden", true);

		PrintWriter writer = new PrintWriter(file + ".java");
		writer.print("package org.jsync.sync.test;\n" + "\n" + "public class ManualTest implements TestInterface{\n"
				+ "	public final static boolean result = false;\n" + "	\n" + "	public String getResult(){\n"
				+ "		return \"Hello World\";\n" + "	}\n" + "}\n");
		writer.flush();
		writer.close();
		val loadObject = new Sync<TestInterface>(className, "res/.src", "class/").setOptions("-1.7");
		while (true) {
			System.out.println("Error " + loadObject.update() + " .");
			if (loadObject.getInstance() != null) {
				String result = loadObject.getInstance().getResult();
				System.out.println(result);
			}
		}
	}
}