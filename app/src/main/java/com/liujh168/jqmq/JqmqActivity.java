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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
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
    float screenHeight;
    float screenWidth;
    float ration;
    Button btn;
    EditText fen;
    Bitmap imgWelcome;  //当前logo图片引用
    SoundPool soundPool;//声音池
    HashMap<Integer, Integer> soundPoolMap; //声音池中声音ID与自定义声音ID的Map    ImageView imgBoard;

    Handler hd = new Handler(){
        @Override
        public void handleMessage(Message msg)
        {
            switch(msg.what)
            {
                case 0:
                    gotoGameview();
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

        //获取屏幕分辨率
        DisplayMetrics dm=new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        screenHeight=dm.heightPixels;
        screenWidth=dm.widthPixels;

        imgWelcome = BitmapFactory.decodeResource(this.getResources(), R.drawable.board);
        imgWelcome = scaleToFit(imgWelcome,screenHeight/imgWelcome.getHeight());
        initScreenSize(screenWidth,screenHeight);
        initSound();

        setContentView(new WelcomeView(JqmqActivity.this));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menufileopen:
                Toast.makeText(this, R.string.menu_file_open, Toast.LENGTH_LONG).show();
                break;
            case R.id.menufilesave:
                Toast.makeText(this,  R.string.menu_file_save, Toast.LENGTH_LONG).show();
                break;
            case R.id.menu_sys_set:
                Toast.makeText(this,  R.string.btn_txt_settings, Toast.LENGTH_LONG).show();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    public static Bitmap scaleToFit(Bitmap bm,float fblRatio)//缩放图片的方法
    {
        if(bm!=null){
            int width = bm.getWidth(); //图片宽度
            int height = bm.getHeight();//图片高度
            Matrix matrix = new Matrix();
            matrix.postScale((float)fblRatio, (float)fblRatio);//图片等比例缩小为原来的fblRatio倍
            return Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);//声明位图
        }
        return null;
    }

    private  class WelcomeView extends SurfaceView implements SurfaceHolder.Callback
    {
        SurfaceHolder myholder;
        Paint mPaint;
        int currentAlpha;  			//当前的不透明值
        JqmqActivity activity;

        public WelcomeView(JqmqActivity acti) {
            super(acti);
            this.activity=acti;
            currentAlpha=255;
            mPaint = new Paint();
            myholder=this.getHolder();
            myholder.addCallback(this);
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
                            mPaint.setColor(Color.BLACK);//设置画笔颜色
                            mPaint.setAlpha(255);       //设置不透明度为255
                            canvas.drawRect(0, 0, screenWidth, screenHeight, mPaint);////绘制黑填充矩形清背景
                            mPaint.setAlpha(currentAlpha);
                            //动态更改图片的透明度值并不断重绘
                            if(imgWelcome!=null) {
                                int curX=(int) (screenWidth/2-imgWelcome.getWidth()/2);//图片位置
                                int curY=(int) (screenHeight/2-imgWelcome.getHeight()/2);
                                canvas.drawBitmap(imgWelcome, curX, curY, mPaint);
                            }
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

    public void initScreenSize(float w , float h)  //更新自定义view中屏幕尺寸
    {
        //前面代码放入oncreate,后面的放入GameView
        int tempWidth=(int) (GameView.screen_width=w);
        int tempHeight=(int) (GameView.screen_height=h);
        if(tempHeight>tempWidth)
        {
            GameView.screen_height=tempHeight;
            GameView.screen_width=tempWidth;
        }
        else
        {
            GameView.screen_height=tempWidth;
            GameView.screen_width=tempHeight;
        }
    }

    public void initSound()
    {
        //声音池
        soundPool = new SoundPool(4, AudioManager.STREAM_MUSIC, 100);
        soundPoolMap = new HashMap<Integer, Integer>();

        soundPoolMap.put(1, soundPool.load(this, R.raw.capture, 1));//背景音乐
        soundPoolMap.put(2, soundPool.load(this, R.raw.move, 1)); //玩家走棋
        soundPoolMap.put(4, soundPool.load(this, R.raw.win, 1)); //赢了
        soundPoolMap.put(5, soundPool.load(this, R.raw.loss, 1)); //输了
    }
    //播放声音
    public void playSound(int sound, int loop)
    {
        if(!GameView.isnoPlaySound)
        {
            return;
        }
        AudioManager mgr = (AudioManager)this.getSystemService(Context.AUDIO_SERVICE);
        float streamVolumeCurrent = mgr.getStreamVolume(AudioManager.STREAM_MUSIC);
        float streamVolumeMax = mgr.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        float volume = streamVolumeCurrent / streamVolumeMax;
        soundPool.play(soundPoolMap.get(sound), volume, volume, 1, loop, 1f);
    }

    public void gotoGameview()
    {
        setContentView(R.layout.mainlayout);
        btn =  (Button) findViewById(R.id.btnstart);
        fen =  (EditText) findViewById(R.id.edtInfofen);
        //imgBoard=(ImageView) findViewById(R.id.imgBoard);
        btn.setOnClickListener(new View.OnClickListener() {
                                   @Override
                                   public void onClick(View view) {
                                       fen.setText(R.string.wokao);
                                       //setContentView(new WelcomeView(JqmqActivity.this));
                                   }
                               }

        );
    }
}
