package nz.ac.unitec.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;

import org.json.JSONObject;
import org.json.JSONWriter;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

public class ClientController implements Runnable {
	
	Socket client;
	DataInputStream dis;
	DataOutputStream dos;
	ObservableList<String> names = FXCollections.observableArrayList();
	double startXL;				///< Local startX coordinate
	double startYL;				///< Local startY coordinate
	double startXR;				///< Remote startX coordinate
	double startYR;				///< Remote startY coordinate
	GraphicsContext gc;
	Color color = Color.BLACK;	///< Current color
	Tool tool = Tool.PEN;		///< Current tool
	
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
	private ToggleGroup tgColor;
	
	@FXML
	private ToggleGroup tgTool;
	
	@FXML
	private Pane wrapperPane;
    
    @FXML
    void initialize() {
    	
    	/// Bind Wrapper Pane and Canvas dimensions
        canvas.widthProperty().bind(wrapperPane.widthProperty());
        canvas.heightProperty().bind(wrapperPane.heightProperty());
        canvas.widthProperty().addListener(event -> draw(canvas));
        canvas.heightProperty().addListener(event -> draw(canvas));
        draw(canvas);
    	
    	gc = canvas.getGraphicsContext2D();
    	
    	/// Mouse pressed event
    	canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, 
    		new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent event) {
					
					startXL = event.getX();
					startYL = event.getY();

	            	gc.setStroke(color);
	            	
					if((tool.equals(Tool.PEN)) || (tool.equals(Tool.LINE))) {
		            	gc.beginPath();
		            	gc.moveTo(startXL, startYL);
	            		gc.stroke();
					}
					
            		try {
            			StringWriter sw = new StringWriter();
    					JSONWriter jw = new JSONWriter(sw);
    					jw.object();
    					jw.key("tool").value(tool.toString());
						jw.key("action").value("pressed");
						jw.key("color").value("" + color);
						jw.key("x").value("" + startXL);
						jw.key("y").value("" + startYL);
						jw.endObject();
            			dos.writeInt(Constants.CANVAS_BROADCAST);
						dos.writeUTF(sw.toString());
	        			dos.flush();
					} catch (UnsupportedEncodingException e1) {
						e1.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});

    	/// Mouse dragged event
    	canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, 
    		new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent event) {
					
					double x = event.getX();
					double y = event.getY();
					
					if(tool.equals(Tool.PEN)) {
			            gc.lineTo(x, y);
			            gc.stroke();
					}
            		
            		try {
            			StringWriter sw = new StringWriter();
    					JSONWriter jw = new JSONWriter(sw);
    					jw.object();
            			jw.key("tool").value(tool.toString());
						jw.key("action").value("dragged");
						jw.key("color").value("" + color.toString());
						jw.key("x").value("" + x);
						jw.key("y").value("" + y);
						jw.endObject();
            			dos.writeInt(Constants.CANVAS_BROADCAST);
						dos.writeUTF(sw.toString());
	        			dos.flush();
					} catch (UnsupportedEncodingException e1) {
						e1.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
    		});

    	/// Mouse released event
    	canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, 
    		new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent event) {
					
					double x = event.getX();
					double y = event.getY();
					
					if(tool == Tool.LINE) {
		            	gc.lineTo(x, y);
		            	gc.stroke();
					}
            		
            		try {
            			StringWriter sw = new StringWriter();
    					JSONWriter jw = new JSONWriter(sw);
    					jw.object();
            			jw.key("tool").value(tool.toString());
						jw.key("action").value("released");
						jw.key("color").value("" + color.toString());
						jw.key("x").value("" + x);
						jw.key("y").value("" + y);
						jw.endObject();
            			dos.writeInt(Constants.CANVAS_BROADCAST);
						dos.writeUTF(sw.toString());
	        			dos.flush();
					} catch (UnsupportedEncodingException e1) {
						e1.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
    		});
    	
    	/// Color picking
    	tgColor.selectedToggleProperty().addListener(new ChangeListener<Toggle>()
        {
	        @Override
	        public void changed(ObservableValue<? extends Toggle> ov, Toggle t, Toggle t1)
            {
	        	RadioButton rb = (RadioButton)t1.getToggleGroup().getSelectedToggle();
	        	color = Color.valueOf(rb.getStyleClass().toString().split(" ")[1]);
            }
        });
    	
    	/// Tool picking
    	tgTool.selectedToggleProperty().addListener(new ChangeListener<Toggle>()
        {
	        @Override
	        public void changed(ObservableValue<? extends Toggle> ov, Toggle t, Toggle t1)
            {
	        	RadioButton rb = (RadioButton)t1.getToggleGroup().getSelectedToggle();
	        	tool = new Tool(Tool.valueOf(rb.idProperty().getValue()));
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
								if(!names.contains(name)) {
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
						
						/// Set stroke color
						int c = Integer.decode(obj.getString("color").substring(0, 8));
		            	gc.setStroke(Color.rgb((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF));
		            	
						double x = obj.getDouble("x");
						double y = obj.getDouble("y");
						String action = obj.getString("action");
						String tl = obj.getString("tool");
						
						if(action.equals("pressed")) {
							startXR = x;
							startYR = y;
						}
						
						/// Pen
						if(Tool.PEN.toString().equals(tl)) {
							if(action.equals("pressed")) {
								Platform.runLater(new Runnable() {
									@Override public void run() {
						            	gc.beginPath();
						            	gc.moveTo(startXR, startYR);
					            		gc.stroke();
									}
								});
							} else if(action.equals("dragged")) {
								Platform.runLater(new Runnable() {
									@Override public void run() {
						            	gc.lineTo(x, y);
						            	gc.stroke();
									}
								});
							}
						
						/// Line
						} else if(Tool.LINE.toString().equals(tl)) {
							if(action.equals("pressed")) {
								Platform.runLater(new Runnable() {
									@Override public void run() {
						            	gc.beginPath();
						            	gc.moveTo(startXR, startYR);
					            		gc.stroke();
									}
								});
							} else if(action.equals("released")) {
								Platform.runLater(new Runnable() {
									@Override public void run() {
						            	gc.lineTo(x, y);
						            	gc.stroke();
									}
								});
							}
							
						/// Circle
						} else if(Tool.CIRCLE.toString().equals(tl)) {
							if(action.equals("released")) {
								Platform.runLater(new Runnable() {
									@Override public void run() {
										drawCircle(startXR, startYR, x, y);
									}
								});
							}
							
						/// Rectangle
						} else if(Tool.RECTANGLE.toString().equals(tl)) {
							if(action.equals("released")) {
								Platform.runLater(new Runnable() {
									@Override public void run() {
										drawRectangle(startXR, startYR, x, y);
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
	
	void draw(Canvas c) {
	}
	
	void drawCircle(double startX, double startY, double endX, double endY) {
		double topLeftX = startX < endX ? startX : endX;
		double topLeftY = startY < endY ? startY : endY;
		double w = startX < endX ? endX - startX : startX - endX;
		double h = startY < endY ? endY - startY : startY - endY;
		gc.strokeOval(topLeftX, topLeftY, w, h);
	}
	
	void drawRectangle(double startX, double startY, double endX, double endY) {
		double topLeftX = startX < endX ? startX : endX;
		double topLeftY = startY < endY ? startY : endY;
		double w = startX < endX ? endX - startX : startX - endX;
		double h = startY < endY ? endY - startY : startY - endY;
		gc.strokeRect(topLeftX, topLeftY, w, h);
	}
}

