package nz.ac.unitec.server;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

public class ServerThread extends Thread
{
	DataInputStream dis;
	DataOutputStream dos;
	Socket remoteClient;	
	ServerController serverController;
	ArrayList<ServerThread> connectedClients;
	String userName;
	
	public ServerThread(Socket remoteClient, ServerController serverController, ArrayList<ServerThread> connectedClients)
	{
		this.userName = "";
		this.remoteClient = remoteClient;
		this.connectedClients = connectedClients;
		try {
			this.dis = new DataInputStream(remoteClient.getInputStream());
			this.dos = new DataOutputStream(remoteClient.getOutputStream());
			this.serverController = serverController;
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public void run()
	{
		while(true)
		{
			try {
				int mesgType = dis.readInt();
				
				switch(mesgType)
				{
					case Constants.CHAT_MESSAGE:
						String data = dis.readUTF();
						serverController.getTextArea().appendText(remoteClient.getInetAddress()+":"+remoteClient.getPort()+"("+userName+")"+">"+data+"\n");
						
						for(ServerThread otherClient: connectedClients)
						{
							if(otherClient.equals(this))
							{
								otherClient.getDos().writeInt(Constants.CHAT_BROADCAST);
								otherClient.getDos().writeUTF("Me : " + data);
							}
							else
							{
								otherClient.getDos().writeInt(Constants.CHAT_BROADCAST);
								otherClient.getDos().writeUTF(userName + " : " + data);
							}
						}
						
						break;
					case Constants.REGISTER_CLIENT:
						String name = dis.readUTF();
						serverController.getTextArea().appendText(remoteClient.getInetAddress()+":"+remoteClient.getPort()+"("+name+")" + " has joined the chat" + "\n");
						userName = name;


						for(ServerThread client: connectedClients)
						{
							for(ServerThread clientName: connectedClients) 
							{
							client.getDos().writeInt(Constants.REGISTER_BROADCAST);
							client.getDos().writeUTF(clientName.getUserName());
							}
						}

						break;
					case Constants.PRIVATE_MESSAGE:
						String data1 = dis.readUTF();
						serverController.getTextArea().appendText(remoteClient.getInetAddress()+":"+remoteClient.getPort()+"("+userName+")"+">"+data1+"\n");
						
						String[] str = data1.split("\\s+");
						String name1 = str[0].substring(1);

						for(ServerThread otherClient: connectedClients)
						{
							if(otherClient.userName.equals(name1))
							{
								otherClient.getDos().writeInt(Constants.CHAT_BROADCAST);
								otherClient.getDos().writeUTF("Private message from " + userName + " : " + data1);
							}
							else if(otherClient.equals(this))
							{
								otherClient.getDos().writeInt(Constants.CHAT_BROADCAST);
								otherClient.getDos().writeUTF("My private message : " + data1);
							}
						}
						
						break;
						
					case Constants.CANVAS_BROADCAST:
						String str1 = dis.readUTF();
						for(ServerThread otherClient: connectedClients)
						{
							otherClient.getDos().writeInt(Constants.CANVAS_BROADCAST);
							otherClient.getDos().writeUTF(str1);
						}
						
						break;
				}				
			}
			catch (IOException e)
			{
				e.printStackTrace();
				return;
			}
		}
	}

	public DataOutputStream getDos() {
		return dos;
	}

	public String getUserName()
	{
		return userName;
	}
}
