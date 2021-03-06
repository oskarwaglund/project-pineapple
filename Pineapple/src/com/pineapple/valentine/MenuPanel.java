package com.pineapple.valentine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

import com.pineapple.valentineGen.R;
import com.scoreloop.client.android.core.controller.RequestController;
import com.scoreloop.client.android.core.controller.RequestControllerException;
import com.scoreloop.client.android.core.controller.RequestControllerObserver;
import com.scoreloop.client.android.core.controller.ScoreController;
import com.scoreloop.client.android.core.controller.ScoresController;
import com.scoreloop.client.android.core.controller.TermsOfServiceController;
import com.scoreloop.client.android.core.controller.TermsOfServiceControllerObserver;
import com.scoreloop.client.android.core.controller.UserController;
import com.scoreloop.client.android.core.model.Score;
import com.scoreloop.client.android.core.model.Session;
import com.scoreloop.client.android.core.model.User;

public class MenuPanel extends SurfaceView implements SurfaceHolder.Callback{
	private static final String TAG = MenuPanel.class.getSimpleName();
	public final static String LEVEL = "com.pineapple.valentineGen.LEVEL";
	private final int MAIN_MENU = 0;
	private final int LEVEL_MENU = 1;
	private final int SETTINGS_MENU = 2;
	private final int HIGHSCORE_MENU = 3;
	private final int LEFT = 0, RIGHT = 1, UP = 2, DOWN = 3, UPDATE = 4;
	private final int PLAY = 4;
	private final int width = 155, height = 100;
	private double scaleX, scaleY;
	private Button playButton, settingsButton, highscoreButton, soundButton, musicButton, scoreButton, setNameButton, difficultyButton;
	private Button[] leaderboardButtons;
	private MainThread thread;
	private Bitmap backgroundBitmap;
	private Context context;
	private Protagonist protagonist;
	private Matrix renderMatrix;
	private Bitmap bodyBitmap, weaponBitmap, footBitmap, pupilBitmap, eyeMouthBitmap;
	private Bitmap bodyBitmapFlipped, weaponBitmapFlipped, footBitmapFlipped, pupilBitmapFlipped, eyeMouthBitmapFlipped;
	private Button[] levelButtons;
	private Slider musicSlider, soundSlider;
	private Bitmap[] levelBitmaps;
	private Bitmap[] butterflyBitmaps;
	private Bitmap sliderLineBitmap, sliderHandleBitmap;
	private Bitmap onBitmap, offBitmap, updateBitmap;
	private int nextLevel;
	private int menuState;
	private SoundManager sm;
	private MediaPlayer theme;
	private int currentLevel;
	private int touchX, touchY;
	private int[] desiredX = new int[]{30, 60, 100, 120, 190};
	private SharedPreferences settings, localScores;
	private Butterfly butterfly;
	private float aimAngle, feetAngle;
	private int time = 0;
	private Paint userPaint, leaderboardPaint, loadPaint;
	private LevelBriefer briefer;
	private boolean loading = true;

	private final TermsOfServiceController controller;
	private static String userName;
	private static ArrayList<List<Score>> highScoreList= new ArrayList<List<Score>>();
	private static int currentHighScoreMode = 0;
	private static int leaderboardLevel = 0, leaderboardDifficulty = 0, leaderboardPage = 0;
	private static int loaderRotation = 0;
	private static final int maxPages = 3, scoresPerPage = 15;
	private Paint textBackground = new Paint();
	private static int space = 2;
	private boolean sync = false;
	private int updateSession = 0;

	public MenuPanel(Context context) {
		super(context);
		getHolder().addCallback(this);
		setFocusable(true);
		this.context = context;
		sm = new SoundManager(context, 1);
		protagonist = new Protagonist(-20, 80);
		butterfly = new Butterfly(70, 73);
		renderMatrix = new Matrix();
		setKeepScreenOn(true);



		settings = context.getSharedPreferences("gameSettings", Context.MODE_PRIVATE);
		localScores = context.getSharedPreferences("localScores", Context.MODE_PRIVATE);
		currentLevel = settings.getInt("currentLevel", 0);


		userName = (settings.getString("userName", null) == null)?"loading...":settings.getString("userName", null);

		userPaint = new Paint();
		userPaint.setColor(Color.WHITE);

		loadPaint = new Paint();
		loadPaint.setColor(Color.WHITE);
		leaderboardPaint = new Paint();
		leaderboardPaint.setColor(Color.WHITE);

		textBackground.setARGB(120, 40, 40, 40);



		//controller to handle the user's acceptance of the Terms Of Service
		controller = new TermsOfServiceController(new TermsOfServiceControllerObserver() {
			@Override
			public void termsOfServiceControllerDidFinish(final TermsOfServiceController controller, final Boolean accepted) {
				if(accepted != null) {
					Editor e = settings.edit();
					e.putBoolean("TOSAccepted", accepted);
					e.commit();
				}
			}
		});

		// first, a request observer...
		RequestControllerObserver observer = new RequestControllerObserver() {

			@Override
			public void requestControllerDidReceiveResponse(RequestController requestController) {
				UserController userController = (UserController)requestController;
				// insert values into text fields
				User user = userController.getUser();
				setUserName(user.getLogin());

			}

			@Override
			public void requestControllerDidFail(RequestController aRequestController, Exception anException) {

			}
		};

		// here's the UserController doing our work to update the profile data
		UserController userController = new UserController(observer);

		// and fire the request
		userController.loadUser();
		thread = new MainThread(this.getHolder(), this);
		//Start the thread
		if (thread.getState()==Thread.State.TERMINATED) { 
			thread = new MainThread(getHolder(),this);

		}
		thread.setRunning(true);
		try{thread.start();} catch(IllegalThreadStateException err){}
	}

