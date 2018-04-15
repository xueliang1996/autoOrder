package com.myjo.autoOrder.tianma.serviceimpl;

import java.util.Optional;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.myjo.autoOrder.tianma.domain.TaobaoAPI;
import com.myjo.autoOrder.tianma.service.TaobaoService;
import com.taobao.api.ApiException;
import com.taobao.api.DefaultTaobaoClient;
import com.taobao.api.TaobaoClient;
import com.taobao.api.domain.Trade;
import com.taobao.api.request.TradeFullinfoGetRequest;
import com.taobao.api.request.TradeGetRequest;
import com.taobao.api.request.TradeMemoAddRequest;
import com.taobao.api.response.TradeFullinfoGetResponse;
import com.taobao.api.response.TradeGetResponse;
import com.taobao.api.response.TradeMemoAddResponse;

@Service
public class TaobaoImpl implements TaobaoService {

	private static final Logger LOGGER = LoggerFactory.getLogger(TaobaoImpl.class);
	@Autowired
	private TaobaoAPI taobaoAPI;

	@Override
	public JSONObject addMemo(long tid, long flag, String memo) {
		TaobaoClient client = new DefaultTaobaoClient(taobaoAPI.getTaobaoUrl(), taobaoAPI.getAppkey(),
				taobaoAPI.getSecret());
		TradeMemoAddRequest req = new TradeMemoAddRequest();
		try {
			req.setTid(tid);
			req.setMemo(memo);
			req.setFlag(flag);
			TradeMemoAddResponse rsp;

			rsp = client.execute(req, taobaoAPI.getSessionKey());
			JSONObject json = new JSONObject(rsp.getBody());
			return json;
		} catch (ApiException e) {
			LOGGER.info("修改备注及标旗失败");
			return null;
		}
	}

	@Override
	public Optional<Trade> getTaobaoTrade(long tid) {
		Trade trade = null;
		TaobaoClient client = new DefaultTaobaoClient(taobaoAPI.getTaobaoUrl(), taobaoAPI.getAppkey(),
				taobaoAPI.getSecret());
		TradeGetRequest req = new TradeGetRequest();

		req.setFields(
				"tid,title,buyer_nick,type,status,num,payment,orders,created,pay_time,price,discount_fee,total_fee,is_daixiao");
		req.setTid(tid);
		try {
			TradeGetResponse rsp = client.execute(req, taobaoAPI.getSessionKey());
			if (rsp.isSuccess()) {
				trade = rsp.getTrade();
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		}
		return Optional.ofNullable(trade);
	}

	@Override
	public Optional<Trade> getTaobaoTradeFullInfo(long tid) {

		// receiver_name String 东方不败收货人的姓名
		// receiver_state String 浙江省收货人的所在省份
		// receiver_address String 淘宝城911号收货人的详细地址
		// receiver_zip String 223700收货人的邮编
		// receiver_mobile String 13512501826收货人的手机号码
		// receiver_phone String 13819175372收货人的电话号码
		// consign_time Date 2000-01-01 00:00:00卖家发货时间。格式:yyyy-MM-dd HH:mm:ss
		// received_payment String
		// 200.07卖家实际收到的支付宝打款金额（由于子订单可以部分确认收货，这个金额会随着子订单的确认收货而不断增加，交易成功后等于买家实付款减去退款金额）。精确到2位小数;单位:元。如:200.07，表示:200元7分
		// receiver_city 所在城市
		// receiver_district 所在地区

		Trade trade = null;
		TaobaoClient client = new DefaultTaobaoClient(taobaoAPI.getTaobaoUrl(), taobaoAPI.getAppkey(),
				taobaoAPI.getSecret());
		TradeFullinfoGetRequest req = new TradeFullinfoGetRequest();
		req.setFields(
				"tid,type,created,seller_memo,buyer_nick,seller_flag,status,num,payment,orders,receiver_name,receiver_state,receiver_address,receiver_zip,receiver_mobile,receiver_phone,received_payment,receiver_city,receiver_district,buyer_message,is_daixiao,orders.refund_id");
		req.setTid(tid);
		try {
			TradeFullinfoGetResponse rsp = client.execute(req, taobaoAPI.getSessionKey());
			if (rsp.isSuccess()) {
				trade = rsp.getTrade();
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		}
		return Optional.ofNullable(trade);
	}

	@Override
	public void updateTradeMemo(long tid, long flag, String memo) {
		// TODO Auto-generated method stub

	}

}
