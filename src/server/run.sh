PORT=$1

javac -d out $(find . -name "*.java")
jar -cvfm server.jar manifest.txt -C out .
set -a; source .env; set +a
java -jar server.jar "$PORT"