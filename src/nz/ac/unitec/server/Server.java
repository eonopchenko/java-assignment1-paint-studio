package nz.ac.unitec.server;

import java.net.URL;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class Server extends Application {
	
	public static void main(String[] args) {
		Application.launch(args);
	}

	@Override
	public void start(Stage stage) throws Exception {
		URL fxmlUrl = this.getClass().getClassLoader().getResource("ServerLayout.fxml");
		AnchorPane root = FXMLLoader.<AnchorPane>load(fxmlUrl);
		Scene scene = new Scene(root);
		stage.setScene(scene);
		stage.setTitle("Server");
		stage.show();
	}
}
