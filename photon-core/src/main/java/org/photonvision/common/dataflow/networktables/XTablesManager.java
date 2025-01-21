package org.photonvision.common.dataflow.networktables;

import org.kobe.xbot.JClient.XTablesClient;
import org.photonvision.common.dataflow.DataChangeService;
import org.photonvision.common.dataflow.events.OutgoingUIEvent;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.TimedTaskManager;

import java.util.HashMap;

public class XTablesManager {
    private static final Logger logger = new Logger(XTablesManager.class, LogGroup.NetworkTables);

    private final XTablesClient xtClient;
    private static XTablesManager INSTANCE;
    public static final String ROOT_NAME = "photonvision.";
    private XTablesManager() {
        xtClient = new XTablesClient();
    }

    public XTablesClient getXtClient() {
        return xtClient;
    }

    public static XTablesManager getInstance() {
        if (INSTANCE == null) INSTANCE = new XTablesManager();
        return INSTANCE;
    }

    public void broadcastConnectedStatus() {
        TimedTaskManager.getInstance().addOneShotTask(this::broadcastConnectedStatusImpl, 1000L);
    }

    public boolean isConnected() {
        return xtClient != null && xtClient.getSocketMonitor().isConnected("REQUEST") &&
                xtClient.getSocketMonitor().isConnected("PUSH") &&
                xtClient.getSocketMonitor().isConnected("SUBSCRIBE");
    }

    private void broadcastConnectedStatusImpl() {
        HashMap<String, Object> map = new HashMap<>();
        var subMap = new HashMap<String, Object>();

        subMap.put("connected", isConnected());
        if (isConnected()) {
            subMap.put("address", xtClient.getIp());
        }

        map.put("ntConnectionInfo", subMap);
        DataChangeService.getInstance()
                .publishEvent(new OutgoingUIEvent<>("networkTablesConnected", map));
    }


    public void close() {
        xtClient.shutdown();
    }
}

