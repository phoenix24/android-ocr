package net.londatiga.android;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.List;

import com.googlecode.tesseract.android.TessBaseAPI;

import android.app.Activity;
import android.app.AlertDialog;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

import android.net.Uri;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;

import android.widget.Button;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ImageView;

public class MainActivity extends Activity implements View.OnClickListener {
	private Uri mImageCaptureUri;
	// private ImageView mImageView;

	public static final String DATA_PATH = Environment
			.getExternalStorageDirectory().toString() + "/OCRDemo1/";
	public static final String lang = "eng";

	private static final int PICK_FROM_CAMERA = 1;
	private static final int CROP_FROM_CAMERA = 2;
	private static final int PICK_FROM_FILE = 3;

	private static int OCR_NAME = 5;
	private static int OCR_IDNUMBER = 6;
	private static int OCR_DATE = 7;
	private static int OCR_ADDRESS = 8;
	private static int OCR_GENDER = 9;
	private static int OCR_FATHERNAME = 10;
	private static int OCR_NONE = 11;

	private int ocrMode;
	private static final String TAG = "OCRDemo";

	// private Bitmap MainImage = null;
	static String[] allfiles = { "eng.cube.bigrams", "eng.cube.fold",
			"eng.cube.lm", "eng.cube.lm_", "eng.cube.nn", "eng.cube.params",
			"eng.cube.size", "eng.cube.word-freq", "eng.tesseract_cube.nn",
			"eng.traineddata" };

