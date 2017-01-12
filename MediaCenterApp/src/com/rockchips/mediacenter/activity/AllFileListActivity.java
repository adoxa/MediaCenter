package com.rockchips.mediacenter.activity;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import momo.cn.edu.fjnu.androidutils.utils.BitmapUtils;
import momo.cn.edu.fjnu.androidutils.utils.SizeUtils;
import org.xutils.x;
import org.xutils.view.annotation.ViewInject;

import com.rockchips.mediacenter.adapter.AllFileListAdapter;
import com.rockchips.mediacenter.adapter.FileListAdapter;
import com.rockchips.mediacenter.adapter.FolderListAdapter;
import com.rockchips.mediacenter.audioplayer.InternalAudioPlayer;
import com.rockchips.mediacenter.bean.AllFileInfo;
import com.rockchips.mediacenter.bean.LocalDevice;
import com.rockchips.mediacenter.bean.LocalMediaFile;
import com.rockchips.mediacenter.bean.LocalMediaFolder;
import com.rockchips.mediacenter.data.ConstData;
import com.rockchips.mediacenter.imageplayer.InternalImagePlayer;
import com.rockchips.mediacenter.modle.db.LocalMediaFileService;
import com.rockchips.mediacenter.modle.task.AVBitmapLoadTask;
import com.rockchips.mediacenter.modle.task.AllFileLoadTask;
import com.rockchips.mediacenter.modle.task.FileLoadTask;
import com.rockchips.mediacenter.modle.task.FileMediaDataLoadTask;
import com.rockchips.mediacenter.modle.task.FolderLoadTask;
import com.rockchips.mediacenter.util.DialogUtils;
import com.rockchips.mediacenter.util.MediaFileUtils;
import com.rockchips.mediacenter.utils.GetDateUtil;
import com.rockchips.mediacenter.videoplayer.InternalVideoPlayer;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.AsyncTask.Status;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import com.rockchips.mediacenter.viewutils.preview.PreviewWidget;
import com.rockchips.mediacenter.R;
import com.rockchips.mediacenter.basicutils.util.ResLoadUtil;
import com.rockchips.mediacenter.basicutils.util.DiskUtil;
import com.rockchips.mediacenter.basicutils.bean.LocalDeviceInfo;
import com.rockchips.mediacenter.basicutils.bean.LocalMediaInfo;

/**
 * @author GaoFei
 * 所有文件浏览页面
 */
public class AllFileListActivity extends AppBaseActivity implements OnItemSelectedListener, OnItemClickListener{

	public static final String TAG = "FileListActivity";
	protected static final int START_PLAYER_REQUEST_CODE = 99;
	@ViewInject(R.id.text_path_title)
	private TextView mTextPathTitle;
	@ViewInject(R.id.list_file)
	private ListView mListFile;
	@ViewInject(R.id.widget_preview)
	private PreviewWidget mWidgetPreview;
	@ViewInject(R.id.layout_no_files)
	private RelativeLayout mLayoutNoFiles;
	@ViewInject(R.id.layout_search_no_data)
	private LinearLayout mLayoutSearchNoData;
	@ViewInject(R.id.progress_loading)
	private ProgressBar mPregressLoading;
	@ViewInject(R.id.layout_content_page)
	private LinearLayout mLayoutContentPage;
	@ViewInject(R.id.text_file_name)
	private TextView mTextFileName;
	private int mCurrMediaType;
	/**
	 * 当前设备
	 */
	private LocalDevice mCurrDevice;
	/**
	 * 当前目录路径
	 */
	private String mCurrFolder;
	private AllFileListAdapter mAllFileListAdapter;
	private FileListAdapter mFileAdapter;
	private FolderLoadTask mFolderLoadTask;
	private FileLoadTask mFileLoadTask;
	private LocalMediaFolder mSelectFolder;
	private LocalMediaFile mSelectFile;
	/**
	 * 文件列表加载器
	 */
	private AllFileLoadTask mAllFileLoadTask;
	
