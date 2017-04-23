package org.jsync.sync.test;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.jsync.sync.ClassSync;
import org.jsync.sync.Commiter;
import org.jsync.sync.SourceSync;

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
		writer.print("package org.jsync.sync.test;\n" + "\n" + "public class ManualTest1 implements TestInterface{\n"
				+ "	public final static boolean result = false;\n" + "	\n" + "	public String getResult(){\n"
				+ "		return \"Hello World\";\n" + "	}\n" + "}\n");
		writer.flush();
		writer.close();
		SourceSync.options = "-8";
		val sourceSync = new SourceSync(className, "org", "res/src/");
		val classSync = new ClassSync<TestInterface>(ManualSyncTest.class.getClassLoader(), className, "res/src/");
		TestInterface obj;
		SourceSync[] arrS = new SourceSync[1];
		ClassSync[] arrC = new ClassSync[1];
		arrS[0] = sourceSync;
		arrC[0] = classSync;
		while (true) {
			if (sourceSync.isSourceDirty()) {
				if (SourceSync.updateSource(arrS)) {
				} else {
					System.out.println(sourceSync.getCompileError());
				}
			}
			if (classSync.isClassDirty()) {
				if (ClassSync.updateClass(arrC)) {
					obj = classSync.newInstance();
					String result = obj.getResult();
					System.out.println(result);
				}
			}
		}
	}
}