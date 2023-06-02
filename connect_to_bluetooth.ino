#include "BluetoothSerial.h"
#include <Arduino.h>


/* Check if Bluetooth configurations are enabled in the SDK */
/* If not, then you have to recompile the SDK */
#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)
#error Bluetooth is not enabled! Please run `make menuconfig` to and enable it
#endif

BluetoothSerial SerialBT;
int readData[2];

#define LEFT 26 //use pin D26 to control left motor DAC2
#define RIGHT 25 //use pin D25 to control right motor DAC1

const int volt0 = 0; //max speed
const int volt1 = 50; // medium speed
const int volt2 = 100; //min speed
const int volt3 = 255; //stop
int checkUp = 0;
void setup() {
  Serial.begin(115200);
   // Set a timeout value for reading data
  /* If no name is given, default 'ESP32' is applied */
   SerialBT.begin("DucVu");  //Bluetooth device name
  /* If you want to give your own name to ESP32 Bluetooth device, then */
  /* specify the name as an argument SerialBT.begin("myESP32Bluetooth)*/
  SerialBT.begin();
  Serial.println("Bluetooth Started! Ready to pair...");

  //setup PIN MODE
  //pinMode(LEFT, OUTPUT);
  //pinMode(RIGHT, OUTPUT);
  //setup DAC
  //dac_output_enable(DAC_CHANNEL_1); // Enable DAC1 is GPIO25
  //dac_output_enable(DAC_CHANNEL_2); // Enable DAC2 is GPIO26
}
/*
  // Set DAC1 voltage level
  //desiredVoltage1 is a value of Volt output, have 8 bit: 0->255 with 0V -> 3.3V
  dac_output_voltage(DAC_CHANNEL_1, desiredVoltage1);

  // Set DAC2 voltage level
  dac_output_voltage(DAC_CHANNEL_2, desiredVoltage2);
*/
void loop() {
  //ESP32 to android: Serial
  if (Serial.available())
  {
    SerialBT.write(Serial.read());
  }
  // Android to ESP32: SerialBT
  if (SerialBT.available())
  {
    //Serial.write(SerialBT.read());
    for(int i = 0; i < 2; i++) {
    readData[i] = SerialBT.read();
    }
    /*
    76: Turn Left: 'L' letter from android app
    82: Turn Right 'R' letter from android app
    85: Up 'U' letter from android app
    68: Down 'D' letter from android app

    49: ON '1' letter from android app
    48: OFF '0' letter from android app

    MOTOR LEFT is DAC2 | D26
    MOTOR RIGHT is DAC1 | D25

    */
    if(readData[1] == 49) { //ON
      if(readData[0] == 85) {
        //stop down
        Serial.println("\n ok, Up");
        checkUp = 1;
        dacWrite(LEFT, volt0);
        dacWrite(RIGHT, volt0);
      }
      else if(readData[0] == 76 && checkUp == 1) {
        //stop turn right
        Serial.println("\n ok, Turn Left");
        //motor right max speed, motor left min speed
         dacWrite(RIGHT, volt0);
         dacWrite(LEFT, volt2);
      }
      else if(readData[0] == 82 && checkUp == 1) {
        //Stop turn left
        Serial.println("\n ok, Turn Right");
        //motor left max speed, motor right min speed
         dacWrite(RIGHT, volt2);
         dacWrite(LEFT, volt0);
      }
      else if(readData[0] == 68) {
        //stop up
        Serial.println("\n ok, Down");
      }
    }
    else if(readData[1] == 48) {
      if(readData[0] == 76 && checkUp == 1) {
        Serial.println("\n Stop Turn Left");
        dacWrite(LEFT, volt0);
        dacWrite(RIGHT, volt0);
      }
      else if(readData[0] == 82 && checkUp == 1) {
        Serial.println("\n Stop Turn Right");
        dacWrite(LEFT, volt0);
        dacWrite(RIGHT, volt0);
      }
      else if(readData[0] == 85) {
        Serial.println("\n Stop Up");
        checkUp = 0;
        dacWrite(LEFT, volt3);
        dacWrite(RIGHT, volt3);
      }
      else if(readData[0] == 68) {
        Serial.println("\n Stop Down");
      }
    }
  }
  delay(20);
}