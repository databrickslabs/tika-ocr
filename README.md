# tika-ocr-inputformat

**Digitization of documents with Tika on Databricks** : *The volume of available data is growing by the second. 
About [64 zettabytes](https://www.wsj.com/articles/how-to-understand-the-data-explosion-11638979214) was created 
or copied last year, according to IDC, a technology market research firm. By 2025, this number will grow to an 
estimated [175 zetabytes](https://www.statista.com/statistics/871513/worldwide-data-created/), and it is becoming 
increasingly granular and difficult to codify, unify, and centralize. And though more financial services institutions 
(FSIs) are talking about big data and using technology to capture more data than ever, Forrester reports that 70% of
all data within an enterprise still goes unused for analytics. The open source nature of Lakehouse for Financial 
Services makes it possible for bank compliance officers, insurance underwriting agents or claim adjusters to combine 
latest technologies in optical character recognition (OCR) and natural language processing (NLP) in order to transform 
any financial document, in any format, into valuable data assets. The Apache Tika toolkit detects and extracts 
metadata and text from over a thousand different file types (such as PPT, XLS, and PDF). Combined with 
[Tesseract](https://en.wikipedia.org/wiki/Tesseract_(software), the most commonly used OCR technology, there is 
literally no limit to what files we can ingest, store and exploit for analytics / operation purpose.*

## Project Support
Please note that all projects in the /databrickslabs github account are provided for your exploration only, 
and are not formally supported by Databricks with Service Level Agreements (SLAs). 
They are provided AS-IS and we do not make any guarantees of any kind. 
Please do not submit a support ticket relating to any issues arising from the use of these projects.

Any issues discovered through the use of this project should be filed as GitHub Issues on the Repo. 
They will be reviewed as time permits, but there are no formal SLAs for support.

## Building the Project

```
mvn clean install
```

## Deploying / Installing the Project

```
mvn release:prepare
```

## Releasing the Project

```
mvn release:perform
```

## Using the Project

Add `com.databricks.labs:tika-ocr:0.1` maven dependency to your databricks runtime.
Alternatively, compile this project with maven profile `shaded` enabled to generate an uber jar that you upload to 
your databricks runtime. You can now read any file, extracting text content from any file format.

```python
spark.read.format('tika').load(path_to_any_file)
```

|                path|length|    modificationTime|             content|         contentType|         contentText|     contentMetadata|
| ------------------ | ---- | ------------------ | ------------------ | ------------------ | ------------------ | ------------------ |
|file:/Users/antoi...| 36864|2022-08-25 14:15:...|[D0 CF 11 E0 A1 B...|  application/msword|key\n\nvalue\n\nh...|{meta:page-count ...|
|file:/Users/antoi...| 34030|2022-08-25 14:16:...|[89 50 4E 47 0D 0...|           image/png|key\n\nvalue\n\nh...|{tiff:BitsPerSamp...|
|file:/Users/antoi...| 26294|2022-08-25 14:13:...|[50 4B 03 04 14 0...|application/vnd.o...|\n\n\nimage1.png\...|{meta:page-count ...|
|file:/Users/antoi...| 22805|2022-08-25 14:13:...|[25 50 44 46 2D 3...|     application/pdf|\n \n \n\n \n\nke...|{dc:format -> app...|

For Tesseract support, please make sure Tesseract library is available on each executor. This can be achieved using a 
simple [init script](https://docs.databricks.com/clusters/init-scripts.html) as follows

```shell
#!/usr/bin/env bash
sudo apt-get install -y tesseract-ocr
```