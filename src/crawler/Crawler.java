package crawler;


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import wrappers.Wrapper4WebMD;

import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;


public class Crawler {
	Wrapper4WebMD wrapper = new Wrapper4WebMD();
	
	public Crawler() {
		
		System.setProperty("http.proxyHost", "http://proxyserver.com/");
		//System.setProperty("http.proxyPort", "1080");
	}

	public void start(String entryPoint, String board) {
		int index = 1, page;
		boolean duplicate;
		Document indexDoc = null, threadDoc = null;
		String jsonFilename, htmlFilename, htmlFilenameBase;
		String url = "", baseURL = "http://forums.webmd.com";
		
		indexDoc = getDoc(entryPoint);	
		
		while (indexDoc != null) { //for each page in the index
			Elements threadElements = indexDoc.getElementsByClass("thread_fmt");
			
			for (Element element: threadElements) { //for each thread on the index page
				jsonFilename = "./data/json/WebMD/" + board + "/ncj2ey-thread" + Integer.toString(index) + ".json";
				htmlFilenameBase = "./data/HTML/WebMD/" + board + "/ncj2ey-thread" + Integer.toString(index) + "pg";
				url = element.getElementsByTag("a").first().attr("href");
				threadDoc = getDoc(url);
				
				page = 1;
				duplicate = false;
				while (threadDoc != null) { //for each page
					htmlFilename = htmlFilenameBase + Integer.toString(page) + ".html";
					try {
						saveHtmlFile(threadDoc.toString(),htmlFilename);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					
					try {
						wrapper.parseHTML(htmlFilename);
					} catch (DuplicateThreadException e) {
						wrapper.reset();
						duplicate = true;
						break;
					}

					System.out.println("Thread page: " + wrapper.getNextPage(threadDoc));
					if (wrapper.getNextPage(threadDoc) != "") {
						threadDoc = getDoc(baseURL + wrapper.getNextPage(threadDoc));
						page++;
					} else {
						threadDoc = null;
					}
				}
				
				if(!duplicate) {
					index++;
					//save json file
					wrapper.save2Json(jsonFilename);
					//reset for next thread
					wrapper.reset(); 
				}
			}	
			
			System.out.println("Index page:" + getNextPage(indexDoc));
			if (getNextPage(indexDoc) != "") {
				indexDoc = getDoc(entryPoint + getNextPage(indexDoc));
			} else {
				indexDoc = null;
			}
		}
	}
	
	private Document getDoc(String url) {
		Document doc = null;
		int count = 0;
		int maxTries = 3;
		
		try {
			doc = Jsoup.connect(url).get();
		} catch (IOException e) {
			if(++count == maxTries) {System.err.println(e.getMessage()); e.printStackTrace();}
		}
		return doc;
	}
	
	private String getNextPage(Document doc) {
		String url = "";
		Element pages = doc.getElementsByClass("pages").first();
		Elements links = pages.getElementsByTag("a");
		System.out.println("links: " + links.size());
		for (Element link : links) {
		  System.out.println("link attr: " + link.attr("onclick"));
		  if ("wmdTrack('he-pagenum_next');".equals(link.attr("onclick"))) {
			  System.out.println("HIT");
			  url = link.attr("href");
			  break;
		  }
		}
		
		return url;
	}
	
	private void saveHtmlFile(String htmlContent, String saveLocation) throws IOException {
        FileWriter fileWriter = new FileWriter(saveLocation);
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        bufferedWriter.write(htmlContent.toString());
        bufferedWriter.close();
        System.out.println("Downloading completed successfully!");
    }
	
	public static void main(String[] args) throws DuplicateThreadException {
		Crawler robot = new Crawler();
		robot.start("http://exchanges.webmd.com/organ-transplant-exchange/forum/index",  "organ-transplant-exchange");
	}
}
