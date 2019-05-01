package com.liujh168.jqmq;
import static android.content.ContentValues.TAG;
import java.util.HashMap;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

public class JqmqActivity extends Activity {
    Button btn;
    TextView txt;
    ImageView imgBoard;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.mainlayout);
        btn =  (Button) findViewById(R.id.btnstart);
        txt =  (TextView) findViewById(R.id.txtInfo);
        imgBoard = (ImageView) findViewById(R.id.imgBoard);

        Log.d("JqmqActivity", "onCreate: from JqmqActivity");
        btn.setOnClickListener(new View.OnClickListener() {
                                   @Override
                                   public void onClick(View view) {
                                       txt.setText("Btn Click Message!");
                                   }
                               }

        );
        imgBoard.setOnClickListener(new View.OnClickListener() {
                                   @Override
                                   public void onClick(View view) {
                                       txt.setText(R.string.app_name);
                                   }
                               }

        );
    }

    public void onbtnclick(View v){
        Toast.makeText(this, R.string.clickmessage,Toast.LENGTH_LONG).show();
        txt.setText("btn Click Message!");
    }
}

