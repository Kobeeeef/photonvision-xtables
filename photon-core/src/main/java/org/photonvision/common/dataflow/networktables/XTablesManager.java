package org.photonvision.common.dataflow.networktables;

import org.kobe.xbot.JClient.XTablesClient;
import org.photonvision.PhotonVersion;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.configuration.NetworkConfig;
import org.photonvision.common.dataflow.DataChangeService;
import org.photonvision.common.dataflow.events.OutgoingUIEvent;
import org.photonvision.common.logging.LogGroup;

import org.photonvision.common.logging.Logger;
import java.util.HashMap;

public class XTablesManager {
    private static final Logger logger = new Logger(XTablesManager.class, LogGroup.NetworkTables);

    private final XTablesClient xtClient;
    private static XTablesManager INSTANCE;

    private XTablesManager() {
        xtClient = new XTablesClient();
    }

    public static XTablesManager getInstance() {
        if (INSTANCE == null) INSTANCE = new XTablesManager();
        return INSTANCE;
    }



    public void close() {
        xtClient.shutdown();
    }
}

