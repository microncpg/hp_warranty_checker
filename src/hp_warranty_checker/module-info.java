module hp_warranty_checker {
	exports kk.run;
	exports kk.gui;

	opens kk.gui;

	requires org.jsoup;
	requires java.logging;
	requires javafx.graphics;
	requires javafx.fxml;
	requires javafx.controls;
	requires fastcsv;

	requires htmlunit;
}