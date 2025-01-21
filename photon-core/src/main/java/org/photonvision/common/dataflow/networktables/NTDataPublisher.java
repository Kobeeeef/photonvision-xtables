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

package org.photonvision.common.dataflow.networktables;

import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEvent;
import edu.wpi.first.networktables.NetworkTablesJNI;
import org.photonvision.common.dataflow.CVPipelineResultConsumer;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.targeting.PhotonPipelineResult;
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.photonvision.vision.pipeline.result.CalibrationPipelineResult;
import org.photonvision.vision.target.TrackedTarget;

import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class NTDataPublisher implements CVPipelineResultConsumer {
    private final Logger logger = new Logger(NTDataPublisher.class, LogGroup.General);

    private final NetworkTable rootTable = NetworkTablesManager.getInstance().kRootTable;

    NTDataChangeListener pipelineIndexListener;
    private final Supplier<Integer> pipelineIndexSupplier;
    private final Consumer<Integer> pipelineIndexConsumer;

    NTDataChangeListener driverModeListener;
    private final BooleanSupplier driverModeSupplier;
    private final Consumer<Boolean> driverModeConsumer;
    private String cameraNickname;

    public NTDataPublisher(
            String cameraNickname,
            Supplier<Integer> pipelineIndexSupplier,
            Consumer<Integer> pipelineIndexConsumer,
            BooleanSupplier driverModeSupplier,
            Consumer<Boolean> driverModeConsumer)
    {
        updateCameraNickname(cameraNickname);
        this.pipelineIndexSupplier = pipelineIndexSupplier;
        this.pipelineIndexConsumer = pipelineIndexConsumer;
        this.driverModeSupplier = driverModeSupplier;
        this.driverModeConsumer = driverModeConsumer;

        updateEntries();
    }

    private void onPipelineIndexChange(NetworkTableEvent entryNotification) {
        var newIndex = (int) entryNotification.valueData.value.getInteger();
        var originalIndex = pipelineIndexSupplier.get();

        // ignore indexes below 0
        if (newIndex < 0) {
            if (XTablesManager.getInstance().isReady())
                XTablesManager.getInstance().getXtClient().putInteger(cameraNickname + "pipelineIndexState", originalIndex);
            return;
        }

        if (newIndex == originalIndex) {
            logger.debug("Pipeline index is already " + newIndex);
            return;
        }

        pipelineIndexConsumer.accept(newIndex);
        var setIndex = pipelineIndexSupplier.get();
        if (newIndex != setIndex) { // set failed
            if (XTablesManager.getInstance().isReady())
                XTablesManager.getInstance().getXtClient().putInteger(cameraNickname + "pipelineIndexState", setIndex);
            // TODO: Log
        }
        logger.debug("Set pipeline index to " + newIndex);
    }

    private void onDriverModeChange(NetworkTableEvent entryNotification) {
        var newDriverMode = entryNotification.valueData.value.getBoolean();
        var originalDriverMode = driverModeSupplier.getAsBoolean();

        if (newDriverMode == originalDriverMode) {
            logger.debug("Driver mode is already " + newDriverMode);
            return;
        }

        driverModeConsumer.accept(newDriverMode);
        logger.debug("Set driver mode to " + newDriverMode);
    }

    private void removeEntries() {
        if (pipelineIndexListener != null) pipelineIndexListener.remove();
        if (driverModeListener != null) driverModeListener.remove();

    }

    private void updateEntries() {
        if (pipelineIndexListener != null) pipelineIndexListener.remove();
        if (driverModeListener != null) driverModeListener.remove();


    }

    public void updateCameraNickname(String newCameraNickname) {
        this.cameraNickname = XTablesManager.ROOT_NAME + cameraNickname + ".";

    }

    @Override
    public void accept(CVPipelineResult result) {
        CVPipelineResult acceptedResult;
        if (result
                instanceof
                CalibrationPipelineResult) // If the data is from a calibration pipeline, override the list
            // of targets to be null to prevent the data from being sent and
            // continue to post blank/zero data to the network tables
            acceptedResult =
                    new CVPipelineResult(
                            result.sequenceID,
                            result.processingNanos,
                            result.fps,
                            List.of(),
                            result.inputAndOutputFrame);
        else acceptedResult = result;
        var now = NetworkTablesJNI.now();
        var captureMicros = MathUtils.nanosToMicros(result.getImageCaptureTimestampNanos());

        var offset = NetworkTablesManager.getInstance().getOffset();

        // Transform the metadata timestamps from the local nt::Now timebase to the Time Sync Server's
        // timebase
        var simplified =
                new PhotonPipelineResult(
                        acceptedResult.sequenceID,
                        captureMicros + offset,
                        now + offset,
                        NetworkTablesManager.getInstance().getTimeSinceLastPong(),
                        TrackedTarget.simpleFromTrackedTargets(acceptedResult.targets),
                        acceptedResult.multiTagResult);

        // random guess at size of the array

        if (XTablesManager.getInstance().isReady())
            XTablesManager.getInstance().getXtClient().putInteger(cameraNickname + "pipelineIndexState", pipelineIndexSupplier.get());
        if (XTablesManager.getInstance().isReady())
            XTablesManager.getInstance().getXtClient().putDouble(cameraNickname + "latencyMillis", acceptedResult.getLatencyMillis());
        if (XTablesManager.getInstance().isReady())
            XTablesManager.getInstance().getXtClient().putBoolean(cameraNickname + "hasTarget", acceptedResult.hasTargets());

        if (acceptedResult.hasTargets()) {
            var bestTarget = acceptedResult.targets.get(0);
            if (XTablesManager.getInstance().isReady())
                XTablesManager.getInstance().getXtClient().putDouble(cameraNickname + "targetPitch", bestTarget.getPitch());
            if (XTablesManager.getInstance().isReady())
                XTablesManager.getInstance().getXtClient().putDouble(cameraNickname + "targetYaw", bestTarget.getYaw());
            if (XTablesManager.getInstance().isReady())
                XTablesManager.getInstance().getXtClient().putDouble(cameraNickname + "targetArea", bestTarget.getArea());
            if (XTablesManager.getInstance().isReady())
                XTablesManager.getInstance().getXtClient().putDouble(cameraNickname + "targetSkew", bestTarget.getSkew());


            var pose = bestTarget.getBestCameraToTarget3d();
            double x = pose.getTranslation().getX();
            double y = pose.getTranslation().getY();
            double z = pose.getTranslation().getZ();

// Extract rotation as a quaternion (qw, qx, qy, qz)
            Rotation3d rotation = pose.getRotation();
            double qw = rotation.getQuaternion().getW();
            double qx = rotation.getQuaternion().getX();
            double qy = rotation.getQuaternion().getY();
            double qz = rotation.getQuaternion().getZ();

// Convert to Double[]
            Double[] targetPoseArray = new Double[] {x, y, z, qw, qx, qy, qz};
            if (XTablesManager.getInstance().isReady())
                XTablesManager.getInstance().getXtClient().putList(cameraNickname + "targetPose", targetPoseArray);

            var targetOffsetPoint = bestTarget.getTargetOffsetPoint();
            if (XTablesManager.getInstance().isReady())
                XTablesManager.getInstance().getXtClient().putDouble(cameraNickname + "targetPixelsX", targetOffsetPoint.x);
            if (XTablesManager.getInstance().isReady())
                XTablesManager.getInstance().getXtClient().putDouble(cameraNickname + "targetPixelsY", targetOffsetPoint.y);

        } else {
            if (XTablesManager.getInstance().isReady())
                XTablesManager.getInstance().getXtClient().putDouble(cameraNickname + "targetPitch", 0d);
            if (XTablesManager.getInstance().isReady())
                XTablesManager.getInstance().getXtClient().putDouble(cameraNickname + "targetYaw", 0d);
            if (XTablesManager.getInstance().isReady())
                XTablesManager.getInstance().getXtClient().putDouble(cameraNickname + "targetArea", 0d);
            if (XTablesManager.getInstance().isReady())
                XTablesManager.getInstance().getXtClient().putDouble(cameraNickname + "targetSkew", 0d);

            if (XTablesManager.getInstance().isReady())
                XTablesManager.getInstance().getXtClient().putList(cameraNickname + "targetPose", new Double[] {0d, 0d, 0d, 0d, 0d});
            if (XTablesManager.getInstance().isReady())
                XTablesManager.getInstance().getXtClient().putDouble(cameraNickname + "targetPixelsX", 0d);
            if (XTablesManager.getInstance().isReady())
                XTablesManager.getInstance().getXtClient().putDouble(cameraNickname + "targetPixelsY", 0d);

        }

        // Something in the result can sometimes be null -- so check probably too many things
        if (acceptedResult.inputAndOutputFrame != null
                && acceptedResult.inputAndOutputFrame.frameStaticProperties != null
                && acceptedResult.inputAndOutputFrame.frameStaticProperties.cameraCalibration != null) {
            var fsp = acceptedResult.inputAndOutputFrame.frameStaticProperties;
            if (XTablesManager.getInstance().isReady())
                XTablesManager.getInstance().getXtClient().putList(cameraNickname + "cameraIntrinsics", Arrays.stream(fsp.cameraCalibration.getIntrinsicsArr())
                        .boxed()
                        .toArray(Double[]::new));
            if (XTablesManager.getInstance().isReady())
                XTablesManager.getInstance().getXtClient().putList(cameraNickname + "cameraDistortion", Arrays.stream(fsp.cameraCalibration.getDistCoeffsArr())
                        .boxed()
                        .toArray(Double[]::new));
        } else {
            if (XTablesManager.getInstance().isReady())
                XTablesManager.getInstance().getXtClient().putList(cameraNickname + "cameraIntrinsics", new Double[]{});
            if (XTablesManager.getInstance().isReady())
                XTablesManager.getInstance().getXtClient().putList(cameraNickname + "cameraDistortion", new Double[]{});

        }
        if (XTablesManager.getInstance().isReady())
            XTablesManager.getInstance().getXtClient().putLong(cameraNickname + "heartbeat", acceptedResult.sequenceID);

    }
}
