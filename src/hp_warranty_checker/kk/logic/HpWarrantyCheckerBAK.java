package kk.logic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlButtonInput;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlHeading2;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.gargoylesoftware.htmlunit.util.Cookie;

import kk.beans.Record;

public class HpWarrantyCheckerBAK {

	public static void processChunk(List<Record> records)
			throws FailingHttpStatusCodeException, MalformedURLException, IOException, InterruptedException {
		/*
		 * try (final WebClient webClient = new
		 * WebClient(BrowserVersion.BEST_SUPPORTED,"proxy-web.micron.com",80)) {
		 * 
		 * webClient.getOptions().setJavaScriptEnabled(true);
		 * webClient.getOptions().setThrowExceptionOnScriptError(false);
		 * webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
		 * webClient.getOptions().setCssEnabled(false);
		 * webClient.getOptions().setPrintContentOnFailingStatusCode(false);
		 * webClient.setAjaxController(new NicelyResynchronizingAjaxController());
		 * webClient.getOptions().setUseInsecureSSL(true);
		 * 
		 * 
		 * java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF
		 * );
		 * 
		 * HtmlPage page = webClient.getPage(
		 * "https://support.hp.com/us-en/checkwarranty/multipleproducts");
		 */

		/**************************
		 * Added by Joe - start
		 *****************************/

		try (WebClient wc = new WebClient(BrowserVersion.CHROME,"proxy-web.micron.com",80)) {
//		try (WebClient wc = new WebClient(BrowserVersion.BEST_SUPPORTED)) {

			// Add on demand
			wc.getOptions().setJavaScriptEnabled(true);
			wc.getOptions().setThrowExceptionOnScriptError(true);
			wc.getOptions().setThrowExceptionOnFailingStatusCode(true);
			wc.getOptions().setCssEnabled(false);
			wc.getOptions().setPrintContentOnFailingStatusCode(false);
			wc.setAjaxController(new NicelyResynchronizingAjaxController());
			wc.getOptions().setUseInsecureSSL(true);
			wc.getCookieManager().setCookiesEnabled(true);
			wc.getCookieManager().clearCookies();
			wc.getOptions().setTimeout(600 * 1000);
			wc.setJavaScriptTimeout(600 * 1000);

			java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.ALL);
			
			//generate a file for caching cookies
			File file = new File("hp.cookie");
			if (!file.isFile())
				file.createNewFile();
			if (file.length() > 0) {
				FileInputStream fileIn = new FileInputStream(file);
				ObjectInputStream in = new ObjectInputStream(fileIn);
				try {
					Set<Cookie> cookies = (Set<Cookie>) in.readObject();
					Iterator<Cookie> item = cookies.iterator();
					while (item.hasNext()) {
						wc.getCookieManager().addCookie(item.next());
					}
				} catch (ClassNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} finally {
					in.close();
					fileIn.close();
				}
			}

			// ready for request
			HtmlPage page = null;
			page = wc.getPage("https://support.hp.com/cn-zh/checkwarranty/multipleproducts");

			if (page == null) {
				System.out.println("URL: https://support.hp.com/cn-zh/checkwarranty/multipleproducts has no response!");
				return;
			}

			// add cookie to cookie file
			FileOutputStream fileOut = new FileOutputStream("hp.cookie");
			ObjectOutput out = new ObjectOutputStream(fileOut);
			out.writeObject(wc.getCookieManager().getCookies());
			out.flush();
			fileOut.flush();
			out.close();
			fileOut.close();
			/****************************
			 * Added by Joe - End
			 ****************************/

			// original codes start here
			final HtmlForm form = page.getForms().stream().filter(f -> f.getId().equals("frmMultipleWarranty")).findFirst().orElseThrow();
			final List<HtmlInput> textFields = form.getInputsByName("serial");
			final HtmlButtonInput button = form.getInputByValue("提交");
			int error1Number = 0;

			for (int i = 0; i < records.size(); i++) {
				//Exclude illegal serial numbers
		        int serialLen = records.get(i).serial.length();
		        
		        if(records.get(i).serial == null || records.get(i).serial == "") {
					records.get(i).description = "Not found";
					error1Number++;
					continue;
				}else if((serialLen > 0 && serialLen < 10) || serialLen > 15) {
			          records.get(i).description = "Not found";
			          error1Number++;
			          continue;
			    }
				
				((HtmlTextInput) textFields.get(i)).setText(records.get(i).serial);

				final List<HtmlAnchor> anchors = page.getByXPath(
						"//ul[@id='wFormEmailCountry" + (i + 1) + "_dd_list']/li/a[text()='大不列颠及北爱尔兰联合王国']");
				final HtmlAnchor dropDown = anchors.get(0);
				dropDown.click();
			}

			synchronized (page) {
				page.wait(200);
			}
			button.click();

			int tries = 0;
			List<String> names = new ArrayList<>();
			List<HtmlElement> errors = new ArrayList<>();
			while (names.size() == 0 && errors.size() == 0) {
				synchronized (page) {
					System.out.println("waiting...");
					page.wait(500);
					if (tries++ > 20) {
						System.out.println("Exiting!");
						return;
					}
				}

				names = page.getByXPath("//div[@class='warrantyResultsTable hidden-sm']/table/tbody/tr/td/h2[1]")
						.stream().map(e -> ((HtmlHeading2) e).asText()).collect(Collectors.toList());
				//This product cannot be identified by Serial Number alone. Please enter a product number or remove this entry
				errors = page.getByXPath("//div[contains(@id,'errorLineOne')][not(contains(@class,'hide'))]");

				if (errors.size() > 0) {

					List<Integer> ids = errors.stream().map(e -> e.getId().replaceAll("errorLineOne", ""))
							.map(id -> Integer.parseInt(id)).collect(Collectors.toList());
					for (int id : ids)
						records.get(id - 1).description = "Not found";

					if (ids.size() > 0)
						return;
				}
				
				if(error1Number > 0) {
					return;
				}
			}

			for (int i = 0; i < records.size(); i++) {
				System.out.println(records.get(i).motherboard + " : " + records.get(i).serial + " -> " + names.get(i));
				if("".contentEquals(names.get(i))) {
					records.get(i).description = "Not found";
					continue;
				}
				records.get(i).description = names.get(i);
			}
		}
	}

}
