FROM openjdk:8-jdk-alpine

# Install Python, Scala, and other dependencies
RUN apk add --no-cache python3 py3-pip bash

# Install Tesseract
RUN apk --no-cache add tesseract-ocr

# Install Scala (version based on Databricks runtime)
ENV SCALA_VERSION 2.12.10
ENV SCALA_HOME /usr/share/scala
RUN wget https://downloads.lightbend.com/scala/$SCALA_VERSION/scala-$SCALA_VERSION.tgz && \
    tar xzf scala-$SCALA_VERSION.tgz && \
    mv scala-$SCALA_VERSION $SCALA_HOME && \
    rm scala-$SCALA_VERSION.tgz && \
    ln -s $SCALA_HOME/bin/* /usr/bin/

# Install Spark (version based on Databricks runtime)
ENV SPARK_VERSION 3.5.0
ENV SPARK_HOME /usr/local/spark
RUN wget https://archive.apache.org/dist/spark/spark-$SPARK_VERSION/spark-$SPARK_VERSION-bin-hadoop3.tgz && \
    tar -xzf spark-$SPARK_VERSION-bin-hadoop3.tgz && \
    mv spark-$SPARK_VERSION-bin-hadoop3 $SPARK_HOME && \
    rm spark-$SPARK_VERSION-bin-hadoop3.tgz

# Install Maven
ENV MAVEN_VERSION 3.9.6
ENV MAVEN_HOME /usr/lib/mvn
RUN wget http://apache.osuosl.org/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz && \
    tar -zxvf apache-maven-$MAVEN_VERSION-bin.tar.gz && \
    mv apache-maven-$MAVEN_VERSION /usr/lib/mvn && \
    rm apache-maven-$MAVEN_VERSION-bin.tar.gz
ENV PATH $PATH:$SCALA_HOME/bin:$SPARK_HOME/bin:$MAVEN_HOME/bin

# Set work directory
WORKDIR /app

# Copy the Maven pom.xml and src directory to /app
COPY pom.xml /app/
COPY src /app/src
COPY target /app/target

# Expose the port Spark will use
EXPOSE 4040

# Optional: Run Maven dependency resolution automatically when the image is built
RUN mvn dependency:resolve

# Optional: Run Maven package to compile the project when the image is built
CMD ["mvn", "package"]