	public void update(){
		if(!loading){
			if(Math.abs(protagonist.getXPos() - desiredX[menuState]) > 10){
				protagonist.accelerate(0.3*Math.signum(desiredX[menuState]-protagonist.getXPos()));
				protagonist.faceDirection((int)Math.signum(desiredX[menuState]-protagonist.getXPos()));
				protagonist.setAim(90 - (int)Math.signum(desiredX[menuState]-protagonist.getXPos())*90);
				protagonist.step(1);
			} else {
				protagonist.slowDown();
				protagonist.setStepCount(0);
				protagonist.faceDirection((int)Math.signum(butterfly.getX()-protagonist.getXPos()));
				protagonist.setAim((int)(180/Math.PI * Math.atan2(butterfly.getY() - protagonist.getYPos(), butterfly.getX() - protagonist.getXPos())));
			}
			if(protagonist.getXPos() > 160){
				goToGame(nextLevel);
			}
			protagonist.breathe();
			protagonist.move();
			butterfly.update();
			briefer.update();
			if(menuState == SETTINGS_MENU){
				setVolumes();
			}

			time++;
		}
	}

	public void playTheme(){
		if(theme == null){ //If theme is null
			try {
				loadTheme();
				theme.prepare();
			} catch(IOException e){	}
			theme.setLooping(true);
			theme.setVolume(settings.getFloat("musicVolume", 1), settings.getFloat("musicVolume", 1));
			theme.start();
		}
		else if(!theme.isPlaying()){ //If theme isn't already playing, reset before loading
			try {
				theme.reset();
				loadTheme();
				theme.prepare();
			} catch(IOException e){	}
			theme.setLooping(true);
			theme.setVolume(settings.getFloat("musicVolume", 1), settings.getFloat("musicVolume", 1));
			theme.start();
		}
	}

	public void stopTheme(){
		if(theme.isPlaying()){
			try {
				theme.stop();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} 

		}
	}

	public void loadSounds(){
		sm = new SoundManager(getContext());
		sm.addSound(0, R.raw.fire_sound);
	}

