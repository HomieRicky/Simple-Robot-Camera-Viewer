package org.warp7.camviewer;

import edu.wpi.first.wpilibj.networktables.NetworkTable;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ricardo on 2016-04-01.
 */
public class AutonHandler {

    public Mat mat;
    public static boolean alreadyInitialized = false;
    private static NetworkTable nt;

    public AutonHandler(Mat m) {
        if(!alreadyInitialized) {
            alreadyInitialized = true;
            nt = NetworkTable.getTable("auton");
        }
        Imgproc.cvtColor(m, m, Imgproc.COLOR_BGR2HSV);
        ArrayList<Mat> hsvMats = new ArrayList<>();
        Core.split(m, hsvMats);
        byte blueA[] = new byte[m.rows() * m.cols() * m.channels()];
        byte[] hue = new byte[m.rows() * m.cols()], sat = new byte[m.rows() * m.cols()], vib = new byte[m.rows() * m.cols()];
        hsvMats.get(0).get(0, 0, hue);
        hsvMats.get(1).get(0, 0, sat);
        hsvMats.get(2).get(0, 0, vib);
        for(int i = 0; i < hue.length; i++) {
            if(vib[i] > -20 && vib[i] < 20) blueA[(i*3)+1] = (byte) (hue[i]*2);
            //blueA[i*3] = vib[i];
        }
        m.put(0, 0, blueA);
        Imgproc.threshold(m, m, 150, 750, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C);
        m.get(0, 0, blueA);
        for(int i = 0; i < blueA.length; i++) {
            blueA[i] = (byte) (255-blueA[i]);
        }
        for(int i = 0; i < blueA.length/3; i++) {
            if(blueA[i*3] >= 230) blueA[i*3] = (byte) 255;
        }
        List<MatOfPoint> points = new ArrayList<MatOfPoint>();
        m.put(0, 0, blueA);
        List<MatOfInt> hulls = new ArrayList<MatOfInt>();
        List<MatOfInt4> convexityDefects = new ArrayList<MatOfInt4>();
        List<MatOfPoint> hullPointMats = new ArrayList<MatOfPoint>();
        List<MatOfPoint> simpleHullPointMats = new ArrayList<MatOfPoint>();
        for(int i = 0; i < points.size(); i++) {
            if(points.get(i).rows() > 100) {
                MatOfInt hull = new MatOfInt();
                Imgproc.convexHull(points.get(i), hull);
                hulls.add(hull);
                MatOfInt4 cd = new MatOfInt4();
                Imgproc.convexityDefects(points.get(i), hull, cd);
                convexityDefects.add(cd);
                MatOfPoint hullPoints = generateHullPointMat(hull, points.get(i));
                hullPointMats.add(hullPoints);
                Imgproc.drawContours(m, points, i, new Scalar(0, 0, 255), 2);
                Imgproc.polylines(m, hullPointMats, false, new Scalar(0, 255, 0), 2);
                for(int j = 0; j < hullPoints.rows(); j++) {
                    Imgproc.circle(m, hullPoints.toArray()[j], 3, new Scalar(255, 255, 255), 1);
                }
                MatOfPoint simpleHull = generateSimpleConvexHull(hullPoints);
                double angleSum = 0;
                Point[] simpleHullA = simpleHull.toArray();
                for(int j = 0; j < simpleHull.rows(); j++) {
                    Imgproc.circle(m, simpleHull.toArray()[j], 5, new Scalar(255, 150, 0), 2);
                    Point a = getLast(simpleHullA, j);
                    Point b = simpleHullA[j];
                    Point c = getNext(simpleHullA, j);
                    double angleAB = Math.toDegrees(Math.atan2(b.y-a.y, b.x-a.x));
                    double angleBC = Math.toDegrees(Math.atan2(c.y-b.y, c.x-b.x));
                    double angle = Math.abs(angleBC-angleAB);
                    //Imgproc.putText(contours, String.valueOf((float) angle), b, Core.FONT_ITALIC, 0.2, Scalar.all(255), 1);
                    if(angle > 180) angle-=180;
                    angleSum += angle;
                }
                if(simpleHull.rows() == 4 && angleSum < 380 && angleSum > 270) simpleHullPointMats.add(simpleHull);
                //if(simpleHullA.length > 0) Imgproc.putText(contours, String.valueOf((float) angleSum), new Point(simpleHullA[0].x-100, simpleHullA[0].y+100), Core.FONT_ITALIC, 0.5, new Scalar(255, 128, 128), 1);
                //System.out.println("Shape interior angle sum: " + angleSum);
            }
        }

        mat = m;

    }

