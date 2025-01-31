package org.photonvision.common.dataflow.networktables;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.kobe.xbot.JClient.ConcurrentXTablesClient;
import org.photonvision.common.dataflow.DataChangeService;
import org.photonvision.common.dataflow.events.OutgoingUIEvent;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.TimedTaskManager;

public class XTablesManager {
    private static final Logger logger = new Logger(XTablesManager.class, LogGroup.NetworkTables);

    private final AtomicReference<ConcurrentXTablesClient> xtClient = new AtomicReference<>();
    private static XTablesManager INSTANCE;
    public static final String ROOT_NAME = "photonvision.";

    private XTablesManager() {
        new Thread(
                        () -> {
                            logger.info("Initializing XTablesManager");

                            xtClient.set(new ConcurrentXTablesClient());
                            xtClient.get().addVersionProperty("PHOTON-VISION");
                            logger.info("XTablesManager initialized");
                        })
                .start();
    }

    public boolean isReady() {
        return xtClient.get() != null;
    }

    public ConcurrentXTablesClient getXtClient() {
        return xtClient.get();
    }

    public static XTablesManager getInstance() {
        if (INSTANCE == null) INSTANCE = new XTablesManager();
        return INSTANCE;
    }

    public void broadcastConnectedStatus() {
        TimedTaskManager.getInstance().addOneShotTask(this::broadcastConnectedStatusImpl, 1000L);
    }

    public boolean isConnected() {
        return xtClient != null
                && xtClient.get().getSocketMonitor().isConnected("REQUEST")
                && xtClient.get().getSocketMonitor().isConnected("PUSH")
                && xtClient.get().getSocketMonitor().isConnected("SUBSCRIBE");
    }

    private void broadcastConnectedStatusImpl() {
        HashMap<String, Object> map = new HashMap<>();
        var subMap = new HashMap<String, Object>();

        subMap.put("connected", isConnected());
        if (isConnected()) {
            subMap.put("address", xtClient.get().getIp());
        }

        map.put("ntConnectionInfo", subMap);
        DataChangeService.getInstance()
                .publishEvent(new OutgoingUIEvent<>("networkTablesConnected", map));
    }

    public void close() {
        xtClient.get().shutdown();
    }
}
