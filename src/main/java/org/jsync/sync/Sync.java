package org.jsync.sync;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
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
	private String options = "";
	private long lastModified;
	private boolean changed = false;
	private static final Sync[] nullVararg = new Sync[0];

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
	public String loadFromFile(Sync... syncs) throws Exception {
		StringBuilder sourceNames = new StringBuilder(folderSourceName + "/");
		sourceNames.append(className.replace('.', '/') + ".java");
		String thisFile = sourceNames.toString();
		for(int i=0;i<syncs.length;i++){
			sourceNames.append(" " + syncs[i].className.replace('.', '/') + ".java");
		}
		String files = sourceNames.toString();
		long newLastModified = new File(thisFile).lastModified();
		// only compile this file also if we must
		if(newLastModified != lastModified || newLastModified == 0){
			lastModified = newLastModified;
		}else{
			return "";
		}
		val errorStringWriter = new StringWriter();
		val outputStringWriter = new StringWriter();
		val errorStream = new PrintWriter(errorStringWriter);
		val outputStream = new PrintWriter(outputStringWriter);		
		val success = BatchCompiler
				.compile(files + " -d " + folderDestinationName + " -cp " + folderDestinationName + " " + options, 
						outputStream,
						errorStream, null);
		if (success == false) {
			return errorStringWriter.toString();
		}
		instance = null;
		
		updateClass();
		
		return errorStringWriter.toString();
	}

	private void updateClass() throws Exception {
		URLClassLoader loader = URLClassLoader
				.newInstance(new URL[] { new File(folderDestinationName).toURI().toURL() }, classLoader);
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
		return loadFromFile(nullVararg);
	}

	public String update(Sync... others) throws Exception {
		StringBuilder err = new StringBuilder();
		err.append(loadFromFile(others));
		// we compiled all files, try to update them now
		for(int i=0;i<others.length;i++){
			others[i].updateClass();
		}
		return err.toString();
	}

	public Sync<T> setOptions(String options) {
		this.options = options;
		return this;
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
		return loadFromFile(nullVararg);
	}
}