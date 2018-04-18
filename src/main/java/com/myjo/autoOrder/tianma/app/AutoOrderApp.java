package com.myjo.autoOrder.tianma.app;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.aliyun.openservices.ons.api.Action;
import com.aliyun.openservices.ons.api.ConsumeContext;
import com.aliyun.openservices.ons.api.Consumer;
import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.ons.api.MessageListener;
import com.aliyun.openservices.ons.api.ONSFactory;
import com.aliyun.openservices.ons.api.PropertyKeyConst;
import com.myjo.autoOrder.tianma.domain.JsonStr;
import com.myjo.autoOrder.tianma.domain.TaobaoAPI;
import com.myjo.autoOrder.tianma.domain.WareHouseInfo;
import com.myjo.autoOrder.tianma.serviceimpl.OrderMapperImpl;
import com.myjo.autoOrder.tianma.serviceimpl.PlaceOrder;
import com.myjo.autoOrder.tianma.serviceimpl.TaobaoImpl;
import com.myjo.autoOrder.tianma.serviceimpl.TianmaHttp;
import com.myjo.autoOrder.tianma.util.CsvFileUtil;
import com.taobao.api.domain.Order;
import com.taobao.api.domain.Trade;

@Component
public class AutoOrderApp implements CommandLineRunner {
	@Autowired
	private TaobaoAPI taobaoAPI;
	private static final Logger LOGGER = LoggerFactory.getLogger(AutoOrderApp.class);
	@Autowired
	private PlaceOrder placeOrder;
	@Autowired
	private TianmaHttp tianmaHttp;
	@Value("${pay_password}")
	private String pay_password;
	@Value("${isBuyerMessage}")
	private boolean isBuyerMessage;
	@Autowired
	private TaobaoImpl taobaoImpl;
	@Autowired
	private OrderMapperImpl orderMapperImpl;
	@Value("${threadNumber}")
	private long threadNumber;
	@Value("${threadAstrictStart}")
	private boolean threadAstrictStart;

	public void startApp() {
		Properties properties = new Properties();
		properties.put(PropertyKeyConst.ConsumerId, "CID_MG_02");
		properties.put(PropertyKeyConst.AccessKey, taobaoAPI.getAppkey());
		properties.put(PropertyKeyConst.SecretKey, taobaoAPI.getSecret());
		if (threadAstrictStart) {
			properties.put(PropertyKeyConst.ConsumeThreadNums, threadNumber);
		}
		Consumer consumer = ONSFactory.createConsumer(properties);
		consumer.subscribe("rmq_sys_jst_23279400", "*", new MessageListener() {
			public Action consume(Message message, ConsumeContext context) {

				System.out.println("Receive: " + message);
				long tid = Long.parseLong(message.getKey());

				String fileName = "E:\\oc2\\log\\autoOrderInfo.csv";
				CsvFileUtil csvFileUtil = null;
				boolean isUpdataMemo = true;
				boolean isSecond = false;
				String lastWarehouseName = "";
				try {
					csvFileUtil = new CsvFileUtil(fileName, true);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				try {
					LOGGER.info("订单编号" + tid);
					if (orderMapperImpl.selectTid(tid) != null) {
						LOGGER.info("订单已存在");
						return Action.CommitMessage;
					}
					Optional<Trade> tradeOptional = taobaoImpl.getTaobaoTrade(tid);
					Trade trade = new Trade();
					if (tradeOptional.isPresent()) {
						trade = tradeOptional.get();
					} else {
						LOGGER.info("订单不存在");
						return Action.CommitMessage;
					}

					if (trade.getSellerMemo() != null) {
						LOGGER.info("淘宝订单" + tid + ",存在卖家备注" + trade.getSellerMemo() + ",不能自动下单");
						return Action.CommitMessage;
					}
					if (isBuyerMessage && trade.getBuyerMessage() != null) {
						LOGGER.info("淘宝订单" + tid + ",存在买家留言" + trade.getBuyerMessage() + ",不能自动下单");
						return Action.CommitMessage;
					}

					if (trade.getIsDaixiao()) {
						LOGGER.info("淘宝订单" + tid + "是代销订单,不能自动下单");
						return Action.CommitMessage;
					}

					if (trade.getStatus().contains("TRADE_CLOSED")
							|| trade.getStatus().contains("TRADE_CLOSED_BY_TAOBAO")) {
						LOGGER.info("淘宝订单" + tid + ".已关闭,不能进行下单");
						return Action.CommitMessage;
					}
					if (trade.getStatus().contains("TRADE_FINISHED")) {
						LOGGER.info("淘宝订单" + tid + ".已交易成功,不能进行下单");
						return Action.CommitMessage;
					}

					for (int i = 0; i < trade.getOrders().size(); i++) {
						Order order = trade.getOrders().get(i);
						Map<String, Object> map1;
						try {
							map1 = placeOrder.addMapInInfo(tid, trade, order, isSecond, lastWarehouseName);
						} catch (Exception e) {
							System.out.println(e.getMessage());
							isUpdataMemo = false;
							continue;
						}

						if (map1 != null || map1.size() != 0) {
							Map<String, Object> map2;
							try {
								String remark = "OC自动下单";
								map2 = placeOrder.assembly(map1, remark);
							} catch (Exception e) {
								System.out.println(e.getMessage());
								continue;
							}

							if (map2 != null || map2.size() != 0) {
								String rp = tianmaHttp.orderBooking(map2);
								// 下单并把相关内容写进数据库
								int status = 0;// 未付款
								Map<String, Long> map3 = tianmaHttp.myDataList(tid, status);
								WareHouseInfo wareHouseInfo = new WareHouseInfo();
								wareHouseInfo.setOrder_id(tid);
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
								wareHouseInfo.setOrderType("自动下单");
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
										csvFileUtil.write(str);
										// 天猫订单标记绿旗，备注132-软件
										if (isUpdataMemo) {
											long flag = 3;
											String memo = "OC下单成功,天马ID:[" + wareHouseInfo.getTm_order_id() + "]";
											org.json.JSONObject msg = taobaoImpl.updateTradeMemo(tid, flag, memo);
											LOGGER.info(String.valueOf(msg));
											LOGGER.info("下单成功" + tid);
										}

									} else {
										// 天猫订单标记紫旗
										long flag = 5;
										String memo = "软件下单失败,失败原因:" + rt;
										org.json.JSONObject msg = taobaoImpl.updateTradeMemo(tid, flag, memo);
										LOGGER.info(String.valueOf(msg));
									}

								}

							}
						} else {
							isUpdataMemo = false;
						}
					}

				} catch (Exception e) {
					LOGGER.info(e.getMessage());
				} finally {
					csvFileUtil.close();
				}

				return Action.CommitMessage;
			}
		});
		consumer.start();
		System.out.println("Consumer Started");

	}

	@Override
	@Async
	public void run(String... arg0) throws Exception {
		try {
			startApp();
			String status = "success";
			orderMapperImpl.insertStatus(status);
		} catch (Exception e) {
			e.printStackTrace();
			// 失败状态写入数据库
			String status = "failure";
			orderMapperImpl.insertStatus(status);
		}
	}
}
