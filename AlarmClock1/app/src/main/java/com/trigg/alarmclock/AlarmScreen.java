package com.trigg.alarmclock;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
@SuppressWarnings("deprecation")



public class AlarmScreen extends Activity implements SensorEventListener{
	
	public final String TAG = this.getClass().getSimpleName();

	private WakeLock mWakeLock;
	private MediaPlayer mPlayer;
	private static final int WAKELOCK_TIMEOUT = 60 * 1000;

	/*====================================================*/
	private SensorManager mySensorManager;
	private int start_degree = -1;
    private int degree;
    private int gravity;
    private int[] degrees = new int[12];
    private int margin = 5;
    private boolean pressing = false;
	/*====================================================*/

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//Setup layout
		this.setContentView(R.layout.activity_alarm_screen);

		String name = getIntent().getStringExtra(AlarmManagerHelper.NAME);
		int timeHour = getIntent().getIntExtra(AlarmManagerHelper.TIME_HOUR, 0);
		int timeMinute = getIntent().getIntExtra(AlarmManagerHelper.TIME_MINUTE, 0);
		String tone = getIntent().getStringExtra(AlarmManagerHelper.TONE);
		
		TextView tvName = (TextView) findViewById(R.id.alarm_screen_name);
		tvName.setText(name);
		
		TextView tvTime = (TextView) findViewById(R.id.alarm_screen_time);
		tvTime.setText(String.format("%02d : %02d", timeHour, timeMinute));
		
		Button dismissButton = (Button) findViewById(R.id.alarm_screen_button);
		dismissButton.setOnTouchListener(myTouchListener);

		/*======================================================================================================*/

		//INIT SENSORS
		mySensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mySensorManager.registerListener(this, mySensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
		mySensorManager.registerListener(this, mySensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_FASTEST);
		
		/*======================================================================================================*/

		//Play alarm tone
		mPlayer = new MediaPlayer();
		try {
			if (tone != null && !tone.equals("")) {
				Uri toneUri = Uri.parse(tone);
				if (toneUri != null) {
					mPlayer.setDataSource(this, toneUri);
					mPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
					mPlayer.setLooping(true);
					mPlayer.prepare();
					mPlayer.start();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//Ensure wakelock release
		Runnable releaseWakelock = new Runnable() {

			@Override
			public void run() {
				getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
				getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
				getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
				getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

				if (mWakeLock != null && mWakeLock.isHeld()) {
					mWakeLock.release();
				}
			}
		};

		new Handler().postDelayed(releaseWakelock, WAKELOCK_TIMEOUT);
	}


	/*======================================================================================================*/

    //BUTTON SET ONTOUCHLISTENER
	View.OnTouchListener myTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            Button dismissButton;
            switch (motionEvent.getActionMasked()) {
                case MotionEvent.ACTION_POINTER_DOWN:
                    dismissButton = (Button) findViewById(R.id.alarm_screen_button);
                    dismissButton.setTextColor(Color.GREEN);
                    pressing = true;
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    dismissButton = (Button) findViewById(R.id.alarm_screen_button);
                    dismissButton.setTextColor(Color.BLACK);
                    pressing = false;
                    start_degree = -1;
                    resetDegrees();
                    return true;

            }
            return true;

        }
    };


	//SENSORS ONSACCURACYCHANGED
	//SENSORS ONSENSORCHANGED
	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
	}


	@Override
	public void onSensorChanged(SensorEvent event) {

		Sensor sensor = event.sensor;

        //SO DETETAR SE TIVER A USAR OS DOIS DEDOS
        if(!pressing)
            return;

        //OBTER VALOR DA ACELERACAO NO EIXO Z
        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER)
		{
			gravity = Math.round(event.values[2]);
		}

		if (sensor.getType() == Sensor.TYPE_ORIENTATION)
		{
            //OBTER VALOR DO GRAU DE ORIENTACAO (0..360)
            degree = Math.round(event.values[0]);

            //DEFINIR ANGLULO INICIAL
            if(start_degree == -1) {
                start_degree = Math.round(event.values[0]);
                System.out.println("start_degree:" +start_degree);
            }

            //SE DEVICE ESTIVER TORTO
            if(gravity < 9) {
                start_degree = Math.round(event.values[0]);
                resetDegrees();
                System.out.println("telemovel torto");
                System.out.println("start_degree:" +start_degree);
            }

		}

        //POR FIM VERIFICAR SE JA PASSOU POR TODOS OS DEGREES (DE 30 EM 30 POR EXEMPLO)
        if( Math.abs((degree % 30) - (start_degree%30)) < margin ){
            //System.out.println("30!!!");
            if(!addDegree(degree)){
                Button dismissButton = (Button) findViewById(R.id.alarm_screen_button);
                dismissButton.setText("BOM DIA");
                mPlayer.stop();
            }

        }

	}


	//SET DEGREES VISITED
	private boolean addDegree(int degree){

        int i=0;
        while(i < 12){
            if(degrees[i] == 0) {
                degrees[i] = degree;
                return true;
            }
            else if(Math.abs(degrees[i] - degree) < margin){
                return true;
            }

            i++;
        }

        return false;
    }


    //RESET DEGREES VISITED 
    private void resetDegrees(){
        for (int i = 0; i < 12; i++) {degrees[i]=0;}
    }

	/*======================================================================================================*/

	
	@Override
	protected void onResume() {
		super.onResume();


		// Set the window to keep screen on
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

		// Acquire wakelock
		PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
		if (mWakeLock == null) {
			mWakeLock = pm.newWakeLock((PowerManager.FULL_WAKE_LOCK | PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), TAG);
		}

		if (!mWakeLock.isHeld()) {
			mWakeLock.acquire();
			Log.i(TAG, "Wakelock aquired!!");
		}

	}

	@Override
	protected void onPause() {
		super.onPause();


		if (mWakeLock != null && mWakeLock.isHeld()) {
			mWakeLock.release();
		}
	}
}
