package com.serverless;

public class EnvironmentVariable {
	
	public static final String S3BucketName_SourcePdf = System.getenv("S3BucketName_SourcePdf");
	public static final String S3Path_SourcePdf = System.getenv("S3Path_SourcePdf");
	public static final String S3BucketName_SearchablePdfDestination = System.getenv("S3BucketName_SearchablePdfDestination");
	public static final String S3Path_SearchablePdfDestination = System.getenv("S3Path_SearchablePdfDestination");
	

}
