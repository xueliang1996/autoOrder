package com.myjo.autoOrder.tianma.service;

import java.util.List;

import com.myjo.autoOrder.tianma.domain.JsonStr;

/*
 * 
 * 过滤仓库
 * 
 */
public interface SearchByArticleno {
	// 保本价过滤
	public List<JsonStr> priceFilter(long tid, String articleno, double price, boolean isSecond);

	// 指定仓库过滤
	public List<JsonStr> rubbishWareHouseFilter(List<JsonStr> jsonStrList, boolean isSecond, String lastWarehouseName);

	// 配货率及库存过滤
	public List<JsonStr> pickRateFilter(List<JsonStr> jsonStrList);

	// 过滤转发到劲浪下单的仓库及遇到直接下单的仓库
	public List<JsonStr> transpondWareHouseFilter(List<JsonStr> jsonStrList);

	// 如果下单时间为周五4:00-周六4:00，则过滤周六周日不发货仓库，如果下单时间为周六4:00-周日4:00，则过滤周日不发货仓库
	public List<JsonStr> orderTimeFilter(List<JsonStr> jsonStrList, String orderTime);

	// 符合的仓库有两个及两个以上,以符合条件的价格最低的仓库为基准仓库，进行对比
	public List<JsonStr> compareFilter(List<JsonStr> jsonStrList);

	// 与基准仓库对比后，还剩余2个或两个以上的
	public List<JsonStr> finalFilter(List<JsonStr> jsonStrList);

}
