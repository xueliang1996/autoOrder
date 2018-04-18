package com.myjo.autoOrder.tianma.app;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.myjo.autoOrder.tianma.domain.JsonStr;
import com.myjo.autoOrder.tianma.domain.WareHouseInfo;
import com.myjo.autoOrder.tianma.serviceimpl.OrderMapperImpl;
import com.myjo.autoOrder.tianma.serviceimpl.PlaceOrder;
import com.myjo.autoOrder.tianma.serviceimpl.TaobaoImpl;
import com.myjo.autoOrder.tianma.serviceimpl.TianmaHttp;
import com.myjo.autoOrder.tianma.serviceimpl.TransformDayImpl;
import com.myjo.autoOrder.tianma.util.CsvFileUtil;
import com.taobao.api.domain.Order;
import com.taobao.api.domain.Trade;

@Component
public class ReplacementOrderApp {

	@Autowired
	TianmaHttp tianmaHttp;
	@Autowired
	TransformDayImpl transformDayImpl;
	@Autowired
	TaobaoImpl taobaoImpl;
	@Value("${pay_password}")
	private String pay_password;
	@Value("${isBuyerMessage}")
	private boolean isBuyerMessage;
	@Autowired
	private OrderMapperImpl orderMapperImpl;
	@Autowired
	private PlaceOrder placeOrder;
	private static final Logger LOGGER = LoggerFactory.getLogger(ReplacementOrderApp.class);

