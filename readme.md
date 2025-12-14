# PKI 
PKI

## 채팅 시나리오
1. Alice는 서버에 접속 후 누구와 채팅할 것인지 정한다
2. 만약 Bob을 지정하였다면 Bob이 서버에 접속할 때까지 Alice는 대기한다.
3. Bob이 서버에 접속하였을 때 Alice가 아직 대기 중이라면 Bob과 Alice는 같은 채널로 연결한다.
4. 그리고 상대방의 공개키를(인증서) 서버로부터 전달받는다.
    - 서버 디렉토리에 인증서가 저장되어 있음
    - 파일명 = 사용자 식별키, 파일내용 = 해당 사용자의 공개키 형태로 저장
    - 서버는 고정된 비대칭키를 사용
      - 서버의 개인키로 서명된 인증서(사용자의 공개키)를 전달
      - 전달 받은 인증서는 서버의 공개키로 복호화(검증)
5. 상대방에게 메시지를 전달할 때 상대방의 공개키로 암호화하고 메시지를 수신할 땐 자신의 개인키로 복화화한다.
6. Bob과 Alice 한 명이라도 채팅 서버에서 이탈하면 시나리오는 종료

## 환경 변수
Client
   - PRIVATE_KEY_PATH 

Server
   - PRIVATE_KEY_PATH
   - PUBLIC_KEY_PATH
   - 

## 프로그램 배포 과정 
실행가능한 Jar 파일 기반 Docker Container 실행 

0. Build Jar
         
         javac -d out $(find . -name "*.java")
            •   -d out: 컴파일된 클래스 파일을 "out" 디렉토리에 저장
            •   *.java: 컴파일 대상 Java 파일들(경로 지정 가능)
         jar -cvfm MyApp.jar manifest.txt -C out .
            •   -c: 새 JAR 파일 생성
            •   -v: 생성 과정을 자세히 출력
            •   -f MyApp.jar: 생성할 JAR 파일 이름 지정
            •   -C out .: out 디렉토리의 내용을 기준으로 JAR 파일 생성
         java -jar MyApp.jar [arg1] [arg2] ...

1. Build Docker Image

2. Register Docker Image in Docker HUB


## 프로그램 실행
0. Download Docker Image over Docker Hub
1. Execute server.Server Container 
   
        docker run pki_server

2. Execute client.Client Container * 2

        docker run pki_client kim / docker run pki_client lee

## 필요한 지식
1. PKI
2. RSA
3. Docker
4. Multi thread programming in JAVA
5. TCP communication in JAVA

## 개발 환경
- OS : Ubuntu 24.04 LTS
- JAVA : openJDK 1.8.0_472
