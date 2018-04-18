package com.myjo.autoOrder.tianma.serviceimpl;

import java.util.List;

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
	public Long selectTid(long tid) {

		return orderMapper.selectTid(tid);
	}

	@Override
	public String selectStatus(long id) {
		// TODO Auto-generated method stub
		return orderMapper.selectStatus(id);
	}

	@Override
	public List<Long> selectTids() {
		// TODO Auto-generated method stub
		return orderMapper.selectTids();
	}

}
