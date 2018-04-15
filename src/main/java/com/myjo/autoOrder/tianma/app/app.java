package com.myjo.autoOrder.tianma.app;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.aliyun.openservices.ons.api.Action;
import com.aliyun.openservices.ons.api.ConsumeContext;
import com.aliyun.openservices.ons.api.Consumer;
import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.ons.api.MessageListener;
import com.aliyun.openservices.ons.api.ONSFactory;
import com.aliyun.openservices.ons.api.PropertyKeyConst;
import com.csvreader.CsvWriter;
import com.myjo.autoOrder.tianma.domain.JsonStr;
import com.myjo.autoOrder.tianma.domain.TaobaoAPI;

@Component
public class app implements CommandLineRunner {
	@Autowired
	private TaobaoAPI taobaoAPI;
	private static final Logger LOGGER = LoggerFactory.getLogger(app.class);
	@Autowired
	private PlaceOrder placeOrder;
	// @Autowired
	// private TianmaHttp tianmaHttp;
	@Value("${pay_password}")
	private String pay_password;
	// @Autowired
	// private TaobaoImpl taobaoImpl;
	// @Autowired
	// private OrderMapperImpl orderMapperImpl;
	private final ReentrantLock lock = new ReentrantLock();

	public void startApp() {
		Properties properties = new Properties();
		properties.put(PropertyKeyConst.ConsumerId, "CID_MG_02");
		properties.put(PropertyKeyConst.AccessKey, taobaoAPI.getAppkey());
		properties.put(PropertyKeyConst.SecretKey, taobaoAPI.getSecret());
		Consumer consumer = ONSFactory.createConsumer(properties);
		consumer.subscribe("rmq_sys_jst_23279400", "*", new MessageListener() {
			public Action consume(Message message, ConsumeContext context) {
				lock.lock();
				try {
					CsvWriter csvWriter = null;
					LOGGER.info("是否加锁" + lock.isLocked());
					System.out.println("Receive: " + message);
					long tid = Long.parseLong(message.getKey());
					LOGGER.info("订单编号" + tid);
					Map<String, Object> map1 = null;
					try {
						map1 = placeOrder.addMapInInfo(tid);
					} catch (Exception e) {
						// TODO: handle exception
					}
					if (map1 != null || map1.size() != 0) {
						String[] str = { String.valueOf(tid) + "\t",
								((JsonStr) map1.get("JsonStr")).getWareHouseName() + "\t",
								((JsonStr) map1.get("JsonStr")).getArticleno(),
								((JsonStr) map1.get("JsonStr")).getPickRate(),
								String.valueOf(((JsonStr) map1.get("JsonStr")).getProxyPrice()),
								((JsonStr) map1.get("JsonStr")).getSize().getSize2(),
								String.valueOf(((JsonStr) map1.get("JsonStr")).getSize().getInventory()) };
						csvWriter = new CsvWriter("E:/lalala.csv", ',', Charset.forName("GBK"));
						csvWriter.writeRecord(str, true);
						csvWriter.flush();
						csvWriter.close();
					}
				} catch (Exception e) {
				} finally {
					lock.unlock();
				}

				return Action.CommitMessage;
			}
		});
		consumer.start();
		System.out.println("Consumer Started");

	}

	@Override
	public void run(String... arg0) throws Exception {
		// TODO Auto-generated method stub
		try {
			startApp();
		} catch (Exception e) {
			e.printStackTrace();
			// 失败状态写入数据库
			String status = "failure";
			// orderMapperImpl.insertStatus(status);
		}
	}

}
