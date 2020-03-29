package org.cv.app;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.log4j.Logger;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;

public class CvLauncher {
    static final Logger logger = Logger.getLogger(CvLauncher.class);

    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        Config config = ConfigFactory.load();
        ApiProperties apiProperties = new ApiProperties(config);
        String cameraID = "0";
        String topic = "videoframe";

        VideoCapture videoCapture = new VideoCapture(Integer.parseInt(cameraID));
        Mat frame = new Mat();
        boolean flag=videoCapture.read(frame);
        System.out.println(flag);

        while (videoCapture.isOpened()) {
            Imgproc.resize(frame, frame, new Size(640, 480), 0, 0, Imgproc.INTER_CUBIC);
            int cols = frame.cols();
            int rows = frame.rows();
            int type = frame.type();
            byte[] data = new byte[(int) (frame.total() * frame.channels())];
            frame.get(0, 0, data);
            String timeStamp = new Timestamp(System.currentTimeMillis()).toString();
            VideoBean videoFrame = VideoBean.newBuilder().setCameraId(cameraID)
                    .setColumns(cols)
                    .setRows(rows)
                    .setDataType(type)
                    .setTimeStamp(timeStamp)
                    .setData(new String(data, StandardCharsets.UTF_8))
                    .build();

            ProducerRecord<String, VideoBean> producerRecord = new ProducerRecord<>(topic, cameraID, videoFrame);
            try (Producer<String, VideoBean> kafkaProducer = new KafkaProducer<>(apiProperties.getKafkaProperties())) {
                kafkaProducer.send(producerRecord, (recordMetadata, exception) -> {
                    if (recordMetadata != null) {
                        logger.info("CameraID: " + cameraID + " and Partition Number: " + recordMetadata.partition());
                    }
                    if (exception != null) {
                        exception.printStackTrace();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}