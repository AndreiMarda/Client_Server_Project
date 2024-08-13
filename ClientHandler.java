package NewPack;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.ArrayList;

public class ClientHandler implements Runnable {
    private Socket socket;

    public ClientHandler(Socket clientSocket) {
        this.socket = clientSocket;
    }
    
    public void run() {
    	try (ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());) {
    						
    		while(true) {
    			
	    		OperationRequest request = (OperationRequest) objectInputStream.readObject();
	    		
	    		switch (request.getOperationType()) {
	    		
	    		case UPLOAD:
	    			handleFileUpload(request, objectInputStream);
	                break;
	                
	    		case UPLOAD_FOLDER:
	    		    handleFolderUpload(request, objectInputStream, objectOutputStream);
	    		    objectOutputStream.reset();
	    		    break;
	                
	    		case DOWNLOAD:
	    		    handleFileDownload(request, objectOutputStream);
	    		    objectOutputStream.reset();
	    		    break;
	    		
	    		case DOWNLOAD_FOLDER:
	                handleDownloadFolder(request, objectOutputStream); 
	                break;
	            
	    		case LIST_DIRECTORY:
	    		    String directoryPath = request.getPath();
	    		    listDirectoryContentsRecursively(objectOutputStream, directoryPath);
	    		    objectOutputStream.reset();
	    		    break;
	
	            case CREATE_FOLDER:
	            	handleCreateFolder(request);
	            	break;
	            
	            case CREATE_FILE:
	                handleCreateFileRequest(request);
	                break;
	            
	            case DELETE:
	                handleDeleteRequest(request);
	                break;
	            
	            case EXIT:
	                System.out.println("Client requested to exit.");
	                try{
	                	socket.close(); 
	                } catch(IOException e) {
	        			System.out.println("Exception while trying to close the socket: " + e.getMessage());
	                } break;
	                
	            default:	
	            	System.out.println("Invalid request from client.");
	            	break;
	    		} 
	    		
    		}

           } catch (IOException e) {
        	   System.out.println("IOException: " + e.getMessage());           
           } catch (ClassNotFoundException e) {
        	   System.out.println("ClassNotFoundException: " + e.getMessage());
		}
    	
       }
   
    
    private void handleFileUpload(OperationRequest request, ObjectInputStream in) throws IOException {
        File fileSave = new File(request.getServerPath());
        
        if (!fileSave.getParentFile().exists()) {
            boolean dirsCreated = fileSave.getParentFile().mkdirs();
            if (!dirsCreated) {
                System.err.println("Failed to create parent directories for " + fileSave.getAbsolutePath());
                return;
            }
        }

        try (FileChannel fileChannel = new FileOutputStream(fileSave).getChannel()) {
            long fileSize = in.readLong();
            System.out.println("File size to receive: " + fileSize);

            ByteBuffer buffer = ByteBuffer.allocateDirect(1024 * 4); 
            long bytesReadTotal = 0;

            while (bytesReadTotal < fileSize) {
                buffer.clear();
                while (buffer.hasRemaining() && bytesReadTotal < fileSize) {
                    int data = in.read();
                    if (data == -1) {
                        throw new IOException("End of stream reached unexpectedly");
                    }
                    buffer.put((byte) data);
                    bytesReadTotal++;
                }
                buffer.flip();
                while (buffer.hasRemaining()) {
                    fileChannel.write(buffer);
                }
            }
        } catch (Exception e) {
            System.out.println("Exception caught while uploading a file: " + e.getMessage());
        }
        System.out.println("File " + request.getPath() + " uploaded successfully.");
    }

    
	private void handleFileDownload(OperationRequest request, ObjectOutputStream out) throws IOException {
	    String filePath = request.getPath(); 
	    File file = new File(filePath);

	    if (file.exists() && !file.isDirectory()) {

	    	out.writeLong(file.length());

	    	try (FileInputStream fis = new FileInputStream(file);
	             BufferedInputStream bis = new BufferedInputStream(fis)) {
	            
	        	byte[] buffer = new byte[4096];
	            int bytesToSend;
	            while ((bytesToSend = bis.read(buffer)) != -1) {
	                out.write(buffer, 0, bytesToSend);
	                out.flush();
	            }
	        }
	    } else {
	        out.writeLong(0); 
	    }
	}
	
	
    private void handleFolderUpload(OperationRequest request, ObjectInputStream in, ObjectOutputStream out) throws IOException {

    	String serverFolderPath = request.getServerPath();
        File serverFolder = new File(serverFolderPath);

        if (!serverFolder.exists()) {
            boolean created = serverFolder.mkdirs();
            if (created) {
                System.out.println("Folder structure initialized: " + serverFolderPath);
            } else {
                System.out.println("Failed to initialize folder structure: " + serverFolderPath);
            }
        } else {
            System.out.println("Folder upload initiated: " + serverFolderPath);
        }
        
    }
    
    
    private void handleDownloadFolder(OperationRequest request, ObjectOutputStream out) throws IOException {
    	
    	String directoryPath = request.getPath();
        File directory = new File(directoryPath);
        File[] files = directory.listFiles();
        List<String> relativePaths = new ArrayList<>();
        for (File file : files) {
            String relativePath = getRelativePath(directoryPath, file);
            if (file.isDirectory()) {
                relativePath += "/";
            }
            relativePaths.add(relativePath);
        }
        out.writeObject(relativePaths);
        out.flush();
    }
    
    
    private String getRelativePath(String directoryPath, File file) {
        Path basePath = Paths.get(directoryPath).toAbsolutePath();
        Path filePath = file.toPath().toAbsolutePath();
        return basePath.relativize(filePath).toString();
    }
    
    
	private void listDirectoryContentsRecursively(ObjectOutputStream out, String directoryPath) throws IOException {
	    File rootDir = new File(directoryPath);
	    List<String> contents = new ArrayList<>();
	    if (rootDir.exists() && rootDir.isDirectory()) {
	        listDirectory(new File(directoryPath), contents, "");
	    }
	    out.writeObject(contents);
	    out.flush();
	}

	
	private void listDirectory(File dir, List<String> contents, String indent) {
	    File[] files = dir.listFiles();
	    if (files != null) {
	        for (File file : files) {
	            contents.add(indent + (file.isDirectory() ? "[Dir] " : "[File] ") + file.getName());
	            if (file.isDirectory()) {
	                listDirectory(file, contents, indent + "  "); // Recursively list subdirectories
	            }
	        }
	    }
	}

	
	private void handleCreateFolder(OperationRequest request) throws IOException {
	    String folderPath = request.getPath(); 
	    File folder = new File(folderPath);

	    if (!folder.exists()) {
	        boolean success = folder.mkdirs(); 
	        if (success) {
	            System.out.println("Folder created successfully: " + folderPath);
	        } else {
	            System.out.println("Failed to create folder: " + folderPath);
	        }
	    } else {
	        System.out.println("Folder already exists: " + folderPath);
	    }
	}


	private void handleCreateFileRequest(OperationRequest request) throws IOException {
	    String filePath = request.getPath(); 
	    File file = new File(filePath);

	    boolean success = file.createNewFile();

	    if (success) {
	        System.out.println("File created successfully: " + filePath);
	    } else {
	        System.out.println("Failed to create file (file may already exist): " + filePath);
	    }
	}

	
	private void handleDeleteRequest(OperationRequest request) throws IOException {
	    String targetPath = request.getPath(); 
	    File target = new File(targetPath);

	    boolean success;
	    if (target.isDirectory()) {
	        success = deleteDirectory(target); 
	    } else {
	        success = target.delete(); 
	    }

	    if (success) {
	        System.out.println("Successfully deleted: " + targetPath);
	    } else {
	        System.out.println("Failed to delete: " + targetPath);
	    }
	}

	
	private boolean deleteDirectory(File dir) {
	    File[] files = dir.listFiles();
	    if (files != null) {
	        for (File file : files) {
	            deleteDirectory(file);
	        }
	    }
	    return dir.delete(); 
	}

}
