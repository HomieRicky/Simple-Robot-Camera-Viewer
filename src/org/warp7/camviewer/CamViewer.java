package org.warp7.camviewer;

import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoWriter;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.*;

/**
 * Created by Ricardo on 2016-03-19.
 */
public class CamViewer extends JFrame {
    JPanel containerPanel;
    JLabel image;
    JPanel imgPanel;
    JPanel sliderPanel;
    public static BufferedImage loadedImage;
    public static int xCrossHair = 0;
    public static int yCrossHair = 0;
    JSlider xSlider;
    JSlider ySlider;
    static final int xHalfScale = 320;
    static final int yHalfScale = 240;
    JButton saveBtn;

    JButton videoBtn;
    static VideoWriter recorder;
    static boolean isRecording = false;

    static NetworkTablesHandler nt;
    static boolean isNTConnected = false;
    static int drivingSide = -1; //1 = battery first, 0 = intake first
    static double rpm = 0;
    static boolean hasBall = false;
    static boolean isCompressorRunning = false;
    JLabel rpmLbl;
    JLabel dirLabel;

    public static Image INTAKES;
    public static Image BATTERY;
    public static Image NO_CONNECTION;

    public static void main(String args[]) {
        //System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        findAndLoadResources();
        recorder = new VideoWriter();
        CamViewer main = new CamViewer();
        //System.out.println(CamViewer.class.getClassLoader().getResource("resources/save.txt").getFile());
        loadState(main);
        USBCameraInputStream stream;
        if(args.length != 0) {
            stream = new USBCameraInputStream(true, Integer.parseInt(args[0]));
            nt = new NetworkTablesHandler(main, true);
        }
        else {
            stream = new USBCameraInputStream(30);
            nt = new NetworkTablesHandler(main, false);
        }
        Thread t = new Thread(stream);
        t.start();


        while(true) {
            if(loadedImage != null) {
                Mat m = bufferedImageToMat(loadedImage);
                if(isRecording) recorder.write(m);
                Imgproc.line(m, new Point(0, yHalfScale+yCrossHair), new Point(2*xHalfScale, yHalfScale+yCrossHair), new Scalar(0, 0, 255), 4);
                Imgproc.line(m, new Point(xHalfScale+xCrossHair-(108/2), 0), new Point(xHalfScale+xCrossHair-(108/2), 2*yHalfScale), new Scalar(0, 0, 255), 4);
                Imgproc.line(m, new Point(xHalfScale+xCrossHair+(108/2), 0), new Point(xHalfScale+xCrossHair+(108/2), 2*yHalfScale), new Scalar(0, 0, 255), 4);
                AutonHandler ah = new AutonHandler(m);
                //main.image.setIcon(new ImageIcon(matToBufferedImage(ah.mat)));
                main.image.setIcon(new ImageIcon(loadedImage));
            }

            if(isNTConnected) {
                main.rpmLbl.setText("RPM: " + rpm);
                if(!hasBall) main.containerPanel.setBackground(Color.WHITE);
                else main.containerPanel.setBackground(Color.ORANGE);
                if(drivingSide == 0) main.dirLabel.setIcon(new ImageIcon(INTAKES));
                else if(drivingSide == 1) main.dirLabel.setIcon(new ImageIcon(BATTERY));
            }
            if(!t.isAlive()) t = new Thread(stream);
            main.pack();
        }
    }

    private static void findAndLoadResources() {
        if(!loadLib("x64")) {
            if(!loadLib("x86")) {
                System.out.println("Couldn't load native libraries!");
                return;
            }
        }
        INTAKES = new ImageIcon(CamViewer.class.getClassLoader().getResource("resources/intake.jpg")).getImage();
        BATTERY = new ImageIcon(CamViewer.class.getClassLoader().getResource("resources/battery.jpg")).getImage();
        NO_CONNECTION = new ImageIcon(CamViewer.class.getClassLoader().getResource("resources/no_connection.jpg")).getImage();

    }

