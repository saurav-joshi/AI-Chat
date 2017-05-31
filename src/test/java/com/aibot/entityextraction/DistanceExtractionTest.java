package com.aibot.entityextraction;

public class DistanceExtractionTest {

    public void testGetFormattedDistanceFromText() throws Exception {
        DistanceExtraction dis = DistanceExtraction.getInstance();
        System.out.println(dis.getFormattedDistanceFromText("3.5 km"));
        System.out.println(dis.getFormattedDistanceFromText("3.5 kilometre"));
        System.out.println(dis.getFormattedDistanceFromText("3.5 kilometer"));

        System.out.println(dis.getFormattedDistanceFromText("4km"));
        System.out.println(dis.getFormattedDistanceFromText("4kilometre"));
        System.out.println(dis.getFormattedDistanceFromText("4kilometer"));

        System.out.println(dis.getFormattedDistanceFromText("5.5 m"));
        System.out.println(dis.getFormattedDistanceFromText("5.5 metre"));
        System.out.println(dis.getFormattedDistanceFromText("5.5 meter"));

        System.out.println(dis.getFormattedDistanceFromText("5m"));
        System.out.println(dis.getFormattedDistanceFromText("5metre"));
        System.out.println(dis.getFormattedDistanceFromText("5meter"));


        System.out.println(dis.getFormattedDistanceFromText("meter"));
        System.out.println(dis.getFormattedDistanceFromText("xx"));
        System.out.println(dis.getFormattedDistanceFromText(null));

    }

}