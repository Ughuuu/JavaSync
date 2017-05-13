package org.jsync.sync;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

import lombok.Getter;
import lombok.Setter;
import lombok.val;

/**
 * The Sync class contains a T class instance that changes if the linked file
 * changes. If that happens, the class is recompiled.
 */
public class ClassSync<T> {
	public static <T> boolean updateClass(ClassSync<T>[] syncs) {
		val first = syncs[0];
		URL url;
		boolean result = true;
		try {
			File root = new File(first.folderDestinationName);
			if (!root.exists() || !root.isDirectory()) {
				return false;
			}
			url = root.toURI().toURL();
		} catch (Exception e) {
			return false;
		}
		val urlClassLoader = URLClassLoader.newInstance(new URL[] { url }, first.classLoader);
		for (val sync : syncs) {
			try {
				sync.checkClass(urlClassLoader);
				sync.lastSynced = sync.classFile.lastModified();
			} catch (Throwable e) {
				result = false;
			}
		}
		return result;
	}

	/**
	 * The full name of the class, with the package. The package contains
	 * dots(.) and the class and package are united by a dot(.) as well.
	 */
	@Getter
	private final String className;

	/**
	 * An instance of file for the class given by the class name. It has .class
	 * at the end and is located at the source folder.
	 */
	private final File classFile;

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
	private long lastSynced;
	@Getter
	@Setter
	private URLClassLoader urlClassLoader;

	private final ClassLoader classLoader;

	/**
	 * Construct a new Sync object for the specified className.
	 * 
	 * @param classLoader
	 *            The class loader this will inherit from
	 * @param className
	 *            The full name of the class to be loaded
	 * @param folderDestinationName
	 *            The destination folder name
	 */
	public ClassSync(ClassLoader classLoader, String className, String folderDestinationName) {
		if (!(folderDestinationName.endsWith("/") || folderDestinationName.endsWith("\\"))) {
			folderDestinationName += '/';
		}
		if (folderDestinationName.equals("")) {
			folderDestinationName = "./";
		}
		this.className = className;
		this.classLoader = classLoader;
		this.folderDestinationName = folderDestinationName;
		this.classFile = new File(folderDestinationName + (className.replace('.', '/') + ".class").toString());
	}

	/**
	 * Get the class type.
	 * 
	 * @return The class type or null
	 * @throws ClassNotFoundException
	 */
	public Class<?> getClassType() throws ClassNotFoundException {
		return urlClassLoader.loadClass(className);
	}

	public boolean hasClassFile() {
		return classFile.exists();
	}

	/**
	 * Checks if the source file has been modified
	 * 
	 * @return The modified state
	 */
	public boolean isClassDirty() {
		return classFile.lastModified() != lastSynced || lastSynced == 0;
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

	private void checkClass(URLClassLoader newClassLoader) throws ClassNotFoundException, IOException {
		newClassLoader.loadClass(className);
		if (urlClassLoader != null)
			urlClassLoader.close();
		urlClassLoader = newClassLoader;
	}
}