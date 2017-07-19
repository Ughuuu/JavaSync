package org.jsync.sync;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;

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

	/**
	 * Create a new Updater with the path set to the res folder, which is default
	 * 
	 * @throws IOException
	 * @throws NoFilepatternException
	 * @throws GitAPIException
	 */
	public Updater() throws IOException, NoFilepatternException, GitAPIException {
		this("res", "master");
	}

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
		if (head != null) {
			try {
				git.checkout().setForce(true).setName(branch).call();
			} catch (Exception e) {
				try {
					git.checkout().setCreateBranch(true).setForce(true).setName(branch).call();
				} catch (Exception ee) {
					// if we are puller
				}
			}
		}
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

	/**
	 * Get all files currently available
	 * 
	 * @return list of files paths
	 * @throws RevisionSyntaxException
	 * @throws AmbiguousObjectException
	 * @throws IncorrectObjectTypeException
	 * @throws IOException
	 */
	public List<String> getFiles()
			throws RevisionSyntaxException, AmbiguousObjectException, IncorrectObjectTypeException, IOException {
		List<String> list = new ArrayList<String>();
		ObjectId head = repository.resolve(Constants.HEAD);
		if (head == null) {
			return list;
		}
		RevWalk walk = null;
		TreeWalk treeWalk = null;
		try {
			walk = new RevWalk(repository);

			RevCommit commit = walk.parseCommit(head);
			RevTree tree = commit.getTree();

			treeWalk = new TreeWalk(repository);
			treeWalk.addTree(tree);
			treeWalk.setRecursive(true);
			while (treeWalk.next()) {
				list.add(treeWalk.getPathString());
			}
		} catch (Exception e) {
			throw e;
		} finally {
			walk.close();
			treeWalk.close();
		}
		return list;
	}

	public int getRevision() throws IOException {
		val head = repository.resolve(Constants.HEAD);

		if (head == null)
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

	public abstract List<DiffEntry> update() throws NoFilepatternException, GitAPIException, IOException;

	protected void initialCommit() throws NoFilepatternException, GitAPIException {
		git.add().addFilepattern(".").call();
		git.commit().setMessage("0").call();
	}
}
