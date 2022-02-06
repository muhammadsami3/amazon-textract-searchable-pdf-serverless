package com.serverless;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.event.S3EventNotification;

public class Handler implements RequestHandler<S3Event, String> {

	private static final Logger LOG = LogManager.getLogger(Handler.class);

	@Override
	public String handleRequest(S3Event event, Context ctx) {
		LOG.info("1 --- received: {}", event.toString());

		S3EventNotification.S3EventNotificationRecord record = event.getRecords().get(0);

		String bucketName = record.getS3().getBucket().getName();
		String keyName = record.getS3().getObject().getKey();
		String keyNameLower = record.getS3().getObject().getKey().toLowerCase();

		LOG.info("Bucket Name: {}" , bucketName);
		LOG.info("File Path: {} ", keyName);

		try {
			if (keyNameLower.endsWith("json")) {
				PdfFromS3 s3Pdf = new PdfFromS3();
				
				s3Pdf.run(bucketName, keyName);

			} else {
				LOG.error("Invalid file extention file recieved: {}", keyName);
			}
		} catch (Exception e) {
			e.printStackTrace();
			LOG.error(e.getMessage());
		}
		return null;
	}
}
