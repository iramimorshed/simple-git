package gitlet;
import java.io.File;
import java.io.Serializable;
import java.util.List;
import static gitlet.Main.GITLET_FOLDER;

/**
 * @author Iram Morshed
 */
public final class Stage implements Serializable {
    /** STAGING AREA for added files using ADD command. */
    static final File STAGED_FOR_ADDITION =
            Utils.join(GITLET_FOLDER, "staged_for_addition");

    /** STAGING AREA for removed files using RM command. */
    static final File STAGED_FOR_REMOVAL =
            Utils.join(GITLET_FOLDER, "staged_for_removal");

    /** Declares the directory for STAGE ADD and STAGE REMOVE. */
    public Stage() {
        STAGED_FOR_ADDITION.mkdir();
        STAGED_FOR_REMOVAL.mkdir();
    }

    /** Clears the staging area. */
    public static void clearStage() {
        List<String> filesStageAdd =
                Utils.plainFilenamesIn(STAGED_FOR_ADDITION);
        List<String> filesStageRemove =
                Utils.plainFilenamesIn(STAGED_FOR_REMOVAL);

        for (String file: filesStageAdd) {
            File delete = Utils.join(STAGED_FOR_ADDITION, file);
            if (!delete.exists()) {
                throw Utils.error("File does not "
                        + "exist in STAGE ADD even "
                        + "though in list of filenames.");
            }
            delete.delete();
        }

        for (String file: filesStageRemove) {
            File delete = Utils.join(STAGED_FOR_REMOVAL, file);
            if (!delete.exists()) {
                throw Utils.error("File does not exist in STAGE REMOVE "
                        + "even though in list of filenames.");
            }
            delete.delete();
        }
    }

    /** Prints the files inside the STAGING AREA;
     * used for STATUS. */
    public static void printStage() {
        List<String> filesStageAdd =
                Utils.plainFilenamesIn(STAGED_FOR_ADDITION);
        List<String> filesStageRemove =
                Utils.plainFilenamesIn(STAGED_FOR_REMOVAL);

        System.out.println("=== Staged Files ===");
        for (String file: filesStageAdd) {
            System.out.println(file);
        }

        System.out.println();

        System.out.println("=== Removed Files ===");
        for (String file: filesStageRemove) {
            System.out.println(file);
        }

        System.out.println();
    }

    /** Returns true if STAGING AREA is clear. */
    public static boolean isStageClear() {
        return Utils.plainFilenamesIn(STAGED_FOR_ADDITION).isEmpty()
                && Utils.plainFilenamesIn(STAGED_FOR_REMOVAL).isEmpty();
    }

    /** Returns true if FILE is in STAGEADD. */
    public static boolean stagedForAdd(String file) {
        return Utils.plainFilenamesIn(STAGED_FOR_ADDITION).contains(file);
    }

    /**
     * Returns true that says if FILE is in STAGEREMOVE.
     * @param file
     * @return boolean
     */
    public static boolean stagedForRemove(String file) {
        return Utils.plainFilenamesIn(STAGED_FOR_REMOVAL).contains(file);
    }

}
