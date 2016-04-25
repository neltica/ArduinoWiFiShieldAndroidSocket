package com.example.door_lock;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;

public class Main2Activity extends Activity implements Runnable ,View.OnClickListener{

    private Socket socket;                                //소켓
    private DataInputStream dataInputStream;            //수신용 스트림
    private DataOutputStream dataOutputStream;           //전송용 스트림
    private byte[] buffer;                        //아두이노로부터 1바이트씩 받을 수 있는데. 이때 받은 1바이트가 저장되는 변수
    private String temphumiString;                //센서값이 저장됨
    private Thread thread;                        //소켓통신은 스레드에서 돌아야 하므로 스레드 변수
    private String ip;                            //첫 액티비티에서 넘겨받은 아이피가 저장됨

    private Button transferButton;                 //전송버튼
    private Button sensorButton;                    //센서값읽어오는 버튼
    private Button appDestroyButton;                //앱종료 버튼

    private final static int TRANSFER=1;               //전송버튼을 누를경우 state
    private final static int SENSOR=2;                //센서버튼을 누를경우  state
    private final static int NOMOVEMENT=0;           //아무것도 안누를 상태  (최초의 상태)
    private int rwFlag;                              //상태를 저장하는 변수 여기에 TRANSFER, SENSOR,NOMOVEMENT등이 들어가서 상태를 저장하고 확인한다.
    private boolean threadStopFlag;                   //스레드를 멈출지 말지 정하는 변수

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);


        ip= (String) getIntent().getExtras().get("IPINFO");           //이전 액티비티에서 넘겨받은 ip값을 읽어옴
        buffer=new byte[1];                                            //버퍼초기화
        rwFlag=NOMOVEMENT;                                            //최초상태는 NOMOVEMENT
        threadStopFlag=false;                                         //처음부터 true면 스레드가 안돌아간다.
        temphumiString=new String();                                     //온습도 변수 초기화

        transferButton=(Button)findViewById(R.id.transfer);                  //버튼 연결
        sensorButton=(Button)findViewById(R.id.sensorrecv);                   //버튼연결
        appDestroyButton=(Button)findViewById(R.id.appdestroy);                //버튼연결

        transferButton.setOnClickListener(this);                            //리스너 연결
        sensorButton.setOnClickListener(this);                             //리스너 연결
        appDestroyButton.setOnClickListener(this);                         //리스너 연결

        thread=new Thread(this);                                        //스레드를 만들고
        thread.start();                                                //동작시킨다.
    }


