/**
 * Rebound! The Fun Bouncing Bubble Game!
 * 
 * By Everett Lum and Jairam Patel
 * 
 * Bubbles bounce and you pop them when they reach zero. *You* set the difficulty! Aim for the high score!
 */

package course.labs.graphicsandanimationslab;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

public class BubbleActivity extends Activity {

	FrameLayout mFrame;
	Bitmap mBitmap;
	int score = 0;
	int level = 1;
	int lives = 3;
	int highScore = 0;

	static final int LEFT = 1;
	static final int RIGHT = 2;
	static final int UP = 3;
	static final int DOWN = 4;
	private GestureDetector mGestureDetector;
	Button addButton;

	Handler mHandler = new Handler()
	{
		public void handleMessage(Message msg)
		{
			if (score > highScore)
			{
				displayAlert("CONGRATS!", "Your new high score: " + score + "\nOld high score: " + highScore);
				highScore = score;
			}
			else
				displayAlert("YOU LOSE...", "High score: " + highScore + "\nYour score: " + score);

		}
	};


	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// Get references to UI elements
		mFrame = (FrameLayout) findViewById(R.id.frame);
		mFrame.setOnTouchListener(new OnTouchListener(){

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				for(int x =0;x < mFrame.getChildCount();x++)
				{
					BubbleView b = (BubbleView)mFrame.getChildAt(x);
					if(b.intersects(event.getX(), event.getY()) && b.bounces == 0 && lives > 0){
						Log.e("REBOUND", "INTERSECTS: " + x);
						b.stop(true);
						score += level;
						TextView scoreView = (TextView)findViewById(R.id.score);
						scoreView.setText("Score: " + score);
						addButton.setEnabled(false);
					}					
				}
				return false;
			}

		});
		mBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.b64);


		// Set up Add and Remove buttons
		addButton = (Button) findViewById(R.id.add_button);
		addButton.setOnClickListener(new OnClickListener() {

			// Create a new BubbleView
			// Add it to the mBubbleViews list
			// Manage RemoveButton

			public void onClick(View v) {
				int randomWidth = (int)(Math.random() * (mFrame.getWidth()/2) + 60);
				int randomHeight = (int)(Math.random() * (mFrame.getHeight()/2) + 60);
				BubbleView b = new BubbleView(BubbleActivity.this, mFrame.getWidth(), mFrame.getHeight(),((int)(Math.random()*level) + 1));
				mFrame.addView(b);
			}
		});
	}
	public void displayAlert(String title, String s) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(s).setTitle(title).setIcon(R.drawable.icon).setCancelable(false)
		.setNegativeButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}

	private class BubbleView extends View {
		// Base Bitmap Size
		private static final int BITMAP_SIZE = 64;

		// Animation refresh rate
		private static final int REFRESH_RATE = 15;

		// Log TAG
		private static final String TAG = "BubbleActivity";

		// Current top and left coordinates
		private float mX, mY;

		private Activity act;

		// Direction and speed of movement
		// measured in terms of how much the BubbleView moves
		// in one time step.
		private float mDx, mDy;

		// Height and width of the FrameLayout
		private int mDisplayWidth, mDisplayHeight;

		// Size of the BubbleView
		private int mScaledBitmapWidth;

		// Underlying Bitmap scaled to new size
		private final Bitmap mScaledBitmap;

		private final Paint mPainter = new Paint();

		// Reference to the movement calculation and update code
		private final ScheduledFuture<?> mMoverFuture;

		private int bounces;

		// context and width and height of the FrameLayout
		public BubbleView(Context context, int w, int h,int bounces) {

			super(context);

			act = (Activity) context;
			this.bounces = bounces;
			mDisplayWidth = w;
			mDisplayHeight = h;

			Log.i(TAG, "Display Dimensions: x:" + mDisplayWidth + " y:"
					+ mDisplayHeight);

			Random r = new Random();
			mPainter.setColor(Color.RED);
			mPainter.setTextSize(72);

			// Set BubbleView's size

			// mScaledBitmapWidth =
			mScaledBitmapWidth = (int)(mDisplayWidth * (Math.random()*.1 + .15));

			mScaledBitmap = Bitmap.createScaledBitmap(mBitmap,
					mScaledBitmapWidth, mScaledBitmapWidth, false);

			// Set initial location

			mX = (int)(Math.random() * (mDisplayWidth/2) + (mDisplayWidth/4));
			mY = (int)(Math.random() * (mDisplayHeight/2) + (mDisplayWidth/4));

			// Set movement direction and speed
			while(mDx == 0 && mDy == 0){
				mDx = (int)(Math.random() * level + 1);
				mDy = (int)(Math.random() * level + 1);
			}
			if (r.nextInt(2) == 0) mDx *= -1;
			if (r.nextInt(2) == 0) mDy *= -1;
			ScheduledExecutorService executor = Executors
					.newScheduledThreadPool(1);

			// The BubbleView's movement calculations & display update
			mMoverFuture = executor.scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {
					if(!isOutOfView()){
						moveUntilOffScreen();
						postInvalidate();
					}
					else{
						stop(false);
					}
				}
			}, 0, REFRESH_RATE, TimeUnit.MILLISECONDS);

			Log.i(TAG, "Bubble created at x:" + mX + " y:" + mY);
			Log.i(TAG, "Bubble direction is dx:" + mDx + " dy:" + mDy);
		}

		// Returns true is the BubbleView intersects position (x,y)
		private synchronized boolean intersects(float x, float y) {
			return x > mX && x < mX + mScaledBitmapWidth && y > mY
					&& y < mY + mScaledBitmapWidth;
		}
		private void stop(final boolean isPopped) {
			if (null != mMoverFuture && mMoverFuture.cancel(true)) {
				mFrame.post(new Runnable() {
					@Override
					public void run() {
						mFrame.removeView(BubbleView.this);
						if (!isPopped)
						{
							if (lives > 0)
							{
								lives--;
								TextView liveDisplay = (TextView)act.findViewById(R.id.lives);
								liveDisplay.setText("Lives: " + lives);
								if (lives >= 0)
									Toast.makeText(getApplicationContext(), "Bubble missed!",Toast.LENGTH_SHORT).show();
								if (lives == 0)
								{
									mHandler.post(new Runnable() {

										@Override
										public void run() {
											if (score > highScore)
											{
												displayAlert("CONGRATS!", "Your new high score: " + score + "\nOld high score: " + highScore);
												highScore = score;
											}
											else
												displayAlert("YOU LOSE...", "High score: " + highScore + "\nYour score: " + score);
										}

									});
								}
							}

						}
						if (mFrame.getChildCount() == 0)
						{
							if (lives > 0)
							{
								level++;
								mHandler.post(new Runnable() {

									@Override
									public void run() {
										addButton.setEnabled(true);
										displayAlert("Level up!", "Level " + level);
									}

								});
							}
							else
							{
								addButton.setEnabled(true);
								for (int i = 0; i < mFrame.getChildCount(); i++)
								{
									BubbleView bv = (BubbleView)mFrame.getChildAt(i);
									bv.mDx *= 10;
									bv.mDy *= 10;
								}
								score = 0;
								level = 1;
								lives = 3;
								TextView liveDisplay = (TextView)act.findViewById(R.id.lives);
								liveDisplay.setText("Lives: " + lives);
								TextView scoreView = (TextView)act.findViewById(R.id.score);
								scoreView.setText("Score: " + score);
							}
						}
					}
				});
			} else {
				Log.e(TAG, "failed to cancel mMoverFuture:" + this);
			}
		}

		// moves the BubbleView
		// returns true if the BubbleView has exited the screen
		private boolean moveUntilOffScreen() {                        

			int result = whereBouncing();

			if (bounces > 0)
			{
				if (result > 0)
				{
					switch (result)
					{
					case LEFT:
					case RIGHT:
						mDx *= -1;
						break;
					case UP:
					case DOWN:
						mDy *= -1;
						break;
					}
					bounces--;
					if (bounces == 0)
						mPainter.setColor(Color.GREEN);
				}
			}
			mX += mDx;
			mY += mDy;

			return false;
		}

		// returns true if the BubbleView has completely left the screen
		private boolean isOutOfView() {
			return mX < 0 - mScaledBitmapWidth || mX > mDisplayWidth
					|| mY < 0 - mScaledBitmapWidth || mY > mDisplayHeight;
		}
		private int whereBouncing() {
			if (mX < 0)
				return LEFT;
			else if (mX > mDisplayWidth - mScaledBitmapWidth)
				return RIGHT;
			else if (mY < 0)
				return UP;
			else if (mY > mDisplayHeight - mScaledBitmapWidth)
				return DOWN;
			else
				return 0;
		}

		// Draws the scaled Bitmap
		@Override
		protected void onDraw(Canvas canvas) {
			canvas.drawBitmap(mScaledBitmap, mX, mY, mPainter);
			canvas.drawText(bounces + "", mX + (mScaledBitmapWidth/3), mY + (mScaledBitmapWidth/2), mPainter);
			//canvas.drawBitmap(mScaledBitmap, getMatrix(), mPainter);
		}
	}
}