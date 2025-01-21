/*
 * Copyright (C) Photon Vision.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.photonvision.vision.frame.consumer;

import edu.wpi.first.networktables.NetworkTable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.function.Consumer;

import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.dataflow.networktables.NetworkTablesManager;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.opencv.CVMat;

public class FileSaveFrameConsumer implements Consumer<CVMat> {
    private final Logger logger = new Logger(FileSaveFrameConsumer.class, LogGroup.General);

    // match type's values from the FMS.
    private static final String[] matchTypes = {"N/A", "P", "Q", "E", "EV"};

    // Formatters to generate unique, timestamped file names
    private static final String FILE_PATH = ConfigManager.getInstance().getImageSavePath().toString();
    private static final String FILE_EXTENSION = ".jpg";
    private static final String NT_SUFFIX = "SaveImgCmd";

    DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    DateFormat tf = new SimpleDateFormat("hhmmssSS");


    private final String ntEntryName;


    private final String cameraUniqueName;
    private String cameraNickname;
    private final String streamType;

    private long savedImagesCount = 0;

    public FileSaveFrameConsumer(String camNickname, String cameraUniqueName, String streamPrefix) {
        this.ntEntryName = streamPrefix + NT_SUFFIX;
        this.cameraNickname = camNickname;
        this.cameraUniqueName = cameraUniqueName;
        this.streamType = streamPrefix;


        NetworkTable fmsTable = NetworkTablesManager.getInstance().getNTInst().getTable("FMSInfo");

        updateCameraNickname(camNickname);
    }

    public void accept(CVMat image) {

        System.out.println("THIS IS REMOVED FOR XTABLES AS NOT NEEDED; D:\\stuff\\IdeaProjects\\photonvision-xtables\\photon-core\\src\\main\\java\\org\\photonvision\\vision\\frame\\consumer\\FileSaveFrameConsumer.java");
    }

    public void updateCameraNickname(String newCameraNickname) {
        // Remove existing entries


        // Recreate and re-init network tables structure
        this.cameraNickname = newCameraNickname;

    }

    public void overrideTakeSnapshot() {

    }

    /**
     * Returns the match Data collected from the NT. eg : Q58 for qualfication match 58. If not in
     * event, returns N/A-0-EVENTNAME
     */
    private String getMatchData() {

        return "XTABLESOVERRIDE-0-0";
    }

    public void close() {
        // troododfa;lkjadsf;lkfdsaj otgooadflsk;j
    }
}