    public CamViewer() {
        super();
        imgPanel = new JPanel(new GridLayout(1, 1));
        sliderPanel = new JPanel();
        image = new JLabel();
        imgPanel.add(image);
        xSlider = new JSlider(JSlider.CENTER, -xHalfScale, xHalfScale, 0);
        xSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                JSlider s = (JSlider) e.getSource();
                xCrossHair = s.getValue();
            }
        });
        xSlider.setMajorTickSpacing(10);
        xSlider.setMinorTickSpacing(2);
        ySlider = new JSlider(JSlider.CENTER, -yHalfScale, yHalfScale, 0);
        ySlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                JSlider s = (JSlider) e.getSource();
                yCrossHair = s.getValue();
            }
        });
        ySlider.setMajorTickSpacing(10);
        ySlider.setMinorTickSpacing(2);
        saveBtn = new JButton("Save current state");
        saveBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveState();
            }
        });
        videoBtn = new JButton("Record video");
        videoBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JButton btn = (JButton) e.getSource();
                if(!isRecording) {
                    btn.setText("Stop recording");
                    btn.setBackground(Color.RED);
                    isRecording = true;
                } else {
                    btn.setText("Record video");
                    btn.setBackground(Color.WHITE);
                    isRecording = false;
                }
                toggleVideo();
            }
        });
        rpmLbl = new JLabel("RPM: ");
        dirLabel = new JLabel(new ImageIcon(NO_CONNECTION));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        sliderPanel.add(xSlider, gbc);
        gbc.gridy = 1;
        sliderPanel.add(ySlider, gbc);
        gbc.gridy = 2;
        sliderPanel.add(saveBtn, gbc);
        containerPanel = new JPanel();
        GridBagConstraints mainGbc = new GridBagConstraints();
        mainGbc.gridx = 0;
        mainGbc.gridy = 0;
        containerPanel.add(imgPanel, mainGbc);
        mainGbc.gridx = 0;
        mainGbc.gridy = 1;
        containerPanel.add(sliderPanel, mainGbc);
        mainGbc.gridy = 2;
        containerPanel.add(videoBtn, mainGbc);
        mainGbc.gridx = 1;
        containerPanel.add(dirLabel);
        mainGbc.gridy = 3;
        mainGbc.gridx = 1;
        containerPanel.add(rpmLbl);

        containerPanel.setPreferredSize(new Dimension(640, 640));
        add(containerPanel);
        pack();
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setVisible(true);
    }

    public void toggleVideo() {
        if(isRecording) {
            int fourcc = VideoWriter.fourcc('X', '2', '6', '4');
            //int fourcc = -1;
            recorder.open("recording_"+System.currentTimeMillis()+".mp4", -1, 30, new Size(640, 480));
            System.out.println(fourcc);
            if(!recorder.isOpened()) {
                System.out.println("Improper!!");
            }
            //recorder.open("recording_"+System.currentTimeMillis()+".AVI", -1, 30, new Size(640, 480));
        } else {
            recorder.release();
        }
    }

    public static BufferedImage matToBufferedImage(Mat frame) {
        //Mat() to BufferedImage
        int type = 0;
        if (frame.channels() == 1) {
            type = BufferedImage.TYPE_BYTE_GRAY;
        } else if (frame.channels() == 3) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        BufferedImage image = new BufferedImage(frame.width(), frame.height(), type);
        WritableRaster raster = image.getRaster();
        DataBufferByte dataBuffer = (DataBufferByte) raster.getDataBuffer();
        byte[] data = dataBuffer.getData();
        frame.get(0, 0, data);
        return image;
    }

    public static Mat bufferedImageToMat(BufferedImage img) {
        byte[] pixels = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
        Mat m = new Mat(480, 640, CvType.CV_8UC3);
        m.put(0, 0, pixels);
        return m;
    }

    public void saveState() {
        //File f = new File("save.txt");
        File f = new File(CamViewer.class.getClassLoader().getResource("resources/save.txt").getFile());
        try {
            PrintWriter bw = new PrintWriter(new FileWriter(f));
            bw.print(String.valueOf(xCrossHair));
            bw.print("\n");
            bw.print(String.valueOf(yCrossHair));
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to save");
            return;
        }
        System.out.println("Saved successfully");
    }

    public static void loadState(CamViewer context) {
        //File f = new File("save.txt");
        boolean loadedSavedValues = false;
        File f = new File(CamViewer.class.getClassLoader().getResource("resources/save.txt").getFile());
        if(!f.exists()) return;
        else if(!f.canRead()) return;
        else {
            try {
                BufferedReader br = new BufferedReader(new FileReader(f));
                xCrossHair = Integer.parseInt(br.readLine());
                yCrossHair = Integer.parseInt(br.readLine());
                br.close();
                loadedSavedValues = true;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                System.out.println("Failed to read. File not found.");
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Failed to read");
            } catch (NumberFormatException e) {
                //e.printStackTrace();
                System.out.println("Failed to read. Could be empty or badly formatted.");
            }
        }
        if(loadedSavedValues) {
            context.xSlider.setValue(xCrossHair);
            context.ySlider.setValue(yCrossHair);
        }
    }

    public static boolean loadLib(String arch) {
        try {
            String nativeLib = CamViewer.class.getClassLoader().getResource("resources/" + arch + "/" + Core.NATIVE_LIBRARY_NAME + ".dll").getFile();
            File f = new File(nativeLib);
            System.out.println(f.getAbsolutePath());
            System.load(f.getAbsolutePath());
            return true;
        } catch (UnsatisfiedLinkError e) {
            System.out.println("Can't load " + arch + " library. " + e.getMessage());
            return false;
        }

    }
}
