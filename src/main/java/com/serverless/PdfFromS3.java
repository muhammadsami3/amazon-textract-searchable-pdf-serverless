package com.serverless;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import com.amazon.textract.pdf.ImageType;
import com.amazon.textract.pdf.PDFDocument;
import com.amazon.textract.pdf.TextLine;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.textract.model.Block;
import com.amazonaws.services.textract.model.BoundingBox;
import com.amazonaws.services.textract.model.GetDocumentTextDetectionResult;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PdfFromS3 {

	private static final Logger LOG = LogManager.getLogger(Handler.class);
	private AmazonS3 s3client;

	public PdfFromS3() {
		s3client = AmazonS3ClientBuilder.defaultClient();
	}

	public void run(String jsonBucketName, String jsonFileName) throws IOException, InterruptedException {

		LOG.info("Generating searchable pdf from: " + jsonBucketName + "/" + jsonFileName);

		// Extract text using Amazon Textract
		List<ArrayList<TextLine>> linesInPages = extractText(jsonBucketName, jsonFileName);

		String sourcePdfKeyName = getSourcePdfKeyName(jsonFileName);

		// Get input pdf document from Amazon S3
		InputStream inputPdf = getFileFromS3(EnvironmentVariable.S3BucketName_SourcePdf, sourcePdfKeyName);
		// Create new PDF document
		PDFDocument pdfDocument = new PDFDocument();

		// For each page add text layer and image in the pdf document
		PDDocument inputDocument = PDDocument.load(inputPdf);
		PDFRenderer pdfRenderer = new PDFRenderer(inputDocument);
		BufferedImage image = null;

		LOG.info("inputDocument.getNumberOfPages {}", inputDocument.getNumberOfPages());

		for (int page = 0; page < inputDocument.getNumberOfPages(); ++page) {
			image = pdfRenderer.renderImageWithDPI(page, 300, org.apache.pdfbox.rendering.ImageType.RGB);

			pdfDocument.addPage(image, ImageType.JPEG, linesInPages.get(page));

			LOG.info("Processed page index: " + page);
		}

		// Save PDF to stream
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		pdfDocument.save(os);
		pdfDocument.close();
		inputDocument.close();

		String destPdfKeyName = getDestPdfKeyName(jsonFileName);
		// Upload PDF to S3
		UploadSearchablePdfToS3(destPdfKeyName, "application/pdf", os.toByteArray());

	}

//	private getPagesFromArray

	private List<ArrayList<TextLine>> extractText(String bucketName, String jsonFileName) throws InterruptedException {

		List<ArrayList<TextLine>> pages = new ArrayList<ArrayList<TextLine>>();
		ArrayList<TextLine> page = null;
		BoundingBox boundingBox = null;
		GetDocumentTextDetectionResult[] textDetectionResultArr = null;
		GetDocumentTextDetectionResult textDetectionResult = null;
		List<Block> blocks = null;

		try {
			// create object mapper instance
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

			InputStream inputJson = getFileFromS3(bucketName, jsonFileName);
			if (inputJson.toString().startsWith("[")) {

				textDetectionResultArr = mapper.readValue(inputJson, GetDocumentTextDetectionResult[].class);
				if (textDetectionResultArr.length > 0) {

					blocks = textDetectionResultArr[0].getBlocks();
				}
			} else {

				textDetectionResult = mapper.readValue(inputJson, GetDocumentTextDetectionResult.class);
				blocks = textDetectionResult.getBlocks();

			}

			LOG.info("blocks size: " + blocks.size());

			for (Block block : blocks) {
				if (block.getBlockType().equals("PAGE")) {
					page = new ArrayList<TextLine>();
					pages.add(page);
				} else if (block.getBlockType().equals("LINE")) {

					boundingBox = block.getGeometry().getBoundingBox();
					page.add(new TextLine(boundingBox.getLeft(), boundingBox.getTop(), boundingBox.getWidth(),
							boundingBox.getHeight(), block.getText()));
				}
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		LOG.info("pages size: {} ", pages.size());

		return pages;
	}

	private InputStream getFileFromS3(String bucketName, String fileName) throws IOException {

		com.amazonaws.services.s3.model.S3Object fullObject = this.s3client
				.getObject(new GetObjectRequest(bucketName, fileName));
		InputStream in = fullObject.getObjectContent();
		LOG.info("getFileFromS3 bucketName: {},fileName: {} ", bucketName, fileName);
		return in;
	}

	private void UploadSearchablePdfToS3(String keyName, String contentType, byte[] bytes) {

		ByteArrayInputStream baInputStream = new ByteArrayInputStream(bytes);
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(bytes.length);
		metadata.setContentType(contentType);

		PutObjectRequest putRequest = new PutObjectRequest(EnvironmentVariable.S3BucketName_SearchablePdfDestination,
				keyName, baInputStream, metadata);

		this.s3client.putObject(putRequest);

		LOG.info("Generated searchable pdf: {}, destination bucket:",
				EnvironmentVariable.S3Path_SearchablePdfDestination + "/" + keyName,
				EnvironmentVariable.S3BucketName_SearchablePdfDestination);
	}

	private String getSourcePdfKeyName(String jsonFileName) {
		String keyName = getFileName(jsonFileName);

		if (EnvironmentVariable.S3Path_SourcePdf.endsWith("/") || EnvironmentVariable.S3Path_SourcePdf.isEmpty()) {
			keyName = EnvironmentVariable.S3Path_SourcePdf + keyName;
		} else {
			keyName = EnvironmentVariable.S3Path_SourcePdf + '/' + keyName;
		}

		LOG.debug("getSourcePdfKeyName: jsonFileName " + jsonFileName + ",keyName:" + keyName);

		return keyName;
	}

	private String getDestPdfKeyName(String jsonFileName) {
		String keyName = getFileName(jsonFileName);

		if (EnvironmentVariable.S3Path_SourcePdf.endsWith("/") || EnvironmentVariable.S3Path_SourcePdf.isEmpty()) {
			keyName = EnvironmentVariable.S3Path_SearchablePdfDestination + keyName;
		} else {
			keyName = EnvironmentVariable.S3Path_SearchablePdfDestination + '/' + keyName;
		}
		LOG.debug("getDestPdfKeyName: jsonFileName " + jsonFileName + ",keyName:" + keyName);
		return keyName;
	}

	private String getFileName(String jsonFileName) {
		Path path = Paths.get(jsonFileName);
		String fileName = path.getFileName().toString().replaceAll(".json", ".pdf");
		LOG.debug("getFileName: jsonFileName " + jsonFileName + ",fileName:" + fileName);
		return fileName;
	}
}
