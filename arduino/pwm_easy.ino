void setup() {
	pinMode(13, OUTPUT);
}

/* Period: 1000 microseconds */
void loop() {
	digitalWrite(13, HIGH);
	delayMicroseconds(100); //Duty Cycle: 10% @1kHz
	digitalWrite(13, LOW);
	delayMicroseconds(1000 - 100);
}
