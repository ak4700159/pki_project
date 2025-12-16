javac -d out $(find . -name "*.java")
jar -cvfm client.jar manifest.txt -C out .
set -a; source .env; set +a
java -jar client.jar localhost 2000 kim lee