	@Async
	@Scheduled(fixedDelayString = "${ReplacementOrder.time}")
	public void runReplacementOrder() {
		LOGGER.info("=====开始自动补单=====");
		int status = 100;
		String fileName = "E:\\oc2\\log\\ReplacementOrderInfo.csv";
		CsvFileUtil csvFileUtil = null;
		boolean isUpdataMemo = true;
		boolean isSecond = true;
		try {
			csvFileUtil = new CsvFileUtil(fileName, true);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		List<com.alibaba.fastjson.JSONObject> jsonList = null;
		try {
			jsonList = tianmaHttp.myRefundDataList(status);
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			// 付款时间距离当前时间24小时以内，订单备注为OC自动下单
			jsonList = jsonList.stream()
					.filter(json -> transformDayImpl.getMillisecond(json.getString("created"))
							&& json.getString("trade_remark") != null && !"".equals(json.getString("trade_remark"))
							&& json.getString("trade_remark").contains("OC自动下单"))
					.collect(Collectors.toList());
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			// 查询订单数目大于1过滤
			jsonList = jsonList.stream().filter(json -> tianmaHttp.myRefundDataList(json.getString("outer_tid")))
					.collect(Collectors.toList());
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (jsonList.size() > 0 || jsonList != null) {
			for (int j = 0; j < jsonList.size(); j++) {
				com.alibaba.fastjson.JSONObject json = jsonList.get(j);
				// 下单流程
				Optional<Trade> tradeOptional = taobaoImpl.getTaobaoTrade(Long.parseLong(json.getString("outer_tid")));
				Trade trade = tradeOptional.get();
				if (trade.getNum() == 1) {
					if (trade.getStatus().contains("WAIT_SELLER_SEND_GOODS") && trade.getSellerMemo() != null
							&& trade.getSellerMemo().contains("OC下单成功")) {
						Order order = trade.getOrders().get(0);
						Map<String, Object> map1;
						try {
							map1 = placeOrder.addMapInInfo(Long.parseLong(json.getString("outer_tid")), trade, order,
									isSecond, json.getString("m_warehouse_name"));
						} catch (Exception e) {
							e.printStackTrace();
							continue;
						}
						if (map1 != null || map1.size() != 0) {
							Map<String, Object> map2;
							try {
								String remark = "OC自动补单";
								map2 = placeOrder.assembly(map1, remark);
							} catch (Exception e) {
								e.printStackTrace();
								continue;
							}
							if (map2 != null || map2.size() != 0) {
								String rp = tianmaHttp.orderBooking(map2);
								// 下单并把相关内容写进数据库
								status = 0;// 未付款
								Map<String, Long> map3 = tianmaHttp
										.myDataList(Long.parseLong(json.getString("outer_tid")), status);
								WareHouseInfo wareHouseInfo = new WareHouseInfo();
								wareHouseInfo.setOrder_id(Long.parseLong(json.getString("outer_tid")));
								wareHouseInfo.setPay_time(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
										.format(((Trade) map1.get("Trade")).getPayTime()));
								wareHouseInfo.setBuyer_nick(((Trade) map1.get("Trade")).getBuyerNick());
								wareHouseInfo.setPayment(Double.parseDouble(((Trade) map1.get("Trade")).getPayment()));
								wareHouseInfo.setTm_order_id(map3.get("order_id"));// map3.get("order_id")
								wareHouseInfo.setOuter_sku_id(((JsonStr) map1.get("JsonStr")).getArticleno());
								wareHouseInfo.setChineseSize(((JsonStr) map1.get("JsonStr")).getSize().getSize2());
								wareHouseInfo.setWareHouseName(((JsonStr) map1.get("JsonStr")).getWareHouseName());
								wareHouseInfo.setProxyPrice(((JsonStr) map1.get("JsonStr")).getProxyPrice());
								wareHouseInfo.setInventory(((JsonStr) map1.get("JsonStr")).getSize().getInventory());
								wareHouseInfo.setPickRate(((JsonStr) map1.get("JsonStr")).getPickRate());
								wareHouseInfo.setTimeEfficacy(((JsonStr) map1.get("JsonStr")).getTimeEfficacy());
								wareHouseInfo.setUpdateTime(((JsonStr) map1.get("JsonStr")).getUpdateTime());
								wareHouseInfo.setOrderType("自动补单");
								long orderIDs = map3.get("orderIDs");
								if (orderIDs != 0) {
									String rt = tianmaHttp.updataBalance(orderIDs, pay_password);
									if (!rt.contains("账户余额不足")) {
										orderMapperImpl.insertOrder(wareHouseInfo);
										String[] str = { wareHouseInfo.getOrder_id() + "\t",
												wareHouseInfo.getBuyer_nick() + "\t", wareHouseInfo.getPayment() + "\t",
												wareHouseInfo.getTm_order_id() + "\t", wareHouseInfo.getOuter_sku_id(),
												wareHouseInfo.getChineseSize(), wareHouseInfo.getWareHouseName(),
												wareHouseInfo.getProxyPrice() + "\t",
												wareHouseInfo.getInventory() + "\t", wareHouseInfo.getPickRate(),
												wareHouseInfo.getTimeEfficacy(), wareHouseInfo.getPay_time(),
												wareHouseInfo.getUpdateTime(), wareHouseInfo.getOrderType() };
										try {
											csvFileUtil.write(str);
										} catch (IOException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
										// 天猫订单标记绿旗，备注132-软件
										if (isUpdataMemo) {
											long flag = 3;
											String memo = "OC补单成功,天马ID:[" + wareHouseInfo.getTm_order_id() + "]";
											org.json.JSONObject msg = taobaoImpl.updateTradeMemo(
													Long.parseLong(json.getString("outer_tid")), flag, memo);
											LOGGER.info(String.valueOf(msg));
											LOGGER.info("补单成功" + Long.parseLong(json.getString("outer_tid")));
										}

									} else {
										// 天猫订单标记紫旗
										long flag = 5;
										String memo = "软件下单失败,失败原因:" + rt;
										org.json.JSONObject msg = taobaoImpl.updateTradeMemo(
												Long.parseLong(json.getString("outer_tid")), flag, memo);
										LOGGER.info(String.valueOf(msg));
									}

								}

							}
						}
					} else {
						LOGGER.info("订单[" + trade.getTid() + "]" + "不符合条件");
					}
				}
			}
		}

		csvFileUtil.close();
		LOGGER.info("=====补单结束=====");
	}
}
