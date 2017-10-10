package nz.ac.unitec.client;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

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
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

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
	private Stage stage;
	
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
	private Button btnLoadImage;
    
    @FXML
    void initialize() {
    	
    	/// Bind Wrapper Pane and Canvas dimensions
        canvas.widthProperty().bind(wrapperPane.widthProperty());
        canvas.heightProperty().bind(wrapperPane.heightProperty());
    	
    	gc = canvas.getGraphicsContext2D();
    	
    	/// Mouse pressed event
    	canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, 
    		new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent event) {
					
					/// Save start coordinates
					startXL = event.getX();
					startYL = event.getY();

					/// Set stroke color
	            	gc.setStroke(color);
	            	
	            	/// Begin path for Pen or Line tool
					if((tool.equals(Tool.PEN)) || (tool.equals(Tool.LINE))) {
		            	gc.beginPath();
		            	gc.moveTo(startXL, startYL);
	            		gc.stroke();
					}
					
					/// Build json object and transmit
					broadcastCanvas("pressed", startXL, startYL);
				}
			});

    	/// Mouse dragged event
    	canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, 
    		new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent event) {
					
					double x = event.getX();
					double y = event.getY();
					
					/// Draw line up to the current coordinate
					if(tool.equals(Tool.PEN)) {
			            gc.lineTo(x, y);
			            gc.stroke();
					}

					/// Build json object and transmit
					broadcastCanvas("dragged", x, y);
				}
    		});

    	/// Mouse released event
    	canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, 
    		new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent event) {
					
					double x = event.getX();
					double y = event.getY();

					/// Draw line up to the current coordinate
					if(tool == Tool.LINE) {
		            	gc.lineTo(x, y);
		            	gc.stroke();
					}

					/// Build json object and transmit
					broadcastCanvas("released", x, y);
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
    
    public void SetStage(Stage stage) {
    	this.stage = stage;
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
    void btnLoadImageOnActionHandler(ActionEvent event) {
    	FileChooser fileChooser = new FileChooser();
    	fileChooser.setTitle("Open Image File");
    	File file = fileChooser.showOpenDialog(stage);
    	
    	if(file != null) {
    	
			Image img = new Image(file.toURI().toString());
			BufferedImage bImage = null;
			try {
				bImage = ImageIO.read(file);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			gc.drawImage(img, 50, 50);
			
			/// Serialize and write image to the output stream
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
	        try {
				ImageIO.write(bImage, "png", byteArrayOutputStream);
				int s = byteArrayOutputStream.size();
		        byte[] size = ByteBuffer.allocate(4).putInt(s).array();
		        
				dos.writeInt(Constants.IMAGE_BROADCAST);
				dos.write(size);
				dos.write(byteArrayOutputStream.toByteArray());
				dos.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
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
						Color col = Color.rgb((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF);
		            	
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
						            	gc.setStroke(col);
						            	gc.beginPath();
						            	gc.moveTo(startXR, startYR);
					            		gc.stroke();
									}
								});
							} else if(action.equals("dragged")) {
								Platform.runLater(new Runnable() {
									@Override public void run() {
						            	gc.setStroke(col);
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
						            	gc.setStroke(col);
						            	gc.beginPath();
						            	gc.moveTo(startXR, startYR);
					            		gc.stroke();
									}
								});
							} else if(action.equals("released")) {
								Platform.runLater(new Runnable() {
									@Override public void run() {
						            	gc.setStroke(col);
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
						            	gc.setStroke(col);
										drawCircle(startXR, startYR, x, y);
									}
								});
							}
							
						/// Rectangle
						} else if(Tool.RECTANGLE.toString().equals(tl)) {
							if(action.equals("released")) {
								Platform.runLater(new Runnable() {
									@Override public void run() {
						            	gc.setStroke(col);
										drawRectangle(startXR, startYR, x, y);
									}
								});
							}
						}
	            		
						break;
						
					case Constants.IMAGE_BROADCAST:
						
						/// Read image from input stream
				        byte[] sizeAr = new byte[4];
				        dis.read(sizeAr);
				        int size = ByteBuffer.wrap(sizeAr).asIntBuffer().get();
				        
				        System.out.println("client: input size = " + size);

				        byte[] imageAr = new byte[size];
				        dis.read(imageAr);
				        
				        gc.drawImage(new Image(new ByteArrayInputStream(imageAr)), 50, 50);
				        
						break;
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/// Canvas network broadcasting
	void broadcastCanvas(String action, double x, double y) {
		try {
			StringWriter sw = new StringWriter();
			JSONWriter jw = new JSONWriter(sw);
			jw.object();
			jw.key("tool").value(tool.toString());
			jw.key("action").value(action);
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