	public void copyFilesInTessdata(String filename) {

		if (!(new File(DATA_PATH + "tessdata/" + filename)).exists()) {
			try {

				AssetManager assetManager = getAssets();
				InputStream in = assetManager.open("tessdata/" + filename);
				// GZIPInputStream gin = new GZIPInputStream(in);
				OutputStream out = new FileOutputStream(DATA_PATH + "tessdata/"
						+ filename);

				// Transfer bytes from in to out
				byte[] buf = new byte[1024];
				int len;
				// while ((lenf = gin.read(buff)) > 0) {
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
				in.close();
				// gin.close();
				out.close();

				Log.v(TAG, "Copied " + filename);
				System.err.println("Copied " + filename);
			} catch (IOException e) {
				Log.e(TAG,
						"Was unable to copy " + filename + ", " + e.toString());
			}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String[] paths = new String[] { DATA_PATH, DATA_PATH + "tessdata/" };

		for (String path : paths) {
			File dir = new File(path);
			if (!dir.exists()) {
				if (!dir.mkdirs()) {
					Log.v(TAG, "ERROR: Creation of directory " + path
							+ " on sdcard failed");
					return;
				} else {
					Log.v(TAG, "Created directory " + path + " on sdcard");
				}
			}

		}
		for (String fileName : allfiles) {

			copyFilesInTessdata(fileName);
		}
		// lang.traineddata file with the app (in assets folder)
		// You can get them at:
		// http://code.google.com/p/tesseract-ocr/downloads/list
		// This area needs work and optimization

		setContentView(R.layout.main);

		Button buttonName = (Button) findViewById(R.id.btn_ocrname);
		Button buttonIdNumber = (Button) findViewById(R.id.btn_ocridnumber);
		Button buttonAddress = (Button) findViewById(R.id.btn_ocraddress);
		Button buttonDate = (Button) findViewById(R.id.btn_ocrdate);
		Button buttonGender = (Button) findViewById(R.id.btn_ocrgender);
		Button buttonFathername = (Button) findViewById(R.id.btn_ocrfathername);
		Button buttonChooseimage = (Button) findViewById(R.id.btn_setimage);

		buttonName.setOnClickListener(this);
		buttonIdNumber.setOnClickListener(this);
		buttonAddress.setOnClickListener(this);
		buttonDate.setOnClickListener(this);
		buttonFathername.setOnClickListener(this);
		buttonGender.setOnClickListener(this);
		buttonChooseimage.setOnClickListener(this);
	}

	public void ShowImageChooser() {
		final String[] items = new String[] { "Take from camera",
				"Select from gallery" };
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.select_dialog_item, items);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setTitle("Select Image");
		builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) { // pick from
																	// camera
				if (item == 0) {
					Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

					mImageCaptureUri = Uri.fromFile(new File(Environment
							.getExternalStorageDirectory(), "tmp_img.jpg")

					);

					intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,
							mImageCaptureUri);

					try {
						intent.putExtra("return-data", true);

						startActivityForResult(intent, PICK_FROM_CAMERA);
					} catch (ActivityNotFoundException e) {
						e.printStackTrace();
					}
				} else { // pick from file

					Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
					photoPickerIntent.setType("image/*");
					startActivityForResult(photoPickerIntent, PICK_FROM_FILE);

					// Intent intent = new Intent();
					//
					// intent.setType("image/*");
					// intent.setAction(Intent.ACTION_GET_CONTENT);
					//
					// startActivityForResult(Intent.createChooser(intent,
					// "Complete action using"), PICK_FROM_FILE);
				}
			}
		});

		AlertDialog dialog = builder.create();

		// mImageView = (ImageView) findViewById(R.id.iv_photo);

		dialog.show();

	}

	public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
		int width = bm.getWidth();
		int height = bm.getHeight();
		float scaleWidth = ((float) newWidth) / width;
		float scaleHeight = ((float) newHeight) / height;
		// CREATE A MATRIX FOR THE MANIPULATION
		Matrix matrix = new Matrix();
		// RESIZE THE BIT MAP
		matrix.postScale(scaleWidth, scaleHeight);

		// "RECREATE" THE NEW BITMAP
		Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height,
				matrix, false);
		bm.recycle();
		return resizedBitmap;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != RESULT_OK) {
			if (requestCode != CROP_FROM_CAMERA) {
				mImageCaptureUri = null;
			}
			return;
		}
		switch (requestCode) {
		case PICK_FROM_CAMERA:

			if (ocrMode != OCR_NONE) {
				doCrop();
			}
			break;

		case PICK_FROM_FILE:

			mImageCaptureUri = data.getData();

			if (ocrMode != OCR_NONE) {
				doCrop();
			}

			break;

		case CROP_FROM_CAMERA:
			Bundle extras = data.getExtras();

			if (extras != null) {
				Bitmap photo = extras.getParcelable("data");
				photo = getResizedBitmap(photo, photo.getWidth() * 3,
						photo.getHeight() * 3);
				// photo = toGrayscale(photo);
				photo = photo.copy(Bitmap.Config.ARGB_8888, true);
				// mImageView.setImageBitmap(photo);

				// System.out.println("OCR text: " + ocr);
				if (ocrMode == OCR_NAME) {
					doOCR(photo, R.id.et_ocrname, R.id.img_ocrname);
				} else if (ocrMode == OCR_ADDRESS) {
					doOCR(photo, R.id.et_ocraddress, R.id.img_ocraddress);
				} else if (ocrMode == OCR_IDNUMBER) {
					doOCR(photo, R.id.et_ocridnumber, R.id.img_ocridnumber);
				} else if (ocrMode == OCR_DATE) {
					doOCR(photo, R.id.et_ocrdate, R.id.img_ocrdate);
				} else if (ocrMode == OCR_GENDER) {
					doOCR(photo, R.id.et_ocrgender, R.id.img_ocrgender);
				} else if (ocrMode == OCR_FATHERNAME) {
					doOCR(photo, R.id.et_ocrfathername, R.id.img_ocrfathername);
				}
				// EditText ocrtext = (EditText) findViewById(R.id.et_ocrtext);
				// ocrtext.setText(ocr);
			}

			// File f = new File(mImageCaptureUri.getPath());

			// if (f.exists())
			// f.delete();

			break;

		}
	}

	private void doOCR(final Bitmap bitmap, final int editTextR,
			final int previewImageR) {

		new AsyncTask<Bitmap, Void, String>() {

			public void onPreExecute() {
				((ImageView) findViewById(previewImageR))
						.setImageResource(R.drawable.ic_loading);
			}

			@Override
			protected String doInBackground(Bitmap... bits) {

				String recognizedText = "";
				try {
					TessBaseAPI baseApi = new TessBaseAPI();
					baseApi.setDebug(true);
					baseApi.init(DATA_PATH, lang,
							TessBaseAPI.OEM_TESSERACT_CUBE_COMBINED);

					// baseApi.init(DATA_PATH, lang);
					baseApi.setImage(bitmap);

					recognizedText = baseApi.getUTF8Text();

					baseApi.end();

					if (lang.equalsIgnoreCase("eng")) {
						// recognizedText = recognizedText
						// .replaceAll("[^a-zA-Z0-9]+", " ");
					}

					recognizedText = recognizedText.trim();
				} catch (Exception exp) {
					exp.printStackTrace();
					Toast.makeText(getApplicationContext(), exp.toString(), Toast.LENGTH_SHORT).show();

				}
				return recognizedText;
			}

			public void onPostExecute(String str) {

				((EditText) findViewById(editTextR)).setText(str);
				Bitmap small = getResizedBitmap(bitmap, bitmap.getWidth() / 2,
						bitmap.getHeight() / 2);
				((ImageView) findViewById(previewImageR)).setImageBitmap(small);
				System.out.println("OCR text: " + str);
			}

		}.execute();

	}

	public Bitmap toGrayscale1(Bitmap bmpOriginal) {
		int width, height;
		height = bmpOriginal.getHeight();
		width = bmpOriginal.getWidth();

		Bitmap bmpGrayscale = Bitmap.createBitmap(width, height,
				Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(bmpGrayscale);
		Paint paint = new Paint();
		ColorMatrix cm = new ColorMatrix();
		cm.setSaturation(0);
		ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
		paint.setColorFilter(f);
		c.drawBitmap(bmpOriginal, 0, 0, paint);
		return bmpGrayscale;
	}

	private void doCrop() {

		try {

			final ArrayList<CropOption> cropOptions = new ArrayList<CropOption>();

			Intent intent = new Intent("com.android.camera.action.CROP");
			intent.setType("image/*");

			List<ResolveInfo> list = getPackageManager().queryIntentActivities(
					intent, 0);

			int size = list.size();

			if (size == 0) {
				Toast.makeText(this, "Can not find image crop app",
						Toast.LENGTH_SHORT).show();

				return;
			} else {
				intent.setData(mImageCaptureUri);

				// intent.putExtra("outputX", 200);
				// intent.putExtra("outputY", 200);
				// intent.putExtra("aspectX", 1);
				// intent.putExtra("aspectY", 1);
				intent.putExtra("scale", true);
				intent.putExtra("return-data", true);

				if (size >= 1) {
					Intent i = new Intent(intent);
					ResolveInfo res = list.get(0);

					i.setComponent(new ComponentName(
							res.activityInfo.packageName, res.activityInfo.name));

					startActivityForResult(i, CROP_FROM_CAMERA);
				}
			}
		} catch (Exception exp) {

			String errorMessage = "Whoops - your device doesn't support the crop action!, more info: "
					+ exp.toString();
			Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();

		}
	}

	@Override
	public void onClick(View v) {

		if (v.getId() == R.id.btn_ocrname) {
			ocrMode = OCR_NAME;

		} else if (v.getId() == R.id.btn_ocraddress) {
			ocrMode = OCR_ADDRESS;

		} else if (v.getId() == R.id.btn_ocridnumber) {
			ocrMode = OCR_IDNUMBER;

		} else if (v.getId() == R.id.btn_ocrdate) {
			ocrMode = OCR_DATE;

		} else if (v.getId() == R.id.btn_ocrgender) {
			ocrMode = OCR_GENDER;

		} else if (v.getId() == R.id.btn_ocrfathername) {
			ocrMode = OCR_FATHERNAME;

		} else if (v.getId() == R.id.btn_setimage) {
			ocrMode = OCR_NONE;
			ShowImageChooser();
		}

		if (v.getId() != R.id.btn_setimage) {
			if (mImageCaptureUri == null) {

				ShowImageChooser();
			} else {

				doCrop();
			}
		}

	}
}