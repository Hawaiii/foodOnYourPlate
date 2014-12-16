package edu.rice.ZhangHua.AR;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.KeyPoint;
import org.opencv.imgproc.Imgproc;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;


/**
 * This class is a controller for puzzle game.
 * It converts the image from Camera into the shuffled image
 */
public class Puzzle15Processor {
	
    private static final String  TAG = "ELEC549::ARModel";

	private Mat foodImg;
	private String foodPath;
	
	private Mat patternImg;
	private String patternPath;
//	private Mat pattern8uc;
	
	private Bitmap tempBitmap;
	private Mat displayMat;
	
	// All the variables for processing
	private Mat grayCameraFeed;
	FeatureDetector Orbdetector;
	DescriptorExtractor OrbExtractor;
	DescriptorMatcher matcher;
	
	Mat descriptors1;
	MatOfKeyPoint keypoints1;
	Mat descriptors2;
	
	MatOfDMatch matches;
	List<DMatch> matchesList;
	
	double max_dist=0;
    double min_dist=99;
    double dist;
    
    Mat tform;
    Vector<Double> R_world;
    
    Mat overlay;
    
    List<Mat> bgra;
    List<Mat> combined;
	
	public Puzzle15Processor(){
	}
	
	public void setSelectFoodPath(String apath){
		foodPath = apath;
	}
	
	public Mat getFoodImgForDisplay() {
		if (foodImg != null) {
			// force it into 960*1280*4 channels
			if (displayMat == null) displayMat = new Mat(960, 1280, CvType.CV_8UC4);
			Imgproc.resize(foodImg, displayMat, displayMat.size());
		} else {
			displayMat = Mat.zeros(960, 1280, CvType.CV_8UC4);
			Log.i(TAG, "foodImg is null");
		}
		return displayMat;
	}
	
	public void setFoodImg(){
		if (foodPath == null){
			Log.i(TAG,"food path is empty. Cannot read food image.");
			return;
		}
		
		tempBitmap = BitmapFactory.decodeFile(foodPath);
		foodImg = new Mat(960, 1280, CvType.CV_8UC4);
		Log.i(TAG,"set food image:"+foodPath);
		Utils.bitmapToMat(tempBitmap, foodImg);
		
		tempBitmap.recycle();
	}
	
	public void setSelectPatternPath(String apath){
		patternPath = apath;
	}
	
	public Mat getPatternImgForDisplay() {
		if (patternImg != null) {
			// force it into 960*1280*4 channels
//			if (displayMat == null) 
			displayMat = new Mat(960, 1280, CvType.CV_8UC1);
			Imgproc.resize(patternImg, displayMat, displayMat.size());
			
			
		} else {
			displayMat = Mat.zeros(960, 1280, CvType.CV_8UC4);
			Log.i(TAG, "patternImg is null");
		}
		return displayMat;
	}
	
	public void setPatternImg(){
		if (patternPath == null){
			Log.i(TAG,"pattern path is empty. Cannot read pattern image.");
			return;
		}
		
		tempBitmap = BitmapFactory.decodeFile(patternPath);
		patternImg = new Mat(960, 1280, CvType.CV_8UC4);
		Log.i(TAG,"set pattern image:"+patternPath);
		Utils.bitmapToMat(tempBitmap, patternImg);
		Imgproc.cvtColor(patternImg, patternImg, Imgproc.COLOR_BGRA2GRAY, 1);
//		patternImg.convertTo(patternImg, CvType.CV_32FC1);
		
		tempBitmap.recycle();
	}
	
	/**
	 * Given camera feed matrix, find perspective, transform and overlay foodImg
	 * Return a Mat of same size and type.
	 * @param cameraFeed
	 * @return a Mat of same size and type.
	 */
	public Mat process(Mat cameraFeed){
		
		// check of unset things
		if (foodImg == null){
			Log.i(TAG, "food image not selected.");
			return cameraFeed;
		}
		if (patternImg == null){
			Log.i(TAG, "pattern image not loaded.");
			return cameraFeed;
		}
		
		// don't resize pattern & food img every frame - so don't resize them here
		Log.i(TAG, "unimplemented process method.");
		grayCameraFeed = cameraFeed.clone();
		Imgproc.cvtColor(cameraFeed, grayCameraFeed, Imgproc.COLOR_BGRA2GRAY, 1);
		tform = perspectiveFind(grayCameraFeed, patternImg);
		if (tform == null) return cameraFeed; // no pattern found
		
		R_world = new Vector<Double>();
		return foodoverlay(foodImg, R_world, cameraFeed, tform);
	}
	
