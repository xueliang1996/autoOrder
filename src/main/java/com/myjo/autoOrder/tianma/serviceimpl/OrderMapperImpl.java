package com.myjo.autoOrder.tianma.serviceimpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.myjo.autoOrder.tianma.domain.WareHouseInfo;
import com.myjo.autoOrder.tianma.mapper.OrderMapper;

@Service
public class OrderMapperImpl implements OrderMapper {

	@Autowired
	private OrderMapper orderMapper;

	@Override
	public void insertOrder(WareHouseInfo wareHouseInfo) {
		orderMapper.insertOrder(wareHouseInfo);
	}

	@Override
	public void insertStatus(String status) {
		// TODO Auto-generated method stub
		orderMapper.insertStatus(status);
	}

	@Override
	public long selectTid(long tid) {

		return orderMapper.selectTid(tid);
	}

}
