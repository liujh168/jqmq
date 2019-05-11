package com.liujh168.jqmq;

import java.util.Stack;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import static android.content.ContentValues.TAG;

public class GameView extends View {

	private static final int RESP_CLICK = 0;
	private static final int RESP_ILLEGAL = 1;
	private static final int RESP_MOVE = 2;
	private static final int RESP_MOVE2 = 3;
	private static final int RESP_CAPTURE = 4;
	private static final int RESP_CAPTURE2 = 5;
	private static final int RESP_CHECK = 6;
	private static final int RESP_CHECK2 = 7;
	private static final int RESP_WIN = 8;
	private static final int RESP_DRAW = 9;
	private static final int RESP_LOSS = 10;

	private static final int PIECE_MARGIN = 8;
	private static final int SQUARE_SIZE = 56;
	private static final int BOARD_WIDTH = 521;
	private static final int BOARD_HEIGHT = 577;
	private static final int ITEM_WIDTH = 100;
	private static final int ITEM_HEIGHT = 20;

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

	private Paint paint;
	volatile boolean thinking;
	Position pos;
	Search search;
	String currentFen, retractFen;
	int sqSelected, mvLast;
	boolean flipped;
	int handicap, level, board , pieces , music ;
	public float currentX=40,currentY=50;
	int mWidth,mHight;

	public GameView(Context context){
		super(context);
	}

	public GameView(Context context, AttributeSet attrs){
		super(context, attrs);
		this.paint=new Paint();
		this.paint.setColor(Color.RED);
		this.paint.setStrokeWidth(3);

        loadPieces();
		pos = new Position();
		search = new Search(pos, 16);
		currentFen = Position.STARTUP_FEN[0];
		retractFen=currentFen;
		flipped = false;
		thinking = false;
		int handicap = 0, level = 0, board = 0, pieces = 0, music = 8;
	}

