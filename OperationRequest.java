package NewPack;

import java.io.Serializable;

public class OperationRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    public enum Type {
        UPLOAD, DOWNLOAD, LIST_DIRECTORY, CREATE_FOLDER, CREATE_FILE, DELETE, DOWNLOAD_FOLDER, UPLOAD_FOLDER, EXIT
    }

    private Type operationType;
    private String path; // Client-side file or folder path for the operation
    private String serverPath; // Server-side path for upload operations

    // Constructor for operations that don't involve file transfer directly or folder-specific operations
    public OperationRequest(Type operationType, String path) {
        this.operationType = operationType;
        this.path = path;
        this.serverPath = null;
    }

    public OperationRequest(Type operationType, String path, String serverPath) {
        this.operationType = operationType;
        this.path = path;
        this.serverPath = serverPath;
    }

    // Getters
    public Type getOperationType() {
        return operationType;
    }

    public String getPath() {
        return path;
    }

    public String getServerPath() {
        return serverPath;
    }
    
}
