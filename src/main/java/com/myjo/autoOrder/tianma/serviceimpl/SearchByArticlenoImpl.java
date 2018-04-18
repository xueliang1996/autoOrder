package com.myjo.autoOrder.tianma.serviceimpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.myjo.autoOrder.tianma.domain.JsonStr;
import com.myjo.autoOrder.tianma.domain.Size;
import com.myjo.autoOrder.tianma.service.SearchByArticleno;

/*
 * 
 * 过滤仓库
 * 
 */
@Service
public class SearchByArticlenoImpl implements SearchByArticleno {
	@Autowired
	private TianmaHttp tianmaHttp;
	@Autowired
	private TaobaoImpl taobaoImpl;

	@Value("${articlenoPostfix}")
	private String articlenoPostfix;// 商家编码后缀
	@Value("${postage}")
	private int postage;// 邮费
	@Value("${percent}")
	private double percent;// 百分比
	// #过滤某个指定仓库及库存数量
	@Value("${rubbishWareHouse}")
	private String rubbishWareHouse;// 过滤的指定仓库
	@Value("${wareHouseNameInventory}")
	private int wareHouseNameInventory;// 指定仓库的最小库存
	// #所有货源的配货率、库存筛选条件
	@Value("${pickRate}")
	private int pickRate;// 配货率
	// 低于60%删除库存小于N；低于70%删除库存小于N
	@Value("${inventory_pickRate}")
	private JSONArray inventory_pickRate;
	// 过滤转发到劲浪下单的仓库
	@Value("${transpondWareHouse}")
	private String transpondWareHouse;
	// 遇到直接下单仓库
	@Value("${directWareHouse}")
	private String directWareHouse;
	// 符合的仓库有两个及两个以上
	@Value("${comparisonParam}")
	private JSONArray comparisonParam;
	// A/B分流，默认设置为B,A=true,B=false
	@Value("${orderSelect}")
	private boolean orderSelect;
	@Value("${ReplacementOrder.postage}")
	private int replacementPostage;
	@Value("${ReplacementOrder.percent}")
	private double replacementPercent;

	private static final Logger LOGGER = LoggerFactory.getLogger(SearchByArticlenoImpl.class);

