package bluetooth.arduinopwm;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

/*
 * Microcontroladores e Microprocessadores
 * Android e Bluetooth
 * Authors: Nov 2013
 * 11080000401 - Cássio Trindade Batista
 * 11080004701 - Thiago Barros Coelho
 * 11080001401 - Gabriel Peixoto de Carvalho
 *
 */

public class AndroidBluetooth extends Activity {
	private static final String TAG = "BT";

	/* Widgets defined in xml layout file */
	SeekBar seek; //widget: defines the number
	TextView text; //widget: show the number
	CheckBox reverse;//widget: wants to reverse motor spin?

	private BluetoothAdapter adapter = null; //pointer to my android BT dev
	private BluetoothSocket skt = null; //socket to manage connection

	public Arduino arduino; //thread to communicate android and arduino
	public OutputStream out = null; //stream to store and send string 

	/* SPP UUID service (Not for peer-peer) */
	public static final UUID uu_id = UUID.
			fromString("00001101-0000-1000-8000-00805F9B34FB");

	/* MAC-address of Bluetooth module BUSSANGRA */
	public static final String address = "20:13:06:03:02:00";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bluetooth);

		text = (TextView) findViewById(R.id.textView1); //show value to be sent
		reverse = (CheckBox) findViewById(R.id.reverse); //reverse spin?
		seek = (SeekBar) findViewById(R.id.seekBar1); //progress bar
		seek.setMax(255); //8bits: 0-255

		adapter = BluetoothAdapter.getDefaultAdapter(); //get BT adapter from SO

		/* Listen when I'm doing some event with SeekBar */
		seek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				arduino.write(String.valueOf(text.getText())); //send via skt in thread
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				//Log.i("CAS", "comecei");
			}

			@Override
			public void onProgressChanged(SeekBar sb, int p, boolean fromUser) {
				if(reverse.isChecked()) //invert value to reverse motor spin
					p *= -1;
				text.setText(String.valueOf(p));
			}
		});
	}//onCreate

	/* Create Insecure RFCOMM channel based on UUID and a pointer to Mac-Addr BT */
	private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
		if(Build.VERSION.SDK_INT >= 10) {
			try {
				final Method m = device.getClass().getMethod(
						"createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class });
				return (BluetoothSocket) m.invoke(device, uu_id); //return here
			} catch (Exception e) {
				Log.e(TAG, "Can't create Insecure RFCOMM Connection" + e.getMessage());
			}
		}
		return device.createRfcommSocketToServiceRecord(uu_id);
	}

	@Override
	public void onResume() {
		super.onResume();
		
		/* Check if Bluetooth is on ... */
		if(!adapter.isEnabled())
			startActivityForResult(new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE), 1);

		/* ... and wait until bluetooth is enabled (Gambiarra) */
		while(!adapter.isEnabled()) { }
		
		/* Set up a pointer to the android using its BT module address */
		BluetoothDevice device = adapter.getRemoteDevice(address);
		try {
			skt = createBluetoothSocket(device); //open RFCOMM channel based on UUID
		} catch (IOException e) {
			errorExit("onResume()", "socket create failed: " + e.getMessage());
		}

		adapter.cancelDiscovery(); //saves battery and prevents another connection

		/* Establish the connection. This will block until it connects */
		try {
			skt.connect();
		} catch (IOException e) {
			try {
				skt.close();
			} catch (IOException e2) {
				errorExit("onResume()", "at closing skt in conn failure" + e2.getMessage());
			}
		}
		arduino = new Arduino(skt); //pass socket to thread
		arduino.start(); //start thread
	}//close onResume

	@Override
	public void onPause() {//to back button
		super.onPause();
		try {
			skt.close();
		} catch (IOException e) {
			errorExit("onPause()", "failed to close socket." + e.getMessage());
		}
	}//close onPause()

	private void errorExit(String title, String msg) {
		Toast.makeText(getBaseContext(), title + ":" + msg, Toast.LENGTH_LONG).show();
		finish();
	}

	/** Thread which communicates devices */
	private class Arduino extends Thread {
		/* Constructor: Receive BluetoothSocket object and set the output stream */
		public Arduino(BluetoothSocket socket) {
			try {
				out = socket.getOutputStream();
			} catch (IOException e) {
				Log.e(TAG, e.getMessage());
			}
		}//close constructor

		/* Call this from the main activity to send data to arduino */
		public void write(String message) {
			byte[] data = message.getBytes();
			try {
				out.write(data);
			} catch (IOException e) {
				Log.e(TAG, "Error data send: " + e.getMessage());
			}
		}//write
	}//close class ConnectThread

}//close Activity Class

/* * * * * * * E O F * * * * * * * * * * 
	Server is the one who listens
	Client is the one who connects in
	Whoever initiate the connection is the server
	Master controls
	Slave is controlled
* * * * * * * * * * * * * * * * * * * * */
