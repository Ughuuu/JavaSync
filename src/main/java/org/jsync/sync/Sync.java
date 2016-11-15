package org.jsync.sync;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

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
	private final static JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
	private final static URLClassLoader classLoader;

	static {
		URLClassLoader loader = null;
		try {
			loader = URLClassLoader.newInstance(new URL[] { new File("").toURI().toURL() });
		} catch (MalformedURLException e) {
			// do nothing in this
		}
		classLoader = loader;
	}

	/**
	 * Instance of the loaded class. May change depending on class reloading.
	 */
	private T instance = null;

	/**
	 * Last time the file was checked for changes. Used to keep the classes in
	 * sync with local changes.
	 */
	private FileTime lastLoadedTime;

	/**
	 * The full name of the loaded class, with the package
	 */
	private String className;

	/**
	 * The file path of the loaded class, used to resync the class if needed.
	 */
	private String filePath;

	/**
	 * The updater used to keep the files in sync also on server. Usually this
	 * represents a branch.
	 */
	private Updater updater;

	/**
	 * Constructor that calls the loadFromFile function.
	 * 
	 * @param filePath
	 *            The path of the file to keep in sync with
	 * @param className
	 *            The class name
	 * @param updater
	 *            The updater of choice
	 * @throws Exception
	 */
	public Sync(String className, Updater updater) throws Exception {
		this.className = className;
		this.updater = updater;
		filePath = updater.getLocalPath() + "/" + className.replace('.', '/') + ".class";
		loadFromFile(filePath, className);
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
	public Sync(String className) throws Exception {
		this(className, new Updater());
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
	public void loadFromFile(String path, String className) throws Exception {
		this.className = className;
		this.filePath = path;

		lastLoadedTime = Files.getLastModifiedTime(Paths.get(filePath));

		// This might throw error
		javaCompiler.run(null, null, null, path);
		
		// Load and instantiate compiled class.
		val loadedClass = (Class<T>)Class.forName(className, true, classLoader);
		
		// Instantiate the object
		instance = (T) loadedClass.newInstance();
	}

	/**
	 * Get the synced class. This operation will not resync, only update does
	 * so.
	 * 
	 * @return The instance or null if none is loaded.
	 */
	public T get() {
		return instance;
	}

	/**
	 * Resyncs the class instance if needed. If the class name is changed, call
	 * with String parameter.
	 * 
	 * @throws Exception
	 * @throws IOException
	 */
	public void update() throws IOException, Exception {
		if (lastLoadedTime != Files.getLastModifiedTime(Paths.get(filePath))) {
			loadFromFile(filePath, className);
			updater.update();
		}
	}

	/**
	 * Resyncs the class instance if needed. Also changes the class name.
	 * 
	 * @param className
	 *            The new class name.
	 * @throws Exception
	 * @throws IOException
	 */
	public void update(String className) throws IOException, Exception {
		this.className = className;
		if (lastLoadedTime != Files.getLastModifiedTime(Paths.get(filePath))) {
			loadFromFile(filePath, className);
		}
	}
}