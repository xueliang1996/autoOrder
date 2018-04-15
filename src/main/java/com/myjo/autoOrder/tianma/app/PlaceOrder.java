package com.myjo.autoOrder.tianma.app;

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

import com.alibaba.fastjson.JSONObject;
import com.myjo.autoOrder.tianma.domain.JsonStr;
import com.myjo.autoOrder.tianma.domain.TmArea;
import com.myjo.autoOrder.tianma.serviceimpl.SearchByArticlenoImpl;
import com.myjo.autoOrder.tianma.serviceimpl.TaobaoImpl;
import com.myjo.autoOrder.tianma.serviceimpl.TianmaHttp;
import com.myjo.autoOrder.tianma.serviceimpl.TransformDayImpl;
import com.taobao.api.domain.Order;
import com.taobao.api.domain.Trade;

@Component
public class PlaceOrder {

	@Autowired
	private SearchByArticlenoImpl searchByArticlenoHandler;
	@Value("${isBuyerMessage}")
	private boolean isBuyerMessage;
	@Value("${is_weekCheck}")
	private boolean is_weekCheck;
	@Value("${isTime}")
	private boolean isTime;
	@Autowired
	private TransformDayImpl transformDayImpl;
	@Autowired
	private TaobaoImpl taobaoImpl;
	@Autowired
	private TianmaHttp tianmaHttp;
	private static final Logger LOGGER = LoggerFactory.getLogger(SearchByArticlenoImpl.class);

