package org.jsync.sync;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;

import org.eclipse.jdt.core.compiler.batch.BatchCompiler;

import lombok.Getter;
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

	/**
	 * Instance of the loaded class. May change depending on class reloading.
	 */
	@Getter
	private T instance = null;

	/**
	 * The full name of the loaded class, with the package
	 */
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
	public Sync(String className) throws Exception {
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
	public Sync(String className, String folderNameSource, String folderNameDestination) throws Exception {
		this.folderSourceName = folderNameSource;
		this.folderDestinationName = folderNameDestination;
		this.className = className;
		update();
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
	public String loadFromFile(String className) throws Exception {

		val errorStringWriter = new StringWriter();
		val outputStringWriter = new StringWriter();
		val errorStream = new PrintWriter(errorStringWriter);
		val outputStream = new PrintWriter(outputStringWriter);
		val success = BatchCompiler
				.compile(folderSourceName + "/" + className.replace('.', '/') + ".java -d " + folderDestinationName, outputStream,
						errorStream, null);
		if (success == false) {
			return errorStringWriter.toString();
		}
		instance = null;

		URLClassLoader loader = URLClassLoader.newInstance(new URL[] { new File(folderDestinationName).toURI().toURL() },
				classLoader);
		val loadedClass = loader.loadClass(className);
		// Instantiate the object
		instance = (T) loadedClass.newInstance();
		return errorStringWriter.toString();
	}

	/**
	 * Resyncs the class instance if needed. If the class name is changed, call
	 * with String parameter.
	 * 
	 * @throws Exception
	 * @throws IOException
	 */
	public String update() throws IOException, Exception {
		return loadFromFile(className);
	}

	/**
	 * Re-sync the class instance if needed. Also changes the class name.
	 * 
	 * @param className
	 *            The new class name.
	 * @throws Exception
	 * @throws IOException
	 */
	public String update(String className) throws IOException, Exception {
		this.className = className;
		return loadFromFile(className);
	}
}