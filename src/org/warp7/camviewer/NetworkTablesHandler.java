package org.warp7.camviewer;

import edu.wpi.first.wpilibj.networktables.NetworkTable;
import edu.wpi.first.wpilibj.tables.ITable;
import edu.wpi.first.wpilibj.tables.ITableListener;

/**
 * Created by Ricardo on 2016-03-31.
 */
public class NetworkTablesHandler {

    CamViewer cvContext;
    NetworkTable drivingSide;
    NetworkTable rpm;
    NetworkTable photosensor;


    public NetworkTablesHandler(CamViewer context, boolean isLocal) {
        cvContext = context;
        NetworkTable.setClientMode();
        if(isLocal) NetworkTable.setIPAddress("localhost");
        else NetworkTable.setIPAddress("roborio-865-frc.local");
        drivingSide = NetworkTable.getTable("data/Robot");
        drivingSide.addTableListener(new ITableListener() {
            @Override
            public void valueChanged(ITable iTable, String s, Object o, boolean b) {
                if(s.equals("direction")) CamViewer.drivingSide = ((Boolean) o).booleanValue() ? 1 : 0;
                else if(s.equals("compressor")) CamViewer.isCompressorRunning = ((Boolean) o).booleanValue();
            }
        });
        rpm = NetworkTable.getTable("data/FlyWheel");
        rpm.addTableListener(new ITableListener() {
            @Override
            public void valueChanged(ITable iTable, String s, Object o, boolean b) {
                if(s.equals("speed")) CamViewer.rpm = ((Double) o).doubleValue();
            }
        });
        photosensor = NetworkTable.getTable("data/Intake");
        photosensor.addTableListener(new ITableListener() {
            @Override
            public void valueChanged(ITable iTable, String s, Object o, boolean b) {
                if(s.equals("photosensor")) CamViewer.hasBall = !((Boolean) o).booleanValue();
            }
        });
        CamViewer.isNTConnected = true;

    }
}
