package com.example.pineapple;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MenuPanel extends SurfaceView implements SurfaceHolder.Callback{
	private final String TAG = MenuPanel.class.getSimpleName();
	public final static String LEVEL = "com.example.joarattacks.LEVEL";
	private double scaleX, scaleY;
	private Button playButton, settingsButton, highscoreButton;
	private MainThread thread;
	private Bitmap backgroundBitmap;
	private Context context;
	private Protagonist protagonist;
	private Matrix renderMatrix;
	private Bitmap bodyBitmap, weaponBitmap, footBitmap, pupilBitmap, eyeMouthBitmap;
	private Bitmap bodyBitmapFlipped, weaponBitmapFlipped, footBitmapFlipped, pupilBitmapFlipped, eyeMouthBitmapFlipped;
	private boolean play = false;
	private int nextLevel;
	
	public MenuPanel(Context context) {
		super(context);
		getHolder().addCallback(this);
		setFocusable(true);
		thread = new MainThread(this.getHolder(), this);
		this.context = context;
		protagonist = new Protagonist(-20, 80);
		renderMatrix = new Matrix();
		setKeepScreenOn(true);
	}
	
	public void update(){
		if(protagonist.getXPos() < 100 || play){
			protagonist.accelerate(0.3);
			protagonist.step(1);
		} else {
			protagonist.slowDown();
			protagonist.setStepCount(0);
		}
		if(protagonist.getXPos() > 160){
			goToGame(nextLevel);
		}
		protagonist.breathe();
		protagonist.move();
	}
	
	public void render(Canvas canvas){
		canvas.drawBitmap(backgroundBitmap, 0, 0, null);
		canvas.drawBitmap(playButton.getBitmap(), (float)(playButton.getX()*scaleX), (float)(playButton.getY()*scaleY), null);
		canvas.drawBitmap(settingsButton.getBitmap(), (float)(settingsButton.getX()*scaleX), (float)(settingsButton.getY()*scaleY), null);
		canvas.drawBitmap(highscoreButton.getBitmap(), (float)(highscoreButton.getX()*scaleX), (float)(highscoreButton.getY()*scaleY), null);
		renderProtagonist(canvas);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.d(TAG, "Hit");
		
		scaleY = (double)getHeight()/100;
		scaleX = (double)getWidth()/155;
		
		Bitmap playBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.start), (int)(Const.menuButtonWidth*scaleX), (int)(Const.menuButtonHeight*scaleY), true);
		playButton = new Button(10, 10, playBitmap);
		
		Bitmap settingsBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.settings), (int)(Const.menuButtonWidth*scaleX), (int)(Const.menuButtonHeight*scaleY), true);
		settingsButton = new Button(10, 30, settingsBitmap);
		
		Bitmap highscoreBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.highscore), (int)(Const.menuButtonWidth*scaleX), (int)(Const.menuButtonHeight*scaleY), true);
		highscoreButton = new Button(10, 50, highscoreBitmap);
		
		backgroundBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.menu_background), (int)(155*scaleX), (int)(100*scaleY), true);
		
		bodyBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.valentine_in_game_90_body), (int)(protagonist.getWidth()*scaleX*Const.bodyXScale), (int)(protagonist.getHeight()*scaleY*Const.bodyYScale), true);
		eyeMouthBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.valentine_in_game_90_eye_mouth), (int)(protagonist.getWidth()*scaleX*Const.eyeMouthXScale), (int)(protagonist.getHeight()*scaleY*Const.eyeMouthYScale), true);
		footBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.valentine_in_game_90_foot), (int)(protagonist.getWidth()*scaleX*Const.footXScale), (int)(protagonist.getHeight()*scaleY*Const.footYScale), true);
		weaponBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.valentine_in_game_90_hand_gun), (int)(protagonist.getWidth()*scaleX*Const.weaponXScale), (int)(protagonist.getHeight()*scaleY*Const.weaponYScale), true);
		pupilBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.valentine_in_game_90_pupil), (int)(protagonist.getWidth()*scaleX*Const.pupilXScale), (int)(protagonist.getHeight()*scaleY*Const.pupilYScale), true);
		
		//Flip images
		renderMatrix.setScale(-1, 1);
		bodyBitmapFlipped = Bitmap.createBitmap(bodyBitmap, 0, 0, bodyBitmap.getWidth(), bodyBitmap.getHeight(), renderMatrix,false);
		eyeMouthBitmapFlipped = Bitmap.createBitmap(eyeMouthBitmap, 0, 0, eyeMouthBitmap.getWidth(), eyeMouthBitmap.getHeight(), renderMatrix,false);
		footBitmapFlipped = Bitmap.createBitmap(footBitmap, 0, 0, footBitmap.getWidth(), footBitmap.getHeight(), renderMatrix,false);
		weaponBitmapFlipped = Bitmap.createBitmap(weaponBitmap, 0, 0, weaponBitmap.getWidth(), weaponBitmap.getHeight(), renderMatrix,false);
		pupilBitmapFlipped = Bitmap.createBitmap(pupilBitmap, 0, 0, pupilBitmap.getWidth(), pupilBitmap.getHeight(), renderMatrix,false);
		
		thread.setRunning(true);
		try{thread.start();} catch(IllegalThreadStateException e){}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		//End the thread
		boolean retry = true;
		while(retry){
			try{
				thread.join();
				retry = false;
			} catch(InterruptedException e){

			}
		}
		
	}
	
	//Pause the game
	public void pause(){
		thread.setRunning(false);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent e){
		if(e.getAction() == MotionEvent.ACTION_DOWN){
			int x = (int)(e.getX()/scaleX);
			int y = (int)(e.getY()/scaleY);
			if(playButton.isClicked(x, y)){
				nextLevel = 0;
				play = true;
			}
		}
		return true;
	}
	
	public void goToGame(int level){
		Intent intent = new Intent(context, GameActivity.class);
		intent.putExtra(LEVEL, level);
		context.startActivity(intent);
	}
	
	public void renderProtagonist(Canvas canvas){
		/*Paint p = new Paint();
		p.setColor(Color.BLUE);
		canvas.drawRect((float)((protagonist.getXPos()-protagonist.getWidth()/2)*scaleX), (float)((protagonist.getYPos()-protagonist.getHeight()/2)*scaleY), (float)((protagonist.getXPos()+protagonist.getWidth()/2)*scaleX), (float)((protagonist.getYPos()+protagonist.getHeight()/2)*scaleY), p);*/
		float aimAngle = 0;
		float feetAngle = (float)(180/Math.PI*Math.sin((double)protagonist.getStepCount()/protagonist.getNumberOfSteps()*Math.PI));
		//Draw all the protagonist parts

		if(protagonist.isFacingRight()){
			//Draw back foot
			renderMatrix.setRotate(-feetAngle, footBitmap.getWidth()/2, footBitmap.getHeight()/2);
			renderMatrix.postTranslate((float)((protagonist.getXPos() - protagonist.getWidth()*(0.5-Const.footXAxis-Const.backFootOffset) - protagonist.getWidth()*Const.footRadius*Math.sin(-feetAngle*Math.PI/180) )*scaleX), (float)((protagonist.getYPos() + protagonist.getHeight()*(Const.footYAxis-0.5) + protagonist.getHeight()*Const.footRadius*Math.cos(-feetAngle*Math.PI/180) )*scaleY));
			canvas.drawBitmap(footBitmap, renderMatrix, null);
			//Draw body
			renderMatrix.setTranslate((float)((protagonist.getXPos() - protagonist.getWidth()*(0.5-Const.bodyXOffset) )*scaleX), (float)((protagonist.getYPos() - protagonist.getHeight()*(0.5-Const.bodyYOffset + Const.breathOffset*Math.sin((double)protagonist.getBreathCount()/protagonist.getBreathMax()*2*Math.PI)) )*scaleY));
			canvas.drawBitmap(bodyBitmap, renderMatrix, null);
			//Draw eyes and mouth
			renderMatrix.setTranslate((float)((protagonist.getXPos() - protagonist.getWidth()*(0.5-Const.eyeMouthXOffset) )*scaleX), (float)((protagonist.getYPos() - protagonist.getHeight()*(0.5-Const.eyeMouthYOffset) )*scaleY));
			canvas.drawBitmap(eyeMouthBitmap, renderMatrix, null);
			//Draw front foot
			renderMatrix.setRotate(feetAngle, footBitmap.getWidth()/2, footBitmap.getHeight()/2);
			renderMatrix.postTranslate((float)((protagonist.getXPos() - protagonist.getWidth()*(0.5-Const.footXAxis) - protagonist.getWidth()*Const.footRadius*Math.sin(feetAngle*Math.PI/180) )*scaleX), (float)((protagonist.getYPos() + protagonist.getHeight()*(Const.footYAxis-0.5) + protagonist.getHeight()*Const.footRadius*Math.cos(feetAngle*Math.PI/180) )*scaleY));
			canvas.drawBitmap(footBitmap, renderMatrix, null);
			//Draw pupils
			renderMatrix.setTranslate((float)((protagonist.getXPos() + protagonist.getWidth()*(Const.pupilXOffset-0.5)+protagonist.getWidth()*Const.pupilRadius*Math.cos(aimAngle*Math.PI/180))*scaleX), (float)((protagonist.getYPos() + protagonist.getHeight()*(Const.pupilYOffset-0.5)-protagonist.getHeight()*Const.pupilRadius*Math.sin(aimAngle*Math.PI/180) )*scaleY));
			canvas.drawBitmap(pupilBitmap, renderMatrix, null);
			//Draw weapon
			renderMatrix.setRotate(-aimAngle, weaponBitmap.getWidth()/2, weaponBitmap.getHeight()/2);
			renderMatrix.postTranslate((float)((protagonist.getXPos() - protagonist.getWidth()*(0.5-Const.weaponXAxis) + protagonist.getWidth()*Const.weaponRadius*Math.cos(aimAngle*Math.PI/180) )*scaleX), (float)((protagonist.getYPos() + protagonist.getHeight()*(Const.weaponYAxis-0.5) - protagonist.getHeight()*Const.weaponRadius*Math.sin(aimAngle*Math.PI/180) )*scaleY));
			canvas.drawBitmap(weaponBitmap, renderMatrix, null);
		} else {
			//Draw back foot
			renderMatrix.setRotate(feetAngle, footBitmapFlipped.getWidth()/2, footBitmapFlipped.getHeight()/2);
			renderMatrix.postTranslate((float)((protagonist.getXPos() - protagonist.getWidth()*(0.5-Const.footXAxis+Const.backFootOffset) - protagonist.getWidth()*Const.footRadius*Math.sin(Math.PI-feetAngle*Math.PI/180) )*scaleX), (float)((protagonist.getYPos() + protagonist.getHeight()*(Const.footYAxis-0.5) + protagonist.getHeight()*Const.footRadius*Math.cos(-feetAngle*Math.PI/180) )*scaleY));
			canvas.drawBitmap(footBitmapFlipped, renderMatrix, null);
			//Draw body
			renderMatrix.setTranslate((float)((protagonist.getXPos() + protagonist.getWidth()*(0.5-Const.bodyXOffset) )*scaleX) - bodyBitmapFlipped.getWidth(), (float)((protagonist.getYPos() - protagonist.getHeight()*(0.5-Const.bodyYOffset + Const.breathOffset*Math.sin((double)protagonist.getBreathCount()/protagonist.getBreathMax()*2*Math.PI)) )*scaleY));
			canvas.drawBitmap(bodyBitmapFlipped, renderMatrix, null);
			//Draw eyes and mouth
			renderMatrix.setTranslate((float)((protagonist.getXPos() + protagonist.getWidth()*(0.5-Const.eyeMouthXOffset) )*scaleX) - eyeMouthBitmapFlipped.getWidth(), (float)((protagonist.getYPos() - protagonist.getHeight()*(0.5-Const.eyeMouthYOffset) )*scaleY));
			canvas.drawBitmap(eyeMouthBitmapFlipped, renderMatrix, null);
			//Draw front foot
			renderMatrix.setRotate(-feetAngle, footBitmapFlipped.getWidth()/2, footBitmapFlipped.getHeight()/2);
			renderMatrix.postTranslate((float)((protagonist.getXPos() - protagonist.getWidth()*(0.5-Const.footXAxis) - protagonist.getWidth()*Const.footRadius*Math.sin(Math.PI+feetAngle*Math.PI/180) )*scaleX), (float)((protagonist.getYPos() + protagonist.getHeight()*(Const.footYAxis-0.5) + protagonist.getHeight()*Const.footRadius*Math.cos(feetAngle*Math.PI/180) )*scaleY));
			canvas.drawBitmap(footBitmapFlipped, renderMatrix, null);
			//Draw pupils
			renderMatrix.setTranslate((float)((protagonist.getXPos() + protagonist.getWidth()*(Const.pupilXOffset-0.5)+protagonist.getWidth()*Const.pupilRadius*Math.cos(aimAngle*Math.PI/180))*scaleX), (float)((protagonist.getYPos() + protagonist.getHeight()*(Const.pupilYOffset-0.5)-protagonist.getHeight()*Const.pupilRadius*Math.sin(aimAngle*Math.PI/180) )*scaleY));
			canvas.drawBitmap(pupilBitmapFlipped, renderMatrix, null);
			//Draw weapon
			renderMatrix.setRotate(180-aimAngle, weaponBitmapFlipped.getWidth()/2, weaponBitmapFlipped.getHeight()/2);
			renderMatrix.postTranslate((float)((protagonist.getXPos()  + protagonist.getWidth()*(0.5-Const.weaponXAxis) + protagonist.getWidth()*Const.weaponRadius*Math.cos(aimAngle*Math.PI/180) )*scaleX - weaponBitmapFlipped.getWidth()), (float)((protagonist.getYPos() + protagonist.getHeight()*(Const.weaponYAxis-0.5) + protagonist.getHeight()*Const.weaponRadius*Math.sin(Math.PI+aimAngle*Math.PI/180) )*scaleY));
			canvas.drawBitmap(weaponBitmapFlipped, renderMatrix, null);
		}


	}
	
	


}