//실제 스레드가 돌아가는 곳
    @Override
    public void run() {
        try {
            Log.i("socket", "attemping connect");
            socket=new Socket(ip,4251);                            //입력한 아이피, 4251포트로 접속을 시도한다.
            Log.i("socket","connect success");                     //성공했으면 connect success를 로그로 출력한다.
        } catch (IOException e) {
            e.printStackTrace();
            Log.i("socket", "connect error");
            finish();
        }

        try {
            dataInputStream=new DataInputStream(socket.getInputStream());                    //수신용 스트림을 생성한다.
        } catch (IOException e) {
            e.printStackTrace();
            Log.i("datainputstream", "datainputstream error");
            finish();
        }
        try {
            dataOutputStream=new DataOutputStream(socket.getOutputStream());               //송신용 스트림을 생성한다.
        } catch (IOException e) {
            e.printStackTrace();
            Log.i("dataoutputstream", "dataoutputstream error");
            finish();
        }


        try {
            dataOutputStream.write(1);     //아두이노에게 접속했다고 알리기 위해서 1바이트를 한번 보낸다.
        } catch (IOException e) {
            e.printStackTrace();
        }



        while(true)
        {
            if(threadStopFlag)           //만약 threadStopFlag가 true가 되서 스레드를 멈추라는 명령이 들어오면
            {
                try {
                    dataOutputStream.close();                                   //출력스트림을 종료한다.
                    Log.i("dataOutputStrema","dataoutputstream close");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    dataInputStream.close();                                    //입력스트림도 종료한다.
                    Log.i("datainputStrema", "datainputstream close");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    socket.close();                                             //소켓도 종료한다.(연결을 끊는다. 아두이노측에서는 연결이 끊겼는지 모른다. 해결방법 없음.)
                    Log.i("socket", "socket close");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
            Log.i("thread", String.valueOf(rwFlag));                            //지금 상태가 어떤지(SENSOR,transfer,NOMOVEMENT) 를 로그켓에 출력해본다.
            switch(rwFlag)
            {
                case SENSOR:                                                     //만약 센서버튼을 눌렀다면
                    buffer[0]='s';                                               //버퍼에 s를 집어넣고
                    try {
                        dataOutputStream.write(buffer);                          //버퍼를 아두이노에게 보낸다.
                        Log.i("write", "s write");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    while(true) {
                        try {
                            dataInputStream.read(buffer);                       //s를 받은 아두이노는 센서값을 읽어와서 안드로이드로 보내기 시작한다. 안드로이드는 1바이트씩 아두이노로 부터 받아서 temphumiString에 1바이트씩 더한다.
                            Log.i("dataInputStream", "data:" + (char) buffer[0]);

                            if (buffer[0] == '\n') {                             //1바이트 받은 값이 \n이면
                                Message message=new Message();
                                message.obj=temphumiString;
                                handler.sendMessage(message);                      //현재는 메인스레드가 아닌 다른 스레드이므로 UI를 만질 수 없다 따라서 handler를 이용해서 UI를 반 강제로 움직여야하는데. 이 줄에 있는 함수가 그것을 가능하게 해준다.  핸들러쪽으로 temphumiString을 보낸다.
                                //openPopup(temphumiString);
                                temphumiString = "";                              //temphumiString을 다시 공백으로 초기화시킨다.
                                break;
                            } else {
                                temphumiString += (char) buffer[0];             //만약 아두이노에서 받은 값이 \n이 아니면 temphumiString에 더한다.
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case TRANSFER:                                                  //전송버튼을 누르면
                    buffer[0]='t';                                               //버퍼에 t를 집어넣고.
                    try {
                        dataOutputStream.write(buffer);                          //아두이노 쪽으로 쏜다.
                        Log.i("write", "t");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
            }
            Log.i("thread","setWaitting start");
            setWait();                                                           //스레드가 계속 돌고 있거나 스레드를 죽였다가 다시 시작하게 하면 비효율적이므로 별도의 명령이 있을때 까지 멈춰있게 한다.
            Log.i("thread", "setWaitting end");
        }
    }

    public synchronized void setNotify() {                                       //스레드를 다시 동작하게 하는 함수
        notify();
    }

    public synchronized void setWait()                                            //스레드를 멈추는 함수
    {
        try {
            wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId())
        {
            case R.id.transfer:                                                  //전송버튼을 누르면
                rwFlag=TRANSFER;                                                //rwFlag의 상태를 TRANSFER로 바꾸고
                setNotify();                                                     //스레드를 다시 동작하게 해서 t를 전송한다.
                break;
            case R.id.sensorrecv:                                                //센서버튼을 누르면
                rwFlag=SENSOR;                                                  //rwFlag를 SENSOR로 바꾸고
                setNotify();                                                    //스레드를 다시 동작하게 하여 s를 보내고 센서값을 받아온다.
                break;
            case R.id.appdestroy:                                               //앱종료 버튼을 누르면
                threadStopFlag=true;                                             //스레드멈춤을 선언하고
                setNotify();                                                     //스레드를 다시 동작하게 한다.
                try {
                    thread.join();                                              //스레드가 멈출때 까지 대기한다.
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Log.i("thread","join thread");                                   //스레드가 정상적으로 종료됐음을 알린다.
                setResult(RESULT_OK);                                             //이전 액티비티에게 정상적으로 종료됐다고 알릴 준비를 한다.
                finish();                                                         //현재 액티비티를 종료시킨다.
                break;
        }
    }

    public void openPopup(String sensorValue)                                    //센서값을 팝업형태로 출력하기 위한 함수이다.
    {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);      //팝업을 만든다.

        TextView sensorValueTextview=new TextView(this);                         //팝업에 센서값을 출력하려면 텍스트뷰가 있어야하기때문에 텍스뷰를 만든다/
        sensorValueTextview.setText(sensorValue);                               //텍스트뷰에 센서값을 설정한다.

        builder.setTitle("sensor value");                                       //팝업의 타이틀을 설정한다.
        builder.setView(sensorValueTextview);                                   //텍스트뷰를 팝업에 설치한다.
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {          //팝업에 확인 버튼을 만든다. 아래의 onClick함수에 별도의 코드를 작성하지 않으면 그냥 팝업종료 버튼으로 동작한다.
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        builder.show();                                                         //이 함수를 호출하면 팝업이 화면에 실제로 나타난다. 만약 호출하지 않으면 팝업이 화면에 나타나지 않는다.

    }

    Handler handler= new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);                                           //메인스레드가 아닌 스레드에서 UI를 움직이기 위해서 필요한 핸들러이다.
            openPopup((String)msg.obj);                                         //핸들러에서 openPopup을 호출하여 팝업을 띄운다.
        }
    };
}