	public void loadTheme(){
		theme = MediaPlayer.create(getContext(), R.raw.short_instrumental);
		try {
			theme.prepare();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		theme.setVolume(settings.getFloat("musicVolume", 0), settings.getFloat("musicVolume", 0));
	}

	public void setVolumes(){
		if(musicSlider.isSliding()){
			Editor e = settings.edit();
			e.putFloat("musicVolume", (float)musicSlider.getValue());
			e.commit();
			theme.setVolume((float)musicSlider.getValue(), (float)musicSlider.getValue());
		}
		if(soundSlider.isSliding()){
			Editor e = settings.edit();
			e.putFloat("soundVolume", (float)soundSlider.getValue());
			e.commit();
			sm.playSound(0, settings.getFloat("soundVolume", 0));
		}
	}

	public void render(Canvas canvas){
		if(loading){
			canvas.drawColor(Color.BLACK);
			canvas.drawText("Loading...", 0, 20, loadPaint);
		} else {
			canvas.drawBitmap(backgroundBitmap, 0, 0, null);
			renderButterfly(canvas);
			renderProtagonist(canvas);
			renderLeaderboards(canvas);
			renderButtons(canvas);
			renderSliders(canvas);
			renderBriefer(canvas);
		}
	}

	public void renderButtons(Canvas canvas){
		switch(menuState){
		case MAIN_MENU:
			canvas.drawBitmap(playButton.getBitmap(), (float)(playButton.getX()*scaleX), (float)(playButton.getY()*scaleY), null);
			canvas.drawBitmap(settingsButton.getBitmap(), (float)(settingsButton.getX()*scaleX), (float)(settingsButton.getY()*scaleY), null);
			canvas.drawBitmap(highscoreButton.getBitmap(), (float)(highscoreButton.getX()*scaleX), (float)(highscoreButton.getY()*scaleY), null);
			break;
		case LEVEL_MENU:
			for(Button b: levelButtons){
				canvas.drawBitmap(b.getBitmap(), (float)(b.getX()*scaleX), (float)(b.getY()*scaleY), null);
			}
			break;
		case SETTINGS_MENU:
			canvas.drawBitmap(soundButton.getBitmap(), (float)(soundButton.getX()*scaleX), (float)(soundButton.getY()*scaleY), null);
			canvas.drawBitmap(musicButton.getBitmap(), (float)(musicButton.getX()*scaleX), (float)(musicButton.getY()*scaleY), null);
			canvas.drawBitmap(scoreButton.getBitmap(), (float)(scoreButton.getX()*scaleX), (float)(scoreButton.getY()*scaleY), null);
			//canvas.drawBitmap(difficultyButton.getBitmap(), (float)(difficultyButton.getX()*scaleX), (float)(difficultyButton.getY()*scaleY), null);
			canvas.drawBitmap(setNameButton.getBitmap(), (float)(setNameButton.getX()*scaleX), (float)(setNameButton.getY()*scaleY), null);
			if(settings.getBoolean("scoring", true)){
				canvas.drawBitmap(onBitmap, (float)((scoreButton.getX()+Const.HUDPadding+scoreButton.getWidth())*scaleX), (float)(scoreButton.getY()*scaleY), null);
			}  else {
				canvas.drawBitmap(offBitmap, (float)((scoreButton.getX()+Const.HUDPadding+scoreButton.getWidth())*scaleX), (float)(scoreButton.getY()*scaleY), null);
			}
			break;
		case HIGHSCORE_MENU:

			for(int i = 0; i < leaderboardButtons.length; i++){
				canvas.drawBitmap(leaderboardButtons[i].getBitmap(), (float)(leaderboardButtons[i].getX()*scaleX), (float)(leaderboardButtons[i].getY()*scaleY), null);
			}

			break;
		}

	}

	public void renderSliders(Canvas canvas){
		if(menuState == SETTINGS_MENU){
			canvas.drawBitmap(sliderLineBitmap, (float)(musicSlider.getX()*scaleX), (float)(musicSlider.getY()*scaleY), null);
			canvas.drawBitmap(sliderHandleBitmap, (float)((musicSlider.getX() + musicSlider.getWidth()*musicSlider.getValue()-Const.sliderHandleWidth/2)*scaleX), (float)((musicSlider.getY()+Const.menuButtonHeight/4*Math.sin(time/45.*Math.PI))*scaleY), null);

			canvas.drawBitmap(sliderLineBitmap, (float)(soundSlider.getX()*scaleX), (float)(soundSlider.getY()*scaleY), null);
			canvas.drawBitmap(sliderHandleBitmap, (float)((soundSlider.getX() + soundSlider.getWidth()*soundSlider.getValue()-Const.sliderHandleWidth/2)*scaleX), (float)((soundSlider.getY()+Const.menuButtonHeight/4*Math.cos(time/45.*Math.PI))*scaleY), null);
		}
	}

	public void renderUser(Canvas canvas){
		canvas.drawText("User: " + userName, (float)((Const.HUDPadding/2*scaleX)), (float)((height-Const.HUDPadding/2)*scaleY), userPaint);
	}

	public void renderLeaderboards(Canvas canvas){

		if(menuState == HIGHSCORE_MENU){
			canvas.drawRect(0, 0, (float)(width*scaleX), (float)(height*scaleY), textBackground);

			String levelS;
			if(leaderboardLevel == 0){
				levelS = "tutorial";
			} else if(leaderboardLevel == 11){
				levelS = "boss";
			} else {
				levelS = "level " + leaderboardLevel;
			}
			String leaderboardTitle = "Leaderboard for " + levelS;

			canvas.drawText(leaderboardTitle, (float)(Const.leaderboardColumns[0]*scaleX), (float)(Const.leaderboardStartY/2*scaleY), leaderboardPaint);

			//Draw column heads
			canvas.drawText("#", (float)(Const.leaderboardColumns[0]*scaleX), (float)(Const.leaderboardStartY*scaleY), leaderboardPaint);
			canvas.drawText("Name", (float)(Const.leaderboardColumns[1]*scaleX), (float)(Const.leaderboardStartY*scaleY), leaderboardPaint);
			canvas.drawText("Score", (float)(Const.leaderboardColumns[2]*scaleX), (float)(Const.leaderboardStartY*scaleY), leaderboardPaint);
			canvas.drawText("Drones", (float)(Const.leaderboardColumns[3]*scaleX), (float)(Const.leaderboardStartY*scaleY), leaderboardPaint);
			canvas.drawText("Ninjas", (float)(Const.leaderboardColumns[4]*scaleX), (float)(Const.leaderboardStartY*scaleY), leaderboardPaint);
			canvas.drawText("Tanks", (float)(Const.leaderboardColumns[5]*scaleX), (float)(Const.leaderboardStartY*scaleY), leaderboardPaint);
			canvas.drawText("Time", (float)(Const.leaderboardColumns[6]*scaleX), (float)(Const.leaderboardStartY*scaleY), leaderboardPaint);
			canvas.drawText("Health", (float)(Const.leaderboardColumns[7]*scaleX), (float)(Const.leaderboardStartY*scaleY), leaderboardPaint);
			canvas.drawLine(0, (float)(Const.leaderboardStartY*scaleY), (float)(width*scaleX), (float)(Const.leaderboardStartY*scaleY), leaderboardPaint);
			int i = 0;
			try{ //Try to render the leaderboard

				if(highScoreList.size() >= leaderboardLevel){
					for(i = 0; leaderboardPage*scoresPerPage+i < highScoreList.get(leaderboardLevel).size(); i++){
						Score s = highScoreList.get(leaderboardLevel).get(leaderboardPage*scoresPerPage+i);
						String health;
						if(s.getUser().equals(Session.getCurrentSession().getUser())){
							leaderboardPaint.setColor(Color.YELLOW);
						}
						health = s.getContext().get("health")+"%";

						//Format time string
						int sec = Integer.parseInt(s.getContext().get("secs").toString());
						int min = Integer.parseInt(s.getContext().get("mins").toString());

						String mins = ((min < 10)?"0"+min:""+min);
						String secs = ((sec < 10)?"0"+sec:""+sec);

						canvas.drawText(s.getRank()+". ", (float)(Const.leaderboardColumns[0]*scaleX), (float)(Const.leaderboardStartY*scaleY+(i+1)*(height-Const.leaderboardStartY)/Const.leaderboardRows*scaleY), leaderboardPaint);
						canvas.drawText(s.getUser().getLogin(), (float)(Const.leaderboardColumns[1]*scaleX), (float)(Const.leaderboardStartY*scaleY+(i+1)*(height-Const.leaderboardStartY)/Const.leaderboardRows*scaleY), leaderboardPaint);
						canvas.drawText(s.getResult().intValue()+"", (float)(Const.leaderboardColumns[2]*scaleX), (float)(Const.leaderboardStartY*scaleY+(i+1)*(height-Const.leaderboardStartY)/Const.leaderboardRows*scaleY), leaderboardPaint);
						canvas.drawText(s.getContext().get("normals")+"", (float)(Const.leaderboardColumns[3]*scaleX), (float)(Const.leaderboardStartY*scaleY+(i+1)*(height-Const.leaderboardStartY)/Const.leaderboardRows*scaleY), leaderboardPaint);
						canvas.drawText(s.getContext().get("ninjas")+"", (float)(Const.leaderboardColumns[4]*scaleX), (float)(Const.leaderboardStartY*scaleY+(i+1)*(height-Const.leaderboardStartY)/Const.leaderboardRows*scaleY), leaderboardPaint);
						canvas.drawText(s.getContext().get("tanks")+"", (float)(Const.leaderboardColumns[5]*scaleX), (float)(Const.leaderboardStartY*scaleY+(i+1)*(height-Const.leaderboardStartY)/Const.leaderboardRows*scaleY), leaderboardPaint);
						canvas.drawText(mins+":"+secs, (float)(Const.leaderboardColumns[6]*scaleX), (float)(Const.leaderboardStartY*scaleY+(i+1)*(height-Const.leaderboardStartY)/Const.leaderboardRows*scaleY), leaderboardPaint);
						canvas.drawText(health, (float)(Const.leaderboardColumns[7]*scaleX), (float)(Const.leaderboardStartY*scaleY+(i+1)*(height-Const.leaderboardStartY)/Const.leaderboardRows*scaleY), leaderboardPaint);

						if(s.getUser().equals(Session.getCurrentSession().getUser())){
							leaderboardPaint.setColor(Color.WHITE);
						}
					}
				} else {
					loaderRotation += 10;
					Matrix m = new Matrix();
					m.setRotate(loaderRotation, updateBitmap.getWidth()/2, updateBitmap.getHeight()/2);
					m.postTranslate((float)(width/2*scaleX - updateBitmap.getWidth()/2), (float)(height/2*scaleY - updateBitmap.getHeight()/2));
					canvas.drawBitmap(updateBitmap, m, null);
				}
				for(; i < 15; i++){
					canvas.drawText((i+1+leaderboardPage*scoresPerPage)+". ", (float)(Const.leaderboardColumns[0]*scaleX), (float)(Const.leaderboardStartY*scaleY+(i+1)*(height-Const.leaderboardStartY)/Const.leaderboardRows*scaleY), leaderboardPaint);
				}
			} catch(IndexOutOfBoundsException e){ //The rendering might get interrupted by a leaderboard update

			}
		}
	} 



	public void renderBriefer(Canvas canvas){
		if(menuState == LEVEL_MENU){
			try{
				briefer.render(canvas);
			} catch(ArrayIndexOutOfBoundsException e){}
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {


	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		new LoadOperation().execute("");
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

	public void resume(){
		if(theme!=null)
			playTheme();
		if (thread.getState()==Thread.State.TERMINATED) { 
			thread = new MainThread(getHolder(),this);

		}
		thread.setRunning(true);
		try{thread.start();} catch(IllegalThreadStateException err){}
		currentHighScoreMode = 0;
		menuState = MAIN_MENU;
	}

	//Pause the game
	public void pause(){
		if(theme != null && theme.isPlaying())
			stopTheme();
		thread.setRunning(false);
	}

	@Override
	public boolean onTouchEvent(MotionEvent e){
		if(!loading){
			if(playButton != null){
				touchX = (int)(e.getX()/scaleX);
				touchY = (int)(e.getY()/scaleY);
				if(e.getAction() == MotionEvent.ACTION_DOWN){

					switch(menuState){
					case MAIN_MENU:
						if(playButton.isClicked(touchX, touchY)){
							menuState = LEVEL_MENU;
						}
						if(settingsButton.isClicked(touchX, touchY)){
							menuState = SETTINGS_MENU;
						}
						if(highscoreButton.isClicked(touchX, touchY)){
							menuState = HIGHSCORE_MENU;
							sync = false;
							updateSession++;
							loadHighscores(updateSession);
						}
						break;
					case LEVEL_MENU:
						for(int i = 0; i < levelButtons.length; i++){
							if(levelButtons[i].isClicked(touchX, touchY)){
								if(briefer.handleClick(i)){
									nextLevel = i;
									menuState = PLAY;
								}
							}
						}
						break;
					case SETTINGS_MENU:
						if(soundButton.isClicked(touchX, touchY)){
							Editor ed = settings.edit();
							ed.putFloat("soundVolume", settings.getFloat("soundVolume", 1)>0?0:1);
							ed.commit();
							soundSlider.setValue(settings.getFloat("soundVolume", 1));
							sm.playSound(0, settings.getFloat("soundVolume", 0));

						}
						if(musicButton.isClicked(touchX, touchY)){
							Editor ed = settings.edit();
							ed.putFloat("musicVolume", settings.getFloat("musicVolume", 1)>0?0:1);
							ed.commit();
							theme.setVolume(settings.getFloat("musicVolume", 1), settings.getFloat("musicVolume", 1));
							musicSlider.setValue(settings.getFloat("musicVolume", 1));
						}
						if(scoreButton.isClicked(touchX, touchY)){
							Editor ed = settings.edit();
							ed.putBoolean("scoring", settings.getBoolean("scoring", true)?false:true);
							ed.commit();
							if(settings.getBoolean("scoring", true)){
								controller.query(null);
							}
						}
						/*if(difficultyButton.isClicked(touchX, touchY)){
					Editor ed = settings.edit();
					ed.putInt("difficulty", settings.getInt("difficulty", 0)==0?1:0);
					ed.commit();

				}*/
						if(setNameButton.isClicked(touchX, touchY)){
							((MainActivity)context).requestName(settings.getString("userName", "Player"));
						}
						soundSlider.handleTouch(touchX, touchY);
						musicSlider.handleTouch(touchX, touchY);
						break;
					case HIGHSCORE_MENU:
						if(leaderboardButtons[UPDATE].isClicked(touchX, touchY)){
							currentHighScoreMode = 0;
							sync = false;
							updateSession++;
							loadHighscores(updateSession);
						}
						if(leaderboardButtons[LEFT].isClicked(touchX, touchY)){
							leaderboardLevel--;
							leaderboardPage = 0;
							if(leaderboardLevel < 0)
								leaderboardLevel = 11;
						}
						if(leaderboardButtons[RIGHT].isClicked(touchX, touchY)){
							leaderboardLevel++;
							leaderboardPage = 0;
							if(leaderboardLevel > 11)
								leaderboardLevel = 0;
						}
						if(leaderboardButtons[UP].isClicked(touchX, touchY)){
							if(leaderboardPage > 0)
								leaderboardPage--;
						}
						if(leaderboardButtons[DOWN].isClicked(touchX, touchY)){
							if(leaderboardPage < maxPages-1)
								leaderboardPage++;
						}
						break;

					}
				}
				if(e.getAction() == MotionEvent.ACTION_MOVE){
					soundSlider.handleTouch(touchX, touchY);
					musicSlider.handleTouch(touchX, touchY);
				}
				if(e.getAction() == MotionEvent.ACTION_UP){
					musicSlider.release();
					soundSlider.release();
				}
			}
		}
		return true;

	}

	public void goToGame(int level){
		stopTheme();

		Intent intent = new Intent(context, GameActivity.class);
		intent.putExtra(LEVEL, level);

		context.startActivity(intent);
		((Activity)context).finish();
	}

	public void back(){
		switch(menuState){
		case LEVEL_MENU:
			briefer.close();
		case SETTINGS_MENU:
		case HIGHSCORE_MENU:
			menuState = MAIN_MENU;
			break;
		}
	}

	public void renderProtagonist(Canvas canvas){
		feetAngle = (float)(180/Math.PI*Math.sin((double)protagonist.getStepCount()/protagonist.getNumberOfSteps()*Math.PI));
		aimAngle = (float)(protagonist.getAim());
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
			renderMatrix.setTranslate((float)((protagonist.getXPos() + protagonist.getWidth()*(Const.pupilXOffset-0.5)+protagonist.getWidth()*Const.pupilRadius*Math.cos(aimAngle*Math.PI/180))*scaleX), (float)((protagonist.getYPos() + protagonist.getHeight()*(Const.pupilYOffset-0.5)+protagonist.getHeight()*Const.pupilRadius*Math.sin(aimAngle*Math.PI/180) )*scaleY));
			canvas.drawBitmap(pupilBitmap, renderMatrix, null);
			//Draw weapon
			renderMatrix.setTranslate((float)((protagonist.getXPos() - protagonist.getWidth()*(0.5-Const.weaponXAxis) + protagonist.getWidth()*Const.weaponRadius)*scaleX), (float)((protagonist.getYPos() + protagonist.getHeight()*(Const.weaponYAxis-0.5))*scaleY));
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
			renderMatrix.setTranslate((float)((protagonist.getXPos() + protagonist.getWidth()*(Const.pupilXOffset-0.5)+protagonist.getWidth()*Const.pupilRadius*Math.cos(aimAngle*Math.PI/180))*scaleX), (float)((protagonist.getYPos() + protagonist.getHeight()*(Const.pupilYOffset-0.5)+protagonist.getHeight()*Const.pupilRadius*Math.sin(aimAngle*Math.PI/180) )*scaleY));
			canvas.drawBitmap(pupilBitmapFlipped, renderMatrix, null);
			//Draw weapon
			renderMatrix.setTranslate((float)((protagonist.getXPos()  + protagonist.getWidth()*(0.5-Const.weaponXAxis) - protagonist.getWidth()*Const.weaponRadius)*scaleX - weaponBitmapFlipped.getWidth()), (float)((protagonist.getYPos() + protagonist.getHeight()*(Const.weaponYAxis-0.5))*scaleY));
			canvas.drawBitmap(weaponBitmapFlipped, renderMatrix, null);
		}
	}

	public void renderButterfly(Canvas canvas){
		renderMatrix.setRotate(butterfly.getRot(), butterflyBitmaps[0].getWidth()/2, butterflyBitmaps[0].getHeight()/2);
		renderMatrix.postTranslate((float)((butterfly.getX()-Const.butterflySize/2)*scaleX), (float)((butterfly.getY()-Const.butterflySize/2)*scaleY));
		canvas.drawBitmap(butterflyBitmaps[(int)(Math.abs(butterfly.getCounter() % 2))], renderMatrix, null);
	}

	public void setUserName(String name){
		userName = name;
		Editor e = settings.edit();
		e.putString("userName", name);
		e.commit();
	}

	public void uploadUserName(String name){
		User user = Session.getCurrentSession().getUser();

		// update his values
		user.setLogin(name);

		// set up a request observer
		RequestControllerObserver observer = new RequestControllerObserver() {

			@Override
			public void requestControllerDidReceiveResponse(RequestController aRequestController) {
				// update displayed values
				User user = ((UserController)aRequestController).getUser();
				setUserName(user.getLogin());
			}

			@Override
			public void requestControllerDidFail(RequestController controller, Exception exception) {

				try{
					RequestControllerException ctrlException = (RequestControllerException) exception;
					if(ctrlException.hasDetail(RequestControllerException.DETAIL_USER_UPDATE_REQUEST_USERNAME_TAKEN)) {
						((MainActivity)context).displayMessage("Error uploading name", "Name is already taken");
					}
					else if(ctrlException.hasDetail(RequestControllerException.DETAIL_USER_UPDATE_REQUEST_USERNAME_TOO_SHORT)) {
						((MainActivity)context).displayMessage("Error uploading name", "Name is too short");
					}
					else if(ctrlException.hasDetail(RequestControllerException.DETAIL_USER_UPDATE_REQUEST_INVALID_USERNAME)) {
						((MainActivity)context).displayMessage("Error uploading name", "The name is invalid");
					} 
				} catch(ClassCastException e){}
			}
		};

		// with our observer, set up the request controller
		UserController userController = new  UserController(observer);

		// pass the user into the controller
		userController.setUser(user);

		// submit our changes
		userController.submitUser();
	}

	public void loadHighscores(final int session){
		if(sync && session == updateSession){
			RequestControllerObserver observer = new RequestControllerObserver() {

				@Override
				public void requestControllerDidReceiveResponse(RequestController requestController) {
					if(((ScoresController)requestController).getScores() != null){
						if(session == updateSession){
							if(localScores.getInt("score_0_"+(currentHighScoreMode/2), 0) > ((ScoresController)requestController).getScores().get(0).getResult()){
								//Upload score to server
								RequestControllerObserver observer = new RequestControllerObserver(){

									@Override
									public void requestControllerDidReceiveResponse(RequestController requestController) {

									}

									@Override
									public void requestControllerDidFail(RequestController aRequestController, Exception anException) {
										((MainActivity)context).displayMessage("Error", "The score sync failed!");
										menuState = MAIN_MENU;
									}
								};
								final ScoreController myScoreController = new ScoreController(observer);
								Score s = new Score((double)localScores.getInt("score_0_"+(currentHighScoreMode/2), 0), null);
								Map<String, Object> context = new HashMap<String, Object>();
								context.put("normals", localScores.getInt("drones_0_"+(currentHighScoreMode/2), 0));
								context.put("ninjas", localScores.getInt("ninjas_0_"+(currentHighScoreMode/2), 0));
								context.put("tanks", localScores.getInt("tanks_0_"+(currentHighScoreMode/2), 0));
								context.put("health", localScores.getInt("health_0_"+(currentHighScoreMode/2), 0));
								context.put("mins", localScores.getInt("mins_0_"+(currentHighScoreMode/2), 0));
								context.put("secs", localScores.getInt("secs_0_"+(currentHighScoreMode/2), 0));
								context.put("cSecs", localScores.getInt("cSecs_0_"+(currentHighScoreMode/2), 0));

								s.setMode(currentHighScoreMode); 
								s.setContext(context);
								myScoreController.submitScore(s);

							}
						}
					}
					currentHighScoreMode+=2;
					if(currentHighScoreMode <= Const.maxModes){
						loadHighscores(session);
					}

				}

				@Override
				public void requestControllerDidFail(RequestController aRequestController, Exception anException) {
					if(session == updateSession){
						if(localScores.getInt("score_0_"+(currentHighScoreMode/2), 0) > 0){
							//Upload score to server
							RequestControllerObserver observer = new RequestControllerObserver(){

								@Override
								public void requestControllerDidReceiveResponse(RequestController requestController) {

								}

								@Override
								public void requestControllerDidFail(RequestController aRequestController, Exception anException) {
									((MainActivity)context).displayMessage("Error", "The score sync failed!");
									menuState = MAIN_MENU;
								}
							};
							final ScoreController myScoreController = new ScoreController(observer);
							Score s = new Score((double)localScores.getInt("score_0_"+(currentHighScoreMode/2), 0), null);
							Map<String, Object> context = new HashMap<String, Object>();
							context.put("normals", localScores.getInt("drones_0_"+(currentHighScoreMode/2), 0));
							context.put("ninjas", localScores.getInt("ninjas_0_"+(currentHighScoreMode/2), 0));
							context.put("tanks", localScores.getInt("tanks_0_"+(currentHighScoreMode/2), 0));
							context.put("health", localScores.getInt("health_0_"+(currentHighScoreMode/2), 0));
							context.put("mins", localScores.getInt("mins_0_"+(currentHighScoreMode/2), 0));
							context.put("secs", localScores.getInt("secs_0_"+(currentHighScoreMode/2), 0));
							context.put("cSecs", localScores.getInt("cSecs_0_"+(currentHighScoreMode/2), 0));

							s.setMode(currentHighScoreMode); 
							s.setContext(context);
							myScoreController.submitScore(s);
						}
						currentHighScoreMode+=2;
						if(currentHighScoreMode <= Const.maxModes){
							loadHighscores(session);
						}
					}
				}
			};
			ScoresController controller = new ScoresController(observer);
			controller.setMode(currentHighScoreMode); 
			controller.loadRangeForUser(Session.getCurrentSession().getUser());
		} else if(session == updateSession){
			if(currentHighScoreMode == 0){
				highScoreList.clear();
			}
			RequestControllerObserver observer = new RequestControllerObserver() {

				@Override
				public void requestControllerDidReceiveResponse(RequestController requestController) {
					if(session == updateSession){
						List<Score> retrievedScores = ((ScoresController)requestController).getScores();
						highScoreList.add(retrievedScores);
						currentHighScoreMode+=2;
						loadHighscores(session);
					}
				}

				@Override
				public void requestControllerDidFail(RequestController aRequestController, Exception anException) {
					((MainActivity)context).displayMessage("Error", "The leaderboards could not be loaded!");
					menuState = MAIN_MENU;
				}
			};
			ScoresController controller = new ScoresController(observer);
			if(currentHighScoreMode <= Const.maxModes){
				controller.setMode(currentHighScoreMode); 
				controller.setRangeLength(maxPages*scoresPerPage); 
				controller.loadRangeAtRank(1);
			} else {
				sync = true;
				currentHighScoreMode = 0;
				loadHighscores(session);
			}
		}
	}

	private class LoadOperation extends AsyncTask<String, Integer, String> {

		@Override
		protected String doInBackground(String... params) {
			scaleY = (double)getHeight()/height;
			scaleX = (double)getWidth()/width;

			BitmapFactory.Options mNoScale = new BitmapFactory.Options();
			mNoScale.inScaled = false;

			Bitmap playBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.start, mNoScale), (int)(1.5*Const.menuButtonWidth*scaleX), (int)(1.5*Const.menuButtonHeight*scaleY), true);
			playButton = new Button(10, 10, playBitmap.getWidth()/scaleX, playBitmap.getHeight()/scaleY, playBitmap);

			Bitmap settingsBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.settings, mNoScale), (int)(1.5*Const.menuButtonWidth*scaleX), (int)(1.5*Const.menuButtonHeight*scaleY), true);
			settingsButton = new Button(10, (int)(10 + 1.5*Const.menuButtonHeight), settingsBitmap.getWidth()/scaleX, settingsBitmap.getHeight()/scaleY, settingsBitmap);

			Bitmap highscoreBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.highscore, mNoScale), (int)(1.5*Const.menuButtonWidth*scaleX), (int)(1.5*Const.menuButtonHeight*scaleY), true);
			highscoreButton = new Button(10, (int)(10 + 2*1.5*Const.menuButtonHeight), highscoreBitmap.getWidth()/scaleX, highscoreBitmap.getHeight()/scaleY, highscoreBitmap);

			Bitmap soundBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.effects, mNoScale), (int)(1.5*Const.menuButtonWidth*scaleX), (int)(1.5*Const.menuButtonHeight*scaleY), true);
			soundButton = new Button(10, 10, soundBitmap.getWidth()/scaleX, soundBitmap.getHeight()/scaleY, soundBitmap);

			Bitmap musicBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.music, mNoScale), (int)(1.5*Const.menuButtonWidth*scaleX), (int)(1.5*Const.menuButtonHeight*scaleY), true);
			musicButton = new Button(10, (int)(10 + 1.5*Const.menuButtonHeight), musicBitmap.getWidth()/scaleX, musicBitmap.getHeight()/scaleY, musicBitmap);

			Bitmap scoreBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.score, mNoScale), (int)(1.5*Const.menuButtonWidth*scaleX), (int)(1.5*Const.menuButtonHeight*scaleY), true);
			scoreButton = new Button(10, (int)(10 + 3*Const.menuButtonHeight), scoreBitmap.getWidth()/scaleX, scoreBitmap.getHeight()/scaleY, scoreBitmap);

			Bitmap difficultyBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.difficulty, mNoScale), (int)(1.5*Const.menuButtonWidth*scaleX), (int)(1.5*Const.menuButtonHeight*scaleY), true);
			difficultyButton = new Button(10, (int)(10 + 4.5*Const.menuButtonHeight), difficultyBitmap.getWidth()/scaleX, difficultyBitmap.getHeight()/scaleY, difficultyBitmap);

			Bitmap setNameBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.set_name, mNoScale), (int)(1.5*Const.menuButtonWidth*scaleX), (int)(1.5*Const.menuButtonHeight*scaleY), true);
			setNameButton = new Button(10, (int)(10 + 6*Const.menuButtonHeight), setNameBitmap.getWidth()/scaleX, setNameBitmap.getHeight()/scaleY, setNameBitmap);

			Bitmap[] leaderboardBitmaps = new Bitmap[]{
					Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.prev_level, mNoScale), (int)(Const.menuButtonWidth*scaleX), (int)(Const.menuButtonHeight*scaleY), true),
					Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.next_level, mNoScale), (int)(Const.menuButtonWidth*scaleX), (int)(Const.menuButtonHeight*scaleY), true),
					Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.prev_page, mNoScale), (int)(Const.menuButtonWidth*scaleX), (int)(Const.menuButtonHeight*scaleY), true),
					Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.next_page, mNoScale), (int)(Const.menuButtonWidth*scaleX), (int)(Const.menuButtonHeight*scaleY), true),
					Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.update_b, mNoScale), (int)(Const.menuButtonWidth*scaleX), (int)(Const.menuButtonHeight*scaleY), true)
			};
			leaderboardButtons = new Button[leaderboardBitmaps.length];
			for(int i = 0; i < leaderboardBitmaps.length; i++){
				leaderboardButtons[i] = new Button((int)((width-5*Const.menuButtonWidth-6*space)/2+ space + i*(space+Const.menuButtonWidth)), (int)(height-Const.menuButtonHeight-space), Const.menuButtonWidth, Const.menuButtonHeight, leaderboardBitmaps[i]);
			}

			musicSlider = new Slider(musicButton.getX() + musicButton.getWidth() + Const.HUDPadding, musicButton.getY(), musicButton.getWidth(), musicButton.getHeight(), settings.getFloat("musicVolume", 1));
			soundSlider = new Slider(soundButton.getX() + soundButton.getWidth() + Const.HUDPadding, soundButton.getY(), soundButton.getWidth(), soundButton.getHeight(), settings.getFloat("soundVolume", 1));


			backgroundBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.menu_background, mNoScale), (int)(width*scaleX), (int)(height*scaleY), true);
			bodyBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.protagonist_body, mNoScale), (int)(protagonist.getWidth()*scaleX*Const.bodyXScale), (int)(protagonist.getHeight()*scaleY*Const.bodyYScale), true);
			eyeMouthBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.protagonist_eye_mouth, mNoScale), (int)(protagonist.getWidth()*scaleX*Const.eyeMouthXScale), (int)(protagonist.getHeight()*scaleY*Const.eyeMouthYScale), true);
			footBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.protagonist_foot, mNoScale), (int)(protagonist.getWidth()*scaleX*Const.footXScale), (int)(protagonist.getHeight()*scaleY*Const.footYScale), true);
			weaponBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.protagonist_weapon, mNoScale), (int)(protagonist.getWidth()*scaleX*Const.weaponXScale), (int)(protagonist.getHeight()*scaleY*Const.weaponYScale), true);
			pupilBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.protagonist_pupil, mNoScale), (int)(protagonist.getWidth()*scaleX*Const.pupilXScale), (int)(protagonist.getHeight()*scaleY*Const.pupilYScale), true);

			//Flip images
			renderMatrix.setScale(-1, 1);
			bodyBitmapFlipped = Bitmap.createBitmap(bodyBitmap, 0, 0, bodyBitmap.getWidth(), bodyBitmap.getHeight(), renderMatrix,false);
			eyeMouthBitmapFlipped = Bitmap.createBitmap(eyeMouthBitmap, 0, 0, eyeMouthBitmap.getWidth(), eyeMouthBitmap.getHeight(), renderMatrix,false);
			footBitmapFlipped = Bitmap.createBitmap(footBitmap, 0, 0, footBitmap.getWidth(), footBitmap.getHeight(), renderMatrix,false);
			weaponBitmapFlipped = Bitmap.createBitmap(weaponBitmap, 0, 0, weaponBitmap.getWidth(), weaponBitmap.getHeight(), renderMatrix,false);
			pupilBitmapFlipped = Bitmap.createBitmap(pupilBitmap, 0, 0, pupilBitmap.getWidth(), pupilBitmap.getHeight(), renderMatrix,false);

			levelBitmaps = new Bitmap[]{
					Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.tutorial, mNoScale), (int)(Const.menuButtonWidth*scaleX), (int)(Const.menuButtonHeight*scaleY), true),
					Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.level_1, mNoScale), (int)(Const.menuButtonWidth*scaleX), (int)(Const.menuButtonHeight*scaleY), true),
					Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.level_2, mNoScale), (int)(Const.menuButtonWidth*scaleX), (int)(Const.menuButtonHeight*scaleY), true),
					Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.level_3, mNoScale), (int)(Const.menuButtonWidth*scaleX), (int)(Const.menuButtonHeight*scaleY), true),
					Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.level_4, mNoScale), (int)(Const.menuButtonWidth*scaleX), (int)(Const.menuButtonHeight*scaleY), true),
					Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.level_5, mNoScale), (int)(Const.menuButtonWidth*scaleX), (int)(Const.menuButtonHeight*scaleY), true),
					Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.level_6, mNoScale), (int)(Const.menuButtonWidth*scaleX), (int)(Const.menuButtonHeight*scaleY), true),
					Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.level_7, mNoScale), (int)(Const.menuButtonWidth*scaleX), (int)(Const.menuButtonHeight*scaleY), true),
					Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.level_8, mNoScale), (int)(Const.menuButtonWidth*scaleX), (int)(Const.menuButtonHeight*scaleY), true),
					Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.level_9, mNoScale), (int)(Const.menuButtonWidth*scaleX), (int)(Const.menuButtonHeight*scaleY), true),
					Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.level_10, mNoScale), (int)(Const.menuButtonWidth*scaleX), (int)(Const.menuButtonHeight*scaleY), true),
					Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.the_final, mNoScale), (int)(Const.menuButtonWidth*scaleX), (int)(Const.menuButtonHeight*scaleY), true),
					Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.credits, mNoScale), (int)(Const.menuButtonWidth*scaleX), (int)(Const.menuButtonHeight*scaleY), true)
			};

			butterflyBitmaps = new Bitmap[]{
					Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.butterfly_in, mNoScale), (int)(Const.butterflySize*scaleX), (int)(Const.butterflySize*scaleY), true),
					Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.butterfly_out, mNoScale), (int)(Const.butterflySize*scaleX), (int)(Const.butterflySize*scaleY), true)
			};

			sliderLineBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.slider_line, mNoScale), (int)(musicSlider.getWidth()*scaleX), (int)(musicSlider.getHeight()*scaleY), true);
			sliderHandleBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.slider_handle, mNoScale), (int)(Const.sliderHandleWidth*scaleX), (int)(musicSlider.getHeight()*scaleY), true);

			onBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.on, mNoScale), (int)(scoreButton.getWidth()*scaleX), (int)(scoreButton.getHeight()*scaleY), true);
			offBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.off, mNoScale), (int)(scoreButton.getWidth()*scaleX), (int)(scoreButton.getHeight()*scaleY), true);

			updateBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.update, mNoScale), (int)(Const.updateSize*scaleX), (int)(Const.updateSize*scaleY), true);
			if(currentLevel > Const.levelCap)
				currentLevel = Const.levelCap;
			levelButtons = new Button[currentLevel+1];
			for(int i = 0; i <= currentLevel; i++){
				levelButtons[i] = new Button(Const.HUDPadding + (int)(Const.menuButtonWidth*(i/Const.levelButtonsPerRow)), Const.HUDPadding + (int)(Const.menuButtonHeight*(i%Const.levelButtonsPerRow)), Const.menuButtonWidth, Const.menuButtonHeight, levelBitmaps[i]);
			}

			userPaint.setTextSize((float)(10*scaleY));
			userPaint.setAntiAlias(true);
			userPaint.setDither(true);

			briefer = new LevelBriefer(70, Const.HUDPadding, (width - 70 - Const.HUDPadding), (height - 2*Const.HUDPadding), scaleX, scaleY, settings, context.getSharedPreferences("localScores", Context.MODE_PRIVATE));

			leaderboardPaint.setTextSize((float)(((height-Const.leaderboardStartY)/Const.leaderboardRows)*scaleY));
			leaderboardPaint.setAntiAlias(true);
			leaderboardPaint.setDither(true);

			loadSounds();
			playTheme();

			return "Executed";
		}      

		@Override
		protected void onPostExecute(String result) {
			loading = false;
		}

		@Override
		protected void onPreExecute() {
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
		}
	}   




}
