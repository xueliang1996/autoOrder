package com.myjo.autoOrder.tianma.domain;

import org.springframework.stereotype.Component;

@Component
public class JsonStr {
	private int wareHouseID;// 货源id
	private String productId;// 产品id
	private String wareHouseName;// 货源名称
	private String articleno;// 货号
	private String articleno_old;// 货号
	private double proxyPrice;// 代理价
	private String pick_date;// 配货时间
	private String expressName;// 支持快递
	private String pickRate;// 配货率
	private String timeEfficacy;// 发货时效
	private String updateTime;// 更新时间
	private String weight;// 重量
	private double first_w;// 不知道什么鬼
	private Size size;// 尺码
	private boolean flag = false;

	public int getWareHouseID() {
		return wareHouseID;
	}

	public void setWareHouseID(int wareHouseID) {
		this.wareHouseID = wareHouseID;
	}

	public double getProxyPrice() {
		return proxyPrice;
	}

	public void setProxyPrice(double proxyPrice) {
		this.proxyPrice = proxyPrice;
	}

	public String getPick_date() {
		return pick_date;
	}

	public void setPick_date(String pick_date) {
		this.pick_date = pick_date;
	}

	public String getExpressName() {
		return expressName;
	}

	public void setExpressName(String expressName) {
		this.expressName = expressName;
	}

	public String getPickRate() {
		return pickRate;
	}

	public void setPickRate(String pickRate) {
		this.pickRate = pickRate;
	}

	public String getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(String updateTime) {
		this.updateTime = updateTime;
	}

	public String getArticleno() {
		return articleno;
	}

	public void setArticleno(String articleno) {
		this.articleno = articleno;
	}

	public String getArticleno_old() {
		return articleno_old;
	}

	public void setArticleno_old(String articleno_old) {
		this.articleno_old = articleno_old;
	}

	public String getWareHouseName() {
		return wareHouseName;
	}

	public void setWareHouseName(String wareHouseName) {
		this.wareHouseName = wareHouseName;
	}

	public String getProductId() {
		return productId;
	}

	public void setProductId(String productId) {
		this.productId = productId;
	}

	public String getTimeEfficacy() {
		return timeEfficacy;
	}

	public void setTimeEfficacy(String timeEfficacy) {
		this.timeEfficacy = timeEfficacy;
	}

	public Size getSize() {
		return size;
	}

	public void setSize(Size size) {
		this.size = size;
	}

	public boolean isFlag() {
		return flag;
	}

	public void setFlag(boolean flag) {
		this.flag = flag;
	}

	public String getWeight() {
		return weight;
	}

	public void setWeight(String weight) {
		this.weight = weight;
	}

	public double getFirst_w() {
		return first_w;
	}

	public void setFirst_w(double first_w) {
		this.first_w = first_w;
	}

	@Override
	public String toString() {
		return "JsonStr [wareHouseID=" + wareHouseID + ", productId=" + productId + ", wareHouseName=" + wareHouseName
				+ ", articleno=" + articleno + ", articleno_old=" + articleno_old + ", proxyPrice=" + proxyPrice
				+ ", pick_date=" + pick_date + ", expressName=" + expressName + ", pickRate=" + pickRate
				+ ", timeEfficacy=" + timeEfficacy + ", updateTime=" + updateTime + ", weight=" + weight + ", first_w="
				+ first_w + ", size=" + size + ", flag=" + flag + "]";
	}

}
