package com.myjo.autoOrder.tianma.domain;

import org.springframework.stereotype.Component;

@Component
public class WareHouseInfo {

	private int id;
	private long order_id;// 订单编号
	private String buyer_nick;// 买家昵称
	private double payment;// 实际支付金额
	private long tm_order_id;// 天马订单编号
	private String outer_sku_id;// 货号
	private String chineseSize;// 尺码
	private String wareHouseName;// 货源名称
	private double proxyPrice;// 代理价
	private int inventory;// 库存
	private String pickRate;// 配货率
	private String timeEfficacy;// 发货时效
	private String updateTime;// 库存更新时间
	private String pay_time;// 付款时间
	private String fix_up;// 是否下单

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getFix_up() {
		return fix_up;
	}

	public void setFix_up(String fix_up) {
		this.fix_up = fix_up;
	}

	public String getBuyer_nick() {
		return buyer_nick;
	}

	public void setBuyer_nick(String buyer_nick) {
		this.buyer_nick = buyer_nick;
	}

	public double getPayment() {
		return payment;
	}

	public void setPayment(double payment) {
		this.payment = payment;
	}

	public String getOuter_sku_id() {
		return outer_sku_id;
	}

	public void setOuter_sku_id(String outer_sku_id) {
		this.outer_sku_id = outer_sku_id;
	}

	public String getChineseSize() {
		return chineseSize;
	}

	public void setChineseSize(String chineseSize) {
		this.chineseSize = chineseSize;
	}

	public String getWareHouseName() {
		return wareHouseName;
	}

	public void setWareHouseName(String wareHouseName) {
		this.wareHouseName = wareHouseName;
	}

	public double getProxyPrice() {
		return proxyPrice;
	}

	public void setProxyPrice(double proxyPrice) {
		this.proxyPrice = proxyPrice;
	}

	public int getInventory() {
		return inventory;
	}

	public void setInventory(int inventory) {
		this.inventory = inventory;
	}

	public String getPickRate() {
		return pickRate;
	}

	public void setPickRate(String pickRate) {
		this.pickRate = pickRate;
	}

	public String getTimeEfficacy() {
		return timeEfficacy;
	}

	public void setTimeEfficacy(String timeEfficacy) {
		this.timeEfficacy = timeEfficacy;
	}

	public String getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(String updateTime) {
		this.updateTime = updateTime;
	}

	public long getOrder_id() {
		return order_id;
	}

	public void setOrder_id(long order_id) {
		this.order_id = order_id;
	}

	public long getTm_order_id() {
		return tm_order_id;
	}

	public void setTm_order_id(long tm_order_id) {
		this.tm_order_id = tm_order_id;
	}

	public String getPay_time() {
		return pay_time;
	}

	public void setPay_time(String pay_time) {
		this.pay_time = pay_time;
	}

	@Override
	public String toString() {
		return "WareHouseInfo [id=" + id + ", order_id=" + order_id + ", buyer_nick=" + buyer_nick + ", payment="
				+ payment + ", tm_order_id=" + tm_order_id + ", outer_sku_id=" + outer_sku_id + ", chineseSize="
				+ chineseSize + ", wareHouseName=" + wareHouseName + ", proxyPrice=" + proxyPrice + ", inventory="
				+ inventory + ", pickRate=" + pickRate + ", timeEfficacy=" + timeEfficacy + ", updateTime=" + updateTime
				+ ", pay_time=" + pay_time + ", fix_up=" + fix_up + "]";
	}

}
