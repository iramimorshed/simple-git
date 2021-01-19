package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static gitlet.Blob.readFromFile;
import static gitlet.Checkout.checkout;
import static gitlet.Checkout.reset;
import static gitlet.Commit.*;
import static gitlet.Stage.*;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Iram Morshed
 */
public class Main {
    /** Current working directory. */
    static final File CWD = new File(".");
    /** Hidden gitlet folder. */
    static final File GITLET_FOLDER = Utils.join(CWD, ".gitlet");
    /** Stores all Blobs made. */
    static final File BLOB_OBJECTS = Utils.join(GITLET_FOLDER, "blobs");
    /** Stores the history of all commits made. */
    static final File COMMIT_HISTORY =
            Utils.join(GITLET_FOLDER, "commits_made");
    /** Stores the HEAD. */
    static final File HEAD = Utils.join(GITLET_FOLDER, "HEAD");
    /** Stores the list of all branches initialized. */
    static final File BRANCHES_FOLDER =
            Utils.join(GITLET_FOLDER, "branches");
    /** Stores the current branch. */
    static final File CURRENT_BRANCH =
            Utils.join(GITLET_FOLDER, "current_branch");

    /** MAIN function that runs the commands.
     * @param args <String[]></> */
    public static void main(String... args) throws IOException {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        }
        if (args[0].equals("init")) {
            init();
            return;
        }
        if (!initExists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }
        switch (args[0]) {
        case "add":
            numArgs(args, 2);
            if (args[1] == null) {
                System.out.println("Must have a file to add.");
                return;
            }
            File addFile = new File(args[1]);
            if (!addFile.exists()) {
                System.out.println("File does not exist.");
                return;
            }
            add(addFile);
            break;
        case "commit":
            numArgs(args, 2);
            if (args[1] == null) {
                System.out.println("Must have a message to commit.");
                return;
            }
            commit(args[1], null, false);
            break;
        case "log":
            log();
            break;
        case "checkout":
            checkout(args);
            break;
        case "rm":
            numArgs(args, 2);
            if (args[1] == null) {
                System.out.println("Must have a file to remove.");
                return;
            }
            File rmFile = new File(args[1]);
            if (!rmFile.exists()) {
                remove(args[1]);
                break;
            }
            remove(rmFile);
            break;
        default:
            mainTwo(args);
        }
    }

    /** Second main method since first was too long.
     * @param args */
    public static void mainTwo(String[] args) throws IOException {
        switch (args[0]) {
        case "global-log":
            numArgs(args, 1);
            globallog();
            break;
        case "branch":
            numArgs(args, 2);
            Branch.branch(args[1]);
            break;
        case "status":
            numArgs(args, 1);
            status();
            break;
        case "find":
            numArgs(args, 2);
            find(args[1]);
            break;
        case "rm-branch":
            numArgs(args, 2);
            Branch.rmBranch(args[1]);
            break;
        case "reset":
            numArgs(args, 2);
            reset(args[1]);
            break;
        case "merge":
            numArgs(args, 2);
            Branch.merge(args[1]);
            break;
        default:
            System.out.println("No command with that name exists.");
            return;
        }

    }

    /** Returns true if GITLET_FOLDER exists. */
    public static boolean initExists() {
        return GITLET_FOLDER.exists();
    }

    /** Runs the INIT command. */
    public static void init() throws IOException {
        if (GITLET_FOLDER.exists()) {
            System.out.println("A Gitlet version-control system "
                    + "already exists in the current directory.");
            return;
        }
        CWD.mkdir();
        GITLET_FOLDER.mkdir();
        COMMIT_HISTORY.mkdir();
        BLOB_OBJECTS.mkdir();
        BRANCHES_FOLDER.mkdir();
        CURRENT_BRANCH.createNewFile();
        HEAD.createNewFile();
        new Stage(); new Commit();
        _active = new Branch("master", true, retrieveHead());
    }

    /** Runs the STATUS command. */
    public static void status() {
        List<String> branches = Utils.plainFilenamesIn(BRANCHES_FOLDER);
        System.out.println("=== Branches ===");
        Branch active = Branch.retrieveActiveBranch();
        for (String branch: branches) {
            Branch fromFile =
                    Branch.retrieveBranch(branch);
            if (fromFile.getBranchName().
                    equals(active.getBranchName())) {
                System.out.println("*"
                        + fromFile.getBranchName());
            } else {
                System.out.println(fromFile.getBranchName());
            }
        }
        System.out.println();
        printStage();
        System.out.println("=== Modifications Not Staged For Commit ===");
        modNotStaged();
        System.out.println();
        System.out.println("=== Untracked Files ===");
        untracked();
        System.out.println();
    }

    /** Helper function for STATUS. */
    public static void modNotStaged() {
        Commit current = retrieveCommit(retrieveHead());
        HashMap<String, String> currentBlobs = current.getBlobs();
        List<String> modified = new ArrayList<>();
        String currContents;
        for (String file: Utils.plainFilenamesIn(CWD)) {
            if (currentBlobs.containsKey(file)) {
                File cwd = Utils.join(CWD, file);
                currContents = readFromFile(currentBlobs.get(file));
                if (!Utils.readContentsAsString(cwd).equals(currContents)
                    && !Stage.stagedForAdd(file) && !stagedForRemove(file)) {
                    modified.add(file + " (modified)");
                } else if (stagedForAdd(file)) {
                    File add = Utils.join(STAGED_FOR_ADDITION, file);
                    if (!Utils.readContentsAsString(cwd)
                            .equals(Utils.readContentsAsString(add))) {
                        modified.add(file + " (modified)");
                    }
                }
            }
        }
        for (String file: Utils.plainFilenamesIn(STAGED_FOR_ADDITION)) {
            if (!Utils.plainFilenamesIn(CWD).contains(file)) {
                modified.add(file + " (deleted)");
            }
        }

        for (String file: currentBlobs.keySet()) {
            if (!stagedForRemove(file)
                    && !Utils.plainFilenamesIn(CWD).contains(file)) {
                modified.add(file + " (deleted)");
            }
        }
        Collections.sort(modified);
        for (String file: modified) {
            System.out.println(file);
        }
    }

    /** Helper function for printing UNTRACKED files. */
    public static void untracked() {
        Commit current = retrieveCommit(retrieveHead());
        HashMap<String, String> currentBlobs = current.getBlobs();
        List<String> untracked = new ArrayList<>();
        for (String file: Utils.plainFilenamesIn(CWD)) {
            if (!stagedForAdd(file) && !currentBlobs.containsKey(file)) {
                untracked.add(file);
            }
        }
        Collections.sort(untracked);
        for (String file: untracked) {
            System.out.println(file);
        }
    }

    /** Validates number of arguments.
     * @param num <int></>
     * @param args <String[]></>
     * */
    public static void numArgs(String[] args, int num) {
        if (args.length != num) {
            throw Utils.error("Must have " + num + " argument(s).");
        }
    }

    /** Retrieves active branch.
     * @return Branch
     * */
    public static Branch getActive() {
        return _active;
    }

    /** Sets active branch.
     * @param other <Branch>
     * */
    public static void setActive(Branch other) {
        _active = other;
    }

    /** Contains active branch. */
    private static Branch _active;


}
