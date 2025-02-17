package cn.me.archko.pdf;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import cx.hell.android.pdfviewpro.APVApplication;
import cx.hell.android.pdfviewpro.Bookmark;
import cx.hell.android.pdfviewpro.BookmarkEntry;
import cx.hell.android.pdfviewpro.StreamUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;

/**
 * 存储最近阅读的记录
 *
 * @author: archko 2014/4/17 :15:05
 */
public class AKRecent implements Serializable {

    public static final String TAG="AKRecent";
    private static final long serialVersionUID=4899452726203839401L;
    private final String FILE_RECENT="AKRecent";

    ArrayList<AKProgress> mAkProgresses;
    Context mContext;
    private static AKRecent sInstance;

    public ArrayList<AKProgress> getAkProgresses() {
        return mAkProgresses;
    }

    public static AKRecent getInstance(Context context) {
        if (null==sInstance) {
            sInstance=new AKRecent(context);
        }
        return sInstance;
    }

    private AKRecent(Context context) {
        mContext=context;
    }

    /**
     * 异步添加进度,
     *
     * @param path          文件全路径
     * @param page          当前的页码
     * @param numberOfPage  总页数
     * @param bookmarkEntry 书签字符串,是一个合并形式的.
     */
    public void addAsync(final String path, final int page, final int numberOfPage, final String bookmarkEntry) {
        Util.execute(false, new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                add(path, page, numberOfPage, bookmarkEntry);
                return null;
            }
        }, (Void[]) null);
    }

    public ArrayList<AKProgress> add(final String path, final int page, final int numberOfPage, String bookmarkEntry) {
        if (TextUtils.isEmpty(path)) {
            Log.d("", "path is null.");
            return mAkProgresses;
        }

        if (null==mAkProgresses) {
            mAkProgresses=new ArrayList<AKProgress>();
            readRecent();
        }

        if (mAkProgresses.size()>0) {
            AKProgress tmp=null;
            for (AKProgress progress : mAkProgresses) {
                if (progress.path.equals(path)) {
                    tmp=progress;
                    break;
                }
            }

            if (tmp!=null) {
                mAkProgresses.remove(tmp);
                tmp.timestampe=System.currentTimeMillis();
                tmp.page=page;
                tmp.numberOfPages=numberOfPage;
                tmp.bookmarkEntry=bookmarkEntry;
                mAkProgresses.add(0, tmp);
                String filepath=mContext.getFilesDir().getPath()+File.separator+FILE_RECENT;
                Util.serializeObject(mAkProgresses, filepath);
            } else {
                addNewRecent(path, page, numberOfPage, bookmarkEntry);
            }
            Log.d(TAG, "add :"+" progress:"+tmp);
        } else {
            addNewRecent(path, page, numberOfPage, bookmarkEntry);
        }
        return mAkProgresses;

    }

    private void addNewRecent(String path, int page, int numberOfPage, String bookmarkEntry) {
        Log.d(TAG, "addNewRecent:"+page+" nop:"+numberOfPage+" path:"+path);
        AKProgress tmp=new AKProgress(path);
        tmp.page=page;
        tmp.numberOfPages=numberOfPage;
        tmp.bookmarkEntry=bookmarkEntry;
        mAkProgresses.add(tmp);
        String filepath=mContext.getFilesDir().getPath()+File.separator+FILE_RECENT;
        Util.serializeObject(mAkProgresses, filepath);
    }

    public ArrayList<AKProgress> remove(final String path) {
        Log.d(TAG, "remove:"+path);
        if (TextUtils.isEmpty(path)) {
            Log.d("", "path is null.");
            return null;
        }

        if (null==mAkProgresses) {
            mAkProgresses=new ArrayList<AKProgress>();
            readRecent();
        }

        if (mAkProgresses.size()>0) {
            AKProgress tmp=null;
            for (AKProgress progress : mAkProgresses) {
                if (progress.path.equals(path)) {
                    tmp=progress;
                    break;
                }
            }

            Log.d(TAG, "remove :"+" progress:"+tmp);
            if (tmp!=null) {
                mAkProgresses.remove(tmp);
                String filepath=mContext.getFilesDir().getPath()+File.separator+FILE_RECENT;
                Util.serializeObject(mAkProgresses, filepath);
            }
        }
        return null;
    }

    public void readRecent() {
        if (null==mAkProgresses) {
            mAkProgresses=new ArrayList<AKProgress>();
        }
        String path=mContext.getFilesDir().getPath()+File.separator+FILE_RECENT;
        ArrayList<AKProgress> progresses=(ArrayList<AKProgress>) Util.deserializeObject(path);
        if (null!=progresses) {
            mAkProgresses.clear();
            mAkProgresses.addAll(progresses);
            Log.d(TAG, "read path:"+path+" size:"+mAkProgresses.size());
        }
    }

    public String backup() {
        if (null==mAkProgresses||mAkProgresses.size()<1) {
            readRecent();
        }

        try {
            String name=DateUtil.formatTime(System.currentTimeMillis(), "yyyy-MM-dd-HH-mm-ss");
            JSONObject root=new JSONObject();
            JSONArray ja=new JSONArray();
            root.put("root", ja);
            root.put("name", name);

            JSONObject tmp;
            int i=0;
            for (AKProgress progress : mAkProgresses) {
                tmp=new JSONObject();
                try {
                    tmp.put("index", progress.index);
                    tmp.put("path", URLEncoder.encode(progress.path));
                    tmp.put("numberOfPages", progress.numberOfPages);
                    tmp.put("page", progress.page);
                    tmp.put("size", progress.size);
                    tmp.put("ext", progress.ext);
                    tmp.put("timestampe", progress.timestampe);
                    tmp.put("bookmarkEntry", progress.bookmarkEntry);
                    ja.put(i, tmp);
                    i++;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            name=Environment.getExternalStorageDirectory().getPath()+File.separator+"mupdf_"+name;
            Log.d(TAG, "backup.name:"+name+" root:"+root);
            StreamUtils.copyStringToFile(root.toString(), name);
            return name;
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean restore(String filepath) {
        boolean flag=false;
        try {
            String content=StreamUtils.parseFile(filepath);
            Log.d(TAG, "restore.file:"+filepath+" content:"+content);
            ArrayList<AKProgress> akProgresses=parseProgresses(content);
            if (null!=akProgresses&&akProgresses.size()>0) {
                mAkProgresses=akProgresses;
                File f=new File(APVApplication.getInstance().getFilesDir().getPath()+File.separator+"mupdf_recent.jso");
                StreamUtils.copyStringToFile(content, f.getAbsolutePath());
                filepath=mContext.getFilesDir().getPath()+File.separator+FILE_RECENT;
                Util.serializeObject(mAkProgresses, filepath);
                flag=true;
            }

            Bookmark b=null;
            try {
                b=new Bookmark(APVApplication.getInstance()).open();
                b.getDb().beginTransaction();
                for (AKProgress progress : akProgresses) {
                    if (!TextUtils.isEmpty(progress.bookmarkEntry)) {
                        b.setLast(progress.path, new BookmarkEntry(progress.bookmarkEntry));
                        Log.d(TAG, "update bookmark:"+progress);
                    }
                }
                b.getDb().setTransactionSuccessful();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (null!=b) {
                    b.getDb().endTransaction();
                    b.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return flag;
    }

    public static AKProgress parseProgress(JSONObject jsonobject) throws Exception {
        if (null==jsonobject) {
            return null;
        }
        AKProgress bean=new AKProgress();
        try {
            bean.index=jsonobject.optInt("index");
            bean.path=URLDecoder.decode(jsonobject.optString("path"));
            bean.numberOfPages=jsonobject.optInt("numberOfPages");
            bean.page=jsonobject.optInt("page");
            bean.size=jsonobject.optInt("size");
            bean.ext=jsonobject.optString("ext");
            bean.timestampe=jsonobject.optLong("timestampe");
            bean.bookmarkEntry=jsonobject.optString("bookmarkEntry");
        } catch (Exception jsonexception) {
            throw new Exception(jsonexception.getMessage()+":"+jsonobject, jsonexception);
        }
        return bean;
    }

    /**
     * @return
     * @throws WeiboException
     */
    private static ArrayList<AKProgress> parseProgresses(String jo) {
        ArrayList<AKProgress> arraylist=new ArrayList<AKProgress>();
        int i=0;

        try {
            JSONObject json=new JSONObject(jo);
            JSONArray jsonarray=json.optJSONArray("root");
            int len=jsonarray.length();
            for (; i<len; i++) {
                AKProgress bean=null;
                bean=parseProgress(jsonarray.optJSONObject(i));
                arraylist.add(bean);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return arraylist;
    }
}
