# Use Ubuntu 22.04 LTS as base image
FROM ubuntu:22.04

# Setup environment
ENV JAVA_HOME /usr/lib/jvm/zulu8.72.0.17-ca-jre8.0.382-linux_x64
ENV SCALA_VERSION 2.12.15
ENV SPARK_VERSION 3.5.0
ENV MAVEN_VERSION 3.9.6
ENV PATH $JAVA_HOME/bin:$PATH

# Install dependencies
RUN apt-get update && \
    apt-get install -y wget unzip software-properties-common && \
    # Create /usr/lib/jvm directory
    mkdir -p /usr/lib/jvm && \
    # Install specific version of Java
    wget https://cdn.azul.com/zulu/bin/zulu8.72.0.17-ca-jre8.0.382-linux_x64.tar.gz && \
    tar zxvf zulu8.72.0.17-ca-jre8.0.382-linux_x64.tar.gz -C /usr/lib/jvm/ && \
    rm zulu8.72.0.17-ca-jre8.0.382-linux_x64.tar.gz && \
    update-alternatives --install /usr/bin/java java $JAVA_HOME/bin/java 1 && \
    # Install specific version of Scala
    wget https://downloads.typesafe.com/scala/$SCALA_VERSION/scala-$SCALA_VERSION.tgz && \
    tar xzf scala-$SCALA_VERSION.tgz && \
    mv scala-$SCALA_VERSION /usr/lib && \
    ln -s /usr/lib/scala-$SCALA_VERSION /usr/lib/scala && \
    echo "PATH=\"/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/lib/scala/bin\"" >> /etc/environment && \
    # Install tesseract-ocr
    apt-get install -y tesseract-ocr && \
    # Cleanup
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Install Spark (version based on Databricks runtime)
ENV SPARK_HOME /usr/local/spark
RUN wget https://archive.apache.org/dist/spark/spark-$SPARK_VERSION/spark-$SPARK_VERSION-bin-hadoop3.tgz && \
    tar -xzf spark-$SPARK_VERSION-bin-hadoop3.tgz && \
    mv spark-$SPARK_VERSION-bin-hadoop3 $SPARK_HOME && \
    rm spark-$SPARK_VERSION-bin-hadoop3.tgz

# Install Maven
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
CMD ["mvn", "-Pshaded", "package"]
