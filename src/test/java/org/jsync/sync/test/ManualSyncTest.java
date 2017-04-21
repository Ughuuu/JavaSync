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
 */
public class ManualSyncTest {
	private static final String className = "org.jsync.sync.test.ManualTest1";

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String args[]) throws Exception {

		String fileName = className.replace('.', '/');
		System.out.println(fileName);
		File file = new File(fileName);
		file.mkdirs();

		PrintWriter writer = new PrintWriter(file + ".java");
		writer.print(
				  "package org.jsync.sync.test;\n" 
				+ "\n" 
				+ "public class ManualTest1 implements TestInterface{\n"
				+ "	public final static boolean result = false;\n" 
				+ "	\n" 
				+ "	public String getResult(){\n"
				+ "		return \"Hello World\";\n" 
				+ "	}\n" 
				+ "}\n");
		writer.flush();
		writer.close();
		Sync.options = "-8";
		val loadClass = new Sync<TestInterface>(ManualSyncTest.class.getClassLoader(), className, "org",
				"res/src/");
		TestInterface obj;
		Sync[] arr = new Sync[1];
		arr[0] = loadClass;
		while (true) {
			if (loadClass.isSourceDirty()) {
				if (Sync.updateSource(arr) && Sync.updateClass(arr)) {
					obj = loadClass.newInstance();
					String result = obj.getResult();
					System.out.println(result);
				} else {
					System.out.println(loadClass.getCompileError());
				}
			}
		}
	}
}