package com.example.door_lock;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener{
	public  static  final String PREFS_NAME="MyPrefs";


	private static final int MAIN2ACTIVITYRESULT=101;

	String IP;//"192.168.219.139";	//211.183.3.103, 61.77.66.144, 175.196.219.174
	final int PORT=4251;

	private EditText in_ip;									//IP 입력 칸
	private Button connectButton;                                 //연결버튼


	//소켓통신용 변수들
	private Socket socket;                                       //WiFi shield와 연결하기 위한 Socket
	private DataOutputStream dataOutputStream;                                 //socket클래스로부터 실제로 통신하기 위한 클래스
	private DataInputStream dataInputStream;

	byte[] buffer;                  //수신용 변수입니다. 여기에 아두이노로부터 받은 데이터가 들어옵니다.
	// 소켓통신용 변수들


	/*
	먼저 스레드(Thread)의 개념을 이해하신다는 전제 하에 설명하겠습니다.
	안드로이드는 설계단계에서부터 UI는 메인스레드에서 동작 나머지 작업들은 별도의 스레드에서 동작하도록 구현되었습니다.
	따라서 메인스레드가 아닌 다른 스레드들에서 텍스트뷰를 동작하게 하거나 이미지를 변환하는 작업을 하려고 하면 안드로이드가 에러를 발생시킵니다.
	이는 UI가 다른 스레드의 블러킹으로 인해서 동작이 멈추는 현상을 미연에 방지하고 시각적으로나 성능적으로나 향상된 품질을 제공하기 위해서입니다.

	아두이노 와이파이실드와 통신하기 위해서는 소켓통신을 해야합니다. 하지만 소켓통신에서 사용하는 함수들은 기본적으로 블러킹(흐름을 멈추고 대기함)함수들이 많이 존재합니다.
	메인스레드에서 소켓함수들을 호출해서 블러킹이 생기면 UI자체가 멈춰버리는 현상이 생기기 때문에 별도의 스레드를 생성해서 소켓통신을 하도록 구글에서 권장하고 있습니다.
	따라서 별도의 스레드를 생성하고 그 안에서 소켓통신의 모든것이 동작하도록 구현해야 합니다.
	* */

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);



		SharedPreferences settings= getSharedPreferences(PREFS_NAME,0);

		in_ip = (EditText)findViewById(R.id.ipedittext);                                 //아이피 입력용 editText
		connectButton = (Button)findViewById(R.id.connect);                               //연결 버튼 사실상 다음 엑티비티로 넘어가는 기능뿐입니다.
		connectButton.setOnClickListener(this);                                           //버튼 리스너 연결


	}



/*연결 버튼을 누르면 다음 액티비티가 초기화되면서 입력한 아이피로 socket통신을 시도 합니다
만약에 다음 액티비티에서 초기화도중에 소켓연결을 실패하거나 종료 버튼을 누르면 아래의 onActivityResult로 돌아와서 앱을 종료시킵니다.
*/

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if(requestCode==MAIN2ACTIVITYRESULT)
		{
			if(resultCode==RESULT_OK)
			{
				finish();
			}
		}
	}


//다음 액티비티로 넘어갑니다.
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub

		switch(v.getId())
		{
			case R.id.connect:
				Intent intent =new Intent(MainActivity.this,Main2Activity.class);
				intent.putExtra("IPINFO",in_ip.getText().toString());                   //다음 액티비티에 아이피 문자열을 넘겨줍니다.
				startActivityForResult(intent,MAIN2ACTIVITYRESULT);                  //다음 액티비티로 넘어가는 동작을 하는 함수
				break;
		}
	}


}
