/**
 * 
 */
package analyzer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;

import json.JSONArray;
import json.JSONException;
import json.JSONObject;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;
import org.tartarus.snowball.ext.porterStemmer;

import structures.Post;

/**
 * @author hongning
 * Sample codes for demonstrating OpenNLP package usage 
 */
public class DocAnalyzer {
	
	String m_threadURL;
	String m_threadTitle;
	ArrayList<JSONObject> m_threads;
	
	SimpleDateFormat m_dateFormatter;
	
	//to store existing post IDs for checking the replyTo relation
	HashSet<String> m_existingPostID;
	
	public DocAnalyzer() {
		m_threads = new ArrayList<JSONObject>();
		m_dateFormatter = new SimpleDateFormat("yyyyMMdd-HH:mm:ss Z");//standard date format for this project
	}
	
	//check if students' crawled json files follows instruction
	//if you decide to use the sample code for your homework assignment, this is the place you can perform tokenization/stemming/word counting
	public void AnalyzeThreadedDiscussion(JSONObject json) {		
		try {
			json.getString("title");
			json.getString("URL");
			
			JSONArray jarray = json.getJSONArray("thread");
			m_existingPostID = new HashSet<String>(); 
			for(int i=0; i<jarray.length(); i++) {
				checkPostFormat(new Post(jarray.getJSONObject(i)));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		//store this into memory for later use
		m_threads.add(json);
	}
	
	//check format for each post
	private void checkPostFormat(Post p) {
		if (p.getID() == null)
			System.err.println("[Error]Missing postID!");
		else if (p.getAuthor() == null)
			System.err.format("[Error]Missing author name in %s!\n", p.getID());
		else if (p.getAuthorID() == null)
			System.err.format("[Error]Missing author ID in %s!\n", p.getID());
		else if (p.getDate() == null)
			System.err.format("[Error]Missing post date in %s!\n", p.getID());
		else if (p.getContent() == null)
			System.err.format("[Error]Missing post content in %s!\n", p.getID());
		else {
			//hard to check!!!
			//register the post ID
//			m_existingPostID.add(p.getID());
//			m_existingPostID.add(p.getAuthorID());//might also be pointing to authors
//			m_existingPostID.add(p.getAuthor());//might also be pointing to authors
//			
//			if (p.getReplyToID()!=null 
//					&& !m_existingPostID.contains(p.getReplyToID()) ) {
//				System.err.format("[Error]Incorrect replyTO post ID '%s' in %s!\n", p.getReplyToID(), p.getID());
//			}
			
			//to check if the date format is correct
			try {
				m_dateFormatter.parse(p.getDate());
			} catch (ParseException e) {
				System.err.format("[Error]Wrong date format '%s' in %s\n!", p.getDate(), p.getID());
			}
		}
	}
	
	//sample code for loading the json file
	public JSONObject LoadJson(String filename) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
			StringBuffer buffer = new StringBuffer(1024);
			String line;
			
			while((line=reader.readLine())!=null) {
				buffer.append(line);
			}
			reader.close();
			
			return new JSONObject(buffer.toString());
		} catch (IOException e) {
			System.err.format("[Error]Failed to open file %s!", filename);
			e.printStackTrace();
			return null;
		} catch (JSONException e) {
			System.err.format("[Error]Failed to parse json file %s!", filename);
			e.printStackTrace();
			return null;
		}
	}
	
	public void LoadDirectory(String folder, String suffix) {
		File dir = new File(folder);
		int size = m_threads.size();
		for (File f : dir.listFiles()) {
			if (f.isFile() && f.getName().endsWith(suffix)){
				AnalyzeThreadedDiscussion(LoadJson(f.getAbsolutePath()));
			}
			else if (f.isDirectory())
				LoadDirectory(f.getAbsolutePath(), suffix);
		}
		size = m_threads.size() - size;
		System.out.println("Loading " + size + " json files from " + folder);
	}

	//sample code for demonstrating how to use Snowball stemmer
	public String SnowballStemmingDemo(String token) {
		SnowballStemmer stemmer = new englishStemmer();
		stemmer.setCurrent(token);
		if (stemmer.stem())
			return stemmer.getCurrent();
		else
			return token;
	}
	
	//sample code for demonstrating how to use Porter stemmer
	public String PorterStemmingDemo(String token) {
		porterStemmer stemmer = new porterStemmer();
		stemmer.setCurrent(token);
		if (stemmer.stem())
			return stemmer.getCurrent();
		else
			return token;
	}
	
	public void TokenizerDemon(String text) {
		try {
			Tokenizer tokenizer = new TokenizerME(new TokenizerModel(new FileInputStream("./data/Model/en-token.bin")));
			
			System.out.format("Token\tSnonball Stemmer\tPorter Stemmer\n");
			for(String token:tokenizer.tokenize(text)){
				System.out.format("%s\t%s\t%s\n", token, SnowballStemmingDemo(token), PorterStemmingDemo(token));
			}
		}
		catch (IOException e) {
		  e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		DocAnalyzer analyzer = new DocAnalyzer();
		
		//codes for demonstrating tokenization and stemming
		//analyzer.TokenizerDemon("I've practiced for 30 years in pediatrics, and I've never seen anything quite like this.");
		
		//codes for loading json file
		//analyzer.AnalyzeThreadedDiscussion(analyzer.LoadJson("./data/json/MedHelp/Anxiety/sample.json"));
		
		//when we want to execute it in command line
		analyzer.LoadDirectory(args[0], args[1]);
	}

}
