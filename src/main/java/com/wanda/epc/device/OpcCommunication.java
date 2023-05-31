package com.wanda.epc.device;

import cn.hutool.json.JSONUtil;
import com.wanda.epc.config.OpcConfig;
import com.wanda.epc.param.DeviceMessage;
import org.apache.commons.lang3.StringUtils;
import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.common.JISystem;
import org.jinterop.dcom.core.JIVariant;
import org.openscada.opc.lib.common.ConnectionInformation;
import org.openscada.opc.lib.da.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

/**
 * @program: iot_dapc
 * @description: opc通讯协议
 * @author: Liurs
 * @create: 2022-09-13 11:23
 **/
@Service
public class OpcCommunication extends BaseDevice implements DisposableBean {

    private final static Logger logger = LoggerFactory.getLogger(OpcCommunication.class);

    private static Server server;

    private Item[] items = null;

    private Set<String> addressList = new HashSet<>();

    private Group group;

    @Autowired
    private OpcConfig opcConfig;

    @Autowired
    CommonDevice commonDevice;

    private AutoReconnectController autos = null;


    @PostConstruct
    public Server init() {
        try {
            close();
            ConnectionInformation ci = new ConnectionInformation();
            ci.setHost(opcConfig.getHost());
            ci.setDomain(opcConfig.getDomain());
            ci.setUser(opcConfig.getUser());
            ci.setPassword(opcConfig.getPassword());
            ci.setClsid(opcConfig.getClsid());
            String progId = opcConfig.getProgId();
            if (progId != null) {
                ci.setProgId(progId);
            } else {
                ci.setClsid(opcConfig.getClsid());
            }
            server = new Server(ci, Executors.newSingleThreadScheduledExecutor());

            JISystem.setJavaCoClassAutoCollection(false);
            JISystem.setAutoRegisteration(false);
            //用于管理server的自动连接
            this.autos = new AutoReconnectController(server);
            //添加监听机制
            this.autos.addListener(new OPCAutoReconnectListener());
            //开启连接
            this.autos.connect();
            Thread.sleep(1000);
            group = server.addGroup();
            loadAddressList();
            logger.info("[OPC采集初始化]:{} 创建Client !", ci.getHost());
            return server;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean processData() throws Exception {
        if (this.items == null || this.items.length == 0) {
            initItem();
            if (addressList.size() <= 0) {
                logger.info("========================采集测点在OPCServer全部没有=========================");
            }
        }
        try {
            Map<Item, ItemState> read = this.group.read(true, items);
            for (Map.Entry<Item, ItemState> entry : read.entrySet()) {
                Item item = entry.getKey();
                String value = readItem(item);
                logger.info("==================="+item.getId()+"+++++++++++++++++++"+value);
                if (StringUtils.isEmpty(value)){
                    logger.info("==================="+item.getId()+"+++++++++++++++++++点位数据采集为空");
                    continue;
                }
                List<DeviceMessage> deviceMessagesList = deviceParamListMap.get(item.getId());
                if (!CollectionUtils.isEmpty(deviceParamListMap)){
                    for (DeviceMessage deviceMessage : deviceMessagesList){
                        deviceMessage.setValue(value);
                        sendMessage(deviceMessage);
                    }
                }
            }
        } catch (Exception e) {

        }
        return true;
    }

    /**
     * 发送心跳
     */
    public void sendHeartbeat() {
        Item item  = items[0];
        try {
            readItem(item);
        } catch (JIException e) {
            logger.info("opc服务器断线，执行重连操作", e);
            init();
        }
    }

    @Override
    public void sendMessage(DeviceMessage dm) {
        if (dm != null){
            commonDevice.sendMessage(dm);
        }
    }


    @Override
    @Async
    public void dispatchCommand(String meter, Integer funcid, String value, String message)  throws JIException, AddFailedException {
        DeviceMessage deviceMessage = controlParamMap.get(meter + "-" + funcid);
        if (deviceMessage!=null){
            String outParamId = deviceMessage.getOutParamId();
            Item item = group.addItem(outParamId);
            JIVariant jiVariant = new JIVariant(value);
            Integer write = item.write(jiVariant);
            logger.info("控制指令==================="+item.getId()+"+++++++++++++++++++"+write);
            //反馈到iot-project
            commonDevice.feedback(message);
        }
    }

    @Override
    public boolean processData(String... obj) throws Exception {
        return false;
    }

    /***
     * @Description
     * @param item 点位
     * @return
     * @throws JIException
     */
    public  String readItem(Item item) throws JIException {
        JIVariant jiVariant = item.read(false).getValue();
        switch (jiVariant.getType()) {
            case JIVariant.VT_I2:
                short shortValue = jiVariant.getObjectAsShort();
                return String.valueOf(shortValue);
            case JIVariant.VT_I4:
                int intValue = jiVariant.getObjectAsInt();
                return String.valueOf(intValue);
            case JIVariant.VT_I8:
                long longValue = jiVariant.getObjectAsLong();
                return String.valueOf(longValue);
            case JIVariant.VT_R4:
                float floatValue = jiVariant.getObjectAsFloat();
                return String.valueOf(new DecimalFormat("0.00").format(floatValue));
            case JIVariant.VT_R8:
                double doubleValue = jiVariant.getObjectAsDouble();
                return String.valueOf(new DecimalFormat("0.00").format(doubleValue));
            case JIVariant.VT_BOOL:
                boolean boolValue = jiVariant.getObjectAsBoolean();
                return String.valueOf(boolValue);
            case JIVariant.VT_BSTR:
                return jiVariant.getObjectAsString2();
            case JIVariant.VT_UI4:
                return new DecimalFormat("0.00").format(jiVariant.getObjectAsUnsigned().getValue());
            case JIVariant.VT_UI2:
                return new DecimalFormat("0.00").format(jiVariant.getObjectAsUnsigned().getValue());
            case JIVariant.VT_UI1:
                return new DecimalFormat("0.00").format(jiVariant.getObjectAsUnsigned().getValue());
            default:
                return null;
        }
    }

    /**
     * @param itemIds
     * @return
     * @throws JIException
     * @throws AddFailedException
     * @Description String数组转换成Item数组
     */
    public Item[] addItemsByGroup(String[] itemIds) throws  JIException, AddFailedException {
        Map<String, Item> itemMap = this.group.addItems(itemIds);
        int arrLength = itemMap.size();
        Item[] items = new Item[arrLength];
        int index = 0;
        for (Map.Entry<String, Item> entry : itemMap.entrySet()) {
            if (index >= arrLength) {
                break;
            }
            Item value = entry.getValue();
            items[index] = value;
            index++;
        }
        return items;
    }

    private boolean initItem(){
        if (addressList!=null && addressList.size()>0){
            String[] itemArr = new String[addressList.size()];
            addressList.toArray(itemArr);
            try{
                items = addItemsByGroup(itemArr);
            }catch (Exception e){
                if (e instanceof AddFailedException) {
                    AddFailedException exception = (AddFailedException) e;
                    Map<String, Integer> errors = exception.getErrors();
                    Set<String> set = errors.keySet();
                    logger.info("================================OpcServer无以下测点================================");
                    logger.info(JSONUtil.toJsonStr(set));
                    logger.info("================================OpcServer无以上测点================================");
                    addressList.removeAll(set);
                    return true;
                }
            }
        }
        return true;
    }

    /**
     * @Description 记载子系统地址到addressList
     */
    private void loadAddressList(){
        for (String outParamId :deviceParamListMap.keySet()){
            addressList.add(outParamId);
        }
    }

    @Override
    public void destroy() {
        logger.info("关闭opc连接");
        close();
    }

    private synchronized void close() {
        if (this.autos != null) {
            this.autos.disconnect();
        }
        if (this.server != null) {
            this.server.disconnect();
        }
        if (this.server != null) {
            try {
                this.group.clear();
            } catch (JIException e) {
                e.printStackTrace();
            }
        }
        this.autos = null;
        this.server = null;
        this.group = null;
    }
}
