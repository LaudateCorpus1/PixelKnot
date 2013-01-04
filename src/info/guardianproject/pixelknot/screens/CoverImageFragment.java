package info.guardianproject.pixelknot.screens;

import info.guardianproject.pixelknot.Constants;
import info.guardianproject.pixelknot.R;
import info.guardianproject.pixelknot.Constants.PixelKnot.Keys;
import info.guardianproject.pixelknot.utils.ActivityListener;
import info.guardianproject.pixelknot.utils.FragmentListener;
import info.guardianproject.pixelknot.utils.IO;
import info.guardianproject.pixelknot.utils.PixelKnotMediaScanner;
import info.guardianproject.pixelknot.utils.PixelKnotMediaScanner.MediaScannerListener;

import java.io.File;

import org.json.JSONException;

import com.actionbarsherlock.app.SherlockFragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

public class CoverImageFragment extends SherlockFragment implements Constants, MediaScannerListener, ActivityListener {
	View root_view;
	ImageView cover_image_holder;
	int blank_image;

	Uri cover_image_uri = null;
	String path_to_cover_image = null;
	File cover_image_file = null;

	Activity a;
	Handler h = new Handler();
	ProgressDialog progress_dialog;

	@Override
	public View onCreateView(LayoutInflater li, ViewGroup container, Bundle savedInstanceState) {
		root_view = li.inflate(R.layout.cover_image_fragment, container, false);
		cover_image_holder = (ImageView) root_view.findViewById(R.id.cover_image_holder);
		cover_image_holder.setImageResource(blank_image);
		return root_view;
	}

	@Override
	public void onAttach(Activity a) {
		super.onAttach(a);
		this.a = a;

		switch(Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this.a).getString(Settings.LANGUAGE, String.valueOf(Settings.Locales.DEFAULT)))) {
		case Settings.Locales.FA:
			blank_image = R.drawable.pixelknot_blank_image_fa;
			break;
		default:
			blank_image = R.drawable.pixelknot_blank_image_en;
			break;
		}

		if(!((FragmentListener) a).getHasSeenFirstPage()) {
			initButtons();
			((FragmentListener) a).setHasSeenFirstPage(true);
		}
	}

	public void setImageData(String path_to_cover_image) {
		this.path_to_cover_image = path_to_cover_image;
		setImageData();
	}

	public void setImageData() {
		h.post(new Runnable() {
			@Override
			public void run() {
				Bitmap b = BitmapFactory.decodeFile(path_to_cover_image);
				int scale = Math.min(4, b.getWidth()/10);
				if(b.getHeight() > b.getWidth())
					scale = Math.min(4, b.getHeight()/10);

				BitmapFactory.Options opts = new BitmapFactory.Options();
				opts.inSampleSize = scale;
				b.recycle();

				Bitmap b_ = BitmapFactory.decodeFile(path_to_cover_image, opts);
				cover_image_holder.setImageBitmap(b_);
				
				try {
					progress_dialog.dismiss();
				} catch(NullPointerException e) {}
				
			}
		});

		((FragmentListener) a).getPixelKnot().setCoverImageName(path_to_cover_image);
		h.postDelayed(new Runnable() {
			@Override
			public void run() {
				((FragmentListener) a).autoAdvance();
			}
		}, 1000);

	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if(resultCode == Activity.RESULT_OK) {
			if(requestCode == Source.GALLERY || requestCode == Source.CAMERA) {
				((FragmentListener) a).setCanAutoAdvance(true);
				if(requestCode == Source.GALLERY) {
					cover_image_uri = data.getData();
					path_to_cover_image = IO.pullPathFromUri(a, cover_image_uri);
					cover_image_file = new File(path_to_cover_image);
					setImageData();
				} else if(requestCode == Source.CAMERA) {
					new PixelKnotMediaScanner(a, path_to_cover_image);
				}    			
			}
		}
	}

	@Override
	public void onMediaScanned(String path, Uri uri) {
		path_to_cover_image = path;
		cover_image_file = new File(path_to_cover_image);
		cover_image_uri = uri;
		setImageData();
	}

	@Override
	public void initButtons() {
		ImageButton take_picture = new ImageButton(a);
		take_picture.setBackgroundColor(getResources().getColor(android.R.color.transparent));
		take_picture.setPadding(0, 0, 0, 0);
		take_picture.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				progress_dialog = ProgressDialog.show(a, "", getString(R.string.please_wait));
				
				cover_image_file = new File(DUMP, "temp_img.jpg");
				cover_image_uri = Uri.fromFile(cover_image_file);
				path_to_cover_image = cover_image_file.getAbsolutePath();
				Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, cover_image_uri);
				startActivityForResult(cameraIntent, Source.CAMERA);
			}

		});
		take_picture.setImageResource(R.drawable.camera_selector);

		ImageButton choose_picture = new ImageButton(a);
		choose_picture.setBackgroundColor(getResources().getColor(android.R.color.transparent));
		choose_picture.setPadding(0, 0, 0, 0);
		choose_picture.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				progress_dialog = ProgressDialog.show(a, "", getString(R.string.please_wait));
				
				Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*");
				startActivityForResult(galleryIntent, Source.GALLERY);
			}

		});
		choose_picture.setImageResource(R.drawable.gallery_selector);

		((FragmentListener) a).setButtonOptions(new ImageButton[] {take_picture, choose_picture});
	}

	@Override
	public void updateUi() {
		try {
			path_to_cover_image = ((FragmentListener) a).getPixelKnot().has(Keys.COVER_IMAGE_NAME) ? ((FragmentListener) a).getPixelKnot().getString(Keys.COVER_IMAGE_NAME) : null;
		} catch (JSONException e) {
			Log.e(Logger.UI, e.toString());
			e.printStackTrace();
		} 

		if(path_to_cover_image == null) {
			cover_image_file = null;
			cover_image_uri = null;
			cover_image_holder.setImageResource(blank_image);
		} else {
			setImageData();
		}
		

	}
}