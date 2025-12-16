javac -d out $(find . -name "*.java")
jar -cvfm server.jar manifest.txt -C out .
set -a; source .env; set +a
java -jar server.jar 2000