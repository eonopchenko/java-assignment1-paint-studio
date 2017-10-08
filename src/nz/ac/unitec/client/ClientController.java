package nz.ac.unitec.client;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import nz.ac.unitec.client.Constants;
import org.json.*;

public class ClientController implements Runnable {
	
	Socket client;
	DataInputStream dis;
	DataOutputStream dos;
	ObservableList<String> names = FXCollections.observableArrayList();
	double startXL;
	double startYL;
	double startXR;
	double startYR;
	GraphicsContext graphicsContext;
	
	@FXML
	private ListView<String> lvUsers;
	
	@FXML
	private TextArea taChat;
	
	@FXML
	private TextField tfSend;

    @FXML
    private Button btnSend;
    
    @FXML
    private Canvas canvas;
    
    @FXML
    void initialize() {
    	graphicsContext = canvas.getGraphicsContext2D();
    	
        double canvasWidth = graphicsContext.getCanvas().getWidth();
        double canvasHeight = graphicsContext.getCanvas().getHeight();
        
        graphicsContext.setFill(Color.LIGHTGRAY);
        graphicsContext.setStroke(Color.BLACK);
        graphicsContext.setLineWidth(5);

        graphicsContext.fill();
        graphicsContext.strokeRect(0, 0, canvasWidth, canvasHeight);

        graphicsContext.setFill(Color.RED);
        graphicsContext.setStroke(Color.BLUE);
        graphicsContext.setLineWidth(1);

    	canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, 
    		new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent event) {
					startXL = event.getX();
					startYL = event.getY();
	            	graphicsContext.beginPath();
	            	graphicsContext.moveTo(startXL, startYL);
	            	graphicsContext.setStroke(Color.BLACK);
            		graphicsContext.stroke();
            		
            		try {
            			dos.writeInt(Constants.CANVAS_BROADCAST);
            			StringWriter sw = new StringWriter();
						JSONWriter jw = new JSONWriter(sw);
						jw.object();
						jw.key("tool").value("pen");
						jw.key("action").value("pressed");
						jw.key("x").value("" + startXL);
						jw.key("y").value("" + startYL);
						jw.endObject();
						dos.writeUTF(sw.toString());
	        			dos.flush();
					} catch (UnsupportedEncodingException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});

    	canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, 
    		new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent event) {
					double x = event.getX();
					double y = event.getY();
	            	graphicsContext.setStroke(Color.BLACK);
	            	graphicsContext.lineTo(x, y);
	            	graphicsContext.stroke();
            		
            		try {
            			dos.writeInt(Constants.CANVAS_BROADCAST);
            			StringWriter sw = new StringWriter();
						JSONWriter jw = new JSONWriter(sw);
						jw.object();
						jw.key("tool").value("pen");
						jw.key("action").value("dragged");
						jw.key("x").value("" + x);
						jw.key("y").value("" + y);
						jw.endObject();
						dos.writeUTF(sw.toString());
	        			dos.flush();
					} catch (UnsupportedEncodingException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
    		});

    	canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, 
    		new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent event) {
					double x = event.getX();
					double y = event.getY();
					
	            	double endX = x - startXL;
	            	endX =  endX < 0 ? (endX * -1) : endX;
	            	
	            	double endY = y - startYL;
	            	endY = endY < 0 ? (endY * -1) : endY;
            		
            		try {
            			dos.writeInt(Constants.CANVAS_BROADCAST);
            			StringWriter sw = new StringWriter();
						JSONWriter jw = new JSONWriter(sw);
						jw.object();
						jw.key("tool").value("pen");
						jw.key("action").value("released");
						jw.key("x").value("" + x);
						jw.key("y").value("" + y);
						jw.endObject();
						dos.writeUTF(sw.toString());
	        			dos.flush();
					} catch (UnsupportedEncodingException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
    		});
    }
    
    @FXML
    void btnSendOnActionHandler(ActionEvent event) {
		try {
			if(tfSend.getText().charAt(0) == '@') {
				dos.writeInt(Constants.PRIVATE_MESSAGE);
			} else {
				dos.writeInt(Constants.CHAT_MESSAGE);
			}
			dos.writeUTF(tfSend.getText());
			dos.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		tfSend.clear();
    }
    
    @FXML
    void lvUsersOnMouseClickedHandler() {
    	String name = lvUsers.getSelectionModel().getSelectedItem();
    	tfSend.setText("@" + name + " ");
    }
    
    public ClientController() {
		try {
			client = new Socket("localhost", 5000);
			dis = new DataInputStream(client.getInputStream());
			dos = new DataOutputStream(client.getOutputStream());
			
			dos.writeInt(Constants.REGISTER_CLIENT);
			dos.writeUTF(Client.getName());
			dos.flush();
			
			Thread clientThread = new Thread(this);
			clientThread.start();
		}
		catch (UnknownHostException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
    }	

	@Override
	public void run() {	
		while(true)
		{
			try {
				int messageType = dis.readInt();

				switch(messageType)
				{
					case Constants.CHAT_BROADCAST:
						taChat.appendText(dis.readUTF()+"\n");
						break;
					case Constants.REGISTER_BROADCAST:
						String name = dis.readUTF();
						Platform.runLater(new Runnable() {
							@Override public void run() {
								if(!names.contains(name))
								{
									taChat.appendText(name + " has joined the chat"+"\n");
									names.add(name);
								}
								lvUsers.setItems(names);
							}
						});
						break;
					case Constants.CANVAS_BROADCAST:
						String str = dis.readUTF();
						JSONObject obj = new JSONObject(str);
						String tool = obj.getString("tool");
						
						if(tool.equals("pen"))
						{
							double x = obj.getDouble("x");
							double y = obj.getDouble("y");
							String action = obj.getString("action");
							if(action.equals("pressed"))
							{
								startXR = x;
								startYR = y;
								Platform.runLater(new Runnable() {
									@Override public void run() {
						            	graphicsContext.beginPath();
						            	graphicsContext.moveTo(startXR, startYR);
						            	graphicsContext.setStroke(Color.BLACK);
					            		graphicsContext.stroke();
									}
								});
							} else if(action.equals("dragged")) {
								Platform.runLater(new Runnable() {
									@Override public void run() {
						            	graphicsContext.setStroke(Color.BLACK);
						            	graphicsContext.lineTo(x, y);
						            	graphicsContext.stroke();
									}
								});
							} else if(action.equals("released")) {
								Platform.runLater(new Runnable() {
									@Override public void run() {
						            	double endX = x - startXR;
						            	endX =  endX < 0 ? (endX * -1) : endX;
						            	double endY = y - startYR;
						            	endY = endY < 0 ? (endY * -1) : endY;
									}
								});
							}
						}
	            		
						break;
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	/// @todo implement generic types
	public void SizeChanged(double width, double height) {
		double canvasWidth = (2f / 3f) * width;
		double canvasHeight = (3f / 4f) * height;
        canvas.setWidth(canvasWidth);
        canvas.setHeight(canvasHeight);
	}
}

