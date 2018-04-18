package com.myjo.autoOrder.tianma.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.myjo.autoOrder.tianma.domain.WareHouseInfo;

public interface OrderMapper {

	@Insert("INSERT INTO oc2_autoorder_info(order_id,buyer_nick,payment,tm_order_id,outer_sku_id,chineseSize,wareHouseName,proxyPrice,inventory,pickRate,timeEfficacy,pay_time,updateTime,orderType) VALUES(#{wareHouseInfo.order_id}, #{wareHouseInfo.buyer_nick}, #{wareHouseInfo.payment}, #{wareHouseInfo.tm_order_id}, #{wareHouseInfo.outer_sku_id}, #{wareHouseInfo.chineseSize}, #{wareHouseInfo.wareHouseName}, #{wareHouseInfo.proxyPrice}, #{wareHouseInfo.inventory}, #{wareHouseInfo.pickRate}, #{wareHouseInfo.timeEfficacy}, #{wareHouseInfo.pay_time}, #{wareHouseInfo.updateTime}, #{wareHouseInfo.orderType})")
	public void insertOrder(@Param("wareHouseInfo") WareHouseInfo wareHouseInfo);

	@Insert("INSERT INTO oc2_autoorder_status(status) VALUES(#{status})")
	public void insertStatus(@Param("status") String status);

	@Select("SELECT t.order_id FROM (SELECT order_id FROM oc2_autoorder_info where pay_time>=DATE_SUB(CURDATE(), INTERVAL 3 MONTH)) t where order_id=#{tid}")
	public Long selectTid(@Param("tid") long tid);

	@Select("SELECT status FROM oc2_autoorder_status where id=#{id}")
	public String selectStatus(@Param("id") long id);

	// 当前时间三个月内的订单编号
	@Select("SELECT t.order_id FROM (SELECT order_id FROM oc2_autoorder_info where pay_time>=DATE_SUB(CURDATE(), INTERVAL 3 MONTH)) t")
	public List<Long> selectTids();
}
