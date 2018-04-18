package com.myjo.autoOrder.tianma.serviceimpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.myjo.autoOrder.tianma.domain.JsonStr;
import com.myjo.autoOrder.tianma.domain.TmArea;
import com.taobao.api.domain.Order;
import com.taobao.api.domain.Trade;

@Component
public class PlaceOrder {

	@Autowired
	private SearchByArticlenoImpl searchByArticlenoHandler;
	@Value("${is_weekCheck}")
	private boolean is_weekCheck;
	@Value("${isTime}")
	private boolean isTime;
	@Value("${tmAreaTransform}")
	private org.json.JSONArray tmAreaTransform;
	@Autowired
	private TransformDayImpl transformDayImpl;
	@Autowired
	private TaobaoImpl taobaoImpl;
	@Autowired
	private TianmaHttp tianmaHttp;
	private static final Logger LOGGER = LoggerFactory.getLogger(SearchByArticlenoImpl.class);

	// 筛选出仓库和订单加入Map中
	public Map<String, Object> addMapInInfo(long tid, Trade trade, Order order, boolean isSecond,
			String lastWarehouseName) {

		Map<String, Object> JsonStrAndTrade = new HashMap<String, Object>();
		double payment = Double.parseDouble(order.getPayment());// 实付金额
		String pay_time = String.valueOf(trade.getPayTime());// 付款时间
		String outer_sku_id = order.getOuterSkuId();// 商家编码
		Long refundId = order.getRefundId();
		if (refundId != null) {
			LOGGER.info("淘宝订单" + tid + ".已经存在退款单" + refundId + "不能进行下单.");
			return null;
		}
		if (order.getNum() != 1) {
			LOGGER.info("淘宝订单" + outer_sku_id + ".数量大于[1]不能进行下单.");
			long flag = 5;
			String memo = "淘宝订单" + outer_sku_id + ".数量大于[1].";
			org.json.JSONObject msg = taobaoImpl.updateTradeMemo(tid, flag, memo);
			LOGGER.info(String.valueOf(msg));
			return null;
		}

		if (isTime) {
			boolean flags = transformDayImpl.getMillisecond(pay_time, System.currentTimeMillis());
			if (!flags) {
				LOGGER.info("此订单付款未超过指定时间,不下此单");
				return null;
			}
		}
		List<JsonStr> jsonStrList = new ArrayList<JsonStr>();
		// 保本价过滤 baobenPrice

		try {
			jsonStrList = searchByArticlenoHandler.priceFilter(tid, outer_sku_id, payment, isSecond);
		} catch (Exception e) {
		}
		if (jsonStrList != null || jsonStrList.size() != 0) {
			// 指定仓库过滤 wareHouseNames

			try {
				jsonStrList = searchByArticlenoHandler.rubbishWareHouseFilter(jsonStrList, isSecond, lastWarehouseName);
			} catch (Exception e) {
				// TODO: handle exception
			}
			if (jsonStrList == null || jsonStrList.size() == 0) {
				// 包含指定的过滤仓库，不下此单
				LOGGER.info("天猫标记紫旗，此订单不下单");
				long flag = 5;
				String memo = "没有计算出仓库信息.请人工处理[" + outer_sku_id + "]";
				org.json.JSONObject msg = taobaoImpl.updateTradeMemo(tid, flag, memo);
				LOGGER.info(String.valueOf(msg));
				return null;
			}
			// 配货率及库存过滤

			try {
				jsonStrList = searchByArticlenoHandler.pickRateFilter(jsonStrList);
			} catch (Exception e) {
				// TODO: handle exception
			}
			if (jsonStrList == null || jsonStrList.size() == 0) {
				LOGGER.info("天猫标记紫旗，此订单不下单");
				long flag = 5;
				String memo = "没有计算出仓库信息.请人工处理[" + outer_sku_id + "]";
				org.json.JSONObject msg = taobaoImpl.updateTradeMemo(tid, flag, memo);
				LOGGER.info(String.valueOf(msg));
				return null;
			}
			// 过滤转发到劲浪下单的仓库及遇到直接下单的仓库

			try {
				jsonStrList = searchByArticlenoHandler.transpondWareHouseFilter(jsonStrList);
			} catch (Exception e) {
				// TODO: handle exception
			}
			if (jsonStrList == null || jsonStrList.size() == 0) {

				LOGGER.info("天猫标记紫旗，此订单不下单");
				long flag = 5;
				String memo = "没有计算出仓库信息.请人工处理[" + outer_sku_id + "]";
				org.json.JSONObject msg = taobaoImpl.updateTradeMemo(tid, flag, memo);
				LOGGER.info(String.valueOf(msg));
				return null;

			} else if (jsonStrList.get(0).isFlag()) {
				if (jsonStrList.size() == 1) {
					// 只有一个遇到直接下单仓库
					LOGGER.info("=======下单=======");
					JsonStrAndTrade.put("JsonStr", jsonStrList.get(0));
					Optional<Trade> tradeOptionals = taobaoImpl.getTaobaoTradeFullInfo(tid);
					JsonStrAndTrade.put("Trade", tradeOptionals.get());
					return JsonStrAndTrade;
				} else {
					// 遇到直接下单仓库多个
					LOGGER.info("=======下单=======");
					List<JsonStr> jsonStrFliterList = new ArrayList<JsonStr>();
					jsonStrFliterList.add(jsonStrList.get(0));
					JsonStrAndTrade.put("JsonStr", jsonStrFliterList.get(0));
					Optional<Trade> tradeOptionals = taobaoImpl.getTaobaoTradeFullInfo(tid);
					JsonStrAndTrade.put("Trade", tradeOptionals.get());
					return JsonStrAndTrade;
				}
			} else if (jsonStrList.get(0).isFlag1()) {
				long flag = 5;
				String memo = "需转发劲浪下单:" + jsonStrList.get(0).getWareHouseName();
				taobaoImpl.updateTradeMemo(tid, flag, memo);
				return null;
			}
			// 如果下单时间为周五4:00-周六4:00，则过滤周六周日不发货仓库，
			// 如果下单时间为周六4:00-周日4:00，则过滤周日不发货仓库
			if (is_weekCheck) {

				try {
					jsonStrList = searchByArticlenoHandler.orderTimeFilter(jsonStrList, pay_time);
				} catch (Exception e) {
					// TODO: handle exception
				}
			}
			if (jsonStrList.size() == 0 || jsonStrList == null) {
				LOGGER.info("天猫标记紫旗，此订单不下单");
				long flag = 5;
				String memo = "没有计算出仓库信息.请人工处理[" + outer_sku_id + "]";
				org.json.JSONObject msg = taobaoImpl.updateTradeMemo(tid, flag, memo);
				LOGGER.info(String.valueOf(msg));
				return null;
			}
			if (jsonStrList.size() == 1) {
				// 符合的仓库只有一个
				LOGGER.info("=======下单=======");
				JsonStrAndTrade.put("JsonStr", jsonStrList.get(0));
				Optional<Trade> tradeOptionals = taobaoImpl.getTaobaoTradeFullInfo(tid);
				JsonStrAndTrade.put("Trade", tradeOptionals.get());
				return JsonStrAndTrade;
			} else {
				// 符合的仓库有两个及两个以上,以符合条件的价格最低的仓库为基准仓库，进行对比

				try {
					jsonStrList = searchByArticlenoHandler.compareFilter(jsonStrList);
				} catch (Exception e) {
					// TODO: handle exception
				}
				if (jsonStrList.size() == 1) {
					// 基准仓库或符合条件仓库只有一个
					LOGGER.info("=======下单=======");
					JsonStrAndTrade.put("JsonStr", jsonStrList.get(0));
					Optional<Trade> tradeOptionals = taobaoImpl.getTaobaoTradeFullInfo(tid);
					JsonStrAndTrade.put("Trade", tradeOptionals.get());
					return JsonStrAndTrade;
				} else {
					// 与基准仓库对比后，还剩余2个或两个以上的
					jsonStrList = searchByArticlenoHandler.finalFilter(jsonStrList);
					LOGGER.info("=======下单=======");
					JsonStrAndTrade.put("JsonStr", jsonStrList.get(0));
					Optional<Trade> tradeOptionals = taobaoImpl.getTaobaoTradeFullInfo(tid);
					JsonStrAndTrade.put("Trade", tradeOptionals.get());
					return JsonStrAndTrade;
				}
			}
		} else {
			LOGGER.info("此单不下");
			return null;
		}
	}

