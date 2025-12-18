HOST=$1
PORT=$2
USER1=$3
USER2=$4

javac -d out $(find . -name "*.java")
jar -cvfm client.jar manifest.txt -C out .
set -a; source .env; set +a
java -jar client.jar "$HOST" "$PORT" "$USER1" "$USER2"