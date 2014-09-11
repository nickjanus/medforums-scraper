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
 * sample code for parsing html files from eHealth forum (http://ehealthforum.com/health/health_forums.html)
 * and extract threaded discussions to json format 
 */
public class Wrapper4eHealth extends WrapperBase {
	
	//TODO: You need to extend this wrapper to deal with threaded discussion across multiple pages
	// 1. get the right "next page"
	// 2. avoid any duplication
	// 3. extract the right reply-to relation when across pages
	// 4. clean up the local structures for processing different threaded discussions 
	
	public Wrapper4eHealth() {
		super();
		
		m_dateParser = new SimpleDateFormat("MMMM dd, yyyy");//Date format in this forum: April 20th, 2012
	}
	
	protected String parseDate(Element dateElm) throws ParseException {
		String text = dateElm.text();
		if (text.contains("replied "))
			text = text.substring(8);		
		
		//decode the descriptive date [st, nd, rd, th]
		if (text.contains("st,"))
			text = text.replace("st,", ",");
		else if (text.contains("nd,"))
			text = text.replace("nd,", ",");
		else if (text.contains("rd,"))
			text = text.replace("rd,", ",");
		else if (text.contains("th,"))
			text = text.replace("th,", ",");
		
		return super.parseDate(text);
	}
	
	private Post extractPost(Element elm) {
		Elements tmpElms = elm.getElementsByAttributeValue("name", "quick_reply_input");
		if (tmpElms==null || !tmpElms.isEmpty())
			return null;
		
		Element tmpElmA, tmpElmB;
		
		//get post ID
		tmpElmA = elm.getElementsByClass("vt_usefull_post_form_holder").first();
		tmpElmB = tmpElmA.getElementsByTag("input").first();
		
		Post p = new Post(tmpElmB.attr("id"));
		
		//get timestamp of this post
		tmpElms = elm.getElementsByClass("vt_reply_timestamp");			
		if (tmpElms != null && !tmpElms.isEmpty()) {
			try {
				p.setDate(parseDate(tmpElms.first()));
				
				//get replyToID
				p.setReplyToID(extractReplyToID(elm.attr("style")));
				p.setLevel(extractLevel(elm.attr("style")));
			} catch (ParseException e) {
				System.err.format("[Error]Failed to parse date in %s!\n", p.getID());
				e.printStackTrace();
			} 
		} else {// no re-occurrence of first post in eHealth
			tmpElmA = elm.getElementsByClass("vt_first_timestamp").first();
			try {
				p.setDate(parseDate(tmpElmA));
			} catch (ParseException e) {
				System.err.println("[Error]Failed to parse date for the first post!\n");
				e.printStackTrace();
			}
		}
		
		//get author information
		tmpElmA = elm.getElementsByClass("vt_asked_by_user").first();
		tmpElmB = tmpElmA.getElementsByTag("a").first();
		p.setAuthor(tmpElmB.text());
		p.setAuthorID(tmpElmB.attr("href"));	
		
		//get post title
		tmpElmA = elm.getElementsByClass("vt_message_subject").first();
		if (tmpElmA != null) // first post's title is thread's title
			p.setTitle(tmpElmA.text());
		
		//get post content
		tmpElmA = elm.getElementsByClass("vt_post_body").first();
		p.setContent(tmpElmA.text());
		
		return p;
	}
	
	private int extractLevel(String text){
		int start = 13, end = text.indexOf("px;", start);//fixed format for padding, starts with "padding-left:"
		if (end==start)
			return 0;
		else
			return Integer.valueOf(text.substring(start, end))/50;		
	}

	@Override
	protected boolean parseHTML(Document doc) {		
		Element tmpElm;	
		Post p;
		
		//extract discussion title
		tmpElm = doc.getElementById("page_h1");
		m_threadTitle = tmpElm.text();	
		
		//extract thread id (URL)
		Elements postElms = doc.getElementsByTag("link");
		for(Element elm:postElms) {
			if (elm.hasAttr("rel") && elm.attr("rel").equals("alternate")) {
				m_threadURL = elm.attr("href").replace("//m.ehealth", "//ehealth");
				break;
			}
		}
				
		//extract first post
		tmpElm = doc.getElementsByClass("vt_first_postrow").first();
		p = extractPost(tmpElm);
		if (p!=null)
			m_posts.add(p);		
		
		//all the replies
		postElms = doc.getElementsByClass("vt_postrow_rest");
		for(Element elm:postElms){
			p = extractPost(elm);
			
			if (p!=null)
				m_posts.add(p);
		}
		
		return !m_posts.isEmpty();
	}

	@Override
	protected String extractReplyToID(String text) {
		int level = extractLevel(text);
		if (level == 0) 
			return m_posts.get(0).getID();
		else {
			Post p = null;
			for(int i=m_posts.size()-1; i>=0; i--) {
				p = m_posts.get(i);
				if (p.getLevel()==level-1)
					break;
			}
			return p.getID();
		}
	}

	/*
	public static void main(String[] args) {
		Wrapper4eHealth wrapper = new Wrapper4eHealth();
		wrapper.parseHTML("./data/HTML/eHealth/Cancer/sample.htm");
		wrapper.save2Json("./data/json/eHealth/Cancer/sample.json");
	}
	*/

}
