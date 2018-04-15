package com.myjo.autoOrder.tianma.domain;

import org.springframework.stereotype.Component;

@Component
public class Size {
	// 8<>41<>1229488,8.5<>42<>1229483,9<>42.5<>1229489,9.5<>43<>1229487,10<>44<>1229485,10.5<>44.5<>1229491
	private String size1;
	private String size2;
	private String skuId;
	private int inventory;

	public String getSize1() {
		return size1;
	}

	public void setSize1(String size1) {
		this.size1 = size1;
	}

	public String getSize2() {
		return size2;
	}

	public void setSize2(String size2) {
		this.size2 = size2;
	}

	public String getSkuId() {
		return skuId;
	}

	public void setSkuId(String skuId) {
		this.skuId = skuId;
	}

	public int getInventory() {
		return inventory;
	}

	public void setInventory(int inventory) {
		this.inventory = inventory;
	}

	@Override
	public String toString() {
		return "Size [size1=" + size1 + ", size2=" + size2 + ", skuId=" + skuId + ", inventory=" + inventory + "]";
	}

}
