package kk.gui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Region;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import kk.beans.Record;
import kk.logic.CsvProcessor;
import kk.logic.HpWarrantyChecker;

public class GUI {

	private Paint paintGreen = Paint.valueOf("#00C800");
	private Paint paintYellow = Paint.valueOf("#C8C800");
	private Paint paintRed = Paint.valueOf("#C80000");

	public static Stage stage;

	@FXML
	private Label labelStatusBar;

	@FXML
	private Circle circle;

	@FXML
	private Button buttonProcess;

	void processData(File file) {
		new Thread() {
			@Override
			public void run() {
				disableGui();

				updateStatus("Processing...");
				circle.setFill(paintYellow);

				updateStatus(String.format("Reading input CSV file..."));

				List<Record> records = new ArrayList<>();

				try {
					records = CsvProcessor.loadCsv(file.getAbsolutePath());
				} catch (IOException e) {
					e.printStackTrace();

					runAlert(AlertType.ERROR, "Error", "CSV error",
							"Error during loading input CSV file. Please make sure it has correct format (columns: \"Motherboard\", \"HP Serial Number\", \"Description\"), and the data is in correct format.");

					circle.setFill(paintGreen);
					updateStatus("Ready.");

					enableGui();

					buttonProcess.setStyle("-fx-border-color: #C6C6C6;");
					return;
				}
				int chunk = 20;

				final int sizeRecords = records.size();

				for (int i = 0; i < sizeRecords; i += chunk) {
					updateStatus(String.format("Processing records (%d-%d/%d)...", i,
							i + chunk < sizeRecords ? i + chunk : sizeRecords, sizeRecords));

					int recs = sizeRecords - i > chunk ? chunk : sizeRecords - i;
					List<Record> recordsChunk = records.subList(i, i + recs);

					processChunk(recordsChunk);

					List<Record> recordsChunkClean = recordsChunk.stream()
							.filter(r -> !r.description.equals("Not found")).collect(Collectors.toList());

					if (recordsChunkClean.size() != recordsChunk.size()) {
						updateStatus(String.format("Processing records (%d-%d/%d), again... ", i,
								i + chunk < sizeRecords ? i + chunk : sizeRecords, records.size()));
						processChunk(recordsChunkClean);
					}
				}

				try {
					final String outputFile = file.getAbsolutePath().replaceAll("\\.csv", "_output.csv");
					CsvProcessor.saveCsv(records, outputFile);

					runAlert(AlertType.INFORMATION, "Success", "CSV File saved",
							"Processing finished successfully. Output file " + outputFile + " has been saved.");
				} catch (IOException e) {
					e.printStackTrace();

					runAlert(AlertType.ERROR, "Error", "Saving error", "Error during saving output CSV file.");

					circle.setFill(paintGreen);
					updateStatus("Ready.");

					enableGui();

					buttonProcess.setStyle("-fx-border-color: #C6C6C6;");
					return;
				}

				circle.setFill(paintGreen);
				updateStatus("Ready.");

				enableGui();

				buttonProcess.setStyle("-fx-border-color: #C6C6C6;");

			}

			private void processChunk(List<Record> recordsChunk) {
				boolean success = false;
				int tries = 0;
				while (!success && tries++ < 3) {
					try {
						HpWarrantyChecker.processChunk(recordsChunk);
						success = true;
					} catch (FailingHttpStatusCodeException | IOException | InterruptedException e) {
						e.printStackTrace();
						updateStatus("Problem with retrieval, trying again...");
					}
				}

				if (!success) {
					runAlert(AlertType.ERROR, "Error", "Processing error",
							"Unexpected error during processing HP Warranty Check website.");

					circle.setFill(paintGreen);
					updateStatus("Ready.");

					enableGui();

					buttonProcess.setStyle("-fx-border-color: #C6C6C6;");
					return;
				}
			}
		}.start();
	}

	@FXML
	void process(ActionEvent event) {
		FileChooser chooser = new FileChooser();
		chooser.setTitle("Choose input CSV file to process");
		chooser.getExtensionFilters().add(new ExtensionFilter("CSV file (*.csv)", "*.csv"));
		File file = chooser.showOpenDialog(stage.getOwner());

		if (file != null)
			processData(file);
	}

	@FXML
	private void initialize() {
		circle.setFill(paintGreen);

		buttonProcess.setOnDragOver(new EventHandler<DragEvent>() {
			@Override
			public void handle(final DragEvent e) {
				final Dragboard db = e.getDragboard();

				boolean isAccepted = db.getFiles().get(0).getName().toLowerCase().endsWith(".csv")
						|| db.getFiles().get(0).getName().toLowerCase().endsWith(".CSV");

				if (db.hasFiles()) {
					if (isAccepted) {
						buttonProcess.setStyle("" + "-fx-border-color: green;" + "-fx-border-width: 5;"
								+ "-fx-background-color: #90ee90;" + "-fx-border-style: solid;");
						e.acceptTransferModes(TransferMode.COPY);
					} else {
						buttonProcess.setStyle("" + "-fx-border-color: red;" + "-fx-border-width: 5;"
								+ "-fx-background-color: #ee9090;" + "-fx-border-style: solid;");
						e.acceptTransferModes(TransferMode.NONE);
					}
				} else {
					e.consume();
				}
			}
		});

		buttonProcess.setOnDragDropped(new EventHandler<DragEvent>() {
			@Override
			public void handle(final DragEvent e) {
				final Dragboard db = e.getDragboard();
				boolean success = false;
				if (db.hasFiles()) {
					success = true;
					final File file = db.getFiles().get(0);
					processData(file);
				}
				e.setDropCompleted(success);
				e.consume();
			}
		});

		buttonProcess.setOnDragExited(new EventHandler<DragEvent>() {
			@Override
			public void handle(final DragEvent event) {
				buttonProcess.setStyle("-fx-border-color: #C6C6C6;");
			}
		});
	}

	public void updateStatus(String status) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				labelStatusBar.setText(status);
			}
		});
	}

	public void disableGui() {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				buttonProcess.setDisable(true);
			}
		});
	}

	public void enableGui() {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				buttonProcess.setDisable(false);
			}
		});
	}

	public void setCircleGreen() {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				circle.setFill(paintGreen);
			}
		});
	}

	public void setCircleYellow() {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				circle.setFill(paintYellow);
			}
		});
	}

	public void setCircleRed() {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				circle.setFill(paintRed);
			}
		});
	}

	private void runAlert(AlertType alertType, String title, String header, String content) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				Alert alert = new Alert(alertType);
				alert.setTitle(title);
				alert.setHeaderText(header);
				alert.setContentText(content);
				alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
				alert.showAndWait();
			}
		});
	}
}
