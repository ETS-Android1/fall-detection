### Fall detection sensor that alerts an app which sends an SMS to close contacts and/or medical help.
### ❄ What it does
Uses an accelerometer to detect a fall and send a ping using serial blue tooth communication to an Android app that on receiving the ping sends a SOS message to emergency contacts.

Technical Details:
------------------
* Bluetooth : The App implements RFCOMM connection to the well-known SPP UUID 00001101-0000-1000-8000-00805F9B34FB
* ESP32: Development board which comes with Wi-Fi, Bluetooth Low Energy and Bluetooth Classic
* ADXL345 sensor: The ADXL345 is a small, thin, low power, 3-axis accelerometer with high resolution (13-bit) measurement at up to ±16g 

🌀 ESP32 and ADXL345 sensor : 
-----------------------------------------------------------------------------------------------------------------------
In order to read accelerometer values from the ESP32 development board, we need to connect:
1. GND pin of ADXL345 to one of the GND pin on the ESP32 development board
2. 3v3 pin of ADXL345 to one of the 3v3 pin on the ESP32 development board
3. SDA pin of ADXL345 to GPIO21 on the ESP32 development board
4. SCL pin of ADXL345 to GPIO22 pin on the ESP32 development board


### 💎  Schematics
![Fritzing](Sketch/Fritzing.png)

### 💎  ESP32 Board
![ESP32](Utilities/ESP32.PNG)

### 💎  Pictures
![Picture 1](Utilities/IMAG0601.jpg)
![Picture 2](Utilities/IMAG0602.jpg)
