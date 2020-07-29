package kk.logic;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import de.siegmar.fastcsv.reader.CsvContainer;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRow;
import kk.beans.Record;

public class CsvProcessor {
	public static void saveCsv(List<Record> records, String pathCsv) throws IOException {
		File csvFile = new File(pathCsv);
		if (!csvFile.isFile())
			csvFile.createNewFile();
		BufferedWriter csvFileOutputStream = null;
		try {
			FileOutputStream fileOutputStream = new FileOutputStream(csvFile);
			// add BOM to the very head of CVS
			fileOutputStream.write(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF });
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8);
			csvFileOutputStream = new BufferedWriter(outputStreamWriter, 1024);
			// add header
			csvFileOutputStream.write("Motherboard");
			csvFileOutputStream.write(",");
			csvFileOutputStream.write("HP Serial Number");
			csvFileOutputStream.write(",");
			csvFileOutputStream.write("Description");
			csvFileOutputStream.newLine();

			// add content
			int totalNum = records.size();
			for (int i = 0; i < totalNum; i++) {
				csvFileOutputStream.write(records.get(i).motherboard);
				csvFileOutputStream.write(",");
				csvFileOutputStream.write(records.get(i).serial);
				csvFileOutputStream.write(",");
				csvFileOutputStream.write(records.get(i).description);
				if (i != totalNum -1)
					csvFileOutputStream.newLine();
			}
			csvFileOutputStream.flush();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				csvFileOutputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/*
		 * try (CsvAppender csvAppender = new CsvWriter().append(new File(pathCsv),
		 * StandardCharsets.UTF_8)) { csvAppender.appendLine("Motherboard",
		 * "HP Serial Number", "Description"); for (Record r : records)
		 * csvAppender.appendLine(r.motherboard, r.serial, r.description); }
		 */
	}

	public static List<Record> loadCsv(String path) throws IOException {
		List<Record> records = new ArrayList<>();

		CsvReader csvReader = new CsvReader();
		csvReader.setContainsHeader(true);
		CsvContainer csv = csvReader.read(new File(path), StandardCharsets.UTF_8);

		System.out.println(csv.getHeader());

		for (CsvRow row : csv.getRows()) {
			Record r = new Record();
			r.motherboard = row.getField(0);
			r.serial = row.getField(1);
			r.description = row.getField(2);

			records.add(r);
		}

		return records;
	}
}
