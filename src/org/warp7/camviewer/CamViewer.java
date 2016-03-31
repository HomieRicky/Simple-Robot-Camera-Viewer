package org.warp7.camviewer;

import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import javax.swing.*;
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
public class CamViewer extends JFrame implements ActionListener {
    JPanel containerPanel;
    JLabel image;
    JPanel imgPanel;
    JPanel sliderPanel;
    public static BufferedImage loadedImage;
    public static int xCrossHair = 0;
    public static int yCrossHair = 0;
    JSlider xSlider;
    JSlider ySlider;
    SliderListener xL;
    SliderListener yL;
    static final int xHalfScale = 320;
    static final int yHalfScale = 240;
    JButton saveBtn;

    public static void main(String args[]) {
        //System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        if(!loadLib("x64")) {
            if(!loadLib("x86")) {
                System.out.println("Couldn't load native libraries!");
                return;
            }
        }
        CamViewer main = new CamViewer();
        //System.out.println(CamViewer.class.getClassLoader().getResource("resources/save.txt").getFile());
        loadState(main);
        USBCameraInputStream stream;
        if(args.length != 0) stream = new USBCameraInputStream(true, Integer.parseInt(args[0]));
        else stream = new USBCameraInputStream(30);
        Thread t = new Thread(stream);
        t.start();


        while(true) {
            if(loadedImage != null) {
                Mat m = bufferedImageToMat(loadedImage);
                Imgproc.line(m, new Point(0, yHalfScale+yCrossHair), new Point(2*xHalfScale, yHalfScale+yCrossHair), new Scalar(0, 0, 255), 2);
                Imgproc.line(m, new Point(xHalfScale+xCrossHair, 0), new Point(xHalfScale+xCrossHair, 2*yHalfScale), new Scalar(0, 0, 255), 2);
                main.image.setIcon(new ImageIcon(matToBufferedImage(m)));
            }
            main.pack();
        }
    }

    public CamViewer() {
        super();

        imgPanel = new JPanel(new GridLayout(1, 1));
        sliderPanel = new JPanel();
        image = new JLabel();
        imgPanel.add(image);
        xSlider = new JSlider(JSlider.CENTER, -xHalfScale, xHalfScale, 0);
        xL = new SliderListener(true);
        xSlider.addChangeListener(xL);
        xSlider.setMajorTickSpacing(10);
        xSlider.setMinorTickSpacing(2);
        ySlider = new JSlider(JSlider.CENTER, -yHalfScale, yHalfScale, 0);
        yL = new SliderListener(false);
        ySlider.addChangeListener(yL);
        ySlider.setMajorTickSpacing(10);
        ySlider.setMinorTickSpacing(2);
        saveBtn = new JButton("Save current state");
        saveBtn.addActionListener(this);
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
        mainGbc.gridx = 1;
        containerPanel.add(sliderPanel, mainGbc);
        containerPanel.setPreferredSize(new Dimension(640, 540));
        add(containerPanel);
        pack();
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setVisible(true);
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

    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getSource() instanceof JButton) {
            saveState();
        }
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
