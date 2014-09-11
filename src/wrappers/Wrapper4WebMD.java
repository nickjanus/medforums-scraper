/**
 * 
 */
package wrappers;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import crawler.DuplicateThreadException;
import structures.Post;

/**
 * @author hongning
 * @version 0.1
 * @category Wrapper
 * sample code for parsing html files from WebMD forum and extract threaded discussions to json format 
 */
public class Wrapper4WebMD extends WrapperBase {
	//TODO: You need to extend this wrapper to deal with threaded discussion across multiple pages
	// 1. get the right "next page" - done
	// 2. avoid any duplication - done
	// 3. extract the right reply-to relation when across pages - done
	// 4. clean up the local structures for processing different threaded discussions - done
	Hashtable<String, String> replyTable = new Hashtable<String, String>(); //key: post id, val: page relative url
	Set<String> threadsVisited = new HashSet<String>();
	int page = 0; //incremented at top of parse function
	
	public Wrapper4WebMD() {
		super();
		
		//Date format in this forum:  Mon Sep 17 2012 13:47:16 GMT-0400 (EDT)
		m_dateParser = new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss 'GMT'Z (z)");
	}
		
	protected String parseDate(Element dateElm) throws ParseException {
		Element tmpElm = dateElm.getElementsByTag("script").first();//get the detailed time
		
		String text = tmpElm.data();
		int start = text.indexOf("DateDelta") + 11;
		int end = text.indexOf("\'", start);
		return super.parseDate(text.substring(start, end));
	}

	//reset after a thread is finished
	public void reset() {
		m_posts.clear();
		page = 0;
		replyTable.clear();
		m_threadTitle = "";
		m_threadURL = "";
	}
	
	public String getNextPage(Document doc) {
		String url = "";
		Element pages = doc.getElementsByClass("pages").first();
		Elements links = pages.getElementsByTag("a");
		for (Element link : links) {
		  if ("ctrs('srb-tpage_next');".equals(link.attr("onclick"))) {
			  url = m_threadURL + link.attr("href");
			  break;
		  }
		}
		
		return url;
	}
	
	@Override
	protected String extractReplyToID(String text) {
		return replyTable.get(text); //exchange the replyTo url with the corresponding post url
	}

	@Override 
	protected boolean parseHTML(Document doc) throws DuplicateThreadException {
		int index = 0;
		Elements postElms = doc.getElementsByClass("thread_fmt"), tmpElms;
		Element tmpElmA, tmpElmB;
		
		String firstItemDate = null;
		page++;
		//get thread information
		if (m_posts.isEmpty()) {
			tmpElmA = doc.getElementsByClass("firstitem_mid_fmt").first();
			
			//extract timestamp
			tmpElmB = tmpElmA.getElementsByClass("first_posted_fmt").first();
			try {
				firstItemDate = parseDate(tmpElmB);
			} catch (ParseException e) {
				System.err.println("[Error]Failed to parse date for the first post!\n");
				e.printStackTrace();
			}
			
			//extract discussion title
			tmpElmB = tmpElmA.getElementsByClass("first_item_title_fmt").first();
			m_threadTitle = tmpElmB.text();
			
			//extract thread ID
			tmpElmB = tmpElmA.getElementsByClass("exchange-reply-form").first();
			m_threadURL = tmpElmB.attr("action");
			if (threadsVisited.contains(m_threadURL)) {throw new DuplicateThreadException();}
			threadsVisited.add(m_threadURL);
		}
		for(Element elm:postElms){
			
			//get post ID
			tmpElmA = elm.getElementsByClass("exchange-reply-form").first();
			Post p = new Post(tmpElmA.attr("action"));
			
			//add entry to reply table, postID => postID by page
			replyTable.put((m_threadURL + "?pg=" + Integer.toString(page) + "#" + Integer.toString(index)), p.getID());
			
			//get timestamp of this post
			tmpElmA = elm.getElementsByClass("posted_fmt").first();	
			if (tmpElmA != null) {//a regular discussion post, otherwise it is the first post
				try {
					p.setDate(parseDate(tmpElmA));
				} catch (ParseException e) {
					System.err.format("[Error]Failed to parse date in %s!\n", p.getID());
					e.printStackTrace();
					continue; // discard this post
				}
			} else if (m_posts.isEmpty()){ // ignore the re-occurrence of first post hereafter				
				p.setDate(firstItemDate);
			}
			
			//TODO use reply-to element for properly parsing anon authors, responders, extra credit?
			//get author information
			tmpElmA = elm.getElementsByClass("post_hdr_fmt").first();
		      tmpElmB = tmpElmA.getElementsByTag("a").first();
		      if (tmpElmB != null) {
		        p.setAuthor(tmpElmB.text());
		        p.setAuthorID(tmpElmB.attr("href"));
		      } else {
		        /*
		         * In the anonymous case, author names are 
		         * everything before the first space.
		         */
		        String postAuthor =  tmpElmA.text().replaceAll(" .+$","");
		        p.setAuthor(postAuthor);
		        p.setAuthorID(postAuthor);
		        //System.out.println("Author: " + postAuthor);
		      }	
			
			//get reply-to
			tmpElms = tmpElmA.getElementsByClass("mlResponseTo");
			if (tmpElms != null && !tmpElms.isEmpty()){
				tmpElmA = tmpElms.first();
				tmpElmB = tmpElmA.getElementsByTag("a").last();
				p.setReplyToID(extractReplyToID(tmpElmB.attr("href")));
			}
			
			//get post content (no title in WebMD)
			tmpElmA = elm.getElementsByClass("post_fmt").first();		
			tmpElms = tmpElmA.getElementsByClass("post_sig_fmt");
			if (tmpElms != null && !tmpElms.isEmpty()) {//detach the signature part 
				tmpElmB = tmpElms.first();
				tmpElmB.remove();
			}
			p.setContent(tmpElmA.text());
			
			m_posts.add(p);		
			index++;
		}
		return !m_posts.isEmpty();
	}	
	
	
	public static void main(String[] args) throws DuplicateThreadException {
		Wrapper4WebMD wrapper = new Wrapper4WebMD();
		wrapper.parseHTML("./data/HTML/WebMD/Allergies/sample.htm");
		wrapper.save2Json("./data/json/WebMD/Allergies/sample.json");
	}
}
