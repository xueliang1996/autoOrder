package com.myjo.autoOrder.tianma.serviceimpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.myjo.autoOrder.tianma.domain.TmArea;
import com.myjo.autoOrder.tianma.util.CookieUtil;

@Service
public class TianmaHttp {

	private static final Logger LOGGER = LoggerFactory.getLogger(TianmaHttp.class);

	@Value("${tianma.url}")
	private String tianmaUrl;
	@Value("${sf_express_price}")
	private double sf_express_price;
	@Autowired
	private TransformDayImpl transformDayImpl;
	private final String userAgent = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3355.4 Safari/537.36";
	private final String accept = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8";
	private final String connection = "keep-alive";
	private final String acceptLanguage = "zh-CN,zh;q=0.8,en;q=0.6,zh-TW;q=0.4";
	private final String xRequestedWith = "XMLHttpRequest";
	private final String contentType = "application/x-www-form-urlencoded; charset=UTF-8";
	private final String acceptEncoding = "gzip, deflate, sdch";

	// Request URL:http://www.tianmasport.com/ms/order/searchByArticleno.do
	public Map<String, String> getSearchByArticleno(String articleno) {
		CookieUtil cookieUtil = new CookieUtil();
		String[] cookie = cookieUtil.getCookie();
		String url = "http://" + tianmaUrl + "/ms/order/searchByArticleno.do";
		Map<String, String> map = new HashMap<String, String>();
		HttpResponse<String> response = null;
		try {
			response = Unirest.post(url).header("Cookie", cookie[0] + "=" + cookie[1]).header("Host", tianmaUrl)
					.header("Connection", connection).header("Accept", accept).header("Accept-Language", acceptLanguage)
					.header("Origin", "http://" + tianmaUrl).header("X-Requested-With", xRequestedWith)
					.header("User-Agent", userAgent).header("Content-Type", contentType)
					.header("Accept-Encoding", acceptEncoding).field("articleno", articleno).asString();

			String rt = response.getBody();
			// System.out.println(rt);
			int jsonIndex1 = rt.indexOf("parseJSON('");
			int jsonIndex2 = rt.indexOf("}]}');");
			int sizeIndex1 = rt.indexOf("size_info =");
			int sizeIndex2 = rt.indexOf("'.split(\",\");");
			int productIdIndex1 = rt.indexOf("hide('");
			int productIdIndex2 = rt.indexOf("')\"");
			// weight 商品重量
			String toOrderStr;
			String bstr = "+r.articleno+'";
			int weightIndex1 = rt.indexOf(bstr);
			int weightIndex2 = rt.indexOf("r.discount", weightIndex1);
			toOrderStr = rt.substring(weightIndex1 + bstr.length(), weightIndex2);
			toOrderStr = toOrderStr.replaceAll("\\'", "").replaceAll("\\\\", "");
			String weight = (toOrderStr.split(",")[3]);
			String productId = rt.substring(productIdIndex1 + 6, productIdIndex2);
			String json = rt.substring(jsonIndex1 + 11, jsonIndex2 + 3);
			String size_info = rt.substring(sizeIndex1 + 13, sizeIndex2);

			map.put("json", json);
			map.put("size_info", size_info);
			map.put("productId", productId);
			map.put("weight", weight);
		} catch (UnirestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		return map;
	}

	// 地址信息
	public List<TmArea> getArea(String pid) {
		CookieUtil cookieUtil = new CookieUtil();
		String[] cookie = cookieUtil.getCookie();
		List<TmArea> list = new ArrayList<>();
		HttpResponse<JsonNode> response = null;
		try {
			response = Unirest.get("http://" + tianmaUrl + "/ms/order/getArea.do?pid=" + pid).header("Host", tianmaUrl)
					.header("Connection", "keep-alive").header("Upgrade-Insecure-Requests", "1")
					.header("User-Agent", userAgent).header("Accept", accept).header("Accept-Encoding", acceptEncoding)
					.header("Accept-Language", acceptLanguage).header("X-Requested-With", xRequestedWith)
					.header("Cookie", cookie[0] + "=" + cookie[1]).asJson();
		} catch (UnirestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		int code = response.getStatus();
		if (code == 200) {
			JSONObject rt = response.getBody().getObject();
			org.json.JSONArray rows = rt.getJSONArray("rows");
			TmArea tmArea;
			JSONObject area;
			for (int i = 0; i < rows.length(); i++) {
				area = rows.getJSONObject(i);
				tmArea = new TmArea();
				tmArea.setId(area.getLong("id"));
				tmArea.setFlag(area.getInt("flag"));
				tmArea.setLevel(area.getInt("level"));
				tmArea.setName(area.getString("name"));
				tmArea.setPpid(area.get("ppid").toString().equals("null") ? 0l : area.getLong("ppid"));
				tmArea.setPid(area.getLong("pid"));
				tmArea.setZipcode(area.getString("zipcode"));
				tmArea.setPpname(area.get("ppname").toString());
				list.add(tmArea);
			}

		} else {
			LOGGER.info("天马地址区域查询失败:" + code);
			return null;
		}
		return list;
	}

	// 获取快递信息
	public com.alibaba.fastjson.JSONObject getPostage(String wareHouseName, String province, String weight,
			double first_w) {
		double totalWeight = Double.parseDouble(weight);
		CookieUtil cookieUtil = new CookieUtil();
		String[] cookie = cookieUtil.getCookie();
		String url1 = "http://" + tianmaUrl + "/ms/order/getPostage.do";
		String url2 = "http://" + tianmaUrl + "/ms/order/defaultPostage.do";
		HttpResponse<String> response = null;
		try {
			response = Unirest.post(url1).header("Cookie", cookie[0] + "=" + cookie[1]).header("Host", tianmaUrl)
					.header("Connection", connection).header("Accept", accept).header("Accept-Language", acceptLanguage)
					.header("Origin", "http://" + tianmaUrl).header("X-Requested-With", xRequestedWith)
					.header("User-Agent", userAgent).header("Content-Type", contentType)
					.header("Accept-Encoding", acceptEncoding).field("wareHouseName", wareHouseName)
					.field("province", province).field("weight", weight).field("first_w", first_w)
					.field("totalWeight", totalWeight).asString();
			String express = response.getBody();
			JSONArray expressJsonA = JSONArray.parseArray(express);
			for (int i = 0; i < expressJsonA.size(); i++) {
				if (expressJsonA.getJSONObject(i).getString("expressName").contains("顺丰(")
						&& Double.parseDouble(expressJsonA.getJSONObject(i).getString("expressName").substring(
								expressJsonA.getJSONObject(i).getString("expressName").indexOf("(") + 1, expressJsonA
										.getJSONObject(i).getString("expressName").indexOf(")"))) <= sf_express_price) {
					return expressJsonA.getJSONObject(i);
				} else {
					response = Unirest.post(url2).header("Cookie", cookie[0] + "=" + cookie[1])
							.header("Host", tianmaUrl).header("Connection", connection).header("Accept", accept)
							.header("Accept-Language", acceptLanguage).header("Origin", "http://" + tianmaUrl)
							.header("X-Requested-With", xRequestedWith).header("User-Agent", userAgent)
							.header("Content-Type", contentType).header("Accept-Encoding", acceptEncoding)
							.field("wareHouseName", wareHouseName).field("province", province).field("weight", weight)
							.field("first_w", first_w).asString();
					express = response.getBody();
					com.alibaba.fastjson.JSONObject expressJson = com.alibaba.fastjson.JSONObject.parseObject(express);
					return expressJson;
				}
			}

		} catch (Exception e) {

		}
		return null;
	}

	// 提交订单
	public String orderBooking(Map<String, Object> map) {
		CookieUtil cookieUtil = new CookieUtil();
		String[] cookie = cookieUtil.getCookie();
		LOGGER.info("提交订单");
		map.entrySet().stream().forEach(o -> {
			LOGGER.info(String.format("%s=%s", o.getKey(), String.valueOf(o.getValue())));
		});
		String rt = null;
		String url = "http://" + tianmaUrl + "/ms/order/booking.do";
		HttpResponse<JsonNode> response = null;
		try {
			response = Unirest.post(url).header("Cookie", cookie[0] + "=" + cookie[1]).header("Host", tianmaUrl)
					.header("Connection", connection).header("Accept", accept).header("Accept-Language", acceptLanguage)
					.header("Origin", "http://" + tianmaUrl).header("X-Requested-With", xRequestedWith)
					.header("User-Agent", userAgent).header("Content-Type", contentType)
					.header("Accept-Encoding", acceptEncoding).field("area", map.get("area"))
					.field("city", map.get("city")).field("recv_name", map.get("recv_name"))
					.field("jsonStr", map.get("jsonStr")).field("remark", map.get("remark"))
					.field("area_id", map.get("area_id")).field("zipcode", map.get("zipcode"))
					.field("recv_tel", map.get("recv_tel")).field("recv_address", map.get("recv_address"))
					.field("province", map.get("province")).field("province_id", map.get("province_id"))
					.field("recv_mobile", map.get("recv_mobile")).field("outer_tid", map.get("outer_tid"))
					.field("city_id", map.get("city_id")).asJson();
			int code = response.getStatus();
			if (code == 200) {
				JSONObject object = response.getBody().getObject();
				if (object.getBoolean("success") == true) {
					rt = object.getString("msg");
					LOGGER.info("msg:" + rt);
				} else {
					LOGGER.info("orderBooking 查询失败:" + object.getString("msg"));
					return null;
				}
			}
		} catch (UnirestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return rt;
	}

	// 获取下单所需的字段
	public long myDataList(long tid) {
		String outer_id = String.valueOf(tid);
		CookieUtil cookieUtil = new CookieUtil();
		String[] cookie = cookieUtil.getCookie();
		String url = "http://" + tianmaUrl + "/ms/tradeOrders/myDataList.do";
		String rt = null;
		String currentTime = transformDayImpl.getCurrentTime();
		HttpResponse<String> response;
		try {
			response = Unirest.post(url).header("Cookie", cookie[0] + "=" + cookie[1]).header("Host", tianmaUrl)
					.header("Connection", connection).header("Accept", accept).header("Accept-Language", acceptLanguage)
					.header("Origin", "http://" + tianmaUrl).header("X-Requested-With", xRequestedWith)
					.header("User-Agent", userAgent).header("Content-Type", contentType)
					.header("Accept-Encoding", acceptEncoding).field("page", 1).field("rows", 15).field("status", 0)
					.field("m_warehouse_name", "").field("goods_no", "").field("names", "")
					.field("startTime", currentTime).field("endsTime", "").field("size", "").field("outer_tid", "")
					.field("order_id", "").asString();

			int code = response.getStatus();
			if (code == 200) {
				JSONObject object = new JSONObject(response);
				org.json.JSONArray jsonA = object.getJSONArray("rows");
				for (int i = 0; i < jsonA.length(); i++) {
					if (outer_id.equals(jsonA.getJSONObject(i).getString("outer_order_id"))) {
						tid = jsonA.getJSONObject(i).getLong("tid");
					}
				}
			} else {
				LOGGER.info("updataBalance:" + code);
				return 0;
			}
		} catch (UnirestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return tid;
	}

	// 下单
	public boolean updataBalance(long orderId, String payPwd) {
		CookieUtil cookieUtil = new CookieUtil();
		String[] cookie = cookieUtil.getCookie();
		LOGGER.info(String.format("http tm-sport updataBalance:orderId:%s,payPwd:********,", orderId));
		String url = "http://" + tianmaUrl + "/ms/tradeInfo/updataBalance.do";
		boolean rt = false;
		HttpResponse<JsonNode> response;
		try {
			response = Unirest.post(url).header("Cookie", cookie[0] + "=" + cookie[1]).header("Host", tianmaUrl)
					.header("Connection", connection).header("Accept", accept).header("Accept-Language", acceptLanguage)
					.header("Origin", "http://" + tianmaUrl).header("X-Requested-With", xRequestedWith)
					.header("User-Agent", userAgent).header("Content-Type", contentType)
					.header("Accept-Encoding", acceptEncoding).field("orderIDs", orderId).field("payPwd", payPwd)
					.asJson();

			int code = response.getStatus();
			if (code == 200) {
				JSONObject object = response.getBody().getObject();

				rt = object.getBoolean("success");
				LOGGER.info(String.valueOf(rt));

			} else {
				LOGGER.info("updataBalance:" + code);
			}
		} catch (UnirestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return rt;
	}
}