    public static void publishMode(String mode) { //LEFT, RIGHT, FORWARD, BACK, STOP, SHOOT, NO TARGET
        nt.putValue("mode", mode);
    }

    public static void publishSpeed(int i) { //STOP:0, SLOW:1, MEDIUM:2, FAST:3
        nt.putNumber("speed", (double) i);
    }

    private MatOfPoint generateHullPointMat(MatOfInt hull, MatOfPoint matOfPoint) {
        int hullArray[] = hull.toArray();
        Point pointArray[] = matOfPoint.toArray();
        List<Point> hullPoints = new ArrayList<>();
        for(int i = 0; i < hullArray.length; i++) {
            hullPoints.add(pointArray[hullArray[i]]);
        }
        MatOfPoint retr = new MatOfPoint();
        retr.fromList(hullPoints);
        return retr;
    }

    private MatOfPoint generateSimpleConvexHull(MatOfPoint mp) {
        double minAngle = 180;
        boolean runFirstTime = true;
        int minAngleIndex = 0;
        while(minAngle < 30 || runFirstTime) {
            minAngle = 180;
            Point[] points = mp.toArray();
            for (int i = 0; i < points.length; i++) {
                Point a = getLast(points, i);
                Point b = points[i];
                Point c = getNext(points, i);
                double angleAB = Math.toDegrees(Math.atan2(b.y-a.y, b.x-a.x));
                double angleBC = Math.toDegrees(Math.atan2(c.y-b.y, c.x-b.x));
                double angle = Math.abs(angleBC-angleAB);
                if(angle > 180) angle-=180;
                if(angle < minAngle) {
                    minAngleIndex = i;
                    minAngle = angle;
                }
            }
            if(minAngle < 30) {
                Point[] newPoints = new Point[points.length-1];
                int subtractor = 0;
                for(int j = 0; j < points.length; j++) {
                    if(j == minAngleIndex) subtractor++;
                    else newPoints[j-subtractor] = points[j];
                }
                mp = new MatOfPoint(newPoints);
            }
            runFirstTime = false;
        }
        boolean intersectingPoints = true;
        while(intersectingPoints) {
            intersectingPoints = false;
            Point[] points = mp.toArray();
            for (int i = 0; i < points.length; i++) {
                Point a = points[i];
                Point b = getNext(points, i);
                double distance = Math.sqrt(Math.pow(Math.abs(b.x-a.x), 2) + Math.pow(Math.abs(b.y-a.y), 2));
                if(distance < 10) {
                    intersectingPoints = true;
                    Point[] newPoints = new Point[points.length-1];
                    int subtractor = 0;
                    for(int j = 0; j < points.length; j++) {
                        if(j == i) subtractor++;
                        else newPoints[j-subtractor] = points[j];
                    }
                    mp = new MatOfPoint(newPoints);
                }
            }
            //System.out.println("Points[].length = " + points.length);
        }
        return mp;
    }

    public static <T> T getLast(T[] array, int curIndex) {
        if(curIndex == 0) return array[array.length-1];
        return array[curIndex-1];
    }
    public static <T> T getNext(T[] array, int curIndex) {
        if(curIndex == array.length-1) return array[0];
        return array[curIndex+1];
    }

    public static Point[] getSpecificMidpoints(MatOfPoint m) {
        Point[] points = m.toArray();
        Point[] midpoints = new Point[points.length];
        for(int i = 0; i < points.length; i++) {
            midpoints[i] = new Point((points[i].x+getNext(points, i).x)/2, (points[i].y+getNext(points, i).y)/2);
        }
        Point[] orderedPoints = midpoints; // Top:0, bottom:1, left:2, right:3
        for(int i = 0; i < midpoints.length; i++) {
            if(midpoints[i].y > orderedPoints[0].y) orderedPoints[0] = midpoints[i];
            if(midpoints[i].y < orderedPoints[1].y) orderedPoints[1] = midpoints[i];
            if(midpoints[i].x > orderedPoints[2].x) orderedPoints[2] = midpoints[i];
            if(midpoints[i].x < orderedPoints[3].x) orderedPoints[3] = midpoints[i];
        }
        return orderedPoints;
    }
}
