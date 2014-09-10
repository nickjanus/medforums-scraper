/**
 * 
 */
package wrappers;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import structures.Post;


/**
 * @author hongning
 * @version 0.1
 * @category Wrapper
 * sample code for parsing html files from WebMD forum and extract threaded discussions to json format 
 */
public class Wrapper4WebMD extends WrapperBase {
	//TODO: You need to extend this wrapper to deal with threaded discussion across multiple pages
	// 1. get the right "next page"
	// 2. avoid any duplication
	// 3. extract the right reply-to relation when across pages
	// 4. clean up the local structures for processing different threaded discussions 
	
	public Wrapper4WebMD() {
		super();
		
		m_dateParser = new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss 'GMT'Z (z)");//Date format in this forum:  Mon Sep 17 2012 13:47:16 GMT-0400 (EDT)
	}
		
	protected String parseDate(Element dateElm) throws ParseException {
		Element tmpElm = dateElm.getElementsByTag("script").first();//get the detailed time
		
		String text = tmpElm.data();
		int start = text.indexOf("DateDelta") + 11;
		int end = text.indexOf("\'", start);
		return super.parseDate(text.substring(start, end));
	}
	
	@Override
	protected String extractReplyToID(String text) {
		return text.replaceAll("\\?pg=\\d+#", "/");//normlize the replyToID to the corresponding postID
	}

	@Override
	protected boolean parseHTML(Document doc) {
		Elements postElms = doc.getElementsByClass("thread_fmt"), tmpElms;
		Element tmpElmA, tmpElmB;
		
		String firstItemDate = null;
		
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
		}
		
		for(Element elm:postElms){
			//get post ID
			tmpElmA = elm.getElementsByClass("exchange-reply-form").first();
			Post p = new Post(tmpElmA.attr("action"));
			
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
			
			//get author information
			tmpElmA = elm.getElementsByClass("post_hdr_fmt").first();
			tmpElmB = tmpElmA.getElementsByTag("a").first();
			p.setAuthor(tmpElmB.text());
			p.setAuthorID(tmpElmB.attr("href"));	
			
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
		}
		return !m_posts.isEmpty();
	}	
	
	
	public static void main(String[] args) {
		Wrapper4WebMD wrapper = new Wrapper4WebMD();
		wrapper.parseHTML("./data/HTML/WebMD/Allergies/sample.htm");
		wrapper.save2Json("./data/json/WebMD/Allergies/sample.json");
	}
}
