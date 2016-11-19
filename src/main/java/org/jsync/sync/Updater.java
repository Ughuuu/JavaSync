package org.jsync.sync;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import lombok.Getter;
import lombok.val;

/**
 * This class is used to sync all your resources with the local git server. The
 * branch master is used to push commits to every change you make.
 * 
 * @author Dragos
 *
 */
public abstract class Updater {
	@Getter
	protected final String localPath;
	protected final Git git;
	protected final String branch;
	protected final Repository repository;

	protected void initialCommit() throws NoFilepatternException, GitAPIException {
		git.add().addFilepattern(".").call();
		git.commit().setMessage("0").call();
	}

	public int getRevision() throws IOException {
		val head = repository.resolve(Constants.HEAD);
		
		if(head == null)
			return 0;

		val stream = new ByteArrayOutputStream();
		repository.open(head).copyTo(stream);

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

	public void clean() throws RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException,
			CheckoutConflictException, GitAPIException, IOException {
		// delete the copy branch if it exists
		try {
			git.branchDelete().setBranchNames("copy").setForce(true).call();
		} catch (Exception e) {
			e.printStackTrace();
			// if branch doesn't exist, continue.
		}
		git.checkout().setName("copy").setOrphan(true).call();
		git.branchDelete().setBranchNames(branch).setForce(true).call();
		// this will be an initial commit
		initialCommit();
		git.branchRename().setOldName("copy").setNewName(branch).call();
	}

	public abstract List<DiffEntry> update() throws NoFilepatternException, GitAPIException, IOException;

	/**
	 * Create a new Updater with the given local path.
	 * 
	 * @param localFolder
	 *            The local path you want to sync with.
	 * @param branch
	 *            The main branch to keep the data on locally.
	 * @throws IOException
	 * @throws NoFilepatternException
	 * @throws GitAPIException
	 */
	public Updater(String localFolder, String branch) throws IOException, NoFilepatternException, GitAPIException {
		localPath = localFolder;
		this.branch = branch;
		val repositoryFolder = new File(localFolder, ".git");
		repository = FileRepositoryBuilder.create(repositoryFolder);
		try {
			repository.create();
		} catch (IllegalStateException e) {
			// we are ok with repositories that already exist, continue on those
		}
		git = new Git(repository);
		val head = repository.resolve(Constants.HEAD);
		// if we are have a null head, do nothing.
		if (head != null){
			try {
				git.checkout().setForce(true).setName(branch).call();
			} catch (Exception e) {
				git.checkout().setCreateBranch(true).setForce(true).setName(branch).call();
			}
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
		this("res", "master");
	}
}
