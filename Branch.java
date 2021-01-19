package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static gitlet.Blob.readFromFile;
import static gitlet.Checkout.fourArgs;
import static gitlet.Checkout.twoArgs;
import static gitlet.Commit.commit;
import static gitlet.Commit.retrieveCommit;
import static gitlet.Commit.retrieveHead;
import static gitlet.Commit.setHead;
import static gitlet.Main.BRANCHES_FOLDER;
import static gitlet.Main.CURRENT_BRANCH;
import static gitlet.Main.CWD;
import static gitlet.Main.GITLET_FOLDER;
import static gitlet.Stage.isStageClear;
import static gitlet.Stage.STAGED_FOR_ADDITION;

/**
 * Branch class which runs several commands related to branches.
 * @author Iram Morshed
 */
public final class Branch implements Serializable {

    /**
     * Official constructor for
     * creating a Branch.
     * @param name String
     * @param isHead boolean
     * @param currentNode String
     * @throws IOException
     */
    public Branch(String name, boolean isHead,
                  String currentNode) throws IOException {
        _name = name;
        _isHead = isHead;
        _currentNode = currentNode;
        saveBranchToFile();
    }

    /**
     * Constructor for creating a Branch.
     * @param name String
     * @param isHead boolean
     * @throws IOException
     */
    public Branch(String name, boolean isHead) throws IOException {
        this(name, isHead, retrieveHead());
    }

    /** Saves a branch with filename, _NAME, under the BRANCHES_FOLDER dir.*/
    public void saveBranchToFile() throws IOException {
        File branch = Utils.join(BRANCHES_FOLDER, _name);
        if (!branch.exists()) {
            branch.createNewFile();
        }
        Utils.writeObject(branch, this);
        if (isHead()) {
            File current = new File(GITLET_FOLDER,
                    CURRENT_BRANCH.getName());
            if (!current.exists()) {
                throw Utils.error("CURRENT_BRANCH file should "
                        + "have been initialized with INIT command.");
            }
            Utils.writeObject(current, this);
        }
    }

    /** Updates the node that the active HEAD branch is pointing to.
     * @param uniqueID String*/
    public void updateNode(String uniqueID) throws IOException {
        if (isHead()) {
            _currentNode = uniqueID;
            setHead(uniqueID);
            saveBranchToFile();
        }
    }

    /** Retrieves a branch with NAME from the BRANCHES_FOLDER dir.
     * @param name String
     * @return Branch */
    public static Branch retrieveBranch(String name) {
        File branch = Utils.join(BRANCHES_FOLDER, name);
        if (!branch.exists()) {
            System.out.println("Can only retrieve a "
                    + "branch if specified by its name.");
            return null;
        }
        return Utils.readObject(branch, Branch.class);
    }

    /** Retrieves the active HEAD branch from the CURRENT_BRANCH file.
     * @return Branch*/
    public static Branch retrieveActiveBranch() {
        File current = Utils.join(GITLET_FOLDER, CURRENT_BRANCH.getName());
        if (!current.exists()) {
            throw Utils.error("CURRENT_BRANCH file should have"
                    + " been initialized with INIT command.");
        }
        return Utils.readObject(current, Branch.class);
    }

    /*** Switches the value of isHead.*/
    public void switchHead() {
        _isHead = !_isHead;
    }

    /**
     * Runs the BRANCH command.
     * @param name String
     * @throws IOException
     */
    public static void branch(String name) throws IOException {
        List<String> branchNames = Utils.plainFilenamesIn(BRANCHES_FOLDER);
        if (name.isBlank()) {
            System.out.println("Please enter a branch name.");
        } else if (branchNames.contains(name)) {
            System.out.println("A branch with that name already exists.");
        } else {
            Branch other = new Branch(name, false);
        }
    }

    /**
     * Runs the RM [BRANCH NAME] command.
     * @param name String
     */
    public static void rmBranch(String name) {
        File branch = Utils.join(BRANCHES_FOLDER, name);
        if (!branch.exists()) {
            System.out.println("A branch with that name does not exist.");
            return;
        }

        Branch active = retrieveActiveBranch();
        if (active.getBranchName().equals(name)) {
            System.out.println("Cannot remove the current branch.");
            return;
        }

        branch.delete();
    }

