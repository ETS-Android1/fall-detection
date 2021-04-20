#include "BluetoothSerial.h"
#include <Wire.h>
#include <Adafruit_Sensor.h>
#include <Adafruit_ADXL345_U.h>

#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)
#error Bluetooth is not enabled! Please run `make menuconfig` to and enable it
#endif

BluetoothSerial SerialBT;

/* Assign a unique ID to this sensor at the same time */
Adafruit_ADXL345_Unified accel = Adafruit_ADXL345_Unified(12345);
 
double previous_x = 0;
double previous_y = 0;
double previous_z = 0;
bool check_post_fall = false;

void displaySensorDetails(void)
{
  sensor_t sensor;
  accel.getSensor(&sensor);
  Serial.println("------------------------------------");
  Serial.print  ("Sensor:       "); Serial.println(sensor.name);
  Serial.print  ("Driver Ver:   "); Serial.println(sensor.version);
  Serial.print  ("Unique ID:    "); Serial.println(sensor.sensor_id);
  Serial.print  ("Max Value:    "); Serial.print(sensor.max_value); Serial.println(" m/s^2");
  Serial.print  ("Min Value:    "); Serial.print(sensor.min_value); Serial.println(" m/s^2");
  Serial.print  ("Resolution:   "); Serial.print(sensor.resolution); Serial.println(" m/s^2");
  Serial.println("------------------------------------");
  Serial.println("");
  delay(3000);
}

void setup(void)
{
  Serial.begin(115200);

  SerialBT.begin("ESP32test"); //Bluetooth device name
  Serial.println("The device started, now you can pair it with bluetooth!");
  Serial.println("");

  Serial.println("Accelerometer Test"); Serial.println("");
  /* Initialise the sensor */
  if(!accel.begin())
  {
    /* There was a problem detecting the ADXL345 ... check your connections */
    Serial.println("Ooops, no ADXL345 detected ... Check your wiring!");
    while(1);
  }

  /* Set the range to appropriate value for your project */
  accel.setRange(ADXL345_RANGE_16_G);
  // accel.setRange(ADXL345_RANGE_8_G);
  // accel.setRange(ADXL345_RANGE_4_G);
  // accel.setRange(ADXL345_RANGE_2_G);

/* Display some basic information on this sensor */
  displaySensorDetails();
}

void loop(void)
{
  /* Get a new sensor event */
  sensors_event_t event;
  accel.getEvent(&event);

  /* Display the results (acceleration is measured in m/s^2) */
  Serial.print("X: "); Serial.print(event.acceleration.x); Serial.print("  ");
  Serial.print("Y: "); Serial.print(event.acceleration.y); Serial.print("  ");
  Serial.print("Z: "); Serial.print(event.acceleration.z); Serial.print("  ");Serial.println("m/s^2 ");
  double net_accel = sqrt( sq(event.acceleration.x) + sq(event.acceleration.y) + sq(event.acceleration.z) );
  Serial.print("Net Acceleration: "); Serial.print (net_accel); Serial.print("\n");

  if (check_post_fall == true)
  {
    delay(500);
    if(net_accel > 10)
    {
      Serial.println("Fall has occured");
      SerialBT.write(1);
      check_post_fall = false;
      goto next_loop;
    }

  }

  if( (abs(event.acceleration.x - previous_x) > 8) ||
      (abs(event.acceleration.y - previous_y) > 8) ||
      (abs(event.acceleration.z - previous_z) > 8) )
      {
        Serial.println("Checking for fall");
        check_post_fall = true;
      }

   delay(500);

   next_loop:
   previous_x = event.acceleration.x;
   previous_y = event.acceleration.y;
   previous_z = event.acceleration.z;
    
}