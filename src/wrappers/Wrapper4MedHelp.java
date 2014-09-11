/**
 * 
 */
package wrappers;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import structures.Post;

/**
 * @author hongning
 * @version 0.1
 * @category Wrapper
 * sample code for parsing html files from MedHelp forum and extract threaded discussions to json format 
 */
public class Wrapper4MedHelp extends WrapperBase {
	//TODO: You need to extend this wrapper to deal with threaded discussion across multiple pages
	//it looks MedHelp puts all the threaded discussions in a single page
	// 1. avoid any duplication
	// 2. extract the right reply-to relation when across pages
	// 3. clean up the local structures for processing different threaded discussions 
	
	public Wrapper4MedHelp() {
		super();
		
		m_dateParser = new SimpleDateFormat("MMM dd, yyyy");//Date format in this forum: Apr 29, 2008
	}
	
	private Post extractPost(Element elm) {
		Element tmpElmA, tmpElmB;
		Elements tmpElms;
		
		//get post ID
		Post p = new Post(elm.attr("id"));

		//prepare user info and timestamp
		tmpElmA = elm.getElementsByClass("user_info").first();
		
		//extract replyToID
		tmpElmB = tmpElmA.getElementsByClass("post_question_forum_to").first();
		if (tmpElmB != null) {//reply-to relation is available
			Element tmpElm = tmpElmB.getElementsByTag("a").first();
			if (tmpElm != null)
				p.setReplyToID(tmpElm.attr("href"));//NOTE: this is to user ID rather than post ID!!!
			else {
				p.setReplyToID(extractReplyToID(tmpElmB.text()));//NOTE: this is to user ID rather than post ID!!!
			}
		}
		
		tmpElmA = tmpElmA.getElementsByClass("float_fix").first();
		tmpElms = tmpElmA.getElementsByTag("div");
		
		//get author information
		tmpElmA = tmpElms.first();
		tmpElmB = tmpElmA.getElementsByTag("a").first();
		p.setAuthor(tmpElmB.text());
		p.setAuthorID(tmpElmB.attr("href"));	
		
		//get timestamp
		tmpElmA = tmpElms.last();
		try {
			p.setDate(parseDate(tmpElmA.text().trim()));
		} catch (ParseException e) {
			//within a day (granularity is just in day)
			p.setDate(m_dateFormatter.format(new Date()));//use current time
		}
		
		//get post content (no title in MedHelp discussions)
		Element tmpElm = elm.getElementsByClass("KonaBody").first();
		p.setContent(tmpElm.text());
		
		return p;
	}
	
	@Override
	protected boolean parseHTML(Document doc) {
		//extract thread id (URL)
		Elements postElms = doc.getElementsByTag("link");
		for(Element elm:postElms) {
			if (elm.hasAttr("rel") && elm.attr("rel").equals("canonical")) {
				m_threadURL = elm.attr("href");
				break;
			}
		}
		
		postElms = doc.getElementsByClass("post_data");
		//special treatment of first post
		Element elm = postElms.get(0), tmpElmA, tmpElmB;
		Post p = new Post(elm.attr("id"));
		
		//extract thread title
		tmpElmA = doc.getElementsByTag("h1").first();
		m_threadTitle = tmpElmA.text();
		
		//prepare user info and timestamp
		tmpElmA = doc.getElementsByClass("post_info").first();
		tmpElmA = tmpElmA.getElementsByClass("user_info").first();
		
		//get author information
		tmpElmB = tmpElmA.getElementsByTag("a").first();
		p.setAuthor(tmpElmB.text());
		p.setAuthorID(tmpElmB.attr("href"));
		
		//get timestamp
		String text = tmpElmA.text().trim();
		int start = text.indexOf('|');
		text = text.substring(start+1).trim();
		try {
			p.setDate(parseDate(text));
		} catch (ParseException e) {
			//within a day (granularity is just in day)
			p.setDate(m_dateFormatter.format(new Date()));//use current time
		}
		
		//get post content (no title in MedHelp discussions)
		tmpElmA = elm.getElementsByClass("KonaBody").first();
		p.setContent(tmpElmA.text());
		m_posts.add(p);
		
		for(int i=1; i<postElms.size(); i++){
			elm = postElms.get(i);
			if (elm.hasAttr("id") && elm.attr("id").startsWith("post_")) {
				m_posts.add(extractPost(elm));
			}
		}
		return !m_posts.isEmpty();
	}

	@Override
	protected String extractReplyToID(String text) {
		return text.substring(4);//remove 'To: '
	}

	/*
	public static void main(String[] args) {
		Wrapper4MedHelp wrapper = new Wrapper4MedHelp();
		wrapper.parseHTML("./data/HTML/MedHelp/Anxiety/sample.htm");
		wrapper.save2Json("./data/json/MedHelp/Anxiety/sample.json");

	}
	*/

}
