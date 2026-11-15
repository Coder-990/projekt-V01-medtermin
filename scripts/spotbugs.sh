./mvnw com.github.spotbugs:spotbugs-maven-plugin:4.8.6.6:spotbugs && \
mkdir -p alati/poslije && \
cp target/spotbugsXml.xml alati/poslije/spotbugs-java.xml && \
python3 spotbugs-pregled.py alati/poslije/spotbugs-java.xml | tee alati/poslije/spotbugs-java.txt