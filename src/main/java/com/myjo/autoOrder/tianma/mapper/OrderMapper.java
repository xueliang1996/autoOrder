package com.myjo.autoOrder.tianma.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import com.myjo.autoOrder.tianma.domain.WareHouseInfo;

@Repository
public interface OrderMapper {

	@Insert("INSERT INTO auto_order_info(order_id,buyer_nick,payment,tm_order_id,outer_sku_id,chineseSize,wareHouseName,proxyPrice,inventory,pickRate,timeEfficacy,pay_time,updateTime,fix_up) VALUES(#{wareHouseInfo.order_id}, #{wareHouseInfo.buyer_nick}, #{wareHouseInfo.payment}, #{wareHouseInfo.tm_order_id}, #{wareHouseInfo.outer_sku_id}, #{wareHouseInfo.chineseSize}, #{wareHouseInfo.wareHouseName}, #{wareHouseInfo.proxyPrice}, #{wareHouseInfo.inventory}, #{wareHouseInfo.pickRate}, #{wareHouseInfo.timeEfficacy}, #{wareHouseInfo.pay_time}, #{wareHouseInfo.updateTime}, #{wareHouseInfo.fix_up})")
	public void insertOrder(@Param("wareHouseInfo") WareHouseInfo wareHouseInfo);

	@Insert("INSERT INTO auto_status(status) VALUES(#{status})")
	public void insertStatus(@Param("status") String status);

	@Select("SELECT order_id FROM auto_order_info where order_id=#{tid}")
	public long selectTid(@Param("tid") long tid);
}
