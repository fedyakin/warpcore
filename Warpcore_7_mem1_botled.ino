#include "FastLED.h"

// For led chips like Neopixels, which have a data line, ground, and power, you just
// need to define DATA_PIN.  For led chipsets that are SPI based (four wires - data, clock,
// ground, and power), like the LPD8806, define both DATA_PIN and CLOCK_PIN
#define DATA_PIN 3
#define CLOCK_PIN 9
#define Serial1Speed 115200

// How many LEDs in your strip?
#define NUM_LEDS 37		// Total number of LEDs

// How are the LEDs distributed?
#define SegmentSize 5		// How many LEDs in each "Magnetic Constrictor" segment
#define TopLEDcount 20		// LEDs above the "Reaction Chamber"
#define ReactionLEDcount 2	// LEDs inside the "Reaction Chamber"
#define BottomLEDcount 15	// LEDs below the "Reaction Chamber"

// Default Settings
#define DefaultWarpFactor 2	// 1-9
#define DefaultMainHue 160	// 1-255	1=Red 32=Orange 64=Yellow 96=Green 128=Aqua 160=Blue 192=Purple 224=Pink 255=Red
#define DefaultSaturation 255	// 1-255
#define DefaultBrightness 160	// 1-255
#define DefaultPattern 1	// 1-5		1=Standard 2=Breach 3=Rainbow 4=Fade 5=Slow Fade


// Initialize internal variables
#define PulseLength SegmentSize*2
#define TopChases TopLEDcount/PulseLength+1*PulseLength
#define TopLEDtotal TopLEDcount+ReactionLEDcount
#define TopDiff TopLEDcount-BottomLEDcount
#define RateMultiplier 6
byte MainHue = DefaultMainHue;
byte ReactorHue = DefaultMainHue;
byte LastHue = DefaultMainHue;
byte WarpFactor = DefaultWarpFactor;
byte LastWarpFactor = DefaultWarpFactor;
byte Rate = RateMultiplier * WarpFactor;
byte Saturation = DefaultSaturation;
byte Brightness = DefaultBrightness;
byte Pattern = DefaultPattern;
byte Pulse;
boolean Rainbow = false;
boolean Fade = false;
boolean SlowFade = false;// Default Settings

// Serial1 input variables
const byte numChars = 20;
char receivedChars[numChars];
char tempChars[numChars];	// temporary array for use when parsing

// Parsing variables
byte warp_factor = WarpFactor;
byte hue = MainHue;
byte saturation = Saturation;
byte brightness = Brightness;
byte pattern = Pattern;

bool newData = false;

// Define the array of LEDarray
CRGB LEDarray[NUM_LEDS];

void setup() {
        delay(2000); // 2 second delay for recovery
	Serial1.begin(Serial1Speed);
	FastLED.addLeds<WS2801,DATA_PIN, CLOCK_PIN, RGB>(LEDarray,NUM_LEDS);
//	FastLED.setCorrection(Typical8mmPixel);	// (255, 224, 140)
//	FastLED.setCorrection(TypicalSMD5050);	// (255, 176, 240)
	FastLED.setCorrection( CRGB( 255, 200, 245) );
	FastLED.setMaxPowerInVoltsAndMilliamps(5,1000);
	FastLED.setBrightness(Brightness);
//	PrintInfo();
}

void loop() {
	receiveSerial1Data();
	if (newData == true) {
		strcpy(tempChars, receivedChars); // this is necessary because strtok() in parseData() replaces the commas with \0
		parseData();
		updateSettings();
		newData = false;
	}

	if (Pattern == 1) {
		standard();
	} else if (Pattern == 2) {
		breach();
	} else if (Pattern == 3) {
		rainbow();
	} else if (Pattern == 4) {
		fade();
	} else if (Pattern == 5) {
		slowFade();
	} else {
		standard();
	}
}

void standard() {
	ReactorHue = MainHue;
	chase();
}

void breach() {
	byte breach_diff = 255 - LastHue;
	byte transition_hue = LastHue + (breach_diff/2);
	if (ReactorHue < 255) {
		incrementReactorHue();
	}
	if (ReactorHue > transition_hue && MainHue < 255) {
		incrementMainHue();
	}
	if (ReactorHue >= 255 && MainHue >= 255) {
		MainHue = LastHue;
		ReactorHue = MainHue + 1;
	}
	Rate = (((ReactorHue - MainHue) / (breach_diff / 9) + 1) * RateMultiplier);
	WarpFactor = Rate / RateMultiplier;
	chase();
}

void rainbow() {
	Rainbow = true;
	chase();
	Rainbow = false;
}

void fade() {
	Fade = true;
	chase();
	Fade = false;
}

void slowFade() {
	SlowFade = true;
	chase();
	SlowFade = false;
}

void incrementHue() {
	incrementMainHue();
	incrementReactorHue();
}

void incrementReactorHue() {
	if (MainHue == 255) {
		ReactorHue = 1;
	} else {
		ReactorHue++;
	}
}

void incrementMainHue() {
	if (MainHue == 255) {
		MainHue = 1;
	} else {
		MainHue++;
	}
}

