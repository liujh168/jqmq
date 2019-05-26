package com.liujh168.jqmq;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Environment;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
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

public class GameView extends View implements View.OnTouchListener {

    private GestureDetector mGestureDetector;

    public static float screen_height;         //screen hight，从其它模块得到。不变的参数
    public static float screen_width;          //screen screen_width

    //    原始棋盘图参数，从属性得来
    public static float board_width = 560f;            //棋盘的大小
    public static float board_height = 646f;
    public static float board_ox = 53.0f;              //左上角棋子位置（相对于棋盘）
    public static float board_oy = 71.0f;
    private static float board_dd = 56f;                //棋盘格大小

    public static float xZoom = screen_width / board_width;     //要充满屏幕的缩放比例
    public static float yZoom = screen_height / board_height;

    public static float boardWidth = board_width * xZoom;          //棋盘的大小
    public static float boardHeight = board_height * yZoom;
    public static float boardOX = board_ox * xZoom;                 //左上角棋子位置（相对于棋盘）
    public static float boardOY = board_oy * yZoom;
    private static float SQUARE_SIZE = board_dd * xZoom;          //棋盘格大小

    public static float imgX = (screen_width - boardWidth * xZoom) / 2;     //棋盘图像的起始坐标
    public static float imgY = (screen_width - boardHeight * yZoom) / 2;
    int viewLeft = 0, viewTop = 0;                               //棋盘view的位置
    public static int isvisible = 2;                                 //控制棋盘棋子是否显示

    public static boolean isnoPlaySound = true; //是否播放声音
    public static boolean isBoardEdit = false;  //是否棋盘编辑状态

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

    private String resultMessage = "";
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
    Bitmap imgSelected, imgBoard, imgbrothers;

    JqmqActivity father;

    public static Position pos;
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
        this.father = (JqmqActivity) context;
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.father = (JqmqActivity) context;

        TypedArray boardattrs = context.obtainStyledAttributes(attrs, R.styleable.GameView);
        board_ox = boardattrs.getInteger(R.styleable.GameView_boardox, 53);     //左上角棋子位置（相对于棋盘）
        board_oy = boardattrs.getInteger(R.styleable.GameView_boardoy, 71);
        board_width = boardattrs.getInteger(R.styleable.GameView_boardw, 560);//棋盘的大小
        board_height = boardattrs.getInteger(R.styleable.GameView_boardh, 646);
        board_dd = boardattrs.getInteger(R.styleable.GameView_boarddd, 56);
        isvisible = boardattrs.getInteger(R.styleable.GameView_isVisible, 0);
        currentFen = boardattrs.getString(R.styleable.GameView_fen);
        int boardbg = boardattrs.getColor(R.styleable.GameView_boardbg, Color.RED);
        int boardline = boardattrs.getColor(R.styleable.GameView_boardline, Color.RED);
        boardattrs.recycle();//TypedArray对象回收
        Log.d(TAG, "GameView: board_ox=" + board_ox);
        Log.d(TAG, "GameView: board_oy=" + board_oy);
        Log.d(TAG, "GameView: board_dd=" + board_dd);
        Log.d(TAG, "GameView: board_width=" + board_width);
        Log.d(TAG, "GameView: board_height=" + board_height);
        Log.d(TAG, "GameView: board_fen=" + currentFen);

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

        mGestureDetector = new GestureDetector(new gestureListener()); //使用派生自OnGestureListener
        setOnTouchListener(this);
        setFocusable(true);
        setClickable(true);
       // setLongClickable(true);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = getMySize((int) imgX, widthMeasureSpec);
        int height = getMySize((int) imgY, heightMeasureSpec);
        height = (int) ((width / 9) * 10 + board_dd * 0.8f);
        setMeasuredDimension(width, height);
        viewLeft = getLeft();
        viewTop = getTop();
        Log.d("测量 GameView", "view_width: " + width + "   view_height: " + height);
        Log.d("测量 GameView", "viewLeft=" + viewLeft + "   viewTop=" + viewTop);
    }

