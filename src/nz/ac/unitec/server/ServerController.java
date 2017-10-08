package nz.ac.unitec.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

public class ServerController implements Runnable {
	
	ServerSocket serverSocket;
	ArrayList <ServerThread> connectedClients = new ArrayList<ServerThread>();
	
	@FXML
	private TextArea txtMessage;
	
	public ServerController() {
		try {
			serverSocket = new ServerSocket(5000);
			Thread serverThread = new Thread(this);
			serverThread.start();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void run()
	{
		try
		{
			while(true)
			{
				Socket remoteClient = serverSocket.accept(); 
								
				ServerThread st = new ServerThread(remoteClient, this, connectedClients);
				st.start();
				
				connectedClients.add(st);

			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public TextArea getTextArea() {
		return txtMessage;
	}
}