void chase() {
	if (Pulse == PulseLength-1) {
		Pulse = 0;
		if (SlowFade == true) {
			incrementHue();
		}
	} else {
		Pulse++;
	}
	if (Fade == true) {
		incrementHue();
	}
	// Ramp LED brightness
	for(int value = 32; value < 255; value = value + Rate) {
		if (Rainbow == true) {
			incrementHue();
		}
		// Set every Nth LED
		for(int chases = 0; chases < TopChases; chases = chases + PulseLength) {
			byte Top =  Pulse + chases;
			byte Bottom = NUM_LEDS + TopDiff - (Pulse + chases) - 1;
			if (Top < TopLEDtotal) {
				LEDarray[Top] = CHSV(MainHue, Saturation, value);
			}
			if (Bottom >= TopLEDcount&& Bottom < NUM_LEDS) {
				LEDarray[Bottom] = CHSV(MainHue, Saturation, value);
			}
		}
		// Keep reaction chamber at full brightness even though we chase the leds right through it
		for (int reaction = 0; reaction < ReactionLEDcount; reaction++) {
			LEDarray[TopLEDcount + reaction] = CHSV(ReactorHue, Saturation, 255);
		}
		fadeToBlackBy( LEDarray, NUM_LEDS, (Rate*0.5));	// Dim all LEDs by Rate/2
		FastLED.show();					// Show set LEDs
	}
}

void receiveSerial1Data() {
	static bool recvInProgress = false;
	static byte ndx = 0;
	char startMarker = '<';
	char endMarker = '>';
	char helpMarker = '?';
	
	char rc;

	while (Serial1.available() > 0 && newData == false) {
		rc = Serial1.read();

		if (rc == helpMarker) {
			PrintInfo();
		} else if (recvInProgress == true) {
			if (rc != endMarker) {
				receivedChars[ndx] = rc;
				ndx++;
				if (ndx >= numChars) {
					ndx = numChars - 1;
				}
			}
			else if (rc == endMarker) {
				receivedChars[ndx] = '\0';
				recvInProgress = false;
				ndx = 0;
				newData = true;
			}
		} else if (rc == startMarker) {
			recvInProgress = true;
		}
	}
}

void parseData() {
	char * strtokIndx;			// this is used by strtok() as an index
	strtokIndx = strtok(tempChars,",");	// get the first part of the string
	warp_factor = atoi(strtokIndx);		// convert this part to an integer
	strtokIndx = strtok(NULL, ",");		// this continues where the previous call left off
	hue = atoi(strtokIndx);
	strtokIndx = strtok(NULL, ",");
	saturation = atoi(strtokIndx);
	strtokIndx = strtok(NULL, ",");
	brightness = atoi(strtokIndx);
	strtokIndx = strtok(NULL, ",");
	pattern = atoi(strtokIndx);
}

void updateSettings() {
	if (pattern > 0 && pattern < 6 && pattern != Pattern) {
		warp_factor = DefaultWarpFactor;
		Rate = RateMultiplier * WarpFactor;
		hue = DefaultMainHue;
		saturation = DefaultSaturation;
		brightness = DefaultBrightness;
		Pattern = pattern;
		Serial1.print("Color Pattern Set To ");
		Serial1.println(Pattern);
		updateSettings();
	} else {
		if (warp_factor > 0 && warp_factor < 10 && warp_factor != LastWarpFactor) {
			WarpFactor = warp_factor;
			LastWarpFactor = warp_factor;
			Rate = RateMultiplier * WarpFactor;
			Serial1.print(F("Warp Factor Set To "));
			Serial1.println(warp_factor);
		}
		if (hue > 0 && hue < 256 && hue != LastHue) {
			MainHue = hue;
			ReactorHue = hue;
			LastHue = hue;
			Serial1.print(F("Color Hue Set To "));
			Serial1.println(hue);
		}
		if (saturation > 0 && saturation < 256 && saturation != Saturation) {
			Saturation = saturation;
			Serial1.print(F("Color Saturation Set To "));
			Serial1.println(saturation);
		}
		if (brightness > 0 && brightness < 256) {
			FastLED.setBrightness(brightness);
			Brightness = brightness;
			Serial1.print(F("Brightness Set To "));
			Serial1.println(brightness);
		}
	}
	newData = false;
}

void PrintInfo() {
	Serial1.println(F("******** Help ********"));
	Serial1.println(F("Input Format - <2, 160, 220, 255>"));
	Serial1.println(F("Input Fields - <Warp Factor, Hue, Saturation, Brightness, Pattern>"));
	Serial1.println(F("Warp Factor range - 1-9"));
	Serial1.println(F("Hue range - 1-255 1=Red 32=Orange 64=Yellow 96=Green 128=Aqua 160=Blue 192=Purple 224=Pink 255=Red"));
	Serial1.println(F("Saturation range - 1-255"));
	Serial1.println(F("Brightness range - 1-255"));
	Serial1.println(F("Pattern - 1-5 1=Standard 2=Breach 3=Rainbow 4=Fade 5=Slow Fade"));
	Serial1.println(F(""));
	Serial1.println(F("** Current Settings **"));
	Serial1.print(F(" <"));
	Serial1.print(WarpFactor);
	Serial1.print(F(", "));
	Serial1.print(MainHue);
	Serial1.print(F(", "));
	Serial1.print(Saturation);
	Serial1.print(F(", "));
	Serial1.print(Brightness);
	Serial1.print(F(", "));
	Serial1.print(Pattern);
	Serial1.println(F(">"));
	Serial1.println(F("**********************"));
}
