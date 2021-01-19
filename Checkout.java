package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import static gitlet.Blob.readFromFile;
import static gitlet.Branch.retrieveActiveBranch;
import static gitlet.Branch.retrieveBranch;
import static gitlet.Commit.findCompleteID;
import static gitlet.Commit.retrieveCommit;
import static gitlet.Commit.retrieveHead;
import static gitlet.Commit.setActiveBranch;
import static gitlet.Main.BRANCHES_FOLDER;
import static gitlet.Main.COMMIT_HISTORY;
import static gitlet.Main.CWD;

/**
 * A class devoted to the three checkout commands.
 * @author Iram Morshed
 */
public final class Checkout implements Serializable {

    /**
     * Runs the CHECKOUT command first, before calling other functions.
     * @param args <String>
     * @throws IOException
     */
    public static void checkout(String[] args) throws IOException {
        if (args.length == 2) {
            twoArgs(args[1]);
        } else if (args.length == 3) {
            threeArgs(args);
        } else if (args.length == 4) {
            fourArgs(args);
        } else {
            System.out.println("Checkout function only "
                    + "accepts 2, 3, or 4 total arguments.");
            return;
        }
    }


    /**
     * Runs the CHECKOUT command when it follows: checkout [branch name].
     * @param args <String></>
     * @throws IOException
     */
    public static void twoArgs(String args) throws IOException {
        List<String> allBranches = Utils.plainFilenamesIn(BRANCHES_FOLDER);
        if (!allBranches.contains(args)) {
            System.out.println("No such branch exists.");
            return;
        }

        Branch current = retrieveActiveBranch();
        Commit atCurrent = retrieveCommit(current.getCurrentNode());

        if (current.getBranchName().equals(args)) {
            System.out.println("No need to checkout the current branch.");
            return;
        }

        Branch given = retrieveBranch(args);
        Commit atGiven = retrieveCommit(given.getCurrentNode());
        List<String> cwd = Utils.plainFilenamesIn(CWD);
        File overwrite; String contents;

        for (String file: cwd) {
            if (atGiven.getBlobs().containsKey(file)
                    && !atCurrent.getBlobs().containsKey(file)) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                return;
            }
        }

        for (String file: atGiven.getBlobs().keySet()) {
            contents = readFromFile(atGiven.getBlobs().get(file));
            overwrite = Utils.join(CWD, file);
            overwrite.createNewFile();
            Utils.writeContents(overwrite, contents);
        }

        for (String file: atCurrent.getBlobs().keySet()) {
            if (!atGiven.getBlobs().containsKey(file)) {
                File delete = Utils.join(CWD, file);
                if (!delete.exists()) {
                    System.out.println("File in KEYSET but "
                            + "doesn't exist which doesn't make sense.");
                    return;
                }
                delete.delete();
            }
        }
        Stage.clearStage();
        setActiveBranch(given);
    }

    /**
     * Runs CHECKOUT when it follows format: checkout -- [file.name].
     * @param args <String[]></>
     * @throws IOException
     */
    public static void threeArgs(String[] args) throws IOException {
        if (!args[1].equals("--")) {
            throw Utils.error("For this scenario, second arg must be '--'");
        }

        File checkout = new File(args[2]);
        if (!checkout.exists()) {
            throw Utils.error("File must exist"
                    + " in order for it to be checked out.");
        }

        String head = retrieveHead();
        Commit latest = retrieveCommit(head);
        if (!latest.getBlobs().containsKey(args[2])) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        String contents = readFromFile(latest.getBlobs().get(args[2]));
        File overwrite = Utils.join(CWD, args[2]); overwrite.createNewFile();
        Utils.writeContents(overwrite, contents);
    }

    /**
     * Runs CHECKOUT command when it follows:
     * checkout [commit id] -- [file.name].
     * @param args <String></>
     * @throws IOException
     */
    public static void fourArgs(String[] args) throws IOException {
        List<String> allCommits = Utils.plainFilenamesIn(COMMIT_HISTORY);
        Commit found = null;
        for (String commitID: allCommits) {
            if (commitID.equals(args[1]) || commitID.startsWith(args[1])) {
                found = retrieveCommit(commitID);
            }
        }
        if (found == null) {
            System.out.println("No commit with that id exists.");
            return;
        }
        if (!args[2].equals("--")) {
            System.out.println("Incorrect operands.");
            return;
        }
        if (!found.getBlobs().containsKey(args[3])) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        String contents = readFromFile(found.getBlobs().get(args[3]));
        File overwrite = Utils.join(CWD, args[3]);
        overwrite.createNewFile();
        Utils.writeContents(overwrite, contents);
    }

    /**
     * Runs the RESET command.
     * @param id <String></>
     * @throws IOException
     */
    public static void reset(String id) throws IOException {
        String completeID = findCompleteID(id);
        if (completeID == null) {
            System.out.println("No commit with that id exists.");
            return;
        }

        Commit toReset = retrieveCommit(completeID);
        Commit current = retrieveCommit(retrieveHead());
        List<String> cwd = Utils.plainFilenamesIn(CWD);
        for (String file: cwd) {
            if (toReset.getBlobs().containsKey(file)
                    && !current.getBlobs().containsKey(file)) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                return;
            }
        }

        for (String file: toReset.getBlobs().keySet()) {
            File overwrite = Utils.join(CWD, file);
            overwrite.createNewFile();
            String contents = readFromFile(toReset.getBlobs().get(file));
            Utils.writeContents(overwrite, contents);
        }

        for (String file: current.getBlobs().keySet()) {
            if (!toReset.getBlobs().containsKey(file)) {
                File delete = Utils.join(CWD, file);
                if (!delete.exists()) {
                    System.out.println("File should exist "
                            + "since it was contained in "
                            + "current commit keyset.");
                    return;
                }
                delete.delete();
            }
        }


        Branch active = retrieveActiveBranch();
        active.updateNode(completeID);
        Stage.clearStage();


    }


}
