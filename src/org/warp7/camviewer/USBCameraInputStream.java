package org.warp7.camviewer;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.warp7.camviewer.CamViewer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

/**
 * Created by Ricardo on 2016-02-22.
 */
public class USBCameraInputStream implements Runnable {


    //Uses protocols from SmartDashboard and GRIP
    private final static int PORT = 1180;
    private final static String ADRESS = "roborio-865-frc.local";
    private final static byte[] MAGIC_NUMBERS = {0x01, 0x00, 0x00, 0x00};
    private final static int HW_COMPRESSION = -1;
    private final static int SIZE_640x480 = 0;

    private byte[] dataBuffer = new byte[64 * 1024];
    private byte[] magicNumbersBuffer = new byte[4];
    private int fps;

    private boolean useLocal = false;
    private VideoCapture localCapturer;

    private boolean shutdown = false;

    //This will initialize the camera on a network. Just put FPS as 30
    public USBCameraInputStream(int fps) {
        this.fps = fps;
    }

    //This will initialize a local camera on the computer for testing purposes. The port is usually 0 for the laptop camera. Try 0 or 1.
    public USBCameraInputStream(boolean useLocal, int port) {
        this.useLocal = true;
        localCapturer = new VideoCapture(port);
    }



    @Override
    public void run() {
        Thread.currentThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                shutdownThread();
            }
        });
        while(!shutdown) {
            try {
                if(useLocal) {
                    Mat m = new Mat();
                    localCapturer.read(m);
                    sendFrame(CamViewer.matToBufferedImage(m), m.rows() * m.cols() * m.channels());
                } else {
                    System.out.println("Attempting to connect to " + ADRESS);
                    try (
                            Socket socket = new Socket(ADRESS, PORT);
                            DataInputStream in = new DataInputStream(socket.getInputStream());
                            DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
                        System.out.println("Connected to " + ADRESS + ":" + PORT);
                        out.writeInt(fps);
                        out.writeInt(HW_COMPRESSION);
                        out.writeInt(SIZE_640x480);

                        while (!Thread.currentThread().isInterrupted()) {
                            in.readFully(magicNumbersBuffer);
                            if (!Arrays.equals(magicNumbersBuffer, MAGIC_NUMBERS)) {
                                throw new IOException("Wrong magic numbers! Bad input.");
                            }
                            int imageSize = in.readInt();
                            dataBuffer = growIfNecessary(dataBuffer, imageSize);
                            in.readFully(dataBuffer, 0, imageSize);
                            //sendMsg("Got frame with " + imageSize + " bytes.");
                            sendFrame(ImageIO.read(new ByteArrayInputStream(dataBuffer)), imageSize);
                        }
                    } catch (IOException e) {
                        System.out.println("Connection failed at " + e.getMessage() + ". Retrying...");
                        //e.printStackTrace();
                    } finally {
                        Thread.sleep(1000);
                    }
                }
            } catch (InterruptedException e) {
                System.out.println("Data thread interrupted!!");
            }
        }
    }

    public void shutdownThread() {
        shutdown = true;
    }

    public void sendFrame(BufferedImage img, int byteAmt) {
        try {
            CamViewer.loadedImage = img;
        } catch(NullPointerException e) { System.out.println("Null BufferedImage!"); }
    }

    /**
     * Return an array big enough to hold least at least "capacity" elements.  If the supplied buffer is big enough,
     * it will be reused to avoid unnecessary allocations.
     */
    private byte[] growIfNecessary(byte[] buffer, int capacity) {
        if (capacity > buffer.length) {
            int newCapacity = buffer.length;
            while (newCapacity < capacity) {
                newCapacity *= 1.5;
            }
            System.out.println("Growing to " + newCapacity);
            return new byte[newCapacity];
        }

        return buffer;
    }
}
