package com.liujh168.jqmq;
import static android.content.ContentValues.TAG;
import java.util.HashMap;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Matrix;
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
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class JqmqActivity extends Activity {
    Button btn;
    TextView txt;
    Bitmap currentLogo;  //当前logo图片引用
    ImageView imgBoard;
    Handler hd = new Handler(){
        @Override
        public void handleMessage(Message msg)
        {
            switch(msg.what)
            {
                case 0:
                    setContentView(R.layout.mainlayout);
                    btn =  (Button) findViewById(R.id.btnstart);
                    txt =  (TextView) findViewById(R.id.txtInfo);
                    //imgBoard=(ImageView) findViewById(R.id.imgBoard);
                    btn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                txt.setText(R.string.btn_txt_auth_agreement_ok);
                                txt.refreshDrawableState();
                                try {
                                    Thread.sleep(50);
                                } catch (Exception e)//抛出异常
                                {
                                    e.printStackTrace();
                                }
                                setContentView(new MyView(JqmqActivity.this));  //再次回来时
                            }
                        }
                    );
                    break;
                case 1:
                    break;
                default:
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.mainlayout);
        btn =  (Button) findViewById(R.id.btnstart);
        txt =  (TextView) findViewById(R.id.txtInfo);
        //imgBoard = (ImageView) findViewById(R.id.imgBoard);

        currentLogo = BitmapFactory.decodeResource(this.getResources(), R.drawable.brothers);
        currentLogo = scaleToFit(currentLogo,1.6f);

        Log.d("JqmqActivity", "onCreate: from JqmqActivity");
        btn.setOnClickListener(new View.OnClickListener() {
                                   @Override
                                   public void onClick(View view) {
                                       txt.setText(R.string.wokao);
                                       setContentView(new MyView(JqmqActivity.this));
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

    public static Bitmap scaleToFit(Bitmap bm,float fblRatio)//缩放图片的方法
    {
        int width = bm.getWidth(); //图片宽度
        int height = bm.getHeight();//图片高度
        Matrix matrix = new Matrix();
        matrix.postScale((float)fblRatio, (float)fblRatio);//图片等比例缩小为原来的fblRatio倍
        return Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);//声明位图
    }

    private  class MyView extends SurfaceView implements SurfaceHolder.Callback
    {
        SurfaceHolder myholder;
        Paint mPaint;
        int currentAlpha;  			//当前的不透明值
        JqmqActivity activity;

        public MyView(JqmqActivity acti) {
            super(acti);
            this.activity=acti;
            currentAlpha=255;
            mPaint = new Paint();
            myholder=this.getHolder();
            myholder.addCallback(this);
        }

        public void brawboard(Canvas canvas)
        {
            //绘制黑填充矩形清背景
            mPaint.setColor(Color.BLACK);//设置画笔颜色
            mPaint.setAlpha(255);       //设置不透明度为255
            canvas.drawRect(0, 0, 1200, 1300, mPaint);//画个矩形
            mPaint.setAlpha(currentAlpha);
            if(currentLogo!=null) {
                canvas.drawBitmap(currentLogo, 0, 0, mPaint);
            }
        }

        //以下实现surface生命周期3个回调函数
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            new Thread(new MyThread()).start();
        }
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        }
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
        }

        class MyThread implements Runnable {
            @Override
            public void run() {
                for (int i = 255; i > 0; i = i - 10) {
                    currentAlpha=i;
                    Canvas canvas = myholder.lockCanvas(null);//获取画布
                    try {
                        synchronized (myholder)//同步
                        {
                            //动态更改图片的透明度值并不断重绘
                            brawboard(canvas);//进行绘制绘制
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (canvas != null)//如果当前画布不为空
                        {
                            myholder.unlockCanvasAndPost(canvas);//解锁画布
                        }
                    }
                    try {
                        if (i == 255)//若是新图片，多等待一会
                        {
                            Thread.sleep(1000);
                        }
                        Thread.sleep(50);
                    } catch (Exception e)//抛出异常
                    {
                        e.printStackTrace();
                    }
                }
                activity.hd.sendEmptyMessage(0);//发送消息，开始加载棋子模型
            }
        }
    }
}