    /**
     * Runs the MERGE command.
     * @param branchName String
     * @throws IOException
     */
    public static void merge(String branchName) throws IOException {
        List<String> allBranches = Utils.plainFilenamesIn(BRANCHES_FOLDER);
        if (!allBranches.contains(branchName)) {
            System.out.println("No branch with that name "
                    +
                    "exists in BRANCHES_FOLDER dir.");
            return;
        }
        if (retrieveActiveBranch().getBranchName().equals(branchName)) {
            System.out.println("Cannot merge a branch with itself.");
        }
        Branch branch = retrieveBranch(branchName);
        Commit current = retrieveCommit(retrieveHead());
        Commit atBranch = retrieveCommit(branch.getCurrentNode());
        String split = findSplitPoint(current, atBranch);
        if (split.equals("") || split.isBlank()) {
            System.out.println("There are no common ancestors "
                    + "between the current branch and given branch.");
            return;
        }
        Commit splitPoint =
                retrieveCommit(findSplitPoint(current, atBranch));
        if (splitPoint.getUniqueID().equals(atBranch.getUniqueID())) {
            System.out.println("Given branch is "
                    + "an ancestor of the current branch.");
            return;
        } else if (splitPoint.getUniqueID().equals(current.getUniqueID())) {
            twoArgs(branchName);
            System.out.println("Current branch fast-forwarded.");
            return;
        }
        merge(branch, splitPoint, current, atBranch);

    }

    /**
     * Splits up files into three respective categories;
     * Helper function for MERGE command.
     * @param given Branch
     * @param split Commit
     * @param current Commit
     * @param atBranch Commit
     * @throws IOException
     */
    public static void merge(Branch given, Commit split,
                             Commit current, Commit atBranch)
                            throws IOException {
        HashMap<String, String> splitBlobs = split.getBlobs();
        HashMap<String, String> currBlobs = current.getBlobs();
        HashMap<String, String> branchBlobs = atBranch.getBlobs();

        List<String> checkout = new ArrayList<>();
        List<String> remove = new ArrayList<>();
        List<String> conflict = new ArrayList<>();

        String splitContents, currContents, branchContents;
        for (String file: branchBlobs.keySet()) {
            if (currBlobs.containsKey(file) && splitBlobs.containsKey(file)) {
                currContents = readFromFile(currBlobs.get(file));
                branchContents = readFromFile(branchBlobs.get(file));
                splitContents = readFromFile(splitBlobs.get(file));
                if (splitContents.equals(currContents)
                        && !branchContents.equals(currContents)) {
                    checkout.add(file);
                } else if (!branchContents.equals(currContents)
                        && !branchContents.equals(splitContents)) {
                    conflict.add(file);
                }
            } else if (splitBlobs.containsKey(file)
                    && !currBlobs.containsKey(file)) {
                branchContents = readFromFile(branchBlobs.get(file));
                splitContents = readFromFile(splitBlobs.get(file));
                if (!branchContents.equals(splitContents)) {
                    conflict.add(file);
                }
            } else if (!splitBlobs.containsKey(file)
                    && !currBlobs.containsKey(file)) {
                checkout.add(file);
            } else if (!splitBlobs.containsKey(file)
                    && currBlobs.containsKey(file)) {
                currContents = readFromFile(currBlobs.get(file));
                branchContents = readFromFile(branchBlobs.get(file));
                if (!currContents.equals(branchContents)) {
                    conflict.add(file);
                }
            }
        }

        for (String file: currBlobs.keySet()) {
            if (splitBlobs.containsKey(file)
                    && !branchBlobs.containsKey(file)) {
                currContents = readFromFile(currBlobs.get(file));
                splitContents = readFromFile(splitBlobs.get(file));
                if (currContents.equals(splitContents)) {
                    remove.add(file);
                } else {
                    conflict.add(file);
                }
            }
        }
        mergeCommands(given, atBranch, checkout, remove, conflict);
    }

    /**
     * Either checks out, removes, or rewrites content
     * if a file is in conflict.
     * @param given Branch
     * @param branch Commit
     * @param checkout List<String>
     * @param remove List<String>
     * @param conflict List<String>
     * @throws IOException
     */
    public static void mergeCommands(Branch given, Commit branch,
                                     List<String> checkout, List<String> remove,
                                     List<String> conflict) throws IOException {
        String[] commands = new String[4];
        if (!isStageClear()) {
            System.out.println("You have uncommitted changes.");
            return;
        }
        Commit current = retrieveCommit(retrieveHead());
        Commit atGiven = retrieveCommit(given.getCurrentNode());
        List<String> cwd = Utils.plainFilenamesIn(CWD);
        for (String file: cwd) {
            if (!current.getBlobs().containsKey(file)
                    && atGiven.getBlobs().containsKey(file)) {
                System.out.println("There is an untracked file in the way;"
                        + " delete it, or add and commit it first.");
                return;
            }
        }

        for (String file: checkout) {
            commands[0] = "checkout";
            commands[1] = branch.getUniqueID();
            commands[2] = "--";
            commands[3] = file;
            fourArgs(commands);
            File retrieve = Utils.join(STAGED_FOR_ADDITION, file);
            retrieve.createNewFile();
            Utils.writeContents(retrieve,
                    Utils.readContents(Utils.join(CWD, file)));
        }

        for (String file: remove) {
            File delete = Utils.join(CWD, file);
            if (!delete.exists()) {
                System.out.println("File does not exist in CWD.");
                return;
            }
            delete.delete();
        }
        mergeConflict(given, branch, conflict);
    }