	// returns the perspective transformation matrix between the skewed object and the front object
	// inputs are the pattern and the scene image with the pattern in it
	// input images are gray-scale
	// TODO move allocation and release of variables used in here outside
	public Mat perspectiveFind(Mat skewObj, Mat frontObj){

		
	    // first image-- the front (scene)
	    descriptors1 = new Mat();
	    keypoints1 = new MatOfKeyPoint();	 
	    Orbdetector.detect(skewObj, keypoints1);
	    OrbExtractor.compute(skewObj, keypoints1, descriptors1);
	    
	    // first image-- template object
	    Mat descriptors2 = new Mat();
	    MatOfKeyPoint keypoints2 = new MatOfKeyPoint();     
	    Orbdetector.detect(frontObj, keypoints2);
	    OrbExtractor.compute(frontObj, keypoints2, descriptors2);
	    
	    // match
	    if (descriptors1.width() == 0 || descriptors2.width() == 0) return null;
	    matches = new MatOfDMatch();
	    matcher.match(descriptors1,descriptors2,matches);

	    matchesList = matches.toList();
	    for(int i=0;i<matches.rows();i++)
	    {
	        dist = matchesList.get(i).distance;
	        if (dist<min_dist) min_dist = dist;
	        if (dist>max_dist) max_dist = dist;
	    }
	    if (matchesList.size() < 4) return null; // if there are not enough good matches
	    
	    //set up good matches, add matches if close enough
	    LinkedList<DMatch> good_matches = new LinkedList<DMatch>();
	    MatOfDMatch gm = new MatOfDMatch();
	    for (int i=0;i<matchesList.size();i++)
	    {
	        if(matchesList.get(i).distance<0.3*(max_dist+min_dist))
	        {
	            good_matches.addLast(matchesList.get(i));
	        }
	    }
	    gm.fromList(good_matches);
	    
	    ///////////////////////////////////////////////////////////////////////////////
	    //put keypoints mats into lists
	    List<KeyPoint> keypoints1_List = keypoints1.toList();
	    List<KeyPoint> keypoints2_List = keypoints2.toList();
	    
	    //put keypoints into point2f to find perspective transform
	    LinkedList<Point> objList = new LinkedList<Point>();
	    LinkedList<Point> sceneList = new LinkedList<Point>();
	    for(int i=0;i<good_matches.size();i++)
	    {
	        objList.addLast(keypoints2_List.get(good_matches.get(i).trainIdx).pt);
	        sceneList.addLast(keypoints1_List.get(good_matches.get(i).queryIdx).pt);
	    }
	    MatOfPoint2f obj = new MatOfPoint2f();
	    MatOfPoint2f scene = new MatOfPoint2f();
		
	    obj.fromList(objList);
	    scene.fromList(sceneList);
	    obj.convertTo(obj, CvType.CV_32FC1);
	    scene.convertTo(scene, CvType.CV_32FC1);
			    
//	    Mat imageOut = skewObj.clone();
//	    Features2d.drawMatches(skewObj, keypoints1, frontObj, keypoints2, gm, imageOut);
//	    Highgui.imwrite("drawMatches.jpeg", imageOut);
	    if (obj.rows() < 4 || scene.rows() < 4) return null;
	    Mat perspectiveTransform = Calib3d.findHomography(obj, scene, 8, 5);
	    //Mat perspectiveTransform = Imgproc.getPerspectiveTransform(obj,scene);
	    
	    //debug output: prints out the transform matrix
	    for(int i=0;i<perspectiveTransform.cols();i++){
			for (int j =0;j<perspectiveTransform.rows();j++){
				System.out.println(perspectiveTransform.get(i,j)[0]);
			}
			System.out.println("\n");
		}
	      
	    
	    descriptors1.release();
	    descriptors2.release();
	    keypoints1.release();
	    keypoints2.release();
	    obj.release();
	    scene.release();
	    matches.release();
	    gm.release();

		return perspectiveTransform;
	}
	
	// Once found the transformation matrix between the skewed object and the front pattern
	// we can lay the food image onto the scene by transforming the food image in the same way
	// TODO move allocation and release of variables
	/**
	 * 
	 * @param food	rgba Mat
	 * @param R_world	world coordinate?
	 * @param scene	rgba Mat
	 * @param tform transformation to be applied to food
	 * @return
	 */
	public Mat foodoverlay(Mat food, Vector<Double> R_world, Mat scene, Mat tform){
		overlay = new Mat(scene.rows(),scene.cols(),CvType.CV_8UC4);

		bgra = new ArrayList<Mat>(4);
		combined = new ArrayList<Mat>();
		
		Core.split(food, bgra);
		for (int i = 0; i < bgra.size(); i++){
			// warp the images: R, G, B, alpha
			combined.add(bgra.get(i).clone()); // allocating space
			Imgproc.warpPerspective(bgra.get(i), combined.get(i), tform, scene.size());
		}
		
		bgra = new ArrayList<Mat>(4);
		Core.split(scene,bgra);

		// mask stuff
		Mat foodAlpha = combined.get(3).clone();
		Mat foodAlphaInverse = combined.get(3).clone();
		Core.addWeighted(foodAlpha, 1.0/255, Mat.zeros(foodAlpha.size(), foodAlpha.type()), 0, 0, foodAlpha);
		Core.addWeighted(foodAlpha, -1.0, Mat.ones(foodAlpha.size(), foodAlpha.type()), 1.0, 0, foodAlphaInverse);
//		Core.addWeighted(foodAlphaInverse, -1.0, Mat.ones(foodAlphaInverse.size(), foodAlphaInverse.type()), 255.0, 0.0,foodAlphaInverse, foodAlphaInverse.type());
		
		for (int i = 0; i < 3; i++){
			Core.multiply(combined.get(i), foodAlpha, combined.get(i));
			Core.multiply(bgra.get(i), foodAlphaInverse, bgra.get(i));
			Core.add(combined.get(i), bgra.get(i), combined.get(i));
		}
		combined.set(3,bgra.get(3)); // sets the alpha
		
		Core.merge(combined,overlay);
		
		return overlay;
	}

	/**
	 * Allocates memory for processing
	 */
	public void allocProcessMemory() {
		// create feature extractors and matchers
//		Orbdetector = FeatureDetector.create(FeatureDetector.SIFT);
//	    OrbExtractor = DescriptorExtractor.create(DescriptorExtractor.SIFT);
		Orbdetector = FeatureDetector.create(FeatureDetector.ORB);
		OrbExtractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
	    matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
	    
	    
	}

	

}
