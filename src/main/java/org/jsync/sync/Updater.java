package org.jsync.sync;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import lombok.Getter;
import lombok.val;

/**
 * This class is used to sync all your resources with the local git server.
 * The branch master is used to push commits to every change you make.
 * @author Dragos
 *
 */
public class Updater {
	@Getter
	private final String localPath;
	private final Git git;
	private final Repository repository;

	void initialCommit() throws NoFilepatternException, GitAPIException {
		git.add().addFilepattern(".").call();
		git.commit().setMessage("0").call();
	}

	int getRevision() throws IOException {
		val id = repository.findRef("HEAD").getObjectId();

		val stream = new ByteArrayOutputStream();
		repository.open(id).copyTo(stream);

		val lastCommit = stream.toString("UTF-8");
		val lastIndex = lastCommit.substring(lastCommit.lastIndexOf('\n') + 1);

		int lastIndexInt = 0;

		try {
			lastIndexInt = Integer.valueOf(lastIndex);
		} catch (Exception e) {
			// if this happens, reset index to 0
		}
		return lastIndexInt;
	}

	void clean() throws RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException,
			CheckoutConflictException, GitAPIException, IOException {
		// delete the copy branch if it exists
		try {
			git.branchDelete().setBranchNames("copy").setForce(true).call();
		} catch (Exception e) {
			e.printStackTrace();
			// if branch doesn't exist, continue.
		}
		git.checkout().setName("copy").setOrphan(true).call();
		git.branchDelete().setBranchNames("master").setForce(true).call();
		// this will be an initial commit
		initialCommit();
		git.branchRename().setOldName("copy").setNewName("master").call();
	}

	void update() throws NoFilepatternException, GitAPIException, IOException {
		val status = git.status().call();
		if (!status.isClean()) {
			git.add().addFilepattern(".").call();
			git.commit().setMessage(Integer.toString(getRevision() + 1)).call();
		}
	}

	/**
	 * Create a new Updater with the given local path.
	 * 
	 * @param localFolder
	 *            The local path you want to sync with
	 * @throws IOException
	 * @throws NoFilepatternException
	 * @throws GitAPIException
	 */
	public Updater(String localFolder) throws IOException, NoFilepatternException, GitAPIException {
		localPath = localFolder;
		val repositoryFolder = new File(localFolder, ".git");
		repository = FileRepositoryBuilder.create(repositoryFolder);
		try {
			repository.create();
		} catch (IllegalStateException e) {
			// we are ok with repositories that already exist, continue on those
		}
		git = new Git(repository);
		// make the file hidden
		Files.setAttribute(Paths.get(repositoryFolder.getParent()), "dos:hidden", true);

		val id = repository.findRef("HEAD").getObjectId();
		// we are at initial commit, add a README.MD file
		if (id == null) {
			new File(repository.getDirectory().getParent(), "README.MD");
			initialCommit();
		}
		git.checkout().setForce(true).setName("master").call();
		val status = git.status().call();
		if (!status.isClean()) {
			// files were added from outside. Add them also
			update();
		}
	}

	/**
	 * Create a new Updater with the path set to the res folder, which is
	 * default
	 * 
	 * @throws IOException
	 * @throws NoFilepatternException
	 * @throws GitAPIException
	 */
	public Updater() throws IOException, NoFilepatternException, GitAPIException {
		this("res");
	}
}