    /**
     * Another helper method for MERGE for solving conflict.
     * @param given Branch
     * @param branch Commit
     * @param conflict List<String>
     * @throws IOException
     */
    public static void mergeConflict(Branch given,
                                     Commit branch, List<String> conflict)
                                    throws IOException {
        Commit current = retrieveCommit(retrieveHead());
        Commit atGiven = retrieveCommit(given.getCurrentNode());
        String currentContents, branchContents;
        for (String file: conflict) {
            File rewrite = Utils.join(CWD, file);
            String head = "<<<<<<< HEAD";
            String divide = "=======";
            String end = ">>>>>>>";
            if (current.getBlobs().containsKey(file)
                    && branch.getBlobs().containsKey(file)) {
                currentContents = readFromFile(current.getBlobs().get(file));
                branchContents = readFromFile(branch.getBlobs().get(file));
                if (!rewrite.exists()) {
                    System.out.println("This file should exist in CWD.");
                }
                String contents = head
                        + System.lineSeparator() + currentContents
                        + divide + System.lineSeparator()
                        + branchContents + end;
                Utils.writeContents(rewrite, contents);
                File stage = Utils.join(STAGED_FOR_ADDITION, file);
                stage.createNewFile();
                Utils.writeContents(stage, Utils.readContents(rewrite));
            } else if (current.getBlobs().containsKey(file)
                    && !branch.getBlobs().containsKey(file)) {
                if (!rewrite.exists()) {
                    rewrite.createNewFile();
                }
                currentContents = readFromFile(current.getBlobs().get(file));
                String contents = head + System.lineSeparator()
                        + currentContents + divide
                        + System.lineSeparator() + end;
                Utils.writeContents(rewrite, contents);
                File stage = Utils.join(STAGED_FOR_ADDITION, file);
                stage.createNewFile();
                Utils.writeContents(stage, Utils.readContents(rewrite));
            }

        }
        String message = "Merged " + given.getBranchName()
                + " into " + retrieveActiveBranch().getBranchName() + ".";
        if (conflict.isEmpty()) {
            commit(message, atGiven.getUniqueID(), true);
        } else {
            commit(message, atGiven.getUniqueID(), true);
            System.out.println("Encountered a merge conflict.");
            return;
        }
    }

    /**
     * Finds the split point between CURRENT and GIVEN.
     * @param current <Commit>
     * @param given <Commit>
     * @return String
     */
    public static String findSplitPoint(Commit current, Commit given) {
        if (current.getUniqueID().equals(given.getUniqueID())) {
            return "";
        }

        List<String> ancestorsAtCurr = allAncestors(current);
        List<String> ancestorsAtGiv = allAncestors(given);
        ancestorsAtCurr.retainAll(ancestorsAtGiv);
        String common = ancestorsAtCurr.get(0);
        if (common.equals("")) {
            System.out.println("There exists no common "
                    + "ancestors between Commit current "
                    + "[" + current + "] and Commit given [" + given + "]");
        }
        return common;
    }

    /**
     * Returns the ancestors of GIVEN.
     * @param given <Commit>
     * @return List<String>
     */
    public static List<String> allAncestors(Commit given) {
        List<String> ancestors = new ArrayList<>();
        while (given != null) {
            if (given.getParent() != null) {
                given = retrieveCommit(given.getParent());
                ancestors.add(given.getUniqueID());
            } else {
                break;
            }
        }
        return ancestors;
    }


    /** Returns the name of this branch.
     * @return String */
    public String getBranchName() {
        return _name;
    }

    /** Returns true if this branch is active.
     * @return boolean */
    public boolean isHead() {
        return _isHead;
    }

    /** Retrieves the current commit that a Branch points to.
     * @return String*/
    public String getCurrentNode() {
        return _currentNode;
    }

    /** Stores the name of a Branch. */
    private String _name;

    /** Is true if this branch is the active branch. */
    private boolean _isHead;

    /** Stores the current commit that a Branch points to. */
    private String _currentNode;

}
