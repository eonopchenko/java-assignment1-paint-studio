package nz.ac.unitec.client;
import java.util.Optional;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class Client extends Application {
	private static String name;
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
//	    	    ClientController controller = loader.getController();

	            Scene scene = new Scene(mainPane);
	            scene.getStylesheets().add(getClass().getResource("client.css").toExternalForm());

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