	/*
	 * 
	 * 保本价过滤 baobenPrice// 测试数据 String articleno = "819474-013";// 货号 String
	 * chineseSize = "43";// 尺码 double price = 600;// 实际付款 String remark = "";//
	 * 商家备注
	 * 
	 * 
	 */
	@Override
	public List<JsonStr> priceFilter(long tid, String articleno, double price, boolean isSecond) {
		String[] articlenoPostfixs = articlenoPostfix.split(",");
		if (articlenoPostfixs.length != 0) {
			for (int i = 0; i < articlenoPostfixs.length; i++) {
				if (articlenoPostfixs[i].isEmpty()) {
					continue;
				}
				if (articleno.contains(articlenoPostfixs[i])) {
					LOGGER.info("商家编码包含其他字符" + articleno + "，此单不下");
					// "软件下单失败，商家编码包含非下单字符"+articleno
					long flag = 5;
					String memo = "软件下单失败，商家编码包含非下单字符[" + articleno + "]";
					taobaoImpl.updateTradeMemo(tid, flag, memo);
					return null;
				}
			}
		}
		String chineseSize = articleno.split("-")[2];
		switch (chineseSize) {
		case "MISC":
			chineseSize = "均码";
			break;
		case "XXL":
			chineseSize = "2XL";
			break;
		case "XXXL":
			chineseSize = "3XL";
			break;
		case "XXXXL":
			chineseSize = "4XL";
			break;
		case "XXS":
			chineseSize = "2Xs";
			break;
		}
		LOGGER.info("--将货源信息封装为对象--");
		double baobenPrice = 0;
		if (isSecond) {
			// 二次下单公式
			baobenPrice = (price - replacementPostage) * replacementPercent;
		} else {
			// 自动下单公式
			baobenPrice = (price - postage) * percent;
		}
		LOGGER.info("计算保本价为:" + baobenPrice);
		Map<String, String> map = null;
		try {
			map = tianmaHttp.getSearchByArticleno(articleno.split("-")[0] + "-" + articleno.split("-")[1]);
		} catch (Exception e) {
			// TODO: handle exception
		}
		if (map.size() == 0 || map == null) {
			LOGGER.info("天猫标记紫旗，此订单不下单");
			long flag = 5;
			String memo = "天马没有找到对应尺码" + articleno;
			org.json.JSONObject msg = taobaoImpl.updateTradeMemo(tid, flag, memo);
			LOGGER.info(String.valueOf(msg));
			return null;
		}
		JSONObject json = new JSONObject(map.get("json"));
		JSONArray jsonArray = json.getJSONArray("rows");
		String[] size_info = map.get("size_info").split(",");
		List<JsonStr> jsonStrList = new ArrayList<JsonStr>();
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject warehouseInfo = jsonArray.getJSONObject(i);
			String warehouseInfoStr = String.valueOf(warehouseInfo);
			if (warehouseInfo.getDouble("proxyPrice") <= baobenPrice) {
				JsonStr jsonStr = new JsonStr();
				jsonStr.setWareHouseName(warehouseInfo.getString("wareHouseName"));
				jsonStr.setWareHouseID(warehouseInfo.getInt("wareHouseID"));
				jsonStr.setArticleno(warehouseInfo.getString("articleno"));
				jsonStr.setArticleno_old(warehouseInfo.getString("articleno_old"));
				jsonStr.setExpressName(warehouseInfo.getString("expressName"));
				jsonStr.setFirst_w(String.valueOf(warehouseInfo.getDouble("first_w")));
				jsonStr.setWeight(map.get("weight"));
				// 取出配货率和发货时效
				String pickRate = warehouseInfo.getString("pickRate");
				pickRate = pickRate.substring(pickRate.indexOf("配货率：") + 4, pickRate.indexOf("<br/>"));
				String timeEfficacy = warehouseInfo.getString("pickRate");
				timeEfficacy = timeEfficacy.substring(timeEfficacy.indexOf("发货时效:") + 5);
				jsonStr.setTimeEfficacy(timeEfficacy);
				jsonStr.setPickRate(pickRate);
				jsonStr.setProxyPrice(warehouseInfo.getDouble("proxyPrice"));
				jsonStr.setUpdateTime(warehouseInfo.getString("updateTime"));
				jsonStr.setProductId(map.get("productId"));
				String pick_date = null;
				switch (warehouseInfo.getString("pick_date")) {
				case "0":
					pick_date = "周一至周五";
					jsonStr.setPick_date(pick_date);
					break;
				case "1":
					pick_date = "周一至周六";
					jsonStr.setPick_date(pick_date);
					break;
				case "2":
					pick_date = "周一至周日";
					jsonStr.setPick_date(pick_date);
					break;
				}
				Size size = new Size();
				for (int j = 0; j < size_info.length; j++) {
					String[] sizes = size_info[j].split("<>");
					if (warehouseInfoStr.contains(sizes[2]) && chineseSize.equals(sizes[1])) {
						size.setSize1(sizes[0]);
						size.setSize2(sizes[1]);
						size.setSkuId(sizes[2]);
						size.setInventory(warehouseInfo.getInt(sizes[2]));
					}
				}
				jsonStr.setSize(size);
				if (jsonStr.getSize().getInventory() == 0) {
					continue;
				}
				jsonStrList.add(jsonStr);
			}

		}

		LOGGER.info("保本价过滤后，剩余可选仓库个数:" + jsonStrList.size());
		if (jsonStrList.size() == 0 || jsonStrList == null) {
			LOGGER.info("天猫标记紫旗，此订单不下单");
			long flag = 5;
			String memo = "没有计算出仓库信息.请人工处理[" + articleno + "]";
			org.json.JSONObject msg = taobaoImpl.updateTradeMemo(tid, flag, memo);
			LOGGER.info(String.valueOf(msg));
			return null;
		}

