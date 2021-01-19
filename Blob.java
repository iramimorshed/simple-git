package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static gitlet.Main.*;
import static gitlet.Stage.STAGED_FOR_ADDITION;

/**
 * Blob class for storing file contents.
 * @author Iram Morshed
 */
public final class Blob implements Serializable {

    /**
     * Creates a Blob.
     * @param filename String
     */
    public Blob(String filename) {
        File blobContent = Utils.join(STAGED_FOR_ADDITION, filename);
        if (!blobContent.exists()) {
            throw Utils.error("File does not exist when creating Blob object.");
        }
        _contents = Utils.readContents(blobContent);
        _blobID = Utils.sha1(_contents);
        _fileName = filename;
        _allBlobs.put(filename, _blobID);
        saveBlob();
    }

    /**
     * Returns the hashmap containing
     * all the blobs.
     * @return HashMap
     */
    public HashMap<String, String> getBlobs() {
        return _allBlobs;
    }

    /**
     * Returns all of the blob contents
     * as a list.
     * @param blobs HashMap
     * @return List<String>
     */
    public static List<String> allBlobContents(HashMap<String, String> blobs) {
        List<String> allBlobContents = new ArrayList<String>();
        for (String key: blobs.keySet()) {
            String uniqueID = blobs.get(key);
            allBlobContents.add(readFromFile(uniqueID));
        }
        return allBlobContents;
    }

    /**
     * Saves this blob to BLOB_OBJECTS.
     */
    private void saveBlob() {
        File blob = Utils.join(BLOB_OBJECTS, _blobID);
        try {
            blob.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Utils.writeContents(blob, _contents);
    }

    /**
     * Reads the contents from this blob.
     * @param uniqueID String
     * @return String
     */
    public static String readFromFile(String uniqueID) {
        File blob = Utils.join(BLOB_OBJECTS, uniqueID);
        if (!blob.exists()) {
            throw Utils.error("File does not exist in BLOBS.");
        }
        String contents = Utils.readContentsAsString(blob);
        return contents;
    }

    /**
     * Returns the blob as a file.
     * @param uniqueID String
     * @return File
     */
    public static File blobToFile(String uniqueID) {
        File blob = Utils.join(BLOB_OBJECTS, uniqueID);
        if (!blob.exists()) {
            throw Utils.error("File does not exist in BLOBS.");
        }
        return blob;
    }

    /**
     * Saves a blob to a file in CWD.
     * @param filename String
     * @param uniqueID String
     */
    private void saveBlob(String filename, String uniqueID) {
        File blob = Utils.join(BLOB_OBJECTS, uniqueID);
        try {
            blob.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        File blobInCWD = Utils.join(CWD, filename);
        Utils.writeContents(blobInCWD, Utils.readContentsAsString(blob));
    }

    /**
     * Retrieves the unique sha1 code
     * for this blob.
     * @return String
     */
    public String getBlobID() {
        return _blobID;
    }

    /**
     * Retrieves contents as a byte[].
     * @return byte[]
     */
    public byte[] getContents() {
        return _contents;
    }

    /**
     * Retrieves the filename
     * of this blob.
     * @return String
     */
    public String getFileName() {
        return _fileName;
    }

    /** Stores all created Blobs [KEY = filename, VALUE = blobID]. */
    private static HashMap<String, String>
            _allBlobs = new HashMap<String, String>();

    /** The SHA1 code of this Blob. */
    private String _blobID;

    /** The byte array storing the file contents of this Blob. */
    private byte[] _contents;

    /** The filename of this Blob. */
    private String _fileName;

}
