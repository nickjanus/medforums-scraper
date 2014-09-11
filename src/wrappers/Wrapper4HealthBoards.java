/**
 * 
 */
package wrappers;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import structures.Post;

/**
 * @author hongning
 * @version 0.11
 * @category Wrapper
 * sample code for parsing html files from HealthBoards forum and extract threaded discussions to json format 
 */
public class Wrapper4HealthBoards extends WrapperBase {
	//TODO: You need to extend this wrapper to deal with threaded discussion across multiple pages
	// 1. get the right "next page"
	// 2. avoid any duplication
	// 3. extract the right reply-to relation when across pages
	// 4. properly clear up the authorTable for different discussions 
	// 5. clean up the local structures for processing different threaded discussions 
	
	Map<String, String> m_authorTable;//to keep track of users' name to ID mapping, in case some reply-to quote only has author's name 
	
	public Wrapper4HealthBoards() {
		super();
		
		m_dateParser = new SimpleDateFormat("dd-MM-yyyy, hh:mm a");//Date format in this forum: 03-08-2013, 02:49 PM
		m_authorTable = new HashMap<String, String>();
	}

	protected String parseDate(Element dateElm) throws ParseException {
		String text = dateElm.text().trim();
		return super.parseDate(text);
	}
	
	private Post extractPost(Element elm) {
		Element tmpElmA, tmpElmB;
		Elements tmpElms = elm.getElementsByTag("tr");
		
		//get post ID
		Post p = new Post(elm.attr("id"));
		
		//get timestamp of this post
		tmpElmA = tmpElms.first();
		tmpElmB = tmpElmA.getElementsByTag("td").first();
		try {
			p.setDate(parseDate(tmpElmB));
		} catch (ParseException e) {
			System.err.format("[Error]Failed to parse date in %s!\n", p.getID());
			e.printStackTrace();
		}
		
		//get author information
		tmpElmA = tmpElms.get(1); // second row
		tmpElmB = tmpElmA.getElementsByClass("bigusername").first();
		p.setAuthor(tmpElmB.text());
		p.setAuthorID(tmpElmB.attr("href"));	
		if (!m_authorTable.containsKey(p.getAuthor()))
			m_authorTable.put(p.getAuthor(), p.getAuthorID());
		
		//get post content (title is not meaningful in healthboards)
		tmpElmB = elm.getElementById(p.getID().replace("post", "post_message_"));
		p.setContent(tmpElmB.text());
		
		//get replyToID (shall we remove the quotation first?)
		tmpElms = tmpElmA.getElementsByTag("table");
		if (tmpElms!=null && !tmpElms.isEmpty()) {
			tmpElmB = tmpElms.first();
			if (tmpElmB.text().contains("Originally Posted by")) {//double check if this is quotation part
				Element tmpElm = tmpElmB.getElementsByTag("a").first();
				if (tmpElm != null && tmpElm.hasAttr("href"))
					p.setReplyToID(extractReplyToID(tmpElm.attr("href")));
				else {//if there happens to be no link to the author
					tmpElm = tmpElmB.getElementsByTag("strong").first();
					if (tmpElm != null && m_authorTable.containsKey(tmpElm.text())) {
						p.setReplyToID(m_authorTable.get(tmpElm.text()));//get the mapping from our local table
					}//we should be able to recover all the reply-to relation here
				}
			}
		}
		
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
		
		//extract thread title
		postElms = doc.getElementsByAttributeValue("class", "navbar");
		for(Element elm:postElms) {
			Elements elmlist = elm.getElementsByTag("strong");
			if ( elmlist != null && !elmlist.isEmpty()) {
				Element tmpElmA = elmlist.first();
				m_threadTitle = tmpElmA.text();
			}
		}
		
		//extract threaded posts
		postElms = doc.getElementsByTag("table");
		for(Element elm:postElms) {
			if (elm.hasAttr("id") && elm.attr("id").startsWith("post")) {
				m_posts.add(extractPost(elm));
			}
		}
		
		return !m_posts.isEmpty();
	}

	@Override
	protected String extractReplyToID(String text) {
		int start = text.indexOf('#');
		if (start == -1)
			return text;
		else
			return text.substring(start+1);
	}

	/*
	public static void main(String[] args) {
		Wrapper4HealthBoards wrapper = new Wrapper4HealthBoards();
//		wrapper.parseHTML("./data/HTML/healthboards/Arthritis/sample.htm");
//		wrapper.save2Json("./data/json/healthboards/Arthritis/sample.json");
		
		//to test the error case provided by Jack
		wrapper.parseHTML("./data/HTML/healthboards/Stress/error.htm");
		wrapper.save2Json("./data/json/healthboards/Stress/error.json");
	}
	*/
}
