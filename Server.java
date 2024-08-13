package NewPack;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

	public static void main(String[] args) throws IOException {
		Server startServer = new Server();
		startServer.runServer(8080);
	}
	
	public void runServer(int port) throws IOException {
		ServerSocket serverSocket = new ServerSocket(port);
		ExecutorService exec = Executors.newCachedThreadPool();
		
		try {		
			System.out.println("Started server with ServerSocket: " + serverSocket);
			System.out.println("Waiting for clients to connect");
			
			while(true) {
				final Socket socket = serverSocket.accept();		
				exec.execute(new ClientHandler(socket));
			
			}
		} catch (Exception e) {
			System.out.println("Exception caught " + e.getMessage());
		}
		
		finally {
			System.out.println("closing...");
			exec.shutdown();
			serverSocket.close();
		}
	}
}
