FROM ubuntu:16.04

RUN apt-get -y update && \
    apt-get -y clean && \
    apt-get -y install  ant wget unzip xsltproc libxml2-utils xmlstarlet less vim

RUN apt-get -y install software-properties-common python-software-properties && \
    add-apt-repository ppa:webupd8team/java && \
    apt-get update && \
    echo oracle-java7-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections && \
    apt-get -y install oracle-java7-installer

ENV JAVA_HOME "/usr/lib/jvm/java-7-oracle"

RUN mkdir -p /app && \
    mkdir -p /app/lib && \
    mkdir -p /app/bin

ENV PATH "/app/bin:$PATH"

WORKDIR /app

RUN mkdir /tmp/saxon && \
    cd /tmp/saxon && \
    wget -O SaxonHE9-7-0-14J.zip "http://downloads.sourceforge.net/project/saxon/Saxon-HE/9.7/SaxonHE9-7-0-14J.zip?r=&use_mirror=netix" && \
    unzip "SaxonHE9-7-0-14J.zip" && \
    mkdir -p /app/lib/saxon && \
    cd /app/lib/saxon && \
    unzip /tmp/saxon/SaxonHE9-7-0-14J.zip
ADD XSL/xsl2 /app/bin/xsl2
RUN chmod +x /app/bin/xsl2

RUN mkdir -p /tmp/NVDL && \
    cd /tmp/NVDL && \
    wget https://github.com/relaxng/jing-trang/archive/V20131210.zip && \
    unzip V20131210.zip &&\
    cd jing-trang-20131210 && \
    ./ant && \
    mkdir -p /app/lib/NVDL/ && \
    cp -r build/* /app/lib/NVDL/
ADD NVDL/nvdl /app/bin/nvdl
RUN chmod +x /app/bin/nvdl

ADD IDDF-validate /app/lib/IDDF-validate
RUN mv /app/lib/IDDF-validate/IDDF-validate /app/bin/IDDF-validate && \
    chmod +x /app/bin/IDDF-validate /app/lib/IDDF-validate/schematron/sch-validate

ADD IPA /tmp/IPA
RUN cd /tmp/IPA && \
    ant

ADD DTL /tmp/DTL
RUN cd /tmp/DTL && \
    cp /tmp/IPA/dist/lib/IPA.jar /tmp/DTL/lib/ && \
    CLASSPATH=/tmp/DTL/lib/antlr.jar ant && \
    mkdir -p /app/lib/DTL && \
    cp /tmp/DTL/lib/*.jar /app/lib/DTL/ && \
    cp /tmp/DTL/DTLengine /app/bin && \
    chmod +x /app/bin/DTLengine

ENTRYPOINT ["/bin/bash", "-l"]

#RUN rm -rf /var/lib/apt/lists/* && \
#    rm -rf /tmp/*
