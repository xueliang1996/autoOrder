package com.myjo.autoOrder.tianma.service;

import java.util.Optional;

import org.json.JSONObject;

import com.taobao.api.domain.Trade;

public interface TaobaoService {
	// 添加淘宝备注
	public JSONObject addMemo(long tid, long flag, String memo);

	// 修改淘宝备注
	public JSONObject updateTradeMemo(long tid, long flag, String memo);

	// taobao.trade.get (获取单笔交易的部分信息(性能高))
	public Optional<Trade> getTaobaoTrade(long tid);

	// taobao.trade.fullinfo.get
	public Optional<Trade> getTaobaoTradeFullInfo(long tid);
}
