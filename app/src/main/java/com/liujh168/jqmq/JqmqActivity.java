package com.liujh168.jqmq;

import static android.content.ContentValues.TAG;
import static android.content.Intent.ACTION_VIEW;
import static java.lang.Thread.sleep;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewDebug;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
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

import com.liujh168.jqmq.GameView.*;

public class JqmqActivity extends Activity {

    GameView gameView;

    EditText edtFen;
    Button btnShowPiece;

    float screenHeight;
    float screenWidth;
    float ration;
    Bitmap imgWelcome;  //当前logo图片引用
    SoundPool soundPool;//声音池
    HashMap<Integer, Integer> soundPoolMap; //声音池中声音ID与自定义声音ID的Map    ImageView gameView;
    private Lock lock = new ReentrantLock();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //获取屏幕分辨率
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        screenHeight = dm.heightPixels;
        screenWidth = dm.widthPixels;

        imgWelcome = BitmapFactory.decodeResource(this.getResources(), R.drawable.welcome);
        imgWelcome = scaleToFit(imgWelcome, screenWidth / imgWelcome.getWidth());

        initScreenSize(screenWidth, screenHeight);
        initSound();

        setContentView(new WelcomeView(JqmqActivity.this));
    }

    Handler hd = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    GotoContentView();
                    break;
                case 1:
                    break;
                default:
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    //主菜单动作
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        playSound(GameView.RESP_CLICK, 0);
        switch (item.getItemId()) {
            case R.id.menuhelpnewer:
            case R.id.menuhelptopic:
            case R.id.menuhelpabout:
                Intent intent =new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("http://www.liujh168.com/index.php/2019/05/29/xmq_manual/"));
                startActivity(intent);
                break;
            case R.id.menuboard_3d:
            case R.id.menuboard_perfect:
            case R.id.menuboard_wood:
            case R.id.menupiece_3d:
            case R.id.menupiece_perfect:
            case R.id.menupiece_wood:
                Toast.makeText(JqmqActivity.this, R.string.txt_info_working, Toast.LENGTH_SHORT).show();
                break;
            case R.id.menufilenew:
            case R.id.menufileopen:
            case R.id.menufilesave:
            case R.id.menufilesaveas:
                Toast.makeText(JqmqActivity.this, R.string.txt_info_working, Toast.LENGTH_SHORT).show();
                break;
            case R.id.menuposfencopy:
                edtFen.setText(GameView.pos.toFen());
                break;
            case R.id.menuposfenpaste:
                GameView.pos.fromFen(edtFen.getText().toString());
                GameView.isvisible = 2;
                btnShowPiece.setText(R.string.btn_txt_hideboard);
                gameView.invalidate();
                break;
            case R.id.menuposupdown:
            case R.id.menuposleftright:
            case R.id.menuposrotate:
                GameView.pos.ChangeSide();  //这样换边电脑下法不对了。约定红下黑上的呢
                gameView.invalidate();
                break;
            case R.id.menuposredfirst:
                GameView.pos.sdPlayer = 0;
                break;
            case R.id.menuposblackfirst:
                GameView.pos.sdPlayer = 1;
                break;
            case R.id.menuposchageside:
                GameView.pos.sdPlayer = 1 - GameView.pos.sdPlayer;
                break;
            case R.id.menuposstart:
                GameView.pos.fromFen(getString(R.string.txt_start_fen));
                gameView.invalidate();
                break;
            case R.id.menuposclear:
                GameView.pos.fromFen(getString(R.string.txt_clear_fen));
                gameView.invalidate();
                break;
            case R.id.menu_music:
                break;
            case R.id.menu_music_stop:
                soundPool.stop(GameView.RESP_BG);
                break;
            case R.id.menuenginelocal:
                GameView.bFromWhich= GameView.BMFROMLOCAL;
                Toast.makeText(JqmqActivity.this, R.string.menu_engine_local, Toast.LENGTH_SHORT).show();
                break;
            case R.id.menuenginecloud:
                GameView.bFromWhich= GameView.BMFROMCLOUD;
                Toast.makeText(JqmqActivity.this, R.string.menu_engine_cloud, Toast.LENGTH_SHORT).show();
                break;
            case R.id.menuposedit:
                GameView.isBoardEdit = true;
                Toast.makeText(JqmqActivity.this, R.string.menu_pos_edit, Toast.LENGTH_SHORT).show();
                break;
            case R.id.menuposgiveup:
                GameView.isBoardEdit = false;
                Toast.makeText(JqmqActivity.this, R.string.menu_pos_giveup, Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        MenuInflater inflator = new MenuInflater(this);
        inflator.inflate(R.menu.pmenu, menu);
        menu.setHeaderTitle("棋盘上下文菜单在此");
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    //上下文菜单被点击是触发该方法
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menucopyfen:
                Toast.makeText(this, R.string.btn_txt_copyfen, Toast.LENGTH_LONG).show();
                break;
            case R.id.menupastefen:
                Toast.makeText(this, R.string.btn_txt_pastefen, Toast.LENGTH_LONG).show();
                break;
            default:
                Toast.makeText(JqmqActivity.this, R.string.txt_info_working, Toast.LENGTH_SHORT).show();
                break;
        }
        playSound(GameView.RESP_CLICK, 0);
        return true;
    }

    public static Bitmap scaleToFit(Bitmap bm, float fblRatio)//缩放图片的方法
    {
        if (bm != null) {
            int width = bm.getWidth(); //图片宽度
            int height = bm.getHeight();//图片高度
            Matrix matrix = new Matrix();
            matrix.postScale((float) fblRatio, (float) fblRatio);//图片等比例缩小为原来的fblRatio倍
            return Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);//声明位图
        }
        return null;
    }

    private class WelcomeView extends SurfaceView implements SurfaceHolder.Callback {
        SurfaceHolder myholder;
        Paint mPaint;
        int currentAlpha;            //当前的不透明值
        JqmqActivity activity;

        public WelcomeView(JqmqActivity acti) {
            super(acti);
            this.activity = acti;
            currentAlpha = 255;
            mPaint = new Paint();
            myholder = this.getHolder();
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
                    currentAlpha = i;
                    Canvas canvas = myholder.lockCanvas(null);//获取画布
                    try {
                        synchronized (myholder)//同步
                        {
                            mPaint.setColor(Color.BLACK);//设置画笔颜色
                            mPaint.setAlpha(255);       //设置不透明度为255
                            canvas.drawRect(0, 0, screenWidth, screenHeight, mPaint);////绘制黑填充矩形清背景
                            mPaint.setAlpha(currentAlpha);
                            //动态更改图片的透明度值并不断重绘
                            if (imgWelcome != null) {
                                int curX = (int) (screenWidth / 2 - imgWelcome.getWidth() / 2);//图片位置居中
                                int curY = (int) (screenHeight / 2 - imgWelcome.getHeight() / 2);
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
                            sleep(1000);
                        }
                        sleep(50);
                    } catch (Exception e)//抛出异常
                    {
                        e.printStackTrace();
                    }
                }
                activity.hd.sendEmptyMessage(0);//发送消息，开始加载棋子模型
            }
        }
    }

    public void initScreenSize(float w, float h)  //更新自定义view中屏幕尺寸
    {
        //前面代码放入oncreate,后面的放入GameView
        int tempWidth = (int) (GameView.screen_width = w);
        int tempHeight = (int) (GameView.screen_height = h);
        if (tempHeight > tempWidth) {
            GameView.screen_height = tempHeight;
            GameView.screen_width = tempWidth;
        } else {
            GameView.screen_height = tempWidth;
            GameView.screen_width = tempHeight;
        }
    }

    public void initSound() {
        //声音池
        soundPool = new SoundPool(4, AudioManager.STREAM_MUSIC, 100);
        soundPoolMap = new HashMap<Integer, Integer>();

//        private static final int RESP_CLICK = 0;
//        private static final int RESP_ILLEGAL = 1;
//        private static final int RESP_MOVE = 2;
//        private static final int RESP_MOVE2 = 3;
//        private static final int RESP_CAPTURE = 4;
//        private static final int RESP_CAPTURE2 = 5;
//        private static final int RESP_CHECK = 6;
//        private static final int RESP_CHECK2 = 7;
//        private static final int RESP_WIN = 8;
//        private static final int RESP_DRAW = 9;
//        private static final int RESP_LOSS = 10;

        soundPoolMap.put(0, soundPool.load(this, R.raw.click, 1));      //？
        soundPoolMap.put(1, soundPool.load(this, R.raw.illegal, 1));
        soundPoolMap.put(2, soundPool.load(this, R.raw.move, 1));
        soundPoolMap.put(3, soundPool.load(this, R.raw.move, 1));
        soundPoolMap.put(4, soundPool.load(this, R.raw.capture, 1));
        soundPoolMap.put(5, soundPool.load(this, R.raw.capture, 1));
        soundPoolMap.put(6, soundPool.load(this, R.raw.check, 1));
        soundPoolMap.put(7, soundPool.load(this, R.raw.check, 1));
        soundPoolMap.put(8, soundPool.load(this, R.raw.win, 1));
        soundPoolMap.put(9, soundPool.load(this, R.raw.draw, 1));
        soundPoolMap.put(10, soundPool.load(this, R.raw.loss, 1));
        soundPoolMap.put(11, soundPool.load(this, R.raw.classic, 1));   //背景音乐mid
    }

    //播放声音
    public void playSound(int sound, int loop) {
        if (!GameView.isnoPlaySound) {
            return;
        }
        AudioManager mgr = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        float streamVolumeCurrent = mgr.getStreamVolume(AudioManager.STREAM_MUSIC);
        float streamVolumeMax = mgr.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        float volume = streamVolumeCurrent / streamVolumeMax;
        soundPool.play(soundPoolMap.get(sound), volume, volume, 1, loop, 1f);
    }

    public void GotoContentView() {
        setContentView(R.layout.mainlayout);

        gameView = (GameView) findViewById(R.id.gameview);

        Button btnStart = (Button) findViewById(R.id.btnstart);
        final Button btnMenu = (Button) findViewById(R.id.btnmenu);
//        Button btnOpen = (Button) findViewById(R.id.btnopen);
//        Button btnSave = (Button) findViewById(R.id.btnsave);
        Button btnCopyfen = (Button) findViewById(R.id.btncopyfen);
        Button btnPastefen = (Button) findViewById(R.id.btnpastefen);
        btnShowPiece = (Button) findViewById(R.id.btnshowpiece);
        Button btnPrompt = (Button) findViewById(R.id.btnprompt);
        Button btnUndo = (Button) findViewById(R.id.btnundo);
        final Button btnReturn = (Button) findViewById(R.id.btnreturn);
        edtFen = (EditText) findViewById(R.id.edtInfofen);

       // registerForContextMenu(gameView);         //没调试好
        btnStart.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            gameView.restart();
                                            btnShowPiece.setText(R.string.btn_txt_hideboard);
                                        }
                                    }
        );

        btnMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSound(GameView.RESP_CLICK, 0);
            }
        });

        btnCopyfen.setOnClickListener(new View.OnClickListener() {
                                          @Override
                                          public void onClick(View view) {
                                              edtFen.setText(GameView.pos.toFen());
                                              playSound(GameView.RESP_CLICK, 0);
                                          }
                                      }
        );

        btnPastefen.setOnClickListener(new View.OnClickListener() {
                                           @Override
                                           public void onClick(View view) {
                                               GameView.pos.fromFen(edtFen.getText().toString());
                                               GameView.isvisible = 2;
                                               btnShowPiece.setText(R.string.btn_txt_hideboard);
                                               gameView.invalidate();
                                               playSound(GameView.RESP_CLICK, 0);
                                           }
                                       }
        );

        btnShowPiece.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                int[] guanggao={R.drawable.field, R.drawable.couple, R.drawable.brothers};
                                                final int MAXNUM = 3;       //自定义棋盘view最大背景图片数量
                                                int suijishu = (int) (Math.random() * MAXNUM);
                                                Log.d(TAG, "" + suijishu);
                                                //隐藏棋盘棋子后，可以在这里随机改变自定义棋盘view的背景（可作为广告画面哈）。
                                                gameView.setBackgroundResource( guanggao[suijishu] );
                                                //gameView.setBackground( getDrawable(guanggao[suijishu] ));

                                                switch (GameView.isvisible){
                                                    case 0:
                                                        GameView.isvisible = 1;
                                                        btnShowPiece.setText(R.string.btn_txt_showpiece);
                                                        break;
                                                    case 1:
                                                        GameView.isvisible = 2;
                                                        btnShowPiece.setText(R.string.btn_txt_hideboard);
                                                        break;
                                                    default:
                                                        GameView.isvisible = 0;
                                                        btnShowPiece.setText(R.string.btn_txt_showboard);
                                                        break;
                                                }
                                                gameView.invalidate();
                                                playSound(GameView.RESP_CLICK, 0);
                                            }
                                        }
        );

        btnPrompt.setOnClickListener(new View.OnClickListener() {
                                         @Override
                                         public void onClick(View view) {
                                             Toast.makeText(JqmqActivity.this, getString(R.string.txt_info_prompt_toast), Toast.LENGTH_LONG).show();
                                             int mv = GameView.search.searchMain(100 << (1 << 1));
                                             GameView.pos.makeMove(mv);
                                             gameView.invalidate();
                                             Toast.makeText(JqmqActivity.this, getString(R.string.txt_info_prompt), Toast.LENGTH_LONG).show();
                                             edtFen.setText(Integer.toHexString(mv));
                                             mv = GameView.search.searchMain(100 << (1 << 1));
                                             GameView.pos.makeMove(mv);
                                             gameView.invalidate();
                                         }
                                     }
        );

        btnUndo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if(GameView.pos.distance>0)
                    {
                        Toast.makeText(JqmqActivity.this, getString(R.string.txt_info_undo), Toast.LENGTH_LONG).show();
                        GameView.pos.undoMakeMove();
                        gameView.invalidate();
                    }
                    if(GameView.pos.distance>0)
                    {
                        Toast.makeText(JqmqActivity.this, getString(R.string.txt_info_undo), Toast.LENGTH_LONG).show();
                        GameView.pos.undoMakeMove();
                        gameView.invalidate();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        btnReturn.setOnClickListener(new View.OnClickListener() {
                                         @Override
                                         public void onClick(View view) {
                                             finish();
                                         }
                                     }
        );
    }
}
