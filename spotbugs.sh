./mvnw com.github.spotbugs:spotbugs-maven-plugin:4.8.6.6:spotbugs && \
mkdir -p alati/prije && \
cp target/spotbugsXml.xml alati/prije/spotbugs-java.xml && \
python3 spotbugs-pregled.py alati/prije/spotbugs-java.xml | tee alati/prije/spotbugs-java.txt