package org.warp7.camviewer;

import org.warp7.camviewer.CamViewer;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Created by Ricardo on 2016-03-30.
 */
public class SliderListener implements ChangeListener {

    boolean isX;

    public SliderListener(boolean isX) {
        this.isX = isX;
    }


    @Override
    public void stateChanged(ChangeEvent e) {
        if(e.getSource() instanceof JSlider) {
            JSlider s = (JSlider) e.getSource();
            int sliderVal = s.getValue();
            if(isX) CamViewer.xCrossHair = sliderVal;
            else CamViewer.yCrossHair = sliderVal;
        }
    }
}