//    @Override
//    public boolean onTouchEvent(MotionEvent e) {
////        super(e);
//        float currentX = e.getX();
//        float currentY = e.getY();
//        int action = e.getAction();
//        switch (action) {
//            case MotionEvent.ACTION_DOWN:
//                if (!thinking) {
//                    currentX = e.getX() - (boardOX - SQUARE_SIZE / 2);
//                    currentY = e.getY() - (boardOY - SQUARE_SIZE / 2);
//                    int x = Util.MIN_MAX(0, (int) (currentX / SQUARE_SIZE), 8);
//                    int y = Util.MIN_MAX(0, (int) (currentY / SQUARE_SIZE), 9);
//                    int currsq = Position.COORD_XY(x + Position.FILE_LEFT, y + Position.RANK_TOP);
////                    Log.d("点击选择的棋盘格为：",Integer.toHexString(currsq));
//                    if (!isBoardEdit) {
//                        clickSquare(currsq);
//                    } else {
//                        clickSquareOnBoardEdit(currsq);
//                    }
//                }
//
//                break;
//            case MotionEvent.ACTION_MOVE:
//                break;
//            case MotionEvent.ACTION_UP:
//                break;
//        }
//        return false;
//    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (isvisible == 0) {
            return;  //仅显示广告
        }

        if (isvisible == 1) {
            canvas.drawBitmap(JqmqActivity.scaleToFit(imgBoard, xZoom), 0, 0, null);
            return;  //仅显示棋盘，不显示棋子
        }

        //以下正常显示棋盘棋子
        canvas.drawBitmap(JqmqActivity.scaleToFit(imgBoard, xZoom), 0, 0, null);
        lock.lock();  //这里应该只锁pos数据更新，重画时间久
        for (int x = Position.FILE_LEFT; x <= Position.FILE_RIGHT; x++) {
            for (int y = Position.RANK_TOP; y <= Position.RANK_BOTTOM; y++) {
                int sq = Position.COORD_XY(x, y);
                sq = (flipped ? Position.SQUARE_FLIP(sq) : sq);
                int pc = pos.squares[sq];
                float xx = boardOX + (x - Position.FILE_LEFT) * SQUARE_SIZE;
                float yy = boardOY + (y - Position.RANK_TOP) * SQUARE_SIZE;
                if (pc > 0) {
                    canvas.drawBitmap(JqmqActivity.scaleToFit(imgPieces[pc], xZoom * 1.15f), xx - SQUARE_SIZE / 2, yy - SQUARE_SIZE / 2, null);
                }
                if (sq == sqSelected || sq == Position.SRC(mvLast) || sq == Position.DST(mvLast)) {
                    canvas.drawBitmap(JqmqActivity.scaleToFit(imgSelected, xZoom), xx - SQUARE_SIZE / 2, yy - SQUARE_SIZE / 2, null);
                }
            }
        }
        lock.unlock();
    }

    private void loadBoard() {
        imgBoard = BitmapFactory.decodeResource(getResources(), R.drawable.board);
        imgbrothers = BitmapFactory.decodeResource(getResources(), R.drawable.brothers);
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
                Toast.makeText(father, resultMessage, Toast.LENGTH_LONG);
                thinking();
            }
        }
    }

    //摆棋状态的clicksquare.在退出摆棋状态时应该检查棋盘的正确性。
    void clickSquareOnBoardEdit(int sq_) {
        final int sq = (flipped ? Position.SQUARE_FLIP(sq_) : sq_);
        int pc = pos.squares[sq];
        if (pc != 0) {
            if (sqSelected == sq) {
                pos.squares[sq] = 0;
                sqSelected = 0;
            } else {
                sqSelected = sq;
            }
            playSound(RESP_CLICK);
        } else if (sqSelected > 0) {
            pos.squares[sq] = pos.squares[sqSelected];
            pos.squares[sqSelected] = 0;
            sqSelected = 0;
            playSound(RESP_CLICK);
        } else {
            //以下弹出棋子列表菜单
            PopupMenu popup = new PopupMenu(father, this);
            popup.getMenuInflater().inflate(R.menu.cmenu, popup.getMenu());
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.popmenurr:
                            pos.squares[sq] = 8 + Position.PIECE_ROOK;
                            Log.d(TAG, "onMenuItemClick: 红车");
                            break;
                        case R.id.popmenurn:
                            pos.squares[sq] = 8 + Position.PIECE_KNIGHT;
                            Log.d(TAG, "onMenuItemClick: 红马");
                            break;
                        case R.id.popmenurc:
                            pos.squares[sq] = 8 + Position.PIECE_CANNON;
                            Log.d(TAG, "onMenuItemClick: 红炮");
                            break;
                        case R.id.popmenurp:
                            pos.squares[sq] = 8 + Position.PIECE_PAWN;
                            Log.d(TAG, "onMenuItemClick: 红兵");
                            break;
                        case R.id.popmenurb:
                            pos.squares[sq] = 8 + Position.PIECE_BISHOP;
                            Log.d(TAG, "onMenuItemClick: 红相");
                            break;
                        case R.id.popmenura:
                            pos.squares[sq] = 8 + Position.PIECE_ADVISOR;
                            Log.d(TAG, "onMenuItemClick: 红仕");
                            break;
                        case R.id.popmenurk:
                            pos.squares[sq] = 8 + Position.PIECE_KING;
                            Log.d(TAG, "onMenuItemClick: 红帅");
                            break;
                        case R.id.popmenubr:
                            pos.squares[sq] = 16 + Position.PIECE_ROOK;
                            break;
                        case R.id.popmenubn:
                            pos.squares[sq] = 16 + Position.PIECE_KNIGHT;
                            break;
                        case R.id.popmenubc:
                            pos.squares[sq] = 16 + Position.PIECE_CANNON;
                            break;
                        case R.id.popmenubp:
                            pos.squares[sq] = 16 + Position.PIECE_PAWN;
                            break;
                        case R.id.popmenubb:
                            pos.squares[sq] = 16 + Position.PIECE_BISHOP;
                            break;
                        case R.id.popmenuba:
                            pos.squares[sq] = 16 + Position.PIECE_ADVISOR;
                            break;
                        case R.id.popmenubk:
                            pos.squares[sq] = 16 + Position.PIECE_KING;
                            break;
                        default:
                            pos.squares[sq] = 0;
                            break;
                    }
                    invalidate();
                    return true;
                }
            });
            popup.show();
        }
        invalidate();
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
                        Toast.makeText(father, resultMessage, Toast.LENGTH_LONG);
                    }
                });
                thinking = false;
            }
        }.start();
    }

    void playSound(int response) {
        father.playSound(response, 0);
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
        xZoom = screen_width / board_width;
        yZoom = screen_height / board_height;
        if (xZoom > yZoom) {
            xZoom = yZoom;
        } else {
            yZoom = xZoom;
        }

        imgX = (screen_width - board_width * xZoom) / 2;
        imgY = (screen_height - board_height * yZoom) / 2;

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

    // 在onTouch()方法中，我们调用GestureDetector的onTouchEvent()方法，将捕捉到的MotionEvent交给GestureDetector
    // 来分析是否有合适的callback函数来处理用户的手势
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.i("GameView", "onTouch");
        //Toast.makeText(father, "onTouch", Toast.LENGTH_LONG).show();
        return mGestureDetector.onTouchEvent(event);
    }

    private class gestureListener implements GestureDetector.OnGestureListener {
        // 用户轻触触摸屏，由1个MotionEvent ACTION_DOWN触发
        public boolean onDown(MotionEvent e) {
            Log.i("MyGesture", "onDownPress");
            //Toast.makeText(father, "onDown", Toast.LENGTH_SHORT).show();

//        super(e);
            float currentX = e.getX();
            float currentY = e.getY();
            int action = e.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    if (!thinking) {
                        currentX = e.getX() - (boardOX - SQUARE_SIZE / 2);
                        currentY = e.getY() - (boardOY - SQUARE_SIZE / 2);
                        int x = Util.MIN_MAX(0, (int) (currentX / SQUARE_SIZE), 8);
                        int y = Util.MIN_MAX(0, (int) (currentY / SQUARE_SIZE), 9);
                        int currsq = Position.COORD_XY(x + Position.FILE_LEFT, y + Position.RANK_TOP);
//                    Log.d("点击选择的棋盘格为：",Integer.toHexString(currsq));
                        if (!isBoardEdit) {
                            clickSquare(currsq);
                        } else {
                            clickSquareOnBoardEdit(currsq);
                        }
                    }

                    break;
                case MotionEvent.ACTION_MOVE:
                    break;
                case MotionEvent.ACTION_UP:
                    break;
            }
            return false;


            //return false;
        }

        //用户轻触触摸屏，尚未松开或拖动，由一个1个MotionEvent ACTION_DOWN触发
        //注意和onDown()的区别，强调的是没有松开或者拖动的状态
        // 而onDown也是由一个MotionEventACTION_DOWN触发的，但是他没有任何限制，
        // 也就是说当用户点击的时候，首先MotionEventACTION_DOWN，onDown就会执行，
        // 如果在按下的瞬间没有松开或者是拖动的时候onShowPress就会执行，如果是按下的时间超过瞬间
        // （这块我也不太清楚瞬间的时间差是多少，一般情况下都会执行onShowPress），拖动了，就不执行onShowPress。
        public void onShowPress(MotionEvent e) {
            Log.i("MyGesture", "onShowPress");
            //Toast.makeText(father, "onShowPress", Toast.LENGTH_SHORT).show();
        }

        // 用户（轻触触摸屏后）松开，由一个1个MotionEvent ACTION_UP触发
        ///轻击一下屏幕，立刻抬起来，才会有这个触发
        //从名子也可以看出,一次单独的轻击抬起操作,当然,如果除了Down以外还有其它操作,那就不再算是Single操作了,所以这个事件 就不再响应
        public boolean onSingleTapUp(MotionEvent e) {
            Log.i("MyGesture", "onSingleTapUp");
           // Toast.makeText(father, "onSingleTapUp", Toast.LENGTH_SHORT).show();
            return true;
        }

        // 用户按下触摸屏，并拖动，由1个MotionEvent ACTION_DOWN, 多个ACTION_MOVE触发
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                float distanceX, float distanceY) {
            Log.i("MyGesture22", "onScroll:" + (e2.getX() - e1.getX()) + "   " + distanceX);
            //Toast.makeText(father, "onScroll", Toast.LENGTH_LONG).show();
            return true;
        }

        // 用户长按触摸屏，由多个MotionEvent ACTION_DOWN触发
        public void onLongPress(MotionEvent e) {
            Log.i("MyGesture", "onLongPress");
            father.edtFen.setText("onLongPress");
            //Toast.makeText(father, "onLongPress", Toast.LENGTH_LONG).show();
        }

        // 用户按下触摸屏、快速移动后松开，由1个MotionEvent ACTION_DOWN, 多个ACTION_MOVE, 1个ACTION_UP触发
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                               float velocityY) {
            Log.i("MyGesture", "onFling");
            Toast.makeText(father, "onFling", Toast.LENGTH_LONG).show();
            return true;
        }
    }

    ;
}
