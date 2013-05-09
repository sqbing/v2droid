package com.v2ex.v2droid;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie2;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.loopj.android.http.PersistentCookieStore;

public class ApiClient {

	public static final String UTF_8 = "UTF-8";
	public static final String DESC = "descend";
	public static final String ASC = "ascend";

	private final static int TIMEOUT_CONNECTION = 20000;
	private final static int TIMEOUT_SOCKET = 20000;
	private final static int RETRY_TIME = 1;

	private static String appUserAgent;

	static final String KEY_ID = "id";
	static final String KEY_TITLE = "title";
	static final String KEY_REPLIES = "replies";
	static final String KEY_USERNAME = "username";
	static final String KEY_AVATAR = "avatar";
	static final String KEY_NODE = "node";
	static final String KEY_TIME = "time";
	static final String KEY_REPLY = "reply";
	static final String KEY_MESSAGE = "message";

	static final String KEY_HEADER_ID = "header_id";
	static final String KEY_HEADER = "header";
	static final String KEY_NAME = "name";
	static final String KEY_LINK = "link";

	static final String KEY_GRAY = "gray";

	static final String KEY_INFO = "info";
	static final String KEY_CONTENT = "content";
	static final String KEY_FAVORITE = "favorite";
	static final String KEY_FLOOR = "floor";
	static final String KEY_ONCE = "once";

	private static Map<String, String> mCookies = new HashMap<String, String>();

	private static String getUserAgent(AppContext appContext) {
		if (appUserAgent == null || appUserAgent == "") {
			StringBuilder ua = new StringBuilder("V2EX.COM");
			ua.append('/' + appContext.getPackageInfo().versionName + '_'
					+ appContext.getPackageInfo().versionCode);// App版本
			ua.append("/Android");// 手机系统平台
			ua.append("/" + android.os.Build.VERSION.RELEASE);// 手机系统版本
			ua.append("/" + android.os.Build.MODEL); // 手机型号
			ua.append("/" + appContext.getAppId());// 客户端唯一标识
			appUserAgent = ua.toString();
		}
		return appUserAgent;
	}

	private static Map<String, String> getCookies(AppContext appContext) {
		if (mCookies.isEmpty() && appContext.getLogin()) {
			PersistentCookieStore pcs = new PersistentCookieStore(appContext);
			List<Cookie> cookieList = pcs.getCookies();
			for (Cookie cookie : cookieList) {
				// cookieStore.addCookie(cookie);
				mCookies.put(cookie.getName(), cookie.getValue());
				System.out.println("getCookies=====>" + cookie.getName());
			}
		}
		return mCookies;
	}

	public static void storeCookies(AppContext appContext) {
		PersistentCookieStore pcs = new PersistentCookieStore(appContext);
		Map<String, String> cookies = getCookies(appContext);

		for (Entry<String, String> cookie : cookies.entrySet()) {
			Cookie c = new BasicClientCookie2(cookie.getKey(),
					cookie.getValue());
			pcs.addCookie(c);
			System.out.println("storeCookies=====>" + cookie.getKey());

		}
	}

	public static Document get(AppContext appContext, String url,
			String referrer) throws IOException {
		Map<String, String> cookies = getCookies(appContext);
		String userAgent = getUserAgent(appContext);

		Connection connection = Jsoup.connect(url).cookies(cookies)
				.referrer(referrer).userAgent(userAgent);

		Response response = connection.execute();
		cookies.putAll(response.cookies());
		mCookies = cookies;
		return response.parse();
	}

	public static Response post(AppContext appContext, String url,
			String referrer, List<NameValuePair> params) throws IOException {
		Map<String, String> cookies = getCookies(appContext);
		String userAgent = getUserAgent(appContext);

		Connection connection = Jsoup.connect(url).cookies(cookies)
				.referrer(referrer).userAgent(userAgent).method(Method.POST);

		for (NameValuePair param : params) {
			connection = connection.data(param.getName(), param.getValue());
		}

		Response response = connection.execute();

		cookies.putAll(response.cookies());
		mCookies = cookies;
		return response;
	}

	public static boolean login(AppContext appContext, String username,
			String password) throws IOException {
		String once = getOnce(get(appContext, URLs.LOGIN_VALIDATE_HTTP,
				URLs.HOST));

		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("next", "/"));
		params.add(new BasicNameValuePair("u", username));
		params.add(new BasicNameValuePair("p", password));
		params.add(new BasicNameValuePair("once", once));
		params.add(new BasicNameValuePair("next", "/"));

