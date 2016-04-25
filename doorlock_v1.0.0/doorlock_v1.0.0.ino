#include <Adafruit_CC3000.h>
#include <SPI.h>
#include "utility/debug.h"
#include "utility/socket.h"

#include <DHT11.h>    //라이브러리 불러옴

// These are the interrupt and control pins
#define ADAFRUIT_CC3000_IRQ   3  // MUST be an interrupt pin!
// These can be any two pins
#define ADAFRUIT_CC3000_VBAT  5
#define ADAFRUIT_CC3000_CS    10
// Use hardware SPI for the remaining pins
// On an UNO, SCK = 13, MISO = 12, and MOSI = 11
Adafruit_CC3000 cc3000 = Adafruit_CC3000(ADAFRUIT_CC3000_CS, ADAFRUIT_CC3000_IRQ, ADAFRUIT_CC3000_VBAT,
                         SPI_CLOCK_DIVIDER); // Wifi 모듈 셋팅

#define WLAN_SSID       "AndroidHotspot4213"           // 공유기 이름(32자 이상 안됨)
#define WLAN_PASS       "123456"          // 공유기 암호
// Security can be WLAN_SEC_UNSEC, WLAN_SEC_WEP, WLAN_SEC_WPA or WLAN_SEC_WPA2
#define WLAN_SECURITY   WLAN_SEC_WPA2

#define LISTEN_PORT           4251    // 포트 4251 사용 - 아두이노앱에서 4251 포트로 전송함

Adafruit_CC3000_Server testServer(LISTEN_PORT);  // Wifi 모듈 클래스 생성 포트 4251

int mDoorLockPin = 2;   //도어락 핀 번호

int pin = 7;          //DHMT11 이 연결된 아두이노의 핀번호
int err;                //DHT11에서 데이터 읽어올때 에러판별용 변수
float temp, humi;       //temp는 온도 humi는 습도
DHT11 dht11(pin);         //7번핀을 연결한다.

void setup(void)
{
  pinMode(mDoorLockPin, OUTPUT);      // 도어락 제어 핀 설정 2번
  digitalWrite(mDoorLockPin, HIGH);   // 도어락 제어 핀 초기값 HIGH

  Serial.begin(115200);   // 시리얼 통신 속도 셋팅
  Serial.println(F("----- START DOOR LOCK CONTROL -----"));

  // 모듈 초기화
  Serial.println(F("\nInitializing..."));
  if (!cc3000.begin())
  {
    Serial.println(F("Couldn't Initializing")); // 모듈 초기화가 되지 않으면 에러
    while (1);
  }

  Serial.print(F("connect to "));
  Serial.println(WLAN_SSID);                          // 접속할 공유기 이름 출력
  if (!cc3000.connectToAP(WLAN_SSID, WLAN_PASS, WLAN_SECURITY)) {   // 공유기 접속
    Serial.println(F("Failed!"));                    // 공유기 접속 안되면 실패 출력
    while (1);
  }

  Serial.println(F("Connected!"));  // 공유기 접속 완료되면 완료 출력

  Serial.println(F("Request DHCP"));
  while (!cc3000.checkDHCP())        // 자동 IP 할당
  {
    delay(100);
  }

  // 공유기에 접속된 IP 출력
  while (! displayConnectionIp()) {
    delay(1000);
  }

  // 서버 동작 시작
  testServer.begin();

  Serial.println(F("Ready..."));
}

void loop(void)
{
  Adafruit_CC3000_ClientRef client = testServer.available();  // 서버 대기 상태
  
  if (client)// client 접속이 있으면
  {
    Serial.println("connect");   //접속됐다고 시리얼에 표시
    while (1)
    {
      if (client.available() > 0)                             // 읽어올 데이터가 있는지 확인
      {
        uint8_t ch = client.read();                            // byte로 데이터 읽음
        //client.write(ch);                                    // 클라이언트로 데이터 전송
        if (ch == 't') {                                       // 클라이언트에서 't' 가 입력되면
          Serial.println(F("Door Open/Close"));                // Door Open/Close 출력
          digitalWrite(mDoorLockPin, HIGH);                                // 2번 포트 출력 HIGH 에서
          delay(100);                                           // 휴식( 안정화 )
          digitalWrite(mDoorLockPin, LOW);                                 // 2번 포트 출력 LOW - 도어락 동작!
          delay(100);                                           // 휴식( 안정화 )
          digitalWrite(mDoorLockPin, HIGH);                                // 도어락 쪽에서도 수동동작 될 수 있도록 하기 위함
          delay(100);                                           // 휴식( 안정화 )
        }
        else if (ch == 's')                                //만약 s가 들어오면
        {
          if ((err = dht11.read(humi, temp)) == 0) //온도, 습도 읽어와서
          {
            Serial.print("temperature:");     //온도
            Serial.print(temp);                 //온도
            Serial.print(" humidity:");            //습도
            Serial.print(humi);                    //습도
            Serial.println();                     //표시

            String data = String(temp) + "," + String(humi) + "\n";   //안드로이드로 보내기 위해서 문자열로 만든다.
            for (int i = 0; i < data.length(); i++)           //문자열에서 한글자씩 안드로이드로 보낸다.
            {
              client.write(data[i]);
              Serial.print(data[i]);                         //보낸 문자를 시리얼에서 출력해본다.
              delay(10);                                     //너무 빨리보내면 안되므로 딜레이를 준다.
            }
          }
          else                                //에러일 경우 처리
          {
            Serial.println();
            Serial.print("Error No :");
            Serial.print(err);
            Serial.println();
          }
        }
      }
      delay(100);
    }
  }
}

bool displayConnectionIp(void)
{
  uint32_t ipAddress, netmask, gateway, dhcpserv, dnsserv;

  if (!cc3000.getIPAddress(&ipAddress, &netmask, &gateway, &dhcpserv, &dnsserv))
  {
    Serial.println(F("Unable to retrieve the IP Address!\r\n"));
    return false;
  }
  else
  {
    Serial.print(F("\nIP Addr: ")); cc3000.printIPdotsRev(ipAddress);
    Serial.println();
    return true;
  }
}