	// 筛选出仓库和订单加入Map中
	public Map<String, Object> addMapInInfo(long tid) {
		Optional<Trade> tradeOptional = taobaoImpl.getTaobaoTrade(tid);
		Map<String, Object> JsonStrAndTrade = new HashMap<String, Object>();
		Trade trade = new Trade();
		if (tradeOptional.isPresent()) {
			trade = tradeOptional.get();
		} else {
			LOGGER.info("订单不存在");
			return null;
		}

		if (trade.getSellerMemo() != null) {
			LOGGER.info("淘宝订单" + tid + ",存在卖家备注" + trade.getSellerMemo() + ",不能自动下单");
			return null;
		}
		if (isBuyerMessage && trade.getBuyerMessage() != null) {
			LOGGER.info("淘宝订单" + tid + ",存在买家留言" + trade.getBuyerMessage() + ",不能自动下单");
			return null;
		}

		if (trade.getIsDaixiao()) {
			LOGGER.info("淘宝订单" + tid + "是代销订单,不能自动下单");
			return null;
		}

		if (trade.getNum() != 1) {
			LOGGER.info("淘宝订单" + tid + ".数量大于[1]不能进行下单.");
			return null;
		}

		Long refundId = trade.getOrders().get(0).getRefundId();
		if (refundId != null) {
			LOGGER.info("淘宝订单" + tid + ".已经存在退款单" + refundId + "不能进行下单.");
			return null;
		}
		if (trade.getStatus().contains("TRADE_CLOSED") || trade.getStatus().contains("TRADE_CLOSED_BY_TAOBAO")) {
			LOGGER.info("淘宝订单" + tid + ".已关闭,不能进行下单");
			return null;
		}
		if (trade.getStatus().contains("TRADE_FINISHED")) {
			LOGGER.info("淘宝订单" + tid + ".已交易成功,不能进行下单");
			return null;
		}
		Order order = trade.getOrders().get(0);
		double payment = Double.parseDouble(trade.getPayment());// 实付金额
		String pay_time = String.valueOf(trade.getPayTime());// 付款时间
		String outer_sku_id = order.getOuterSkuId();// 商家编码
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
			jsonStrList = searchByArticlenoHandler.priceFilter(tid, outer_sku_id, payment);
		} catch (Exception e) {
		}
		if (jsonStrList != null || jsonStrList.size() != 0) {
			// 指定仓库过滤 wareHouseNames

			try {
				jsonStrList = searchByArticlenoHandler.rubbishWareHouseFilter(jsonStrList);
			} catch (Exception e) {
				// TODO: handle exception
			}
			if (jsonStrList == null || jsonStrList.size() == 0) {
				return null;
			}
			// 配货率及库存过滤

			try {
				jsonStrList = searchByArticlenoHandler.pickRateFilter(jsonStrList);
			} catch (Exception e) {
				// TODO: handle exception
			}
			if (jsonStrList == null || jsonStrList.size() == 0) {
				LOGGER.info("天猫标记红旗，备注QH-软件。此订单不下单");
				long flag = 1;
				String memo = "QH-软件";
				// org.json.JSONObject msg = taobaoImpl.addMemo(tid, flag,
				// memo);
				// LOGGER.info(String.valueOf(msg));
				return null;
			}
			// 过滤转发到劲浪下单的仓库及遇到直接下单的仓库

			try {
				jsonStrList = searchByArticlenoHandler.transpondWareHouseFilter(jsonStrList);
			} catch (Exception e) {
				// TODO: handle exception
			}
			if (jsonStrList == null || jsonStrList.size() == 0) {
				LOGGER.info("跳过此过滤条件,重新执行");
				try {
					jsonStrList = searchByArticlenoHandler.priceFilter(tid, outer_sku_id, payment);
					jsonStrList = searchByArticlenoHandler.rubbishWareHouseFilter(jsonStrList);
					jsonStrList = searchByArticlenoHandler.pickRateFilter(jsonStrList);
				} catch (Exception e) {
					// TODO: handle exception
				}

				// 重新执行后还是为空
				if (jsonStrList == null || jsonStrList.size() == 0) {
					LOGGER.info("天猫标记红旗，备注QH-软件。此订单不下单");
					long flag = 1;
					String memo = "QH-软件";
					// org.json.JSONObject msg = taobaoImpl.addMemo(tid, flag,
					// memo);
					// LOGGER.info(String.valueOf(msg));
					return null;
				}
			} else if (jsonStrList.get(0).isFlag() == true) {
				if (jsonStrList.size() == 1) {
					// 只有一个遇到直接下单仓库
					LOGGER.info("=======下单=======");
					System.out.println(jsonStrList);
					JsonStrAndTrade.put("JsonStr", jsonStrList.get(0));
					Optional<Trade> tradeOptionals = taobaoImpl.getTaobaoTradeFullInfo(tid);
					JsonStrAndTrade.put("Trade", tradeOptionals.get());
					return JsonStrAndTrade;
				} else {
					// 遇到直接下单仓库多个
					LOGGER.info("=======下单=======");
					List<JsonStr> jsonStrFliterList = new ArrayList<JsonStr>();
					jsonStrFliterList.add(jsonStrList.get(0));
					System.out.println(jsonStrList);
					JsonStrAndTrade.put("JsonStr", jsonStrFliterList.get(0));
					Optional<Trade> tradeOptionals = taobaoImpl.getTaobaoTradeFullInfo(tid);
					JsonStrAndTrade.put("Trade", tradeOptionals.get());
					return JsonStrAndTrade;
				}
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
			if (jsonStrList == null || jsonStrList.size() == 0) {
				LOGGER.info("天猫标记红旗，备注QH-软件。此订单不下单");
				long flag = 1;
				String memo = "QH-软件";
				// org.json.JSONObject msg = taobaoImpl.addMemo(tid, flag,
				// memo);
				// LOGGER.info(String.valueOf(msg));
				return null;
			}
			if (jsonStrList.size() == 1) {
				// 符合的仓库只有一个
				LOGGER.info("=======下单=======");
				System.out.println(jsonStrList);
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
					System.out.println(jsonStrList);
					JsonStrAndTrade.put("JsonStr", jsonStrList.get(0));
					Optional<Trade> tradeOptionals = taobaoImpl.getTaobaoTradeFullInfo(tid);
					JsonStrAndTrade.put("Trade", tradeOptionals.get());
					return JsonStrAndTrade;
				} else {
					// 与基准仓库对比后，还剩余2个或两个以上的
					jsonStrList = searchByArticlenoHandler.finalFilter(jsonStrList);
					LOGGER.info("=======下单=======");
					System.out.println(jsonStrList);
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
	public Map<String, Object> assembly(Map<String, Object> JsonStrAndTrade) {
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
		String remark = "软件下单";
		String province = trade.getReceiverState();
		String city = trade.getReceiverCity();
		String area = trade.getReceiverDistrict();
		province_id = tmArea0.stream().filter(tmArea -> tmArea.getName().equals(province)).collect(Collectors.toList())
				.get(0).getId();
		List<TmArea> tmArea1 = tianmaHttp.getArea(String.valueOf(province_id));
		city_id = tmArea1.stream().filter(tmArea -> tmArea.getName().equals(city)).collect(Collectors.toList()).get(0)
				.getId();
		List<TmArea> tmArea2 = tianmaHttp.getArea(String.valueOf(city_id));
		List<TmArea> areaList = tmArea2.stream().filter(tmArea -> tmArea.getName().equals(area))
				.collect(Collectors.toList());
		if (areaList == null) {
			// 没有区，不下此单
			LOGGER.info("查不到第三级区县，不下此单");
			long flag = 5;
			String memo = "查不到第三级区县";
			// org.json.JSONObject msg = taobaoImpl.addMemo(tid, flag,
			// memo);
			// LOGGER.info(String.valueOf(msg));
			return null;
		}
		area_id = areaList.get(0).getId();
		zipcode = areaList.get(0).getZipcode();
		JSONObject postage = tianmaHttp.getPostage(jsonStrs.getWareHouseName(), province, jsonStrs.getWeight(),
				jsonStrs.getFirst_w());
		express = postage.getString("expressName");
		StringBuilder jsonStr = new StringBuilder();
		jsonStr.append("[");
		jsonStr.append("{");
		jsonStr.append("\"wareHouseName\": \"" + jsonStrs.getWareHouseName() + "\",");
		jsonStr.append("\"size\": \"" + jsonStrs.getSize().getSize2() + "\",");
		jsonStr.append("\"articleno\": \"" + jsonStrs.getArticleno() + "\",");
		jsonStr.append("\"express\": \"" + express + "\",");
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
		return assemblyMap;
	}
}
