package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static gitlet.Branch.retrieveActiveBranch;
import static gitlet.Main.*;
import static gitlet.Stage.STAGED_FOR_ADDITION;
import static gitlet.Stage.STAGED_FOR_REMOVAL;

/**
 * @author Iram Morshed
 */
public final class Commit implements Serializable {

    /** Constructor for initial commit. */
    public Commit() throws IOException {
        _timestamp = "Wed Dec 31 16:00:00 1969 -0800";
        _message = "initial commit";
        _uniqueID = Utils.sha1(Utils.serialize(this));
        _head = _uniqueID;
        _blobs = new HashMap<>();
        saveCommitAsFile();
        headToFile();
    }

    /** Commit constructor.
     * @param message <String></>
     * @param secondParent <String></>
     * @param merged <boolean></>*/
    public Commit(String message,
                  String secondParent, boolean merged)
                  throws IOException {
        String pattern = "EEE MMM d HH:mm:ss yyyy Z";
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
        _timestamp = dateFormat.format(new Date());
        _message = message;
        _parent = retrieveHead();
        _uniqueID = Utils.sha1(Utils.serialize(this));
        _head = _uniqueID;
        _blobs = new HashMap<>();
        _secondParent = secondParent;
        _isMerged = merged;

        addToParent();
        Branch active = retrieveActiveBranch();
        active.updateNode(_head);

        List<String> retrieveAdded =
                Utils.plainFilenamesIn(STAGED_FOR_ADDITION);
        for (String file: retrieveAdded) {
            Blob blob = new Blob(file);
            _blobs.put(file, blob.getBlobID());
        }
        List<String> retrieveRemoved =
                Utils.plainFilenamesIn(STAGED_FOR_REMOVAL);
        for (String file: retrieveRemoved) {
            File delete = Utils.join(STAGED_FOR_REMOVAL, file);
            if (!delete.exists()) {
                throw Utils.error("File doesn't exist even "
                        + "though STAGEREMOVE DIR says it does.");
            }
            delete.delete();
        }
        saveCommitAsFile();
        headToFile();
    }

    /** Transfers commit files from parent to child. */
    public void addToParent() {
        Commit parent = retrieveCommit(retrieveHead());
        _blobs.putAll(parent._blobs);
        List<String> stagedForRm = Utils.plainFilenamesIn(STAGED_FOR_REMOVAL);
        List<String> cwd = Utils.plainFilenamesIn(CWD);
        Iterator<String> iterator = _blobs.keySet().iterator();
        while (iterator.hasNext()) {
            String filename = iterator.next();
            if (stagedForRm.contains(filename) || !cwd.contains(filename)) {
                iterator.remove();
            }
        }
    }

    /** Saves the head to file. */
    private static void headToFile() {
        File head = Utils.join(GITLET_FOLDER, "HEAD");
        if (!head.exists()) {
            throw Utils.error("Head file was not initialized during INIT.");
        }
        Utils.writeObject(head, _head);
    }

    /** Retrieves the head from the HEAD file.
     * @return String*/
    public static String retrieveHead() {
        File containsHead = Utils.join(GITLET_FOLDER, "HEAD");
        if (!containsHead.exists()) {
            throw Utils.error("Head was not saved for persistence.");
        }
        String head = Utils.readObject(containsHead, String.class);
        return head;
    }

    /** Saves a commit to a file. */
    private void saveCommitAsFile() throws IOException {
        File commit = Utils.join(COMMIT_HISTORY, _uniqueID);
        commit.createNewFile();
        Utils.writeObject(commit, this);
    }

    /** Retrievs a commit specified by ID.
     * @param uniqueID <String></>
     * @return Commit*/
    public static Commit retrieveCommit(String uniqueID) {
        File commit = Utils.join(COMMIT_HISTORY, uniqueID);
        if (!commit.exists()) {
            throw Utils.error("File does not exist");
        } else {
            Commit retrieved = Utils.readObject(commit, Commit.class);
            return retrieved;
        }
    }

    /************** GITLET COMMANDS *********************/

