# Description
This service generates a searchable PDF from a non-searchable PDF and the corresponding textract .json in an S3 bucket (no additional textract call should be used). Output is the same PDF but searchable in another S3 bucket. It also work with multi-page PDF documents. 

You can see an example of [searchable PDF document](https://github.com/muhammadsami3/amazon-textract-searchable-pdf-serverless/raw/master/out_testpdf.pdf) that is generated using [Amazon Textract .json](https://raw.githubusercontent.com/muhammadsami3/amazon-textract-searchable-pdf-serverless/e6ddd8fd9a698c156c430e8850038fc70cb73507/testpdf.json) from a [scanned document](https://github.com/muhammadsami3/amazon-textract-searchable-pdf-serverless/raw/e6ddd8fd9a698c156c430e8850038fc70cb73507/testpdf.pdf). While text is locked in images in the scanned document, you can select, copy, and search text in the searchable PDF document.

# Pre-requisites
- Install node and npm
- Install the [Serverless Framework installed](https://serverless.com/framework/docs/providers/aws/guide/quick-start/)with an AWS account set up.
- Install [Oracle JDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html) and NOT Java JRE. and export JAVA_HOME
- Install [Apache Maven](https://maven.apache.org/). After [downloading](https://maven.apache.org/download.html) and [installing](https://maven.apache.org/install.html) Apache Maven, add the apache-maven-x.x.x folder to the PATH environment variable.
  

# Testing Pre-requisites
```
$ java -version
openjdk version "11.0.13" 2021-10-19
OpenJDK Runtime Environment (build 11.0.13+8-Ubuntu-0ubuntu1.20.04)
OpenJDK 64-Bit Server VM (build 11.0.13+8-Ubuntu-0ubuntu1.20.04, mixed mode, sharing) 
```
```
$ mvn -v
Apache Maven 3.6.3
Maven home: /usr/share/maven
Java version: 11.0.13, vendor: Ubuntu, runtime: /usr/lib/jvm/java-11-openjdk-amd64
Default locale: en_US, platform encoding: UTF-8
OS name: "linux", version: "5.4.0-97-generic", arch: "amd64", family: "unix"
```
# Update the environment variable in the serverless.yml
- S3BucketName_TextractJsonFile: The existing S3 bucket that will trigger the Lambda once a new textract json output get uploaded.
- S3Path_TextractJsonFile: The existing S3 bucket path to the json output.
- S3BucketName_SourcePdf: The existing S3 bucket that contains the non searchable pdf.
- S3Path_SourcePdf: The existing S3 bucket path that contains the non searchable pdf.
- S3BucketName_SearchablePdfDestination: an existing S3 bucket destination for the  searchable pdf.
- S3Path_SearchablePdfDestination: an existing S3 bucket path as a destination for the  searchable pdf.

# Deployment
1. `$ mvn clean package`
2. `$ sls deploy` # you can use --aws-s3-accelerate for faster uploading but it comes with extra charges
3. default aws-region is eu-west-1 # it can be changed from the serverless.yml file.


# Testing 
1. Upload the corresponding textract .json to the \${S3bucketName_TextractJsonFile}/\${S3Path_TextractJsonFile}.
2. It should trigger the created Lambda.
3. Lambda will grab the original pdf from \${S3BucketName_SourcePdf}/${S3Path_SourcePdf}.
4. The source .json file and .pdf must have the same name ex. test.json and test.pdf.
5. Output is the same PDF but searchable in \${S3BucketName_SearchablePdfDestination}/\${S3Path_SearchablePdfDestination}.

# Ref
- https://aws.amazon.com/blogs/machine-learning/generating-searchable-pdfs-from-scanned-documents-automatically-with-amazon-textract/
- https://www.serverless.com/blog/how-to-create-a-rest-api-in-java-using-dynamodb-and-serverless
- https://medium.com/@rostyslav.myronenko/the-serverless-framework-for-an-aws-serverless-java-application-42beba675283