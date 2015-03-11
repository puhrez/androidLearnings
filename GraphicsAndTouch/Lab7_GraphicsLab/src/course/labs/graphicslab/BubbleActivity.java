package course.labs.graphicslab;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

public class BubbleActivity extends Activity {

	// These variables are for testing purposes, do not modify
	private final static int RANDOM = 0;
	private final static int SINGLE = 1;
	private final static int STILL = 2;
	private static int speedMode = RANDOM;

	private static final String TAG = "Lab-Graphics";

	// The Main view
	private RelativeLayout mFrame;

	// Bubble image's bitmap
	private Bitmap mBitmap;

	// Display dimensions
	private int mDisplayWidth, mDisplayHeight;

	// Sound variables

	// AudioManager
	private AudioManager mAudioManager;
	// SoundPool
	private SoundPool mSoundPool;
	// ID for the bubble popping sound
	private int mSoundID;
	// Audio volume
	private float mStreamVolume;

	// Gesture Detector
	private GestureDetector mGestureDetector;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "created");
		setContentView(R.layout.main);

		// Set up user interface
		mFrame = (RelativeLayout) findViewById(R.id.frame);

		// Load basic bubble Bitmap
		mBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.b64);

	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "resumed");
		// Manage bubble popping sound
		// Use AudioManager.STREAM_MUSIC as stream type

		mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

		mStreamVolume = (float) mAudioManager
				.getStreamVolume(AudioManager.STREAM_MUSIC)
				/ mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

		mSoundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);

		mSoundPool.setOnLoadCompleteListener(new OnLoadCompleteListener() {
			public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
				Log.d(TAG, "loadComplete before gesture setup");
				setupGestureDetector();
				Log.d(TAG, "loadComplete after gesture setup");
			}
		});
		
		mSoundID = mSoundPool.load(this, R.raw.bubble_pop, 0);

	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus) {

			// Get the size of the display so this View knows where borders are
			mDisplayWidth = mFrame.getWidth();
			mDisplayHeight = mFrame.getHeight();

		}
	}

	// Set up GestureDetector
	private void setupGestureDetector() {
		Log.d(TAG, "setting up gesture");
		mGestureDetector = new GestureDetector(this,
		new GestureDetector.SimpleOnGestureListener() {

			// If a fling gesture starts on a BubbleView then change the
			// BubbleView's velocity

			@Override
			public boolean onFling(MotionEvent event1, MotionEvent event2,
					float velocityX, float velocityY) {

				Log.d(TAG, "fling");
				int count = mFrame.getChildCount();
				BubbleView child;
				for (int i = 0; i < count; i += 1) {
					child = (BubbleView) mFrame.getChildAt(i);
					if (child.intersects(event1.getRawX(), event1.getRawY())) {
						child.deflect(velocityX, velocityY);
						Log.d(TAG, "intesected true, returning");
						return true;
					}
				}
				
				
				Log.d(TAG, "intersected false, returning");
				
				return false;
			}


			@Override
			public boolean onSingleTapConfirmed(MotionEvent event) {


				BubbleView child;
				int count = mFrame.getChildCount();
				for (int i = 0; i < count; i += 1) {
					child = (BubbleView) mFrame.getChildAt(i);
					if (child.intersects(event.getRawX(), event.getRawY())) {
						child.stopMovement(true);
						Log.d(TAG, "popping");
						return true;
					}
				}
				
				child = new BubbleView(mFrame.getContext(),event.getRawX(), event.getRawY());
				
				
				Log.d(TAG, "adding");
				mFrame.addView(child);
				Log.d(TAG, "added");
				child.startMovement();
				
				
				return true;
			}
		});
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		if (mGestureDetector != null) {
			mGestureDetector.onTouchEvent(event);
		}

		return true;
		
	}

	@Override
	protected void onPause() {
		Log.d(TAG, "pausing");


		mSoundPool.release();
		
		mSoundPool = null;
		
		
		
		super.onPause();
	}

	// BubbleView is a View that displays a bubble.
	// This class handles animating, drawing, and popping amongst other actions.
	// A new BubbleView is created for each bubble on the display

	public class BubbleView extends View {

		private static final int BITMAP_SIZE = 64;
		private static final int REFRESH_RATE = 40;
		private final Paint mPainter = new Paint();
		private ScheduledFuture<?> mMoverFuture;
		private int mScaledBitmapWidth;
		private Bitmap mScaledBitmap;

		// location, speed and direction of the bubble
		private float mXPos, mYPos, mDx, mDy, mRadius, mRadiusSquared;
		private long mRotate, mDRotate;

		BubbleView(Context context, float x, float y) {
			super(context);
			Log.d(TAG, "new bubble");
			// Create a new random number generator to
			// randomize size, rotation, speed and direction
			Random r = new Random();

			// Creates the bubble bitmap for this BubbleView
			createScaledBitmap(r);

			// Radius of the Bitmap
			mRadius = mScaledBitmapWidth / 2;
			mRadiusSquared = mRadius * mRadius;
			
			// Adjust position to center the bubble under user's finger
			mXPos = x - mRadius;
			mYPos = y - mRadius;

			// Set the BubbleView's speed and direction
			setSpeedAndDirection(r);

			// Set the BubbleView's rotation
			setRotation(r);

			mPainter.setAntiAlias(true);

		}

		private void setRotation(Random r) {
			if (speedMode == RANDOM) {

				mDRotate = r.nextInt(4 - 1) + 1;

				
			} else {
				mDRotate = 0;
			}
		}

		private void setSpeedAndDirection(Random r) {

			// Used by test cases
			switch (speedMode) {

			case SINGLE:

				mDx = 20;
				mDy = 20;
				break;

			case STILL:

				// No speed
				mDx = 0;
				mDy = 0;
				break;

			default:
				mDx = r.nextInt((4 - -3)) + -3;
				
				mDy = r.nextInt((4 - -3)) + -3;
			}
		}
		private void createScaledBitmap(Random r) {

			if (speedMode != RANDOM) {
				mScaledBitmapWidth = BITMAP_SIZE * 3;
			} else {


				mScaledBitmapWidth = BITMAP_SIZE * (r.nextInt(4 - 1) + 1);

				
			}

			mScaledBitmap = Bitmap.createScaledBitmap(mBitmap, mScaledBitmapWidth, mScaledBitmapWidth, true);
		}

		// Start moving the BubbleView & updating the display
		private void startMovement() {

			// Creates a WorkerThread
			ScheduledExecutorService executor = Executors
					.newScheduledThreadPool(1);
			Log.d(TAG, "starting movement");

			// Execute the run() in Worker Thread every REFRESH_RATE
			// milliseconds
			// Save reference to this job in mMoverFuture
			mMoverFuture = executor.scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {
					//checking if off screen
					if (moveWhileOnScreen()) {
						stopMovement(false);
					}
					
					BubbleView.this.postInvalidate();
					
				}
			}, 0, REFRESH_RATE, TimeUnit.MILLISECONDS);
		}

		// Returns true if the BubbleView intersects position (x,y)
		private synchronized boolean intersects(float x, float y) {

			return  (
					(x > mXPos && x < mXPos + mScaledBitmapWidth)  &&
					(y > mYPos && y < mYPos + mScaledBitmapWidth)
			);

		}

		// Cancel the Bubble's movement
		// Remove Bubble from mFrame
		// Play pop sound if the BubbleView was popped

		private void stopMovement(final boolean wasPopped) {

			if (null != mMoverFuture) {

				if (!mMoverFuture.isDone()) {
					mMoverFuture.cancel(true);
				}
				Log.d(TAG, "stopping movement");

				// This work will be performed on the UI Thread
				mFrame.post(new Runnable() {
					@Override
					public void run() {
						mFrame.removeView(BubbleView.this);
						
						if (wasPopped) {

							mSoundPool.play(mSoundID, mStreamVolume, mStreamVolume, 0, 0, 1.0f);
							
						}
					}
				});
			}
		}

		// Change the Bubble's speed and direction
		private synchronized void deflect(float velocityX, float velocityY) {
			mDx = velocityX / REFRESH_RATE;
			mDy = velocityY / REFRESH_RATE;
		}

		// Draw the Bubble at its current location
		@Override
		protected synchronized void onDraw(Canvas canvas) {

			canvas.save();
			
			mRotate += mDRotate;

			canvas.rotate(mRotate, mXPos + mRadius, mYPos + mRadius);

			
			canvas.drawBitmap(mScaledBitmap, mXPos, mYPos, mPainter);
			
			canvas.restore();

			
		}

		// Returns true if the BubbleView is still on the screen after the move
		// operation
		private synchronized boolean moveWhileOnScreen() {


			mXPos += mDx;
			mYPos += mDy;
			
			
			return isOutOfView();

		}

		// Return true if the BubbleView is still on the screen after the move
		// operation
		private boolean isOutOfView() {

			
			return (
					(mXPos + mScaledBitmapWidth < 0 || mXPos > mDisplayWidth) || 
					(mYPos + mScaledBitmapWidth < 0 || mYPos > mDisplayHeight)
			);

		}
	}

	// Do not modify below here

	@Override
	public void onBackPressed() {
		openOptionsMenu();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		getMenuInflater().inflate(R.menu.menu, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_still_mode:
			speedMode = STILL;
			return true;
		case R.id.menu_single_speed:
			speedMode = SINGLE;
			return true;
		case R.id.menu_random_mode:
			speedMode = RANDOM;
			return true;
		case R.id.quit:
			exitRequested();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void exitRequested() {
		super.onBackPressed();
	}
}