    /** Runs the COMMIT command.
     * @param message <String></>
     * @param secondParent <String></>
     * @param merged <boolean></>*/
    public static void commit(String message,
                              String secondParent, boolean merged)
            throws IOException {
        if (message.isBlank()) {
            System.out.println("Please enter a commit message.");
            return;
        }

        List<String> stageForAdd = Utils.plainFilenamesIn(STAGED_FOR_ADDITION),
                stageForRm = Utils.plainFilenamesIn(STAGED_FOR_REMOVAL);
        if (stageForAdd.size() == 0 && stageForRm.size() == 0) {
            System.out.println("No changes added to the commit.");
            return;
        }

        Commit snapshot = new Commit(message, secondParent, merged);
        List<String> filesStageAdd =
                Utils.plainFilenamesIn(STAGED_FOR_ADDITION),
                filesStageRemove = Utils.plainFilenamesIn(STAGED_FOR_REMOVAL);
        for (String file: filesStageAdd) {
            File delete = Utils.join(STAGED_FOR_ADDITION, file);
            if (!delete.exists()) {
                System.out.println("File does not exist even "
                        + "though it says it should in Stage.");
                return;
            }
            delete.delete();
        }
        for (String file: filesStageRemove) {
            File delete = Utils.join(STAGED_FOR_REMOVAL, file);
            if (!delete.exists()) {
                System.out.println("File does not exist even "
                        + "though it says it should in Stage.");
                return;
            }
            delete.delete();
        }
    }

    /** Runs the ADD command.
     * @param file <File></>*/
    public static void add(File file) throws IOException {
        List<String> remove = Utils.plainFilenamesIn(STAGED_FOR_REMOVAL);
        if (remove.contains(file.getName())) {
            File inRemove = Utils.join(STAGED_FOR_REMOVAL, file.getName());
            if (Utils.readContentsAsString(inRemove)
                    .equals(Utils.readContentsAsString(file))) {
                inRemove.delete();
                return;
            }
        }

        Commit current = retrieveCommit(retrieveHead());
        if (current.getBlobs().containsKey(file.getName())) {
            String currentContents = Blob.readFromFile(
                    current.getBlobs().get(file.getName()));

            String fileContents = Utils.readContentsAsString(file);

            List<String> stage = Utils.plainFilenamesIn(STAGED_FOR_ADDITION);
            if (currentContents.equals(fileContents)
                    && stage.contains(file.getName())) {
                File delete = Utils.join(STAGED_FOR_ADDITION, file.getName());
                if (!delete.exists()) {
                    throw Utils.error("File does not exist "
                            + "even though file name exists in STAGE ADD.");
                }
                delete.delete();
                return;
            } else if (currentContents.equals(fileContents)) {
                return;
            }
        }
        File add = Utils.join(STAGED_FOR_ADDITION, file.getName());
        add.createNewFile();
        Utils.writeContents(add, Utils.readContents(file));
    }

    /** Runs the RM command.
     * @param file <File></>*/
    public static void remove(File file) throws IOException {
        List<String> stagedforAdd = Utils.plainFilenamesIn(STAGED_FOR_ADDITION);
        boolean wasStaged, wasTracked;
        if (stagedforAdd.contains(file.getName())) {
            File stageForAdd = Utils.join(STAGED_FOR_ADDITION, file.getName());
            if (!stageForAdd.exists()) {
                System.out.println("File does not exist in "
                        + "staged_for_addition even though"
                        + " the DIR contains its filename.");
            }
            stageForAdd.delete();
            wasStaged = true;
        } else {
            wasStaged = false;
        }

        Commit current = retrieveCommit(retrieveHead());
        String name = file.getName();
        if (current.getBlobs().containsKey(name)) {
            File stageForRm = Utils.join(STAGED_FOR_REMOVAL, name);
            stageForRm.createNewFile();
            Utils.writeContents(stageForRm, Utils.readContents(file));
            file.delete();
            wasTracked = true;
        } else {
            wasTracked = false;
        }

        if (!wasStaged && !wasTracked) {
            System.out.println("No reason to remove the file.");
        }
    }

    /** Runs the LOG command.
     * @param filename <String></> */
    public static void remove(String filename) throws IOException {
        Commit current = retrieveCommit(retrieveHead());
        Set<String> currentBlobs = current._blobs.keySet();
        if (currentBlobs.contains(filename)) {
            String contents =
                    Blob.readFromFile
                            (current.getBlobs().get(filename));
            File remove = Utils.join(STAGED_FOR_REMOVAL, filename);
            remove.createNewFile();
            Utils.writeContents(remove, contents);
        }

    }

