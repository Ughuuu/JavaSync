package org.jsync.sync;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.eclipse.jdt.core.compiler.batch.BatchCompiler;

import lombok.Getter;
import lombok.val;

/**
 * The Sync class contains a T class instance that changes if the linked file
 * changes. If that happens, the class is recompiled.
 */
public class SourceSync {
	/**
	 * Options from the Eclipse Java Core Compiler Package
	 */
	public static String options = "";

	/**
	 * Update all the given classes at once. They should be from the same folder
	 * as source and have the same destination folder and the same options.
	 * 
	 * @param syncs
	 *            The sync classes, all in the same root folder.
	 * @return Wether the compilation succeded and changes happened.
	 * @throws Exception
	 */
	public static boolean updateSource(SourceSync[] syncs) {
		// check if update is needed
		boolean areDirty = false;
		for (val sync : syncs) {
			sync.compileError = "";
			sync.compileOutput = "";
			areDirty = areDirty | sync.isSourceDirty();
		}
		if (!areDirty) {
			return false;
		}
		// check if files exist
		val first = syncs[0];
		boolean result = true;
		for (val sync : syncs) {
			if (!sync.javaFile.exists() || sync.javaFile.isDirectory()) {
				sync.compileError += "Cannot find file: " + sync.javaFile.getPath() + '\n';
				result = false;
			}
		}
		if (result == false) {
			return false;
		}
		for (val sync : syncs) {
			sync.lastCompiled = sync.javaFile.lastModified();
		}
		String filesFolder = first.folderSourceName;

		val errorWriter = new StringWriter();
		val outputWriter = new StringWriter();
		val errorStream = new PrintWriter(errorWriter);
		val outputStream = new PrintWriter(outputWriter);
		val success = BatchCompiler.compile(
				filesFolder + " -d " + first.folderDestinationName + " -cp " + System.getProperty("java.class.path")
						+ ";" + first.folderDestinationName + " " + SourceSync.options,
				outputStream, errorStream, null);
		// handle error for each
		for (val sync : syncs) {
			sync.compileError = errorWriter.toString();
			sync.compileOutput = outputWriter.toString();
		}
		return success && "".equals(errorWriter.toString());
	}

	/**
	 * The full name of the class, with the package. The package contains
	 * dots(.) and the class and package are united by a dot(.) as well.
	 */
	@Getter
	private final String className;

	/**
	 * An instance of file for the class given by the class name. It has .java
	 * at the end and is located at the source folder.
	 */
	private final File javaFile;

	/**
	 * The source folder of the class. This is relative to the root project.
	 */
	@Getter
	private final String folderSourceName;

	/**
	 * The destination folder of the class. This is relative to the root
	 * project.
	 */
	@Getter
	private final String folderDestinationName;

	/**
	 * The date the file containing the class was last compiled.
	 */
	@Getter
	private long lastCompiled;

	/**
	 * The output from the compile of the source file.
	 */
	@Getter
	private String compileOutput = "";

	/**
	 * The error from the compile of the source file.
	 */
	@Getter
	private String compileError = "";

	/**
	 * Construct a new Sync object for the specified className.
	 * 
	 * @param className
	 *            The full name of the class to be loaded
	 * @param folderSourceName
	 *            The source folder name
	 * @param folderDestinationName
	 *            The destination folder name
	 */
	public SourceSync(String className, String folderSourceName, String folderDestinationName) {
		if (!(folderDestinationName.endsWith("/") || folderDestinationName.endsWith("\\"))) {
			folderDestinationName += '/';
		}
		if (folderSourceName.equals("")) {
			folderSourceName = "./";
		}
		if (!(folderSourceName.endsWith("/") || folderSourceName.endsWith("\\"))) {
			folderSourceName += '/';
		}
		if (folderDestinationName.equals("")) {
			folderDestinationName = "./";
		}
		this.className = className;
		this.folderDestinationName = folderDestinationName;
		this.folderSourceName = folderSourceName;
		this.javaFile = new File(folderSourceName + (className.replace('.', '/') + ".java").toString());
	}

	public boolean hasSource() {
		return javaFile.exists();
	}

	/**
	 * Checks if the source file has been modified
	 * 
	 * @return The modified state
	 */
	public boolean isSourceDirty() {
		return javaFile.lastModified() != lastCompiled || lastCompiled == 0;
	}
}