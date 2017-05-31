package com.aibot.qa;

import java.util.Arrays;
import java.util.List;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;

import org.apache.spark.mllib.classification.LogisticRegressionModel;
import org.apache.spark.mllib.classification.LogisticRegressionWithSGD;
import org.apache.spark.mllib.feature.HashingTF;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.regression.LabeledPoint;

public class IntentTrain {

    final HashingTF tf = new HashingTF(20);

    public void train(List<String> dataByClass, String modelFile, JavaSparkContext sc){
        //0: greeting
        //1: similar restaurant
        JavaRDD<LabeledPoint> trainingData = sc.emptyRDD();

        for(int i=0; i<dataByClass.size(); i++){
            int label = i;
            String data = dataByClass.get(i);
            JavaRDD<String> rddData = sc.textFile(data);
            JavaRDD<LabeledPoint> examples = rddData.map(new Function<String, LabeledPoint>() {
                @Override public LabeledPoint call(String email) {
                    return new LabeledPoint(label, tf.transform(Arrays.asList(email.split(" "))));
                }
            });
            trainingData.union(examples);
        }
        trainingData.cache(); // Cache data since Logistic Regression is an iterative algorithm.

        // Create a Logistic Regression learner which uses the LBFGS optimizer.
        LogisticRegressionWithSGD lrLearner = new LogisticRegressionWithSGD();
        // Run the actual learning algorithm on the training data.
        LogisticRegressionModel model = lrLearner.run(trainingData.rdd());
        //Save model
        model.save(sc.sc(), modelFile);
        //sc.stop();
    }

    public void test(List<String> examples, String modelFile, JavaSparkContext sc){
        LogisticRegressionModel model = LogisticRegressionModel.load(sc.sc(),modelFile);
        examples.forEach(e ->{
            Vector vectorTestExample =
                    tf.transform(Arrays.asList(e.split(" ")));
            System.out.println("Prediction for positive test example: " + model.predict(vectorTestExample));
        });
    }
}
