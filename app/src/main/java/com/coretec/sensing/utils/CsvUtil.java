package com.coretec.sensing.utils;

import com.coretec.sensing.model.Ap;
import com.coretec.sensing.model.Link;
import com.coretec.sensing.model.Node;
import com.coretec.sensing.model.Poi;
import com.coretec.sensing.model.Point;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static com.coretec.sensing.utils.Const.MAPPath;

public class CsvUtil {

    public static boolean readMapSettingCsv(String filePath) {
        List<String> list = new ArrayList<>();
        try {
            list = Files.readAllLines(Paths.get(filePath), Charset.forName("EUC-KR"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (list.size() < 2)
            return false;


        String[] str = list.get(1).split(",");

        Const.setMapParam(Float.parseFloat(str[0].replaceAll("\\\"", "")), Float.parseFloat(str[1].replaceAll("\\\"", "")), Double.parseDouble(str[2].replaceAll("\\\"", "")), Double.parseDouble(str[3].replaceAll("\\\"", "")));

        return true;
    }

    public static ArrayList<Ap> readApCsv(String filePath) {

        ArrayList<Ap> apArrayList = new ArrayList<>();

        List<String> list = new ArrayList<>();
        try {
            list = Files.readAllLines(Paths.get(filePath), Charset.forName("EUC-KR"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        boolean isHeader = true;

        for (String readLine : list) {
            if (isHeader) {
                isHeader = false;
                continue;
            }
            String[] str = readLine.split(",");

            int seq = Integer.parseInt(str[0].replaceAll("\\\"", ""));
            String name = str[1].replaceAll("\\\"", "");
            String macAddress = str[2].replaceAll("\\\"", "");
            double pointX = Double.parseDouble(str[3].replaceAll("\\\"", ""));
            double pointY = Double.parseDouble(str[4].replaceAll("\\\"", ""));

            apArrayList.add(new Ap(seq, name, macAddress, new Point(pointX, pointY)));
        }

        return apArrayList;
    }

    public static ArrayList<Poi> readPoiCsv(String filePath) {

        ArrayList<Poi> poiArrayList = new ArrayList<>();

        List<String> list = new ArrayList<>();
        try {
            list = Files.readAllLines(Paths.get(filePath), Charset.forName("EUC-KR"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        boolean isHeader = true;

        for (String readLine : list) {
            if (isHeader) {
                isHeader = false;
                continue;
            }
            String[] str = readLine.split(",");

            int seq = Integer.parseInt(str[0].replaceAll("\\\"", ""));
            String name = str[1].replaceAll("\\\"", "");
            double pointX = Double.parseDouble(str[2].replaceAll("\\\"", ""));
            double pointY = Double.parseDouble(str[3].replaceAll("\\\"", ""));

            poiArrayList.add(new Poi(seq, name, new Point(pointX, pointY)));
        }

        return poiArrayList;
    }

    public static ArrayList<Node> readNodeCsv(String filePath) {

        ArrayList<Node> nodeArrayList = new ArrayList<>();

        List<String> list = new ArrayList<>();
        try {
            list = Files.readAllLines(Paths.get(filePath), Charset.forName("EUC-KR"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        boolean isHeader = true;

        for (String readLine : list) {
            if (isHeader) {
                isHeader = false;
                continue;
            }
            String[] str = readLine.split(",");

            int seq = Integer.parseInt(str[0].replaceAll("\\\"", ""));
            double pointX = Double.parseDouble(str[1].replaceAll("\\\"", ""));
            double pointY = Double.parseDouble(str[2].replaceAll("\\\"", ""));

            nodeArrayList.add(new Node(seq, new Point(pointX, pointY)));
        }

        return nodeArrayList;
    }

    public static ArrayList<Link> readLinkCsv(String filePath) {

        ArrayList<Link> linkArrayList = new ArrayList<>();

        List<String> list = new ArrayList<>();
        try {
            list = Files.readAllLines(Paths.get(filePath), Charset.forName("EUC-KR"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        boolean isHeader = true;

        for (String readLine : list) {
            if (isHeader) {
                isHeader = false;
                continue;
            }
            String[] str = readLine.split(",");

            int seq = Integer.parseInt(str[0].replaceAll("\\\"", ""));
            int startNode = Integer.parseInt(str[1].replaceAll("\\\"", ""));
            int endNode = Integer.parseInt(str[2].replaceAll("\\\"", ""));
            int weightP = Integer.parseInt(str[3].replaceAll("\\\"", ""));
            double weightM = Double.parseDouble(str[4].replaceAll("\\\"", ""));

            linkArrayList.add(new Link(seq, startNode, endNode, weightP, weightM));
        }

        return linkArrayList;
    }

    public static void writeApCsv(ArrayList<Ap> apArrayList, String filePath) {
        File apFile = new File(filePath, "AP.csv");

        if (apFile.exists()) {
            apFile.delete();
        }

        CsvManager csvManager = new CsvManager(filePath, "AP.csv");
        csvManager.Write("Number,Name,Mac,PointX,PointY");

        for (Ap ap : apArrayList) {
            csvManager.Write(ap.getSeq() + "," + ap.getName() + "," + ap.getMacAddress() + "," + ap.getPoint().getX() + "," + ap.getPoint().getY());
        }

        csvManager.close();
    }

    public static void writePoiCsv(ArrayList<Poi> poiArrayList, String filePath) {
        File poiFile = new File(filePath, "POI.csv");

        if (poiFile.exists()) {
            poiFile.delete();
        }

        CsvManager csvManager = new CsvManager(filePath, "POI.csv");
        csvManager.Write("Number,Name,PointX,PointY");

        for (Poi poi : poiArrayList) {
            csvManager.Write(poi.getSeq() + "," + poi.getName() + "," + poi.getPoint().getX() + "," + poi.getPoint().getY());
        }

        csvManager.close();
    }

    public static void writeNodeCsv(ArrayList<Node> nodeArrayList, String filePath) {
        File nodeFile = new File(filePath, "NODE.csv");

        if (nodeFile.exists()) {
            nodeFile.delete();
        }

        CsvManager csvManager = new CsvManager(filePath, "NODE.csv");
        csvManager.Write("Number,PointX,PointY");

        for (Node node : nodeArrayList) {
            csvManager.Write(node.getSeq() + "," + node.getPoint().getX() + "," + node.getPoint().getY());
        }

        csvManager.close();
    }

    public static void writeLinkCsv(ArrayList<Link> linkArrayList, String filePath) {
        File linkFile = new File(filePath, "LINK.csv");

        if (linkFile.exists()) {
            linkFile.delete();
        }

        CsvManager csvManager = new CsvManager(filePath, "LINK.csv");
        csvManager.Write("Number,StartNode,EndNode,Weight(P),Weight(M)");

        for (Link link : linkArrayList) {
            csvManager.Write(link.getSeq() + "," + link.getNode_start() + "," + link.getNode_end() + "," + link.getWeight_p() + "," + link.getWeight_m());
        }

        csvManager.close();
    }

    public static void writeCustomMapSetting() {
        File linkFile = new File(MAPPath, "setting.csv");

        if (linkFile.exists()) {
            linkFile.delete();
        }

        CsvManager csvManager = new CsvManager(MAPPath, "setting.csv");
        csvManager.Write("width,height,pixel/meter width,pixel/meter height");
        csvManager.Write("2411,2040,21.5,22.4");
        csvManager.close();
    }
}