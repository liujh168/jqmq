package com.liujh168.jqmq;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static android.content.ContentValues.TAG;
import static java.util.concurrent.locks.Lock.*;

import com.liujh168.jqmq.Position;

public class GameView extends View {
    
    public static float screen_height;         //screen hight，从其它模块得到。不变的参数
    public static float screen_width;          //screen screen_width

    //    原始棋盘图参数，从属性得来
    public static float board_width = 560f ;            //棋盘的大小
    public static float board_height = 646f;
    public static float board_ox = 53.0f ;              //左上角棋子位置（相对于棋盘）
    public static float board_oy = 71.0f ;
    private static float board_dd = 56f;                //棋盘格大小

    public static float xZoom = screen_width/board_width;     //要充满屏幕的缩放比例
    public static float yZoom = screen_height/board_height;

    public static float boardWidth = board_width * xZoom;          //棋盘的大小
    public static float boardHeight = board_height * yZoom;
    public static float boardOX = board_ox * xZoom;                 //左上角棋子位置（相对于棋盘）
    public static float boardOY = board_oy * yZoom;
    private static float SQUARE_SIZE = board_dd * xZoom;          //棋盘格大小

    public static float imgX = (screen_width-boardWidth*xZoom)/2;     //棋盘图像的起始坐标
    public static float imgY = (screen_width-boardHeight*yZoom)/2;
    int viewLeft=0,         viewTop=0;                               //棋盘view的位置
    public static int isvisible = 0;                                    //控制棋盘棋子是否显示

    public static boolean isnoPlaySound = true;//是否播放声音

    public static final int RESP_CLICK = 0;
    public static final int RESP_ILLEGAL = 1;
    public static final int RESP_MOVE = 2;
    public static final int RESP_MOVE2 = 3;
    public static final int RESP_CAPTURE = 4;
    public static final int RESP_CAPTURE2 = 5;
    public static final int RESP_CHECK = 6;
    public static final int RESP_CHECK2 = 7;
    public static final int RESP_WIN = 8;
    public static final int RESP_DRAW = 9;
    public static final int RESP_LOSS = 10;
    public static final int RESP_BG = 11;          //背景音乐

    private String resultMessage="";
    private static final String[] PIECE_NAME = {
            null, null, null, null, null, null, null, null,
            "rk", "ra", "rb", "rn", "rr", "rc", "rp", null,
            "bk", "ba", "bb", "bn", "br", "bc", "bp", null,
    };

    private static final String[] BOARD_NAME = {
            "wood", "green", "white", "sheet", "canvas", "drops", "qianhong"
    };

    private static final String[] PIECES_NAME = {
            "wood", "delicate", "polish"
    };

    private static final String[] SOUND_NAME = {
            "click", "illegal", "move", "move2", "capture", "capture2",
            "check", "check2", "win", "draw", "loss"
    };

    private static final String[] MUSIC_NAME = {
            "express", "funny", "classic", "mozart1", "mozart4", "furelise",
            "lovdream", "waltz", "humour", "pal", "cmusic"
    };

    static final int MUSIC_MUTE = MUSIC_NAME.length;

    static final String[] LEVEL_TEXT = {
            "入门", "业余", "专业", "大师", "特级大师"
    };

    Bitmap[] imgPieces = new Bitmap[PIECE_NAME.length];
    Bitmap imgSelected, imgBoard;

    JqmqActivity father;

    public  static Position pos;
    public static Search search;

    private Paint paint;
    volatile boolean thinking;
    String currentFen;
    String retractFen;
    int sqSelected, mvLast;
    boolean flipped;
    int handicap, level, board, pieces, music;
    int mWidth, mHight;

    private Lock lock = new ReentrantLock();