	/**
	 * 当前焦点文件
	 */
	private AllFileInfo mCurrentFileInfo;
	/**
	 * 当前文件夹列表选中的位置
	 */
	private int mFolderSelection = 0;
	/**
	 * 当前文件列表选中位置
	 */
	private int mFileSelection = 0;
	/**
	 * 当前焦点位置
	 */
	private int mCurrentFocusPosition;
	/**
	 * 视频，音乐文件缩列图文件获取器
	 */
	private AVBitmapLoadTask mBitmapLoadTask;
	
	private Bitmap mOldBitmap;
	
	/**
	 * 上次选中的路径
	 */
	private String mLastSelectPath;
	
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_list);
        x.view().inject(this);
        initDataAndView();
        initEvent();
    }
    
    
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		AllFileInfo allFileInfo = (AllFileInfo)parent.getAdapter().getItem(position);
		if(allFileInfo.getFile().isDirectory()){
			mCurrFolder = allFileInfo.getFile().getPath();
			loadFiles();
		}else{
			loadActivity(allFileInfo);
		}
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position,
			long id) {
		mCurrentFileInfo = (AllFileInfo)parent.getAdapter().getItem(position);
		refreshPreview(mCurrentFileInfo);
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK){
			if(!mCurrFolder.equals(mCurrDevice.getMountPath())){
				mLastSelectPath = mCurrFolder;
				mCurrFolder = new File(mCurrFolder).getParentFile().getPath();
				loadFiles();
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}
	
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		//loadFiles(mSelectFolder, true);
	}
	
    public void initDataAndView(){
    	mPregressLoading.setVisibility(View.GONE);
    	mCurrMediaType = getIntent().getIntExtra(ConstData.IntentKey.EXTRAL_MEDIA_TYPE, -1);
    	mCurrDevice = (LocalDevice)getIntent().getSerializableExtra(ConstData.IntentKey.EXTRAL_LOCAL_DEVICE);
    	//挂载目录作为当前目录
    	mCurrFolder = mCurrDevice.getMountPath();
    	loadFiles();
    }

    public void initEvent(){
    	mListFile.setOnItemClickListener(this);
    	mListFile.setOnItemSelectedListener(this);
    }
    
    private void refreshPreview(AllFileInfo fileInfo)
    {
    	//更新头部信息
        mTextFileName.setText(fileInfo.getFile().getName());
    	mWidgetPreview.updateName(fileInfo.getFile().getName());
    	mWidgetPreview.updateOtherText(getAllFilePreviewInfo(fileInfo));
    	mWidgetPreview.updateImage(getPreviewIcon(fileInfo.getType()));
		Bitmap previewBitmap = null;
        switch (fileInfo.getType())
        {
            case ConstData.MediaType.AUDIO:
            case ConstData.MediaType.VIDEO:
            	updateOtherText(fileInfo);
            	if(!fileInfo.isLoadPreview()){
            		loadBitmapForAVFile(fileInfo);
            	}else{
            		previewBitmap = BitmapUtils.getScaledBitmapFromFile(fileInfo.getPriviewPhotoPath(), SizeUtils.dp2px(this, 280), SizeUtils.dp2px(this, 280));
        			if(previewBitmap != null){
        				mWidgetPreview.updateImage(previewBitmap);
        			}
            	}
                break;
            case ConstData.MediaType.IMAGE:
            	previewBitmap = BitmapUtils.getScaledBitmapFromFile(fileInfo.getFile().getPath(), SizeUtils.dp2px(this, 280), SizeUtils.dp2px(this, 280));
    			if(previewBitmap != null){
    				mWidgetPreview.updateImage(previewBitmap);
    			}
            	updateOtherText(fileInfo);
                break;
        }  
	

    }
    
    /**
     * 是否显示文件夹
     * @return
     */
    public boolean isShowFolder(){
    	return (mListFile.getAdapter() instanceof FolderListAdapter);
    }
    
	/**
	 * 加载文件夹列表
	 */
	public void loadFiles(){
		DialogUtils.showLoadingDialog(this, false);
		mAllFileLoadTask = new AllFileLoadTask(new AllFileLoadTask.CallBack() {
			@Override
			public void onGetFiles(List<AllFileInfo> fileInfos) {
				DialogUtils.closeLoadingDialog();
				mTextPathTitle.setText(mCurrFolder.substring(mCurrFolder.lastIndexOf("/") + 1, mCurrFolder.length()));
				if(fileInfos != null && fileInfos.size() > 0){
					mLayoutContentPage.setVisibility(View.VISIBLE);
					mLayoutNoFiles.setVisibility(View.GONE);
					mListFile.requestFocus();
					mAllFileListAdapter = new AllFileListAdapter(AllFileListActivity.this, R.layout.adapter_file_list_item, fileInfos);
					mListFile.setAdapter(mAllFileListAdapter);
					if(!TextUtils.isEmpty(mLastSelectPath)){
						int position = getFilePosition(mLastSelectPath, fileInfos);
						mListFile.setSelection(position);
					}
				}else{
					mLayoutContentPage.setVisibility(View.GONE);
					mLayoutNoFiles.setVisibility(View.VISIBLE);
				}
			}
		});
		mAllFileLoadTask.execute(mCurrFolder);
	}
	
	
    
    
    private String getAllFilePreviewInfo(AllFileInfo fileInfo){
    	String info = "";
    	if(fileInfo.getFile().isDirectory()){
    		info = getString(R.string.file_tip) + fileInfo.getFile().list().length;
    	}else{
    		 String dateStr = null;
    	     dateStr = GetDateUtil.getTime(this, fileInfo.getFile().lastModified() / 1000);
    	     if (dateStr == null){
    	    	 dateStr = getString(R.string.unknown);
    	     }
    	     info = getString(R.string.file_size_tip) + getFileSize(fileInfo.getFile().length()) + "\n" + getString(R.string.modify_time_tip) + dateStr;
    	}
    	return info;
    }
    
    
    protected String getFileSize(long size){
        if (size < 1024 && size > 0)
        {
            return size + " " + ResLoadUtil.getStringById(this, R.string.unit_disk_size_b);
        }
        else if (size == 0)
        {
            return ResLoadUtil.getStringById(this, R.string.real_unknown);
        }

        return DiskUtil.getDiskSizeString(this, Long.valueOf(size / 1024).intValue(), R.string.unknown, R.string.unit_disk_size_kb,
                R.string.unit_disk_size_mb, R.string.unit_disk_size_gb, R.string.unit_disk_size_tb);
    }
    
    protected Bitmap getPreviewIcon(int type)
    {
        int resId;
        switch (type)
        {
            case ConstData.MediaType.AUDIO:
                resId = R.drawable.icon_preview_audio;
                break;
            case ConstData.MediaType.IMAGE:
                resId = R.drawable.icon_preview_image;
                break;
            case ConstData.MediaType.FOLDER:
                resId = R.drawable.icon_preview_folder;
                break;
            case ConstData.MediaType.VIDEO:
                resId = R.drawable.icon_preview_video;
                break;
            case ConstData.MediaType.DEVICE:
                resId = R.drawable.icon_preview_disk;
                break;
            default:
                resId = R.drawable.icon_preview_unknow;
                break;
        }
        return getBitmapById(resId);
    }

    private Bitmap getBitmapById(int id){
    	Bitmap bitmap = BitmapFactory.decodeResource(getResources(), id);
    	if(mOldBitmap != null && !mOldBitmap.isRecycled())
    		mOldBitmap.recycle();
    	mOldBitmap = bitmap;
    	return bitmap;
    }
    
    
    private void updateOtherText(AllFileInfo allFileInfo)
    {
        String strInfo = null;
        switch(allFileInfo.getType())
        {
            case ConstData.MediaType.AUDIO:
            	strInfo = String.format(getString(R.string.audio_preview_info), 
                		getFileSize(allFileInfo.getFile().length()), 
                		getFileType(allFileInfo.getFile().getName(),getString(R.string.music), mCurrDevice.getDevices_type()), 
                		getRunningTime(allFileInfo),
                		formatCreateDate(allFileInfo),getDescription(""));
            	break;
            case ConstData.MediaType.VIDEO:
            	strInfo = String.format(getString(R.string.audio_preview_info), 
                		getFileSize(allFileInfo.getFile().length()), 
                		getFileType(allFileInfo.getFile().getName(),getString(R.string.video), mCurrDevice.getDevices_type()), 
                		getRunningTime(allFileInfo),
                		formatCreateDate(allFileInfo),getDescription(""));
              break;
            // 显示尺寸
            case ConstData.MediaType.IMAGE:
                strInfo = String.format(getString(R.string.image_preview_info), getFileSize(allFileInfo.getFile().length()),
                        getFileType(allFileInfo.getFile().getName(),getString(R.string.picture), mCurrDevice.getDevices_type()), formatCreateDate(allFileInfo),getDescription(""));
                break;
        }
        mWidgetPreview.updateOtherText(strInfo);
    }
    
    /**
     * 加载播放器
     */
    public void loadActivity(AllFileInfo allFileInfo){
    	int fileType = allFileInfo.getType();
    	if(fileType != ConstData.MediaType.AUDIO && fileType != ConstData.MediaType.VIDEO
    			&& fileType != ConstData.MediaType.IMAGE && fileType != ConstData.MediaType.APK){
    		return;
    	}
        Intent intent = new Intent();
        intent.putExtra(ConstData.IntentKey.IS_INTERNAL_PLAYER, true);
        intent.putExtra(ConstData.IntentKey.EXTRAL_LOCAL_DEVICE, mCurrDevice);
        intent.putExtra(LocalDeviceInfo.DEVICE_EXTRA_NAME, MediaFileUtils.getDeviceInfoFromDevice(mCurrDevice).compress());
        List<LocalMediaInfo> mediaInfos = MediaFileUtils.getMediaInfosFromAllFileInfo(allFileInfo, mCurrDevice);
        List<Bundle> mediaInfoList = new ArrayList<Bundle>();
        for(LocalMediaInfo itemInfo : mediaInfos){
        	mediaInfoList.add(itemInfo.compress());
        }
        int newPosition = 0;
        for(int i = 0; i != mediaInfos.size(); ++i){
        	if(allFileInfo.getFile().getName().equals(mediaInfos.get(i).getmFileName())){
        		newPosition = i;
        		break;
        	}
        }
        if (allFileInfo.getType() == ConstData.MediaType.AUDIO)
        {
            intent.setClass(this, InternalAudioPlayer.class);
            intent.putExtra(ConstData.IntentKey.CURRENT_INDEX, newPosition);
            InternalAudioPlayer.setMediaList(mediaInfoList, newPosition);
        }
        else if (allFileInfo.getType() == ConstData.MediaType.VIDEO)
        {
            intent.setClass(this, InternalVideoPlayer.class);
            intent.putExtra(ConstData.IntentKey.CURRENT_INDEX, newPosition);
            InternalVideoPlayer.setMediaList(mediaInfoList, newPosition);
        }
        else if (allFileInfo.getType() == ConstData.MediaType.IMAGE)
        {
            intent.setClass(this, InternalImagePlayer.class);
            intent.putExtra(ConstData.IntentKey.IS_INTERNAL_PLAYER, true);
            intent.putExtra(ConstData.IntentKey.CURRENT_INDEX, newPosition);
            InternalImagePlayer.setMediaList(mediaInfoList, newPosition);
        }else if(allFileInfo.getType() == ConstData.MediaType.APK){
        	Intent installIntent = new Intent(Intent.ACTION_VIEW);
        	installIntent.setDataAndType(Uri.fromFile(allFileInfo.getFile()), "application/vnd.android.package-archive");
        	installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        	startActivity(installIntent);
        	return;
        }
        startActivityForResult(intent, START_PLAYER_REQUEST_CODE);
    }
    
    private static final int INDEX_OF_SPLIT_01 = -1;
    private static final int INDEX_OF_SPLIT_1 = 1;
    /** DTS2015012807455 解决音乐、视频，不显示时长的问题  by zWX238093 */
    protected String getRunningTime(AllFileInfo fileInfo)
    {
    	if(TextUtils.isEmpty(fileInfo.getDuration())){
    		return getString(R.string.unknown_durnation);
    	}
    	return fileInfo.getDuration();
    }
    
    private String getDescription(String description){	
		if (TextUtils.isEmpty(description))
		{		
			description = getString(R.string.unknown);
		}		
		return description;
	}
    
    private String formatCreateDate(AllFileInfo allFileInfo){
        String dataStr;
        dataStr = GetDateUtil.getTime(AllFileListActivity.this, allFileInfo.getFile().lastModified() / 1000);
        if (TextUtils.isEmpty(dataStr))
        {
            dataStr = getString(R.string.unknown);
        }
        return dataStr;
    }
    
    private String getFileType(String filename,String typename,int deviceType)
	{				
		Log.d(TAG, "=====deviceType==="+deviceType);
		String fileType=typename;
		if (deviceType == ConstData.DeviceType.DEVICE_TYPE_U 
				|| deviceType == ConstData.DeviceType.DEVICE_TYPE_SD
				|| deviceType == ConstData.DeviceType.DEVICE_TYPE_SMB
				|| deviceType == ConstData.DeviceType.DEVICE_TYPE_NFS)
		{					
			fileType = filename.substring(filename.lastIndexOf(".")+1);
		}		
		return fileType;
	}
    
   /**
    * 获取文件夹索引
    * @param mediaFolder
    * @param mediaFiles
    * @return
    */
    private int getFolderIndex(LocalMediaFolder mediaFolder, List<LocalMediaFolder> mediaFiles){
    	if(mediaFolder == null)
    		return -1;
    	if(mediaFiles == null || mediaFiles.size() == 0)
    		return -1;
    	for(int i = 0; i != mediaFiles.size(); ++i){
    		if(mediaFiles.get(i).getFolderId() == mediaFolder.getFolderId()){
    			return i;
    		}
    	}
    	return -1;
    }
    
    /**
     * 获取文件索引
     * @param mediaFile
     * @param mediaFiles
     * @return
     */
    private int getFileIndex(LocalMediaFile mediaFile, List<LocalMediaFile> mediaFiles){
    	if(mediaFile == null)
    		return -1;
    	if(mediaFiles == null || mediaFiles.size() == 0)
    		return -1;
    	for(int i = 0; i != mediaFiles.size(); ++i){
    		if(mediaFiles.get(i).getFileId() == mediaFile.getFileId()){
    			return i;
    		}
    	}
    	return -1;
    }
    
    /**
     * 获取音屏和视频文件的缩列图
     * @param allFileInfo
     */
    private void loadBitmapForAVFile(AllFileInfo allFileInfo){
    	if(mBitmapLoadTask != null && mBitmapLoadTask.getStatus() == Status.RUNNING)
    		mBitmapLoadTask.cancel(true);
    	mBitmapLoadTask = new AVBitmapLoadTask(new AVBitmapLoadTask.CallBack() {
			
			@Override
			public void onFinished(AllFileInfo allFileInfo) {
				if(allFileInfo == mCurrentFileInfo){
					refreshPreview(allFileInfo);
				}
			}
		});
    	mBitmapLoadTask.execute(allFileInfo);
    }
    
    /**
     * 根据路径获取列表位置,最好改成异步实现
     * @param path
     * @param allFileInfos
     * @return
     */
	public int getFilePosition(String path, List<AllFileInfo> allFileInfos){
		int position = 0;
		if(allFileInfos != null && allFileInfos.size() > 0){
			for(int i = 0; i < allFileInfos.size(); ++i){
				if(allFileInfos.get(i).getFile().getPath().equals(path)){
					position = i;
					break;
				}
			}
		}
		return position;
	}
}