		post(appContext, URLs.LOGIN_VALIDATE_HTTP, URLs.LOGIN_VALIDATE_HTTP,
				params);

		Map<String, String> cookies = getCookies(appContext);

		if (cookies.containsKey("auth")) {
			return true;
		}

		return false;
	}

	public static Response newTopic(AppContext appContext, String url,
			String title, String content) throws IOException {
		String once = getOnce(get(appContext, url, URLs.HOST));

		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("title", title));
		params.add(new BasicNameValuePair("content", content));
		params.add(new BasicNameValuePair("once", once));

		Response response = post(appContext, url, url, params);

		System.out.println("newTopic response=====>" + response.statusCode());

		return response;
	}

	public static Response reply(AppContext appContext, String url,
			String content, String once) throws IOException {

		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("content", content));
		params.add(new BasicNameValuePair("once", once));

		Response response = post(appContext, url, url, params);

		System.out.println("reply response=====>" + response.statusCode());

		return response;
	}

	public static String getOnce(Document doc) {
		String once = "";

		Element item = doc.select("input[name=once]").first();
		if (item != null) {
			once = item.attr("value");
			System.out.println("once======>" + item.attr("value"));
		}

		return once;
	}

	public static ArrayList<HashMap<String, String>> getTopics(
			AppContext appContext, Document doc,
			ArrayList<HashMap<String, String>> topics) {

		Elements items = doc.select("div[class=cell item]");

		if (!topics.isEmpty()) {
			topics.remove(topics.size() - 1);
		}

		for (Element item : items) {
			// System.out.println("item======>" + item.toString());
			Element titleElement = item.select("span[class=item_title]>a").get(
					0);
			String href = titleElement.attr("href");
			String id = getMatcher("/t/([\\d]+)", href);
			String replies = getMatcher("#reply([\\d]+)", href);
			// System.out.println("replies======>" + replies);
			String title = titleElement.text();
			Element usernameElement = item.select("td>a").get(0);
			String href2 = usernameElement.attr("href");
			String username = getMatcher("/member/([0-9a-zA-Z]+)", href2);
			Element avatarElement = usernameElement.select("img").get(0);
			String avatar = avatarElement.attr("src");
			Element nodeElement = item.select("span[class=small fade]>a")
					.get(0);
			String node = nodeElement.text();
			// System.out.println(node);
			Element timeElement = item.select("span[class=small fade]").get(1);
			// System.out.println("t=>" + timeElement.text());
			String time = timeElement.text();
			// System.out.println("time=>" + time);
			/*
			 * Elements links =
			 * item.select("span[class=small fade]").select("a"); String reply =
			 * ""; if (links.size() == 3) { Element replyElement = links.get(2);
			 * reply = replyElement.text();
			 * System.out.println("replyElement======>" + replyElement.text());
			 * }
			 */

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
			map.put(KEY_TIME, time);
			/*
			 * //map.put(KEY_REPLY, reply); if (links.size() == 3) {
			 * map.put(KEY_REPLY, reply); }
			 */

			// adding HashList to ArrayList
			topics.add(map);
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

	public static ArrayList<HashMap<String, String>> getFavorites(
			AppContext appContext, Document doc,
			ArrayList<HashMap<String, String>> topics) {

		Elements items = doc.select("div[class=cell item]");

		if (!topics.isEmpty()) {
			topics.remove(topics.size() - 1);
		}

		for (Element item : items) {
			// System.out.println("item======>" + item.toString());
			Element titleElement = item.select("span[class=item_title]>a").get(
					0);
			String href = titleElement.attr("href");
			String id = getMatcher("/t/([\\d]+)", href);
			String replies = getMatcher("#reply([\\d]+)", href);
			System.out.println("replies======>" + replies);
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
			Element timeElement = item.select("span[class=small fade]").get(0);
			System.out.println("t=>" + timeElement.text());
			String time = timeElement.text();
			System.out.println("time=>" + time);
			/*
			 * Elements links =
			 * item.select("span[class=small fade]").select("a"); String reply =
			 * ""; if (links.size() == 3) { Element replyElement = links.get(2);
			 * reply = replyElement.text();
			 * System.out.println("replyElement======>" + replyElement.text());
			 * }
			 */

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
			map.put(KEY_TIME, time);
			/*
			 * //map.put(KEY_REPLY, reply); if (links.size() == 3) {
			 * map.put(KEY_REPLY, reply); }
			 */

			// adding HashList to ArrayList
			topics.add(map);
		}

		HashMap<String, String> mapMore = new HashMap<String, String>();

		mapMore.put(KEY_ID, MainActivity.MORE_TAG);
		mapMore.put(KEY_USERNAME, MainActivity.MORE_TAG);
		mapMore.put(KEY_REPLIES, MainActivity.MORE_TAG);
		mapMore.put(KEY_AVATAR, MainActivity.MORE_TAG);
		mapMore.put(KEY_NODE, MainActivity.MORE_TAG);

		if (items.isEmpty() && topics.isEmpty()) {
			mapMore.put(KEY_TITLE, "您目前尚未收藏任何主题");
		} else {
			mapMore.put(KEY_TITLE, MainActivity.MORE_TAG);
		}

		// adding HashList to ArrayList
		topics.add(mapMore);

		return topics;
	}

	public static ArrayList<HashMap<String, String>> getUserTopics(
			AppContext appContext, Document doc,
			ArrayList<HashMap<String, String>> topics, String avatar) {

		Elements items = doc.select("div[class=cell item]");

		if (!topics.isEmpty()) {
			topics.remove(topics.size() - 1);
		}

		for (Element item : items) {
			// System.out.println("item======>" + item.toString());
			Element titleElement = item.select("span[class=item_title]>a").get(
					0);
			String href = titleElement.attr("href");
			String id = getMatcher("/t/([\\d]+)", href);
			String replies = getMatcher("#reply([\\d]+)", href);
			// System.out.println("replies======>" + replies);
			String title = titleElement.text();
			Element usernameElement = item.select("span[class=small fade]")
					.select("a").get(1);
			String href2 = usernameElement.attr("href");
			String username = getMatcher("/member/([0-9a-zA-Z]+)", href2);

			Element nodeElement = item.select("span[class=small fade]>a")
					.get(0);
			String node = nodeElement.text();
			// System.out.println(node);
			Element timeElement = item.select("span[class=small fade]").get(0);
			// System.out.println("t=>" + timeElement.text());
			String time = timeElement.text();
			// System.out.println("time=>" + time);
			String[] s = time.split("\u00a0");
			time = "";
			for (int i = 4; i < s.length; i++) {
				// System.out.println(s[i]);
				time += s[i];
			}

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
			map.put(KEY_TIME, time);

			// adding HashList to ArrayList
			topics.add(map);
		}

		HashMap<String, String> mapMore = new HashMap<String, String>();

		mapMore.put(KEY_ID, MainActivity.MORE_TAG);
		mapMore.put(KEY_USERNAME, MainActivity.MORE_TAG);
		mapMore.put(KEY_REPLIES, MainActivity.MORE_TAG);
		mapMore.put(KEY_AVATAR, MainActivity.MORE_TAG);
		mapMore.put(KEY_NODE, MainActivity.MORE_TAG);

		if (items.isEmpty() && topics.isEmpty()) {
			mapMore.put(KEY_TITLE, "您目前尚未收藏任何主题");
		} else {
			mapMore.put(KEY_TITLE, MainActivity.MORE_TAG);
		}

		// adding HashList to ArrayList
		topics.add(mapMore);

		return topics;
	}

	public static ArrayList<HashMap<String, String>> getUserReplies(
			AppContext appContext, Document doc,
			ArrayList<HashMap<String, String>> replies) {
		System.out.println("getUserReplies======>");
		Elements itemsDockArea = doc.select("div[class=dock_area]");
		Elements itemsInner = doc.select("div[class=reply_content]");
		System.out.println("itemsDockArea======>" + itemsDockArea.toString());
		System.out.println("itemsInner======>" + itemsInner.toString());

		if (!replies.isEmpty()) {
			replies.remove(replies.size() - 1);
		}

		int i = 0;
		for (Element item : itemsDockArea) {
			Element grayElement = item.select("span[class=gray]").get(0);
			String gray = grayElement.text();

			Element timeElement = item.select("span[class=fade]").get(0);
			String time = timeElement.text();

			Element replyElement = itemsInner.get(i);
			String reply = replyElement.toString();

			// creating new HashMap
			HashMap<String, String> map = new HashMap<String, String>();

			// adding each child node to HashMap key =>
			// value
			map.put(KEY_GRAY, gray);
			map.put(KEY_TIME, time);
			map.put(KEY_REPLY, reply);

			System.out.println("gray======>" + gray);
			System.out.println("time======>" + time);

			// adding HashList to ArrayList
			replies.add(map);
			i++;
		}

		HashMap<String, String> mapMore = new HashMap<String, String>();

		mapMore.put(KEY_GRAY, MainActivity.MORE_TAG);
		mapMore.put(KEY_TIME, MainActivity.MORE_TAG);
		// mapMore.put(KEY_REPLY, MainActivity.MORE_TAG);

		if (itemsDockArea.isEmpty() && replies.isEmpty()) {
			mapMore.put(KEY_REPLY, "目前尚未有回复");
		} else {
			mapMore.put(KEY_REPLY, MainActivity.MORE_TAG);
		}

		// adding HashList to ArrayList
		replies.add(mapMore);

		return replies;
	}

	public static String getUserReplies(AppContext appContext, Document doc) {
		String replies = null;
		if (doc != null) {
			Elements items = doc.select("div#Wrapper");
			if (!items.isEmpty()) {
				replies = items.get(0).toString();
				// System.out.println("getUserAvatar=====>" + replies);
			}
		}

		return replies;
	}

	public static String getUserAvatar(AppContext appContext, Document doc) {
		String avatar = null;
		if (doc != null) {
			Elements items = doc.select("img[class=avatar]");
			if (!items.isEmpty()) {
				avatar = items.get(0).attr("src");
				System.out.println("getUserAvatar=====>" + avatar);
			}
		}

		return avatar;
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

	public static String getMessageNum(AppContext appContext, Document doc) {
		String messageNum = null;
		if (doc != null) {
			Elements items = doc.select("input[class=super special button]");
			if (!items.isEmpty()) {
				messageNum = items.get(0).attr("value")
						.replaceAll("[^0-9]", "");
				appContext.setMessageNum(messageNum);
				System.out.println("messageNum=====>" + messageNum);
			}
		}

		return messageNum;
	}

	public static String getMessages(AppContext appContext, Document doc) {
		String messages = null;
		Elements items = doc.select("div[class=box]");
		if (!items.isEmpty()) {
			messages = items.get(0).toString();
		}

		return messages;
	}
	
	public static String[] getNodes(AppContext appContext, Document doc) {
		
		Elements items = doc.select("a[class=item_node]");
		String[] nodes = new String[items.size()];
		
		int i = 0;
		for (Element item : items) {
			String node = item.text();
			nodes[i] = node;
			i++;
		}

		return nodes;
	}

	public static ArrayList<HashMap<String, String>> getMessages(
			AppContext appContext, Document doc,
			ArrayList<HashMap<String, String>> messages) {
		System.out.println("getUserReplies======>");
		Elements items = doc.select("div[class=cell]");

		if (!messages.isEmpty()) {
			messages.remove(messages.size() - 1);
		}

		int i = 0;
		for (Element item : items) {
			Element avatarElement = item.select("img[class=avatar]").get(0);
			String avatar = avatarElement.attr("src");

			Element titleElement = item.select("span[class=fade]").get(0);
			String title = titleElement.text();

			Element timeElement = item.select("span[class=snow]").get(0);
			String time = timeElement.text();

			Element messageElement = item.select("div[class=payload]").get(0);
			String message = messageElement.toString();

			// creating new HashMap
			HashMap<String, String> map = new HashMap<String, String>();

			// adding each child node to HashMap key =>
			// value
			map.put(KEY_AVATAR, avatar);
			map.put(KEY_TITLE, title);
			map.put(KEY_TIME, time);
			map.put(KEY_MESSAGE, message);

			// adding HashList to ArrayList
			messages.add(map);
			i++;
		}

		HashMap<String, String> mapMore = new HashMap<String, String>();

		mapMore.put(KEY_AVATAR, MainActivity.MORE_TAG);
		mapMore.put(KEY_TIME, MainActivity.MORE_TAG);
		mapMore.put(KEY_MESSAGE, MainActivity.MORE_TAG);

		if (items.isEmpty() && messages.isEmpty()) {
			mapMore.put(KEY_TITLE, "目前尚未有消息");
		} else {
			mapMore.put(KEY_TITLE, MainActivity.MORE_TAG);
		}

		// adding HashList to ArrayList
		messages.add(mapMore);

		return messages;
	}

	public static int getTopic(AppContext appContext, Document doc,
			HashMap<String, String> content) {

		Element itemContent = doc.select("div[class=box]").get(0);

		Element elAvatar = itemContent.select("div[class=header]")
				.select("img[class=avatar]").get(0);
		String avatar = elAvatar.attr("src");

		Element elTitle = itemContent.select("div[class=header]").select("h1")
				.get(0);
		String title = elTitle.text();

		Element elInfo = itemContent.select("div[class=header]")
				.select("small[class=gray]").get(0);
		String info = elInfo.text();

		Elements elsContent = itemContent.select("div[class=topic_content]");
		String contentStr = "";
		if (!elsContent.isEmpty()) {
			Element elContent = elsContent.get(0);
			contentStr = elContent.toString();
		}

		Elements elsFavorite = itemContent.select("div[class=inner]")
				.select("div[class=fr]").select("span");
		String favorite = "";
		if (!elsFavorite.isEmpty()) {
			System.out.println("!elsFavorite.isEmpty()======>");
			Element elFavorite = elsFavorite.get(0);
			favorite = elFavorite.text();
		}
		System.out.println("favorite======>" + favorite);

		Elements elsOnce = doc.select("input[name=once]");
		String once = "";
		if (!elsOnce.isEmpty()) {
			once = elsOnce.get(0).attr("value");
		}

		content.put(KEY_AVATAR, avatar);
		content.put(KEY_TITLE, title);
		content.put(KEY_INFO, info);
		content.put(KEY_CONTENT, contentStr);
		content.put(KEY_FAVORITE, favorite);
		content.put(KEY_ONCE, once);

		Elements itemsReply = doc.select("div[class=box]").get(1)
				.select("table");
		int replyNum = itemsReply.size();

		StringBuilder sb = new StringBuilder();
		sb.append("<html><head>");
		sb.append("<style type=\"text/css\">body{color: #63656a }");
		sb.append("</style></head>");
		sb.append("<body link=\"#C0C0C0\" vlink=\"#808080\" alink=\"#FF0000\">");
		sb.append("<table cellpadding=\"5\" cellspacing=\"0\"  width=\"100%\" border=\"0\">");

		for (Element item : itemsReply) {
			Element avatarElement = item.select("img[class=avatar]").get(0);
			String ravatar = avatarElement.attr("src");

			Element usernmeElement = item.select("strong").get(0);
			String rusername = usernmeElement.text();

			Element timeElement = item.select("span[class=fade small]").get(0);
			String rtime = timeElement.text();

			Element floorElement = item.select("span[class=no]").get(0);
			String rfloor = floorElement.text();

			Element replyElement = item.select("div[class=reply_content]").get(
					0);
			String rreply = replyElement.toString();

			sb.append("<tr>");
			sb.append("<td width=\"32\" border=\"1\" valign=\"top\" align=\"left\"><img src=");
			sb.append(ravatar);
			sb.append(" Height=32  Width=32 class=\"avatar\" border=\"0\" align=\"center\"auto /></td>");
			// sb.append("<td width=\"10\" valign=\"top\"></td>");
			sb.append("<td width=\"auto\" valign=\"top\" align=\"left\">");
			sb.append("<strong>");
			sb.append(rusername);
			sb.append("</strong>");

			sb.append("<span class=\"time\" style=\"font-size: 11px; color: #ccc;\">");
			sb.append("&nbsp;&nbsp;&nbsp;" + rtime);
			sb.append("</span>");

			sb.append("<span class=\"floor\" style=\"font-size: 9px;ine-height: 9px;font-weight: 500;border-radius: 8px;display: inline-block;background-color: #f3ede0;color: #ccc;padding:2px 5px 1px 5px; color: #ccc; float: right;\">");
			sb.append(rfloor);
			sb.append("</span>");

			sb.append("<br/>");
			sb.append("<div class=\"reply_content\" style=\"padding-top: 5px; padding-bottom: 10px;\">");
			sb.append(rreply);
			sb.append("</div>");
			sb.append("</td>");
			sb.append("</tr>");
		}

		sb.append("</table>");
		sb.append("</body></html>");

		// replies = sb.toString();

		content.put(KEY_REPLIES, sb.toString());

		return replyNum;
	}

}