    public GameView(Context context) {
        super(context);
        this.father=(JqmqActivity) context;
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.father=(JqmqActivity)context;

        TypedArray boardattrs = context.obtainStyledAttributes(attrs, R.styleable.GameView);
        board_ox = boardattrs.getInteger(R.styleable.GameView_boardox, 53);     //左上角棋子位置（相对于棋盘）
        board_oy = boardattrs.getInteger(R.styleable.GameView_boardoy, 71);
        board_width = boardattrs.getInteger(R.styleable.GameView_boardw, 560);//棋盘的大小
        board_height = boardattrs.getInteger(R.styleable.GameView_boardh, 646);
        board_dd = boardattrs.getInteger(R.styleable.GameView_boarddd, 56);
        isvisible = boardattrs.getInteger(R.styleable.GameView_isVisible, 0);
        currentFen = boardattrs.getString(R.styleable.GameView_fen);
        int boardbg= boardattrs.getColor(R.styleable.GameView_boardbg, Color.RED);
        int boardline = boardattrs.getColor(R.styleable.GameView_boardline, Color.RED);
        boardattrs.recycle();//TypedArray对象回收
        Log.d(TAG, "GameView: board_ox="+board_ox);
        Log.d(TAG, "GameView: board_oy="+board_oy);
        Log.d(TAG, "GameView: board_dd="+board_dd);
        Log.d(TAG, "GameView: board_width="+board_width);
        Log.d(TAG, "GameView: board_height="+board_height);
        Log.d(TAG, "GameView: board_fen="+currentFen);

        initChessViewFinal();   //更新相关绘图参数

        this.paint = new Paint();
//        this.paint.setColor(boardline);
        this.paint.setStrokeWidth(3);

        loadPieces();
        loadBoard();
        pos = new Position();
        search = new Search(pos, 16);
        currentFen = Position.STARTUP_FEN[0];
        retractFen = currentFen;
        flipped = false;
        thinking = false;
        int handicap = 0, level = 1, board = 0, pieces = 0, music = 8;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = getMySize((int)imgX, widthMeasureSpec);
        int height = getMySize((int)imgY, heightMeasureSpec);
        height = (int)((width/9)*10 + board_dd*0.8f) ;
        setMeasuredDimension(width, height);
        viewLeft=getLeft();
        viewTop=getTop();
        Log.d("测量 GameView","view_width: " + width + "   view_height: " + height);
        Log.d("测量 GameView","viewLeft=" + viewLeft + "   viewTop=" + viewTop);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float currentX = e.getX();
        float currentY = e.getY();
        int action = e.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (!thinking) {
                    currentX = e.getX() - (boardOX - SQUARE_SIZE/2);
                    currentY = e.getY() - (boardOY - SQUARE_SIZE/2);
                    int x = Util.MIN_MAX(0, (int) (currentX / SQUARE_SIZE), 8);
                    int y = Util.MIN_MAX(0, (int) (currentY / SQUARE_SIZE), 9);
                    int currsq = Position.COORD_XY(x + Position.FILE_LEFT, y + Position.RANK_TOP);
//                    Log.d("点击选择的棋盘格为：",Integer.toHexString(currsq));
                    clickSquare(currsq);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                break;
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //        paint.setColor(Color.RED);
        //        canvas.drawCircle(currentX, currentY, 35, paint);
        canvas.drawBitmap(JqmqActivity.scaleToFit(imgBoard, xZoom), 0, 0, null);

        if(isvisible == 0) return;  //不显示棋子

//        pos.fromFen("rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR w");
        lock.lock();  //这里应该只锁pos数据更新，重画时间久
        for (int x = Position.FILE_LEFT; x <= Position.FILE_RIGHT; x++) {
            for (int y = Position.RANK_TOP; y <= Position.RANK_BOTTOM; y++) {
                int sq = Position.COORD_XY(x, y);
                sq = (flipped ? Position.SQUARE_FLIP(sq) : sq);
                int pc = pos.squares[sq];
                float xx = boardOX + (x - Position.FILE_LEFT) * SQUARE_SIZE ;
                float yy = boardOY + (y - Position.RANK_TOP) * SQUARE_SIZE ;
                if (pc > 0) {
                    canvas.drawBitmap(JqmqActivity.scaleToFit(imgPieces[pc], xZoom*1.15f), xx-SQUARE_SIZE/2, yy-SQUARE_SIZE/2, null);
                }
				if (sq == sqSelected || sq == Position.SRC(mvLast) || sq == Position.DST(mvLast)) {
					canvas.drawBitmap(JqmqActivity.scaleToFit(imgSelected,xZoom), xx-SQUARE_SIZE/2, yy-SQUARE_SIZE/2, null);
				}
            }
        }
        lock.unlock();
    }

    private void loadBoard() {
        imgBoard = BitmapFactory.decodeResource(getResources(), R.drawable.board);
    }

    private void loadPieces() {
        imgSelected = BitmapFactory.decodeResource(getResources(), R.drawable.mm);

        imgPieces[8] = BitmapFactory.decodeResource(getResources(), R.drawable.rk);
        imgPieces[9] = BitmapFactory.decodeResource(getResources(), R.drawable.ra);
        imgPieces[10] = BitmapFactory.decodeResource(getResources(), R.drawable.rb);
        imgPieces[11] = BitmapFactory.decodeResource(getResources(), R.drawable.rn);
        imgPieces[12] = BitmapFactory.decodeResource(getResources(), R.drawable.rr);
        imgPieces[13] = BitmapFactory.decodeResource(getResources(), R.drawable.rc);
        imgPieces[14] = BitmapFactory.decodeResource(getResources(), R.drawable.rp);
        imgPieces[16] = BitmapFactory.decodeResource(getResources(), R.drawable.bk);
        imgPieces[17] = BitmapFactory.decodeResource(getResources(), R.drawable.ba);
        imgPieces[18] = BitmapFactory.decodeResource(getResources(), R.drawable.bb);
        imgPieces[19] = BitmapFactory.decodeResource(getResources(), R.drawable.bn);
        imgPieces[20] = BitmapFactory.decodeResource(getResources(), R.drawable.br);
        imgPieces[21] = BitmapFactory.decodeResource(getResources(), R.drawable.bc);
        imgPieces[22] = BitmapFactory.decodeResource(getResources(), R.drawable.bp);
    }

    void clickSquare(int sq_) {
        int sq = (flipped ? Position.SQUARE_FLIP(sq_) : sq_);
        int pc = pos.squares[sq];
        if ((pc & Position.SIDE_TAG(pos.sdPlayer)) != 0) {
            if (mvLast > 0) {
                mvLast = 0;
            }
            sqSelected = sq;
            invalidate();
            playSound(RESP_CLICK);
        } else if (sqSelected > 0) {
            int mv = Position.MOVE(sqSelected, sq);
            if (!pos.legalMove(mv)) {
                playSound(RESP_ILLEGAL);
                Toast.makeText(father, "不能这么走啊，亲", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!pos.makeMove(mv)) {
                playSound(RESP_ILLEGAL);
                Toast.makeText(father, "不能送将啊，亲", Toast.LENGTH_SHORT).show();
                return;
            }
            int response = pos.inCheck() ? RESP_CHECK :
                    pos.captured() ? RESP_CAPTURE : RESP_MOVE;
            if (pos.captured()) {
                pos.setIrrev();
            }
            mvLast = mv;
            sqSelected = 0;

            invalidate();
            playSound(response);
            if (!getResult()) {
                Toast.makeText(father,resultMessage,Toast.LENGTH_LONG);
                thinking();
            }
        }
    }

    void thinking() {
        thinking = true;
        new Thread() {
            public void run() {
                int mv = mvLast;
                lock.lock();
                mvLast = search.searchMain(100 << (level << 1));
                pos.makeMove(mvLast);
                lock.unlock();
                int response = pos.inCheck() ? RESP_CHECK2 :
                        pos.captured() ? RESP_CAPTURE2 : RESP_MOVE2;
                if (pos.captured()) {
                    pos.setIrrev();
                }
                getResult(response);
                //此处更新棋盘，在UI线程提示消息
                father.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        invalidate();
                        Toast.makeText(father,resultMessage,Toast.LENGTH_LONG);
                    }
                });
                thinking = false;
            }
        }.start();
    }

    void playSound(int response) {
        father.playSound(response,0);
    }

    /**
     * Player Move Result
     */
    boolean getResult() {
        return getResult(-1);
    }

    /**
     * Computer Move Result
     */
    boolean getResult(int response) {
        if (pos.isMate()) {
            resultMessage = "response < 0 ? \"祝贺你取得胜利！\" : \"请再接再厉！\"";
            playSound(response < 0 ? RESP_WIN : RESP_LOSS);
            return true;
        }
        int vlRep = pos.repStatus(3);
        if (vlRep > 0) {
            vlRep = (response < 0 ? pos.repValue(vlRep) : -pos.repValue(vlRep));
            playSound(vlRep > Position.WIN_VALUE ? RESP_LOSS :
                    vlRep < -Position.WIN_VALUE ? RESP_WIN : RESP_DRAW);
            resultMessage = vlRep > Position.WIN_VALUE ? "长打作负，请不要气馁！" :
                    vlRep < -Position.WIN_VALUE ? "电脑长打作负，祝贺你取得胜利！" : "双方不变作和，辛苦了！";
            return true;
        }
        if (pos.moveNum > 100) {
            playSound(RESP_DRAW);
            resultMessage = "超过自然限着作和，辛苦了！";
            return true;
        }
        if (response >= 0) {
            playSound(response);
            retractFen = currentFen;
            currentFen = pos.toFen();
        }
        return false;
    }

    public static void initChessViewFinal() {
        xZoom = screen_width/board_width;
        yZoom = screen_height/board_height;
        if(xZoom>yZoom){
            xZoom = yZoom;
        }else{
            yZoom=xZoom;
        }

        imgX = (screen_width - board_width* xZoom)/2;
        imgY = (screen_height - board_height* yZoom)/2;

        boardOX = board_ox * xZoom;     //左上角棋子位置（相对于棋盘）
        boardOY = board_oy * yZoom;
        boardWidth = board_width * xZoom;          //棋盘的大小
        boardHeight = board_height * xZoom;
        SQUARE_SIZE = board_dd * xZoom;          //棋盘格大小
    }

    private void restart() {
        pos.fromFen(currentFen);
        retractFen = currentFen;
        sqSelected = mvLast = 0;
        invalidate();
        if (flipped && pos.sdPlayer == 0) {
            thinking();
        }
    }

    private int getMySize(int defaultSize, int measureSpec) {
        int mySize = defaultSize;

        int mode = MeasureSpec.getMode(measureSpec);
        int size = MeasureSpec.getSize(measureSpec);

        switch (mode) {
            case MeasureSpec.UNSPECIFIED: {//如果没有指定大小，就设置为默认大小
                mySize = defaultSize;
                break;
            }
            case MeasureSpec.AT_MOST: {//如果测量模式是最大取值为size
                //我们将大小取最大值,你也可以取其他值
                mySize = size;
                break;
            }
            case MeasureSpec.EXACTLY: {//如果是固定的大小，那就不要去改变它
                mySize = size;
                break;
            }
        }
        return mySize;
    }

    private void setimgsize(ImageView img) {
        //设置图片宽高
        img.setLayoutParams(new ViewGroup.LayoutParams(mWidth, mHight));
        img.setImageResource(R.drawable.ba);
        img.setScaleType(ImageView.ScaleType.FIT_CENTER);
    }

    private void getWidthHight() {
        this.post(new Runnable() {
            @Override
            public void run() {
                mWidth = getWidth();
                mHight = mWidth;
                Log.d(TAG, "run: this.post.Runnable.run of getWidthHight");
            }
        });
    }
    //根据Position数据生成棋盘棋子图片(这个函数未使用，待调试）
    private void drawBoard() {
        Bitmap board = BitmapFactory.decodeResource(getResources(), R.drawable.board);
        // 得到图片的宽、高
        int width_board = board.getWidth();
        int height_board = board.getHeight();

        // 创建一个你需要尺寸的Bitmap
        Bitmap mBitmap = Bitmap.createBitmap(width_board, height_board, Bitmap.Config.ARGB_8888);
        // 用这个Bitmap生成一个Canvas,然后canvas就会把内容绘制到上面这个bitmap中
        Canvas mCanvas = new Canvas(mBitmap);

        // 绘制棋盘图片
        mCanvas.drawBitmap(board, 0.0f, 0.0f, paint);
        // 绘制棋子图片
        Bitmap piece = BitmapFactory.decodeResource(getResources(), R.drawable.bk);

        // 得到棋子的宽、高
        int width_piece = piece.getWidth();
        int height_piece = piece.getHeight();
        // 绘制图片－－保证其在水平方向居中
        mCanvas.drawBitmap(piece, (width_board - width_piece) / 2, 0.0f,
                paint);

        // 绘制文字
        paint.setColor(Color.WHITE);// 白色画笔
        paint.setTextSize(80.0f);// 设置字体大小

        // 绘制文字
        paint.setColor(Color.RED);// 红色画笔
        paint.setTextSize(120.0f);// 设置字体大小

        String distanceTextString = "中国象棋盲棋训练系统：";
        String distanceDataString = String.valueOf(888);
        String distanceScalString = "元";

        float distanceTextString_width = paint.measureText(
                distanceTextString, 0, distanceTextString.length());

        float distanceDataString_width = paint.measureText(
                distanceDataString, 0, distanceDataString.length());
        float distanceScalString_width = paint.measureText(
                distanceScalString, 0, distanceScalString.length());
        float x = (width_board - distanceTextString_width
                - distanceDataString_width - distanceScalString_width) / 2;

        mCanvas.drawText(distanceTextString, x, height_piece, paint);// 绘制文字
        mCanvas.drawText(distanceDataString, x + distanceTextString_width,
                height_piece, paint);// 绘制文字

        mCanvas.drawText(distanceScalString, x + distanceTextString_width
                + distanceDataString_width, height_piece, paint);// 绘制文字

        // 保存绘图为本地图片
        mCanvas.save(Canvas.ALL_SAVE_FLAG);
        mCanvas.restore();

        File file = new File(Environment.getExternalStorageDirectory().getPath() + "/盲棋.png");
        Log.i("CXC", Environment.getExternalStorageDirectory().getPath());
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            mBitmap.compress(Bitmap.CompressFormat.PNG, 50, fos);

        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            fos.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}