    /** Runs the LOG command. */
    public static void log() {
        String head = retrieveHead();
        Commit history = retrieveCommit(head);
        while (history != null) {
            System.out.println("===");
            System.out.println("commit " + history.getUniqueID());
            if (history.getSecondParent() != null && history.getMerged()) {
                System.out.println("Merge: "
                        + history.getParent().substring(0, 7)
                        + " "
                        + history.getSecondParent().substring(0, 7));
            }
            System.out.println("Date: " + history.getTimestamp());
            System.out.println(history.getMessage());
            System.out.println();
            if (history.getParent() != null) {
                history = retrieveCommit(history.getParent());
            } else {
                break;
            }
        }
    }

    /** Runs GLOBAL_LOG. */
    public static void globallog() {
        List<String> allCommits = Utils.plainFilenamesIn(COMMIT_HISTORY);
        for (String uniqueID: allCommits) {
            Commit history = retrieveCommit(uniqueID);
            System.out.println("===");
            System.out.println("commit " + history.getUniqueID());
            System.out.println("Date: " + history.getTimestamp());
            System.out.println(history.getMessage());
            System.out.println();
        }

    }

    /** Find the commit.
     * @param args <String></>*/
    public static void find(String args) {
        List<String> allCommits = Utils.plainFilenamesIn(COMMIT_HISTORY);
        boolean found = false;
        for (String commit: allCommits) {
            Commit fromFile = retrieveCommit(commit);
            if (fromFile.getMessage().startsWith(args)) {
                System.out.println(fromFile.getUniqueID());
                found = true;
            }
        }
        if (!found) {
            System.out.println("Found no commit with that message.");
            return;
        }
    }

    /** Find the complete ID.
     * @param args <String></>
     * @return String*/
    public static String findCompleteID(String args) {
        List<String> allCommits = Utils.plainFilenamesIn(COMMIT_HISTORY);
        String completeID = null;
        for (String commit: allCommits) {
            if (commit.startsWith(args)) {
                completeID = commit;
            }
        }
        return completeID;

    }

    /** Retrieves the parent of a commit.
     * @return String*/
    public String getParent() {
        return _parent;
    }

    /** Retrieves the timestamp of a commit.
     * @return String*/
    public String getTimestamp() {
        return _timestamp;
    }

    /** Retrieves the message of a commit.
     * @return String*/
    public String getMessage() {
        return _message;
    }

    /** Retrieves the unique ID for a commit.
     * @return String*/
    public String getUniqueID() {
        return _uniqueID;
    }

    /** Retrieves the list of blobs.
     * @return HashMap<String, String>*/
    public HashMap<String, String> getBlobs() {
        return _blobs;
    }

    /** Retrieves the second parent.
     * @return String */
    public String getSecondParent() {
        return _secondParent;
    }

    /** Retrieves boolean ismerged.
     * @return boolean */
    public boolean getMerged() {
        return _isMerged;
    }

    /** Retrieves the head.
     * @return String*/
    public static String getHead() {
        return _head;
    }

    /** Sets the head.
     * @param commit <String></>*/
    public static void setHead(String commit) {
        _head = commit;
        headToFile();
    }

    /** Sets active branch to CURRENT. */
    public static void setActiveBranch(Branch current) throws IOException {
        Branch active = retrieveActiveBranch();
        active.switchHead(); current.switchHead();
        setHead(current.getCurrentNode());
        setActive(current);
        active.saveBranchToFile();
        current.saveBranchToFile();
    }

    /** Stores parent ID. */
    private String _parent = null;

    /** Stores second parent if merge made. */
    private String _secondParent = null;

    /** Stores if it is a merged commit or not. */
    private boolean _isMerged = false;

    /** Stores time commit made. */
    private String _timestamp;

    /** Stores message. */
    private String _message;

    /** Stores sha1 code for commit. */
    private String _uniqueID;

    /** Stores head. */
    private static String _head;

    /** Stores blobs. */
    private HashMap<String, String> _blobs;

}
