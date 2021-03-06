package com.v2ex.v2droid;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class HtmlParser {

	static final String KEY_ID = "id";
	static final String KEY_TITLE = "title";
	static final String KEY_REPLIES = "replies";
	static final String KEY_USERNAME = "username";
	static final String KEY_AVATAR = "avatar";
	static final String KEY_NODE = "node";

	static final String KEY_HEADER_ID = "header_id";
	static final String KEY_HEADER = "header";
	static final String KEY_NAME = "name";
	static final String KEY_LINK = "link";

	// constructor
	public HtmlParser() {

	}

	/**
	 * 根据URL获得所有的html信息
	 * 
	 * @param url
	 * @return
	 */
	public static String getHtmlByUrl(String url) {
		String html = null;
		HttpClient httpClient = new DefaultHttpClient();// 创建httpClient对象
		HttpGet httpget = new HttpGet(url);// 以get方式请求该URL
		try {
			HttpResponse responce = httpClient.execute(httpget);// 得到responce对象
			int resStatu = responce.getStatusLine().getStatusCode();// 返回码
			if (resStatu == HttpStatus.SC_OK) {// 200正常 其他就不对
				// 获得相应实体
				HttpEntity entity = responce.getEntity();
				if (entity != null) {
					html = EntityUtils.toString(entity);// 获得html源代码
				}
			}
		} catch (Exception e) {
			System.out.println("访问[" + url + "]出现异常!");
			e.printStackTrace();
		} finally {
			httpClient.getConnectionManager().shutdown();
		}
		return html;
	}

	public static ArrayList<HashMap<String, String>> getTopics(String url,
			ArrayList<HashMap<String, String>> topics) {
		try {
			// String html = getHtmlByUrl(url);
			// Document doc = Jsoup.parse(html);
			Document doc = Jsoup.connect(url).get();
			Elements items = doc.select("div[class=cell item]");

			if (!items.isEmpty() && !topics.isEmpty()) {
				topics.remove(topics.size() - 1);
			}

			for (Element item : items) {
				//System.out.println("item======>" + item.toString());
				Element titleElement = item.select("span[class=item_title]>a")
						.get(0);
				String href = titleElement.attr("href");
				String id = getMatcher("/t/([\\d]+)", href);
				String replies = getMatcher("#reply([\\d]+)", href);
				String title = titleElement.text();
				Element usernameElement = item.select("td>a").get(0);
				String href2 = usernameElement.attr("href");
				String username = getMatcher("/member/([0-9a-zA-Z]+)", href2);
				Element avatarElement = usernameElement.select("img").get(0);
				String avatar = avatarElement.attr("src");
				Element nodeElement = item.select("span[class=small fade]>a")
						.get(0);
				String node = nodeElement.text();
				System.out.println(node);

				// creating new HashMap
				HashMap<String, String> map = new HashMap<String, String>();

				// adding each child node to HashMap key =>
				// value
				map.put(KEY_ID, id);
				map.put(KEY_TITLE, title);
				map.put(KEY_USERNAME, username);
				map.put(KEY_REPLIES, replies);
				map.put(KEY_AVATAR, avatar);
				map.put(KEY_NODE, node);

				// adding HashList to ArrayList
				topics.add(map);
			}
		} catch (IOException e) {
			System.out.println("访问[" + url + "]出现异常!");
			e.printStackTrace();
		}

		HashMap<String, String> mapMore = new HashMap<String, String>();

		mapMore.put(KEY_ID, MainActivity.MORE_TAG);
		mapMore.put(KEY_TITLE, MainActivity.MORE_TAG);
		mapMore.put(KEY_USERNAME, MainActivity.MORE_TAG);
		mapMore.put(KEY_REPLIES, MainActivity.MORE_TAG);
		mapMore.put(KEY_AVATAR, MainActivity.MORE_TAG);
		mapMore.put(KEY_NODE, MainActivity.MORE_TAG);

		// adding HashList to ArrayList
		topics.add(mapMore);

		return topics;
	}

	public static ArrayList<HashMap<String, String>> getNodes(String url,
			ArrayList<HashMap<String, String>> nodes) {
		try {
			// String html = getHtmlByUrl(url);
			// Document doc = Jsoup.parse(html);
			Document doc = Jsoup.connect(url).get();
			Elements items = doc.select("div#Wrapper")
					.select("div[class=content]").select("div[class=box]")
					.select("table");

			int headerId = 0;
			int nodeId = 0;

			for (Element item : items) {
				Elements headerElements = item.select("span[class=fade");
				if (headerElements.isEmpty()) {
					continue;
				}

				Element headerElement = headerElements.get(0);

				Elements nodeItems = item.select("a");
				for (Element nodeItem : nodeItems) {
					HashMap<String, String> map = new HashMap<String, String>();

					map.put(KEY_ID, Integer.toString(nodeId));
					map.put(KEY_HEADER_ID, Integer.toString(headerId));
					map.put(KEY_HEADER, headerElement.text());
					map.put(KEY_NAME, nodeItem.text());
					map.put(KEY_LINK, nodeItem.attr("href"));
					nodes.add(map);

					nodeId++;
				}

				headerId++;

				System.out.println("table===> " + headerElement.toString());
			}
		} catch (IOException e) {
			System.out.println("访问[" + url + "]出现异常!");
			e.printStackTrace();
		}

		return nodes;
	}
	
	public static ArrayList<HashMap<String, String>> getNodeTopics(String url, String nodeName,
			ArrayList<HashMap<String, String>> topics) {
		//try {
			 String html = getHtmlByUrl(url);
			 System.out.println("getHtmlByUrl======>" + html);
			 Document doc = Jsoup.parse(html);
			//Document doc = Jsoup.connect(url).get();
			Elements items = doc.select("div#TopicsNode").select("table");
			System.out.println("item======>" + items.toString());
			if (!items.isEmpty() && !topics.isEmpty()) {
				topics.remove(topics.size() - 1);
			}

			for (Element item : items) {
				System.out.println("item======>" + item.toString());
				Element titleElement = item.select("span[class=item_title]>a")
						.get(0);
				String href = titleElement.attr("href");
				System.out.println("href======>" + href);
				String id = getMatcher("/t/([\\d]+)", href);
				System.out.println("id======>" + id);
				String replies = getMatcher("#reply([\\d]+)", href);
				System.out.println("replies======>" + replies);
				String title = titleElement.text();
				System.out.println("title======>" + title);
				Element usernameElement = item.select("td>a").get(0);
				String href2 = usernameElement.attr("href");
				System.out.println("href2======>" + href2);
				String username = getMatcher("/member/([0-9a-zA-Z]+)", href2);
				System.out.println("username======>" + username);
				Element avatarElement = usernameElement.select("img").get(0);
				String avatar = avatarElement.attr("src");
				System.out.println("avatar======>" + avatar);

				// creating new HashMap
				HashMap<String, String> map = new HashMap<String, String>();

				// adding each child node to HashMap key =>
				// value
				map.put(KEY_ID, id);
				map.put(KEY_TITLE, title);
				map.put(KEY_USERNAME, username);
				map.put(KEY_REPLIES, replies);
				map.put(KEY_AVATAR, avatar);
				map.put(KEY_NODE, nodeName);

				// adding HashList to ArrayList
				topics.add(map);
			}
		//} catch (IOException e) {
		//	System.out.println("访问[" + url + "]出现异常!");
		//	e.printStackTrace();
		//}

		HashMap<String, String> mapMore = new HashMap<String, String>();

		mapMore.put(KEY_ID, MainActivity.MORE_TAG);
		mapMore.put(KEY_TITLE, MainActivity.MORE_TAG);
		mapMore.put(KEY_USERNAME, MainActivity.MORE_TAG);
		mapMore.put(KEY_REPLIES, MainActivity.MORE_TAG);
		mapMore.put(KEY_AVATAR, MainActivity.MORE_TAG);
		mapMore.put(KEY_NODE, MainActivity.MORE_TAG);

		// adding HashList to ArrayList
		topics.add(mapMore);

		return topics;
	}

	public static String getMatcher(String regex, String source) {
		String result = "";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(source);
		while (matcher.find()) {
			result = matcher.group(1);// 只取第一组
		}
		return result;
	}

	public static String getTopicOnce(String url) {
		System.out.println("getTopicOnce======>");
		String once = "";

		String html = getHtmlByUrl(url);
		System.out.println("html======>" + html);
		Document doc = Jsoup.parse(html);

		Element item = doc.select("input[name=once]").first();
		if (item != null) {
			once = item.attr("value");
			System.out.println("once======>" + item.attr("value"));
		}

		return once;
	}

	public static String getTopicOnce2(String html) {
		System.out.println("getTopicOnce======>");
		String once = "";

		// System.out.println("html======>" + html);
		Document doc = Jsoup.parse(html);

		Element item = doc.select("input[name=once]").first();
		if (item != null) {
			once = item.attr("value");
			System.out.println("once======>" + item.attr("value"));
		}

		return once;
	}

}