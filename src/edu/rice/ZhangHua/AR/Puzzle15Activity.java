package edu.rice.ZhangHua.AR;


import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.android.JavaCameraView;

import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

public class Puzzle15Activity extends Activity implements CvCameraViewListener {

    private static final String  TAG = "ELEC549::AR";
    
	private static int RESULT_LOAD_FOOD = 1;
	private static int RESULT_LOAD_PATTERN = 2;
	
	private static int SHOW_CAMERA = 1;
	private static int SHOW_FOOD = 2;
	private static int SHOW_PATTERN = 3;
	private static int SHOW_AR = 4;
	private int toggleShow = SHOW_CAMERA;

    private CameraBridgeViewBase mOpenCvCameraView;
    private Puzzle15Processor    mPuzzle15;
    private MenuItem             mSelectFood;
    private MenuItem			 mSelectPattern;
    private MenuItem			 mShowFood;
    private MenuItem             mAbout;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {

        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");

                    /* Now enable camera view to start receiving frames */
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Log.d(TAG, "Creating and setting view");
        mOpenCvCameraView = (CameraBridgeViewBase) new JavaCameraView(this, -1);
        setContentView(mOpenCvCameraView);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mPuzzle15 = new Puzzle15Processor();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        mSelectFood = menu.add("Select food");
        mSelectPattern = menu.add("Select pattern");
        mShowFood = menu.add("Toggle show");
        mAbout = menu.add("About Us");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "Menu Item selected " + item);
        if (item == mAbout) {
            //TODO
        } else if (item == mShowFood){
        	toggleShow = (toggleShow%4) + 1;
        } else if (item == mSelectPattern){
        	selectPatternImg();
        }
        else if (item == mSelectFood) {
        	selectFoodImg();
        }
        return true;
    }


	public void onCameraViewStarted(int width, int height) {
    	mPuzzle15.setFoodImg();
		mPuzzle15.setPatternImg();
		mPuzzle15.allocProcessMemory();

    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(Mat inputFrame) {
    	if (toggleShow == SHOW_FOOD){
    		Log.i(TAG, "displaying food image");
    		return mPuzzle15.getFoodImgForDisplay();
    	} else if (toggleShow == SHOW_PATTERN){
    		Log.i(TAG, "displaying pattern image");
    		return mPuzzle15.getPatternImgForDisplay();
    	} else if (toggleShow == SHOW_AR){
    		Log.i(TAG, "displaying AR");
    		return mPuzzle15.process(inputFrame);
    	}
    	return inputFrame;
    }
    
    private void selectFoodImg(){
    	// let one set the food image from the android device gallery/photos
    	Log.i(TAG,"calling select food img()");
    	Intent selectIntent = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		startActivityForResult(selectIntent, RESULT_LOAD_FOOD);
    }
    
    private void selectPatternImg() {
    	// let one set the pattern image from the android device gallery/photos
    	Log.i(TAG,"calling select pattern img()");
    	Intent selectIntent = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
    	startActivityForResult(selectIntent, RESULT_LOAD_PATTERN);

	}

    
    @Override
	/**
	 * Selects image from gallery and display image on view.
	 */
	 protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	     super.onActivityResult(requestCode, resultCode, data);
	      
	     if (requestCode == RESULT_LOAD_FOOD && resultCode == RESULT_OK && null != data) {
	         Uri selectedImage = data.getData();
	         String[] filePathColumn = { MediaStore.Images.Media.DATA };
	 
	         Cursor cursor = getContentResolver().query(selectedImage,
	                 filePathColumn, null, null, null);
	         cursor.moveToFirst();
	 
	         int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
	         String picturePath = cursor.getString(columnIndex);
	         cursor.close();
	                      
	         // Set food image
	         Log.i(TAG,"selected food path:"+picturePath);
	         mPuzzle15.setSelectFoodPath(picturePath);
	         
	     } else if (requestCode == RESULT_LOAD_PATTERN && resultCode == RESULT_OK && null != data) {
	         Uri selectedImage = data.getData();
	         String[] filePathColumn = { MediaStore.Images.Media.DATA };
	 
	         Cursor cursor = getContentResolver().query(selectedImage,
	                 filePathColumn, null, null, null);
	         cursor.moveToFirst();
	 
	         int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
	         String picturePath = cursor.getString(columnIndex);
	         cursor.close();
	                      
	         // Set pattern image
	         Log.i(TAG,"selected pattern path:"+picturePath);
	         mPuzzle15.setSelectPatternPath(picturePath);
	     }
    }
}
