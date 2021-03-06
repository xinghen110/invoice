package com.hl.websocket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import com.alibaba.fastjson.JSON;
import com.hl.dao.RedisDao;
import com.hl.service.InvoiceService;
import com.hl.util.Const;
import com.hl.util.SocketLoadTool;

@Component("systemWebSocketHandler")
public class SystemWebSocketHandler implements WebSocketHandler {
	
	private static Logger logger = Logger.getLogger(SystemWebSocketHandler.class);
	
	@Resource(name = "socketListener")
	private SocketLoadTool socketListener;
	
	@Resource(name = "redisDao")
	private RedisDao redisDao;
	
	//private Logger log = LoggerFactory.getLogger(SystemWebSocketHandler.class);
	
	@Resource(name = "invoiceService")
	private InvoiceService invoiceService;
	
    private static final ArrayList<WebSocketSession> users = new ArrayList<WebSocketSession>();;
 

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    	logger.info("通过websocket与web前端建立连接,id为" + session.getId());
        users.add(session); //把当前会话添加到用户列表里
		//返回最新的报错发票数量
		Map<String, Object>map = new HashMap<>();
		map.put("msg_id", 205);
		String fault_num = (String)redisDao.getValue("fault_num");
		if(fault_num != null){
			map.put("fault_num", new Integer(fault_num));
		}
		session.sendMessage(new TextMessage(JSON.toJSONString(map)));
    }
    //接收消息，（可选）返回消息
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
    	logger.info("接收到消息" + message.getPayload());
    	String message_str = (String) message.getPayload();
    	Map<String, Object>cus_map = JSON.parseObject(message_str);
    	Map<String, Object>attibutes = session.getAttributes();
    	String code = (String) cus_map.get("code");
    	switch (code) {
		case "001":{
			//设置当前用户设置监控台状态
			attibutes.put("console_status",cus_map.get("console_status"));
			break;
		}
		case "002":{
			//客户端告诉服务端监控台图片加载完成，返回region_list
			invoiceService.broadcastRegionList();
			break;
		}	
		default:
			break;
		}
    }
 
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
/*        if(session.isOpen()){
            session.close();
        }
        users.remove(session);*/
        //System.out.println("传输异常");
        //log.error("handleTransportError" + exception.getMessage());
        //exception.printStackTrace();
    }
 
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        users.remove(session);
        logger.info("会话正常关闭"+ closeStatus.getReason() );
    }
 
    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
 
    /**
     * 给所有在线用户发送消息
     *
     * @param message
     */
    public void sendMessageToUsers(TextMessage message, int[] console_status_mul) {
    	try {
            for (WebSocketSession user : users) {
                try {
                    if (user.isOpen()) {
                    	Integer user_status = (Integer) user.getAttributes().get("console_status");
                    	logger.info("user_status=" + user_status);
                    	for(int console_status : console_status_mul){
                        	if(user_status != null && user_status == console_status){
                                user.sendMessage(message);
                                logger.info("成功群发消息");
                                break;
                        	}
                    	}
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    
    
    
 
}