	private void getWidthHight() {
		this.post(new Runnable() {
			@Override
			public void run() {
				mWidth = getWidth();
				mHight = mWidth;
				Log.i(TAG, "run: this.post.Runnable.run of getWidthHight");
				Log.i("宽度", ""+mWidth);
			}
		});
	}
	private void setimgsize(ImageView img){
		//设置图片宽高
		img.setLayoutParams(new ViewGroup.LayoutParams(mWidth, mHight));
		img.setImageResource(R.drawable.ba);
		img.setScaleType(ImageView.ScaleType.FIT_CENTER);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// 解决显示不全的问题
		int expandSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2, MeasureSpec.AT_MOST);
		super.onMeasure(widthMeasureSpec, expandSpec);
		Log.i(TAG, "run: onMeasure");
		Log.i("宽度", "makeMeasureSpec："+expandSpec+ "测量出来的width" + widthMeasureSpec);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
        paint.setColor(Color.RED);
        canvas.drawCircle(currentX,currentY,35,paint);
        paint.setColor(Color.BLUE);
        canvas.drawCircle(currentX+50,currentY+50,50,paint);
		canvas.drawBitmap(imgBoard, 0, 0, null);
		canvas.drawBitmap(imgPieces[3], 0, 0, null);
//
//		for (int x = Position.FILE_LEFT; x <= Position.FILE_RIGHT; x ++) {
//			for (int y = Position.RANK_TOP; y <= Position.RANK_BOTTOM; y ++) {
//				int sq = Position.COORD_XY(x, y);
//				sq = (flipped ? Position.SQUARE_FLIP(sq) : sq);
//				int xx = PIECE_MARGIN + (x - Position.FILE_LEFT) * SQUARE_SIZE;
//				int yy = PIECE_MARGIN + (y - Position.RANK_TOP) * SQUARE_SIZE;
//				int pc = pos.squares[sq];
//				if (pc > 0) {
//					canvas.drawBitmap(imgPieces[pc], xx, yy, null);
//				}
//				if (sq == sqSelected || sq == Position.SRC(mvLast) ||
//						sq == Position.DST(mvLast)) {
//					canvas.drawBitmap(imgSelected, xx, yy, null);
//				}
//			}
//		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent e){
		int bzcol,bzrow;
		int xSpan=0,sXtart=0,ySpan=0,sYtart=0;
		currentX=e.getX();
		currentX=e.getX();
		invalidate();
		return  true;

//		if(thinking)//如果正在进行电脑下棋
//		{
//			return false;
//		}
//
//		int col = (int)( (e.getX()-0)/50);
//		int row = (int) ((e.getY()-0)/50);
//		if(	((e.getX()-col*xSpan-sXtart)*(e.getX()-col*xSpan-sXtart)+
//				(e.getY()-row*ySpan-sYtart)*(e.getY()-row*ySpan-sYtart))<xSpan/2*xSpan/2)
//		{
//			bzcol=col-1;
//			bzrow=row-1;//看其在哪一个格子上
//		}
//		else if(((e.getX()-col*xSpan-sXtart)*(e.getX()-col*xSpan-sXtart)+
//				(e.getY()-(row+1)*ySpan-sYtart)*(e.getY()-(row+1)*ySpan-sYtart))<xSpan/2*xSpan/2)
//		{
//			bzcol=col-1;
//			bzrow=row;
//		}
//		else if(((e.getX()-(1+col)*xSpan-sXtart)*(e.getX()-(1+col)*xSpan-sXtart)+
//				(e.getY()-(row+1)*ySpan-sYtart)*(e.getY()-(row+1)*ySpan-sYtart))<xSpan/2*xSpan/2)
//		{
//			bzcol=col;
//			bzrow=row;
//		}
//		else if(
//				((e.getX()-(1+col)*xSpan-sXtart)*(e.getX()-(1+col)*xSpan-sXtart)+
//						(e.getY()-row*ySpan-sYtart)*(e.getY()-row*ySpan-sYtart))<xSpan/2*xSpan/2)
//		{
//			bzcol=col;
//			bzrow=row-1;
//		}
//
//		if (!thinking ) {
//			int x=0;// = Util.MIN_MAX(0, (e.getX() - PIECE_MARGIN) / SQUARE_SIZE, 8);
//			int y=0;// = Util.MIN_MAX(0, (e.getY() - PIECE_MARGIN) / SQUARE_SIZE, 9);
//			clickSquare(Position.COORD_XY(x + Position.FILE_LEFT, y + Position.RANK_TOP));
//		}
//
//		return  true;
	}
	void loadBoard() {
		imgBoard = BitmapFactory.decodeResource(getResources(), R.drawable.board);
	}

	void loadPieces() {
		imgSelected = BitmapFactory.decodeResource(getResources(), R.drawable.ba);;
		imgPieces=new Bitmap[]{//棋子
						BitmapFactory.decodeResource(getResources(), R.drawable.bk),
						BitmapFactory.decodeResource(getResources(), R.drawable.ba),
						BitmapFactory.decodeResource(getResources(), R.drawable.bb),
						BitmapFactory.decodeResource(getResources(), R.drawable.bn),
						BitmapFactory.decodeResource(getResources(), R.drawable.br),
						BitmapFactory.decodeResource(getResources(), R.drawable.bc),
						BitmapFactory.decodeResource(getResources(), R.drawable.bp),
						BitmapFactory.decodeResource(getResources(), R.drawable.rk),
						BitmapFactory.decodeResource(getResources(), R.drawable.ra),
						BitmapFactory.decodeResource(getResources(), R.drawable.rb),
						BitmapFactory.decodeResource(getResources(), R.drawable.rn),
						BitmapFactory.decodeResource(getResources(), R.drawable.rr),
						BitmapFactory.decodeResource(getResources(), R.drawable.rc),
						BitmapFactory.decodeResource(getResources(), R.drawable.rp),
				};
	}

	void clickSquare(int sq_) {
		int sq = (flipped ? Position.SQUARE_FLIP(sq_) : sq_);
		int pc = pos.squares[sq];
		if ((pc & Position.SIDE_TAG(pos.sdPlayer)) != 0) {
			if (sqSelected > 0) {
				drawSquare(sqSelected);
			}
			if (mvLast > 0) {
				drawMove(mvLast);
				mvLast = 0;
			}
			sqSelected = sq;
			drawSquare(sq);
			playSound(RESP_CLICK);
		} else if (sqSelected > 0) {
			int mv = Position.MOVE(sqSelected, sq);
			if (!pos.legalMove(mv)) {
				return;
			}
			if (!pos.makeMove(mv)) {
				playSound(RESP_ILLEGAL);
				return;
			}
			int response = pos.inCheck() ? RESP_CHECK :
					pos.captured() ? RESP_CAPTURE : RESP_MOVE;
			if (pos.captured()) {
				pos.setIrrev();
			}
			mvLast = mv;
			sqSelected = 0;
			drawMove(mv);
			playSound(response);
			if (!getResult()) {
				thinking();
			}
		}
	}

	void drawSquare(int sq_) {
		int sq = (flipped ? Position.SQUARE_FLIP(sq_) : sq_);
		int x = PIECE_MARGIN + (Position.FILE_X(sq) - Position.FILE_LEFT) * SQUARE_SIZE;
		int y = PIECE_MARGIN + (Position.RANK_Y(sq) - Position.RANK_TOP) * SQUARE_SIZE;
		//canvas.repaint(x, y, SQUARE_SIZE, SQUARE_SIZE);
	}

	void drawMove(int mv) {
		drawSquare(Position.SRC(mv));
		drawSquare(Position.DST(mv));
	}

	void playSound(int response) {
	}

	void thinking() {
		thinking = true;
		new Thread() {
			public void run() {
				//setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				int mv = mvLast;
				mvLast = search.searchMain(100 << (level << 1));
				pos.makeMove(mvLast);
				drawMove(mv);
				drawMove(mvLast);
				int response = pos.inCheck() ? RESP_CHECK2 :
						pos.captured() ? RESP_CAPTURE2 : RESP_MOVE2;
				if (pos.captured()) {
					pos.setIrrev();
				}
				//setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				getResult(response);
				thinking = false;
			}
		}.start();
	}

	/** Player Move Result */
	boolean getResult() {
		return getResult(-1);
	}

	/** Computer Move Result */
	boolean getResult(int response) {
		if (pos.isMate()) {
			playSound(response < 0 ? RESP_WIN : RESP_LOSS);
			//showMessage(response < 0 ? "祝贺你取得胜利！" : "请再接再厉！");
			return true;
		}
		int vlRep = pos.repStatus(3);
		if (vlRep > 0) {
			vlRep = (response < 0 ? pos.repValue(vlRep) : -pos.repValue(vlRep));
			playSound(vlRep > Position.WIN_VALUE ? RESP_LOSS :
					vlRep < -Position.WIN_VALUE ? RESP_WIN : RESP_DRAW);
			//showMessage(vlRep > Position.WIN_VALUE ? "长打作负，请不要气馁！" :
			//		vlRep < -Position.WIN_VALUE ? "电脑长打作负，祝贺你取得胜利！" : "双方不变作和，辛苦了！");
			return true;
		}
		if (pos.moveNum > 100) {
			playSound(RESP_DRAW);
			//showMessage("超过自然限着作和，辛苦了！");
			return true;
		}
		if (response >= 0) {
			playSound(response);
			retractFen = currentFen;
			currentFen = pos.toFen();
		}
		return false;
	}
}