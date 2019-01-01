package com.yzh.revenuechart;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BrokenLineView brokenLineView = findViewById(R.id.bv);
        BrokenLineView brokenLineView2 = findViewById(R.id.bv2);


        ArrayList<String> xRawDatas = new ArrayList<>();
        xRawDatas.add("2月");
        xRawDatas.add("3月");
        xRawDatas.add("4月");
        xRawDatas.add("5月");
        xRawDatas.add("6月");
        xRawDatas.add("7月");
        brokenLineView.setxRawData(xRawDatas);

        ArrayList<Double> yList = new ArrayList<>();
        yList.add(123d);
        yList.add(203d);
        yList.add(323d);
        yList.add(423d);
        yList.add(523d);
        yList.add(223d);

        brokenLineView.setyRawData(yList);
        ArrayList<Double> yList2 = new ArrayList<>();
        yList2.add(333d);
        yList2.add(233d);
        yList2.add(533d);
        yList2.add(333d);
        yList2.add(433d);
        yList2.add(433d);

        brokenLineView.addyRawData(yList2);



        ArrayList<String> xRawDatas2 = new ArrayList<>();
        xRawDatas2.add("3月");
        xRawDatas2.add("4月");
        xRawDatas2.add("5月");
        xRawDatas2.add("6月");
        brokenLineView2.setxRawData(xRawDatas2);
        ArrayList<Double> yList3 = new ArrayList<>();
        yList3.add(333d);
        yList3.add(233d);
        yList3.add(533d);
        yList3.add(333d);
        brokenLineView2.addyRawData(yList3);


    }
}