	/*
	 * 
	 * 组合出下单所需的内容 jsonStr = jsonStr + '{"wareHouseName":"' + rowData.wareHouse +
	 * '","size":"' + encodeURI(rowData.size) + '","articleno":"' +
	 * rowData.articleno +'","express":"' + rowData.express + '","orderCount":"'
	 * + rowData.orderCount + '","productID":"' + rowData.productID +
	 * '","ercipei":"' + ercipei + '","size1":"' + encodeURI(rowData.size1) +
	 * '","hezi":"' + hezi + '","wareHouseID":' + rowData.wareHouseID +
	 * ',"sku_id":' + rowData.sku +
	 * ',"articleno_old":"' + rowData.articleno_old + '"},'; jsonStr = jsonStr +
	 * ']';
	 *
	 */
	public Map<String, Object> assembly(Map<String, Object> JsonStrAndTrade, String remark) {
		JsonStr jsonStrs = (JsonStr) JsonStrAndTrade.get("JsonStr");
		Map<String, Object> assemblyMap = new HashMap<String, Object>();
		List<TmArea> tmArea0 = tianmaHttp.getArea("0");
		Trade trade = (Trade) JsonStrAndTrade.get("Trade");
		long province_id;
		long city_id;
		long area_id;
		String zipcode;
		String express;
		String recv_name = trade.getReceiverName();
		String recv_mobile = trade.getReceiverMobile();
		String recv_tel = trade.getReceiverPhone();
		String recv_address = trade.getReceiverAddress();
		String province = trade.getReceiverState();
		String city = trade.getReceiverCity();
		String area = trade.getReceiverDistrict();
		province_id = tmArea0.stream().filter(tmArea -> tmArea.getName().equals(province)).collect(Collectors.toList())
				.get(0).getId();
		List<TmArea> tmArea1 = tianmaHttp.getArea(String.valueOf(province_id));
		city_id = tmArea1.stream().filter(tmArea -> tmArea.getName().equals(city)).collect(Collectors.toList()).get(0)
				.getId();
		List<TmArea> tmArea2 = tianmaHttp.getArea(String.valueOf(city_id));
		outside: for (int i = 0; i < tmAreaTransform.length(); i++) {
			for (int j = 0; j < tmArea2.size(); j++) {
				if (tmAreaTransform.getJSONObject(i).getString("Before").equals(tmArea2.get(j).getName())) {
					tmArea2.get(j).setName(tmAreaTransform.getJSONObject(i).getString("Later"));
				}
			}
		}
		tmArea2 = tmArea2.stream().filter(tmArea -> tmArea.getName().equals(area)).collect(Collectors.toList());
		if (tmArea2 == null || tmArea2.size() == 0) {
			// 没有区，不下此单
			LOGGER.info("查不到此地址[" + area + "]");
			long flag = 5;
			String memo = "查不到此地址[" + area + "],请人工处理[" + jsonStrs.getArticleno() + "-" + jsonStrs.getSize().getSize2()
					+ "]";
			org.json.JSONObject msg = taobaoImpl.updateTradeMemo(trade.getTid(), flag, memo);
			LOGGER.info(String.valueOf(msg));
			return null;
		}
		area_id = tmArea2.get(0).getId();
		zipcode = tmArea2.get(0).getZipcode();
		if (zipcode == null || "".equals(zipcode)) {
			zipcode = tmArea1.stream().filter(tmArea -> tmArea.getName().equals(city)).collect(Collectors.toList())
					.get(0).getZipcode();
		}
		String expresses = tianmaHttp.getPostage(jsonStrs.getWareHouseName(), province, jsonStrs.getWeight(),
				jsonStrs.getFirst_w());
		StringBuilder jsonStr = new StringBuilder();
		jsonStr.append("[");
		jsonStr.append("{");
		jsonStr.append("\"wareHouseName\": \"" + jsonStrs.getWareHouseName() + "\",");
		jsonStr.append("\"size\": \"" + jsonStrs.getSize().getSize2() + "\",");
		jsonStr.append("\"articleno\": \"" + jsonStrs.getArticleno() + "\",");
		jsonStr.append("\"express\": \"" + expresses + "\",");
		jsonStr.append("\"orderCount\": \"1\",");
		jsonStr.append("\"productID\": \"" + jsonStrs.getProductId() + "\",");
		jsonStr.append("\"ercipei\": \"1\",");
		jsonStr.append("\"size1\": \"" + jsonStrs.getSize().getSize1() + "\",");
		jsonStr.append("\"hezi\": \"1\",");
		jsonStr.append("\"wareHouseID\":" + jsonStrs.getWareHouseID() + ",");
		jsonStr.append("\"sku_id\":" + jsonStrs.getSize().getSkuId() + ",");
		jsonStr.append("\"articleno_old\": \"" + jsonStrs.getArticleno_old() + "\"");
		jsonStr.append("}");
		jsonStr.append("]");
		assemblyMap.put("jsonStr", jsonStr.toString());
		assemblyMap.put("recv_name", recv_name);
		assemblyMap.put("recv_mobile", recv_mobile);
		assemblyMap.put("recv_tel", recv_tel);
		assemblyMap.put("zipcode", zipcode);
		assemblyMap.put("recv_address", recv_address);
		assemblyMap.put("remark", remark);
		assemblyMap.put("province", province);
		assemblyMap.put("city", city);
		assemblyMap.put("area", area);
		assemblyMap.put("outer_tid", trade.getTid());
		assemblyMap.put("province_id", province_id);
		assemblyMap.put("city_id", city_id);
		assemblyMap.put("area_id", area_id);
		LOGGER.info(String.valueOf(assemblyMap.get("jsonStr")));
		LOGGER.info(String.valueOf(assemblyMap.get("recv_name")));
		LOGGER.info(String.valueOf(assemblyMap.get("recv_mobile")));
		LOGGER.info(String.valueOf(assemblyMap.get("recv_tel")));
		LOGGER.info(String.valueOf(assemblyMap.get("zipcode")));
		LOGGER.info(String.valueOf(assemblyMap.get("recv_address")));
		LOGGER.info(String.valueOf(assemblyMap.get("remark")));
		LOGGER.info(String.valueOf(assemblyMap.get("province")));
		LOGGER.info(String.valueOf(assemblyMap.get("city")));
		LOGGER.info(String.valueOf(assemblyMap.get("area")));
		LOGGER.info(String.valueOf(assemblyMap.get("outer_tid")));
		LOGGER.info(String.valueOf(assemblyMap.get("province_id")));
		LOGGER.info(String.valueOf(assemblyMap.get("city_id")));
		LOGGER.info(String.valueOf(assemblyMap.get("area_id")));
		return assemblyMap;
	}
}
