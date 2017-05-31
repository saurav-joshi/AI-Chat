package com.aibot.qa;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class S3Handler implements Serializable {

    public static final String accessKey = "AKIAJKSZZOYOAH7TKBLQ";
    public static final String secretKey = "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";


    public static List<String> getFilesInFolder(String bucketName, String folderName) {
        AmazonS3 s3 = new AmazonS3Client(new BasicAWSCredentials(accessKey, secretKey));

        ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
            .withBucketName(bucketName)
            .withPrefix(folderName + "/");

        List<String> files = new ArrayList<>();

        ObjectListing objectListing;
        do {
            objectListing = s3.listObjects(listObjectsRequest);
            files.addAll(
                objectListing.getObjectSummaries().stream()
                .map(S3ObjectSummary::getKey).collect(Collectors.toList())
            );
            listObjectsRequest.setMarker(objectListing.getNextMarker());
        } while (objectListing.isTruncated());

        System.out.println("Read from S3  ==>> "+ files);
        return files;
    }

    public static List<String> readLinesFromFile(String bucketName, String key) throws IOException {
        AmazonS3 s3 = new AmazonS3Client(new BasicAWSCredentials(accessKey, secretKey));
        S3Object s3object = s3.getObject(new GetObjectRequest(bucketName, key));
        List<String> lines = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(s3object.getObjectContent(), "8859_1"));
        String line;
        while((line = reader.readLine()) != null) {
            //System.out.println(line);
            lines.add(line.trim());
        }

//        System.out.println(lines.get(0));
        return lines;
    }

    public static void writeLinesToFile(String bucketName, String key, List<String> lines) throws IOException {
        AmazonS3 s3 = new AmazonS3Client(new BasicAWSCredentials(accessKey, secretKey));

        byte[] data = lines.stream().collect(Collectors.joining("\n")).getBytes(Charset.defaultCharset());
        ObjectMetadata metaData = new ObjectMetadata();
        metaData.setContentLength(data.length);
        s3.putObject(new PutObjectRequest(bucketName, key, new ByteArrayInputStream(data), metaData));
    }

}
