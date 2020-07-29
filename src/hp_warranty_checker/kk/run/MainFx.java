package kk.run;

import java.io.IOException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import kk.gui.GUI;

public class MainFx extends Application {
	@Override
	public void start(Stage stage) throws IOException {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/kk/fxml/GUI.fxml"));
		GUI.stage = stage;

		Pane newPane = loader.load();
		stage.setScene(new Scene(newPane));
		System.out.println(GUI.class.getResourceAsStream("icon.png"));
		Image image = new Image(GUI.class.getResourceAsStream("icon.png"));
		stage.getIcons().add(image);
		stage.setTitle("HP Warranty Checker");
		stage.setResizable(false);
		stage.setX(200);
		stage.setY(200);
		stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent t) {
				Platform.exit();
				System.exit(0);
			}
		});
		stage.show();
	}

	public static void main(String[] args) {
		launch(args);
	}
}
