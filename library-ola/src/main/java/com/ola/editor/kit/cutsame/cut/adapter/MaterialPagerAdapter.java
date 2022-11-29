package com.ola.editor.kit.cutsame.cut.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.viewpager.widget.PagerAdapter;

import com.ola.editor.kit.cutsame.cut.lyrics.PlayerMaterialLyricsView;
import com.ola.editor.kit.cutsame.cut.textedit.view.PlayerMaterialTextEditView;
import com.ola.editor.kit.cutsame.cut.videoedit.customview.PlayerMaterialVideoView;
import com.ola.chat.picker.album.model.MaterialEditType;

import org.jetbrains.annotations.NotNull;

/**
 * @Author：yangbaojiang
 * @Date: 2022/9/27 11:08
 * @E-Mail: pgrammer.ybj@outlook.com
 * TODO:内部Adapter，包装setAdapter传进来的adapter，设置getCount返回Integer.MAX_VALUE
 */
public class MaterialPagerAdapter extends PagerAdapter {

    /**
     * 轮播图地址集合
     */
    private final MaterialEditType[] mEditTypeArray;
    private final PlayerMaterialVideoView playerMaterialVideoView;
    private final PlayerMaterialTextEditView playerMaterialTextEditView;
    private final PlayerMaterialLyricsView playerMaterialLyricsView;
    private final Context mContext;

    public MaterialPagerAdapter(Context context, @NotNull MaterialEditType[] editTypeArray,
                                PlayerMaterialVideoView playerMaterialVideoView,
                                PlayerMaterialTextEditView playerMaterialTextEditView,
                                PlayerMaterialLyricsView playerMaterialLyricsView) {
        mContext = context;
        mEditTypeArray = editTypeArray;
        this.playerMaterialVideoView = playerMaterialVideoView;
        this.playerMaterialTextEditView = playerMaterialTextEditView;
        this.playerMaterialLyricsView = playerMaterialLyricsView;
    }

    @Override
    public int getCount() {
        return mEditTypeArray.length;
    }

    @Override
    public boolean isViewFromObject(View arg0, Object arg1) {
        return arg0 == arg1;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        if (mEditTypeArray.length <= 0) {
            return container;
        }
        MaterialEditType materialEditType = mEditTypeArray[position];
        if (MaterialEditType.TYPE_VIDEO == materialEditType) {
            //视频
            container.addView(playerMaterialVideoView);
            return playerMaterialVideoView;
        } else if (MaterialEditType.TYPE_TEXT == materialEditType) {
            //文本
            container.addView(playerMaterialTextEditView);
            return playerMaterialTextEditView;
        } else {
            //歌词需要展示两列RecycleView
            container.addView(playerMaterialLyricsView);
            return playerMaterialLyricsView;
        }
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    //重写getPageTitle()方法
    @Override
    public CharSequence getPageTitle(int position) {
        //返回tab选项的名字
        return mContext.getString(mEditTypeArray[position].getStringId());
    }
}
