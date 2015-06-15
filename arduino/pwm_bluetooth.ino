/*
 * Microcontroladores e Microprocessadores 
 * Bluetooth e PWM
 * Authors: Nov 2013
 * 11080001401 - Gabriel Peixoto de Carvalho 
 * 11080004701 - Thiago Barros Coelho
 * 11080000401 - Cássio Trindade Batista 
 *
 */

#include <SoftwareSerial.h>
#include <LiquidCrystal.h>

/* Bluetooth pins */
#define rxPin 2 //digital pin to receiver
#define txPin 3 //digital pin to transceiver

/* MOTOR: "Ponte H" pins */
#define spin_right 9 //rotate motor to right
#define spin_left 10 //rotate motor to left

SoftwareSerial androide(rxPin, txPin); //BLUETOOTH: define pins to Rx and Tx
LiquidCrystal lcd(11, 12, 5, 6, 7, 8); //LCD: define LCD pins

void setup() {
	/* Define Rx as input and Tx as output */
	pinMode(rxPin, INPUT);
	pinMode(txPin, OUTPUT);

	/* When right is HIGH, left is LOW. Both are outputs */
	pinMode(spin_right, OUTPUT); 
	pinMode(spin_left, OUTPUT); 

	androide.begin(9600); //bauds to bluetooth
	lcd.begin(16, 2); //size of LCD matrix
	lcd.print("Arduino Fast PWM"); //initial text

	/* Initialize time registers TCCRn according to datasheet table */
	TCCR1A = 0b10100001;
	TCCR1B = 0b00001011;

	/* Flush to initialize registers OCRn */
	OCR1A = 0;
	OCR1B = 0;
}  

void loop() {
	int i = 0; //index to str
	char str_int9[5] = {0}; 
	int duty_cycle = 0;

	/* if "snnn" number (as string) arrives on SoftSerial port */
	if(androide.available()) {
		do {
			str_int9[i++] = androide.read(); //get each char and fill string
			delay(3); 
		} while (androide.available() > 0); //while has char in SoftSerial

		/* duty_cycle = str2int16, (as sint9 from -255 to +255) "snnn" */
		duty_cycle = atoi(str_int9);

		/* Prints Duty Cycle on LCD */
		lcd.setCursor(0, 0);
		lcd.clear();
		lcd.print("Duty cycle:");
		lcd.setCursor(0, 1);
		lcd.print(duty_cycle);

		if(duty_cycle < 0) { //reversal spin (left)
			OCR1A = 0; //flush to stop to rotate to right
			OCR1B = abs(duty_cycle); //sets B to rotate to left
		} else { //normal spin (right) 
			OCR1B = 0; //flush to stop rotating to left
			OCR1A = abs(duty_cycle); //sets A to rotate to right
		}
	}
}
