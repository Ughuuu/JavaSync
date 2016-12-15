package org.jsync.sync;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.compiler.batch.BatchCompiler;

import lombok.Getter;
import lombok.Setter;
import lombok.val;

/**
 * The Sync class contains a T class instance that changes if the linked file
 * changes. If that happens, the class is recompiled.
 * 
 * @author Dragos
 *
 * @param <T>
 *            The class type
 */
public class Sync<T> {
	private final static ClassLoader classLoader = Sync.class.getClassLoader();
	private final String folderSourceName;
	private final String folderDestinationName;
	private String options = "";
	private long lastModified;
	private boolean changed = false;
	private static final List<Sync> nullList = new ArrayList<Sync>();
	private URLClassLoader loader;

	/**
	 * Instance of the loaded class. May change depending on class reloading.
	 */
	@Getter
	private T instance = null;

	/**
	 * The full name of the loaded class, with the package
	 */
	@Setter
	@Getter
	private String className;

	/**
	 * Constructor that calls the loadFromFile function.
	 * 
	 * @param filePath
	 *            The path of the file to keep in sync with
	 * @param className
	 *            The class name
	 * @throws Exception
	 */
	public Sync(String className) {
		this(className, "res/.src", "res/.src");
	}

	/**
	 * Constructor that calls the loadFromFile function.
	 * 
	 * @param filePath
	 *            The path of the file to keep in sync with
	 * @param className
	 *            The class name
	 * @throws Exception
	 */
	public Sync(String className, String folderNameSource, String folderNameDestination) {
		this.folderSourceName = folderNameSource;
		this.folderDestinationName = folderNameDestination;
		this.className = className;
	}

	public boolean getChanged() {
		boolean changedReturn = changed;
		changed = false;
		return changedReturn;
	}

	public boolean needsChange() {
		StringBuilder sourceName = new StringBuilder(folderSourceName + "/");
		sourceName.append(className.replace('.', '/') + ".java");
		long newLastModified = new File(sourceName.toString()).lastModified();
		return newLastModified != lastModified || newLastModified == 0;
	}

	@SuppressWarnings("unchecked")
	/**
	 * Load the class instance for the first time from the given file as the
	 * given name.
	 * 
	 * @param filePath
	 *            The path to the file that contains the java code
	 * @param className
	 *            The class name, including the package
	 * @throws ClassNotFoundException
	 *             Wrong given className
	 * @throws IOException
	 *             Cannot open file
	 * @throws InstantiationException
	 *             ?
	 * @throws IllegalAccessException
	 *             ?
	 */
	public String loadFromFile(List<Sync> syncs) throws Exception {
		StringBuilder sourceNames = new StringBuilder(folderSourceName + "/");
		sourceNames.append(className.replace('.', '/') + ".java");
		String thisFile = sourceNames.toString();
		for (int i = 0; i < syncs.size(); i++) {
			sourceNames.append(
					" " + syncs.get(i).folderSourceName + "/" + syncs.get(i).className.replace('.', '/') + ".java");
		}
		String files = sourceNames.toString();
		long newLastModified = new File(thisFile).lastModified();

		lastModified = newLastModified;

		val errorStringWriter = new StringWriter();
		val outputStringWriter = new StringWriter();
		val errorStream = new PrintWriter(errorStringWriter);
		val outputStream = new PrintWriter(outputStringWriter);
		val success = BatchCompiler.compile(files + " -d " + folderDestinationName + " -cp "
				+ System.getProperty("java.class.path") + ";" + folderDestinationName + " " + options, outputStream,
				errorStream, null);
		if (success == false) {
			return errorStringWriter.toString();
		}
		instance = null;

		updateClass();

		return errorStringWriter.toString();
	}

	private void updateClass() throws Exception {
		val loadedClass = loader.loadClass(className);
		// Instantiate the object
		instance = (T) loadedClass.newInstance();
		changed = true;
	}

	/**
	 * Resyncs the class instance if needed. If the class name is changed, call
	 * with String parameter.
	 * 
	 * @throws Exception
	 * @throws IOException
	 */
	public String update() throws Exception {
		if (loader != null) {
			loader.close();
			loader = null;
		}
		loader = URLClassLoader.newInstance(new URL[] { new File(folderDestinationName).toURI().toURL() }, classLoader);
		return loadFromFile(nullList);
	}

	public String update(List<Sync> others) throws Exception {
		StringBuilder err = new StringBuilder();
		if (loader != null) {
			loader.close();
			loader = null;
		}
		loader = URLClassLoader.newInstance(new URL[] { new File(folderDestinationName).toURI().toURL() }, classLoader);
		err.append(loadFromFile(others));
		// we compiled all files, try to update them now
		try {
			for (int i = 0; i < others.size(); i++) {
				others.get(i).loader = loader;
				others.get(i).updateClass();
			}
		} catch (Exception e) {
			// do nothing, error is already sent down
		}
		return err.toString();
	}

	public void reloadClasses(List<Sync> others) {
		try {
			for (int i = 0; i < others.size(); i++) {
				if (others.get(i).getInstance() != null) {
					others.get(i).loader = loader;
					others.get(i).updateClass();
				}
			}
		} catch (Exception e) {
			// do nothing, error is already sent down
		}
	}

	public Sync<T> setOptions(String options) {
		this.options = options;
		return this;
	}
}