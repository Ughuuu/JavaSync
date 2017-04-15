package org.jsync.sync;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import org.eclipse.jdt.core.compiler.batch.BatchCompiler;

import lombok.Getter;
import lombok.val;

/**
 * The Sync class contains a T class instance that changes if the linked file
 * changes. If that happens, the class is recompiled.
 */
public class Sync<T> {
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
	private final File classFile;

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
	 * Options from the Eclipse Java Core Compiler Package
	 */
	public static String options = "";

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

	private URLClassLoader urlClassLoader;
	private final ClassLoader classLoader;

	/**
	 * Construct a new Sync object for the specified className.
	 * 
	 * @param classLoader
	 *            The class loader this will inherit from
	 * @param className
	 *            The full name of the class to be loaded
	 * @param folderSourceName
	 *            The source folder name
	 * @param folderDestinationName
	 *            The destination folder name
	 */
	public Sync(ClassLoader classLoader, String className, String folderSourceName, String folderDestinationName) {
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
		this.classLoader = classLoader;
		this.folderDestinationName = folderDestinationName;
		this.folderSourceName = folderSourceName;
		this.classFile = new File((className.replace('.', '/') + ".java").toString());
	}

	/**
	 * Get a new instance of the class or null
	 * 
	 * @return A new instance or null.
	 */
	@SuppressWarnings("unchecked")
	public T newInstance() {
		try {
			return (T) urlClassLoader.loadClass(className).newInstance();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Checks if the source file has been modified
	 * 
	 * @return The modified state
	 */
	public boolean isDirty() {
		return classFile.lastModified() != lastCompiled || lastCompiled == 0;
	}

	/**
	 * Update all the given classes at once. They should be from the same folder
	 * as source and have the same destination folder and the same options.
	 * 
	 * @param syncs
	 *            The sync classes, all in the same root folder.
	 * @return Wether the compilation succeded and changes happened.
	 * @throws Exception
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static boolean updateAll(List<Sync> syncs) {
		// check if update is needed
		boolean areDirty = false;
		for (val sync : syncs) {
			sync.compileError = "";
			sync.compileOutput = "";
			areDirty = areDirty | sync.isDirty();
		}
		if (!areDirty) {
			return false;
		}
		// check if files exist
		val first = syncs.get(0);
		URL url;
		boolean result = true;
		for (val sync : syncs) {
			if (!sync.classFile.exists() || sync.classFile.isDirectory()) {
				sync.compileError += "Cannot find file: " + sync.classFile.getPath() + '\n';
				result = false;
			}
		}
		if (result == false) {
			return false;
		}
		for (val sync : syncs) {
			sync.lastCompiled = sync.classFile.lastModified();
		}
		String filesFolder = first.folderSourceName;

		val errorWriter = new StringWriter();
		val outputWriter = new StringWriter();
		val errorStream = new PrintWriter(errorWriter);
		val outputStream = new PrintWriter(outputWriter);
		val success = BatchCompiler.compile(filesFolder + " -d " + first.folderDestinationName + " -cp "
				+ System.getProperty("java.class.path") + ";" + first.folderDestinationName + " " + Sync.options,
				outputStream, errorStream, null);
		if ("".equals(errorWriter.toString()) && success) {
			// check if generated classes exists
			try {
				File root = new File(first.folderDestinationName);
				if (!root.exists() || !root.isDirectory()) {
					for (val sync : syncs) {
						sync.compileError += "Cannot get containing folder: " + root.getPath() + '\n';
					}
					return false;
				}
				url = root.toURI().toURL();
			} catch (Exception e) {
				for (val sync : syncs) {
					sync.compileError += e.toString() + '\n';
				}
				return false;
			}
			val urlClassLoader = URLClassLoader.newInstance(new URL[] { url }, first.classLoader);
			for (val sync : syncs) {
				try {
					sync.checkClass(urlClassLoader);
				} catch (Exception e) {
					sync.compileError += e.toString();
					result = false;
				}
			}
			// handle error for each
			for (val sync : syncs) {
				sync.compileError = errorWriter.toString();
				sync.compileOutput = outputWriter.toString();
			}
			return result;
		}
		return false;
	}

	/**
	 * Update the given class. If it depends on other classes, use updateAll.
	 * 
	 * @param sync
	 *            The sync classes, all in the same root folder.
	 * @return If the compilation succeeded and changes happened.
	 * @throws Exception
	 */
	public static <T> boolean update(Sync<T> sync) {
		sync.compileError = "";
		sync.compileOutput = "";
		if (!sync.isDirty()) {
			return false;
		}
		// check if file exist
		try {
			if (!sync.classFile.exists() || sync.classFile.isDirectory()) {
				sync.compileError += "Cannot find file: " + sync.classFile.getPath() + '\n';
				return false;
			}
		} catch (Exception e) {
			sync.compileError += e.toString();
			return false;
		}
		sync.lastCompiled = sync.classFile.lastModified();

		val errorWriter = new StringWriter();
		val outputWriter = new StringWriter();
		val errorStream = new PrintWriter(errorWriter);
		val outputStream = new PrintWriter(outputWriter);
		val compileCommand = (sync.className.replace('.', '/') + ".java").toString() + " -d "
				+ sync.folderDestinationName + " -cp " + System.getProperty("java.class.path") + ";"
				+ sync.folderDestinationName + " " + Sync.options;
		val success = BatchCompiler.compile(compileCommand, outputStream, errorStream, null);
		sync.compileError = errorWriter.toString();
		sync.compileOutput = outputWriter.toString();
		if ("".equals(sync.compileError) && success) {

			URL url;
			try{
				File root = new File(sync.folderDestinationName);
				url = root.toURI().toURL();
			}catch (Exception e) {
				sync.compileError += e.toString();
				return false;
			}
			val urlClassLoader = URLClassLoader.newInstance(new URL[] { url }, sync.classLoader);
			try {
				sync.checkClass(urlClassLoader);
			} catch (Exception e) {
				sync.compileError += e.toString();
				return false;
			}
			return true;
		}
		return false;
	}

	private void checkClass(URLClassLoader newClassLoader) throws Exception {
		newClassLoader.loadClass(className);
		if (urlClassLoader != null)
			urlClassLoader.close();
		urlClassLoader = newClassLoader;
	}
}