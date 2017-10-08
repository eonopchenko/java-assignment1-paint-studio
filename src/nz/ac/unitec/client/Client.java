package nz.ac.unitec.client;
import java.net.URL;
import java.util.Optional;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import nz.ac.unitec.client.ClientController;

public class Client extends Application {
	private static String name;
	private double width;
	private double height;
	
    public void start(Stage primaryStage) throws Exception {

	    	TextInputDialog dialog = new TextInputDialog("Walter");
	    	dialog.setTitle("Nickname Input");
	    	dialog.setHeaderText("");
	    	dialog.setContentText("Please enter your nickname:");

	    	Optional<String> result = dialog.showAndWait();
	    	if (result.isPresent()) {
	    	    name = result.get();
	    	    
	    	    FXMLLoader loader = new FXMLLoader(this.getClass().getClassLoader().getResource("ClientLayout.fxml"));
	    	    Pane mainPane = loader.load();
	    	    ClientController controller = loader.getController();

	            Scene scene = new Scene(mainPane);
	            scene.getStylesheets().add(getClass().getResource("client.css").toExternalForm());

	            scene.widthProperty().addListener(new ChangeListener<Number>() {
	                @Override public void changed(ObservableValue<? extends Number> observableValue, Number oldSceneWidth, Number newSceneWidth) {
	                	width = (double) newSceneWidth;
	                    controller.SizeChanged(width, height);
	                }
	            });
	            
	            scene.heightProperty().addListener(new ChangeListener<Number>() {
	                @Override public void changed(ObservableValue<? extends Number> observableValue, Number oldSceneHeight, Number newSceneHeight) {
	                	height = (double) newSceneHeight;
	                    controller.SizeChanged(width, height);
	                }
	            });

	            primaryStage.setTitle("Chat Client " + name);
	            primaryStage.setScene(scene);
	            primaryStage.show();
	    	}
    }

    public static void main(String[] args) {
        Application.launch(args);
    }

	public static String getName()
	{
		return name;
	}    
    
}