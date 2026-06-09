FROM docker.elastic.co/elasticsearch/elasticsearch:9.2.8
RUN bin/elasticsearch-plugin install --batch analysis-nori