		return jsonStrList;
	}

	/*
	 * 
	 * 指定仓库过滤 wareHouseNames
	 * 
	 * 
	 */
	@Override
	public List<JsonStr> rubbishWareHouseFilter(List<JsonStr> jsonStrList, boolean isSecond, String lastWarehouseName) {
		String[] wareHouseNames = rubbishWareHouse.split(",");
		List<JsonStr> jsonStrList2 = null;
		LOGGER.info("过滤指标：" + "指定仓库=\"" + rubbishWareHouse + "\"," + "库存=" + wareHouseNameInventory);
		if (rubbishWareHouse.length() > 0) {
			jsonStrList = jsonStrList.stream().filter(jsonStr -> !rubbishWareHouse.contains(jsonStr.getWareHouseName())
					|| jsonStr.getSize().getInventory() >= wareHouseNameInventory).collect(Collectors.toList());
			LOGGER.info("过滤指定仓库后,剩余可选仓库个数为:" + jsonStrList.size());
		}
		if (isSecond) {
			try {
				jsonStrList2 = jsonStrList.stream()
						.filter(jsonStr -> !jsonStr.getWareHouseName().contains(lastWarehouseName))
						.collect(Collectors.toList());
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
		if (jsonStrList2 != null || jsonStrList2.size() != 0) {
			return jsonStrList2;
		}
		return jsonStrList;
	}

	/*
	 * 
	 * 配货率及库存过滤
	 * 
	 */
	@Override
	public List<JsonStr> pickRateFilter(List<JsonStr> jsonStrList) {

		jsonStrList = jsonStrList.stream()
				.filter(jsonStr -> Integer
						.parseInt(jsonStr.getPickRate().substring(0, jsonStr.getPickRate().indexOf("%"))) > pickRate)
				.collect(Collectors.toList());
		LOGGER.info("过滤指标：" + "配货率<" + pickRate + "%" + "全删:" + jsonStrList.size());
		List<JsonStr> jsonStrFiltereList = new ArrayList<JsonStr>();
		for (int i = 0; i < inventory_pickRate.length(); i++) {
			JSONObject inventory_pickRates = inventory_pickRate.getJSONObject(i);
			for (int j = 0; j < jsonStrList.size(); j++) {
				JsonStr jsonStr = jsonStrList.get(j);
				if (Integer
						.parseInt(jsonStr.getPickRate().substring(0,
								jsonStr.getPickRate().indexOf("%"))) >= inventory_pickRates.getInt("PickRates1")
						&& Integer.parseInt(jsonStr.getPickRate().substring(0,
								jsonStr.getPickRate().indexOf("%"))) < inventory_pickRates.getInt("PickRates2")
						&& jsonStr.getSize().getInventory() >= inventory_pickRates.getInt("inventorys")) {
					jsonStrFiltereList.add(jsonStr);
				}
				if (i == inventory_pickRate.length() - 1
						&& Integer.parseInt(jsonStr.getPickRate().substring(0,
								jsonStr.getPickRate().indexOf("%"))) >= inventory_pickRates.getInt("PickRates2")
						&& jsonStr.getSize().getInventory() >= 1) {
					jsonStrFiltereList.add(jsonStr);
				}
			}
		}

		LOGGER.info("配货率及库存过滤后,剩余可选仓库个数为:" + jsonStrFiltereList.size());
		return jsonStrFiltereList;
	}

	/*
	 * 
	 * 过滤转发到劲浪下单的仓库及遇到直接下单的仓库
	 * 
	 */
	@Override
	public List<JsonStr> transpondWareHouseFilter(List<JsonStr> jsonStrList) {
		List<JsonStr> jsonStrFilterList = new ArrayList<JsonStr>();
		List<JsonStr> jsonStrDirectList = new ArrayList<JsonStr>();
		LOGGER.info("过滤条件：" + "需要转发到劲浪下单的仓库=\"" + transpondWareHouse + "\"遇到直接下单仓库=\"" + directWareHouse + "\"");

		if (transpondWareHouse.length() > 0) {
			for (int j = 0; j < jsonStrList.size(); j++) {
				JsonStr jsonStr = jsonStrList.get(j);
				if (transpondWareHouse.contains(jsonStr.getWareHouseName())) {
					jsonStrFilterList.add(jsonStr);
				}
			}
		}
		// 符合条件仓库，跳过此订单，天猫标记紫旗并备注劲浪
		if (jsonStrFilterList.size() != 0 && jsonStrFilterList != null) {
			// 天猫标记紫旗并备注劲浪
			LOGGER.info("天猫标记紫旗并备注劲浪,跳过此单");
			LOGGER.info("需要转发到劲浪下单的仓库:" + jsonStrFilterList);
			jsonStrFilterList.forEach(jsonStr -> jsonStr.setFlag1(true));
			return jsonStrFilterList;
		}
		if (directWareHouse.length() > 0) {
			for (int j = 0; j < jsonStrList.size(); j++) {
				JsonStr jsonStr = jsonStrList.get(j);
				if (directWareHouse.contains(jsonStr.getWareHouseName())) {
					jsonStrDirectList.add(jsonStr);
				}
			}

		}
		if (jsonStrDirectList.size() != 0 && jsonStrDirectList != null) {
			// 符合条件仓库，直接下单
			jsonStrDirectList.forEach(jsonStr -> jsonStr.setFlag(true));
			LOGGER.info("遇到直接下单仓库，数量为:" + jsonStrDirectList.size());
			return jsonStrDirectList;
		}
		LOGGER.info("无符合过滤条件仓库:" + jsonStrList.size());
		return jsonStrList;
	}

	/*
	 * 
	 * 如果下单时间为周五4:00-周六4:00，则过滤周六周日不发货仓库， 如果下单时间为周六4:00-周日4:00，则过滤周日不发货仓库 String
	 * orderTime = "2018-04-07 16:13:56";
	 */
	@Override
	public List<JsonStr> orderTimeFilter(List<JsonStr> jsonStrList, String orderTime) {

		String[] orderTimes = orderTime.split(" ");
		TransformDayImpl transformDayHandler = new TransformDayImpl();
		int i = transformDayHandler.dayForWeek(orderTimes[0]);
		switch (i) {
		case 5:
			if (!transformDayHandler.isDateBefore(orderTimes[1])) {
				jsonStrList = jsonStrList.stream().filter(jsonStr -> !jsonStr.getPick_date().contains("周一至周五"))
						.collect(Collectors.toList());
				LOGGER.info("下单时间为：周五," + orderTimes[1]);
				LOGGER.info("过滤周一至周五配货仓库");
				LOGGER.info("过滤后,剩余仓库数量为:" + jsonStrList.size());
				break;
			}
			break;
		case 6:
			if (transformDayHandler.isDateBefore(orderTimes[1])) {
				jsonStrList = jsonStrList.stream().filter(jsonStr -> !jsonStr.getPick_date().contains("周一至周五"))
						.collect(Collectors.toList());
				LOGGER.info("下单时间为：周六," + orderTimes[1]);
				LOGGER.info("过滤周一至周五配货仓库");
				LOGGER.info("过滤后,剩余仓库数量为:" + jsonStrList.size());
				break;
			} else {
				jsonStrList = jsonStrList.stream().filter(jsonStr -> !jsonStr.getPick_date().contains("周一至周六"))
						.collect(Collectors.toList());
				LOGGER.info("下单时间为：周六," + orderTimes[1]);
				LOGGER.info("过滤周一至周六配货仓库");
				LOGGER.info("过滤后,剩余仓库数量为:" + jsonStrList.size());
				break;
			}
		case 7:
			if (transformDayHandler.isDateBefore(orderTimes[1])) {
				jsonStrList = jsonStrList.stream().filter(jsonStr -> !jsonStr.getPick_date().contains("周一至周六"))
						.collect(Collectors.toList());
				LOGGER.info("下单时间为：周日," + orderTimes[1]);
				LOGGER.info("过滤周一至周日配货仓库");
				LOGGER.info("过滤后,剩余仓库数量为:" + jsonStrList.size());
				break;
			}
			break;
		}
		return jsonStrList;
	}

	/*
	 * 
	 * 符合的仓库有两个及两个以上,以符合条件的价格最低的仓库为基准仓库，进行对比
	 * 
	 */

	@Override
	public List<JsonStr> compareFilter(List<JsonStr> jsonStrList) {
		LOGGER.info("符合的仓库有两个及两个以上,以符合条件的价格最低的仓库为基准仓库，进行对比");
		double price = jsonStrList.get(0).getProxyPrice();// 基准价格
		int pickRate = 0;// 基准配货率
		JsonStr standardJsonStr = new JsonStr();// 基准仓库
		List<JsonStr> jsonStrFilterList = new ArrayList<JsonStr>();
		for (int i = 0; i < jsonStrList.size(); i++) {
			for (int j = jsonStrList.size() - 1; j > i; j--) {
				if (jsonStrList.get(i).getProxyPrice() > jsonStrList.get(j).getProxyPrice()
						&& price > jsonStrList.get(j).getProxyPrice()) {
					price = jsonStrList.get(j).getProxyPrice();
				}
			}
		}
		for (int i = 0; i < jsonStrList.size(); i++) {
			if (price == jsonStrList.get(i).getProxyPrice()) {
				standardJsonStr = jsonStrList.get(i);
				break;
			}
			continue;
		}
		pickRate = Integer
				.parseInt(standardJsonStr.getPickRate().substring(0, standardJsonStr.getPickRate().indexOf("%")));
		JSONObject A = null;
		JSONObject B = null;
		JSONObject C = null;
		JSONObject D = null;
		JSONObject E = null;
		for (int i = 0; i < comparisonParam.length(); i++) {
			switch (i) {
			case 0:
				A = comparisonParam.getJSONObject(0);

				break;
			case 1:
				B = comparisonParam.getJSONObject(1);

				break;
			case 2:
				C = comparisonParam.getJSONObject(2);

				break;
			case 3:
				D = comparisonParam.getJSONObject(3);

				break;
			case 4:
				E = comparisonParam.getJSONObject(4);

				break;
			}
		}
		for (int i = 0; i < jsonStrList.size(); i++) {
			double prices = jsonStrList.get(i).getProxyPrice();
			int pickRates = Integer.parseInt(
					jsonStrList.get(i).getPickRate().substring(0, jsonStrList.get(i).getPickRate().indexOf("%")));
			if (price == prices && pickRate == pickRates) {
				continue;
			}
			double p = (prices - price) / price;
			int r = pickRates - pickRate;
			if (A.getDouble("Prices") >= p && r >= A.getInt("PickRates")
					|| B.getDouble("Prices") >= p && r >= B.getInt("PickRates")
					|| C.getDouble("Prices") >= p && r >= C.getInt("PickRates")
					|| D.getDouble("Prices") >= p && r >= D.getInt("PickRates")
					|| E.getDouble("Prices") >= p && r >= E.getInt("PickRates")) {
				jsonStrFilterList.add(jsonStrList.get(i));
			}
		}
		LOGGER.info("不包含基准仓库的筛选结果:" + jsonStrFilterList.size());
		if (jsonStrFilterList.size() == 0 || jsonStrFilterList == null) {
			LOGGER.info("无符合条件仓库，使用基准仓库");
			jsonStrFilterList.add(standardJsonStr);
			return jsonStrFilterList;
		}
		return jsonStrFilterList;
	}

	/*
	 * 
	 * 与基准仓库对比后，还剩余2个或两个以上的
	 * 
	 */
	@Override
	public List<JsonStr> finalFilter(List<JsonStr> jsonStrList) {
		JsonStr jsonStrFilter = jsonStrList.get(0);// 基准数据
		List<JsonStr> jsonStrFilterList = new ArrayList<JsonStr>();
		if (orderSelect) {
			// true比对价格
			for (int i = 0; i < jsonStrList.size(); i++) {
				if (jsonStrList.get(i).getProxyPrice() < jsonStrFilter.getProxyPrice()) {
					jsonStrFilter = jsonStrList.get(i);
				}
			}
			jsonStrFilterList.add(jsonStrFilter);
			return jsonStrFilterList;
		} else {
			// false比对配货率
			for (int i = 0; i < jsonStrList.size(); i++) {
				if (Double.parseDouble(jsonStrList.get(i).getPickRate().substring(0,
						jsonStrList.get(i).getPickRate().indexOf("%"))) > Double.parseDouble(
								jsonStrFilter.getPickRate().substring(0, jsonStrFilter.getPickRate().indexOf("%")))) {
					jsonStrFilter = jsonStrList.get(i);
				}

			}
			jsonStrFilterList.add(jsonStrFilter);
			return jsonStrFilterList;
		}
	}
}
