package com.coretec.sensing.utils;

import android.net.wifi.rtt.RangingResult;
import android.util.Log;

import com.coretec.sensing.model.Ap;
import com.coretec.sensing.model.Point;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import Jama.Matrix;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class Calculation {
    static double[][] a;
    static double[][] aT;
    static double[][] b;

    //전체 N개 중에서 K씩 묶은 조합의 개수 구하기
    public static int getCombination(int n, int k) {
        int molecule = n; // 분자
        int denominator = k; // 분모

        for (int i = 1; i < k; i++) {
            molecule *= (n - i);
            denominator *= i;
        }

        return molecule / denominator;
    }

    public static double[][] getMyLocation(List<RangingResult> scanResultArrayList, HashMap<String, Ap> apArrayList) {
//        initializeData(scanResultArrayList, apArrayList);

//        Matrix matrixA = new Matrix(a);
//        Matrix matrixB = new Matrix(b);
//        Matrix matrixAT = matrixA.transpose();
//
//        Matrix data1 = matrixAT.times(matrixA);
//        Matrix data2 = data1.inverse();
//        Matrix data3 = data2.times(matrixAT);
//        Matrix data4 = data3.times(matrixB);
//        Matrix data5 = data4.times(0.5f);
//
//        return data5.getArray();

//        initializeData();
        if (initializeData(scanResultArrayList, apArrayList) == 0)
            return null;

        double[][] data1 = multiplication(aT, a);
        double[][] data2 = mInverse(data1);
        double[][] data3 = multiplication(data2, 0.5f);
        double[][] data4 = multiplication(data3, aT);
        double[][] data5 = multiplication(data4, b);

        return data5;
    }

    private static void initializeData() {
        ArrayList<Point> pointArrayList = new ArrayList<>();
        ArrayList<Double> distanceArrayList = new ArrayList<>();

        //내 자리를 중심으로 했을 경우
//        pointArrayList.add(new Point(5.136860516, 8.478154788));
//        pointArrayList.add(new Point(2.61512899,10.47452558));
//        pointArrayList.add(new Point(4.100148666,10.50721469));
//
//        distanceArrayList.add(8.161844273);
//        distanceArrayList.add(9.342963674);
//        distanceArrayList.add(9.676058679);
//
//        Point targetPoint = new Point(2.033729777,0.282527328);
//        double targetDistance = 1.104839594;

        //팀장님 자리를 기준
        pointArrayList.add(new Point(5.136860516, 8.478154788));
        pointArrayList.add(new Point(2.61512899, 10.47452558));
        pointArrayList.add(new Point(4.100148666, 10.50721469));

        distanceArrayList.add(5.269564456);
        distanceArrayList.add(5.445703405);
        distanceArrayList.add(6.097540626);

        Point targetPoint = new Point(2.033729777, 0.282527328);
        double targetDistance = 5.12904154933064;

        a = new double[pointArrayList.size()][2];
        b = new double[pointArrayList.size()][1];

        for (int i = 0; i < pointArrayList.size(); i++) {
            Point point = pointArrayList.get(i);
            double distance = distanceArrayList.get(i);

            a[i][0] = point.getX() - targetPoint.getX();
            a[i][1] = point.getY() - targetPoint.getY();

            b[i][0] = (Math.pow(targetDistance, 2) - Math.pow(targetPoint.getX(), 2) - Math.pow(targetPoint.getY(), 2))
                    - (Math.pow(distance, 2) - Math.pow(point.getX(), 2) - Math.pow(point.getY(), 2));
        }

        aT = transposeMatrix(a);
    }

    private static int initializeData(List<RangingResult> scanResultArrayList, HashMap<String, Ap> apArrayList) {
        int count = 0;

        a = new double[scanResultArrayList.size() - 1][2];
        b = new double[scanResultArrayList.size() - 1][1];

        RangingResult targetRtt = getTargetRtt(scanResultArrayList);

        if (targetRtt == null)
            return 0;

        Ap targetAp = getTargetAp(apArrayList, targetRtt.getMacAddress().toString());

        if (targetAp == null)
            return 0;

        for (RangingResult rtt : scanResultArrayList) {

            if (rtt == targetRtt || rtt.getStatus() == RangingResult.STATUS_FAIL)
                continue;

            Ap ap = apArrayList.get(rtt.getMacAddress().toString());

            a[count][0] = ap.getPoint().getX() - targetAp.getPoint().getX();
            a[count][1] = ap.getPoint().getY() - targetAp.getPoint().getY();

            b[count][0] = (Math.pow((targetRtt.getDistanceMm() / 1000f), 2) - Math.pow(targetAp.getPoint().getX(), 2) - Math.pow(targetAp.getPoint().getY(), 2))
                    - (Math.pow((rtt.getDistanceMm() / 1000f), 2) - Math.pow(ap.getPoint().getX(), 2) - Math.pow(ap.getPoint().getY(), 2));

            count++;
        }

        aT = transposeMatrix(a);
        return 1;
    }

    //전치행렬
    private static double[][] transposeMatrix(double[][] arr) {
        int m = arr.length;
        int n = arr[0].length;
        double[][] result = new double[n][m];

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                result[j][i] = arr[i][j];
            }
        }

        return result;
    }

    //곱셈
    private static double[][] multiplication(double[][] arr1, double[][] arr2) {
        double[][] answer = new double[arr1.length][arr2[0].length];

        for (int i = 0; i < arr1.length; ++i) {
            for (int j = 0; j < arr2[0].length; ++j) {
                for (int k = 0; k < arr1[0].length; ++k) {
                    answer[i][j] += arr1[i][k] * arr2[k][j];
                }
            }
        }

        return answer;
    }

    private static double[][] multiplication(double[][] arr1, double value) {
        double[][] answer = new double[arr1.length][arr1[0].length];

        for (int i = 0; i < arr1.length; ++i) {
            for (int j = 0; j < arr1[0].length; ++j) {
                answer[i][j] = arr1[i][j] * value;
            }
        }

        return answer;
    }

    //역행렬
    // 가우스-조던 소거법을 이용하여 역행렬 구하는 함수
    private static double[][] mInverse(double[][] matrix) {
        int n = matrix.length;

        double[][] result = new double[n][n];
        double[][] tmpWork = matrix.clone();

        // 계산 결과가 저장되는 result 행렬을 단위행렬로 초기화
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                result[i][j] = (i == j) ? 1 : 0;

        // 대각 요소를 0 이 아닌 수로 만듦
        double ERROR = 1.0e-10;
        for (int i = 0; i < n; i++)
            if (-ERROR < tmpWork[i][i] && tmpWork[i][i] < ERROR) //if (-ERROR < tmpWork[i][i] && tmpWork[i][i] < ERROR)
            {
                for (int k = 0; k < n; k++) {
                    if (-ERROR < tmpWork[k][i] && tmpWork[k][i] < ERROR) continue;
                    for (int j = 0; j < n; j++) {
                        tmpWork[i][j] += tmpWork[k][j];
                        result[i][j] += result[k][j];  // result[i*n+j] += result[k*n+j];
                    }
                    break;
                }
                if (-ERROR < tmpWork[i][i] && tmpWork[i][i] < ERROR) return result;
            }

        // Gauss-Jordan eliminatio
        for (int i = 0; i < n; i++) {
            // 대각 요소를 1로 만듦
            double constant = tmpWork[i][i];      // 대각 요소의 값 저장
            for (int j = 0; j < n; j++) {
                tmpWork[i][j] /= constant;   // tmpWork[i][i] 를 1 로 만드는 작업
                result[i][j] /=
                        constant; // result[i*n+j] /= constant;   // i 행 전체를 tmpWork[i][i] 로 나눔
            }

            // i 행을 제외한 k 행에서 tmpWork[k][i] 를 0 으로 만드는 단계
            for (int k = 0; k < n; k++) {
                if (k == i) continue;      // 자기 자신의 행은 건너뜀
                if (tmpWork[k][i] == 0) continue;   // 이미 0 이 되어 있으면 건너뜀

                // tmpWork[k][i] 행을 0 으로 만듦
                constant = tmpWork[k][i];
                for (int j = 0; j < n; j++) {
                    tmpWork[k][j] = tmpWork[k][j] - tmpWork[i][j] * constant;
                    result[k][j] = result[k][j] - result[i][j] *
                            constant;  // result[k*n+j] = result[k*n+j] - result[i*n+j] * constant;
                }
            }
        }
        return result;
    }

    //최단거리 RTT 객체 구하기
    private static RangingResult getTargetRtt(List<RangingResult> scanResultArrayList) {
        RangingResult rangingResult = null;
        int distance = Integer.MAX_VALUE;

        for (RangingResult temp : scanResultArrayList) {
            if (temp.getStatus() == RangingResult.STATUS_SUCCESS && distance > temp.getDistanceMm()) {
                rangingResult = temp;
                distance = temp.getDistanceMm();
            }
        }

        return rangingResult;
    }

    //최단거리 RTT의 Mac주소를 기준으로 AP객체 구하기
    private static Ap getTargetAp(HashMap<String, Ap> apArrayList, String macAddress) {
        return apArrayList.get(macAddress);
    }

    public static double getDistance(Point myLocation, Point ap) {
        return Math.sqrt(Math.pow(Math.abs(myLocation.getX() - ap.getX()), 2) + Math.pow(Math.abs(myLocation.getY() - ap.getY()), 2));
    }
}
