package NewPack;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class Client {

	private final String SERVER_ADDRESS = "localhost";
	private final int PORT = 8080;
    
    public static void main(String[] args) throws UnknownHostException, IOException {
    	Thread directoryMonitoring = new Thread(new Monitor("C:\\Facultate\\Anul_IV\\Erasmus\\To"));
    	directoryMonitoring.start();
        
    	Client client = new Client();
    	client.runClient();
    	directoryMonitoring.interrupt();
    }
    
    public void runClient() {	
    	try 	(Socket socket = new Socket(SERVER_ADDRESS, PORT);     	
    			ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
    			ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
    		
    		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        	
        	System.out.println("connecting to server with = " + socket);

            while (true) {
            	
                System.out.println("\nChoose operation:\n"
                		+ "1. Upload file\n"
                		+ "2. Download file\n"
                		+ "3. List Directory Contents\n"
                		+ "4. Create a folder\n"
                		+ "5. Create File\n"
                		+ "6. Delete Folder or file\n"
                		+ "7. Upload a folder\n"
                		+ "8. Download a folder\n"
                		+ "9. Exit");
                
                int choice = Integer.parseInt(reader.readLine());

                switch(choice) {
                
                case 1: 
                	System.out.print("Enter the absolute path of the file to upload, from the client's machine: ");
                	String filePath = reader.readLine();
                	System.out.print("Enter the absolute path where you want to upload, from the server's machine: ");
                    String serverPath = reader.readLine();
                	uploadFile(out, filePath, serverPath);
                    break;
	                
                case 2: 
                	System.out.print("Enter the absolute path of the file to download, from the server's machine: ");
                    String fileToDownloadPath = reader.readLine();
                    System.out.print("Enter the absolute path where you want to download, from the client's machine: ");
                    String savePath = reader.readLine();
                    downloadFile(out, in, fileToDownloadPath, savePath);
	                break;
                
                case 3:
                	listDirectoryContents(reader, out, in);
                	break;
                	
                case 4:
                    createFolder(reader, out);
                    break;
                
                case 5:
                    createFile(reader, out);
                    break;
                	
                case 6: 
                    deleteFileOrFolder(reader, out);
                    break;
                
                case 7: 
                	System.out.print("Enter the absolute path of the folder to upload, from the client's machine: ");
                    String localFolderPath = reader.readLine();
                    System.out.print("Enter the absolute path where you want to upload, from the server's machine: ");
                    String serverDestinationPath = reader.readLine();
                    uploadFolder(out, localFolderPath, serverDestinationPath);
                    break;
                    
                case 8: 
                	System.out.print("Enter the absolute path of the folder to download, from the server's machine: ");
                    String serverFolderPath = reader.readLine();
                    System.out.print("Enter the absolute path where you want to download, from the client's machine: ");
                    String localDestinationPath = reader.readLine();
                    downloadFolder(in, out, serverFolderPath, localDestinationPath);
                    
                    System.out.println("It exited from the function");
                	break;
                	
                case 9: 
                    try {
                    	socket.close(); 
                    } catch(Exception e) {
                    	e.printStackTrace();
                    } finally {
                    	System.out.println("Exiting...");
                    }
                    return; 
                
                default: 
                	System.out.println("Invalid choice.");
                	break;
                }
            }

        } catch (Exception e) {
            System.out.println("You encountered next exception: " + e.getMessage());
        }
        
    }
    
    
    private void uploadFile(ObjectOutputStream out, String filePath, String serverPath) throws IOException {
    	Path path = Paths.get(filePath);
        File file = path.toFile();
        if (!file.exists() || file.isDirectory()) {
            System.out.println("The specified path does not exist or is a directory: " + filePath);
            return;
        }

        if (!serverPath.endsWith(File.separator)) {
            serverPath += File.separator;
        }
        serverPath += file.getName();

        OperationRequest request = new OperationRequest(OperationRequest.Type.UPLOAD, filePath, serverPath);
        out.writeObject(request);
        out.flush();

        // Send file size
        out.writeLong(Files.size(path));
        
        
        try (FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.READ)) {
            ByteBuffer buffer = ByteBuffer.allocateDirect(4096); 
            while (fileChannel.read(buffer) != -1) {
                buffer.flip(); // Prepare buffer for writing
                while (buffer.hasRemaining()) {
                    out.write(buffer.get()); // Write buffer contents to the ObjectOutputStream
                }
                buffer.compact(); // Prepare buffer for reading
            }
            out.flush(); // Ensure all data from the buffer is sent
        } catch (Exception e) {
            System.out.println("We caught this exception: " + e.getMessage());
        }
        System.out.println("File " + file.getName() + " uploaded successfully to " + serverPath);
    }
    
    
    private void downloadFile(ObjectOutputStream out, ObjectInputStream in, String serverFilePath, String localFilePath) throws IOException, ClassNotFoundException {

    	out.reset();
    	out.writeObject(new OperationRequest(OperationRequest.Type.DOWNLOAD, serverFilePath));
        out.flush();

        long fileSize = in.readLong();

        if (fileSize > 0) {
        	try (FileOutputStream fos = new FileOutputStream(localFilePath);
                 BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            	
        		byte[] buffer = new byte[4096];
                int bytesRead;
                long totalRead = 0;
                while (totalRead < fileSize) {
                    bytesRead = in.read(buffer, 0, buffer.length);
                    
                    if (bytesRead == -1) 
                    	break; 
                    bos.write(buffer, 0, bytesRead);
                    totalRead += bytesRead;
                }
                bos.flush(); 
                System.out.println("Download completed successfully for: " + localFilePath);
            } catch (IOException e) {
                System.out.println("Error downloading the file: " + e.getMessage());
            }
        } else {
            System.out.println("File not found on server or empty file: " + serverFilePath);
        }
    }

    
    private void listDirectoryContents(BufferedReader reader, ObjectOutputStream out, ObjectInputStream in) throws IOException, ClassNotFoundException {
        System.out.print("Enter the path of the directory to list: ");
        String directoryPath = reader.readLine();

        out.reset();
        out.writeObject(new OperationRequest(OperationRequest.Type.LIST_DIRECTORY, directoryPath));
        out.flush();

        @SuppressWarnings("unchecked")
        List<String> directoryContents = (List<String>) in.readObject();
        System.out.println("Contents of the directory: ");
        directoryContents.forEach(System.out::println);
    }

    private void createFolder(BufferedReader reader, ObjectOutputStream out) throws IOException {
        System.out.print("Enter the path of the folder to create: ");
        String folderPath = reader.readLine();

        OperationRequest request = new OperationRequest(OperationRequest.Type.CREATE_FOLDER, folderPath);
        out.reset();
        out.writeObject(request);
        out.flush();

        System.out.println("Folder creation request sent.");
    }

    private void createFile(BufferedReader reader, ObjectOutputStream out) throws IOException {
        System.out.print("Enter the absolute path of the file to create: ");
        String filePath = reader.readLine();

        OperationRequest request = new OperationRequest(OperationRequest.Type.CREATE_FILE, filePath);
        out.reset();
        out.writeObject(request);
        out.flush();

        System.out.println("File creation request sent.");
    }

    private void deleteFileOrFolder(BufferedReader reader, ObjectOutputStream out) throws IOException {
        System.out.print("Enter the absolute path of the file or folder to delete: ");
        String pathToDelete = reader.readLine();

        OperationRequest request = new OperationRequest(OperationRequest.Type.DELETE, pathToDelete);
        out.reset();
        out.writeObject(request);
        out.flush();

        System.out.println("Delete request sent for: " + pathToDelete);
    }

    
    private void uploadFolder(ObjectOutputStream out, String localFolderPath, String serverDestinationPath) throws IOException {
        File localFolder = new File(localFolderPath);
        if (!localFolder.isDirectory()) {
            System.out.println(localFolderPath + " is not a directory.");
            return;
        }
        uploadFolderContents(localFolder, localFolderPath, serverDestinationPath, out);
    }

    private void uploadFolderContents(File folder, String localFolderPath, String serverPath, ObjectOutputStream out) throws IOException {
        File[] files = folder.listFiles();
        
        if (files != null) {
            for (File file : files) {
                String serverFilePath = serverPath + "\\" + file.getName();
                if (file.isDirectory()) {
                    uploadFolderContents(file,localFolderPath, serverFilePath, out); 
                } else {
                    uploadSingleFile(file, serverFilePath, out); 
                }
            }
        }
    }

    private void uploadSingleFile(File file, String serverPath, ObjectOutputStream out) throws IOException {
        System.out.println("Uploading " + file.getAbsolutePath() + " to " + serverPath);

        OperationRequest request = new OperationRequest(OperationRequest.Type.UPLOAD, file.getAbsolutePath(), serverPath);
        out.reset();
        out.writeObject(request);
        out.flush();

        long fileSize = file.length();
        out.writeLong(fileSize);
        
        try (FileInputStream fis = new FileInputStream(file)){
 
        	byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) > 0) {
            	out.write(buffer, 0, bytesRead);
            }
            out.flush(); 
            
        } catch(Exception e) {
        	System.out.println("Exception caught: " + e.getMessage());
        }
            System.out.println("Uploaded " + file.getName() + " successfully.");
    }
    
    private void downloadFolder(ObjectInputStream in, ObjectOutputStream out, String serverFolderPath, String localDestinationPath) throws IOException, ClassNotFoundException {
        out.reset();
        out.writeObject(new OperationRequest(OperationRequest.Type.DOWNLOAD_FOLDER, serverFolderPath));
        out.flush();

        // Receive the list of files and directories from the server
        @SuppressWarnings("unchecked")
        List<String> contents = (List<String>) in.readObject();
        
        for (String entry : contents) {
            String localPath = Paths.get(localDestinationPath, entry).toString();
            String serverPath = Paths.get(serverFolderPath, entry).toString();
            
            if (entry.endsWith("/")) { 

            	new File(localPath).mkdirs();
                downloadFolder(in, out, serverPath, localPath);
            } else {

            	downloadFile(out, in, serverPath, localPath);
            }
        }
    }
    
}
